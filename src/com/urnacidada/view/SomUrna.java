package com.urnacidada.view;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.util.concurrent.TimeUnit;

/** Bipes e aviso falado. Falhas ou ausência de dispositivo de áudio não interrompem a votação. */
public final class SomUrna {
    private static volatile boolean silenciado;
    private SomUrna() { }
    public static void silenciar(boolean valor) { silenciado = valor; }
    public static boolean silenciado() { return silenciado; }
    public static void curto() { tocar(850, 45); }
    public static void fim() {
        if (silenciado) return;
        Thread audio = new Thread(() -> {
            tocar(620, 360);
            falarFim();
        }, "audio-fim-da-votacao");
        audio.setDaemon(true);
        audio.start();
    }

    private static void falarFim() {
        String script = "Add-Type -AssemblyName System.Speech; "
                + "$voz=new-object System.Speech.Synthesis.SpeechSynthesizer; "
                + "$pt=$voz.GetInstalledVoices() | Where-Object { $_.VoiceInfo.Culture.Name -eq 'pt-BR' } | Select-Object -First 1; "
                + "if ($pt) { $voz.SelectVoice($pt.VoiceInfo.Name) }; "
                + "$voz.Speak('Você já votou. Obrigado por participar.'); $voz.Dispose()";
        try {
            Process processo = new ProcessBuilder("powershell.exe", "-WindowStyle", "Hidden",
                    "-NoProfile", "-NonInteractive", "-Command", script)
                    .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
            if (!processo.waitFor(4, TimeUnit.SECONDS)) processo.destroyForcibly();
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
