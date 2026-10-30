package com.urnacidada.testes;

import com.sun.net.httpserver.HttpServer;
import com.urnacidada.model.Eleicao;
import com.urnacidada.model.TipoVoto;
import com.urnacidada.model.Voto;
import com.urnacidada.repository.FalhaFirestore;
import com.urnacidada.repository.FirestoreRestVotoClient;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static com.urnacidada.testes.MiniTeste.*;

final class FirestoreTestes {
    private FirestoreTestes() { }
    private static List<Voto> votos() { return List.of(
            new Voto("Deputado Federal", TipoVoto.VALIDO, "8101", Instant.parse("2026-10-29T12:00:00Z")),
            new Voto("Deputado Estadual", TipoVoto.LEGENDA, "81", Instant.parse("2026-10-29T12:00:01Z")),
            new Voto("Senador", TipoVoto.BRANCO, null, Instant.parse("2026-10-29T12:00:02Z")),
            new Voto("Governador", TipoVoto.NULO, "99", Instant.parse("2026-10-29T12:00:03Z")),
            new Voto("Presidente", TipoVoto.VALIDO, "82", Instant.parse("2026-10-29T12:00:04Z"))); }
    private static HttpServer servidor(int status, AtomicInteger chamadas, StringBuilder corpo,
                                      boolean atrasar) throws Exception {
        HttpServer s=HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        s.createContext("/v1/", exchange -> {
            chamadas.incrementAndGet(); if (corpo != null) corpo.append(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            if (atrasar) try { Thread.sleep(500); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            exchange.sendResponseHeaders(status, -1); exchange.close();
        }); s.start(); return s;
    }
    private static FirestoreRestVotoClient cliente(HttpServer s, Duration timeout) {
        return new FirestoreRestVotoClient("projeto-ficticio", "AIza"+"A".repeat(35),
                URI.create("http://127.0.0.1:"+s.getAddress().getPort()+"/v1/"), Duration.ofMillis(300), timeout);
    }
    static void executar() {
        grupo("Cliente REST do Firestore (servidor HTTP local falso)");
        for (int status : new int[]{200, 409}) teste("HTTP "+status+" é aceito como sucesso idempotente", () -> {
            AtomicInteger n=new AtomicInteger(); StringBuilder corpo=new StringBuilder(); HttpServer s=servidor(status,n,corpo,false);
            try { cliente(s,Duration.ofSeconds(2)).criarSessao("123e4567-e89b-12d3-a456-426614174000",votos()); igual(1,n.get());
                verdade(corpo.toString().contains("stringValue") && corpo.toString().contains("arrayValue") && corpo.toString().contains("nullValue"), "corpo usa tipos REST Firestore");
                verdade(corpo.toString().contains("LEGENDA"), "tipo de voto de legenda enviado");
            } finally { s.stop(0); }
        });
        for (int status : new int[]{400,401,403,404}) teste("HTTP "+status+" é classificado como erro de configuração", () -> {
            HttpServer s=servidor(status,new AtomicInteger(),null,false);
            try { FalhaFirestore f=lanca(FalhaFirestore.class, () -> cliente(s,Duration.ofSeconds(2)).criarSessao("sessao",votos())); igual(FalhaFirestore.Tipo.CONFIGURACAO,f.tipo()); }
            finally { s.stop(0); }
        });
        teste("HTTP 503 é transitório", () -> { HttpServer s=servidor(503,new AtomicInteger(),null,false);
            try { igual(FalhaFirestore.Tipo.TRANSITORIA,lanca(FalhaFirestore.class,()->cliente(s,Duration.ofSeconds(2)).criarSessao("s",votos())).tipo()); } finally { s.stop(0); }});
        teste("tempo esgotado é transitório", () -> { HttpServer s=servidor(200,new AtomicInteger(),null,true);
            try { igual(FalhaFirestore.Tipo.TRANSITORIA,lanca(FalhaFirestore.class,()->cliente(s,Duration.ofMillis(80)).criarSessao("s",votos())).tipo()); } finally { s.stop(0); }});
        teste("servidor indisponível mantém falha transitória", () -> {
            FirestoreRestVotoClient c=new FirestoreRestVotoClient("projeto-ficticio","AIza"+"A".repeat(35),URI.create("http://127.0.0.1:1/v1/"),Duration.ofMillis(100),Duration.ofMillis(150));
            igual(FalhaFirestore.Tipo.TRANSITORIA,lanca(FalhaFirestore.class,()->c.criarSessao("s",votos())).tipo());
        });
    }
}
