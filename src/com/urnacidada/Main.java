package com.urnacidada;

import com.urnacidada.model.Eleicao;
import com.urnacidada.repository.AutenticacaoFirebase;
import com.urnacidada.repository.FirestoreRestVotoClient;
import com.urnacidada.repository.FirestoreAdminClient;
import com.urnacidada.repository.VotoRepository;
import com.urnacidada.repository.VotoRepositoryEmArquivo;
import com.urnacidada.repository.VotoRepositoryEmMemoria;
import com.urnacidada.repository.VotoRepositoryLocalPrimeiro;
import com.urnacidada.view.TelaUrna;

import javax.swing.JFormattedTextField;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.text.MaskFormatter;
import java.awt.GridLayout;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

/** Entrada da aplicação, autenticação Firebase opcional e seleção dos modos. */
public final class Main {
    private static final String AJUDA = """
            Urna Cidadã — simulação educativa
            Uso: iniciar.bat [--local | --memoria | --administrar | --ajuda]
              padrão     autenticação Firebase se configurada; caso contrário, modo local
              --local    apenas arquivo local, sem rede
              --memoria  demonstração; nenhum dado é mantido
              --administrar abre as ferramentas protegidas de administração
              --ajuda    mostra esta ajuda
            """;

    private Main() { }

