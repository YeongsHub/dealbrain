package com.example.sales.service;

import com.example.sales.exception.InvalidFileTypeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileServiceTest {

    private FileService fileService;

    @BeforeEach
    void setUp() {
        fileService = new FileService();
        ReflectionTestUtils.setField(fileService, "maxFileSizeConfig", "10MB");
    }

    @Test
    @DisplayName("detectMimeType - should detect CSV file")
    void detectMimeType_CsvFile() {
        MockMultipartFile csvFile = new MockMultipartFile(
                "file",
                "deals.csv",
                "text/csv",
                "Deal_ID,Company_Name\nDEAL-001,Samsung".getBytes()
        );

        String mimeType = fileService.detectMimeType(csvFile);

        assertThat(mimeType).isIn("text/csv", "text/plain");
    }

    @Test
    @DisplayName("detectMimeType - should detect PDF file")
    void detectMimeType_PdfFile() {
        // PDF magic bytes
        byte[] pdfContent = "%PDF-1.4".getBytes();
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                pdfContent
        );

        String mimeType = fileService.detectMimeType(pdfFile);

        assertThat(mimeType).isEqualTo("application/pdf");
    }

    @Test
    @DisplayName("getFileType - should return CSV for csv file")
    void getFileType_CsvFile() {
        MockMultipartFile csvFile = new MockMultipartFile(
                "file",
                "deals.csv",
                "text/csv",
                "Deal_ID,Company_Name\nDEAL-001,Samsung".getBytes()
        );

        String fileType = fileService.getFileType(csvFile);

        assertThat(fileType).isEqualTo("CSV");
    }

    @Test
    @DisplayName("getFileType - should return PDF for pdf file")
    void getFileType_PdfFile() {
        byte[] pdfContent = "%PDF-1.4".getBytes();
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                pdfContent
        );

        String fileType = fileService.getFileType(pdfFile);

        assertThat(fileType).isEqualTo("PDF");
    }

    @Test
    @DisplayName("validateFile - should throw exception for invalid file type")
    void validateFile_InvalidType() {
        // Use actual binary content that Tika will detect as non-CSV/PDF
        byte[] pngHeader = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        MockMultipartFile pngFile = new MockMultipartFile(
                "file",
                "image.png",
                "image/png",
                pngHeader
        );

        assertThatThrownBy(() -> fileService.validateFile(pngFile))
                .isInstanceOf(InvalidFileTypeException.class);
    }

    @Test
    @DisplayName("isCsvFile - should return true for CSV")
    void isCsvFile_True() {
        MockMultipartFile csvFile = new MockMultipartFile(
                "file",
                "deals.csv",
                "text/csv",
                "header1,header2".getBytes()
        );

        assertThat(fileService.isCsvFile(csvFile)).isTrue();
    }

    @Test
    @DisplayName("isPdfFile - should return true for PDF")
    void isPdfFile_True() {
        byte[] pdfContent = "%PDF-1.4".getBytes();
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                pdfContent
        );

        assertThat(fileService.isPdfFile(pdfFile)).isTrue();
    }
}
