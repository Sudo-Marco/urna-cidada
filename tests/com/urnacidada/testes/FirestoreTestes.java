package com.urnacidada.testes;

import com.sun.net.httpserver.HttpServer;
import com.urnacidada.model.TipoVoto;
import com.urnacidada.model.Voto;
import com.urnacidada.repository.AutenticacaoFirebase;
import com.urnacidada.repository.FalhaFirestore;
import com.urnacidada.repository.FirestoreRestVotoClient;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static com.urnacidada.testes.MiniTeste.*;

final class FirestoreTestes {
    private static final String UID = "uid-abc";
    private static final String SESSAO = "00000000-0000-4000-8000-000000000001";
    private FirestoreTestes() { }

    private static List<Voto> votos() {
        return List.of(
                new Voto("Deputado Federal", TipoVoto.VALIDO, "1736", Instant.parse("2026-10-29T12:00:00Z")),
                new Voto("Governador", TipoVoto.BRANCO, null, Instant.parse("2026-10-29T12:00:01Z")),
                new Voto("Presidente", TipoVoto.VALIDO, "13", Instant.parse("2026-10-29T12:00:02Z")));
    }

    private static HttpServer servidor(int statusCommit, String hashLido, AtomicInteger chamadas,
                                       AtomicInteger commits, StringBuilder corpos,
                                       StringBuilder autorizacoes, boolean atrasar) throws Exception {
        return servidor(statusCommit, hashLido, chamadas, commits, corpos, autorizacoes, atrasar, false);
    }

    private static HttpServer servidor(int statusCommit, String hashLido, AtomicInteger chamadas,
                                       AtomicInteger commits, StringBuilder corpos,
                                       StringBuilder autorizacoes, boolean atrasar,
                                       boolean participanteJaVotou) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/", exchange -> {
            chamadas.incrementAndGet();
            String metodo = exchange.getRequestMethod();
            String caminho = exchange.getRequestURI().getPath();
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            if (!body.isEmpty() && corpos != null) corpos.append(body);
            if (autorizacoes != null) autorizacoes.append(exchange.getRequestHeaders().getFirst("Authorization"));
            if (atrasar) try { Thread.sleep(500); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }

