package com.urnacidada.repository;

public class RepositorioException extends Exception {
    public RepositorioException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
