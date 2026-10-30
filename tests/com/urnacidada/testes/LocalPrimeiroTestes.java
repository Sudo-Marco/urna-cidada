package com.urnacidada.testes;

import com.sun.net.httpserver.HttpServer;
import com.urnacidada.model.TipoVoto;
import com.urnacidada.model.Voto;
import com.urnacidada.repository.FirestoreRestVotoClient;
import com.urnacidada.repository.VotoRepositoryEmArquivo;
import com.urnacidada.repository.VotoRepositoryLocalPrimeiro;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static com.urnacidada.testes.MiniTeste.*;

final class LocalPrimeiroTestes {
    private LocalPrimeiroTestes() { }
    private static List<Voto> votos() { return List.of(
            new Voto("Deputado Federal",TipoVoto.VALIDO,"8101",Instant.parse("2026-10-29T12:00:00Z")),
            new Voto("Deputado Estadual",TipoVoto.LEGENDA,"81",Instant.parse("2026-10-29T12:00:01Z")),
            new Voto("Senador",TipoVoto.BRANCO,null,Instant.parse("2026-10-29T12:00:02Z")),
            new Voto("Governador",TipoVoto.NULO,"99",Instant.parse("2026-10-29T12:00:03Z")),
            new Voto("Presidente",TipoVoto.VALIDO,"82",Instant.parse("2026-10-29T12:00:04Z"))); }
    static void executar() {
        grupo("Persistência local-primeiro e reenvio em segundo plano");
        teste("sem configuração, salva no arquivo e informa modo local", () -> {
            Path p=Files.createTempDirectory("urna-pending").resolve("pendentes.tsv");
            try(var repo=new VotoRepositoryLocalPrimeiro(new VotoRepositoryEmArquivo(p,"local"),null,false,"Firebase não configurado")) {
                repo.salvarSessao("s1",votos()); igual(1,new VotoRepositoryEmArquivo(p,"local").lerSessoes().size());
                igual("Modo local",repo.estadoPersistencia()); verdade(repo.avisoDaUltimaGravacao().orElseThrow().contains("Firebase"));
            }
        });
        teste("offline seguido de online envia sessão e limpa arquivo, sem segunda criação", () -> {
            AtomicInteger chamadas=new AtomicInteger();
            HttpServer s=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
            s.createContext("/v1/",e->{ int num=chamadas.incrementAndGet(); e.getRequestBody().readAllBytes();
                e.sendResponseHeaders(num==1?503:200,-1); e.close(); }); s.start();
            Path p=Files.createTempDirectory("urna-offline-online").resolve("pendentes.tsv");
            FirestoreRestVotoClient c=new FirestoreRestVotoClient("projeto-ficticio","AIza"+"A".repeat(35),
                    URI.create("http://127.0.0.1:"+s.getAddress().getPort()+"/v1/"),Duration.ofMillis(200),Duration.ofSeconds(2));
            try(VotoRepositoryLocalPrimeiro repo=new VotoRepositoryLocalPrimeiro(new VotoRepositoryEmArquivo(p,"local"),c,true)) {
                repo.salvarSessao("123e4567-e89b-12d3-a456-426614174000",votos());
                long limite=System.currentTimeMillis()+4000;
                while(chamadas.get()<1 && System.currentTimeMillis()<limite) Thread.sleep(10);
                verdade(chamadas.get()>=1,"tentativa offline foi feita");
                long livre=System.currentTimeMillis()+1000; while(System.currentTimeMillis()<livre && repo.estadoPersistencia().startsWith("Firebase")) Thread.sleep(10);
                repo.sincronizarAgora();
                limite=System.currentTimeMillis()+4000;
                while(!repo.estadoPersistencia().equals("Online · tudo enviado") && System.currentTimeMillis()<limite) Thread.sleep(10);
                igual("Online · tudo enviado",repo.estadoPersistencia()); igual(2,chamadas.get());
                igual(0,new VotoRepositoryEmArquivo(p,"local").lerSessoes().size());
            } finally { s.stop(0); }
        });
        teste("ao abrir, envia pendências já existentes", () -> {
            AtomicInteger chamadas=new AtomicInteger();
            HttpServer s=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
            s.createContext("/v1/",e->{ chamadas.incrementAndGet(); e.getRequestBody().readAllBytes(); e.sendResponseHeaders(200,-1); e.close(); }); s.start();
            Path p=Files.createTempDirectory("urna-inicio-pendente").resolve("pendentes.tsv");
            VotoRepositoryEmArquivo arquivo=new VotoRepositoryEmArquivo(p,"local");
            arquivo.salvarSessao("123e4567-e89b-12d3-a456-426614174000",votos());
            FirestoreRestVotoClient cliente=new FirestoreRestVotoClient("projeto-ficticio","AIza"+"A".repeat(35),
                    URI.create("http://127.0.0.1:"+s.getAddress().getPort()+"/v1/"),Duration.ofMillis(200),Duration.ofSeconds(2));
            try(VotoRepositoryLocalPrimeiro repo=new VotoRepositoryLocalPrimeiro(arquivo,cliente,true)) {
                long limite=System.currentTimeMillis()+4000;
                while(!repo.estadoPersistencia().equals("Online · tudo enviado") && System.currentTimeMillis()<limite) Thread.sleep(10);
                igual("Online · tudo enviado",repo.estadoPersistencia()); igual(1,chamadas.get()); igual(0,arquivo.lerSessoes().size());
            } finally { s.stop(0); }
        });
        teste("recuo aumenta progressivamente até cinco minutos", () -> {
            igual(60L,VotoRepositoryLocalPrimeiro.proximoIntervalo(30));
            igual(120L,VotoRepositoryLocalPrimeiro.proximoIntervalo(60));
            igual(240L,VotoRepositoryLocalPrimeiro.proximoIntervalo(120));
            igual(300L,VotoRepositoryLocalPrimeiro.proximoIntervalo(240));
            igual(300L,VotoRepositoryLocalPrimeiro.proximoIntervalo(300));
        });
    }
}
