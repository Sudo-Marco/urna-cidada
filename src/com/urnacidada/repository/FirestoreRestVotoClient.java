package com.urnacidada.repository;

import com.urnacidada.LogUrna;
import com.urnacidada.model.Voto;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Cliente mínimo da API REST do Firestore, implementado apenas com APIs do JDK. */
public final class FirestoreRestVotoClient {
    private static final URI ENDPOINT = URI.create("https://firestore.googleapis.com/v1/");
    private final String projectId;
    private final String apiKey;
    private final URI endpointBase;
    private final HttpClient http;
    private final Duration timeoutRequisicao;
    private final AutenticacaoFirebase.Sessao autenticacao;
    private static final Pattern IMPRESSAO_EXISTENTE = Pattern.compile(
            "\\\"impressao\\\"\\s*:\\s*\\{\\s*\\\"stringValue\\\"\\s*:\\s*\\\"([0-9a-f]{64})\\\"");
    private static final Pattern MARCADOR_VOTO = Pattern.compile(
            "\\\"votou\\\"\\s*:\\s*\\{\\s*\\\"booleanValue\\\"\\s*:\\s*(true|false)\\s*}");

    public FirestoreRestVotoClient(String projectId, String apiKey) {
        this(projectId, apiKey, ENDPOINT, Duration.ofSeconds(5), Duration.ofSeconds(10), null);
    }

    public FirestoreRestVotoClient(String projectId, String apiKey, AutenticacaoFirebase.Sessao autenticacao) {
        this(projectId, apiKey, ENDPOINT, Duration.ofSeconds(5), Duration.ofSeconds(10), autenticacao);
    }

    /** Construtor testável com servidor local falso e tempos menores para os testes. */
    public FirestoreRestVotoClient(String projectId, String apiKey, URI endpointBase,
                                   Duration timeoutConexao, Duration timeoutRequisicao) {
        this(projectId, apiKey, endpointBase, timeoutConexao, timeoutRequisicao, null);
    }

    public FirestoreRestVotoClient(String projectId, String apiKey, URI endpointBase,
                                   Duration timeoutConexao, Duration timeoutRequisicao,
                                   AutenticacaoFirebase.Sessao autenticacao) {
        this.projectId = projectId;
        this.apiKey = apiKey;
        this.endpointBase = endpointBase;
        this.timeoutRequisicao = timeoutRequisicao;
        this.autenticacao = autenticacao;
        this.http = HttpClient.newBuilder().connectTimeout(timeoutConexao).build();
    }

    /** Cria uma sessão pelo ID da votação; 409 significa que a sessão já existe e é idempotente. */
    public void criarSessao(String sessaoId, List<Voto> votos) throws RepositorioException {
        criarSessao(sessaoId, votos, timeoutRequisicao);
    }

