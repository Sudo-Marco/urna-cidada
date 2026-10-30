package com.urnacidada;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Map;
import java.util.regex.Pattern;

/** Configuração opcional do Firebase; nenhum valor secreto é incluído no projeto. */
public record ConfiguracaoUrna(String projectId, String apiKey, Estado estado, String mensagem) {

    public enum Estado { AUSENTE, INCOMPLETA, EXEMPLO, VALIDA }

    private static final Pattern PROJECT_ID = Pattern.compile("[a-z0-9][a-z0-9-]{4,28}[a-z0-9]");
    private static final Pattern API_KEY = Pattern.compile("AIza[0-9A-Za-z_-]{30,}");

    public boolean valida() { return estado == Estado.VALIDA; }

    @Override
    public String toString() {
        return "ConfiguracaoUrna[projectId=" + projectId + ", apiKey=<oculta>, estado=" + estado + "]";
    }

    public static ConfiguracaoUrna carregar(Path arquivo) {
        return carregar(arquivo, System.getenv());
    }

    /** Variante injetável para testes determinísticos da precedência de variáveis de ambiente. */
    public static ConfiguracaoUrna carregar(Path arquivo, Map<String, String> ambiente) {
        Properties propriedades = new Properties();
        if (Files.isRegularFile(arquivo)) {
            try (var leitor = Files.newBufferedReader(arquivo)) {
                propriedades.load(leitor);
            } catch (IOException e) {
                return new ConfiguracaoUrna("", "", Estado.INCOMPLETA,
                        "Não foi possível ler urna.properties. A urna continuará em modo local.");
            }
        }
        String projectId = primeiroValor(ambiente.get("URNA_FIRESTORE_PROJECT_ID"),
                ambiente.get("FIRESTORE_PROJECT_ID"), propriedades.getProperty("firebase.projectId"));
        String apiKey = primeiroValor(ambiente.get("URNA_FIRESTORE_API_KEY"),
                ambiente.get("FIRESTORE_API_KEY"), propriedades.getProperty("firebase.apiKey"));
        boolean semProject = projectId.isBlank();
        boolean semKey = apiKey.isBlank();
        if (semProject && semKey) {
            return new ConfiguracaoUrna("", "", Estado.AUSENTE,
                    "Modo local · configure urna.properties para ativar o envio ao Firebase.");
        }
        if (semProject || semKey) {
            return new ConfiguracaoUrna(projectId, apiKey, Estado.INCOMPLETA,
                    "Configuração do Firebase incompleta · votos serão guardados neste computador.");
        }
        if (pareceExemplo(projectId) || pareceExemplo(apiKey)) {
            return new ConfiguracaoUrna(projectId, apiKey, Estado.EXEMPLO,
                    "Configuração de exemplo não preenchida · votos serão guardados neste computador.");
        }
        if (!PROJECT_ID.matcher(projectId).matches() || !API_KEY.matcher(apiKey).matches()) {
            return new ConfiguracaoUrna(projectId, apiKey, Estado.INCOMPLETA,
                    "Configuração do Firebase inválida · confira urna.properties; votos ficam locais.");
        }
        return new ConfiguracaoUrna(projectId, apiKey, Estado.VALIDA, "Firebase configurado.");
    }

    private static String primeiroValor(String ambiente1, String ambiente2, String arquivo) {
        if (ambiente1 != null && !ambiente1.isBlank()) return ambiente1.strip();
        if (ambiente2 != null && !ambiente2.isBlank()) return ambiente2.strip();
        return arquivo == null ? "" : arquivo.strip();
    }

    private static boolean pareceExemplo(String valor) {
        String normalizado = valor.toLowerCase(java.util.Locale.ROOT);
        return normalizado.contains("seu_") || normalizado.contains("seu-")
                || normalizado.contains("exemplo") || normalizado.contains("substitua")
                || normalizado.equals("project_id") || normalizado.equals("api_key");
    }
}
