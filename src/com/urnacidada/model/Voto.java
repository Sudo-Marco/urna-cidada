package com.urnacidada.model;

import java.time.Instant;

/**
 * Voto ANÔNIMO. Não guarda nada sobre quem votou.
 * "numero" é null quando o voto é branco.
 */
public record Voto(String cargo, TipoVoto tipo, String numero, Instant dataHora) {
}
