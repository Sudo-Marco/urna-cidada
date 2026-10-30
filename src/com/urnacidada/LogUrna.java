package com.urnacidada;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/** Log pequeno e rotativo; nunca recebe a chave da API nem o corpo das requisições. */
public final class LogUrna {
    private static final Logger LOGGER = Logger.getLogger("com.urnacidada");

    private LogUrna() { }

    public static void configurar(Path arquivo) throws IOException {
        LOGGER.setUseParentHandlers(true);
        FileHandler handler = new FileHandler(arquivo.resolve("urna.log").toString(), 512 * 1024, 3, true);
        handler.setFormatter(new SimpleFormatter());
        LOGGER.addHandler(handler);
        LOGGER.setLevel(Level.INFO);
    }

    public static Logger logger() { return LOGGER; }
}
