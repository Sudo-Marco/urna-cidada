package com.urnacidada.view;

import com.urnacidada.service.ApuracaoLocal;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Painel administrativo com a apuração anônima e local separada por cargo. */
public final class PainelApuracaoLocal {

    private static final Color AZUL = new Color(0x17324D);
    private static final Color FUNDO = new Color(0xF3F6F9);
    private static final Color TEXTO = new Color(0x1F2937);
    private static final Color SECUNDARIO = new Color(0x5F6B7A);
    private static final Color BORDA = new Color(0xD8E0E8);
    private static final Color VERDE = new Color(0xE5F4EC);
    private static final Color LARANJA = new Color(0xFFF1E3);

    private PainelApuracaoLocal() { }

    public static void exibir(ApuracaoLocal.Resultado resultado) {
        Runnable abrir = () -> criarJanela(resultado);
        try {
            if (SwingUtilities.isEventDispatchThread()) abrir.run();
            else SwingUtilities.invokeAndWait(abrir);
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível abrir o painel de apuração local.", e);
        }
    }

    private static void criarJanela(ApuracaoLocal.Resultado resultado) {
        JDialog janela = new JDialog((Frame) null, "Urna Cidadã | Apuração local", true);
        janela.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        janela.setMinimumSize(new Dimension(920, 620));
        janela.setSize(1080, 720);
        janela.setLocationRelativeTo(null);

        JPanel conteudo = new JPanel(new BorderLayout(0, 14));
        conteudo.setBackground(FUNDO);
        conteudo.setBorder(BorderFactory.createEmptyBorder(18, 20, 16, 20));
        conteudo.add(criarCabecalho(resultado), BorderLayout.NORTH);

        JTabbedPane cargos = new JTabbedPane();
        cargos.setFont(new Font("SansSerif", Font.BOLD, 13));
        cargos.setBackground(Color.WHITE);
        for (ApuracaoLocal.CargoResultado cargo : resultado.cargos()) {
            cargos.addTab(cargo.nome(), criarAba(cargo));
        }
        conteudo.add(cargos, BorderLayout.CENTER);
        conteudo.add(criarRodape(janela), BorderLayout.SOUTH);
        janela.setContentPane(conteudo);
        janela.setAlwaysOnTop(true);
        janela.setAutoRequestFocus(true);
        janela.setVisible(true);
        janela.toFront();
        janela.requestFocus();
    }

    private static JPanel criarCabecalho(ApuracaoLocal.Resultado resultado) {
        JPanel cabecalho = new JPanel(new BorderLayout(12, 12));
        cabecalho.setOpaque(false);

        JPanel titulo = new JPanel(new BorderLayout(0, 5));
        titulo.setOpaque(false);
        JLabel nome = new JLabel("APURAÇÃO LOCAL OFFLINE");
        nome.setFont(new Font("SansSerif", Font.BOLD, 22));
        nome.setForeground(AZUL);
        JLabel descricao = new JLabel("Contagem salva neste computador; não depende do Firebase.");
        descricao.setFont(new Font("SansSerif", Font.PLAIN, 13));
        descricao.setForeground(SECUNDARIO);
        titulo.add(nome, BorderLayout.NORTH);
        titulo.add(descricao, BorderLayout.SOUTH);
        cabecalho.add(titulo, BorderLayout.NORTH);

        JPanel indicadores = new JPanel(new GridLayout(1, 3, 10, 0));
        indicadores.setOpaque(false);
        indicadores.add(cartao("SESSÕES", String.valueOf(resultado.sessoes()), "participações completas"));
        indicadores.add(cartao("VOTOS REGISTRADOS", String.valueOf(resultado.totalVotos()), "somados entre os cargos"));
        indicadores.add(cartao("CARGOS", String.valueOf(resultado.cargos().size()), "apurados separadamente"));
        cabecalho.add(indicadores, BorderLayout.CENTER);
        return cabecalho;
    }

    private static JPanel criarAba(ApuracaoLocal.CargoResultado cargo) {
        JPanel aba = new JPanel(new BorderLayout(0, 12));
        aba.setBackground(FUNDO);
        aba.setBorder(BorderFactory.createEmptyBorder(14, 12, 12, 12));

        JPanel destaques = new JPanel(new GridLayout(1, 2, 10, 0));
        destaques.setOpaque(false);
        destaques.add(cartaoRanking("MAIS VOTADO DO CARGO", textoRanking(cargo, true), VERDE));
        destaques.add(cartaoRanking("MENOS VOTADO DO CARGO", textoRanking(cargo, false), LARANJA));
        aba.add(destaques, BorderLayout.NORTH);

        JTable tabela = criarTabela(cargo);
        JScrollPane rolagem = new JScrollPane(tabela);
        rolagem.setBorder(BorderFactory.createLineBorder(BORDA));
        aba.add(rolagem, BorderLayout.CENTER);

        JLabel totais = new JLabel("Nominais: " + cargo.votosNominais()
                + "   ·   Legenda: " + cargo.votosLegenda()
                + "   ·   Branco: " + cargo.brancos()
                + "   ·   Nulo: " + cargo.nulos()
                + "   ·   Total do cargo: " + cargo.totalVotos());
        totais.setFont(new Font("SansSerif", Font.BOLD, 13));
        totais.setForeground(AZUL);
        aba.add(totais, BorderLayout.SOUTH);
        return aba;
    }

