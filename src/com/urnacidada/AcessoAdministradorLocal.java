package com.urnacidada;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Properties;

/** Verificador local protegido para liberar somente a consulta offline da apuração. */
public final class AcessoAdministradorLocal {
    private static final int ITERACOES = 310_000;
    private static final int TAMANHO_CHAVE = 256;
    private static final int TAMANHO_SAL = 16;

    private AcessoAdministradorLocal() { }

    public static void registrar(Path pastaDados, String email, char[] senha)
            throws IOException, GeneralSecurityException {
        Files.createDirectories(pastaDados);
        byte[] sal = new byte[TAMANHO_SAL];
        new SecureRandom().nextBytes(sal);
        byte[] resumo = derivar(senha, sal, ITERACOES);
        Properties dados = new Properties();
        dados.setProperty("email", email.strip().toLowerCase(java.util.Locale.ROOT));
        dados.setProperty("iteracoes", Integer.toString(ITERACOES));
        dados.setProperty("sal", Base64.getEncoder().encodeToString(sal));
        dados.setProperty("resumo", Base64.getEncoder().encodeToString(resumo));
        Path destino = pastaDados.resolve("administrador-local.properties");
        Path temporario = Files.createTempFile(pastaDados, ".administrador-local-", ".tmp");
        try (var saida = Files.newOutputStream(temporario)) {
            dados.store(saida, "Verificador local para apuração offline");
        }
        try {
            Files.move(temporario, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.AtomicMoveNotSupportedException ignorada) {
            Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporario);
            Arrays.fill(resumo, (byte) 0);
            Arrays.fill(sal, (byte) 0);
        }
    }

    public static boolean verificar(Path pastaDados, String email, char[] senha)
            throws IOException, GeneralSecurityException {
        Path arquivo = pastaDados.resolve("administrador-local.properties");
        if (!Files.isRegularFile(arquivo)) return false;
        Properties dados = new Properties();
        try (var entrada = Files.newInputStream(arquivo)) { dados.load(entrada); }
        String esperadoEmail = dados.getProperty("email", "");
        String informadoEmail = email == null ? "" : email.strip().toLowerCase(java.util.Locale.ROOT);
        if (!esperadoEmail.equals(informadoEmail)) return false;
        int iteracoes = Integer.parseInt(dados.getProperty("iteracoes", "0"));
        if (iteracoes < 100_000 || iteracoes > 1_000_000) return false;
        byte[] sal = Base64.getDecoder().decode(dados.getProperty("sal", ""));
        byte[] esperado = Base64.getDecoder().decode(dados.getProperty("resumo", ""));
        byte[] informado = derivar(senha, sal, iteracoes);
        try { return java.security.MessageDigest.isEqual(esperado, informado); }
        finally {
            Arrays.fill(sal, (byte) 0);
            Arrays.fill(esperado, (byte) 0);
            Arrays.fill(informado, (byte) 0);
        }
    }

    private static byte[] derivar(char[] senha, byte[] sal, int iteracoes) throws GeneralSecurityException {
        PBEKeySpec especificacao = new PBEKeySpec(senha, sal, iteracoes, TAMANHO_CHAVE);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(especificacao).getEncoded(); }
        finally { especificacao.clearPassword(); }
    }
}
