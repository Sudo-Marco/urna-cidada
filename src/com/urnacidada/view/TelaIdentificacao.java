package com.urnacidada.view;

import com.urnacidada.repository.CpfIdentificador;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.text.MaskFormatter;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.ParseException;
import java.util.concurrent.atomic.AtomicReference;

/** Janela inicial para escolher o tipo de acesso e informar o CPF em uma única tela. */
@SuppressWarnings("serial")
public final class TelaIdentificacao extends JDialog {

    private static final Color MARINHO = new Color(0x17324D);
    private static final Color VERDE = new Color(0x18794E);
    private static final Color TEXTO = new Color(0x1F2937);
    private static final Color CINZA = new Color(0x667085);
    private static final Color FUNDO = new Color(0xF4F7FA);
    private static final Color BORDA = new Color(0xD7DEE7);

    private final JFormattedTextField campoCpf = criarCampoCpf();
    private final JToggleButton entrar = criarOpcao("ENTRAR", "Já tenho cadastro", "Acesse com o CPF que já cadastrou.");
    private final JToggleButton primeiroAcesso = criarOpcao("PRIMEIRO ACESSO", "Quero me cadastrar", "Crie seu cadastro e comece a votar.");
    private final JLabel orientacao = new JLabel();
    private final JLabel erro = new JLabel(" ");
    private Resultado resultado;

