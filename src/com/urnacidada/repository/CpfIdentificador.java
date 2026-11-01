package com.urnacidada.repository;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

/** Valida o formato do identificador e cria credenciais sem transmitir os dígitos ao Firebase. */
public final class CpfIdentificador {
    private static final byte[] SALT = "urna-cidada-identificador-v1".getBytes(StandardCharsets.UTF_8);
    private static final byte[] CREDENCIAL_SALT = "urna-cidada-credencial-v1".getBytes(StandardCharsets.UTF_8);
    private static final int ITERACOES = 120_000;

    private CpfIdentificador() { }

    public static String normalizar(String entrada) {
        if (entrada == null || !entrada.matches("[0-9.\\-\\s]+")) {
            throw new IllegalArgumentException("Digite 11 números no formato 000.000.000-00.");
        }
        String cpf = entrada.replaceAll("[^0-9]", "");
        if (cpf.length() != 11) {
            throw new IllegalArgumentException("Digite 11 números no formato 000.000.000-00.");
        }
        return cpf;
    }

    public static String emailFirebase(String entrada) {
        // O prefixo ocupa quatro caracteres; limitar o resumo a 60 mantém a parte local em 64.
        return "cpf-" + derivar(entrada, SALT).substring(0, 60) + "@example.com";
    }

    /** Credencial técnica determinística: o participante não informa uma senha separada. */
    public static String senhaTecnicaFirebase(String entrada) {
        return "U" + derivar(entrada, CREDENCIAL_SALT);
    }

    private static String derivar(String entrada, byte[] sal) {
        String cpf = normalizar(entrada);
        char[] segredo = cpf.toCharArray();
        PBEKeySpec especificacao = new PBEKeySpec(segredo, sal, ITERACOES, 256);
        try {
            byte[] resumo = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(especificacao).getEncoded();
            return HexFormat.of().formatHex(resumo);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível preparar as credenciais técnicas do participante.", e);
        } finally {
            especificacao.clearPassword();
            java.util.Arrays.fill(segredo, '\0');
        }
    }

}
