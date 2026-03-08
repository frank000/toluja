package com.toluja.app.item;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ItemImageStorageService {

    private final ItemImageProperties properties;

    public String saveAsWebp(MultipartFile file, String tenantId) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!(originalName.endsWith(".jpg")
                || originalName.endsWith(".jpeg")
                || originalName.endsWith(".png")
                || originalName.endsWith(".webp"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato de imagem inválido. Use JPG, PNG ou WEBP.");
        }

        BufferedImage source;
        try (InputStream in = file.getInputStream()) {
            source = ImageIO.read(in);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falha ao ler imagem enviada");
        }
        if (source == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo enviado não é uma imagem válida");
        }

        BufferedImage resized = resizeAndCrop(source, properties.width(), properties.height());
        String fileName = tenantId + "_" + UUID.randomUUID() + ".webp";
        Path itemsDir = properties.itemsDir();
        Path target = itemsDir.resolve(fileName);

        try {
            Files.createDirectories(itemsDir);
            writeWebp(resized, target, properties.quality());
            return fileName;
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao salvar imagem do item: " + ex.getMessage());
        }
    }

    public Path resolvePublicPath(String fileName) {
        Path resolved = properties.itemsDir().resolve(fileName).normalize();
        if (!resolved.startsWith(properties.itemsDir())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Caminho de imagem inválido");
        }
        return resolved;
    }

    public String toPublicUrl(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }
        return "/api/public/item-images/" + imagePath;
    }

    public void deleteIfExists(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolvePublicPath(imagePath));
        } catch (Exception ignored) {
            // Falha de limpeza não deve impedir o fluxo principal.
        }
    }

    private void writeWebp(BufferedImage image, Path target, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType("image/webp");
        if (!writers.hasNext()) {
            writeWebpWithCwebp(image, target, quality);
            return;
        }

        ImageWriter writer = writers.next();
        Path tempFile = Files.createTempFile("item-image-", ".webp");
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(tempFile.toFile())) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
            }
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private void writeWebpWithCwebp(BufferedImage image, Path target, float quality) throws IOException {
        Path tempInput = Files.createTempFile("item-image-input-", ".png");
        Path tempOutput = Files.createTempFile("item-image-output-", ".webp");

        try {
            ImageIO.write(image, "png", tempInput.toFile());
            int qualityPercent = Math.max(1, Math.min(100, Math.round(quality * 100f)));

            Process process = new ProcessBuilder(
                    properties.cwebpBin(),
                    "-q", String.valueOf(qualityPercent),
                    tempInput.toString(),
                    "-o", tempOutput.toString()
            ).redirectErrorStream(true).start();

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            process.getInputStream().transferTo(output);
            int exitCode;
            try {
                exitCode = process.waitFor();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IOException("Conversão WEBP interrompida");
            }

            if (exitCode != 0 || !Files.exists(tempOutput) || Files.size(tempOutput) == 0) {
                String details = output.toString().trim();
                throw new IOException("Encoder WEBP não disponível. Instale o binário cwebp ou configure um plugin ImageIO. "
                        + (details.isBlank() ? "" : "Detalhes: " + details));
            }

            Files.move(tempOutput, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(tempInput);
            Files.deleteIfExists(tempOutput);
        }
    }

    private BufferedImage resizeAndCrop(BufferedImage source, int targetWidth, int targetHeight) {
        double sourceRatio = (double) source.getWidth() / source.getHeight();
        double targetRatio = (double) targetWidth / targetHeight;

        int scaledWidth;
        int scaledHeight;
        if (sourceRatio > targetRatio) {
            scaledHeight = targetHeight;
            scaledWidth = (int) Math.round(targetHeight * sourceRatio);
        } else {
            scaledWidth = targetWidth;
            scaledHeight = (int) Math.round(targetWidth / sourceRatio);
        }

        BufferedImage scaled = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(source, 0, 0, scaledWidth, scaledHeight, null);
        g2d.dispose();

        int x = Math.max(0, (scaledWidth - targetWidth) / 2);
        int y = Math.max(0, (scaledHeight - targetHeight) / 2);
        BufferedImage output = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = output.createGraphics();
        g.drawImage(scaled, -x, -y, null);
        g.dispose();
        return output;
    }
}
