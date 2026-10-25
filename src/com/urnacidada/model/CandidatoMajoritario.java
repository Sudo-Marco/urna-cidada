package com.urnacidada.model;

/** Candidato fictício de um cargo majoritário (por exemplo, prefeito). */
public final class CandidatoMajoritario extends Candidato {

    public CandidatoMajoritario(String numero, String nome, String partido) {
        super(numero, nome, partido);
    }

    @Override
    public String tipoCargo() {
        return "Majoritário";
    }

    @Override
    public String explicacaoCargo() {
        return "Cargo majoritário: é escolhido o candidato que recebe a maioria dos votos.";
    }
}
