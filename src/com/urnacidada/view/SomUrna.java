package com.urnacidada.view;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/** Bipes simples opcionais, sintetizados sem arquivos externos. Falha de áudio é ignorada. */
public final class SomUrna {
    private static volatile boolean silenciado;
    private SomUrna() { }
    public static void silenciar(boolean valor) { silenciado = valor; }
    public static boolean silenciado() { return silenciado; }
    public static void curto() { tocar(850, 45); }
    public static void fim() { tocar(620, 360); }
    private static void tocar(int frequencia, int milissegundos) {
        if (silenciado) return;
        try {
            float taxa = 8000;
            byte[] dados = new byte[(int) (taxa * milissegundos / 1000)];
            for (int i = 0; i < dados.length; i++) {
                dados[i] = (byte) (Math.sin(2 * Math.PI * frequencia * i / taxa) * 35);
            }
            var formato = new javax.sound.sampled.AudioFormat(taxa, 8, 1, true, false);
            try (SourceDataLine linha = AudioSystem.getSourceDataLine(formato)) {
                linha.open(formato);
                linha.start();
                linha.write(dados, 0, dados.length);
                linha.drain();
            }
        } catch (Exception ignorada) { }
    }
}