    public void criarSessao(String sessaoId, List<Voto> votos, Duration timeout) throws RepositorioException {
        validarIdSessao(sessaoId);
        if (autenticacao == null) {
            throw new FalhaFirestore(FalhaFirestore.Tipo.CONFIGURACAO,
                    "A sincronização com Firestore exige identificação do participante.", null);
        }
        MarcadorParticipante marcador = lerMarcadorParticipante(timeout);
        if (marcador.votou()) {
            throw new FalhaFirestore(FalhaFirestore.Tipo.USUARIO_JA_VOTOU,
                    "Este CPF já concluiu uma votação. Cada CPF cadastrado pode votar uma vez.", null);
        }
        String fingerprint = impressao(votos);
        for (Voto voto : votos) {
            if (voto.tipo() == com.urnacidada.model.TipoVoto.VALIDO) {
                garantirResultado(voto.cargo(), voto.numero(), timeout);
            }
        }
        String caminho = "projects/" + segmento(projectId)
                + "/databases/(default)/documents:commit?key="
                + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
        URI uri = endpointBase.resolve(caminho);
        HttpRequest requisicao = construir(uri, timeout)
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(
                        jsonCommit(sessaoId, votos, fingerprint, marcador.existe()), StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> resposta = http.send(requisicao, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = resposta.statusCode();
            if (status >= 200 && status < 300) return;
            if (status == 409) {
                if (sessaoExistenteIgual(sessaoId, fingerprint, timeout)) return;
                if (participanteJaVotou(timeout)) {
                    throw new FalhaFirestore(FalhaFirestore.Tipo.USUARIO_JA_VOTOU,
                            "Este CPF já concluiu uma votação. Cada CPF cadastrado pode votar uma vez.", null);
                }
                throw new FalhaFirestore(FalhaFirestore.Tipo.CONFIGURACAO,
                        "O Firestore informou conflito ao gravar a votação.", null);
            }
            if (status == 400 || status == 401 || status == 403 || status == 404) {
                LogUrna.logger().warning("Firestore rejeitou a configuração ou os dados (HTTP " + status + ").");
                throw new FalhaFirestore(FalhaFirestore.Tipo.CONFIGURACAO,
                        status == 403
                                ? "Conta não autorizada ou regras do Firebase desatualizadas. Confira Authentication e Firestore."
                                : "Configuração do Firebase inválida. Confira o projeto, a chave de API e as regras.", null);
            }
            if (status == 408 || status == 429 || status >= 500) {
                throw new FalhaFirestore(FalhaFirestore.Tipo.TRANSITORIA,
                        "Firebase temporariamente indisponível (HTTP " + status + ").", null);
            }
            throw new FalhaFirestore(FalhaFirestore.Tipo.CONFIGURACAO,
                    "Firebase recusou a solicitação (HTTP " + status + ").", null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FalhaFirestore(FalhaFirestore.Tipo.TRANSITORIA,
                    "Envio interrompido; a votação continua guardada localmente.", e);
        } catch (IOException e) {
            LogUrna.logger().info("Falha de rede ao enviar uma sessão; ela permanece local ("
                    + e.getClass().getSimpleName() + ").");
            throw new FalhaFirestore(FalhaFirestore.Tipo.TRANSITORIA,
                    e instanceof java.net.http.HttpTimeoutException || e instanceof SocketTimeoutException
                            ? "Tempo esgotado ao conectar ao Firebase. A votação continua guardada localmente."
                            : "Sem conexão com o Firebase. A votação continua guardada localmente.", e);
        }
    }

    private void garantirResultado(String cargo, String numero, Duration timeout) throws RepositorioException {
        String id = idResultado(cargo, numero);
        String caminho = "projects/" + segmento(projectId) + "/databases/(default)/documents/resultados?documentId="
                + segmento(id) + "&key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
        URI uri = endpointBase.resolve(caminho);
        HttpRequest request = construir(uri, timeout)
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(jsonResultadoInicial(cargo, numero), StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<Void> response = http.send(request, HttpResponse.BodyHandlers.discarding());
            int status = response.statusCode();
            if ((status >= 200 && status < 300) || status == 409) return;
            if (status == 408 || status == 429 || status >= 500) {
                throw new FalhaFirestore(FalhaFirestore.Tipo.TRANSITORIA,
                        "Firebase temporariamente indisponível (HTTP " + status + ").", null);
            }
            throw new FalhaFirestore(FalhaFirestore.Tipo.CONFIGURACAO,
                    "Não foi possível preparar a contagem de votos no Firebase (HTTP " + status + ").", null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FalhaFirestore(FalhaFirestore.Tipo.TRANSITORIA,
                    "Preparação do Firebase interrompida; a sessão continua guardada localmente.", e);
        } catch (IOException e) {
            throw new FalhaFirestore(FalhaFirestore.Tipo.TRANSITORIA,
                    "Sem conexão com o Firebase. A sessão continua guardada localmente.", e);
        }
    }

    private boolean sessaoExistenteIgual(String sessaoId, String fingerprint, Duration timeout)
            throws RepositorioException {
        String caminho = "projects/" + segmento(projectId) + "/databases/(default)/documents/sessoes/"
                + segmento(sessaoId) + "?key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
        HttpRequest request;
        try {
            request = construir(endpointBase.resolve(caminho), timeout).GET().build();
        } catch (RepositorioException e) {
            throw new FalhaFirestore(FalhaFirestore.Tipo.TRANSITORIA,
                    "Sem conexão para verificar o participante no Firestore.", e);
        }
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) return false;
            Matcher matcher = IMPRESSAO_EXISTENTE.matcher(response.body());
            return matcher.find() && fingerprint.equals(matcher.group(1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FalhaFirestore(FalhaFirestore.Tipo.TRANSITORIA,
                    "Não foi possível confirmar uma tentativa anterior no Firebase.", e);
        } catch (IOException e) {
            throw new FalhaFirestore(FalhaFirestore.Tipo.TRANSITORIA,
                    "Não foi possível confirmar uma tentativa anterior no Firebase.", e);
        }
    }

    /** Consulta a marca individual, sem relacioná-la aos documentos anônimos de voto. */
    public boolean participanteJaVotou() throws RepositorioException {
        return participanteJaVotou(timeoutRequisicao);
    }

    private boolean participanteJaVotou(Duration timeout) throws RepositorioException {
        return lerMarcadorParticipante(timeout).votou();
    }

    private MarcadorParticipante lerMarcadorParticipante(Duration timeout) throws RepositorioException {
        if (autenticacao == null) throw new FalhaFirestore(FalhaFirestore.Tipo.CONFIGURACAO,
                "A consulta exige identificação do participante.", null);
        String caminho = "projects/" + segmento(projectId) + "/databases/(default)/documents/participantes/"
                + segmento(autenticacao.uid()) + "?key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
        HttpRequest request = construir(endpointBase.resolve(caminho), timeout).GET().build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 200) {
                Matcher marcador = MARCADOR_VOTO.matcher(response.body());
                if (!marcador.find()) throw new FalhaFirestore(FalhaFirestore.Tipo.CONFIGURACAO,
                        "O registro do participante está em formato inválido no Firestore.", null);
                return new MarcadorParticipante(true, Boolean.parseBoolean(marcador.group(1)));
            }
            if (response.statusCode() == 404) return new MarcadorParticipante(false, false);
            if (response.statusCode() == 408 || response.statusCode() == 429 || response.statusCode() >= 500) {
                throw new FalhaFirestore(FalhaFirestore.Tipo.TRANSITORIA,
                        "Não foi possível verificar se o CPF já votou. Confira a conexão e tente novamente.", null);
            }
            throw new FalhaFirestore(FalhaFirestore.Tipo.CONFIGURACAO,
                    "O Firestore não autorizou a verificação do participante (HTTP " + response.statusCode() + ").", null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FalhaFirestore(FalhaFirestore.Tipo.TRANSITORIA, "Verificação interrompida.", e);
        } catch (IOException e) {
            throw new FalhaFirestore(FalhaFirestore.Tipo.TRANSITORIA,
                    "Sem conexão para verificar o participante no Firestore.", e);
        }
    }

    private HttpRequest.Builder construir(URI uri, Duration timeout) throws RepositorioException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri).timeout(timeout);
        if (autenticacao != null) builder.header("Authorization", "Bearer " + autenticacao.token());
        return builder;
    }

