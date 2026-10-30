package com.urnacidada.model;

import java.util.Objects;
import java.util.List;
import java.util.Optional;

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
    private final String numeroPartido;
    private final List<String> propostas;
    private final String vice;

    protected Candidato(String numero, String nome, String partido) {
        this(numero, nome, partido, prefixoPartido(numero),
                List.of("Ampliar o acesso a serviços públicos com metas abertas.",
                        "Publicar indicadores simples para acompanhar resultados."), null);
    }

    protected Candidato(String numero, String nome, String partido, String numeroPartido,
                        List<String> propostas, String vice) {
        Objects.requireNonNull(numero, "numero");
        Objects.requireNonNull(nome, "nome");
        Objects.requireNonNull(partido, "partido");
        Objects.requireNonNull(numeroPartido, "numeroPartido");
        Objects.requireNonNull(propostas, "propostas");
        if (numero.isBlank() || !numero.chars().allMatch(Character::isDigit)
                || nome.isBlank() || partido.isBlank() || !numeroPartido.matches("[0-9]{2}")
                || !numero.startsWith(numeroPartido)) {
            throw new IllegalArgumentException("Número, nome e partido do candidato devem ser válidos.");
        }
        if (propostas.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("As propostas não podem estar vazias.");
        }
        List<String> copia = List.copyOf(propostas);
        if (copia.size() < 2 || copia.size() > 4
                || copia.stream().anyMatch(p -> p == null || p.isBlank() || p.length() > 90)) {
            throw new IllegalArgumentException("Cada candidato precisa ter de duas a quatro propostas de até 90 caracteres.");
        }
        this.numero = numero;
        this.nome = nome;
        this.partido = partido;
        this.numeroPartido = numeroPartido;
        this.propostas = copia;
        if (vice != null && (!vice.isBlank() && vice.strip().length() > 60)) {
            throw new IllegalArgumentException("O nome fictício do vice deve ter até 60 caracteres.");
        }
        this.vice = vice == null || vice.isBlank() ? null : vice.strip();
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

    public String numeroPartido() {
        return numeroPartido;
    }

    public List<String> propostas() {
        return propostas;
    }

    public Optional<String> vice() {
        return Optional.ofNullable(vice);
    }

    /** Nome do tipo de cargo (por exemplo, "Proporcional"). */
    public abstract String tipoCargo();

    /** Frase curta, em linguagem simples, explicando como o cargo é decidido. */
    public abstract String explicacaoCargo();

    @Override
    public String toString() {
        return nome + " - " + partido + " (" + tipoCargo() + ")";
    }

    private static String prefixoPartido(String numero) {
        return numero != null && numero.length() >= 2 && numero.substring(0, 2).matches("[0-9]{2}")
                ? numero.substring(0, 2) : "00";
    }
}
