package com.urnacidada.model;

import java.util.Objects;

/**
 * Abstração comum a todos os candidatos fictícios.
 *
 * <p>O estado (número, nome e partido) é encapsulado e validado na criação. Cada tipo de
 * cargo especializa duas operações, usadas pela tela: {@link #tipoCargo()} e
 * {@link #explicacaoCargo()}.</p>
 */
public abstract class Candidato {

    private final String numero;
    private final String nome;
    private final String partido;

    protected Candidato(String numero, String nome, String partido) {
        Objects.requireNonNull(numero, "numero");
        Objects.requireNonNull(nome, "nome");
        Objects.requireNonNull(partido, "partido");
        if (numero.isBlank() || !numero.chars().allMatch(Character::isDigit)
                || nome.isBlank() || partido.isBlank()) {
            throw new IllegalArgumentException("Número, nome e partido do candidato devem ser válidos.");
        }
        this.numero = numero;
        this.nome = nome;
        this.partido = partido;
    }

    public String numero() {
        return numero;
    }

    public String nome() {
        return nome;
    }

    public String partido() {
        return partido;
    }

    /** Nome do tipo de cargo (por exemplo, "Proporcional"). */
    public abstract String tipoCargo();

    /** Frase curta, em linguagem simples, explicando como o cargo é decidido. */
    public abstract String explicacaoCargo();

    @Override
    public String toString() {
        return nome + " - " + partido + " (" + tipoCargo() + ")";
    }
}
