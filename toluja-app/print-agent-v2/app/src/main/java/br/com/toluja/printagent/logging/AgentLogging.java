package br.com.toluja.printagent.logging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public final class AgentLogging {
    private static final Logger ROOT = Logger.getLogger("");

    private AgentLogging() {
    }

    public static void configure() {
        ROOT.setLevel(Level.INFO);
        for (var handler : ROOT.getHandlers()) {
            ROOT.removeHandler(handler);
        }

        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.INFO);
        consoleHandler.setFormatter(new SimpleFormatter());
        ROOT.addHandler(consoleHandler);

        try {
            Path logsDir = logsDir();
            Files.createDirectories(logsDir);
            FileHandler fileHandler = new FileHandler(
                    logsDir.resolve("toluja-print-agent-%g.log").toString(),
                    1_048_576,
                    5,
                    true
            );
            fileHandler.setLevel(Level.INFO);
            fileHandler.setFormatter(new SimpleFormatter());
            ROOT.addHandler(fileHandler);
        } catch (IOException ex) {
            Logger.getLogger(AgentLogging.class.getName())
                    .warning("Nao foi possivel configurar log em arquivo: " + ex.getMessage());
        }
    }

    public static Path logsDir() {
        if (!isWindows()) {
            return Path.of("logs");
        }
        return dataDir().resolve("logs");
    }

    public static Path dataDir() {
        if (isWindows()) {
            String programData = System.getenv("ProgramData");
            if (programData != null && !programData.isBlank()) {
                return Path.of(programData, "Toluja", "PrintAgent");
            }
            return Path.of("C:\\ProgramData\\Toluja\\PrintAgent");
        }

        return Path.of("data");
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }
}
