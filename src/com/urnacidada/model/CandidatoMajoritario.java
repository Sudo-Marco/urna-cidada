package com.urnacidada.model;

import java.util.List;

/** Candidato fictício de um cargo majoritário (por exemplo, prefeito). */
public final class CandidatoMajoritario extends Candidato {

    public CandidatoMajoritario(String numero, String nome, String partido) {
        super(numero, nome, partido);
    }

    public CandidatoMajoritario(String numero, String nome, String partido, String numeroPartido,
                                List<String> propostas, String vice) {
        super(numero, nome, partido, numeroPartido, propostas, vice);
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
