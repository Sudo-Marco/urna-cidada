package com.urnacidada.view;

import com.urnacidada.LogUrna;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
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
        fim(null);
    }

    /** Reproduz o som final e avisa quando a gravação terminar. */
    public static void fim(Runnable aoTerminar) {
        if (silenciado) {
            if (aoTerminar != null) aoTerminar.run();
            return;
        }
        Thread audio = new Thread(() -> {
            try {
                tocarFinalizacao();
            } finally {
                if (aoTerminar != null) aoTerminar.run();
            }
        }, "audio-fim-da-votacao");
        audio.setDaemon(false);
        audio.start();
    }

    /** Reproduz a gravação fornecida para o encerramento, sem gerar uma imitação por tons. */
    private static void tocarFinalizacao() {
        if (silenciado) return;
        try (InputStream recurso = SomUrna.class.getResourceAsStream(
                "/com/urnacidada/audio/som-finalizacao.wav")) {
            if (recurso == null) {
                LogUrna.logger().warning("A gravação de encerramento não está incluída no aplicativo.");
                return;
            }
            try (AudioInputStream audio = AudioSystem.getAudioInputStream(new BufferedInputStream(recurso))) {
                AudioFormat saida = new AudioFormat(8_000, 8, 1, true, false);
                try (SourceDataLine linha = AudioSystem.getSourceDataLine(saida)) {
                    linha.open(saida);
                    linha.start();
                    byte[] dados = new byte[4096];
                    int quantidade;
                    while (!silenciado && (quantidade = audio.read(dados, 0, dados.length)) != -1) {
                        // WAV de 8 bits guarda PCM sem sinal; a linha de áudio usada pelos
                        // cliques da urna recebe PCM com sinal.
                        for (int i = 0; i < quantidade; i++) {
                            dados[i] = (byte) ((dados[i] & 0xff) - 128);
                        }
                        linha.write(dados, 0, quantidade);
                    }
                    if (silenciado) linha.stop();
                    else linha.drain();
                }
            }
        } catch (Exception erro) {
            LogUrna.logger().warning("Não foi possível reproduzir o áudio de encerramento: "
                    + erro.getClass().getSimpleName());
        }
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
