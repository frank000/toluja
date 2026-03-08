package com.toluja.app.item;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;

@Component
@RequiredArgsConstructor
public class ItemImageStorageInitializer implements CommandLineRunner {

    private final ItemImageProperties properties;

    @Override
    public void run(String... args) throws Exception {
        Files.createDirectories(properties.itemsDir());
    }
}
