package com.urnacidada.repository;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Ações administrativas do Firestore, autorizadas pelo marcador controlado pelo responsável do projeto. */
public final class FirestoreAdminClient {
    private static final URI ENDPOINT = URI.create("https://firestore.googleapis.com/v1/");
    private static final Pattern DOCUMENT_NAME = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern NEXT_PAGE = Pattern.compile("\"nextPageToken\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ADMIN_ATIVO = Pattern.compile(
            "\"ativo\"\\s*:\\s*\\{\\s*\"booleanValue\"\\s*:\\s*true\\s*}");
    private static final int PAGE_SIZE = 500;
    private static final String DOCUMENTO_CONTROLE = "_controle";
    private static final List<ResultadoInicial> RESULTADOS_INICIAIS = List.of(
            new ResultadoInicial("Deputado Federal", "1736", "df_1736"),
            new ResultadoInicial("Deputado Federal", "5730", "df_5730"),
            new ResultadoInicial("Deputado Federal", "9462", "df_9462"),
            new ResultadoInicial("Governador", "14", "gov_14"),
            new ResultadoInicial("Governador", "28", "gov_28"),
            new ResultadoInicial("Governador", "37", "gov_37"),
            new ResultadoInicial("Presidente", "13", "pres_13"),
            new ResultadoInicial("Presidente", "27", "pres_27"),
            new ResultadoInicial("Presidente", "39", "pres_39"));

    private final String projectId;
    private final String apiKey;
    private final AutenticacaoFirebase.Sessao sessao;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public FirestoreAdminClient(String projectId, String apiKey, AutenticacaoFirebase.Sessao sessao) {
        this.projectId = projectId;
        this.apiKey = apiKey;
        this.sessao = sessao;
    }

    /** Só mostra os controles administrativos quando o próprio UID tem ativo=true no Firestore. */
    public boolean ehAdministrador() throws RepositorioException {
        HttpResponse<String> resposta = enviar("GET", documento("administradores", sessao.uid()), null);
        if (resposta.statusCode() == 404) return false;
        if (resposta.statusCode() == 401 || resposta.statusCode() == 403) return false;
        exigirSucesso(resposta, "Não foi possível consultar a permissão administrativa.");
        return ADMIN_ATIVO.matcher(resposta.body()).find();
    }

    /** Apaga os votos, zera a apuração e rearma os participantes sem excluir contas ou coleções. */
    public ResultadoLimpeza limparVotacao() throws RepositorioException {
        List<String> sessoes = listarIds("sessoes");
        List<String> resultados = listarIds("resultados");
        List<String> participantes = listarIds("participantes");
        int sessoesRemovidas = 0;
        int participantesRearmados = 0;
        try {
            for (String id : sessoes) {
                if (id.equals(DOCUMENTO_CONTROLE)) continue;
                apagarDocumento("sessoes", id);
                sessoesRemovidas++;
            }
            for (String id : resultados) apagarDocumento("resultados", id);
            for (ResultadoInicial resultado : RESULTADOS_INICIAIS) criarResultadoZero(resultado);
            for (String uid : participantes) {
                if (uid.equals(DOCUMENTO_CONTROLE)) continue;
                atualizarMarcador(uid);
                participantesRearmados++;
            }
            garantirDocumentoControle("sessoes", "{\"fields\":{\"tipo\":{\"stringValue\":\"CONTROLE\"}}}");
            garantirDocumentoControle("participantes", "{\"fields\":{\"votou\":{\"booleanValue\":false},\"tipo\":{\"stringValue\":\"CONTROLE\"}}}");
            return new ResultadoLimpeza(sessoesRemovidas, participantesRearmados);
        } catch (RepositorioException erro) {
            throw new RepositorioException("A limpeza da votação parou no Firestore. Os registros de administradores e participantes foram preservados. "
                    + "Publique as regras atualizadas e tente novamente.", erro);
        }
    }

    private void apagarDocumento(String colecao, String id) throws RepositorioException {
        HttpResponse<String> resposta = enviar("DELETE", documento(colecao, id), null);
        if (resposta.statusCode() == 404) return;
        exigirSucesso(resposta, "O Firestore não autorizou a remoção dos votos.");
    }

    private void criarResultadoZero(ResultadoInicial resultado) throws RepositorioException {
        String caminho = "projects/" + segmento(projectId)
                + "/databases/(default)/documents/resultados?documentId=" + segmento(resultado.id())
                + "&key=" + segmento(apiKey);
        String corpo = "{\"fields\":{\"cargo\":{\"stringValue\":" + aspas(resultado.cargo())
                + "},\"numero\":{\"stringValue\":" + aspas(resultado.numero())
                + "},\"contagem\":{\"integerValue\":\"0\"},\"ultimaSessaoId\":{\"nullValue\":null}}}";
        HttpResponse<String> resposta = enviar("POST", ENDPOINT.resolve(caminho), corpo);
        if (resposta.statusCode() == 409) {
            atualizarResultadoZero(resultado);
            return;
        }
        exigirSucesso(resposta, "Não foi possível preparar o contador " + resultado.id() + ".");
    }