            int status;
            byte[] content = new byte[0];
            if (caminho.endsWith("/documents:commit")) {
                commits.incrementAndGet();
                status = statusCommit;
            } else if ("GET".equals(metodo) && caminho.contains("/documents/sessoes/")) {
                status = hashLido == null ? 404 : 200;
                if (hashLido != null) content = ("{\"fields\":{\"impressao\":{\"stringValue\":\"" + hashLido + "\"}}}")
                        .getBytes(StandardCharsets.UTF_8);
            } else if ("GET".equals(metodo) && caminho.contains("/documents/participantes/")) {
                status = participanteJaVotou ? 200 : 404;
                if (participanteJaVotou) content = "{\"fields\":{\"votou\":{\"booleanValue\":true}}}"
                        .getBytes(StandardCharsets.UTF_8);
            } else {
                status = statusCommit >= 400 && statusCommit != 409 ? statusCommit : 201;
            }
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(status, content.length);
            if (content.length > 0) exchange.getResponseBody().write(content);
            exchange.close();
        });
        server.start();
        return server;
    }

    private static AutenticacaoFirebase.Sessao usuarioTeste() throws Exception {
        HttpServer auth = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        auth.createContext("/v1/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            exchange.getRequestBody().readAllBytes();
            String body = "{\"localId\":\"" + UID + "\",\"idToken\":\"id-token\",\"refreshToken\":\"refresh-token\",\"expiresIn\":\"3600\"}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        auth.start();
        try {
            URI base = URI.create("http://127.0.0.1:" + auth.getAddress().getPort() + "/v1/");
            return new AutenticacaoFirebase("AIza" + "A".repeat(35), base, base)
                    .entrar("52998224725");
        } finally { auth.stop(0); }
    }

    private static FirestoreRestVotoClient cliente(HttpServer server, Duration timeout) throws Exception {
        return new FirestoreRestVotoClient("projeto-ficticio", "AIza" + "A".repeat(35),
                URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/"),
                Duration.ofMillis(300), timeout, usuarioTeste());
    }

    private static String impressao(List<Voto> votos) throws Exception {
        var metodoJson = FirestoreRestVotoClient.class.getDeclaredMethod("json", List.class);
        metodoJson.setAccessible(true);
        String json = (String) metodoJson.invoke(null, votos);
        byte[] bytes = MessageDigest.getInstance("SHA-256").digest(json.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(bytes);
    }

    static void executar() {
        grupo("Cliente REST autenticado do Firestore (servidor HTTP local falso)");

        teste("grava sessão e incrementa apenas dois candidatos em lote atômico autenticado", () -> {
            AtomicInteger chamadas = new AtomicInteger(), commits = new AtomicInteger();
            StringBuilder corpos = new StringBuilder(), autorizacoes = new StringBuilder();
            HttpServer server = servidor(200, null, chamadas, commits, corpos, autorizacoes, false);
            try {
                cliente(server, Duration.ofSeconds(2)).criarSessao(SESSAO, votos());
                igual(4, chamadas.get());
                igual(1, commits.get());
                verdade(corpos.toString().contains("df_1736") && corpos.toString().contains("pres_13"), "IDs dos candidatos no Firestore");
                verdade(corpos.toString().contains("increment") && corpos.toString().contains("\"integerValue\":\"1\""), "contador incrementado atomicamente");
                verdade(corpos.toString().contains("arrayValue") && corpos.toString().contains("nullValue"), "sessão usa tipos REST Firestore");
                verdade(corpos.toString().contains("BRANCO"), "voto branco guardado sem contar como candidato");
                verdade(corpos.toString().contains("documents/participantes/" + UID)
                        && corpos.toString().contains("\"votou\":{\"booleanValue\":true}"), "marca de participação separada criada no mesmo lote");
                verdade(!corpos.toString().contains("52998224725"), "CPF não acompanha os documentos de voto");
                verdade(corpos.toString().contains(SESSAO), "sessão usa UUID aleatório");
                verdade(autorizacoes.toString().contains("Bearer id-token"), "requisições enviam token Firebase");
            } finally { server.stop(0); }
        });

        teste("HTTP 409 no commit é idempotente quando a sessão existente tem os mesmos votos", () -> {
            AtomicInteger chamadas = new AtomicInteger(), commits = new AtomicInteger();
            HttpServer server = servidor(409, impressao(votos()), chamadas, commits, null, null, false);
            try {
                cliente(server, Duration.ofSeconds(2)).criarSessao(SESSAO, votos());
                igual(1, commits.get());
                igual(5, chamadas.get());
            } finally { server.stop(0); }
        });

        teste("marca de participante usada impede uma nova votação", () -> {
            AtomicInteger chamadas = new AtomicInteger(), commits = new AtomicInteger();
            HttpServer server = servidor(409, "f".repeat(64), chamadas, commits, null, null, false, true);
            try {
                FalhaFirestore falha = lanca(FalhaFirestore.class,
                        () -> cliente(server, Duration.ofSeconds(2)).criarSessao(SESSAO, votos()));
                igual(FalhaFirestore.Tipo.USUARIO_JA_VOTOU, falha.tipo());
                igual(0, commits.get());
            } finally { server.stop(0); }
        });

        teste("consulta de pré-voto detecta CPF já usado pela marca do participante", () -> {
            AtomicInteger chamadas = new AtomicInteger();
            HttpServer server = servidor(200, "f".repeat(64), chamadas, new AtomicInteger(), null, null, false, true);
            try { verdade(cliente(server, Duration.ofSeconds(2)).participanteJaVotou(), "documento já existe"); }
            finally { server.stop(0); }
        });

        teste("consulta de pré-voto libera conta sem documento", () -> {
            HttpServer server = servidor(200, null, new AtomicInteger(), new AtomicInteger(), null, null, false);
            try { verdade(!cliente(server, Duration.ofSeconds(2)).participanteJaVotou(), "documento ainda não existe"); }
            finally { server.stop(0); }
        });

        for (int status : new int[]{400, 401, 403, 404}) teste("HTTP " + status + " é erro de configuração", () -> {
            HttpServer server = servidor(status, null, new AtomicInteger(), new AtomicInteger(), null, null, false);
            try {
                FalhaFirestore falha = lanca(FalhaFirestore.class,
                        () -> cliente(server, Duration.ofSeconds(2)).criarSessao(SESSAO, votos()));
                igual(FalhaFirestore.Tipo.CONFIGURACAO, falha.tipo());
            } finally { server.stop(0); }
        });

        teste("HTTP 503 é transitório", () -> {
            HttpServer server = servidor(503, null, new AtomicInteger(), new AtomicInteger(), null, null, false);
            try { igual(FalhaFirestore.Tipo.TRANSITORIA, lanca(FalhaFirestore.class,
                    () -> cliente(server, Duration.ofSeconds(2)).criarSessao(SESSAO, votos())).tipo()); }
            finally { server.stop(0); }
        });

        teste("tempo esgotado é transitório", () -> {
            HttpServer server = servidor(200, null, new AtomicInteger(), new AtomicInteger(), null, null, true);
            try { igual(FalhaFirestore.Tipo.TRANSITORIA, lanca(FalhaFirestore.class,
                    () -> cliente(server, Duration.ofMillis(80)).criarSessao(SESSAO, votos())).tipo()); }
            finally { server.stop(0); }
        });

        teste("servidor indisponível mantém falha transitória", () -> {
            FirestoreRestVotoClient client = new FirestoreRestVotoClient("projeto-ficticio", "AIza" + "A".repeat(35),
                    URI.create("http://127.0.0.1:1/v1/"), Duration.ofMillis(100), Duration.ofMillis(150), usuarioTeste());
            igual(FalhaFirestore.Tipo.TRANSITORIA,
                    lanca(FalhaFirestore.class, () -> client.criarSessao(SESSAO, votos())).tipo());
        });
    }
}
