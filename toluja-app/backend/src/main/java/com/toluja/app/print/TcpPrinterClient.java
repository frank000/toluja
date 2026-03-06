package com.toluja.app.print;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@Component
public class TcpPrinterClient implements PrinterClient {

    private final int connectTimeoutMs;

    public TcpPrinterClient(@Value("${printers.connect-timeout-ms:3000}") int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public void print(String host, int port, String content) throws Exception {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), connectTimeoutMs);
            socket.setSoTimeout(connectTimeoutMs);
            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {
                writer.print(content);
                writer.flush();
            }
        }
    }

    @Override
    public void print(String content) {
        throw new UnsupportedOperationException("Use print(host, port, content)");
    }
}
