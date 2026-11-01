package com.urnacidada.testes;

import com.urnacidada.model.TipoVoto;
import com.urnacidada.model.Voto;
import com.urnacidada.repository.RepositorioException;
import com.urnacidada.repository.UsuarioJaVotouException;
import com.urnacidada.repository.VotoRepository;
import com.urnacidada.repository.VotoRepositoryComReserva;
import com.urnacidada.repository.VotoRepositoryEmArquivo;
import com.urnacidada.repository.VotoRepositoryEmArquivo.SessaoSalva;
import com.urnacidada.repository.VotoRepositoryEmMemoria;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.urnacidada.testes.MiniTeste.grupo;
import static com.urnacidada.testes.MiniTeste.igual;
import static com.urnacidada.testes.MiniTeste.lanca;
import static com.urnacidada.testes.MiniTeste.teste;
import static com.urnacidada.testes.MiniTeste.verdade;

/** Testes da persistência: memória, arquivo local e reserva automática. */
final class RepositorioTestes {

    /** Repositório de mentira que pode ser "ligado" e "desligado" para simular a internet. */
    private static final class Nuvem implements VotoRepository {
        boolean disponivel = true;
        final Map<String, List<Voto>> sessoes = new LinkedHashMap<>();

        @Override
        public void salvarSessao(String sessaoId, List<Voto> votos) throws RepositorioException {
            if (!disponivel) {
                throw new RepositorioException("sem internet", null);
            }
            sessoes.put(sessaoId, votos); // mesma sessão = mesmo documento (idempotente)
        }

        @Override
        public String descricao() {
            return "Nuvem de teste";
        }
    }

    private RepositorioTestes() { }

    private static Path arquivoTemporario() throws IOException {
        return Files.createTempDirectory("urna-testes").resolve("dados").resolve("votos.tsv");
    }

    private static List<Voto> doisVotos() {
        return List.of(
                new Voto("Vereador, Câmara", TipoVoto.VALIDO, "12345", Instant.parse("2026-10-20T12:00:00Z")),
                new Voto("Prefeito", TipoVoto.BRANCO, null, Instant.parse("2026-10-20T12:00:05Z")));
    }