    private static JTable criarTabela(ApuracaoLocal.CargoResultado cargo) {
        String[] colunas = {"TIPO", "NÚMERO", "CANDIDATURA / PARTIDO", "PARTIDO", "VOTOS"};
        DefaultTableModel modelo = new DefaultTableModel(colunas, 0) {
            @Override public boolean isCellEditable(int linha, int coluna) { return false; }
            @Override public Class<?> getColumnClass(int coluna) {
                return coluna == 4 ? Integer.class : String.class;
            }
        };
        List<ApuracaoLocal.CandidatoResultado> candidatos = new ArrayList<>(cargo.candidatos());
        candidatos.sort(Comparator.comparingInt(ApuracaoLocal.CandidatoResultado::votos).reversed()
                .thenComparing(ApuracaoLocal.CandidatoResultado::nome));
        for (ApuracaoLocal.CandidatoResultado candidato : candidatos) {
            modelo.addRow(new Object[]{"Voto nominal", candidato.numero(), candidato.nome(),
                    candidato.partido(), candidato.votos()});
        }
        for (ApuracaoLocal.LegendaResultado legenda : cargo.legendas()) {
            modelo.addRow(new Object[]{"Voto de legenda", legenda.numero(), "Legenda", legenda.partido(), legenda.votos()});
        }

        JTable tabela = new JTable(modelo);
        tabela.setRowHeight(34);
        tabela.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tabela.setForeground(TEXTO);
        tabela.setBackground(Color.WHITE);
        tabela.setGridColor(new Color(0xE9EEF3));
        tabela.setFillsViewportHeight(true);
        tabela.setAutoCreateRowSorter(true);
        tabela.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        tabela.getTableHeader().setForeground(AZUL);
        tabela.getTableHeader().setBackground(new Color(0xEAF0F6));
        tabela.getColumnModel().getColumn(0).setPreferredWidth(110);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(80);
        tabela.getColumnModel().getColumn(2).setPreferredWidth(200);
        tabela.getColumnModel().getColumn(3).setPreferredWidth(230);
        tabela.getColumnModel().getColumn(4).setPreferredWidth(70);
        return tabela;
    }

    private static String textoRanking(ApuracaoLocal.CargoResultado cargo, boolean maior) {
        if (cargo.votosNominais() == 0) return "Ainda não há votos nominais neste cargo.";
        int quantidade = maior
                ? cargo.candidatos().stream().mapToInt(ApuracaoLocal.CandidatoResultado::votos).max().orElse(0)
                : cargo.candidatos().stream().mapToInt(ApuracaoLocal.CandidatoResultado::votos).min().orElse(0);
        List<String> nomes = cargo.candidatos().stream()
                .filter(c -> c.votos() == quantidade)
                .map(ApuracaoLocal.CandidatoResultado::nome)
                .toList();
        String prefixo = nomes.size() > 1 ? "Empate: " : "";
        return prefixo + String.join(", ", nomes) + " · " + quantidade + (quantidade == 1 ? " voto" : " votos");
    }

    private static JPanel cartao(String titulo, String valor, String detalhe) {
        JPanel painel = new JPanel(new BorderLayout(0, 3));
        painel.setBackground(Color.WHITE);
        painel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDA), BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        JLabel rotulo = texto(titulo, 10, Font.BOLD, SECUNDARIO);
        JLabel numero = texto(valor, 22, Font.BOLD, AZUL);
        JLabel legenda = texto(detalhe, 11, Font.PLAIN, SECUNDARIO);
        painel.add(rotulo, BorderLayout.NORTH);
        painel.add(numero, BorderLayout.CENTER);
        painel.add(legenda, BorderLayout.SOUTH);
        return painel;
    }

    private static JPanel cartaoRanking(String titulo, String valor, Color fundo) {
        JPanel painel = new JPanel(new BorderLayout(0, 6));
        painel.setBackground(fundo);
        painel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDA), BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        painel.add(texto(titulo, 10, Font.BOLD, SECUNDARIO), BorderLayout.NORTH);
        JLabel destaque = texto("<html><body width='340'>" + escaparHtml(valor).replace(", ", ",<br>") + "</body></html>",
                13, Font.BOLD, TEXTO);
        painel.add(destaque, BorderLayout.CENTER);
        painel.setPreferredSize(new Dimension(10, 80));
        return painel;
    }

    private static JPanel criarRodape(JDialog janela) {
        JPanel rodape = new JPanel(new BorderLayout(10, 0));
        rodape.setOpaque(false);
        JLabel privacidade = texto("A apuração exibe totais por candidatura; ela não associa votos a participantes.",
                11, Font.PLAIN, SECUNDARIO);
        rodape.add(privacidade, BorderLayout.CENTER);
        JButton fechar = new JButton("Fechar");
        fechar.setFont(new Font("SansSerif", Font.BOLD, 13));
        fechar.setForeground(Color.WHITE);
        fechar.setBackground(AZUL);
        fechar.setFocusPainted(false);
        fechar.setMargin(new Insets(9, 22, 9, 22));
        fechar.addActionListener(e -> janela.dispose());
        rodape.add(fechar, BorderLayout.EAST);
        return rodape;
    }

    private static JLabel texto(String valor, int tamanho, int estilo, Color cor) {
        JLabel label = new JLabel(valor, SwingConstants.LEFT);
        label.setFont(new Font("SansSerif", estilo, tamanho));
        label.setForeground(cor);
        return label;
    }

    private static String escaparHtml(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
