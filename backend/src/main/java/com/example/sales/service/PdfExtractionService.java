package com.example.sales.service;

import com.example.sales.exception.PdfExtractionException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfExtractionService {

    public ExtractedContent extractText(MultipartFile file) {
        // Handle empty files gracefully
        if (file.isEmpty() || file.getSize() == 0) {
            log.info("Empty file received: {}", file.getOriginalFilename());
            return ExtractedContent.builder()
                    .text("")
                    .pageCount(null)
                    .build();
        }

        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();

        // Check if it's a PDF file
        if (contentType != null && contentType.equals("application/pdf") ||
                (fileName != null && fileName.toLowerCase().endsWith(".pdf"))) {
            return extractFromPdf(file);
        }

        // For other text-based files, read as plain text
        return extractAsPlainText(file);
    }

    private ExtractedContent extractFromPdf(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            try (PDDocument document = Loader.loadPDF(bytes)) {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document).trim();
                int pageCount = document.getNumberOfPages();

                log.info("Extracted {} characters from {} ({} pages)",
                        text.length(), file.getOriginalFilename(), pageCount);

                return ExtractedContent.builder()
                        .text(text)
                        .pageCount(pageCount)
                        .build();
            }
        } catch (IOException e) {
            log.error("Failed to extract text from PDF: {}", file.getOriginalFilename(), e);
            throw new PdfExtractionException("Failed to extract text from PDF: " + e.getMessage());
        }
    }

    private ExtractedContent extractAsPlainText(MultipartFile file) {
        try {
            String text = new String(file.getBytes(), StandardCharsets.UTF_8).trim();

            log.info("Extracted {} characters from {}", text.length(), file.getOriginalFilename());

            return ExtractedContent.builder()
                    .text(text)
                    .pageCount(null)
                    .build();
        } catch (IOException e) {
            log.error("Failed to read file as text: {}", file.getOriginalFilename(), e);
            throw new PdfExtractionException("Failed to read file: " + e.getMessage());
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class ExtractedContent {
        private String text;
        private Integer pageCount;
    }
}