    private TelaIdentificacao() {
        super((java.awt.Frame) null, "Urna Cidadã | Identificação", true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(940, 610));
        setSize(1020, 660);
        setContentPane(criarConteudo());
        setLocationRelativeTo(null);
        getRootPane().setDefaultButton(botaoContinuar);
        entrar.setSelected(true);
        atualizarSelecao();
        entrar.addActionListener(e -> atualizarSelecao());
        primeiroAcesso.addActionListener(e -> atualizarSelecao());
        addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent e) {
                campoCpf.requestFocusInWindow();
            }
        });
    }

    private final JButton botaoContinuar = criarBotaoPrimario();

    /** Abre a tela na thread do Swing e devolve null se o usuário cancelar. */
    public static Resultado exibir() {
        AtomicReference<Resultado> retorno = new AtomicReference<>();
        Runnable abrir = () -> {
            TelaIdentificacao tela = new TelaIdentificacao();
            tela.setVisible(true);
            retorno.set(tela.resultado);
        };
        try {
            if (SwingUtilities.isEventDispatchThread()) abrir.run();
            else SwingUtilities.invokeAndWait(abrir);
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível abrir a identificação da urna.", e);
        }
        return retorno.get();
    }

    private JPanel criarConteudo() {
        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(FUNDO);
        raiz.add(criarPainelMarca(), BorderLayout.WEST);
        raiz.add(criarPainelFormulario(), BorderLayout.CENTER);
        return raiz;
    }

    private JPanel criarPainelMarca() {
        JPanel painel = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics grafico) {
                Graphics2D g = (Graphics2D) grafico.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setPaint(new GradientPaint(0, 0, new Color(0x122B42),
                        getWidth(), getHeight(), new Color(0x20506A)));
                g.fillRect(0, 0, getWidth(), getHeight());

                // Texturas discretas nos cantos para dar profundidade sem competir com o texto.
                g.setColor(new Color(0xB7F1D5));
                for (int linha = 0; linha < 5; linha++) {
                    for (int coluna = 0; coluna < 5; coluna++) {
                        g.setComposite(java.awt.AlphaComposite.getInstance(
                        java.awt.AlphaComposite.SRC_OVER, 0.25f));
                        g.fillOval(getWidth() - 112 + coluna * 15, 24 + linha * 15, 4, 4);
                        g.fillOval(24 + coluna * 15, getHeight() - 104 + linha * 15, 4, 4);
                    }
                }
                g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.22f));
                g.setColor(new Color(0x66D6A6));
                g.drawOval(getWidth() - 112, 56, 116, 116);
                g.drawOval(12, getHeight() - 174, 112, 112);
                g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.17f));
                g.drawRoundRect(getWidth() - 82, 188, 46, 28, 8, 8);
                g.drawLine(getWidth() - 72, 198, getWidth() - 60, 198);
                g.drawLine(getWidth() - 72, 205, getWidth() - 52, 205);
                g.dispose();
            }
        };
        painel.setOpaque(false);
        painel.setPreferredSize(new Dimension(340, 0));
        painel.setBorder(BorderFactory.createEmptyBorder(32, 30, 26, 30));

        JPanel marca = new JPanel();
        marca.setOpaque(false);
        marca.setLayout(new javax.swing.BoxLayout(marca, javax.swing.BoxLayout.Y_AXIS));
        JPanel areaSelo = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));
        areaSelo.setOpaque(false);
        JLabel selo = new JLabel("U", SwingConstants.CENTER);
        selo.setOpaque(true);
        selo.setBackground(new Color(0xE5F5EE));
        selo.setForeground(VERDE);
        selo.setFont(new Font("SansSerif", Font.BOLD, 29));
        selo.setPreferredSize(new Dimension(58, 58));
        selo.setBorder(BorderFactory.createLineBorder(new Color(0xD6EEE2), 1, true));
        areaSelo.add(selo);
        marca.add(areaSelo);
        marca.add(javax.swing.Box.createVerticalStrut(13));
        JLabel nome = rotuloComSombra("URNA CIDADÃ", 18, Font.BOLD, Color.WHITE);
        nome.setAlignmentX(CENTER_ALIGNMENT);
        marca.add(nome);
        marca.add(javax.swing.Box.createVerticalStrut(4));
        JLabel categoria = rotulo("SIMULAÇÃO EDUCATIVA", 10, Font.BOLD, new Color(0xC4D2E0));
        categoria.setAlignmentX(CENTER_ALIGNMENT);
        marca.add(categoria);
        painel.add(marca, BorderLayout.NORTH);

        JPanel meio = new JPanel();
        meio.setOpaque(false);
        meio.setLayout(new javax.swing.BoxLayout(meio, javax.swing.BoxLayout.Y_AXIS));
        meio.add(javax.swing.Box.createVerticalGlue());
        JLabel chamada = rotuloComSombra("Sua participação", 27, Font.BOLD, Color.WHITE);
        chamada.setAlignmentX(CENTER_ALIGNMENT);
        chamada.setHorizontalAlignment(SwingConstants.CENTER);
        chamada.setPreferredSize(new Dimension(270, 42));
        chamada.setMaximumSize(new Dimension(270, 42));
        meio.add(chamada);
        meio.add(javax.swing.Box.createVerticalStrut(12));
        JPanel destaque = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));
        destaque.setOpaque(false);
        JPanel linhaDestaque = new JPanel();
        linhaDestaque.setBackground(new Color(0x66D6A6));
        linhaDestaque.setPreferredSize(new Dimension(46, 4));
        destaque.add(linhaDestaque);
        meio.add(destaque);
        meio.add(javax.swing.Box.createVerticalStrut(10));
        JTextPane descricao = criarDescricaoCentralizada();
        descricao.setAlignmentX(CENTER_ALIGNMENT);
        descricao.setPreferredSize(new Dimension(280, 66));
        descricao.setMaximumSize(new Dimension(280, 66));
        meio.add(descricao);
        meio.add(javax.swing.Box.createVerticalStrut(24));
        JPanel destaques = new JPanel();
        destaques.setOpaque(false);
        destaques.setLayout(new javax.swing.BoxLayout(destaques, javax.swing.BoxLayout.Y_AXIS));
        destaques.setMaximumSize(new Dimension(280, 138));
        destaques.setAlignmentX(CENTER_ALIGNMENT);
        destaques.add(item("01", "Identificação pelo CPF"));
        destaques.add(javax.swing.Box.createVerticalStrut(10));
        destaques.add(item("02", "Sem senha para participantes"));
        destaques.add(javax.swing.Box.createVerticalStrut(10));
        destaques.add(item("03", "Candidatos fictícios"));
        meio.add(destaques);
        meio.add(javax.swing.Box.createVerticalGlue());
        painel.add(meio, BorderLayout.CENTER);

        JLabel rodape = rotulo("PROJETO ACADÊMICO", 10, Font.BOLD, new Color(0xC4D2E0));
        rodape.setHorizontalAlignment(SwingConstants.CENTER);
        painel.add(rodape, BorderLayout.SOUTH);
        return painel;
    }

    private JPanel criarPainelFormulario() {
        JPanel painel = new JPanel(new BorderLayout(0, 24));
        painel.setBackground(FUNDO);
        painel.setBorder(BorderFactory.createEmptyBorder(46, 48, 34, 48));

        JPanel cabecalho = new JPanel();
        cabecalho.setOpaque(false);
        cabecalho.setLayout(new javax.swing.BoxLayout(cabecalho, javax.swing.BoxLayout.Y_AXIS));
        cabecalho.add(rotulo("Bem-vindo(a)", 31, Font.BOLD, TEXTO));
        cabecalho.add(javax.swing.Box.createVerticalStrut(8));
        cabecalho.add(rotulo("Escolha como deseja continuar.", 15, Font.PLAIN, CINZA));
        painel.add(cabecalho, BorderLayout.NORTH);

        JPanel formulario = new JPanel();
        formulario.setOpaque(false);
        formulario.setLayout(new javax.swing.BoxLayout(formulario, javax.swing.BoxLayout.Y_AXIS));
        ButtonGroup grupo = new ButtonGroup();
        grupo.add(entrar);
        grupo.add(primeiroAcesso);
        JPanel opcoes = new JPanel(new GridLayout(1, 2, 12, 0));
        opcoes.setOpaque(false);
        opcoes.setMaximumSize(new Dimension(Integer.MAX_VALUE, 88));
        opcoes.add(entrar);
        opcoes.add(primeiroAcesso);
        formulario.add(opcoes);
        formulario.add(javax.swing.Box.createVerticalStrut(27));
        formulario.add(rotulo("CPF", 13, Font.BOLD, TEXTO));
        formulario.add(javax.swing.Box.createVerticalStrut(8));
        campoCpf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
        formulario.add(campoCpf);
        formulario.add(javax.swing.Box.createVerticalStrut(9));
        orientacao.setFont(new Font("SansSerif", Font.PLAIN, 12));
        orientacao.setForeground(CINZA);
        formulario.add(orientacao);
        formulario.add(javax.swing.Box.createVerticalStrut(5));
        erro.setFont(new Font("SansSerif", Font.PLAIN, 12));
        erro.setForeground(new Color(0xB42318));
        formulario.add(erro);
        painel.add(formulario, BorderLayout.CENTER);

        JPanel rodape = new JPanel(new BorderLayout(12, 0));
        rodape.setOpaque(false);
        JLabel dica = rotulo("Aceita uma sequência inventada de 11 números no formato 000.000.000-00.",
                12, Font.PLAIN, CINZA);
        rodape.add(dica, BorderLayout.CENTER);
        JButton cancelar = criarBotaoSecundario("Cancelar");
        cancelar.addActionListener(e -> dispose());
        JPanel acoes = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 0));
        acoes.setOpaque(false);
        acoes.add(cancelar);
        acoes.add(botaoContinuar);
        rodape.add(acoes, BorderLayout.SOUTH);
        painel.add(rodape, BorderLayout.SOUTH);
        botaoContinuar.addActionListener(e -> confirmar());
        return painel;
    }

    private void confirmar() {
        try {
            String cpf = CpfIdentificador.normalizar(campoCpf.getText());
            resultado = new Resultado(primeiroAcesso.isSelected(), cpf);
            dispose();
        } catch (IllegalArgumentException e) {
            erro.setText(e.getMessage());
            campoCpf.requestFocusInWindow();
        }
    }

    private void atualizarSelecao() {
        estilizarOpcao(entrar);
        estilizarOpcao(primeiroAcesso);
        orientacao.setText(entrar.isSelected()
                ? "Digite o CPF já cadastrado. Não é necessária senha."
                : "Um CPF com 11 números cria ou reutiliza o cadastro existente.");
    }

    private static void estilizarOpcao(JToggleButton botao) {
        boolean selecionado = botao.isSelected();
        botao.setBackground(selecionado ? new Color(0xE8F4EE) : Color.WHITE);
        botao.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(selecionado ? VERDE : BORDA, selecionado ? 2 : 1, true),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        botao.setForeground(selecionado ? MARINHO : TEXTO);
    }

    private static JToggleButton criarOpcao(String titulo, String linha1, String linha2) {
        JToggleButton botao = new JToggleButton("<html><div style='font-family:SansSerif;font-size:12pt;font-weight:bold'>"
                + linha1 + "</div><div style='font-family:SansSerif;font-size:9pt;color:#667085;margin-top:5px'>"
                + linha2 + "</div></html>");
        botao.setToolTipText(titulo);
        botao.setHorizontalAlignment(SwingConstants.LEFT);
        botao.setVerticalAlignment(SwingConstants.CENTER);
        botao.setFocusPainted(false);
        botao.setOpaque(true);
        botao.setMargin(new Insets(10, 12, 10, 12));
        return botao;
    }

    private static JPanel item(String numero, String texto) {
        JPanel linha = new JPanel(new BorderLayout(12, 0));
        linha.setOpaque(false);
        JLabel ponto = new JLabel(numero, SwingConstants.CENTER);
        ponto.setOpaque(true);
        ponto.setBackground(new Color(0x294966));
        ponto.setForeground(Color.WHITE);
        ponto.setFont(new Font("SansSerif", Font.BOLD, 11));
        ponto.setPreferredSize(new Dimension(34, 30));
        linha.add(ponto, BorderLayout.WEST);
        linha.add(rotulo(texto, 13, Font.PLAIN, Color.WHITE), BorderLayout.CENTER);
        linha.setMaximumSize(new Dimension(290, 34));
        return linha;
    }

    private static JTextPane criarDescricaoCentralizada() {
        JTextPane texto = new JTextPane();
        texto.setText("Uma experiência simples\npara conhecer o processo\nde votação.");
        texto.setEditable(false);
        texto.setFocusable(false);
        texto.setOpaque(false);
        texto.setFont(new Font("SansSerif", Font.PLAIN, 14));
        texto.setForeground(new Color(0xD8E2EC));
        texto.setBorder(BorderFactory.createEmptyBorder());
        texto.setMargin(new Insets(0, 0, 0, 0));
        StyledDocument documento = texto.getStyledDocument();
        SimpleAttributeSet centralizado = new SimpleAttributeSet();
        StyleConstants.setAlignment(centralizado, StyleConstants.ALIGN_CENTER);
        StyleConstants.setLineSpacing(centralizado, 0.12f);
        documento.setParagraphAttributes(0, documento.getLength(), centralizado, false);
        texto.setCaretPosition(0);
        return texto;
    }

    private static JLabel rotulo(String texto, int tamanho, int estilo, Color cor) {
        JLabel label = new JLabel(texto);
        label.setFont(new Font("SansSerif", estilo, tamanho));
        label.setForeground(cor);
        return label;
    }

    private static JLabel rotuloComSombra(String texto, int tamanho, int estilo, Color cor) {
        JLabel label = new JLabel(texto, SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics grafico) {
                Graphics2D g = (Graphics2D) grafico.create();
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setFont(getFont());
                java.awt.FontMetrics metricas = g.getFontMetrics();
                Insets margens = getInsets();
                int largura = getWidth() - margens.left - margens.right;
                int x = margens.left + Math.max(0, (largura - metricas.stringWidth(getText())) / 2);
                int y = margens.top + (getHeight() - margens.top - margens.bottom
                        - metricas.getHeight()) / 2 + metricas.getAscent();
                g.setColor(new Color(5, 19, 31, 125));
                g.drawString(getText(), x + 2, y + 2);
                g.setColor(getForeground());
                g.drawString(getText(), x, y);
                g.dispose();
            }
        };
        label.setFont(new Font("SansSerif", estilo, tamanho));
        label.setForeground(cor);
        label.setOpaque(false);
        return label;
    }

    private static JButton criarBotaoPrimario() {
        JButton botao = new JButton("Continuar");
        botao.setFont(new Font("SansSerif", Font.BOLD, 14));
        botao.setForeground(Color.WHITE);
        botao.setBackground(VERDE);
        botao.setFocusPainted(false);
        botao.setBorder(BorderFactory.createEmptyBorder(13, 25, 13, 25));
        botao.setOpaque(true);
        return botao;
    }

    private static JButton criarBotaoSecundario(String texto) {
        JButton botao = new JButton(texto);
        botao.setFont(new Font("SansSerif", Font.BOLD, 13));
        botao.setForeground(MARINHO);
        botao.setBackground(Color.WHITE);
        botao.setFocusPainted(false);
        botao.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDA, 1, true),
                BorderFactory.createEmptyBorder(12, 18, 12, 18)));
        botao.setOpaque(true);
        return botao;
    }

    private static JFormattedTextField criarCampoCpf() {
        try {
            MaskFormatter mascara = new MaskFormatter("###.###.###-##");
            mascara.setPlaceholderCharacter(' ');
            mascara.setValidCharacters("0123456789");
            mascara.setAllowsInvalid(false);
            mascara.setCommitsOnValidEdit(true);
            JFormattedTextField campo = new JFormattedTextField(mascara);
            campo.setFont(new Font("SansSerif", Font.PLAIN, 20));
            campo.setForeground(TEXTO);
            campo.setBackground(Color.WHITE);
            campo.setCaretColor(VERDE);
            campo.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDA, 1, true),
                    BorderFactory.createEmptyBorder(10, 14, 10, 14)));
            campo.setColumns(14);
            return campo;
        } catch (ParseException e) {
            throw new IllegalStateException("Não foi possível preparar o campo de CPF.", e);
        }
    }

    public record Resultado(boolean primeiroAcesso, String cpf) { }
}
