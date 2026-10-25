package com.urnacidada.repository;

public class RepositorioException extends Exception {

    private static final long serialVersionUID = 1L;

    public RepositorioException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
