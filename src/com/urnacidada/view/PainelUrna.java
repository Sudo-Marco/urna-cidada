package com.urnacidada.view;

import com.urnacidada.view.EstadoTela.Modo;
import com.urnacidada.view.EstadoTela.TipoMensagem;
import com.urnacidada.model.Cargo;
import com.urnacidada.model.Candidato;
import com.urnacidada.model.Eleicao;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.Objects;

/**
 * Visão da urna: desenha a tela e o teclado. Não tem regra de votação; apenas repassa
 * os cliques para {@link AcoesUrna} e mostra o {@link EstadoTela} recebido em {@link #exibir}.
 */
@SuppressWarnings("serial")
public final class PainelUrna extends JPanel {

    private static final Color FUNDO = new Color(0xEEF2F7);
    private static final Color AZUL = new Color(0x1B3857);
    private static final Color TEXTO = new Color(0x1F2933);
    private static final Color CINZA = new Color(0x5B6670);
    private static final Color ALERTA_TITULO = new Color(0xB00020);
    private static final Color COR_SUCESSO = new Color(0x1E7B4F);
    private static final Color COR_AVISO = new Color(0x8A5200);
    private static final Color COR_ERRO = new Color(0xB00020);

    private static final int LARGURA_DO_TECLADO = 400;

    private static final SimpleAttributeSet CENTRALIZADO = new SimpleAttributeSet();

    static {
        StyleConstants.setAlignment(CENTRALIZADO, StyleConstants.ALIGN_CENTER);
    }

    private final AcoesUrna acoes;
    private final Eleicao eleicao;
    private final JLabel persistencia = rotulo(11, Font.PLAIN, CINZA);
    private boolean fimTocado;
    private boolean encerrarNoFim;

    private final JLabel progresso = rotulo(13, Font.BOLD, CINZA);
    private final JLabel instrucaoCargo = rotulo(13, Font.BOLD, CINZA);
    private final JLabel cargo = rotulo(26, Font.BOLD, AZUL);
    private final JPanel caixas = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
    private final JTextPane titulo = criarTexto();
    private final JTextPane detalhe = criarTexto();
    private final JTextPane explicacao = criarTexto();
    private final JTextPane mensagem = criarTexto();

    private final JButton[] teclas = new JButton[10];
    private final JButton btnBranco = botao("BRANCO", new Color(0x4F5B66));
    private final JButton btnCorrige = botao("CORRIGE", new Color(0xAA5C0C));
    private final JButton btnConfirma = botao("CONFIRMA", new Color(0x1E7B4F));
    private final JButton btnNova = botao("Iniciar nova votação", AZUL);
    private final JButton btnDicas = botao("Dicas contra desinformação", AZUL);

    public PainelUrna(AcoesUrna acoes, String descricaoDaGravacao) {
        this(acoes, descricaoDaGravacao, Eleicao.exemplo());
    }

    public PainelUrna(AcoesUrna acoes, String descricaoDaGravacao, Eleicao eleicao) {
        this.acoes = Objects.requireNonNull(acoes, "acoes");
        this.eleicao = Objects.requireNonNull(eleicao, "eleicao");
        setLayout(new BorderLayout(0, 14));
        setBackground(FUNDO);
        setOpaque(true);
        setBorder(new EmptyBorder(18, 24, 14, 24));
        add(criarCabecalho(), BorderLayout.NORTH);
        add(criarCorpo(), BorderLayout.CENTER);
        add(criarRodape(descricaoDaGravacao), BorderLayout.SOUTH);
    }

    /** Redesenha a tela de acordo com o estado. Deve ser chamado na thread da tela. */
    public void exibir(EstadoTela e) {
        progresso.setText(e.progresso());
        cargo.setText(e.cargo());
        montarCaixas(e.posicoes(), e.digitado());

        definirTexto(titulo, e.titulo(), fonteTitulo(e.modo()), corTitulo(e.modo()));
        definirTexto(detalhe, e.detalhe(), fonteDetalhe(e.modo()), corDetalhe(e.modo()));
        definirTexto(explicacao, e.explicacao(), new Font("SansSerif", Font.PLAIN, 13), CINZA);
        definirTexto(mensagem, e.mensagem(), new Font("SansSerif", Font.BOLD, 14), corMensagem(e.tipoMensagem()));

        for (JButton tecla : teclas) {
            tecla.setEnabled(e.controlesHabilitados());
        }
        btnBranco.setEnabled(e.controlesHabilitados());
        btnCorrige.setEnabled(e.controlesHabilitados());
        btnConfirma.setEnabled(e.controlesHabilitados());
        btnNova.setText(e.finalizada() && encerrarNoFim ? "Encerrar" : "Iniciar nova votação");
        btnNova.setVisible(e.finalizada() || e.modo() == Modo.AGUARDANDO_INICIO);
        if (e.finalizada() && !fimTocado) SomUrna.fim();
        fimTocado = e.finalizada();

        revalidate();
        repaint();
    }

