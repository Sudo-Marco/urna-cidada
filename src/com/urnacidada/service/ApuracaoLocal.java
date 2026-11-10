package com.urnacidada.service;

import com.urnacidada.model.Candidato;
import com.urnacidada.model.CandidatoProporcional;
import com.urnacidada.model.Cargo;
import com.urnacidada.model.Eleicao;
import com.urnacidada.model.Voto;
import com.urnacidada.repository.RepositorioException;
import com.urnacidada.repository.VotoRepositoryEmArquivo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Apura, sem rede, os votos anônimos arquivados neste computador. */
public final class ApuracaoLocal {
    private ApuracaoLocal() { }

    /** Lê o histórico local e devolve contagens agregadas, sem identificadores de participantes. */
    public static Resultado apurar(Path pastaDados) throws IOException, RepositorioException {
        Map<String, VotoRepositoryEmArquivo.SessaoSalva> sessoes = new LinkedHashMap<>();
        if (Files.isDirectory(pastaDados)) {
            try (var arquivos = Files.list(pastaDados)) {
                for (Path arquivo : arquivos.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().startsWith("votos-"))
                        .filter(p -> p.getFileName().toString().endsWith(".tsv"))
                        .sorted(Comparator.comparing(p -> p.getFileName().toString())).toList()) {
                    for (VotoRepositoryEmArquivo.SessaoSalva sessao
                            : new VotoRepositoryEmArquivo(arquivo, "Histórico local").lerSessoes()) {
                        sessoes.putIfAbsent(sessao.sessaoId(), sessao);
                    }
                }
            }
        }

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

        List<CargoResultado> cargos = new ArrayList<>();
        for (Cargo cargo : Eleicao.exemplo().cargos()) {
            List<CandidatoResultado> candidaturas = cargo.candidatos().stream()
                    .map(candidato -> new CandidatoResultado(candidato.numero(), candidato.nome(), candidato.partido(),
                            candidatos.getOrDefault(chave(cargo.nome(), candidato.numero()), 0)))
                    .toList();
            List<LegendaResultado> partidos = cargo.candidatos().stream()
                    .filter(CandidatoProporcional.class::isInstance)
                    .map(Candidato::numeroPartido)
                    .distinct()
                    .map(numero -> {
                        Candidato candidato = cargo.candidatos().stream()
                                .filter(c -> c.numeroPartido().equals(numero)).findFirst().orElseThrow();
                        return new LegendaResultado(numero, candidato.partido(),
                                legendas.getOrDefault(chave(cargo.nome(), numero), 0));
                    })
                    .toList();
            int votosNominais = candidaturas.stream().mapToInt(CandidatoResultado::votos).sum();
            int votosLegenda = partidos.stream().mapToInt(LegendaResultado::votos).sum();
            cargos.add(new CargoResultado(cargo.nome(), candidaturas, partidos, votosNominais, votosLegenda,
                    brancos.getOrDefault(cargo.nome(), 0), nulos.getOrDefault(cargo.nome(), 0),
                    totais.getOrDefault(cargo.nome(), 0)));
        }
        return new Resultado(sessoes.size(), totalVotos, cargos);
    }

    /** Texto simples mantido para uso por scripts ou leitores que não precisam da interface Swing. */
    public static String gerarRelatorio(Path pastaDados) throws IOException, RepositorioException {
        Resultado resultado = apurar(pastaDados);
        StringBuilder relatorio = new StringBuilder("APURAÇÃO LOCAL · ESTE COMPUTADOR\n")
                .append("Sessões salvas: ").append(resultado.sessoes())
                .append("  |  Votos registrados nos cargos: ").append(resultado.totalVotos()).append("\n")
                .append("Contagem agregada e anônima; esta consulta não acessa o Firebase.\n\n");
        for (CargoResultado cargo : resultado.cargos()) {
            relatorio.append(cargo.nome().toUpperCase(java.util.Locale.ROOT)).append('\n');
            cargo.candidatos().forEach(candidato -> relatorio.append("  ")
                    .append(candidato.numero()).append(" — ").append(candidato.nome()).append(": ")
                    .append(candidato.votos()).append(" voto(s)\n"));
            cargo.legendas().forEach(legenda -> relatorio.append("  Legenda ").append(legenda.numero())
                    .append(" — ").append(legenda.partido()).append(": ").append(legenda.votos())
                    .append(" voto(s)\n"));
            relatorio.append("  Votos nominais: ").append(cargo.votosNominais())
                    .append("  |  Legenda: ").append(cargo.votosLegenda())
                    .append("  |  Branco: ").append(cargo.brancos())
                    .append("  |  Nulo: ").append(cargo.nulos())
                    .append("  |  Total: ").append(cargo.totalVotos()).append("\n\n");
        }
        return relatorio.toString();
    }

    private static String chave(String cargo, String numero) {
        return cargo + '\u0000' + String.valueOf(numero);
    }

    public record Resultado(int sessoes, int totalVotos, List<CargoResultado> cargos) {
        public Resultado { cargos = List.copyOf(cargos); }
    }

    public record CargoResultado(String nome, List<CandidatoResultado> candidatos,
                                 List<LegendaResultado> legendas, int votosNominais,
                                 int votosLegenda, int brancos, int nulos, int totalVotos) {
        public CargoResultado {
            candidatos = List.copyOf(candidatos);
            legendas = List.copyOf(legendas);
        }
    }

    public record CandidatoResultado(String numero, String nome, String partido, int votos) { }
    public record LegendaResultado(String numero, String partido, int votos) { }
}
