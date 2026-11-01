package com.urnacidada;

import com.urnacidada.model.Eleicao;
import com.urnacidada.repository.AutenticacaoFirebase;
import com.urnacidada.repository.FirestoreRestVotoClient;
import com.urnacidada.repository.VotoRepository;
import com.urnacidada.repository.VotoRepositoryEmArquivo;
import com.urnacidada.repository.VotoRepositoryEmMemoria;
import com.urnacidada.repository.VotoRepositoryLocalPrimeiro;
import com.urnacidada.view.TelaUrna;

import javax.swing.JPasswordField;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.GridLayout;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

/** Entrada da aplicação, autenticação Firebase opcional e seleção dos modos. */
public final class Main {
    private static final String AJUDA = """
            Urna Cidadã — simulação educativa
            Uso: iniciar.bat [--local | --memoria | --ajuda]
              padrão     autenticação Firebase se configurada; caso contrário, modo local
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

            ConfiguracaoUrna config = modo.equals("padrao")
                    ? ConfiguracaoUrna.carregar(app.resolve("urna.properties"))
                    : new ConfiguracaoUrna("", "", ConfiguracaoUrna.Estado.AUSENTE, "");
            AutenticacaoFirebase.Sessao usuario = null;
            if (config.valida()) {
                AutenticacaoFirebase autenticacao = new AutenticacaoFirebase(config.apiKey());
                usuario = autenticar(autenticacao);
                if (usuario == null) return; // cancelar o fluxo de identificação
            }

            trava = TravaInstancia.tentar(dados.resolve(".urna-instancia.lock"));
            if (trava == null) throw new IOException("Outra urna já está aberta. Feche-a antes de iniciar uma nova.");
            repositorio = selecionarRepositorio(modo, dados, config, usuario);
            if (usuario != null) {
                VotoRepositoryLocalPrimeiro local = (VotoRepositoryLocalPrimeiro) repositorio;
                FirestoreRestVotoClient consulta = new FirestoreRestVotoClient(
                        config.projectId(), config.apiKey(), usuario);
                local.sincronizarAgora();
                if (local.temPendentes()) {
                    local.close();
                    repositorio = null;
                    trava.close();
                    trava = null;
                    JOptionPane.showMessageDialog(null,
                            "Há uma votação deste CPF aguardando sincronização. Conecte-se à internet e abra a urna novamente antes de votar.",
                            "Sincronização pendente", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (consulta.participanteJaVotou()) {
                    local.close();
                    repositorio = null;
                    trava.close();
                    trava = null;
                    JOptionPane.showMessageDialog(null,
                            "Este CPF já concluiu uma votação. Cada CPF cadastrado pode votar uma vez.",
                            "CPF já utilizado", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
            }
            try { javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getCrossPlatformLookAndFeelClassName()); }
            catch (Exception ignored) { }

            VotoRepository repositorioDaTela = repositorio;
            TravaInstancia finalTrava = trava;
            AutenticacaoFirebase.Sessao participante = usuario;
            String dica = repositorioDaTela.dicaPersistencia();
            SwingUtilities.invokeLater(() -> {
                TelaUrna tela = new TelaUrna(Eleicao.exemplo(), repositorioDaTela, dica,
                        () -> java.util.UUID.randomUUID().toString(), participante != null,
                        () -> { try { finalTrava.close(); } catch (IOException ignored) { } },
                        () -> Main.main(args));
                tela.setVisible(true);
                tela.iniciar();
                tela.mostrarAvisoInicial();
            });
        } catch (Exception e) {
            LogUrna.logger().severe("Falha ao iniciar a urna (" + e.getClass().getSimpleName() + ").");
            if (repositorio instanceof AutoCloseable c) try { c.close(); } catch (Exception ignored) { }
            if (trava != null) try { trava.close(); } catch (IOException ignored) { }
            falha(e.getMessage() == null ? "Não foi possível iniciar a urna." : e.getMessage(), e);
        }
    }

    static VotoRepository selecionarRepositorio(String modo, Path dados, ConfiguracaoUrna config,
                                                 AutenticacaoFirebase.Sessao usuario) throws IOException {
        if (modo.equals("--memoria")) return new VotoRepositoryEmMemoria();
        if (modo.equals("--local")) return new VotoRepositoryEmArquivo(dados.resolve("votos-locais.tsv"), "Modo local");
        if (usuario != null && config.valida()) {
            String uid = usuario.uid();
            if (!uid.matches("[A-Za-z0-9_-]{1,128}")) throw new IOException("Firebase retornou um identificador de conta inválido.");
            Path arquivo = dados.resolve("votos-pendentes-" + uid + ".tsv");
            FirestoreRestVotoClient cliente = new FirestoreRestVotoClient(config.projectId(), config.apiKey(), usuario);
            return new VotoRepositoryLocalPrimeiro(new VotoRepositoryEmArquivo(arquivo, "Modo local primeiro"),
                    cliente, true, config.mensagem(), false);
        }
        Path arquivo = dados.resolve("votos-pendentes.tsv");
        return new VotoRepositoryLocalPrimeiro(new VotoRepositoryEmArquivo(arquivo, "Modo local primeiro"),
                null, false, config.mensagem());
    }

    private static AutenticacaoFirebase.Sessao autenticar(AutenticacaoFirebase autenticacao)
            throws com.urnacidada.repository.RepositorioException {
        Object[] opcoes = {"Entrar", "Primeiro acesso", "Cancelar"};
        int escolha = JOptionPane.showOptionDialog(null,
                "Identifique-se com seu CPF. Cada CPF cadastrado pode votar uma vez.",
                "Urna Cidadã — participante", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, opcoes, opcoes[0]);
        if (escolha == 2 || escolha < 0) return null;
        String cpfEntrada = JOptionPane.showInputDialog(null, "CPF", "Identificação do participante", JOptionPane.QUESTION_MESSAGE);
        if (cpfEntrada == null || cpfEntrada.isBlank()) return null;
        String cpf;
        try { cpf = com.urnacidada.repository.CpfIdentificador.normalizar(cpfEntrada); }
        catch (IllegalArgumentException erro) {
            JOptionPane.showMessageDialog(null, erro.getMessage(), "CPF inválido", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        JPasswordField senha = campoSenha("Senha (mínimo de seis caracteres)");
        if (senha == null) return null;
        char[] senhaValor = senha.getPassword();
        try {
            if (escolha == 0) return autenticacao.entrar(cpf, senhaValor);
            char[] confirmacao = null;
            try {
                JPasswordField confirmar = campoSenha("Digite a senha novamente");
                if (confirmar == null) return null;
                confirmacao = confirmar.getPassword();
                if (!Arrays.equals(senhaValor, confirmacao)) {
                    JOptionPane.showMessageDialog(null, "As senhas não conferem.", "Cadastro", JOptionPane.ERROR_MESSAGE);
                    return null;
                }
                return autenticacao.cadastrar(cpf, senhaValor);
            } finally { if (confirmacao != null) Arrays.fill(confirmacao, '\0'); }
        } catch (com.urnacidada.repository.RepositorioException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Autenticação Firebase", JOptionPane.ERROR_MESSAGE);
            return null;
        } finally { Arrays.fill(senhaValor, '\0'); }
    }

    private static JPasswordField campoSenha(String rotulo) {
        JPasswordField campo = new JPasswordField(22);
        JPanel painel = new JPanel(new GridLayout(2, 1, 4, 4));
        painel.add(new JLabel(rotulo));
        painel.add(campo);
        return JOptionPane.showConfirmDialog(null, painel, "Conta do participante",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION ? campo : null;
    }

    private static void falha(String texto, Exception causa) {
        if (causa != null) LogUrna.logger().severe("Falha fatal: " + causa.getClass().getSimpleName());
        if (!GraphicsEnvironment.isHeadless()) JOptionPane.showMessageDialog(null, texto,
                "Urna Cidadã — não foi possível iniciar", JOptionPane.ERROR_MESSAGE);
        else System.err.println(texto);
    }
}
