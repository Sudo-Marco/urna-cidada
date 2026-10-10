package com.urnacidada.model;

import java.util.List;

/** Define quais cargos serão votados e em que ordem. */
public record Eleicao(List<Cargo> cargos) {

    public Eleicao {
        if (cargos.isEmpty()) {
            throw new IllegalArgumentException("A eleição precisa de pelo menos um cargo.");
        }
        cargos = List.copyOf(cargos);
    }

    /** Eleição de exemplo com candidatos fictícios (ajustem como o grupo decidir). */
    public static Eleicao exemplo() {
        Cargo vereador = new Cargo("Vereador", 5, List.of(
                new Candidato("12345", "Ana Exemplo", "Partido Fictício A"),
                new Candidato("23456", "Bruno Teste", "Partido Fictício B"),
                new Candidato("34567", "Carla Modelo", "Partido Fictício C")));

        Cargo prefeito = new Cargo("Prefeito", 2, List.of(
                new Candidato("11", "Diego Simulado", "Partido Fictício A"),
                new Candidato("22", "Elisa Educativa", "Partido Fictício B"),
                new Candidato("33", "Fábio Didático", "Partido Fictício C")));

        // Primeiro o cargo proporcional, depois o majoritário.
        return new Eleicao(List.of(vereador, prefeito));
    }
}
