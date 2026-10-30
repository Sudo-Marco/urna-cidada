package com.urnacidada.view;

import com.urnacidada.model.Candidato;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;

/** Lista rolável com os candidatos e propostas fictícias do cargo atual. */
@SuppressWarnings("serial")
final class PainelCandidatos extends JPanel {
    private final JPanel cartoes = new JPanel();
    private final JLabel titulo = new JLabel("CANDIDATOS DESTE CARGO");
    PainelCandidatos() {
        super(new BorderLayout(0, 8));
        setPreferredSize(new Dimension(250, 10));
        setMinimumSize(new Dimension(210, 10));
        setBackground(new Color(0xE5EBF1));
        setBorder(new EmptyBorder(8, 8, 8, 8));
        titulo.setFont(new Font("SansSerif", Font.BOLD, 12));
        add(titulo, BorderLayout.NORTH);
        cartoes.setLayout(new javax.swing.BoxLayout(cartoes, javax.swing.BoxLayout.Y_AXIS));
        cartoes.setBackground(new Color(0xE5EBF1));
        JScrollPane rolagem = new JScrollPane(cartoes);
        rolagem.setBorder(BorderFactory.createEmptyBorder());
        rolagem.getVerticalScrollBar().setUnitIncrement(16);
        add(rolagem, BorderLayout.CENTER);
    }
    void atualizar(String nomeCargo, List<Candidato> candidatos, String digitado) {
        titulo.setText("CANDIDATOS · " + nomeCargo.toUpperCase(java.util.Locale.ROOT));
        cartoes.removeAll();
        for (Candidato c : candidatos) {
            boolean selecionado = !digitado.isEmpty() && (c.numero().startsWith(digitado)
                    || (digitado.length() <= 2 && c.numeroPartido().startsWith(digitado)));
            JPanel cartao = new JPanel(new BorderLayout(8, 4));
            cartao.setAlignmentX(LEFT_ALIGNMENT);
            cartao.setMaximumSize(new Dimension(Integer.MAX_VALUE, 230));
            cartao.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(selecionado ? new Color(0x16824A) : new Color(0xBCC7D1), selecionado ? 2 : 1),
                    new EmptyBorder(8, 8, 8, 8)));
            cartao.setBackground(selecionado ? new Color(0xE4F5E9) : Color.WHITE);
            JPanel cabecalho = new JPanel(new BorderLayout(6, 0));
            cabecalho.setOpaque(false);
            JLabel sigla = new JLabel(iniciais(c.nome()), SwingConstants.CENTER);
            sigla.setOpaque(true); sigla.setBackground(new Color(0xC8D8E8));
            sigla.setFont(new Font("SansSerif", Font.BOLD, 14)); sigla.setPreferredSize(new Dimension(42, 42));
            JLabel nome = new JLabel("<html><b>" + escapar(c.numero()) + " · " + escapar(c.nome()) + "</b><br>" + escapar(c.partido()) + "</html>");
            cabecalho.add(sigla, BorderLayout.WEST); cabecalho.add(nome, BorderLayout.CENTER);
            cartao.add(cabecalho, BorderLayout.NORTH);
            StringBuilder proposta = new StringBuilder("<html><small>O QUE PRETENDE FAZER</small><ul>");
            for (String p : c.propostas()) proposta.append("<li>").append(escapar(p)).append("</li>");
            proposta.append("</ul>");
            c.vice().ifPresent(v -> proposta.append("<b>Vice:</b> ").append(escapar(v)));
            proposta.append("</html>");
            JLabel texto = new JLabel(proposta.toString());
            texto.setFont(new Font("SansSerif", Font.PLAIN, 11));
            cartao.add(texto, BorderLayout.CENTER);
            cartoes.add(cartao); cartoes.add(javax.swing.Box.createVerticalStrut(6));
        }
        cartoes.revalidate(); cartoes.repaint();
    }
    private static String iniciais(String nome) {
        String[] partes = nome.split("\\s+");
        return partes.length < 2 ? partes[0].substring(0, 1) : "" + partes[0].charAt(0) + partes[1].charAt(0);
    }
    private static String escapar(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
