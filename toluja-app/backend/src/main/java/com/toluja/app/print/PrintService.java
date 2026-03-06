package com.toluja.app.print;

import com.toluja.app.dto.PrintAgentDtos;
import com.toluja.app.order.Order;
import com.toluja.app.order.OrderItem;
import com.toluja.app.printagent.PrintAgentJobService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class PrintService {

    private final String mode;
    private final String printer1;
    private final String printer2;
    private final String printer3;
    private final String agentDeviceId;
    private final String agentStoreId;
    private final TcpPrinterClient tcpPrinterClient;
    private final CupsPrinterClient cupsPrinterClient;
    private final PrintAgentJobService printAgentJobService;

    public PrintService(@Value("${printers.mode:direct}") String mode,
                        @Value("${printers.printer1}") String printer1,
                        @Value("${printers.printer2}") String printer2,
                        @Value("${printers.printer3:}") String printer3,
                        @Value("${printers.agent.device-id:agent-store-001}") String agentDeviceId,
                        @Value("${printers.agent.store-id:store-001}") String agentStoreId,
                        TcpPrinterClient tcpPrinterClient,
                        CupsPrinterClient cupsPrinterClient,
                        PrintAgentJobService printAgentJobService) {
        this.mode = mode;
        this.printer1 = printer1;
        this.printer2 = printer2;
        this.printer3 = printer3;
        this.agentDeviceId = agentDeviceId;
        this.agentStoreId = agentStoreId;
        this.tcpPrinterClient = tcpPrinterClient;
        this.cupsPrinterClient = cupsPrinterClient;
        this.printAgentJobService = printAgentJobService;
    }

    public void printOrder(Order order) throws Exception {
        String content = buildCoupon(order);
        if ("agent".equalsIgnoreCase(mode)) {
            enqueueForAgent(order, content);
            return;
        }
        if (isConfigured(printer1)) {
            printToConfigured(printer1, content);
        }
        if (isConfigured(printer2)) {
            printToConfigured(printer2, content);
        }
        if (isConfigured(printer3)) {
            printToConfigured(printer3, content);
        }
    }

    private void enqueueForAgent(Order order, String content) {
        List<PrintAgentDtos.JobDelivery> deliveries = new ArrayList<>();
        if (isConfigured(printer1)) {
            deliveries.add(toAgentDelivery(printer1, "printer-1", "Printer 1"));
        }
        if (isConfigured(printer2)) {
            deliveries.add(toAgentDelivery(printer2, "printer-2", "Printer 2"));
        }
        if (isConfigured(printer3)) {
            deliveries.add(toAgentDelivery(printer3, "printer-3", "Printer 3"));
        }
        if (deliveries.isEmpty()) {
            throw new IllegalArgumentException("Nenhuma impressora configurada para o modo agent");
        }

        PrintAgentDtos.NextJobResponse job = new PrintAgentDtos.NextJobResponse(
                UUID.randomUUID().toString(),
                order.getTenantId(),
                agentStoreId,
                agentDeviceId,
                String.valueOf(order.getId()),
                "TEXT",
                Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8)),
                order.getCriadoEm(),
                deliveries
        );
        printAgentJobService.enqueue(job);
    }

    private boolean isConfigured(String destination) {
        if (destination == null) {
            return false;
        }
        String normalized = destination.trim();
        return !normalized.isEmpty() && !"none".equalsIgnoreCase(normalized);
    }

    private PrintAgentDtos.JobDelivery toAgentDelivery(String destination, String printerId, String printerName) {
        if (destination.startsWith("cups://")) {
            return new PrintAgentDtos.JobDelivery(
                    UUID.randomUUID().toString(),
                    printerId,
                    printerName,
                    "CUPS",
                    destination.substring("cups://".length()),
                    1
            );
        }

        if (destination.startsWith("windows://")) {
            return new PrintAgentDtos.JobDelivery(
                    UUID.randomUUID().toString(),
                    printerId,
                    printerName,
                    "WINDOWS_QUEUE",
                    destination.substring("windows://".length()),
                    1
            );
        }

        // Destino lógico (ex.: IMPRESSORA1) tratado como fila CUPS local do agente.
        return new PrintAgentDtos.JobDelivery(
                UUID.randomUUID().toString(),
                printerId,
                printerName,
                "CUPS",
                destination,
                1
        );
    }

    private void printToConfigured(String destination, String content) throws Exception {
        if (destination.startsWith("tcp://")) {
            String hostPort = destination.substring("tcp://".length());
            String[] parts = hostPort.split(":");
            String host = parts[0];
            int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 9100;
            tcpPrinterClient.print(host, port, content);
            return;
        }

        if (destination.startsWith("cups://")) {
            String queueName = destination.substring("cups://".length());
            cupsPrinterClient.print(queueName, content);
            return;
        }

        if (destination.startsWith("windows://")) {
            throw new IllegalArgumentException("windows:// só é suportado no modo agent");
        }

        cupsPrinterClient.print(destination, content);
    }

    private String buildCoupon(Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== CUPOM DE PEDIDO ===\n");
        sb.append("Código: ").append(order.getCodigo()).append("\n");
        sb.append("Senha: ").append(String.format("%02d", order.getSenhaChamada())).append("\n");
        sb.append("Data/Hora: ").append(order.getCriadoEm().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))).append("\n");
        sb.append("Atendente: ").append(order.getUser().getNomeExibicao()).append("\n");
        sb.append("-------------------------\n");

        for (OrderItem item : order.getItens()) {
            sb.append(item.getNomeSnapshot())
                    .append(" | qtd: ").append(item.getQuantidade())
                    .append(" | preço: ").append(item.getPrecoSnapshot())
                    .append(" | subtotal: ").append(item.getSubtotal())
                    .append("\n");
            if (!item.getSubitens().isEmpty()) {
                for (var subitem : item.getSubitens()) {
                    sb.append("  + ").append(subitem.getCategoriaNomeSnapshot())
                            .append(": ").append(subitem.getNomeSnapshot())
                            .append(" (").append(subitem.getPrecoSnapshot()).append(")")
                            .append("\n");
                }
            }
        }

        sb.append("-------------------------\n");
        sb.append("Total: ").append(order.getTotal()).append("\n");
        sb.append("Observação: ").append(order.getObservacao() == null ? "-" : order.getObservacao()).append("\n");
        sb.append("=========================\n\n");
        return sb.toString();
    }
}
