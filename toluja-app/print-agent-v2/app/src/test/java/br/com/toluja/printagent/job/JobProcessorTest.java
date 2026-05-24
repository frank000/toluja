package br.com.toluja.printagent.job;

import br.com.toluja.printagent.api.dto.JobDelivery;
import br.com.toluja.printagent.api.dto.NextJobResponse;
import br.com.toluja.printagent.print.PrintDispatcher;
import br.com.toluja.printagent.print.PrintExecutor;
import br.com.toluja.printagent.print.PrintResult;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JobProcessorTest {
    @Test
    void createsSuccessAckWhenPrintSucceeds() {
        JobProcessor processor = new JobProcessor((delivery, payload) -> PrintResult.ok());

        var ack = processor.process(job(List.of(delivery("ok", "CUPS", "IMPRESSORA1", 1))));

        assertEquals("SUCCESS", ack.deliveries().getFirst().status());
        assertEquals(null, ack.deliveries().getFirst().errorMessage());
    }

    @Test
    void createsPartialErrorAckWhenOneDeliveryFails() {
        PrintExecutor executor = (delivery, payload) -> delivery.deliveryId().equals("ok")
                ? PrintResult.ok()
                : PrintResult.failed("falha de impressao");
        JobProcessor processor = new JobProcessor(executor);

        var ack = processor.process(job(List.of(
                delivery("ok", "CUPS", "IMPRESSORA1", 1),
                delivery("error", "CUPS", "IMPRESSORA2", 1)
        )));

        assertEquals("SUCCESS", ack.deliveries().get(0).status());
        assertEquals("ERROR", ack.deliveries().get(1).status());
        assertEquals("falha de impressao", ack.deliveries().get(1).errorMessage());
    }

    @Test
    void dispatcherRejectsUnknownChannel() {
        PrintDispatcher dispatcher = new PrintDispatcher(List.of(), 1000);

        PrintResult result = dispatcher.print(delivery("unknown", "RAW", "IMPRESSORA", 1), "teste".getBytes(StandardCharsets.UTF_8));

        assertEquals(false, result.success());
        assertEquals("Canal de impressao nao suportado: RAW", result.errorMessage());
    }

    @Test
    void dispatcherRejectsInvalidCopies() {
        PrintDispatcher dispatcher = new PrintDispatcher(List.of(), 1000);

        PrintResult result = dispatcher.print(delivery("copies", "CUPS", "IMPRESSORA", 0), "teste".getBytes(StandardCharsets.UTF_8));

        assertEquals(false, result.success());
        assertEquals("copies deve ser maior ou igual a 1", result.errorMessage());
    }

    private NextJobResponse job(List<JobDelivery> deliveries) {
        return new NextJobResponse(
                "job",
                "tenant",
                "store",
                "device",
                "order",
                "TEXT",
                Base64.getEncoder().encodeToString("teste".getBytes(StandardCharsets.UTF_8)),
                OffsetDateTime.parse("2026-05-16T12:00:00Z"),
                deliveries
        );
    }

    private JobDelivery delivery(String id, String channel, String destination, int copies) {
        return new JobDelivery(id, "printer-" + id, "Printer " + id, channel, destination, copies);
    }
}
