package com.urnacidada.testes;

import com.urnacidada.repository.CpfIdentificador;

import static com.urnacidada.testes.MiniTeste.*;

final class CpfIdentificadorTestes {
    private CpfIdentificadorTestes() { }

    static void executar() {
        grupo("CPF: validação e identificador técnico");
        teste("normaliza CPF digitado com pontuação", () ->
                igual("52998224725", CpfIdentificador.normalizar("529.982.247-25")));
        teste("rejeita CPF com dígito verificador incorreto", () ->
                lanca(IllegalArgumentException.class, () -> CpfIdentificador.normalizar("529.982.247-26")));
        teste("rejeita sequência repetida e caracteres fora do formato", () -> {
            lanca(IllegalArgumentException.class, () -> CpfIdentificador.normalizar("111.111.111-11"));
            lanca(IllegalArgumentException.class, () -> CpfIdentificador.normalizar("abc52998224725"));
        });
        teste("deriva um identificador estável sem conter os dígitos do CPF", () -> {
            String primeiro = CpfIdentificador.emailFirebase("529.982.247-25");
            igual(primeiro, CpfIdentificador.emailFirebase("52998224725"));
            verdade(primeiro.startsWith("cpf-") && primeiro.endsWith("@example.com"), "formato técnico");
            verdade(!primeiro.contains("52998224725"), "sem CPF em texto claro");
        });
    }
}