    private static String jsonResultadoInicial(String cargo, String numero) {
        return "{\"fields\":{\"cargo\":{\"stringValue\":" + aspas(cargo)
                + "},\"numero\":{\"stringValue\":" + aspas(numero)
                + "},\"contagem\":{\"integerValue\":\"0\"},\"ultimaSessaoId\":{\"nullValue\":null}}}";
    }

    private String jsonCommit(String sessaoId, List<Voto> votos, String fingerprint, boolean marcadorExiste) {
        String participante = "projects/" + segmento(projectId) + "/databases/(default)/documents/participantes/"
                + segmento(autenticacao.uid());
        String sessao = "projects/" + segmento(projectId) + "/databases/(default)/documents/sessoes/"
                + segmento(sessaoId);
        StringBuilder body = new StringBuilder("{\"writes\":[{\"update\":{\"name\":")
                .append(aspas(participante)).append(",\"fields\":{\"votou\":{\"booleanValue\":true}}},")
                .append("\"currentDocument\":{\"exists\":").append(marcadorExiste)
                .append("}},{\"update\":{\"name\":")
                .append(aspas(sessao)).append(",\"fields\":{\"impressao\":{\"stringValue\":")
                .append(aspas(fingerprint)).append("},\"votos\":")
                .append(valorVotos(votos)).append("}},\"currentDocument\":{\"exists\":false}}");
        for (Voto voto : votos) {
            if (voto.tipo() != com.urnacidada.model.TipoVoto.VALIDO) continue;
            String resultado = "projects/" + segmento(projectId) + "/databases/(default)/documents/resultados/"
                    + segmento(idResultado(voto.cargo(), voto.numero()));
            body.append(", {\"update\":{\"name\":").append(aspas(resultado)).append(",\"fields\":{\"cargo\":{\"stringValue\":")
                    .append(aspas(voto.cargo())).append("},\"numero\":{\"stringValue\":").append(aspas(voto.numero()))
                    .append("},\"ultimaSessaoId\":{\"stringValue\":").append(aspas(sessaoId))
                    .append("}}},\"updateMask\":{\"fieldPaths\":[\"cargo\",\"numero\",\"ultimaSessaoId\"]},")
                    .append("\"updateTransforms\":[{\"fieldPath\":\"contagem\",\"increment\":{\"integerValue\":\"1\"}}],")
                    .append("\"currentDocument\":{\"exists\":true}}");
        }
        return body.append("]}").toString();
    }

