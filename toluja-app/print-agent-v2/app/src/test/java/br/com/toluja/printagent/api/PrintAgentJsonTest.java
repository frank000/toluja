package br.com.toluja.printagent.api;

import br.com.toluja.printagent.api.dto.AckRequest;
import br.com.toluja.printagent.api.dto.DeliveryAck;
import br.com.toluja.printagent.api.dto.NextJobResponse;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrintAgentJsonTest {
    @Test
    void parsesNextJob() throws Exception {
        NextJobResponse job = PrintAgentJson.parseNextJob("""
                {
                  "jobId": "job-1",
                  "tenantId": "tenant",
                  "storeId": "store",
                  "deviceId": "device",
                  "orderId": "order",
                  "payloadType": "TEXT",
                  "payloadBase64": "SGVsbG8=",
                  "createdAt": "2026-05-16T12:00:00Z",
                  "deliveries": [
                    {
                      "deliveryId": "delivery-1",
                      "printerId": "printer",
                      "printerName": "Printer",
                      "channel": "CUPS",
                      "destination": "IMPRESSORA1",
                      "copies": 2
                    }
                  ]
                }
                """);

        assertEquals("job-1", job.jobId());
        assertEquals(OffsetDateTime.parse("2026-05-16T12:00:00Z"), job.createdAt());
        assertEquals("delivery-1", job.deliveries().getFirst().deliveryId());
        assertEquals(2, job.deliveries().getFirst().copies());
    }

    @Test
    void writesAckRequest() {
        String json = PrintAgentJson.writeAckRequest(new AckRequest(List.of(
                DeliveryAck.success("delivery-ok"),
                DeliveryAck.error("delivery-error", "falha")
        )));

        assertTrue(json.contains("\"deliveryId\":\"delivery-ok\""));
        assertTrue(json.contains("\"status\":\"SUCCESS\""));
        assertTrue(json.contains("\"errorMessage\":null"));
        assertTrue(json.contains("\"deliveryId\":\"delivery-error\""));
        assertTrue(json.contains("\"errorMessage\":\"falha\""));
    }

    @Test
    void parsesAckResponse() throws Exception {
        var response = PrintAgentJson.parseAckResponse("""
                {"jobId":"job-1","status":"SUCCESS","receivedDeliveries":1}
                """);

        assertEquals("job-1", response.jobId());
        assertEquals("SUCCESS", response.status());
        assertEquals(1, response.receivedDeliveries());
    }
}
