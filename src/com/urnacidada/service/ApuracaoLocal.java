package com.urnacidada.service;

import com.urnacidada.model.Candidato;
import com.urnacidada.model.CandidatoProporcional;
import com.urnacidada.model.Cargo;
import com.urnacidada.model.Eleicao;
import com.urnacidada.model.TipoVoto;
import com.urnacidada.model.Voto;
import com.urnacidada.repository.RepositorioException;
import com.urnacidada.repository.VotoRepositoryEmArquivo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Apura, sem rede, os votos anônimos arquivados neste computador. */
public final class ApuracaoLocal {
    private ApuracaoLocal() { }

    public static String gerarRelatorio(Path pastaDados) throws IOException, RepositorioException {
        Map<String, VotoRepositoryEmArquivo.SessaoSalva> sessoes = new LinkedHashMap<>();
        if (Files.isDirectory(pastaDados)) {
            try (var arquivos = Files.list(pastaDados)) {
                for (Path arquivo : arquivos.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().startsWith("votos-"))
                        .filter(p -> p.getFileName().toString().endsWith(".tsv")).toList()) {
                    for (VotoRepositoryEmArquivo.SessaoSalva sessao
                            : new VotoRepositoryEmArquivo(arquivo, "Histórico local").lerSessoes()) {
                        sessoes.putIfAbsent(sessao.sessaoId(), sessao);
                    }
                }
            }
        }

        if (sessoes.isEmpty()) {
            return "Não há votos salvos neste computador.\n\nA apuração local inclui somente votações registradas aqui. "
                    + "Ela não consulta o Firestore nem outros computadores.";
        }

        Eleicao eleicao = Eleicao.exemplo();
        Map<String, Integer> candidatos = new LinkedHashMap<>();
        Map<String, Integer> legendas = new LinkedHashMap<>();
        Map<String, Integer> brancos = new LinkedHashMap<>();
        Map<String, Integer> nulos = new LinkedHashMap<>();
        Map<String, Integer> totais = new LinkedHashMap<>();
        int totalVotos = 0;
        for (VotoRepositoryEmArquivo.SessaoSalva sessao : sessoes.values()) {
            for (Voto voto : sessao.votos()) {
                String cargo = voto.cargo();
                totais.merge(cargo, 1, Integer::sum);
                totalVotos++;
                switch (voto.tipo()) {
                    case VALIDO -> candidatos.merge(chave(cargo, voto.numero()), 1, Integer::sum);
                    case LEGENDA -> legendas.merge(chave(cargo, voto.numero()), 1, Integer::sum);
                    case BRANCO -> brancos.merge(cargo, 1, Integer::sum);
                    case NULO -> nulos.merge(cargo, 1, Integer::sum);
                }
            }
        }

        StringBuilder relatorio = new StringBuilder("APURAÇÃO LOCAL · ESTE COMPUTADOR\n")
                .append("Sessões salvas: ").append(sessoes.size())
                .append("  |  Votos: ").append(totalVotos).append("\n")
                .append("Os votos são anônimos e não são enviados ao Firebase por esta consulta.\n\n");
        for (Cargo cargo : eleicao.cargos()) {
            relatorio.append(cargo.nome().toUpperCase(java.util.Locale.ROOT)).append('\n');
            for (Candidato candidato : cargo.candidatos()) {
                relatorio.append("  ").append(candidato.numero()).append(" — ").append(candidato.nome())
                        .append(": ").append(candidatos.getOrDefault(chave(cargo.nome(), candidato.numero()), 0))
                        .append(" voto(s)\n");
            }
            for (Candidato candidato : cargo.candidatos()) {
                if (candidato instanceof CandidatoProporcional) {
                    String numeroPartido = candidato.numeroPartido();
                    String partido = candidato.partido();
                    boolean primeiroDoPartido = cargo.candidatos().stream()
                            .filter(CandidatoProporcional.class::isInstance)
                            .filter(c -> c.numeroPartido().equals(numeroPartido))
                            .findFirst().map(c -> c.numero().equals(candidato.numero())).orElse(false);
                    if (primeiroDoPartido) {
                        relatorio.append("  Legenda ").append(numeroPartido).append(" — ").append(partido)
                                .append(": ").append(legendas.getOrDefault(chave(cargo.nome(), numeroPartido), 0))
                                .append(" voto(s)\n");
                    }
                }
            }
            relatorio.append("  Branco: ").append(brancos.getOrDefault(cargo.nome(), 0))
                    .append("  |  Nulo: ").append(nulos.getOrDefault(cargo.nome(), 0))
                    .append("  |  Total: ").append(totais.getOrDefault(cargo.nome(), 0)).append("\n\n");
        }
        return relatorio.toString();
    }

    private static String chave(String cargo, String numero) {
        return cargo + '\u0000' + String.valueOf(numero);
    }
}
