package com.urnacidada.repository;

/** Uma conta já tem uma sessão de votação e não pode registrar outra. */
public final class UsuarioJaVotouException extends RepositorioException {
    private static final long serialVersionUID = 1L;

    public UsuarioJaVotouException(String mensagem) {
        super(mensagem, null);
    }
}