    static void executar() {
        grupo("Persistência: memória e arquivo local");

        teste("memória guarda os votos recebidos", () -> {
            VotoRepositoryEmMemoria memoria = new VotoRepositoryEmMemoria();
            memoria.salvarSessao("s1", doisVotos());
            igual(2, memoria.getVotos().size());
        });

        teste("arquivo: grava e lê de volta, inclusive branco, acentos e vírgula no cargo", () -> {
            VotoRepositoryEmArquivo arquivo = new VotoRepositoryEmArquivo(arquivoTemporario(), "teste");
            arquivo.salvarSessao("sessão-1", doisVotos());
            List<SessaoSalva> lidas = arquivo.lerSessoes();
            igual(1, lidas.size());
            igual("sessão-1", lidas.get(0).sessaoId());
            igual(doisVotos(), lidas.get(0).votos());
        });

        teste("arquivo lê o formato TSV usado pelas versões anteriores", () -> {
            Path caminho = arquivoTemporario();
            Files.createDirectories(caminho.getParent());
            Files.writeString(caminho,
                    "sessao-antiga\tVereador,VALIDO,12345,2026-10-20T12:00:00Z\tPrefeito,BRANCO,,2026-10-20T12:00:05Z\n",
                    StandardCharsets.UTF_8);
            List<SessaoSalva> lidas = new VotoRepositoryEmArquivo(caminho, "teste").lerSessoes();
            igual(1, lidas.size()); igual("sessao-antiga", lidas.get(0).sessaoId()); igual(2, lidas.get(0).votos().size());
        });

        teste("arquivo: gravar a mesma sessão duas vezes não duplica", () -> {
            VotoRepositoryEmArquivo arquivo = new VotoRepositoryEmArquivo(arquivoTemporario(), "teste");
            arquivo.salvarSessao("s1", doisVotos());
            arquivo.salvarSessao("s1", doisVotos());
            arquivo.salvarSessao("s2", doisVotos());
            igual(2, arquivo.lerSessoes().size());
        });

        teste("arquivo: conta usada com votos diferentes é recusada", () -> {
            VotoRepositoryEmArquivo arquivo = new VotoRepositoryEmArquivo(arquivoTemporario(), "teste");
            arquivo.salvarSessao("uid-conta", doisVotos());
            List<Voto> outrosVotos = List.of(
                    new Voto("Vereador, Câmara", TipoVoto.VALIDO, "12345", Instant.parse("2026-10-20T13:00:00Z")),
                    new Voto("Prefeito", TipoVoto.BRANCO, null, Instant.parse("2026-10-20T13:00:05Z")));
            lanca(UsuarioJaVotouException.class, () -> arquivo.salvarSessao("uid-conta", outrosVotos));
            igual(1, arquivo.lerSessoes().size());
        });

        teste("arquivo inexistente é lido como vazio", () -> {
            VotoRepositoryEmArquivo arquivo = new VotoRepositoryEmArquivo(arquivoTemporario(), "teste");
            igual(0, arquivo.lerSessoes().size());
        });

        teste("arquivo: remove somente as sessões indicadas", () -> {
            VotoRepositoryEmArquivo arquivo = new VotoRepositoryEmArquivo(arquivoTemporario(), "teste");
            arquivo.salvarSessao("s1", doisVotos());
            arquivo.salvarSessao("s2", doisVotos());
            arquivo.removerSessoes(Set.of("s1"));
            List<SessaoSalva> restantes = arquivo.lerSessoes();
            igual(1, restantes.size());
            igual("s2", restantes.get(0).sessaoId());
        });

        teste("arquivo: linha corrompida é ignorada na leitura, mas não é apagada", () -> {
            Path caminho = arquivoTemporario();
            VotoRepositoryEmArquivo arquivo = new VotoRepositoryEmArquivo(caminho, "teste");
            arquivo.salvarSessao("s1", doisVotos());
            Files.writeString(caminho, "lixo\tsem,formato\n", StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.APPEND);
            Files.writeString(caminho, "%ZZ\tlinha,inválida,\n", StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.APPEND);
            igual(1, arquivo.lerSessoes().size());
            arquivo.salvarSessao("s2", doisVotos());
            verdade(Files.readString(caminho, StandardCharsets.UTF_8).contains("lixo"), "linha preservada");
            verdade(Files.readString(caminho, StandardCharsets.UTF_8).contains("%ZZ"), "linha com escape inválido preservada");
        });

        teste("arquivo: erro de gravação vira RepositorioException", () -> {
            Path pasta = Files.createTempDirectory("urna-testes");
            Path ocupado = pasta.resolve("arquivo-comum");
            Files.writeString(ocupado, "x"); // um ARQUIVO no lugar onde deveria haver uma pasta
            VotoRepositoryEmArquivo arquivo = new VotoRepositoryEmArquivo(ocupado.resolve("votos.tsv"), "teste");
            lanca(RepositorioException.class, () -> arquivo.salvarSessao("s1", doisVotos()));
        });

        grupo("Persistência: reserva automática (plano B sem internet)");

        teste("com conexão: grava na nuvem, nada fica na reserva e não há aviso", () -> {
            Nuvem nuvem = new Nuvem();
            VotoRepositoryEmArquivo reserva = new VotoRepositoryEmArquivo(arquivoTemporario(), "reserva");
            VotoRepositoryComReserva repo = new VotoRepositoryComReserva(nuvem, reserva);
            repo.salvarSessao("s1", doisVotos());
            igual(1, nuvem.sessoes.size());
            igual(0, reserva.lerSessoes().size());
            verdade(repo.avisoDaUltimaGravacao().isEmpty(), "sem aviso");
        });

        teste("sem conexão: o voto vai para a reserva e a votação não falha", () -> {
            Nuvem nuvem = new Nuvem();
            nuvem.disponivel = false;
            VotoRepositoryEmArquivo reserva = new VotoRepositoryEmArquivo(arquivoTemporario(), "reserva");
            VotoRepositoryComReserva repo = new VotoRepositoryComReserva(nuvem, reserva);
            repo.salvarSessao("s1", doisVotos());
            igual(0, nuvem.sessoes.size());
            igual(1, reserva.lerSessoes().size());
            verdade(repo.avisoDaUltimaGravacao().orElseThrow().contains("guardados neste computador"),
                    "aviso explica a reserva");
        });

        teste("sem conexão e sem reserva: devolve erro para o participante tentar de novo", () -> {
            Path pasta = Files.createTempDirectory("urna-testes");
            Path ocupado = pasta.resolve("arquivo-comum");
            Files.writeString(ocupado, "x");
            Nuvem nuvem = new Nuvem();
            nuvem.disponivel = false;
            VotoRepositoryEmArquivo reserva = new VotoRepositoryEmArquivo(ocupado.resolve("v.tsv"), "reserva");
            VotoRepositoryComReserva repo = new VotoRepositoryComReserva(nuvem, reserva);
            lanca(RepositorioException.class, () -> repo.salvarSessao("s1", doisVotos()));
        });

        teste("sincronizar envia os pendentes quando a conexão volta e esvazia a reserva", () -> {
            Nuvem nuvem = new Nuvem();
            nuvem.disponivel = false;
            VotoRepositoryEmArquivo reserva = new VotoRepositoryEmArquivo(arquivoTemporario(), "reserva");
            VotoRepositoryComReserva repo = new VotoRepositoryComReserva(nuvem, reserva);
            repo.salvarSessao("s1", doisVotos());
            repo.salvarSessao("s2", doisVotos());

            igual(0, repo.sincronizarPendentes()); // ainda sem internet
            igual(2, reserva.lerSessoes().size());

            nuvem.disponivel = true;
            igual(2, repo.sincronizarPendentes());
            igual(2, nuvem.sessoes.size());
            igual(0, reserva.lerSessoes().size());
        });

        teste("a próxima gravação com sucesso também envia o que ficou pendente", () -> {
            Nuvem nuvem = new Nuvem();
            nuvem.disponivel = false;
            VotoRepositoryEmArquivo reserva = new VotoRepositoryEmArquivo(arquivoTemporario(), "reserva");
            VotoRepositoryComReserva repo = new VotoRepositoryComReserva(nuvem, reserva);
            repo.salvarSessao("antiga", doisVotos());

            nuvem.disponivel = true;
            repo.salvarSessao("nova", doisVotos());
            igual(Set.of("antiga", "nova"), nuvem.sessoes.keySet());
            igual(0, reserva.lerSessoes().size());
        });

        teste("descrição informa a nuvem e a reserva", () -> {
            VotoRepositoryComReserva repo = new VotoRepositoryComReserva(new Nuvem(),
                    new VotoRepositoryEmArquivo(arquivoTemporario(), "reserva"));
            verdade(repo.descricao().contains("Nuvem de teste") && repo.descricao().contains("reserva"),
                    "descrição");
        });
    }
}
