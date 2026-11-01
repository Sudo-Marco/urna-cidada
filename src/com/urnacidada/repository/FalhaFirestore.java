package com.urnacidada.repository;

/** Erro do Firestore classificado para decidir se a sincronização deve ser repetida. */
public final class FalhaFirestore extends RepositorioException {
    private static final long serialVersionUID = 1L;
    public enum Tipo { CONFIGURACAO, TRANSITORIA, USUARIO_JA_VOTOU }
    private final Tipo tipo;

    public FalhaFirestore(Tipo tipo, String mensagem, Throwable causa) {
        super(mensagem, causa);
        this.tipo = tipo;
    }

    public Tipo tipo() { return tipo; }
}
