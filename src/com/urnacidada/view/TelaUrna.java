package com.urnacidada.view;

import com.urnacidada.model.Eleicao;
import com.urnacidada.repository.VotoRepository;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/** Janela principal: junta o painel, o controlador e as teclas do computador. */
@SuppressWarnings("serial")
public final class TelaUrna extends JFrame {

    private static final String DICAS = """
            PARE, CONFIRA, COMPARTILHE COM CUIDADO

            1. Confira quem publicou e procure a fonte original.
            2. Compare a informação com fontes independentes.
            3. Verifique data, contexto e se a imagem ou o vídeo aparece em notícias antigas.
            4. Áudios, imagens e vídeos podem ser editados ou gerados por IA.
            5. Desconfie de urgência e de afirmações sem evidência.
            6. Na dúvida, não compartilhe; consulte fontes oficiais e serviços de checagem.

            Esta urna usa candidatos fictícios e serve apenas para fins educativos.
            """;

    private final VotoRepository repositorio;
    private final ExecutorService gravacao = Executors.newSingleThreadExecutor(tarefa -> {
        Thread t = new Thread(tarefa, "gravacao-de-votos");
        t.setDaemon(true);
        return t;
    });
    private final PainelUrna painel;
    private final String dicaInicial;
    private final boolean umaSessaoPorConta;
    private ControladorUrna controlador;
    private final Timer timerEstado;
    private final Timer timerProximoParticipante;
    private final Runnable aoFechar;
    private final Runnable aoReiniciar;
    private boolean reinicioAutomatico;

    public TelaUrna(Eleicao eleicao, VotoRepository repositorio) {
        this(eleicao, repositorio, repositorio.dicaPersistencia());
    }

    public TelaUrna(Eleicao eleicao, VotoRepository repositorio, String dicaInicial) {
        this(eleicao, repositorio, dicaInicial, () -> java.util.UUID.randomUUID().toString(), false);
    }

    public TelaUrna(Eleicao eleicao, VotoRepository repositorio, String dicaInicial,
                    Supplier<String> idSessao, boolean umaSessaoPorConta) {
        this(eleicao, repositorio, dicaInicial, idSessao, umaSessaoPorConta, () -> { }, () -> { });
    }

    public TelaUrna(Eleicao eleicao, VotoRepository repositorio, String dicaInicial,
                    Supplier<String> idSessao, boolean umaSessaoPorConta,
                    Runnable aoFechar, Runnable aoReiniciar) {
        super("Urna Cidadã · Simulador educativo");
        this.repositorio = repositorio;
        this.dicaInicial = dicaInicial;
        this.umaSessaoPorConta = umaSessaoPorConta;
        this.aoFechar = aoFechar;
        this.aoReiniciar = aoReiniciar;

        painel = new PainelUrna(new RepasseDeAcoes(), repositorio.descricao());
        painel.encerrarNoFim(true);
        controlador = new ControladorUrna(eleicao, repositorio, gravacao, SwingUtilities::invokeLater,
                this::exibirEstado, this::mostrarDicas, idSessao, umaSessaoPorConta);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setContentPane(painel);
        setMinimumSize(new Dimension(1120, 680));
        setSize(1240, 720);
        setLocationRelativeTo(null);
        configurarTeclado();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                encerrar();
                TelaUrna.this.aoFechar.run();
                if (reinicioAutomatico) TelaUrna.this.aoReiniciar.run();
            }
        });

        timerEstado = new Timer(1000, ev -> painel.atualizarPersistencia(
                repositorio.estadoPersistencia(), dicaInicial));
        timerProximoParticipante = new Timer(5000, ev -> {
            reinicioAutomatico = true;
            dispose();
        });
        timerProximoParticipante.setRepeats(false);
        timerEstado.start();

    }

    /** Publica o primeiro estado depois de mostrar a janela. */
    public void iniciar() {
        controlador.novaVotacao();
    }

    private void exibirEstado(EstadoTela estado) {
        painel.exibir(estado);
        painel.atualizarPersistencia(repositorio.estadoPersistencia(), dicaInicial);
        if (estado.finalizada() && !timerProximoParticipante.isRunning()) timerProximoParticipante.restart();
    }

    public void mostrarAvisoInicial() {
        repositorio.avisoDaUltimaGravacao().ifPresent(texto -> JOptionPane.showMessageDialog(this, texto,
                "Modo de armazenamento", JOptionPane.INFORMATION_MESSAGE));
    }

    /** O painel é criado antes do controlador; este objeto liga os dois sem referência circular. */
    private final class RepasseDeAcoes implements AcoesUrna {
        @Override public void digitar(char digito) { controlador.digitar(digito); }
        @Override public void branco() { controlador.branco(); }
        @Override public void corrigir() { controlador.corrigir(); }
        @Override public void confirmar() { controlador.confirmar(); }
        @Override public void novaVotacao() {
            if (controlador.estado().finalizada()) TelaUrna.this.dispose();
            else controlador.novaVotacao();
        }
        @Override public void mostrarDicas() { controlador.mostrarDicas(); }
    }

    private void configurarTeclado() {
        JRootPane raiz = getRootPane();
        for (char d = '0'; d <= '9'; d++) {
            char digito = d;
            vincular(raiz, KeyStroke.getKeyStroke(digito), "digito-" + digito, () -> {
                SomUrna.curto(); controlador.digitar(digito);
            });
        }
        vincular(raiz, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "confirma", () -> { SomUrna.curto(); controlador.confirmar(); });
        vincular(raiz, KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "corrige", () -> { SomUrna.curto(); controlador.corrigir(); });
        vincular(raiz, KeyStroke.getKeyStroke(KeyEvent.VK_B, 0), "branco", () -> { SomUrna.curto(); controlador.branco(); });
    }

    private static void vincular(JRootPane raiz, KeyStroke tecla, String nome, Runnable acao) {
        raiz.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(tecla, nome);
        raiz.getActionMap().put(nome, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                acao.run();
            }
        });
    }

    private void mostrarDicas() {
        JTextArea area = new JTextArea(DICAS, 15, 46);
        area.setWrapStyleWord(true);
        area.setLineWrap(true);
        area.setEditable(false);
        area.setFont(new Font("SansSerif", Font.PLAIN, 15));
        area.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Dicas de verificação",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void encerrar() {
        gravacao.shutdown();
        timerEstado.stop();
        timerProximoParticipante.stop();
        if (repositorio instanceof AutoCloseable recurso) {
            try {
                recurso.close();
            } catch (Exception ignorada) {
                // a janela já foi fechada; não há o que mostrar ao participante
            }
        }
    }
}
