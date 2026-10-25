package com.urnacidada.model;

import java.util.List;
import java.util.Optional;

/**
 * Um cargo em disputa (ex.: Vereador, Prefeito).
 * "digitos" é a quantidade de dígitos do número do candidato nesse cargo.
 */
public record Cargo(String nome, int digitos, List<Candidato> candidatos) {

    public Cargo {
        if (nome == null || nome.isBlank() || digitos < 1 || candidatos == null) {
            throw new IllegalArgumentException("Cargo e quantidade de dígitos devem ser válidos.");
        }
        candidatos = List.copyOf(candidatos);
        if (candidatos.stream().anyMatch(c -> c.numero().length() != digitos)
                || candidatos.stream().map(Candidato::numero).distinct().count() != candidatos.size()) {
            throw new IllegalArgumentException("O número de cada candidato deve ter os dígitos definidos pelo cargo.");
        }
    }

    public Optional<Candidato> buscarCandidato(String numero) {
        return candidatos.stream()
                .filter(c -> c.numero().equals(numero))
                .findFirst();
    }
}
