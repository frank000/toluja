package com.toluja.app.printagent;

import com.toluja.app.dto.PrintAgentDtos;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class PrintAgentJobService {

    private final Map<String, Deque<PrintAgentDtos.NextJobResponse>> queueByTenantAndDevice = new ConcurrentHashMap<>();
    private final Map<String, ReservedJobState> pendingByJobId = new ConcurrentHashMap<>();

    public PrintAgentDtos.NextJobResponse reserveNextJob(String tenantId, String deviceId) {
        Deque<PrintAgentDtos.NextJobResponse> queue = queueByTenantAndDevice.get(queueKey(tenantId, deviceId));
        if (queue == null || queue.isEmpty()) {
            return null;
        }
        PrintAgentDtos.NextJobResponse job = queue.pollFirst();
        if (job == null) {
            return null;
        }

        Map<String, Boolean> pending = new HashMap<>();
        for (PrintAgentDtos.JobDelivery delivery : job.deliveries()) {
            pending.put(delivery.deliveryId(), true);
        }
        pendingByJobId.put(job.jobId(), new ReservedJobState(tenantId, deviceId, pending));
        return job;
    }

    public PrintAgentDtos.AckResponse ack(String jobId, String tenantId, PrintAgentDtos.AckRequest request) {
        ReservedJobState state = pendingByJobId.get(jobId);
        if (state == null) {
            throw new ResponseStatusException(NOT_FOUND, "Job não encontrado ou já confirmado");
        }
        if (!Objects.equals(state.tenantId(), tenantId)) {
            throw new ResponseStatusException(NOT_FOUND, "Job não pertence ao tenant autenticado");
        }

        Map<String, Boolean> pending = state.pendingDeliveries();
        if (request.deliveries().isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "ACK sem entregas");
        }

        int received = 0;
        boolean allSuccess = true;
        for (PrintAgentDtos.DeliveryAck deliveryAck : request.deliveries()) {
            if (!pending.containsKey(deliveryAck.deliveryId())) {
                throw new ResponseStatusException(BAD_REQUEST, "deliveryId inválido no ACK: " + deliveryAck.deliveryId());
            }
            if (!Objects.equals(deliveryAck.status(), "SUCCESS") && !Objects.equals(deliveryAck.status(), "ERROR")) {
                throw new ResponseStatusException(BAD_REQUEST, "status de entrega inválido: " + deliveryAck.status());
            }
            pending.put(deliveryAck.deliveryId(), false);
            received++;
            allSuccess = allSuccess && Objects.equals(deliveryAck.status(), "SUCCESS");
        }

        boolean hasPending = pending.values().stream().anyMatch(Boolean::booleanValue);
        if (!hasPending) {
            pendingByJobId.remove(jobId);
        }

        return new PrintAgentDtos.AckResponse(jobId, allSuccess ? "SUCCESS" : "PARTIAL_OR_ERROR", received);
    }

    public PrintAgentDtos.NextJobResponse enqueue(PrintAgentDtos.NextJobResponse request) {
        queueByTenantAndDevice
                .computeIfAbsent(queueKey(request.tenantId(), request.deviceId()), key -> new ArrayDeque<>())
                .addLast(request);
        return request;
    }

    @PostConstruct
    void seedSampleJob() {
        String payload = java.util.Base64.getEncoder().encodeToString("""
                === CUPOM DE PEDIDO ===
                Pedido #ABC123
                1x X-Burger
                Total: R$ 22,00
                """.getBytes());

        List<PrintAgentDtos.JobDelivery> deliveries = new ArrayList<>();
        deliveries.add(new PrintAgentDtos.JobDelivery(
                UUID.randomUUID().toString(),
                "balcao-usb-1",
                "Balcao USB",
                "CUPS",
                "EPSON_TM_T20X",
                1
        ));
        deliveries.add(new PrintAgentDtos.JobDelivery(
                UUID.randomUUID().toString(),
                "cozinha-usb-1",
                "Cozinha USB",
                "CUPS",
                "BEMATECH_MP4200",
                1
        ));

        enqueue(new PrintAgentDtos.NextJobResponse(
                UUID.randomUUID().toString(),
                "default",
                "store-001",
                "agent-store-001",
                "order-abc123",
                "TEXT",
                payload,
                OffsetDateTime.now(),
                deliveries
        ));
    }

    private String queueKey(String tenantId, String deviceId) {
        return tenantId + "::" + deviceId;
    }

    private record ReservedJobState(String tenantId, String deviceId, Map<String, Boolean> pendingDeliveries) {}
}