    public void atualizarPersistencia(String estado, String caminho) {
        persistencia.setText(estado);
        persistencia.setToolTipText(caminho);
    }

    public void encerrarNoFim(boolean valor) { encerrarNoFim = valor; }

    // ------------------------------------------------------------------ montagem

    private JPanel criarCabecalho() {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setOpaque(false);
        JLabel nome = rotulo(26, Font.BOLD, AZUL);
        nome.setText("URNA CIDADÃ");
        nome.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel aviso = rotulo(12, Font.BOLD, new Color(0x8E402D));
        aviso.setText("SIMULAÇÃO EDUCATIVA · NÃO É UMA URNA OFICIAL");
        aviso.setHorizontalAlignment(SwingConstants.CENTER);
        p.add(nome, BorderLayout.NORTH);
        p.add(aviso, BorderLayout.SOUTH);
        return p;
    }

    private JPanel criarCorpo() {
        JPanel corpo = new JPanel(new BorderLayout(14, 0));
        corpo.setOpaque(false);
        JPanel teclado = criarTeclado();
        teclado.setPreferredSize(new Dimension(LARGURA_DO_TECLADO, 10)); // largura fixa: não muda com o texto
        corpo.add(criarPainelCandidatos(), BorderLayout.WEST);
        corpo.add(criarTela(), BorderLayout.CENTER);
        corpo.add(teclado, BorderLayout.EAST);
        return corpo;
    }

    private JScrollPane criarPainelCandidatos() {
        JPanel lista = new JPanel();
        lista.setLayout(new javax.swing.BoxLayout(lista, javax.swing.BoxLayout.Y_AXIS));
        lista.setBackground(Color.WHITE);
        lista.setBorder(new EmptyBorder(12, 10, 12, 8));

        JLabel tituloLista = rotulo(13, Font.BOLD, AZUL);
        tituloLista.setText("NÚMEROS DOS CANDIDATOS");
        tituloLista.setAlignmentX(0f);
        lista.add(tituloLista);
        lista.add(javax.swing.Box.createVerticalStrut(8));

        for (Cargo cargo : eleicao.cargos()) {
            JLabel tituloCargo = rotulo(11, Font.BOLD, CINZA);
            tituloCargo.setText(cargo.nome().toUpperCase(java.util.Locale.ROOT));
            tituloCargo.setAlignmentX(0f);
            lista.add(tituloCargo);
            if (cargo.candidatos().isEmpty()) {
                JLabel semCandidaturas = rotulo(11, Font.ITALIC, CINZA);
                semCandidaturas.setText("Sem candidaturas");
                semCandidaturas.setAlignmentX(0f);
                lista.add(semCandidaturas);
            } else {
                for (Candidato candidato : cargo.candidatos()) {
                    JLabel item = rotulo(12, Font.PLAIN, TEXTO);
                    item.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
                    item.setText(candidato.numero() + "  " + candidato.nome());
                    item.setAlignmentX(0f);
                    lista.add(item);
                }
            }
            lista.add(javax.swing.Box.createVerticalStrut(8));
        }

        JScrollPane painel = new JScrollPane(lista);
        painel.setPreferredSize(new Dimension(232, 10));
        painel.setMinimumSize(new Dimension(216, 10));
        painel.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        painel.getVerticalScrollBar().setUnitIncrement(14);
        painel.setBorder(BorderFactory.createLineBorder(new Color(0xBEC8D2), 1));
        return painel;
    }

