package com.urnacidada;

import com.urnacidada.model.Eleicao;
import com.urnacidada.repository.FirestoreRestVotoClient;
import com.urnacidada.repository.VotoRepository;
import com.urnacidada.repository.VotoRepositoryEmArquivo;
import com.urnacidada.repository.VotoRepositoryEmMemoria;
import com.urnacidada.repository.VotoRepositoryLocalPrimeiro;
import com.urnacidada.view.TelaUrna;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.file.Path;

/** Entrada da aplicação e seleção dos três modos documentados. */
public final class Main {
    private static final String AJUDA = """
            Urna Cidadã — simulação educativa
            Uso: iniciar.bat [--local | --memoria | --ajuda]
              padrão     arquivo local primeiro e sincronização Firebase se configurado
              --local    apenas arquivo local, sem rede
              --memoria  demonstração; nenhum dado é mantido
              --ajuda    mostra esta ajuda
            """;
    private Main() { }

    public static void main(String[] args) {
        if (args.length > 1 || (args.length == 1 && !args[0].equals("--local")
                && !args[0].equals("--memoria") && !args[0].equals("--ajuda") && !args[0].equals("-h"))) {
            falha("Opção não reconhecida.\n\n" + AJUDA, null);
            return;
        }
        if (args.length == 1 && (args[0].equals("--ajuda") || args[0].equals("-h"))) {
            System.out.println(AJUDA);
            return;
        }
        if (GraphicsEnvironment.isHeadless()) {
            falha("Não foi encontrado um ambiente gráfico. Execute a urna em uma sessão de área de trabalho.", null);
            return;
        }
        String modo = args.length == 0 ? "padrao" : args[0];
        TravaInstancia trava = null;
        VotoRepository repositorio = null;
        try {
            Path app = CaminhosAplicacao.diretorioDaAplicacao();
            Path perfil = Path.of(System.getProperty("user.home", "."));
            Path dados = CaminhosAplicacao.resolverPastaDados(app, perfil);
            Path logs = CaminhosAplicacao.resolverPastaLogs(app, perfil);
            LogUrna.configurar(logs);
            trava = TravaInstancia.tentar(dados.resolve(".urna-instancia.lock"));
            if (trava == null) throw new IOException("Outra urna já está aberta. Feche-a antes de iniciar uma nova.");
            repositorio = selecionarRepositorio(modo, dados, Path.of("urna.properties"));
            try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
            catch (Exception ignored) { }
            VotoRepository repositorioDaTela = repositorio;
            TravaInstancia finalTrava = trava;
            String dica = repositorioDaTela.dicaPersistencia();
            SwingUtilities.invokeLater(() -> {
                TelaUrna tela = new TelaUrna(Eleicao.exemplo(), repositorioDaTela, dica);
                tela.setVisible(true);
                tela.mostrarAvisoInicial();
                tela.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override public void windowClosed(java.awt.event.WindowEvent e) {
                        try { finalTrava.close(); } catch (IOException ignored) { }
                    }
                });
            });
        } catch (Exception e) {
            LogUrna.logger().severe("Falha ao iniciar a urna (" + e.getClass().getSimpleName() + ").");
            if (repositorio instanceof AutoCloseable c) try { c.close(); } catch (Exception ignored) { }
            if (trava != null) try { trava.close(); } catch (IOException ignored) { }
            falha(e.getMessage() == null ? "Não foi possível iniciar a urna." : e.getMessage(), e);
        }
    }

    static VotoRepository selecionarRepositorio(String modo, Path dados, Path configPath) throws IOException {
        if (modo.equals("--memoria")) return new VotoRepositoryEmMemoria();
        if (modo.equals("--local")) return new VotoRepositoryEmArquivo(dados.resolve("votos-locais.tsv"), "Modo local");
        ConfiguracaoUrna config = ConfiguracaoUrna.carregar(CaminhosAplicacao.diretorioDaAplicacao().resolve(configPath));
        VotoRepositoryEmArquivo arquivo = new VotoRepositoryEmArquivo(dados.resolve("votos-pendentes.tsv"), "Modo local primeiro");
        FirestoreRestVotoClient cliente = config.valida()
                ? new FirestoreRestVotoClient(config.projectId(), config.apiKey()) : null;
        return new VotoRepositoryLocalPrimeiro(arquivo, cliente, config.valida(), config.mensagem());
    }

    private static void falha(String texto, Exception causa) {
        if (causa != null) LogUrna.logger().severe("Falha fatal: " + causa.getClass().getSimpleName());
        if (!GraphicsEnvironment.isHeadless()) JOptionPane.showMessageDialog(null, texto,
                "Urna Cidadã — não foi possível iniciar", JOptionPane.ERROR_MESSAGE);
        else System.err.println(texto);
    }
}
