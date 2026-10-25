package com.urnacidada.testes;

import java.util.Objects;

/** Mini-biblioteca de testes, sem dependências externas: cada teste imprime OK ou FALHA. */
public final class MiniTeste {

    /** Corpo de um teste; pode lançar qualquer exceção. */
    public interface Corpo {
        void rodar() throws Exception;
    }

    private static int passou;
    private static int falhou;

    private MiniTeste() { }

    public static void grupo(String nome) {
        System.out.println("\n== " + nome);
    }

    public static void teste(String nome, Corpo corpo) {
        try {
            corpo.rodar();
            passou++;
            System.out.println("  OK     " + nome);
        } catch (Throwable erro) {
            falhou++;
            System.out.println("  FALHA  " + nome + "  ->  " + erro);
        }
    }

    public static void igual(Object esperado, Object obtido) {
        if (!Objects.equals(esperado, obtido)) {
            throw new AssertionError("esperava <" + esperado + "> mas veio <" + obtido + ">");
        }
    }

    public static void verdade(boolean condicao, String descricao) {
        if (!condicao) {
            throw new AssertionError("esperava verdadeiro: " + descricao);
        }
    }

    public static void verdade(boolean condicao) {
        verdade(condicao, "condição");
    }

    /** Confere que o trecho lança exatamente o tipo de exceção indicado (ou subtipo). */
    public static <T extends Throwable> T lanca(Class<T> tipo, Corpo corpo) {
        try {
            corpo.rodar();
        } catch (Throwable erro) {
            if (tipo.isInstance(erro)) {
                return tipo.cast(erro);
            }
            throw new AssertionError("lançou " + erro + " em vez de " + tipo.getSimpleName());
        }
        throw new AssertionError("deveria ter lançado " + tipo.getSimpleName());
    }

    /** Imprime o resumo e diz se tudo passou. */
    public static boolean resumo() {
        System.out.println("\n" + passou + " passaram, " + falhou + " falharam");
        return falhou == 0;
    }
}
