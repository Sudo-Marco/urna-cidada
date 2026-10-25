package com.urnacidada;

import com.urnacidada.model.Eleicao;
import com.urnacidada.repository.FirestoreVotoRepository;
import com.urnacidada.repository.VotoRepository;
import com.urnacidada.repository.VotoRepositoryComReserva;
import com.urnacidada.repository.VotoRepositoryEmArquivo;
import com.urnacidada.repository.VotoRepositoryEmMemoria;
import com.urnacidada.view.TelaUrna;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.GraphicsEnvironment;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Ponto de entrada da Urna Cidadã.
 *
 * <p>Modos de gravação:</p>
 * <ul>
 *   <li>(sem argumento) Cloud Firestore, com reserva local se a internet falhar;</li>
 *   <li>{@code --local}: somente arquivo local em {@code dados/};</li>
 *   <li>{@code --memoria}: demonstração, nada é salvo ao fechar.</li>
 * </ul>
 */
public final class Main {

    private static final Path PASTA_DADOS = Path.of("dados");

    private static final String AJUDA = """
            Uso: urna-cidada [--memoria | --local]

              (sem argumento)  grava no Cloud Firestore (variável FIRESTORE_PROJECT_ID),
                               com reserva em dados/votos-pendentes.tsv se a rede falhar
              --local          grava somente em dados/votos-locais.tsv
              --memoria        demonstração; nada é salvo ao fechar
              --ajuda          mostra esta mensagem
            """;

    private enum ModoDeGravacao { PADRAO, LOCAL, MEMORIA }

    private record Escolha(VotoRepository repositorio, Optional<String> avisoInicial) { }

    private Main() { }

    public static void main(String[] args) {
        if (args.length > 1 || (args.length == 1 && !modoValido(args[0]))) {
            System.err.println(AJUDA);
            return;
        }
        if (args.length == 1 && (args[0].equals("--ajuda") || args[0].equals("-h"))) {
            System.out.println(AJUDA);
            return;
        }
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("A interface Swing precisa de um ambiente gráfico.");
            return;
        }

        ModoDeGravacao modo = interpretar(args);
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignorada) {
            // segue com o visual padrão
        }

        Escolha escolha = criarRepositorio(modo);
        SwingUtilities.invokeLater(() -> {
            TelaUrna tela = new TelaUrna(Eleicao.exemplo(), escolha.repositorio());
            tela.setVisible(true);
            escolha.avisoInicial().ifPresent(texto -> JOptionPane.showMessageDialog(
                    tela, texto, "Aviso de configuração", JOptionPane.INFORMATION_MESSAGE));
        });
    }

    private static boolean modoValido(String arg) {
        return arg.equals("--memoria") || arg.equals("--local") || arg.equals("--ajuda") || arg.equals("-h");
    }

    private static ModoDeGravacao interpretar(String[] args) {
        if (args.length == 0) {
            return ModoDeGravacao.PADRAO;
        }
        return switch (args[0]) {
            case "--memoria" -> ModoDeGravacao.MEMORIA;
            case "--local" -> ModoDeGravacao.LOCAL;
            default -> ModoDeGravacao.PADRAO;
        };
    }

    private static Escolha criarRepositorio(ModoDeGravacao modo) {
        return switch (modo) {
            case MEMORIA -> new Escolha(new VotoRepositoryEmMemoria(), Optional.empty());
            case LOCAL -> new Escolha(arquivoLocal(), Optional.empty());
            case PADRAO -> criarFirestoreComReserva();
        };
    }

    private static Escolha criarFirestoreComReserva() {
        String projeto = System.getenv("FIRESTORE_PROJECT_ID");
        if (projeto == null || projeto.isBlank()) {
            return new Escolha(arquivoLocal(), Optional.of(
                    "A variável FIRESTORE_PROJECT_ID não está definida.\n"
                            + "Os votos serão salvos somente no arquivo local dados/votos-locais.tsv.\n"
                            + "Para usar o Firestore, siga o README."));
        }
        try {
            FirestoreVotoRepository firestore = new FirestoreVotoRepository(projeto);
            VotoRepositoryEmArquivo reserva = new VotoRepositoryEmArquivo(
                    PASTA_DADOS.resolve("votos-pendentes.tsv"), "Reserva local");
            VotoRepositoryComReserva repositorio = new VotoRepositoryComReserva(firestore, reserva);

            Thread sincronizacao = new Thread(repositorio::sincronizarPendentes, "sincronizacao-inicial");
            sincronizacao.setDaemon(true);
            sincronizacao.start();
            return new Escolha(repositorio, Optional.empty());
        } catch (RuntimeException e) {
            return new Escolha(arquivoLocal(), Optional.of(
                    "Não foi possível iniciar o Firestore:\n" + e.getMessage()
                            + "\n\nOs votos serão salvos somente no arquivo local dados/votos-locais.tsv."));
        }
    }

    private static VotoRepositoryEmArquivo arquivoLocal() {
        return new VotoRepositoryEmArquivo(PASTA_DADOS.resolve("votos-locais.tsv"),
                "Arquivo local (dados/votos-locais.tsv)");
    }
}
