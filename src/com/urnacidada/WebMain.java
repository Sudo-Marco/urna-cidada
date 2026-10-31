package com.urnacidada;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.urnacidada.model.Candidato;
import com.urnacidada.model.Eleicao;
import com.urnacidada.repository.VotoRepository;
import com.urnacidada.view.ControladorUrna;
import com.urnacidada.view.EstadoTela;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Servidor local da interface web. A lógica de votação continua no UrnaService. */
public final class WebMain {
    private static final String HOST = "127.0.0.1";

    private final Eleicao eleicao = Eleicao.exemplo();
    private final VotoRepository repositorio;
    private final int port;
    private ControladorUrna controlador;
    private int indiceCargo;

    private WebMain(VotoRepository repositorio, int port) {
        this.repositorio = repositorio;
        this.port = port;
    }

    public static void main(String[] args) {
        TravaInstancia trava = null;
        VotoRepository repositorio = null;
        HttpServer servidor = null;
        ExecutorService executor = null;
        try {
            String modo = modo(args);
            int port = Integer.parseInt(System.getenv().getOrDefault("URNA_WEB_PORT", "8080"));
            if (port < 1 || port > 65535) throw new IllegalArgumentException("A porta deve estar entre 1 e 65535.");

            Path app = CaminhosAplicacao.diretorioDaAplicacao();
            Path perfil = Path.of(System.getProperty("user.home", "."));
            Path dados = CaminhosAplicacao.resolverPastaDados(app, perfil);
            Path logs = CaminhosAplicacao.resolverPastaLogs(app, perfil);
            LogUrna.configurar(logs);
            trava = TravaInstancia.tentar(dados.resolve(".urna-instancia.lock"));
            if (trava == null) throw new IOException("Outra urna já está aberta. Feche-a antes de iniciar o servidor web.");
            repositorio = Main.selecionarRepositorio(modo, dados, Path.of("urna.properties"));

            WebMain appWeb = new WebMain(repositorio, port);
            servidor = HttpServer.create(new InetSocketAddress(HOST, port), 0);
            servidor.createContext("/", appWeb::atender);
            executor = Executors.newSingleThreadExecutor(tarefa -> {
                Thread thread = new Thread(tarefa, "urna-servidor-web");
                thread.setDaemon(false);
                return thread;
            });
            servidor.setExecutor(executor);
            servidor.start();

            HttpServer servidorFinal = servidor;
            ExecutorService executorFinal = executor;
            TravaInstancia travaFinal = trava;
            VotoRepository repositorioFinal = repositorio;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                servidorFinal.stop(1);
                executorFinal.shutdownNow();
                fechar(repositorioFinal);
                try { travaFinal.close(); } catch (IOException ignorada) { }
            }, "urna-encerramento-web"));

            System.out.println("Urna web disponível em http://127.0.0.1:" + port);
            System.out.println("Armazenamento: " + repositorio.dicaPersistencia());
            System.out.println("Pressione Ctrl+C para encerrar o servidor.");
            new CountDownLatch(1).await();
        } catch (Exception e) {
            System.err.println("Não foi possível iniciar a urna web: " + mensagem(e));
            if (servidor != null) servidor.stop(0);
            if (executor != null) executor.shutdownNow();
            fechar(repositorio);
            if (trava != null) try { trava.close(); } catch (IOException ignorada) { }
        }
    }

    private void atender(HttpExchange troca) throws IOException {
        try {
            if (!hostPermitido(troca)) {
                responder(troca, 403, "application/json; charset=utf-8", "{\"erro\":\"Acesso local permitido apenas pelo endereço da urna.\"}");
                return;
            }
            String caminho = troca.getRequestURI().getPath();
            if (caminho.startsWith("/api/")) {
                atenderApi(troca, caminho);
                return;
            }
            atenderArquivo(troca, caminho);
        } catch (IllegalArgumentException | IllegalStateException e) {
            responder(troca, 400, "application/json; charset=utf-8", "{\"erro\":" + json(mensagem(e)) + "}");
        } catch (Exception e) {
            LogUrna.logger().warning("Falha na requisição web (" + e.getClass().getSimpleName() + ").");
            responder(troca, 500, "application/json; charset=utf-8", "{\"erro\":" + json(mensagem(e)) + "}");
        } finally {
            troca.close();
        }
    }

    private synchronized void atenderApi(HttpExchange troca, String caminho) throws IOException {
        String metodo = troca.getRequestMethod();
        if (caminho.equals("/api/state") && metodo.equals("GET")) {
            responder(troca, 200, "application/json; charset=utf-8", estadoJson());
            return;
        }
        if (!metodo.equals("POST")) {
            responder(troca, 405, "application/json; charset=utf-8", "{\"erro\":\"Método não permitido.\"}");
            return;
        }
        if (caminho.equals("/api/start")) {
            if (controlador != null && !controlador.estado().finalizada()) {
                responder(troca, 409, "application/json; charset=utf-8", "{\"erro\":\"A votação atual ainda está em andamento.\"}");
                return;
            }
            controlador = new ControladorUrna(eleicao, repositorio, Runnable::run, Runnable::run,
                    ignorado -> { }, () -> { });
            indiceCargo = 0;
            controlador.novaVotacao();
        } else {
            if (controlador == null) {
                responder(troca, 409, "application/json; charset=utf-8", "{\"erro\":\"Inicie uma votação primeiro.\"}");
                return;
            }
            switch (caminho) {
                case "/api/digit" -> {
                    String valor = parametro(troca.getRequestURI().getRawQuery(), "valor");
                    if (valor.length() != 1 || valor.charAt(0) < '0' || valor.charAt(0) > '9') {
                        throw new IllegalArgumentException("Envie um único dígito de 0 a 9.");
                    }
                    controlador.digitar(valor.charAt(0));
                }
                case "/api/white" -> controlador.branco();
                case "/api/correct" -> controlador.corrigir();
                case "/api/confirm" -> controlador.confirmar();
                default -> {
                    responder(troca, 404, "application/json; charset=utf-8", "{\"erro\":\"Ação não encontrada.\"}");
                    return;
                }
            }
            atualizarIndiceCargo(controlador.estado());
        }
        responder(troca, 200, "application/json; charset=utf-8", estadoJson());
    }

    private void atenderArquivo(HttpExchange troca, String caminho) throws IOException {
        if (!troca.getRequestMethod().equals("GET")) {
            responder(troca, 405, "text/plain; charset=utf-8", "Método não permitido.");
            return;
        }
        String arquivo = switch (caminho) {
            case "/", "/index.html" -> "index.html";
            case "/styles.css" -> "styles.css";
            case "/app.js" -> "app.js";
            default -> null;
        };
        if (arquivo == null) {
            responder(troca, 404, "text/plain; charset=utf-8", "Página não encontrada.");
            return;
        }
        Path origem = CaminhosAplicacao.diretorioDaAplicacao().resolve("web").resolve(arquivo);
        if (!Files.isRegularFile(origem)) {
            responder(troca, 500, "text/plain; charset=utf-8", "Arquivos da interface web não encontrados.");
            return;
        }
        String tipo = arquivo.endsWith(".css") ? "text/css; charset=utf-8"
                : arquivo.endsWith(".js") ? "text/javascript; charset=utf-8" : "text/html; charset=utf-8";
        troca.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        troca.getResponseHeaders().set("Content-Security-Policy", "default-src 'self'; style-src 'self'; script-src 'self'; connect-src 'self'; img-src 'self' data:");
        byte[] conteudo = Files.readAllBytes(origem);
        troca.getResponseHeaders().set("Content-Type", tipo);
        troca.sendResponseHeaders(200, conteudo.length);
        troca.getResponseBody().write(conteudo);
    }

    private String estadoJson() {
        if (controlador == null) {
            return "{\"iniciada\":false,\"persistencia\":" + json(repositorio.estadoPersistencia())
                    + ",\"mensagem\":" + json(repositorio.dicaPersistencia()) + "}";
        }
        EstadoTela estado = controlador.estado();
        StringBuilder json = new StringBuilder("{");
        campo(json, "iniciada", true).append(',');
        campo(json, "modo", estado.modo().name()).append(',');
        campo(json, "cargo", estado.cargo()).append(',');
        campo(json, "progresso", estado.progresso()).append(',');
        campo(json, "posicoes", estado.posicoes()).append(',');
        campo(json, "digitado", estado.digitado()).append(',');
        campo(json, "titulo", estado.titulo()).append(',');
        campo(json, "detalhe", estado.detalhe()).append(',');
        campo(json, "explicacao", estado.explicacao()).append(',');
        campo(json, "mensagem", estado.mensagem()).append(',');
        campo(json, "tipoMensagem", estado.tipoMensagem().name()).append(',');
        campo(json, "controlesHabilitados", estado.controlesHabilitados()).append(',');
        campo(json, "finalizada", estado.finalizada()).append(',');
        campo(json, "persistencia", repositorio.estadoPersistencia()).append(',');
        json.append("\"candidatos\":[");
        if (!estado.finalizada()) {
            List<Candidato> candidatos = eleicao.cargos().get(indiceCargo).candidatos();
            for (int i = 0; i < candidatos.size(); i++) {
                if (i > 0) json.append(',');
                candidatoJson(json, candidatos.get(i));
            }
        }
        json.append("]}");
        return json.toString();
    }

    private static void candidatoJson(StringBuilder json, Candidato candidato) {
        json.append('{');
        campo(json, "numero", candidato.numero()).append(',');
        campo(json, "nome", candidato.nome()).append(',');
        campo(json, "partido", candidato.partido()).append(',');
        campo(json, "vice", candidato.vice().orElse("")).append(',');
        json.append("\"propostas\":[");
        for (int i = 0; i < candidato.propostas().size(); i++) {
            if (i > 0) json.append(',');
            json.append(json(candidato.propostas().get(i)));
        }
        json.append("]}");
    }

    private void atualizarIndiceCargo(EstadoTela estado) {
        if (estado.finalizada()) return;
        String progresso = estado.progresso();
        int separador = progresso.indexOf(" DE ");
        if (progresso.startsWith("CARGO ") && separador > 6) {
            try {
                indiceCargo = Integer.parseInt(progresso.substring(6, separador)) - 1;
                indiceCargo = Math.max(0, Math.min(indiceCargo, eleicao.cargos().size() - 1));
            } catch (NumberFormatException ignorada) { }
        }
    }

    private boolean hostPermitido(HttpExchange troca) {
        String host = troca.getRequestHeaders().getFirst("Host");
        if (host == null || !(host.equalsIgnoreCase(HOST + ":" + port)
                || host.equalsIgnoreCase("localhost:" + port))) return false;
        String origem = troca.getRequestHeaders().getFirst("Origin");
        return origem == null || origem.equals("http://" + HOST + ":" + port)
                || origem.equals("http://localhost:" + port);
    }

    private static String parametro(String query, String nome) {
        if (query == null) return "";
        for (String parte : query.split("&")) {
            String[] par = parte.split("=", 2);
            if (par.length == 2 && URLDecoder.decode(par[0], StandardCharsets.UTF_8).equals(nome)) {
                return URLDecoder.decode(par[1], StandardCharsets.UTF_8);
            }
        }
        return "";
    }

    private static String modo(String[] args) {
        if (args.length == 0) return "padrao";
        if (args.length == 1 && (args[0].equals("--local") || args[0].equals("--memoria"))) return args[0];
        throw new IllegalArgumentException("Uso: scripts\\servir.bat [--local | --memoria]. A porta pode ser definida em URNA_WEB_PORT.");
    }

    private static StringBuilder campo(StringBuilder json, String nome, String valor) {
        return json.append(json(nome)).append(':').append(json(valor));
    }

    private static StringBuilder campo(StringBuilder json, String nome, int valor) {
        return json.append(json(nome)).append(':').append(valor);
    }

    private static StringBuilder campo(StringBuilder json, String nome, boolean valor) {
        return json.append(json(nome)).append(':').append(valor);
    }

    private static String json(String texto) {
        StringBuilder saida = new StringBuilder("\"");
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            switch (c) {
                case '"' -> saida.append("\\\"");
                case '\\' -> saida.append("\\\\");
                case '\b' -> saida.append("\\b");
                case '\f' -> saida.append("\\f");
                case '\n' -> saida.append("\\n");
                case '\r' -> saida.append("\\r");
                case '\t' -> saida.append("\\t");
                default -> {
                    if (c < 0x20) saida.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    else saida.append(c);
                }
            }
        }
        return saida.append('"').toString();
    }

    private static void responder(HttpExchange troca, int status, String tipo, String corpo) throws IOException {
        byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
        troca.getResponseHeaders().set("Content-Type", tipo);
        troca.getResponseHeaders().set("Cache-Control", "no-store");
        troca.sendResponseHeaders(status, bytes.length);
        troca.getResponseBody().write(bytes);
    }

    private static String mensagem(Exception e) {
        return e.getMessage() == null || e.getMessage().isBlank() ? "Falha inesperada." : e.getMessage();
    }

    private static void fechar(VotoRepository repositorio) {
        if (repositorio instanceof AutoCloseable recurso) {
            try { recurso.close(); } catch (Exception ignorada) { }
        }
    }
}
