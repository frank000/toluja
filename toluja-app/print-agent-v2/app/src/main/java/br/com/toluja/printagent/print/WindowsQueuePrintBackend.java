package br.com.toluja.printagent.print;

import br.com.toluja.printagent.api.dto.JobDelivery;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.Locale;

public final class WindowsQueuePrintBackend implements PrintBackend {
    @Override
    public String channel() {
        return "WINDOWS_QUEUE";
    }

    @Override
    public void print(JobDelivery delivery, byte[] payload) throws PrintBackendException {
        if (!isWindows()) {
            throw new PrintBackendException("WINDOWS_QUEUE e suportado apenas no Windows");
        }

        PrintService service = findPrintService(delivery.destination());
        DocPrintJob job = service.createPrintJob();
        PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
        attributes.add(new Copies(delivery.copies()));

        try {
            Doc doc = new SimpleDoc(payload, DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
            job.print(doc, attributes);
        } catch (Exception ex) {
            throw new PrintBackendException("Falha ao imprimir na fila Windows: " + delivery.destination(), ex);
        }
    }

    private PrintService findPrintService(String destination) throws PrintBackendException {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        return Arrays.stream(services)
                .filter(service -> service.getName().equalsIgnoreCase(destination))
                .findFirst()
                .orElseThrow(() -> new PrintBackendException(queueNotFoundMessage(destination, services)));
    }

    private String queueNotFoundMessage(String destination, PrintService[] services) {
        if (services.length == 0) {
            return "Fila Windows nao encontrada: " + destination
                    + ". Nenhuma fila local foi detectada pelo Java PrintService.";
        }

        String available = Arrays.stream(services)
                .map(PrintService::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining(", "));
        return "Fila Windows nao encontrada: " + destination + ". Filas detectadas: " + available;
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }
}
