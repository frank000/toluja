package br.com.toluja.printagent.print;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PrinterDetector {
    private PrinterDetector() {
    }

    public static List<DetectedPrinter> listPrinters() {
        Map<String, DetectedPrinter> printers = new LinkedHashMap<>();

        for (PrintService service : PrintServiceLookup.lookupPrintServices(null, null)) {
            printers.putIfAbsent(service.getName(), new DetectedPrinter(service.getName(), "JAVA_PRINT_SERVICE"));
        }

        if (!isWindows()) {
            for (String cupsName : listCupsPrinters()) {
                printers.putIfAbsent(cupsName, new DetectedPrinter(cupsName, "CUPS_LPSTAT"));
            }
        }

        return new ArrayList<>(printers.values());
    }

    private static List<String> listCupsPrinters() {
        ProcessBuilder builder = new ProcessBuilder("lpstat", "-a");
        builder.redirectErrorStream(true);
        try {
            Process process = builder.start();
            boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return List.of();
            }
            if (process.exitValue() != 0) {
                return List.of();
            }

            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            List<String> names = new ArrayList<>();
            for (String line : output.split("\\R")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                String[] parts = trimmed.split("\\s+", 2);
                if (parts.length > 0 && !parts[0].isBlank()) {
                    names.add(parts[0]);
                }
            }
            return names;
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return List.of();
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }
}
