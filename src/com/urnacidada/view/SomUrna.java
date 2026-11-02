package com.urnacidada.view;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.SourceDataLine;

/** Sinais sonoros da urna. Falhas ou ausência de dispositivo não interrompem a votação. */
public final class SomUrna {
    private static volatile boolean silenciado;
    private SomUrna() { }
    public static void silenciar(boolean valor) { silenciado = valor; }
    public static boolean silenciado() { return silenciado; }
    public static void curto() { tocar(850, 45); }
    public static void fim() {
        if (silenciado) return;
        Thread audio = new Thread(SomUrna::tocarPilili, "audio-fim-da-votacao");
        audio.setDaemon(true);
        audio.start();
    }

    /** Três notas ascendentes e curtas, lembrando o sinal clássico de encerramento da urna. */
    private static void tocarPilili() {
        if (silenciado) return;
        final int taxa = 16_000;
        final int[] frequencias = {740, 988, 1175};
        final int[] duracoes = {95, 100, 300};
        AudioFormat formato = new AudioFormat(taxa, 8, 1, true, false);
        try {
            try (SourceDataLine linha = AudioSystem.getSourceDataLine(formato)) {
                linha.open(formato);
                linha.start();
                for (int nota = 0; nota < frequencias.length; nota++) {
                    if (silenciado) break;
                    int amostras = taxa * duracoes[nota] / 1000;
                    byte[] dados = new byte[amostras];
                    for (int i = 0; i < amostras; i++) {
                        double tempo = i / (double) taxa;
                        double ataque = Math.min(1.0, i / (taxa * 0.008));
                        double soltura = Math.min(1.0, (amostras - i) / (taxa * 0.018));
                        double envelope = Math.min(ataque, soltura);
                        double fundamental = Math.sin(2 * Math.PI * frequencias[nota] * tempo);
                        double harmonico = Math.sin(4 * Math.PI * frequencias[nota] * tempo) * 0.18;
                        dados[i] = (byte) ((fundamental + harmonico) * 58 * envelope);
                    }
                    linha.write(dados, 0, dados.length);
                    if (nota < frequencias.length - 1) {
                        int pausa = taxa * 24 / 1000;
                        linha.write(new byte[pausa], 0, pausa);
                    }
                }
                linha.drain();
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
