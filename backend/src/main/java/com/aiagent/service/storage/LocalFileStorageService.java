package com.aiagent.service.storage;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class LocalFileStorageService implements FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootLocation);
            log.info("File storage initialized at: {}", rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize file storage", e);
        }
    }

    @Override
    public String store(MultipartFile file, String directory) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store empty file");
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            originalFileName = "unnamed";
        }

        String storedFileName = generateStoredFileName(originalFileName);
        Path targetDir = rootLocation.resolve(directory);
        Files.createDirectories(targetDir);

        Path targetPath = targetDir.resolve(storedFileName);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        String relativePath = directory + "/" + storedFileName;
        log.info("Stored file: {} -> {}", originalFileName, relativePath);
        return relativePath;
    }

    @Override
    public String store(InputStream inputStream, String fileName, String directory) throws IOException {
        String storedFileName = generateStoredFileName(fileName);
        Path targetDir = rootLocation.resolve(directory);
        Files.createDirectories(targetDir);

        Path targetPath = targetDir.resolve(storedFileName);
        Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);

        String relativePath = directory + "/" + storedFileName;
        log.info("Stored file: {} -> {}", fileName, relativePath);
        return relativePath;
    }

    @Override
    public Resource loadAsResource(String filePath) throws IOException {
        try {
            Path path = rootLocation.resolve(filePath).normalize();
            Resource resource = new UrlResource(path.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new IOException("File not found or not readable: " + filePath);
            }
        } catch (MalformedURLException e) {
            throw new IOException("Invalid file path: " + filePath, e);
        }
    }

    @Override
    public InputStream loadAsStream(String filePath) throws IOException {
        Path path = rootLocation.resolve(filePath).normalize();
        return Files.newInputStream(path);
    }

    @Override
    public boolean delete(String filePath) throws IOException {
        Path path = rootLocation.resolve(filePath).normalize();
        if (Files.exists(path)) {
            Files.delete(path);
            log.info("Deleted file: {}", filePath);
            return true;
        }
        return false;
    }

    @Override
    public boolean exists(String filePath) {
        Path path = rootLocation.resolve(filePath).normalize();
        return Files.exists(path);
    }

    @Override
    public long getFileSize(String filePath) throws IOException {
        Path path = rootLocation.resolve(filePath).normalize();
        return Files.size(path);
    }

    @Override
    public String getContentType(String filePath) {
        try {
            Path path = rootLocation.resolve(filePath).normalize();
            String contentType = Files.probeContentType(path);
            return contentType != null ? contentType : "application/octet-stream";
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

    private String generateStoredFileName(String originalFileName) {
        String extension = "";
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFileName.substring(dotIndex);
        }
        return UUID.randomUUID().toString() + extension;
    }

    public Path getRootLocation() {
        return rootLocation;
    }
}
