package com.urnacidada.model;

import java.util.List;

/** Candidato fictício de um cargo proporcional (por exemplo, vereador). */
public final class CandidatoProporcional extends Candidato {

    public CandidatoProporcional(String numero, String nome, String partido) {
        super(numero, nome, partido);
    }

    public CandidatoProporcional(String numero, String nome, String partido, String numeroPartido,
                                 List<String> propostas) {
        super(numero, nome, partido, numeroPartido, propostas, null);
        if (!numero.startsWith(numeroPartido)) {
            throw new IllegalArgumentException("Nos cargos proporcionais, o número do candidato começa pelo número do partido.");
        }
    }

    @Override
    public String tipoCargo() {
        return "Proporcional";
    }

    @Override
    public String explicacaoCargo() {
        return "Cargo proporcional: as vagas são distribuídas conforme os votos recebidos "
                + "pelos partidos e candidatos.";
    }
}
