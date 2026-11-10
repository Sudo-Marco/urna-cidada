package com.urnacidada.view;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.Clip;
import javax.sound.sampled.SourceDataLine;
import java.io.BufferedInputStream;
import java.io.InputStream;

/** Sinais sonoros da urna. Falhas ou ausência de dispositivo não interrompem a votação. */
public final class SomUrna {
    private static volatile boolean silenciado;
    private SomUrna() { }
    public static void silenciar(boolean valor) { silenciado = valor; }
    public static boolean silenciado() { return silenciado; }
    public static void curto() { tocar(850, 45); }
    public static void fim() {
        if (silenciado) return;
        Thread audio = new Thread(SomUrna::tocarFinalizacao, "audio-fim-da-votacao");
        audio.setDaemon(true);
        audio.start();
    }

    /** Reproduz a gravação fornecida para o encerramento, sem gerar uma imitação por tons. */
    private static void tocarFinalizacao() {
        if (silenciado) return;
        try (InputStream recurso = SomUrna.class.getResourceAsStream(
                "/com/urnacidada/audio/som-finalizacao.wav")) {
            if (recurso == null || silenciado) return;
            try (AudioInputStream audio = AudioSystem.getAudioInputStream(new BufferedInputStream(recurso));
                 Clip clip = AudioSystem.getClip()) {
                clip.open(audio);
                if (silenciado) return;
                clip.start();
                while (!silenciado && clip.isRunning()) {
                    Thread.sleep(50);
                }
                if (silenciado) clip.stop();
            }
        } catch (Exception ignorada) { }
    }

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