    private void atualizarResultadoZero(ResultadoInicial resultado) throws RepositorioException {
        URI uri = ENDPOINT.resolve("projects/" + segmento(projectId)
                + "/databases/(default)/documents/resultados/" + segmento(resultado.id())
                + "?key=" + segmento(apiKey));
        String corpo = "{\"fields\":{\"cargo\":{\"stringValue\":" + aspas(resultado.cargo())
                + "},\"numero\":{\"stringValue\":" + aspas(resultado.numero())
                + "},\"contagem\":{\"integerValue\":\"0\"},\"ultimaSessaoId\":{\"nullValue\":null}}}";
        HttpResponse<String> resposta = enviar("PATCH", uri, corpo);
        exigirSucesso(resposta, "Não foi possível zerar o contador " + resultado.id() + ".");
    }

    private void atualizarMarcador(String uid) throws RepositorioException {
        URI uri = ENDPOINT.resolve("projects/" + segmento(projectId)
                + "/databases/(default)/documents/participantes/" + segmento(uid)
                + "?key=" + segmento(apiKey) + "&updateMask.fieldPaths=votou");
        HttpResponse<String> resposta = enviar("PATCH", uri,
                "{\"fields\":{\"votou\":{\"booleanValue\":false}}}");
        exigirSucesso(resposta, "Não foi possível rearmar um participante para a próxima votação.");
    }

    private void garantirDocumentoControle(String colecao, String corpo) throws RepositorioException {
        URI consulta = documento(colecao, DOCUMENTO_CONTROLE);
        HttpResponse<String> existente = enviar("GET", consulta, null);
        if (existente.statusCode() == 200) return;
        if (existente.statusCode() != 404) exigirSucesso(existente, "Não foi possível consultar a coleção " + colecao + ".");
        String caminho = "projects/" + segmento(projectId)
                + "/databases/(default)/documents/" + colecao
                + "?documentId=" + segmento(DOCUMENTO_CONTROLE) + "&key=" + segmento(apiKey);
        HttpResponse<String> resposta = enviar("POST", ENDPOINT.resolve(caminho), corpo);
        if (resposta.statusCode() == 409) return;
        exigirSucesso(resposta, "Não foi possível manter a coleção " + colecao + " visível no Firestore.");
    }

    private List<String> listarIds(String colecao) throws RepositorioException {
        List<String> ids = new ArrayList<>();
        String tokenPagina = "";
        do {
            String caminho = "projects/" + segmento(projectId)
                    + "/databases/(default)/documents/" + colecao
                    + "?pageSize=" + PAGE_SIZE + "&key=" + segmento(apiKey)
                    + (tokenPagina.isBlank() ? "" : "&pageToken=" + segmento(tokenPagina));
            HttpResponse<String> resposta = enviar("GET", ENDPOINT.resolve(caminho), null);
            exigirSucesso(resposta, "Não foi possível listar a coleção " + colecao + ".");

            Matcher nomes = DOCUMENT_NAME.matcher(resposta.body());
            String prefixo = "/documents/" + colecao + "/";
            while (nomes.find()) {
                String nome = nomes.group(1);
                int inicioId = nome.lastIndexOf(prefixo);
                if (inicioId >= 0) ids.add(nome.substring(inicioId + prefixo.length()));
            }
            Matcher proxima = NEXT_PAGE.matcher(resposta.body());
            tokenPagina = proxima.find() ? proxima.group(1) : "";
        } while (!tokenPagina.isBlank());
        return ids;
    }

    private URI documento(String colecao, String id) {
        return ENDPOINT.resolve("projects/" + segmento(projectId)
                + "/databases/(default)/documents/" + colecao + "/" + segmento(id)
                + "?key=" + segmento(apiKey));
    }

    private HttpResponse<String> enviar(String metodo, URI uri, String corpo) throws RepositorioException {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + sessao.token());
            if (corpo != null) builder.header("Content-Type", "application/json; charset=utf-8");
            if (corpo == null) builder.method(metodo, HttpRequest.BodyPublishers.noBody());
            else builder.method(metodo, HttpRequest.BodyPublishers.ofString(corpo, StandardCharsets.UTF_8));
            return http.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException erro) {
            Thread.currentThread().interrupt();
            throw new RepositorioException("A operação administrativa foi interrompida.", erro);
        } catch (IOException erro) {
            String detalhe = erro instanceof java.net.http.HttpTimeoutException || erro instanceof SocketTimeoutException
                    ? "Tempo esgotado ao acessar o Firestore."
                    : "Sem conexão com o Firestore.";
            throw new RepositorioException(detalhe, erro);
        }
    }

    private static void exigirSucesso(HttpResponse<String> resposta, String mensagem) throws RepositorioException {
        if (resposta.statusCode() >= 200 && resposta.statusCode() < 300) return;
        if (resposta.statusCode() == 401 || resposta.statusCode() == 403) {
            throw new RepositorioException(mensagem + " Publique as regras atualizadas e confirme que sua conta é administradora.", null);
        }
        throw new RepositorioException(mensagem + " (HTTP " + resposta.statusCode() + ").", null);
    }

    private static String segmento(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String aspas(String valor) {
        return "\"" + valor.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    public record ResultadoLimpeza(int sessoesRemovidas, int participantesRearmados) { }
    private record ResultadoInicial(String cargo, String numero, String id) { }
}
