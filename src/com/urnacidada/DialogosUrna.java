package com.urnacidada;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;

/** Diálogos do início da urna trazidos para a frente ao abrir pelo terminal ou pela IDE. */
public final class DialogosUrna {

    private DialogosUrna() { }

    public static int opcoes(Component pai, Object mensagem, String titulo, int tipoMensagem,
                             Object[] opcoes, Object opcaoInicial) {
        JOptionPane painel = new JOptionPane(mensagem, tipoMensagem, JOptionPane.DEFAULT_OPTION,
                null, opcoes, opcaoInicial);
        Object selecionado = mostrar(painel, pai, titulo);
        if (selecionado == null || selecionado == JOptionPane.UNINITIALIZED_VALUE) {
            return JOptionPane.CLOSED_OPTION;
        }
        for (int i = 0; i < opcoes.length; i++) {
            if (Objects.equals(opcoes[i], selecionado)) return i;
        }
        return JOptionPane.CLOSED_OPTION;
    }

    public static int confirmar(Component pai, Object mensagem, String titulo,
                                int tipoOpcao, int tipoMensagem) {
        JOptionPane painel = new JOptionPane(mensagem, tipoMensagem, tipoOpcao);
        Object selecionado = mostrar(painel, pai, titulo);
        return selecionado instanceof Integer valor ? valor : JOptionPane.CLOSED_OPTION;
    }

    public static void mensagem(Component pai, Object mensagem, String titulo, int tipoMensagem) {
        mostrar(new JOptionPane(mensagem, tipoMensagem, JOptionPane.DEFAULT_OPTION), pai, titulo);
    }

    public static String entrada(Component pai, Object mensagem, String titulo, int tipoMensagem) {
        JTextField campo = new JTextField(18);
        JPanel painel = new JPanel(new GridLayout(2, 1, 4, 4));
        painel.add(new JLabel(mensagem.toString()));
        painel.add(campo);
        int resultado = confirmar(pai, painel, titulo, JOptionPane.OK_CANCEL_OPTION, tipoMensagem);
        return resultado == JOptionPane.OK_OPTION ? campo.getText() : null;
    }

    private static Object mostrar(JOptionPane painel, Component pai, String titulo) {
        JDialog dialogo = painel.createDialog(pai, titulo);
        dialogo.setAlwaysOnTop(true);
        dialogo.setAutoRequestFocus(true);
        dialogo.setFocusableWindowState(true);
        dialogo.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent evento) {
                dialogo.toFront();
                dialogo.requestFocus();
            }
        });
        dialogo.setLocationRelativeTo(pai);
        dialogo.setVisible(true);
        Object selecionado = painel.getValue();
        dialogo.dispose();
        return selecionado;
    }
}
