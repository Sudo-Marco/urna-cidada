package com.urnacidada.model;

import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.Map;

/**
 * Um cargo em disputa (ex.: Deputado Federal, Presidente).
 * "digitos" é a quantidade de dígitos do número do candidato nesse cargo.
 */
public record Cargo(String nome, int digitos, List<Candidato> candidatos) {

    public Cargo {
        if (nome == null || nome.isBlank() || digitos < 1 || candidatos == null || candidatos.isEmpty()) {
            throw new IllegalArgumentException("Cargo e quantidade de dígitos devem ser válidos.");
        }
        if (candidatos.size() > 5) {
            throw new IllegalArgumentException("Cada cargo pode ter no máximo cinco candidaturas.");
        }
        if (candidatos.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("A lista do cargo não pode conter candidaturas vazias.");
        }
        candidatos = List.copyOf(candidatos);
        validarCargoConhecido(nome, digitos, candidatos);
        if (candidatos.stream().anyMatch(c -> c.numero().length() != digitos)
                || candidatos.stream().map(Candidato::numero).distinct().count() != candidatos.size()) {
            throw new IllegalArgumentException("O número de cada candidato deve ter os dígitos definidos pelo cargo.");
        }
        long partidosDiferentesComMesmoNumero = candidatos.stream()
                .collect(java.util.stream.Collectors.groupingBy(Candidato::numeroPartido))
                .values().stream()
                .filter(grupo -> grupo.stream().map(Candidato::partido).distinct().count() > 1)
                .count();
        if (partidosDiferentesComMesmoNumero > 0) {
            throw new IllegalArgumentException("O mesmo número de partido não pode identificar partidos diferentes no cargo.");
        }
        long codigosDiferentesParaMesmoPartido = candidatos.stream()
                .collect(java.util.stream.Collectors.groupingBy(Candidato::partido))
                .values().stream()
                .filter(grupo -> grupo.stream().map(Candidato::numeroPartido).distinct().count() > 1)
                .count();
        if (codigosDiferentesParaMesmoPartido > 0) {
            throw new IllegalArgumentException("O mesmo partido não pode usar números diferentes no mesmo cargo.");
        }
    }

    public Optional<Candidato> buscarCandidato(String numero) {
        return candidatos.stream()
                .filter(c -> c.numero().equals(numero))
                .findFirst();
    }

    /** Partido fictício correspondente aos dois dígitos digitados para voto de legenda. */
    public Optional<String> buscarPartidoPorNumero(String numeroPartido) {
        if (numeroPartido == null || !numeroPartido.matches("[0-9]{2}")) {
            return Optional.empty();
        }
        return candidatos.stream()
                .filter(c -> c.numeroPartido().equals(numeroPartido))
                .map(Candidato::partido)
                .findFirst();
    }

    public boolean permiteVotoDeLegenda() {
        return candidatos.stream().anyMatch(CandidatoProporcional.class::isInstance);
    }

    private static void validarCargoConhecido(String nome, int digitos, List<Candidato> candidatos) {
        String chave = nome.strip().toLowerCase(java.util.Locale.ROOT);
        Map<String, Integer> comprimentos = Map.of(
                "deputado federal", 4, "deputado estadual", 5, "senador", 3, "governador", 2, "presidente", 2);
        Integer esperado = comprimentos.get(chave);
        if (esperado == null) return; // permite cargos customizados usados por exemplos e exercícios
        if (digitos != esperado) throw new IllegalArgumentException("O cargo " + nome + " exige " + esperado + " dígitos.");
        boolean proporcional = chave.equals("deputado federal") || chave.equals("deputado estadual");
        for (Candidato candidato : candidatos) {
            if (proporcional != (candidato instanceof CandidatoProporcional)) {
                throw new IllegalArgumentException("O tipo de candidatura não corresponde ao cargo " + nome + ".");
            }
            boolean exigeVice = chave.equals("governador") || chave.equals("presidente");
            if (exigeVice != candidato.vice().isPresent()) {
                throw new IllegalArgumentException(exigeVice
                        ? "Governador e Presidente precisam de vice fictício."
                        : "Este cargo não deve ter vice no modelo da simulação.");
            }
        }
    }
}
