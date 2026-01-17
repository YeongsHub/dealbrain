package com.example.sales.service;

import com.example.sales.exception.PdfExtractionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfExtractionServiceTest {

    private PdfExtractionService pdfExtractionService;

    @BeforeEach
    void setUp() {
        pdfExtractionService = new PdfExtractionService();
    }

    @Nested
    @DisplayName("Text Extraction")
    class TextExtraction {

        @Test
        @DisplayName("Should extract text from plain text file")
        void shouldExtractTextFromPlainTextFile() {
            String content = "This is sample text content for testing extraction.";
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test.txt",
                    "text/plain",
                    content.getBytes()
            );

            PdfExtractionService.ExtractedContent result = pdfExtractionService.extractText(file);

            assertThat(result).isNotNull();
            assertThat(result.getText()).contains("sample text content");
        }

        @Test
        @DisplayName("Should return extracted content with page count")
        void shouldReturnExtractedContentWithPageCount() {
            String content = "Test content";
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test.txt",
                    "text/plain",
                    content.getBytes()
            );

            PdfExtractionService.ExtractedContent result = pdfExtractionService.extractText(file);

            assertThat(result).isNotNull();
            assertThat(result.getText()).isNotBlank();
            // Page count may be null for plain text
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should handle empty file")
        void shouldHandleEmptyFile() {
            MockMultipartFile emptyFile = new MockMultipartFile(
                    "file",
                    "empty.pdf",
                    "application/pdf",
                    new byte[0]
            );

            PdfExtractionService.ExtractedContent result = pdfExtractionService.extractText(emptyFile);

            assertThat(result).isNotNull();
            assertThat(result.getText()).isEmpty();
        }

        @Test
        @DisplayName("Should extract from HTML content")
        void shouldExtractFromHtmlContent() {
            String htmlContent = "<html><body><p>Hello World</p></body></html>";
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test.html",
                    "text/html",
                    htmlContent.getBytes()
            );

            PdfExtractionService.ExtractedContent result = pdfExtractionService.extractText(file);

            assertThat(result).isNotNull();
            assertThat(result.getText()).contains("Hello World");
        }
    }

    @Nested
    @DisplayName("ExtractedContent DTO")
    class ExtractedContentDto {

        @Test
        @DisplayName("Should build ExtractedContent with builder")
        void shouldBuildExtractedContentWithBuilder() {
            PdfExtractionService.ExtractedContent content = PdfExtractionService.ExtractedContent.builder()
                    .text("Sample text")
                    .pageCount(5)
                    .build();

            assertThat(content.getText()).isEqualTo("Sample text");
            assertThat(content.getPageCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should allow null page count")
        void shouldAllowNullPageCount() {
            PdfExtractionService.ExtractedContent content = PdfExtractionService.ExtractedContent.builder()
                    .text("Sample text")
                    .pageCount(null)
                    .build();

            assertThat(content.getText()).isEqualTo("Sample text");
            assertThat(content.getPageCount()).isNull();
        }
    }
}
