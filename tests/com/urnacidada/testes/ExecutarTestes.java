package com.urnacidada.testes;

/**
 * Executa todas as baterias de testes. Não precisa de Maven nem de internet:
 * veja {@code scripts/testar.bat}.
 */
public final class ExecutarTestes {

    private ExecutarTestes() { }

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");

        ModeloTestes.executar();
        UrnaServiceTestes.executar();
        RepositorioTestes.executar();
        ControladorUrnaTestes.executar();
        PainelUrnaTestes.executar();
        ConfiguracaoTestes.executar();
        CpfIdentificadorTestes.executar();
        AutenticacaoTestes.executar();
        FirestoreTestes.executar();
        LocalPrimeiroTestes.executar();

        System.exit(MiniTeste.resumo() ? 0 : 1);
    }
}
