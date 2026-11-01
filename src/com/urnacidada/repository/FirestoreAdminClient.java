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

    /** Remove sessões, contadores e marcas de participação. Mantém os registros de administradores. */
    public int apagarDadosVotacao() throws RepositorioException {
        List<Documento> documentos = new ArrayList<>();
        for (String colecao : List.of("sessoes", "resultados", "participantes")) {
            for (String id : listarIds(colecao)) documentos.add(new Documento(colecao, id));
        }

        int removidos = 0;
        try {
            for (Documento documento : documentos) {
                HttpResponse<String> resposta = enviar("DELETE", documento(documento.colecao(), documento.id()), null);
                if (resposta.statusCode() == 404) continue;
                exigirSucesso(resposta, "O Firestore não autorizou a remoção dos dados.");
                removidos++;
            }
            return removidos;
        } catch (RepositorioException erro) {
            throw new RepositorioException("A limpeza do Firestore parou após remover " + removidos
                    + " documentos. Confira as regras e a conexão antes de tentar novamente.", erro);
        }
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

    private record Documento(String colecao, String id) { }
}
