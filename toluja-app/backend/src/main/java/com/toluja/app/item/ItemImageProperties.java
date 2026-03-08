package com.toluja.app.item;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class ItemImageProperties {

    private final Path baseDir;
    private final int width;
    private final int height;
    private final float quality;
    private final String cwebpBin;

    public ItemImageProperties(
            @Value("${app.images.base-dir:${user.dir}/data/images}") String baseDir,
            @Value("${app.images.item.width:400}") int width,
            @Value("${app.images.item.height:300}") int height,
            @Value("${app.images.item.quality:0.8}") float quality,
            @Value("${app.images.webp.cwebp-bin:cwebp}") String cwebpBin
    ) {
        this.baseDir = Path.of(baseDir).toAbsolutePath().normalize();
        this.width = Math.max(64, width);
        this.height = Math.max(64, height);
        this.quality = Math.max(0.1f, Math.min(1.0f, quality));
        this.cwebpBin = cwebpBin;
    }

    public Path itemsDir() {
        return baseDir.resolve("items");
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public float quality() {
        return quality;
    }

    public String cwebpBin() {
        return cwebpBin;
    }
}
