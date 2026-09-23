package com.Luxurycars.carstore.service;

import com.Luxurycars.carstore.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final Path uploadDir;

    public FileStorageService(@Value("${app.upload.dir}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
            log.info("📁 Upload directory: " + this.uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + uploadDir, e);
        }
    }

    // ─── SAVE FILE ───
    // Returns the relative path like "1.jpg" (to store in DB)
    public String saveFile(MultipartFile file, Long carId) {
        // Validate
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Only image files are allowed");
        }
        if (file.getSize() > 5 * 1024 * 1024) {  // 5MB
            throw new BadRequestException("File size exceeds 5MB limit");
        }

        // Build filename: <carId>_<uuid>.<ext>
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = carId + "_" + UUID.randomUUID() + extension;

        // Save
        try {
            Path targetPath = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("✅ Saved file: " + targetPath);
            return filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save file: " + e.getMessage(), e);
        }
    }

    // ─── LOAD FILE BYTES ───
    public byte[] loadFile(String filename) {
        try {
            Path filePath = uploadDir.resolve(filename).normalize();
            if (!filePath.startsWith(uploadDir)) {
                throw new BadRequestException("Invalid file path");
            }
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new RuntimeException("File not found: " + filename, e);
        }
    }

    // ─── DELETE FILE ───
    public void deleteFile(String filename) {
        if (filename == null) return;
        try {
            Path filePath = uploadDir.resolve(filename).normalize();
            if (filePath.startsWith(uploadDir)) {
                Files.deleteIfExists(filePath);
                log.info("🗑️  Deleted file: " + filePath);
            }
        } catch (IOException e) {
            // Log but don't throw — deletion failure shouldn't crash
            log.warn("Failed to delete file: " + filename + " — " + e.getMessage());
        }
    }
}
