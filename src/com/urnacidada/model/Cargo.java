package com.urnacidada.model;

import java.util.List;
import java.util.Optional;

/**
 * Um cargo em disputa (ex.: Vereador, Prefeito).
 * "digitos" é a quantidade de dígitos do número do candidato nesse cargo.
 */
public record Cargo(String nome, int digitos, List<Candidato> candidatos) {

    public Cargo {
        candidatos = List.copyOf(candidatos);
    }

    public Optional<Candidato> buscarCandidato(String numero) {
        return candidatos.stream()
                .filter(c -> c.numero().equals(numero))
                .findFirst();
    }
}
