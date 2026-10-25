package com.urnacidada.model;

/** Candidato fictício de um cargo proporcional (por exemplo, vereador). */
public final class CandidatoProporcional extends Candidato {

    public CandidatoProporcional(String numero, String nome, String partido) {
        super(numero, nome, partido);
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
