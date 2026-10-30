package com.urnacidada;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Mantém uma trava exclusiva durante toda a execução para evitar duas urnas concorrentes. */
public final class TravaInstancia implements AutoCloseable {
    private final FileChannel canal;
    private final FileLock trava;

    private TravaInstancia(FileChannel canal, FileLock trava) {
        this.canal = canal;
        this.trava = trava;
    }

    public static TravaInstancia tentar(Path arquivo) throws IOException {
        FileChannel canal = FileChannel.open(arquivo, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        try {
            FileLock trava = canal.tryLock();
            if (trava == null) {
                canal.close();
                return null;
            }
            return new TravaInstancia(canal, trava);
        } catch (OverlappingFileLockException ocupado) {
            canal.close();
            return null;
        } catch (IOException | RuntimeException falha) {
            canal.close();
            throw falha;
        }
    }

    @Override
    public void close() throws IOException {
        try { trava.release(); } finally { canal.close(); }
    }
}
