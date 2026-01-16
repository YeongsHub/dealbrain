package com.example.sales.service;

import com.example.sales.exception.FileSizeExceededException;
import com.example.sales.exception.InvalidFileTypeException;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@Service
@Slf4j
public class FileService {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "text/csv",
            "text/plain",  // CSV sometimes detected as text/plain
            "application/csv",
            "application/pdf"
    );

    private static final Set<String> CSV_MIME_TYPES = Set.of(
            "text/csv",
            "text/plain",
            "application/csv"
    );

    private static final String PDF_MIME_TYPE = "application/pdf";

    private final Tika tika = new Tika();

    @Value("${spring.servlet.multipart.max-file-size:10MB}")
    private String maxFileSizeConfig;

    private long getMaxFileSizeBytes() {
        String size = maxFileSizeConfig.toUpperCase().replace("MB", "").trim();
        return Long.parseLong(size) * 1024 * 1024;
    }

    public void validateFile(MultipartFile file) {
        validateFileSize(file);
        validateFileType(file);
    }

    private void validateFileSize(MultipartFile file) {
        long maxSize = getMaxFileSizeBytes();
        if (file.getSize() > maxSize) {
            throw new FileSizeExceededException(file.getOriginalFilename(), file.getSize(), maxSize);
        }
    }

    private void validateFileType(MultipartFile file) {
        String mimeType = detectMimeType(file);
        if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new InvalidFileTypeException(file.getOriginalFilename(), mimeType);
        }
    }

    public String detectMimeType(MultipartFile file) {
        try {
            return tika.detect(file.getInputStream(), file.getOriginalFilename());
        } catch (IOException e) {
            log.error("Failed to detect MIME type for file: {}", file.getOriginalFilename(), e);
            return "unknown";
        }
    }

    public String getFileType(MultipartFile file) {
        String mimeType = detectMimeType(file);
        if (CSV_MIME_TYPES.contains(mimeType)) {
            return "CSV";
        } else if (PDF_MIME_TYPE.equals(mimeType)) {
            return "PDF";
        }
        return "UNKNOWN";
    }

    public boolean isCsvFile(MultipartFile file) {
        return CSV_MIME_TYPES.contains(detectMimeType(file));
    }

    public boolean isPdfFile(MultipartFile file) {
        return PDF_MIME_TYPE.equals(detectMimeType(file));
    }
}
