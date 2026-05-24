package br.com.toluja.printagent.api;

import br.com.toluja.printagent.api.dto.AckRequest;
import br.com.toluja.printagent.api.dto.DeliveryAck;
import br.com.toluja.printagent.config.AgentConfig;
import br.com.toluja.printagent.config.PrinterConfig;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrintAgentClientTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void fetchesNextJobAndSendsAck() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        StringBuilder ackBody = new StringBuilder();
        server.createContext("/api/print-agent/jobs/next", exchange -> {
            assertEquals("SECRET", exchange.getRequestHeaders().getFirst("X-Print-Key"));
            byte[] response = """
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
                          "copies": 1
                        }
                      ]
                    }
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.createContext("/api/print-agent/jobs/job-1/ack", exchange -> {
            ackBody.append(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"jobId\":\"job-1\",\"status\":\"SUCCESS\",\"receivedDeliveries\":1}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        PrintAgentClient client = new PrintAgentClient(config(server.getAddress().getPort()));
        var job = client.fetchNextJob();
        var ack = client.sendAck("job-1", new AckRequest(List.of(DeliveryAck.success("delivery-1"))));

        assertTrue(job.isPresent());
        assertEquals("job-1", job.get().jobId());
        assertEquals("SUCCESS", ack.status());
        assertTrue(ackBody.toString().contains("\"status\":\"SUCCESS\""));
    }

    @Test
    void returnsEmptyWhenNoJob() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/print-agent/jobs/next", exchange -> {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();

        PrintAgentClient client = new PrintAgentClient(config(server.getAddress().getPort()));

        assertTrue(client.fetchNextJob().isEmpty());
    }

    private AgentConfig config(int port) {
        return new AgentConfig(
                "http://127.0.0.1:" + port,
                "tenant",
                "store",
                "device",
                "SECRET",
                1000,
                2000,
                1,
                0,
                30000,
                List.of(new PrinterConfig("p", "P", "CUPS", "P"))
        );
    }
}