    private JPanel criarTela() {
        JPanel tela = new JPanel(new GridBagLayout());
        tela.setBackground(Color.WHITE);
        tela.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xBEC8D2), 2),
                new EmptyBorder(16, 18, 14, 18)));

        progresso.setHorizontalAlignment(SwingConstants.CENTER);
        instrucaoCargo.setText("SEU VOTO PARA");
        instrucaoCargo.setHorizontalAlignment(SwingConstants.CENTER);
        cargo.setHorizontalAlignment(SwingConstants.CENTER);
        caixas.setOpaque(false);

        linha(tela, progresso, 0, 0, 0, 0);
        linha(tela, instrucaoCargo, 1, 0, 20, 0);
        linha(tela, cargo, 2, 0, 0, 4);
        linha(tela, caixas, 3, 0, 0, 8);
        linha(tela, titulo, 4, 0, 66, 0);
        linha(tela, detalhe, 5, 0, 34, 4);
        linha(tela, explicacao, 6, 0, 56, 0);
        JLabel comandos = rotulo(11, Font.PLAIN, CINZA);
        comandos.setText("Aperte VERDE para CONFIRMAR · LARANJA para CORRIGIR");
        comandos.setHorizontalAlignment(SwingConstants.CENTER);
        linha(tela, comandos, 7, 0, 24, 0);
        linha(tela, new JLabel(), 8, 1.0, 0, 0); // empurra a mensagem para o rodapé da tela
        linha(tela, mensagem, 9, 0, 84, 6);
        return tela;
    }

    private static void linha(JPanel pai, java.awt.Component filho, int linha, double pesoVertical,
                              int alturaMinima, int espacoAbaixo) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = linha;
        c.weightx = 1;
        c.weighty = pesoVertical;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, espacoAbaixo, 0);
        if (alturaMinima > 0) {
            filho.setMinimumSize(new Dimension(10, alturaMinima));
            filho.setPreferredSize(new Dimension(10, alturaMinima));
        }
        pai.add(filho, c);
    }

    private JPanel criarTeclado() {
        JPanel teclado = new JPanel(new BorderLayout(0, 12));
        teclado.setOpaque(false);

        JPanel numeros = new JPanel(new GridLayout(4, 3, 8, 8));
        numeros.setOpaque(false);
        int[] ordem = {1, 2, 3, 4, 5, 6, 7, 8, 9, -1, 0, -1};
        for (int n : ordem) {
            if (n < 0) {
                numeros.add(new JLabel());
                continue;
            }
            char digito = (char) ('0' + n);
            JButton tecla = botao(String.valueOf(n), Color.WHITE);
            tecla.setForeground(TEXTO);
            tecla.setFont(new Font("SansSerif", Font.BOLD, 26));
            tecla.addActionListener(ev -> { SomUrna.curto(); acoes.digitar(digito); });
            teclas[n] = tecla;
            numeros.add(tecla);
        }

        JPanel acoesPanel = new JPanel(new GridLayout(1, 3, 8, 8));
        acoesPanel.setOpaque(false);
        acoesPanel.setPreferredSize(new Dimension(10, 62));
        btnBranco.addActionListener(ev -> { SomUrna.curto(); acoes.branco(); });
        btnCorrige.addActionListener(ev -> { SomUrna.curto(); acoes.corrigir(); });
        btnConfirma.addActionListener(ev -> { SomUrna.curto(); acoes.confirmar(); });
        acoesPanel.add(btnBranco);
        acoesPanel.add(btnCorrige);
        acoesPanel.add(btnConfirma);

        teclado.add(numeros, BorderLayout.CENTER);
        teclado.add(acoesPanel, BorderLayout.SOUTH);
        return teclado;
    }

    private JPanel criarRodape(String descricaoDaGravacao) {
        JPanel rodape = new JPanel(new BorderLayout(10, 0));
        rodape.setOpaque(false);
        JLabel gravacao = rotulo(12, Font.PLAIN, CINZA);
        gravacao.setText("Gravação: " + descricaoDaGravacao);
        gravacao.setHorizontalAlignment(SwingConstants.CENTER);
        persistencia.setHorizontalAlignment(SwingConstants.CENTER);
        JButton som = botao("Som: ligado", AZUL);
        som.addActionListener(ev -> {
            SomUrna.silenciar(!SomUrna.silenciado());
            som.setText(SomUrna.silenciado() ? "Som: desligado" : "Som: ligado");
        });

        btnDicas.addActionListener(ev -> acoes.mostrarDicas());
        btnNova.addActionListener(ev -> acoes.novaVotacao());
        btnNova.setVisible(false);

        JPanel informacao = new JPanel(new GridLayout(2, 1));
        informacao.setOpaque(false);
        informacao.add(gravacao);
        informacao.add(persistencia);
        JPanel acoesRodape = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        acoesRodape.setOpaque(false);
        acoesRodape.add(som);
        acoesRodape.add(btnDicas);
        rodape.add(acoesRodape, BorderLayout.WEST);
        rodape.add(informacao, BorderLayout.CENTER);
        rodape.add(btnNova, BorderLayout.EAST);
        return rodape;
    }

    // ------------------------------------------------------------------ detalhes visuais

    private void montarCaixas(int quantidade, String digitado) {
        caixas.removeAll();
        for (int i = 0; i < quantidade; i++) {
            String texto = i < digitado.length() ? String.valueOf(digitado.charAt(i)) : "";
            JLabel caixa = new JLabel(texto, SwingConstants.CENTER);
            caixa.setFont(new Font("SansSerif", Font.BOLD, 30));
            caixa.setForeground(TEXTO);
            caixa.setOpaque(true);
            caixa.setBackground(Color.WHITE);
            caixa.setBorder(BorderFactory.createLineBorder(AZUL, 2));
            caixa.setPreferredSize(new Dimension(42, 52));
            caixas.add(caixa);
        }
        caixas.setVisible(quantidade > 0);
        caixas.revalidate();
        caixas.repaint();
    }

    private static Font fonteTitulo(Modo modo) {
        return switch (modo) {
            case AGUARDANDO_INICIO -> new Font("SansSerif", Font.BOLD, 24);
            case FIM -> new Font("SansSerif", Font.BOLD, 46);
            case CANDIDATO -> new Font("SansSerif", Font.BOLD, 24);
            case NUMERO_ERRADO, BRANCO -> new Font("SansSerif", Font.BOLD, 28);
            case DIGITANDO -> new Font("SansSerif", Font.PLAIN, 18);
        };
    }

    private static Color corTitulo(Modo modo) {
        return modo == Modo.NUMERO_ERRADO ? ALERTA_TITULO : TEXTO;
    }

    private static Font fonteDetalhe(Modo modo) {
        return modo == Modo.NUMERO_ERRADO
                ? new Font("SansSerif", Font.BOLD, 20)
                : new Font("SansSerif", Font.PLAIN, 17);
    }

    private static Color corDetalhe(Modo modo) {
        return modo == Modo.NUMERO_ERRADO ? ALERTA_TITULO : TEXTO;
    }

    private static Color corMensagem(TipoMensagem tipo) {
        return switch (tipo) {
            case SUCESSO -> COR_SUCESSO;
            case AVISO -> COR_AVISO;
            case ERRO -> COR_ERRO;
            case INFO, NENHUMA -> AZUL;
        };
    }

    private static JLabel rotulo(int tamanho, int estilo, Color cor) {
        JLabel l = new JLabel();
        l.setFont(new Font("SansSerif", estilo, tamanho));
        l.setForeground(cor);
        return l;
    }

    private static JTextPane criarTexto() {
        JTextPane p = new JTextPane();
        p.setEditable(false);
        p.setFocusable(false);
        p.setOpaque(false);
        p.setBorder(null);
        return p;
    }

    private static void definirTexto(JTextPane painel, String texto, Font fonte, Color cor) {
        painel.setFont(fonte);
        painel.setForeground(cor);
        painel.setText(texto);
        StyledDocument doc = painel.getStyledDocument();
        doc.setParagraphAttributes(0, doc.getLength(), CENTRALIZADO, false);
    }

    private static JButton botao(String texto, Color fundo) {
        JButton b = new JButton(texto);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setBackground(fundo);
        b.setForeground(Color.WHITE);
        b.setOpaque(true);
        b.setFocusable(false); // as teclas do computador ficam por conta da janela
        b.setMargin(new Insets(4, 6, 4, 6));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x3A4750), 1), new EmptyBorder(4, 8, 4, 8)));
        return b;
    }
}