    private static String idResultado(String cargo, String numero) {
        String chave = switch (cargo.strip().toLowerCase(java.util.Locale.ROOT)) {
            case "deputado federal" -> "df";
            case "governador" -> "gov";
            case "presidente" -> "pres";
            default -> throw new IllegalArgumentException("Cargo sem código de apuração: " + cargo);
        };
        return chave + "_" + numero;
    }

    private record MarcadorParticipante(boolean existe, boolean votou) { }

    private static String impressao(List<Voto> votos) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(json(votos).getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException impossivel) {
            throw new IllegalStateException("SHA-256 não está disponível.", impossivel);
        }
    }

    static String json(List<Voto> votos) {
        return "{\"fields\":{\"votos\":" + valorVotos(votos) + "}}";
    }

    private static String valorVotos(List<Voto> votos) {
        StringBuilder json = new StringBuilder("{\"arrayValue\":{\"values\":[");
        for (int i = 0; i < votos.size(); i++) {
            if (i > 0) json.append(',');
            Voto voto = votos.get(i);
            json.append("{\"mapValue\":{\"fields\":{")
                    .append("\"cargo\":{\"stringValue\":").append(aspas(voto.cargo())).append("},")
                    .append("\"tipo\":{\"stringValue\":").append(aspas(voto.tipo().name())).append("},")
                    .append("\"numero\":");
            if (voto.numero() == null) json.append("{\"nullValue\":null}");
            else json.append("{\"stringValue\":").append(aspas(voto.numero())).append('}');
            json.append(",\"dataHora\":{\"stringValue\":")
                    .append(aspas(voto.dataHora().toString())).append("}}}}");
        }
        return json.append("]}}").toString();
    }

    private static String aspas(String valor) {
        StringBuilder saida = new StringBuilder("\"");
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            switch (c) {
                case '"' -> saida.append("\\\"");
                case '\\' -> saida.append("\\\\");
                case '\b' -> saida.append("\\b");
                case '\f' -> saida.append("\\f");
                case '\n' -> saida.append("\\n");
                case '\r' -> saida.append("\\r");
                case '\t' -> saida.append("\\t");
                default -> {
                    if (c < 0x20) saida.append(String.format("\\u%04x", (int) c));
                    else saida.append(c);
                }
            }
        }
        return saida.append('"').toString();
    }

    private static String segmento(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static void validarIdSessao(String sessaoId) throws RepositorioException {
        try {
            if (!UUID.fromString(sessaoId).toString().equalsIgnoreCase(sessaoId)) throw new IllegalArgumentException();
        } catch (RuntimeException erro) {
            throw new RepositorioException("O identificador da votação precisa ser um UUID aleatório.", erro);
        }
    }
}
