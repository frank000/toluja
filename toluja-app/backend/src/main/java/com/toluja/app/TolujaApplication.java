package com.toluja.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
public class TolujaApplication {
    public static void main(String[] args) throws IOException {
        Files.createDirectories(Path.of("./data"));
        SpringApplication.run(TolujaApplication.class, args);
    }
}
