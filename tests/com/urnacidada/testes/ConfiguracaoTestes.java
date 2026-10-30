package com.urnacidada.testes;

import com.urnacidada.ConfiguracaoUrna;
import java.nio.file.Files;
import java.nio.file.Path;
import static com.urnacidada.testes.MiniTeste.*;

final class ConfiguracaoTestes {
    private ConfiguracaoTestes() { }
    static void executar() {
        grupo("Configuração do Firebase (sem exibir credenciais)");
        teste("arquivo ausente mantém modo local", () -> {
            ConfiguracaoUrna c = ConfiguracaoUrna.carregar(Files.createTempDirectory("urna-config").resolve("ausente.properties"), java.util.Map.of());
            igual(ConfiguracaoUrna.Estado.AUSENTE, c.estado()); verdade(!c.valida());
        });
        teste("configuração incompleta mantém modo local", () -> {
            Path p=Files.createTempFile("urna-config", ".properties"); Files.writeString(p, "firebase.projectId=projeto-ficticio\n");
            igual(ConfiguracaoUrna.Estado.INCOMPLETA, ConfiguracaoUrna.carregar(p, java.util.Map.of()).estado());
        });
        teste("valores de exemplo são identificados", () -> {
            Path p=Files.createTempFile("urna-config", ".properties"); Files.writeString(p, "firebase.projectId=SEU_PROJECT_ID\nfirebase.apiKey=SUA_CHAVE_API\n");
            igual(ConfiguracaoUrna.Estado.EXEMPLO, ConfiguracaoUrna.carregar(p, java.util.Map.of()).estado());
        });
        teste("configuração sintaticamente válida é reconhecida e a chave fica mascarada", () -> {
            Path p=Files.createTempFile("urna-config", ".properties"); String chave="AIza"+"A".repeat(35);
            Files.writeString(p, "firebase.projectId=urna-ficticia-2026\nfirebase.apiKey="+chave+"\n");
            ConfiguracaoUrna c=ConfiguracaoUrna.carregar(p, java.util.Map.of()); igual(ConfiguracaoUrna.Estado.VALIDA, c.estado());
            verdade(!c.toString().contains(chave), "chave não aparece em toString");
        });
        teste("variável URNA_FIRESTORE sobrescreve o arquivo", () -> {
            Path p=Files.createTempFile("urna-config", ".properties");
            Files.writeString(p,"firebase.projectId=arquivo-ficticio\nfirebase.apiKey=AIza"+"A".repeat(35)+"\n");
            String chave="AIza"+"B".repeat(35);
            ConfiguracaoUrna c=ConfiguracaoUrna.carregar(p,java.util.Map.of("URNA_FIRESTORE_PROJECT_ID","urna-ficticia-2026","URNA_FIRESTORE_API_KEY",chave));
            igual("urna-ficticia-2026",c.projectId()); verdade(!c.toString().contains(chave));
        });
    }
}
