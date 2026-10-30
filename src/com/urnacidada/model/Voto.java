package com.urnacidada.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Voto ANÔNIMO. Não guarda nada sobre quem votou.
 * "numero" é null quando o voto é branco.
 */
public record Voto(String cargo, TipoVoto tipo, String numero, Instant dataHora) {
    public Voto {
        if (cargo == null || cargo.isBlank()) throw new IllegalArgumentException("O cargo do voto é obrigatório.");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(dataHora, "dataHora");
        if (tipo == TipoVoto.BRANCO && numero != null) throw new IllegalArgumentException("Voto branco não possui número.");
        if (tipo != TipoVoto.BRANCO && (numero == null || numero.isBlank())) {
            throw new IllegalArgumentException("Voto válido ou nulo precisa registrar o número digitado.");
        }
        if (tipo == TipoVoto.LEGENDA && !numero.matches("[0-9]{2}")) {
            throw new IllegalArgumentException("Voto de legenda precisa registrar os dois dígitos do partido.");
        }
    }
}
