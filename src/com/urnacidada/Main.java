package com.urnacidada;

import com.urnacidada.model.Eleicao;
import com.urnacidada.repository.AutenticacaoFirebase;
import com.urnacidada.repository.FirestoreRestVotoClient;
import com.urnacidada.repository.FirestoreAdminClient;
import com.urnacidada.repository.VotoRepository;
import com.urnacidada.repository.VotoRepositoryEmArquivo;
import com.urnacidada.repository.VotoRepositoryEmMemoria;
import com.urnacidada.repository.VotoRepositoryLocalPrimeiro;
import com.urnacidada.service.ApuracaoLocal;
import com.urnacidada.view.TelaUrna;

import javax.swing.JFormattedTextField;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
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
    /** Hash técnico usado somente para particionar a fila offline por CPF sem gravar o CPF. */
    private static String identificadorLocalOffline;
    private static boolean modoLocalOffline;
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
        identificadorLocalOffline = null;
        modoLocalOffline = false;
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
            if (modoAdministracao) {
                executarAdministracao(config, dados);
                return;
            }
            AutenticacaoFirebase.Sessao usuario = null;
            if (config.valida()) {
                AutenticacaoFirebase autenticacao = new AutenticacaoFirebase(config.apiKey());
                usuario = autenticar(autenticacao);
                if (usuario == null && !modoLocalOffline) return; // cancelar o fluxo de identificação
            }

            trava = TravaInstancia.tentar(dados.resolve(".urna-instancia.lock"));
            if (trava == null) throw new IOException("Outra urna já está aberta. Feche-a antes de iniciar uma nova.");
            if (modoLocalOffline) {
                Path arquivo = dados.resolve("votos-historico-" + identificadorLocalOffline + ".tsv");
                VotoRepositoryEmArquivo offline = new VotoRepositoryEmArquivo(arquivo, "Modo local sem internet");
                if (!offline.lerSessoes().isEmpty()) {
                    trava.close();
                    trava = null;
                    identificadorLocalOffline = null;
                    modoLocalOffline = false;
                    JOptionPane.showMessageDialog(null,
                            "Este CPF já tem uma votação salva neste computador. A fila local ainda pode ser consultada pela administração.",
                            "CPF já utilizado neste computador", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                repositorio = offline;
            } else {
                if (identificadorLocalOffline != null) {
                    Path arquivoOffline = dados.resolve("votos-historico-" + identificadorLocalOffline + ".tsv");
                    if (!new VotoRepositoryEmArquivo(arquivoOffline, "Votos offline").lerSessoes().isEmpty()) {
                        trava.close();
                        trava = null;
                        JOptionPane.showMessageDialog(null,
                                "Este CPF já tem um voto salvo offline neste computador. A votação local não será repetida nem enviada sem conferência administrativa.",
                                "CPF já utilizado offline", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                }
                repositorio = selecionarRepositorio(modo, dados, config, usuario);
            }
            identificadorLocalOffline = null;
            modoLocalOffline = false;
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
                try {
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
                } catch (com.urnacidada.repository.FalhaFirestore falha) {
                    if (falha.tipo() != com.urnacidada.repository.FalhaFirestore.Tipo.TRANSITORIA) throw falha;
                    if (local.temVotoNoHistorico()) {
                        local.close();
                        repositorio = null;
                        trava.close();
                        trava = null;
                        JOptionPane.showMessageDialog(null,
                                "Este CPF já tem uma votação salva neste computador. Conecte-se ao Firebase para confirmar o estado antes de abrir outra sessão.",
                                "Verificação offline", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    LogUrna.logger().info("Sem conexão na consulta do marcador; sessão pode ser gravada localmente até a sincronização.");
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
            identificadorLocalOffline = null;
            modoLocalOffline = false;
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
            String chaveHistorico = identificadorLocalOffline == null ? uid : identificadorLocalOffline;
            Path historico = dados.resolve("votos-historico-" + chaveHistorico + ".tsv");
            FirestoreRestVotoClient cliente = new FirestoreRestVotoClient(config.projectId(), config.apiKey(), usuario);
            return new VotoRepositoryLocalPrimeiro(new VotoRepositoryEmArquivo(arquivo, "Fila de sincronização local"),
                    new VotoRepositoryEmArquivo(historico, "Histórico local de votos"),
                    cliente, true, config.mensagem(), false);
        }
        Path arquivo = dados.resolve("votos-pendentes.tsv");
        return new VotoRepositoryLocalPrimeiro(new VotoRepositoryEmArquivo(arquivo, "Fila de sincronização local"),
                new VotoRepositoryEmArquivo(dados.resolve("votos-historico.tsv"), "Histórico local de votos"),
                null, false, config.mensagem(), false);
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
        identificadorLocalOffline = com.urnacidada.repository.CpfIdentificador
                .emailFirebase(cpf).substring(4, 64);
        try {
            if (escolha == 0) return autenticacao.entrar(cpf);
            return autenticacao.cadastrar(cpf);
        } catch (com.urnacidada.repository.RepositorioException e) {
            if (falhaDeConexao(e.getMessage())) {
                int escolhaOffline = DialogosUrna.opcoes(null,
                        "O Firebase está sem conexão. Você pode continuar neste computador; esse voto ficará local e não será enviado automaticamente ao Firestore.",
                        "Votação local offline", JOptionPane.WARNING_MESSAGE,
                        new Object[]{"Continuar neste computador", "Cancelar"}, "Continuar neste computador");
                if (escolhaOffline == 0) {
                    modoLocalOffline = true;
                    return null;
                }
            } else {
                identificadorLocalOffline = null;
                DialogosUrna.mensagem(null, e.getMessage(), "Autenticação Firebase", JOptionPane.ERROR_MESSAGE);
            }
            if (!modoLocalOffline) identificadorLocalOffline = null;
            return null;
        }
    }

    private static boolean falhaDeConexao(String mensagem) {
        if (mensagem == null) return false;
        return mensagem.contains("Sem conexão") || mensagem.contains("Tempo esgotado")
                || mensagem.contains("indisponível") || mensagem.contains("interrompida");
    }

    private static CredencialAdministrador pedirCredencialAdministrador() {
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
        return new CredencialAdministrador(email.getText().strip(), senha.getPassword());
    }

    private static void executarAdministracao(ConfiguracaoUrna config, Path dados) {
        TravaInstancia travaAdmin = null;
        CredencialAdministrador credencial = null;
        try {
            int inicio = DialogosUrna.opcoes(null, "Escolha uma função da administração.",
                    "Administração da urna", JOptionPane.QUESTION_MESSAGE,
                    new Object[]{"Apuração local offline", "Administrar votação no Firebase", "Cancelar"},
                    "Apuração local offline");
            if (inicio == 2 || inicio < 0) return;
            if (inicio == 0) {
                credencial = pedirCredencialAdministrador();
                if (credencial == null) return;
                if (!AcessoAdministradorLocal.verificar(dados, credencial.email(), credencial.senha())) {
                    DialogosUrna.mensagem(null,
                            "A apuração offline fica disponível depois que a conta administrativa acessa a urna uma vez com o Firebase conectado.",
                            "Acesso offline não habilitado", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                travaAdmin = adquirirTravaAdmin(dados);
                if (travaAdmin == null) return;
                mostrarApuracaoLocal(dados);
                return;
            }

            if (!config.valida()) {
                DialogosUrna.mensagem(null, "Configure o Firebase para administrar a votação online.",
                        "Firebase não configurado", JOptionPane.ERROR_MESSAGE);
                return;
            }
            credencial = pedirCredencialAdministrador();
            if (credencial == null) return;
            AutenticacaoFirebase autenticacao = new AutenticacaoFirebase(config.apiKey());
            AutenticacaoFirebase.Sessao usuario;
            try {
                usuario = autenticacao.entrarAdministrador(credencial.email(), credencial.senha());
            } catch (com.urnacidada.repository.RepositorioException erro) {
                if (AcessoAdministradorLocal.verificar(dados, credencial.email(), credencial.senha())) {
                    DialogosUrna.mensagem(null,
                            "Sem acesso ao Firebase. A senha local foi aceita para consultar somente os votos deste computador.",
                            "Apuração local", JOptionPane.INFORMATION_MESSAGE);
                    travaAdmin = adquirirTravaAdmin(dados);
                    if (travaAdmin != null) mostrarApuracaoLocal(dados);
                } else {
                    DialogosUrna.mensagem(null, erro.getMessage(), "Acesso administrativo", JOptionPane.ERROR_MESSAGE);
                }
                return;
            }

            FirestoreAdminClient administrador = new FirestoreAdminClient(config.projectId(), config.apiKey(), usuario);
            boolean ehAdmin;
            try {
                ehAdmin = administrador.ehAdministrador();
            } catch (com.urnacidada.repository.RepositorioException erro) {
                if (AcessoAdministradorLocal.verificar(dados, credencial.email(), credencial.senha())) {
                    travaAdmin = adquirirTravaAdmin(dados);
                    if (travaAdmin != null) mostrarApuracaoLocal(dados);
                } else {
                    DialogosUrna.mensagem(null, erro.getMessage(), "Acesso administrativo", JOptionPane.ERROR_MESSAGE);
                }
                return;
            }
            if (!ehAdmin) {
                DialogosUrna.mensagem(null,
                        "Esta conta não tem permissão de administrador no Firestore.",
                        "Acesso negado", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                AcessoAdministradorLocal.registrar(dados, credencial.email(), credencial.senha());
            } catch (Exception erro) {
                LogUrna.logger().warning("Não foi possível habilitar a autenticação administrativa offline.");
            }
            travaAdmin = adquirirTravaAdmin(dados);
            if (travaAdmin == null) return;

            int acao = DialogosUrna.opcoes(null, "Conta administrativa confirmada.",
                    "Administração da urna", JOptionPane.QUESTION_MESSAGE,
                    new Object[]{"Apuração local deste computador", "Zerar votação no Firebase", "Cancelar"},
                    "Apuração local deste computador");
            if (acao == 0) {
                mostrarApuracaoLocal(dados);
                return;
            }
            if (acao != 1 || !confirmarApagamento()) return;

            FirestoreAdminClient.ResultadoLimpeza limpeza = administrador.limparVotacao();
            int arquivos = CaminhosAplicacao.apagarArquivosDeVoto(dados);
            DialogosUrna.mensagem(null,
                    "Nova votação preparada. " + limpeza.sessoesRemovidas() + " sessões removidas; "
                            + limpeza.participantesRearmados() + " participantes liberados para votar novamente; "
                            + "os nove contadores foram zerados. " + arquivos
                            + " arquivos de voto deste computador foram apagados. As contas e coleções foram mantidas.",
                    "Votação reiniciada", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            DialogosUrna.mensagem(null,
                    e.getMessage() == null ? "Não foi possível concluir a limpeza." : e.getMessage(),
                    "Limpeza incompleta", JOptionPane.ERROR_MESSAGE);
        } finally {
            if (travaAdmin != null) try { travaAdmin.close(); } catch (IOException ignored) { }
            if (credencial != null) Arrays.fill(credencial.senha(), '\0');
        }
    }

    private static TravaInstancia adquirirTravaAdmin(Path dados) throws IOException {
        TravaInstancia trava = TravaInstancia.tentar(dados.resolve(".urna-instancia.lock"));
        if (trava == null) {
            DialogosUrna.mensagem(null, "Feche as outras janelas da urna antes de consultar ou zerar os dados.",
                    "Urna em uso", JOptionPane.WARNING_MESSAGE);
        }
        return trava;
    }

    private static void mostrarApuracaoLocal(Path dados) {
        try {
            JTextArea texto = new JTextArea(ApuracaoLocal.gerarRelatorio(dados), 23, 52);
            texto.setEditable(false);
            texto.setCaretPosition(0);
            DialogosUrna.mensagem(null, new JScrollPane(texto), "Apuração local offline",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception erro) {
            DialogosUrna.mensagem(null,
                    erro.getMessage() == null ? "Não foi possível ler os votos locais." : erro.getMessage(),
                    "Apuração local", JOptionPane.ERROR_MESSAGE);
        }
    }

    private record CredencialAdministrador(String email, char[] senha) { }

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
                "Isso remove as sessões de votação, zera os contadores e libera novamente os participantes no Firestore. "
                        + "As contas de CPF, os documentos de participantes, os administradores e as coleções serão mantidos. "
                        + "Também apaga o histórico de votos deste computador. Outros computadores mantêm seus arquivos; "
                        + "limpe as filas pendentes antes de reconectá-los para não enviar votos da rodada anterior.",
                "Preparar nova votação", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
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
