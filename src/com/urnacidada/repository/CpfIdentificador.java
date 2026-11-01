package com.urnacidada.repository;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

/** Valida o CPF informado e cria um identificador técnico sem transmitir seus dígitos ao Firebase. */
public final class CpfIdentificador {
    private static final byte[] SALT = "urna-cidada-identificador-v1".getBytes(StandardCharsets.UTF_8);
    private static final int ITERACOES = 120_000;

    private CpfIdentificador() { }

    public static String normalizar(String entrada) {
        if (entrada == null || !entrada.matches("[0-9.\\-\\s]+")) {
            throw new IllegalArgumentException("Digite um CPF válido.");
        }
        String cpf = entrada.replaceAll("[^0-9]", "");
        if (cpf.length() != 11 || cpf.chars().distinct().count() == 1 || !digitosVerificadoresValidos(cpf)) {
            throw new IllegalArgumentException("O CPF informado não é válido.");
        }
        return cpf;
    }

    public static String emailFirebase(String entrada) {
        String cpf = normalizar(entrada);
        char[] segredo = cpf.toCharArray();
        PBEKeySpec especificacao = new PBEKeySpec(segredo, SALT, ITERACOES, 256);
        try {
            byte[] resumo = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(especificacao).getEncoded();
            return "cpf-" + HexFormat.of().formatHex(resumo) + "@example.com";
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível preparar o identificador do participante.", e);
        } finally {
            especificacao.clearPassword();
            java.util.Arrays.fill(segredo, '\0');
        }
    }

    private static boolean digitosVerificadoresValidos(String cpf) {
        int soma = 0;
        for (int i = 0; i < 9; i++) soma += (cpf.charAt(i) - '0') * (10 - i);
        int primeiro = (soma * 10) % 11;
        if (primeiro == 10) primeiro = 0;
        if (primeiro != cpf.charAt(9) - '0') return false;

        soma = 0;
        for (int i = 0; i < 10; i++) soma += (cpf.charAt(i) - '0') * (11 - i);
        int segundo = (soma * 10) % 11;
        if (segundo == 10) segundo = 0;
        return segundo == cpf.charAt(10) - '0';
    }
}
