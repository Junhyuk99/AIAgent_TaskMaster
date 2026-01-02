package com.aiagent.util;

import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

public class FileValidationUtil {

    public static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

    public static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            // Documents
            "pdf", "doc", "docx", "txt", "md", "rtf", "odt",
            // Spreadsheets
            "xls", "xlsx", "csv", "ods",
            // Presentations
            "ppt", "pptx", "odp",
            // Images
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg",
            // Data formats
            "json", "xml", "yaml", "yml",
            // Code
            "html", "htm", "css", "js", "ts", "java", "py", "go", "rs", "cpp", "c", "h"
    );

    public static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            // Documents
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain",
            "text/markdown",
            "application/rtf",
            "application/vnd.oasis.opendocument.text",
            // Spreadsheets
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/csv",
            "application/vnd.oasis.opendocument.spreadsheet",
            // Presentations
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.oasis.opendocument.presentation",
            // Images
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/bmp",
            "image/webp",
            "image/svg+xml",
            // Data formats
            "application/json",
            "application/xml",
            "text/xml",
            "text/yaml",
            "application/x-yaml",
            // Code
            "text/html",
            "text/css",
            "application/javascript",
            "text/javascript",
            "application/x-javascript"
    );

    public static ValidationResult validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ValidationResult.error("File is empty or not provided");
        }

        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            return ValidationResult.error(
                    String.format("File size exceeds maximum limit of %dMB", MAX_FILE_SIZE / (1024 * 1024))
            );
        }

        // Check file extension
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            return ValidationResult.error("File name is required");
        }

        String extension = getFileExtension(fileName);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            return ValidationResult.error(
                    String.format("File extension '%s' is not allowed", extension)
            );
        }

        // Check content type (optional, as it can be spoofed)
        String contentType = file.getContentType();
        if (contentType != null && !ALLOWED_CONTENT_TYPES.contains(contentType) &&
                !contentType.startsWith("text/")) {
            // Allow any text/* content type
            return ValidationResult.error(
                    String.format("Content type '%s' is not allowed", contentType)
            );
        }

        return ValidationResult.success();
    }

    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1);
    }

    public static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    public record ValidationResult(boolean valid, String errorMessage) {
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }
    }
}
