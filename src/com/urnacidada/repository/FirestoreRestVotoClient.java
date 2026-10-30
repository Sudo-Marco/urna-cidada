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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/** Cliente mínimo da API REST do Firestore, implementado apenas com APIs do JDK. */
public final class FirestoreRestVotoClient {
    private static final URI ENDPOINT = URI.create("https://firestore.googleapis.com/v1/");
    private final String projectId;
    private final String apiKey;
    private final URI endpointBase;
    private final HttpClient http;
    private final Duration timeoutRequisicao;

    public FirestoreRestVotoClient(String projectId, String apiKey) {
        this(projectId, apiKey, ENDPOINT, Duration.ofSeconds(5), Duration.ofSeconds(10));
    }

    /** Construtor testável com servidor local falso e tempos menores para os testes. */
    public FirestoreRestVotoClient(String projectId, String apiKey, URI endpointBase,
                                   Duration timeoutConexao, Duration timeoutRequisicao) {
        this.projectId = projectId;
        this.apiKey = apiKey;
        this.endpointBase = endpointBase;
        this.timeoutRequisicao = timeoutRequisicao;
        this.http = HttpClient.newBuilder().connectTimeout(timeoutConexao).build();
    }

    /** Cria uma sessão pelo ID da votação; 409 significa que a sessão já existe e é idempotente. */
    public void criarSessao(String sessaoId, List<Voto> votos) throws RepositorioException {
        criarSessao(sessaoId, votos, timeoutRequisicao);
    }

    public void criarSessao(String sessaoId, List<Voto> votos, Duration timeout) throws RepositorioException {
        String caminho = "projects/" + segmento(projectId)
                + "/databases/(default)/documents/sessoes?documentId=" + segmento(sessaoId);
        String chave = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
        URI uri = endpointBase.resolve(caminho + "&key=" + chave);
        HttpRequest requisicao = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(json(votos), StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<Void> resposta = http.send(requisicao, HttpResponse.BodyHandlers.discarding());
            int status = resposta.statusCode();
            if ((status >= 200 && status < 300) || status == 409) return;
            if (status == 400 || status == 401 || status == 403 || status == 404) {
                LogUrna.logger().warning("Firestore rejeitou a configuração ou os dados (HTTP " + status + ").");
                throw new FalhaFirestore(FalhaFirestore.Tipo.CONFIGURACAO,
                        "Configuração do Firebase inválida. Confira o projeto, a chave de API e as regras.", null);
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

    static String json(List<Voto> votos) {
        StringBuilder json = new StringBuilder("{\"fields\":{\"votos\":{\"arrayValue\":{\"values\":[");
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
        return json.append("]}}}}").toString();
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
}
