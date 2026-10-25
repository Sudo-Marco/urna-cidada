package com.urnacidada.testes;

import com.urnacidada.model.Eleicao;
import com.urnacidada.repository.VotoRepositoryEmMemoria;
import com.urnacidada.view.AcoesUrna;
import com.urnacidada.view.ControladorUrna;
import com.urnacidada.view.EstadoTela;
import com.urnacidada.view.EstadoTela.Modo;
import com.urnacidada.view.EstadoTela.TipoMensagem;
import com.urnacidada.view.PainelUrna;

import javax.swing.AbstractButton;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static com.urnacidada.testes.MiniTeste.grupo;
import static com.urnacidada.testes.MiniTeste.igual;
import static com.urnacidada.testes.MiniTeste.teste;
import static com.urnacidada.testes.MiniTeste.verdade;

/** Testes da visão Swing, sem abrir janela (modo headless): montagem, desenho e botões. */
final class PainelUrnaTestes {

    /** Registra o que a tela pediu. */
    private static final class Gravador implements AcoesUrna {
        final List<String> pedidos = new ArrayList<>();

        @Override public void digitar(char digito) { pedidos.add("digitar " + digito); }
        @Override public void branco() { pedidos.add("branco"); }
        @Override public void corrigir() { pedidos.add("corrigir"); }
        @Override public void confirmar() { pedidos.add("confirmar"); }
        @Override public void novaVotacao() { pedidos.add("nova"); }
        @Override public void mostrarDicas() { pedidos.add("dicas"); }
    }

    private PainelUrnaTestes() { }

    private static AbstractButton achar(Container raiz, String texto) {
        for (Component c : raiz.getComponents()) {
            if (c instanceof AbstractButton b && texto.equals(b.getText())) {
                return b;
            }
            if (c instanceof Container filho) {
                AbstractButton achado = achar(filho, texto);
                if (achado != null) {
                    return achado;
                }
            }
        }
        return null;
    }

    private static void ajustar(Component c) {
        c.invalidate();
        if (c instanceof Container k) {
            for (Component filho : k.getComponents()) {
                ajustar(filho);
            }
        }
    }

    private static void organizar(Component c) {
        c.doLayout();
        if (c instanceof Container k) {
            for (Component filho : k.getComponents()) {
                organizar(filho);
            }
        }
    }

    /** Desenha o painel numa imagem; qualquer erro de layout ou de desenho vira exceção. */
    private static void desenhar(PainelUrna painel) {
        Dimension tamanho = new Dimension(920, 640);
        painel.setSize(tamanho);
        ajustar(painel);
        organizar(painel);
        organizar(painel);
        BufferedImage imagem = new BufferedImage(tamanho.width, tamanho.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagem.createGraphics();
        painel.paint(g);
        g.dispose();
    }

    /**
     * Componentes Swing só podem ser usados na thread de eventos. Rodar fora dela mistura
     * threads no processamento de texto e pode travar (deadlock), por isso todo teste de
     * visão passa por aqui.
     */
    private static void caso(String nome, MiniTeste.Corpo corpo) {
        teste(nome, () -> naThreadDaTela(corpo));
    }

    private static void naThreadDaTela(MiniTeste.Corpo corpo) throws Exception {
        Throwable[] erro = new Throwable[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                corpo.rodar();
            } catch (Throwable t) {
                erro[0] = t;
            }
        });
        if (erro[0] instanceof Exception e) {
            throw e;
        }
        if (erro[0] instanceof Error e) {
            throw e;
        }
    }

    static void executar() {
        grupo("Visão Swing (PainelUrna), sem janela");

        caso("monta e desenha todos os modos e tipos de mensagem sem erro", () -> {
            PainelUrna painel = new PainelUrna(new Gravador(), "teste");
            for (Modo modo : Modo.values()) {
                for (TipoMensagem tipo : TipoMensagem.values()) {
                    painel.exibir(new EstadoTela(modo, "VEREADOR", "CARGO 1 DE 2", 5, "12", "Título",
                            "Detalhe", "Explicação do cargo", "Mensagem de teste", tipo, true));
                    desenhar(painel);
                }
            }
        });

        caso("desenha textos longos sem erro", () -> {
            PainelUrna painel = new PainelUrna(new Gravador(), "Cloud Firestore (projeto exemplo) + reserva local");
            String longo = "Mensagem muito longa. ".repeat(20);
            painel.exibir(new EstadoTela(Modo.CANDIDATO, "VEREADOR", "CARGO 1 DE 2", 5, "12345", longo, longo,
                    longo, longo, TipoMensagem.ERRO, true));
            desenhar(painel);
        });

        caso("o botão FIM de votação mostra 'Iniciar nova votação' e esconde as teclas", () -> {
            Gravador gravador = new Gravador();
            PainelUrna painel = new PainelUrna(gravador, "teste");
            painel.exibir(new EstadoTela(Modo.FIM, "VOTAÇÃO CONCLUÍDA", "", 0, "", "FIM", "Obrigado",
                    "", "Registrado", TipoMensagem.SUCESSO, false));
            verdade(achar(painel, "Iniciar nova votação").isVisible(), "botão de nova votação visível");
            verdade(!achar(painel, "5").isEnabled(), "teclas desabilitadas");
            achar(painel, "Iniciar nova votação").doClick(0);
            igual("nova", gravador.pedidos.get(0));
        });

        caso("cada botão da tela dispara a ação certa", () -> {
            Gravador gravador = new Gravador();
            PainelUrna painel = new PainelUrna(gravador, "teste");
            achar(painel, "7").doClick(0);
            achar(painel, "0").doClick(0);
            achar(painel, "BRANCO").doClick(0);
            achar(painel, "CORRIGE").doClick(0);
            achar(painel, "CONFIRMA").doClick(0);
            achar(painel, "Dicas contra desinformação").doClick(0);
            igual(List.of("digitar 7", "digitar 0", "branco", "corrigir", "confirmar", "dicas"),
                    gravador.pedidos);
        });

        caso("o painel conectado ao controlador desenha o fluxo inteiro", () -> {
            VotoRepositoryEmMemoria memoria = new VotoRepositoryEmMemoria();
            PainelUrna[] painel = new PainelUrna[1];
            ControladorUrna[] controlador = new ControladorUrna[1];
            AcoesUrna ligacao = new AcoesUrna() {
                @Override public void digitar(char d) { controlador[0].digitar(d); }
                @Override public void branco() { controlador[0].branco(); }
                @Override public void corrigir() { controlador[0].corrigir(); }
                @Override public void confirmar() { controlador[0].confirmar(); }
                @Override public void novaVotacao() { controlador[0].novaVotacao(); }
                @Override public void mostrarDicas() { controlador[0].mostrarDicas(); }
            };
            painel[0] = new PainelUrna(ligacao, memoria.descricao());
            controlador[0] = new ControladorUrna(Eleicao.exemplo(), memoria, Runnable::run, Runnable::run,
                    estado -> {
                        painel[0].exibir(estado);
                        desenhar(painel[0]);
                    }, () -> { });
            controlador[0].novaVotacao();

            for (String botao : List.of("1", "2", "3", "4", "5", "CONFIRMA", "2", "2", "CONFIRMA")) {
                achar(painel[0], botao).doClick(0);
            }
            igual(2, memoria.getVotos().size());
            verdade(achar(painel[0], "Iniciar nova votação").isVisible(), "chegou ao FIM");
        });
    }
}
