package com.urnacidada.testes;

import com.urnacidada.repository.CpfIdentificador;

import static com.urnacidada.testes.MiniTeste.*;

final class CpfIdentificadorTestes {
    private CpfIdentificadorTestes() { }

    static void executar() {
        grupo("Identificador em formato CPF e credencial técnica");
        teste("normaliza CPF digitado com pontuação", () ->
                igual("52998224725", CpfIdentificador.normalizar("529.982.247-25")));
        teste("aceita identificadores inventados com onze dígitos, mesmo sem dígitos verificadores reais", () -> {
            igual("12345678900", CpfIdentificador.normalizar("123.456.789-00"));
            igual("11111111111", CpfIdentificador.normalizar("111.111.111-11"));
        });
        teste("exige onze dígitos e rejeita caracteres fora do formato", () -> {
            lanca(IllegalArgumentException.class, () -> CpfIdentificador.normalizar("123.456.789-0"));
            lanca(IllegalArgumentException.class, () -> CpfIdentificador.normalizar("abc52998224725"));
        });
        teste("deriva um identificador estável sem conter os dígitos do CPF", () -> {
            String primeiro = CpfIdentificador.emailFirebase("529.982.247-25");
            igual(primeiro, CpfIdentificador.emailFirebase("52998224725"));
            verdade(primeiro.startsWith("cpf-") && primeiro.endsWith("@example.com"), "formato técnico");
            verdade(!primeiro.contains("52998224725"), "sem CPF em texto claro");
            String credencial = CpfIdentificador.senhaTecnicaFirebase("529.982.247-25");
            igual(credencial, CpfIdentificador.senhaTecnicaFirebase("52998224725"));
            verdade(!credencial.contains("52998224725"), "credencial técnica não contém CPF em texto claro");
        });
    }
}
