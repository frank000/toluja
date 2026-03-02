package com.toluja.app.print;

import org.springframework.stereotype.Component;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@Component
public class TcpPrinterClient implements PrinterClient {

    public void print(String host, int port, String content) throws Exception {
        try (Socket socket = new Socket(host, port);
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {
            writer.print(content);
            writer.flush();
        }
    }

    @Override
    public void print(String content) {
        throw new UnsupportedOperationException("Use print(host, port, content)");
    }
}
