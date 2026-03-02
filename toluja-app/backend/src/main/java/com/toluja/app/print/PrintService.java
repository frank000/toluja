package com.toluja.app.print;

import com.toluja.app.order.Order;
import com.toluja.app.order.OrderItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
public class PrintService {

    private final String printer1;
    private final String printer2;
    private final TcpPrinterClient tcpPrinterClient;
    private final CupsPrinterClient cupsPrinterClient;

    public PrintService(@Value("${printers.printer1}") String printer1,
                        @Value("${printers.printer2}") String printer2,
                        TcpPrinterClient tcpPrinterClient,
                        CupsPrinterClient cupsPrinterClient) {
        this.printer1 = printer1;
        this.printer2 = printer2;
        this.tcpPrinterClient = tcpPrinterClient;
        this.cupsPrinterClient = cupsPrinterClient;
    }

    public void printOrder(Order order) throws Exception {
        String content = buildCoupon(order);
        printToConfigured(printer1, content);
        printToConfigured(printer2, content);
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

        throw new IllegalArgumentException("Formato de impressora inválido: " + destination);
    }

    private String buildCoupon(Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== CUPOM DE PEDIDO ===\n");
        sb.append("Código: ").append(order.getCodigo()).append("\n");
        sb.append("Data/Hora: ").append(order.getCriadoEm().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))).append("\n");
        sb.append("Atendente: ").append(order.getUser().getNomeExibicao()).append("\n");
        sb.append("-------------------------\n");

        for (OrderItem item : order.getItens()) {
            sb.append(item.getNomeSnapshot())
                    .append(" | qtd: ").append(item.getQuantidade())
                    .append(" | preço: ").append(item.getPrecoSnapshot())
                    .append(" | subtotal: ").append(item.getSubtotal())
                    .append("\n");
        }

        sb.append("-------------------------\n");
        sb.append("Total: ").append(order.getTotal()).append("\n");
        sb.append("Observação: ").append(order.getObservacao() == null ? "-" : order.getObservacao()).append("\n");
        sb.append("=========================\n\n");
        return sb.toString();
    }
}
