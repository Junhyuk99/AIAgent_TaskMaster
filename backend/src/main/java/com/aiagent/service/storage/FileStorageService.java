package com.aiagent.service.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public interface FileStorageService {

    String store(MultipartFile file, String directory) throws IOException;

    String store(InputStream inputStream, String fileName, String directory) throws IOException;

    Resource loadAsResource(String filePath) throws IOException;

    InputStream loadAsStream(String filePath) throws IOException;

    boolean delete(String filePath) throws IOException;

    boolean exists(String filePath);

    long getFileSize(String filePath) throws IOException;

    String getContentType(String filePath);
}
