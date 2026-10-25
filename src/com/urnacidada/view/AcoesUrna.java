package com.urnacidada.view;

/** O que a tela pode pedir. Quem implementa decide o que fazer (ver {@link ControladorUrna}). */
public interface AcoesUrna {
    void digitar(char digito);

    void branco();

    void corrigir();

    void confirmar();

    void novaVotacao();

    void mostrarDicas();
}
