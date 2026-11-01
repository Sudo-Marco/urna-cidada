package com.urnacidada;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Resolve diretórios ao lado da aplicação, sem depender do diretório de onde ela foi iniciada. */
public final class CaminhosAplicacao {

    private CaminhosAplicacao() { }

    public static Path diretorioDaAplicacao() {
        String configurado = System.getenv("URNA_HOME");
        if (configurado != null && !configurado.isBlank()) return Path.of(configurado).toAbsolutePath().normalize();
        try {
            Path origem = Path.of(CaminhosAplicacao.class.getProtectionDomain().getCodeSource()
                    .getLocation().toURI()).toAbsolutePath().normalize();
            if (Files.isRegularFile(origem)) return origem.getParent();
            Path atual = origem;
            while (atual != null) {
                if (Files.exists(atual.resolve("pom.xml")) || Files.exists(atual.resolve("scripts"))) return atual;
                atual = atual.getParent();
            }
        } catch (URISyntaxException | SecurityException ignorada) { }
        return Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
    }

    public static Path resolverPastaDados(Path app, Path perfil) throws IOException {
        Path local = app.resolve("dados");
        if (testarGravacao(local)) return local;
        Path alternativa = perfil.resolve(".urna-cidada").resolve("dados");
        if (testarGravacao(alternativa)) return alternativa;
        throw new IOException("Não há permissão para gravar os dados ao lado da urna nem no perfil do usuário.");
    }

    public static Path resolverPastaLogs(Path app, Path perfil) throws IOException {
        Path local = app.resolve("logs");
        if (testarGravacao(local)) return local;
        Path alternativa = perfil.resolve(".urna-cidada").resolve("logs");
        if (testarGravacao(alternativa)) return alternativa;
        throw new IOException("Não há permissão para gravar o log da urna.");
    }

    /** Remove arquivos de votação somente deste computador; os logs e a configuração ficam intactos. */
    public static int apagarArquivosDeVoto(Path pastaDados) throws IOException {
        if (!Files.isDirectory(pastaDados)) return 0;
        int removidos = 0;
        try (var arquivos = Files.list(pastaDados)) {
            for (Path arquivo : arquivos.toList()) {
                String nome = arquivo.getFileName().toString();
                if (Files.isRegularFile(arquivo) && nome.startsWith("votos-") && nome.endsWith(".tsv")) {
                    if (Files.deleteIfExists(arquivo)) removidos++;
                }
            }
        }
        return removidos;
    }

    private static boolean testarGravacao(Path pasta) {
        try {
            Files.createDirectories(pasta);
            Path teste = Files.createTempFile(pasta, ".urna-verifica-", ".tmp");
            Files.deleteIfExists(teste);
            return true;
        } catch (IOException | SecurityException e) {
            return false;
        }
    }
}
