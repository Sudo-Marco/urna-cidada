package com.urnacidada.repository;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Firebase Authentication REST: CPF como identificador de conta e renovação de token. */
public final class AutenticacaoFirebase {
    private static final URI AUTH_BASE = URI.create("https://identitytoolkit.googleapis.com/v1/");
    private static final URI TOKEN_BASE = URI.create("https://securetoken.googleapis.com/v1/");
    private static final Pattern CAMPO = Pattern.compile("\"%s\"\\s*:\\s*\"([^\"]*)\"");

    private final String apiKey;
    private final URI authBase;
    private final URI tokenBase;
    private final HttpClient http;
    private final Duration timeout = Duration.ofSeconds(12);

    public AutenticacaoFirebase(String apiKey) {
        this(apiKey, AUTH_BASE, TOKEN_BASE);
    }

    /** Construtor testável com servidor REST local. */
    public AutenticacaoFirebase(String apiKey, URI authBase, URI tokenBase) {
        this.apiKey = apiKey;
        this.authBase = authBase;
        this.tokenBase = tokenBase;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    /** Registra uma conta técnica derivada do CPF; não pede senha ao participante. */
    public Sessao cadastrar(String cpf) throws RepositorioException {
        String resposta = postAuth("accounts:signUp", "{\"email\":" + aspas(CpfIdentificador.emailFirebase(cpf))
                + ",\"password\":" + aspas(CpfIdentificador.senhaTecnicaFirebase(cpf))
                + ",\"returnSecureToken\":true}");
        return criarSessao(resposta);
    }

    /** Inicia sessão usando as credenciais técnicas derivadas do CPF validado. */
    public Sessao entrar(String cpf) throws RepositorioException {
        String resposta = postAuth("accounts:signInWithPassword", "{\"email\":" + aspas(CpfIdentificador.emailFirebase(cpf))
                + ",\"password\":" + aspas(CpfIdentificador.senhaTecnicaFirebase(cpf))
                + ",\"returnSecureToken\":true}");
        return criarSessao(resposta);
    }

    private Sessao criarSessao(String resposta) throws RepositorioException {
        String idToken = campo(resposta, "idToken");
        String refreshToken = campo(resposta, "refreshToken");
        String uid = campo(resposta, "localId");
        if (idToken.isBlank() || refreshToken.isBlank() || uid.isBlank()) {
            throw new RepositorioException("Firebase não retornou os dados da conta.", null);
        }
        long segundos = parseLong(campo(resposta, "expiresIn"), 3600);
        return new Sessao(uid, idToken, refreshToken,
                Instant.now().plusSeconds(Math.max(60, segundos - 60)));
    }

    private synchronized Tokens renovar(String refreshToken) throws RepositorioException {
        String body = "grant_type=refresh_token&refresh_token="
                + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(uri(tokenBase, "token?key="
                        + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)))
                .timeout(timeout).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
        String response = enviar(request);
        String token = campo(response, "id_token");
        if (token.isBlank()) throw new RepositorioException("Firebase não renovou a sessão. Entre novamente.", null);
        return new Tokens(token, campo(response, "refresh_token"), parseLong(campo(response, "expires_in"), 3600));
    }

    private String postAuth(String endpoint, String json) throws RepositorioException {
        HttpRequest request = HttpRequest.newBuilder(uri(authBase, endpoint + "?key="
                        + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)))
                .timeout(timeout).header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)).build();
        return enviar(request);
    }

    private String enviar(HttpRequest request) throws RepositorioException {
        try {
            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 200 && response.statusCode() < 300) return response.body();
            String erro = campo(response.body(), "message");
            throw new RepositorioException(mensagemErro(erro, response.statusCode()), null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RepositorioException("Autenticação interrompida. Tente novamente.", e);
        } catch (IOException e) {
            throw new RepositorioException("Sem conexão com Firebase Authentication. Confira a internet e tente novamente.", e);
        }
    }

    private static String mensagemErro(String erro, int status) {
        if (erro.contains("EMAIL_EXISTS")) return "Este CPF já tem cadastro. Escolha Entrar.";
        if (erro.contains("EMAIL_NOT_FOUND")) return "Este CPF ainda não tem cadastro. Escolha Primeiro acesso.";
        if (erro.contains("INVALID_LOGIN_CREDENTIALS") || erro.contains("INVALID_PASSWORD")) {
            return "Não foi possível entrar com este CPF. Se ele foi cadastrado em uma versão anterior, a conta precisa ser migrada.";
        }
        if (erro.contains("WEAK_PASSWORD")) return "Não foi possível criar a credencial técnica do CPF.";
        if (erro.contains("OPERATION_NOT_ALLOWED")) return "Ative o provedor Email/Senha em Firebase Authentication.";
        if (erro.contains("TOO_MANY_ATTEMPTS_TRY_LATER")) return "Muitas tentativas. Aguarde um pouco antes de tentar novamente.";
        if (erro.contains("API_KEY_INVALID") || status == 403) return "Configuração de Firebase Authentication inválida.";
        return "Firebase Authentication recusou a solicitação (HTTP " + status + ").";
    }

    private static URI uri(URI base, String path) { return base.resolve("./" + path); }

    private static String campo(String json, String nome) {
        Matcher matcher = Pattern.compile(CAMPO.pattern().formatted(Pattern.quote(nome))).matcher(json);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static long parseLong(String valor, long padrao) {
        try { return Long.parseLong(valor); } catch (NumberFormatException ignorada) { return padrao; }
    }

    private record Tokens(String idToken, String refreshToken, long expiresIn) { }

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
                default -> { if (c < 0x20) saida.append(String.format("\\u%04x", (int) c)); else saida.append(c); }
            }
        }
        return saida.append('"').toString();
    }

    /** Credenciais em memória; o refresh token não é gravado no disco nem no log. */
    public final class Sessao {
        private final String uid;
        private String idToken;
        private String refreshToken;
        private Instant expiraEm;

        private Sessao(String uid, String idToken, String refreshToken, Instant expiraEm) {
            this.uid = uid;
            this.idToken = idToken;
            this.refreshToken = refreshToken;
            this.expiraEm = expiraEm;
        }

        public String uid() { return uid; }

        public synchronized String token() throws RepositorioException {
            if (Instant.now().isAfter(expiraEm)) {
                Tokens atualizados = renovar(refreshToken);
                idToken = atualizados.idToken();
                if (!atualizados.refreshToken().isBlank()) refreshToken = atualizados.refreshToken();
                expiraEm = Instant.now().plusSeconds(Math.max(60, atualizados.expiresIn() - 60));
            }
            return idToken;
        }
    }
}