    public static void main(String[] args) {
        // Work around Java 2D/Direct3D paint failures on some Windows graphics drivers.
        System.setProperty("sun.java2d.d3d", "false");
        if (args.length > 1 || (args.length == 1 && !args[0].equals("--local")
                && !args[0].equals("--memoria") && !args[0].equals("--administrar")
                && !args[0].equals("--ajuda") && !args[0].equals("-h"))) {
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

            boolean modoAdministracao = modo.equals("--administrar");
            ConfiguracaoUrna config = modo.equals("padrao") || modoAdministracao
                    ? ConfiguracaoUrna.carregar(app.resolve("urna.properties"))
                    : new ConfiguracaoUrna("", "", ConfiguracaoUrna.Estado.AUSENTE, "");
            AutenticacaoFirebase.Sessao usuario = null;
            if (config.valida()) {
                AutenticacaoFirebase autenticacao = new AutenticacaoFirebase(config.apiKey());
                usuario = modoAdministracao ? autenticarAdministrador(autenticacao) : autenticar(autenticacao);
                if (usuario == null) return; // cancelar o fluxo de identificação
            }
            if (modoAdministracao) {
                if (usuario == null) {
                    falha("Configure o Firebase para usar a administração.", null);
                    return;
                }
                executarAdministracao(config, usuario, dados);
                return;
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
                tela.iniciar();
                tela.setVisible(true);
                javax.swing.Timer avisoInicial = new javax.swing.Timer(250, evento -> tela.mostrarAvisoInicial());
                avisoInicial.setRepeats(false);
                avisoInicial.start();
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
        int escolha = DialogosUrna.opcoes(null,
                "Identifique-se somente com seu CPF. Cada CPF cadastrado pode votar uma vez; não é necessária senha.",
                "Urna Cidadã — participante", JOptionPane.QUESTION_MESSAGE, opcoes, opcoes[0]);
        if (escolha == 2 || escolha < 0) return null;
        JFormattedTextField campoCpf = campoCpf();
        JPanel painelCpf = new JPanel(new GridLayout(2, 1, 4, 4));
        painelCpf.add(new JLabel("CPF (000.000.000-00)"));
        painelCpf.add(campoCpf);
        int resultado = DialogosUrna.confirmar(null, painelCpf, "Identificação do participante",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (resultado != JOptionPane.OK_OPTION) return null;
        String cpf;
        try { cpf = com.urnacidada.repository.CpfIdentificador.normalizar(campoCpf.getText()); }
        catch (IllegalArgumentException erro) {
            DialogosUrna.mensagem(null, erro.getMessage(), "Identificador inválido", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        try {
            if (escolha == 0) return autenticacao.entrar(cpf);
            return autenticacao.cadastrar(cpf);
        } catch (com.urnacidada.repository.RepositorioException e) {
            DialogosUrna.mensagem(null, e.getMessage(), "Autenticação Firebase", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private static AutenticacaoFirebase.Sessao autenticarAdministrador(AutenticacaoFirebase autenticacao) {
        JTextField email = new JTextField(24);
        JPasswordField senha = new JPasswordField(24);
        JPanel painel = new JPanel(new GridLayout(4, 1, 4, 4));
        painel.add(new JLabel("E-mail da conta administrativa"));
        painel.add(email);
        painel.add(new JLabel("Senha de administrador"));
        painel.add(senha);
        int resultado = DialogosUrna.confirmar(null, painel, "Acesso administrativo",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (resultado != JOptionPane.OK_OPTION) return null;
        char[] valorSenha = senha.getPassword();
        try {
            return autenticacao.entrarAdministrador(email.getText().strip(), valorSenha);
        } catch (com.urnacidada.repository.RepositorioException e) {
            DialogosUrna.mensagem(null, e.getMessage(), "Acesso administrativo", JOptionPane.ERROR_MESSAGE);
            return null;
        } finally {
            Arrays.fill(valorSenha, '\0');
        }
    }

    private static void executarAdministracao(ConfiguracaoUrna config,
                                               AutenticacaoFirebase.Sessao usuario,
                                               Path dados) {
        TravaInstancia travaAdmin = null;
        try {
            FirestoreAdminClient administrador = new FirestoreAdminClient(config.projectId(), config.apiKey(), usuario);
            if (!administrador.ehAdministrador()) {
                DialogosUrna.mensagem(null,
                        "Esta conta não tem permissão de administrador no Firestore.",
                        "Acesso negado", JOptionPane.ERROR_MESSAGE);
                return;
            }
            travaAdmin = TravaInstancia.tentar(dados.resolve(".urna-instancia.lock"));
            if (travaAdmin == null) throw new IOException("Feche as outras janelas da urna antes de apagar os dados.");

            int acao = DialogosUrna.opcoes(null, "Conta administrativa confirmada.",
                    "Administração da urna", JOptionPane.WARNING_MESSAGE,
                    new Object[]{"Apagar dados salvos", "Cancelar"}, "Cancelar");
            if (acao != 0 || !confirmarApagamento()) return;

            int documentos = administrador.apagarDadosVotacao();
            int arquivos = CaminhosAplicacao.apagarArquivosDeVoto(dados);
            DialogosUrna.mensagem(null,
                    "Limpeza concluída. " + documentos + " documentos foram removidos do Firestore e "
                            + arquivos + " arquivos de voto deste computador.",
                    "Dados apagados", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            DialogosUrna.mensagem(null,
                    e.getMessage() == null ? "Não foi possível concluir a limpeza." : e.getMessage(),
                    "Limpeza incompleta", JOptionPane.ERROR_MESSAGE);
        } finally {
            if (travaAdmin != null) try { travaAdmin.close(); } catch (IOException ignored) { }
        }
    }

    private static JFormattedTextField campoCpf() {
        try {
            MaskFormatter mascara = new MaskFormatter("###.###.###-##");
            mascara.setPlaceholderCharacter(' ');
            mascara.setValidCharacters("0123456789");
            mascara.setAllowsInvalid(false);
            mascara.setCommitsOnValidEdit(true);
            JFormattedTextField campo = new JFormattedTextField(mascara);
            campo.setColumns(14);
            return campo;
        } catch (java.text.ParseException e) {
            throw new IllegalStateException("Não foi possível preparar o campo de CPF.", e);
        }
    }

    private static boolean confirmarApagamento() {
        int confirmacao = DialogosUrna.confirmar(null,
                "Isso apaga sessões, contadores e marcas de voto no Firestore, além dos arquivos de voto deste computador. "
                        + "Votos pendentes em outros computadores podem voltar a ser enviados quando a conexão retornar. "
                        + "A ação não pode ser desfeita. Os registros de administradores serão mantidos.",
                "Confirmar limpeza da urna", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirmacao != JOptionPane.YES_OPTION) return false;
        String palavra = DialogosUrna.entrada(null, "Para confirmar, digite APAGAR:",
                "Confirmação administrativa", JOptionPane.WARNING_MESSAGE);
        return "APAGAR".equals(palavra);
    }

    private static void falha(String texto, Exception causa) {
        if (causa != null) LogUrna.logger().severe("Falha fatal: " + causa.getClass().getSimpleName());
        if (!GraphicsEnvironment.isHeadless()) DialogosUrna.mensagem(null, texto,
                "Urna Cidadã — não foi possível iniciar", JOptionPane.ERROR_MESSAGE);
        else System.err.println(texto);
    }
}
