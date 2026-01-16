package com.example.sales.service;

import com.example.sales.model.dto.AnalyzeResponse;
import com.example.sales.model.dto.DealAnalysisResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesServiceTest {

    @Mock
    private FileService fileService;

    @InjectMocks
    private SalesService salesService;

    @Test
    @DisplayName("analyze - should process CSV file and return mock deals")
    void analyze_CsvFile_ReturnsMockDeals() {
        // Given
        MockMultipartFile csvFile = new MockMultipartFile(
                "file",
                "deals.csv",
                "text/csv",
                "Deal_ID,Company_Name\nDEAL-001,Samsung".getBytes()
        );

        when(fileService.getFileType(any(MultipartFile.class))).thenReturn("CSV");
        when(fileService.isCsvFile(any(MultipartFile.class))).thenReturn(true);
        doNothing().when(fileService).validateFile(any(MultipartFile.class));

        // When
        AnalyzeResponse response = salesService.analyze(List.of(csvFile));

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getFiles()).hasSize(1);
        assertThat(response.getFiles().get(0).getStatus()).isEqualTo("PROCESSED");
        assertThat(response.getDeals()).isNotEmpty();
        assertThat(response.getSummary().getTotalDeals()).isEqualTo(response.getDeals().size());

        verify(fileService).validateFile(csvFile);
    }

    @Test
    @DisplayName("analyze - should return correct file info for PDF")
    void analyze_PdfFile_ReturnsFileInfo() {
        // Given
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "meeting_notes.pdf",
                "application/pdf",
                "%PDF-1.4".getBytes()
        );

        when(fileService.getFileType(any(MultipartFile.class))).thenReturn("PDF");
        when(fileService.isCsvFile(any(MultipartFile.class))).thenReturn(false);
        doNothing().when(fileService).validateFile(any(MultipartFile.class));

        // When
        AnalyzeResponse response = salesService.analyze(List.of(pdfFile));

        // Then
        assertThat(response.getFiles()).hasSize(1);
        assertThat(response.getFiles().get(0).getFileType()).isEqualTo("PDF");
        assertThat(response.getFiles().get(0).getStatus()).isEqualTo("PROCESSED");
        // PDF files don't generate mock deals (will be used for RAG in Stage 4)
        assertThat(response.getDeals()).isEmpty();
    }

    @Test
    @DisplayName("analyze - should handle multiple files")
    void analyze_MultipleFiles_ProcessesAll() {
        // Given
        MockMultipartFile csvFile = new MockMultipartFile(
                "file1", "deals.csv", "text/csv", "data".getBytes()
        );
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file2", "notes.pdf", "application/pdf", "%PDF-1.4".getBytes()
        );

        when(fileService.getFileType(csvFile)).thenReturn("CSV");
        when(fileService.getFileType(pdfFile)).thenReturn("PDF");
        when(fileService.isCsvFile(csvFile)).thenReturn(true);
        when(fileService.isCsvFile(pdfFile)).thenReturn(false);
        doNothing().when(fileService).validateFile(any(MultipartFile.class));

        // When
        AnalyzeResponse response = salesService.analyze(List.of(csvFile, pdfFile));

        // Then
        assertThat(response.getFiles()).hasSize(2);
        verify(fileService, times(2)).validateFile(any(MultipartFile.class));
    }

    @Test
    @DisplayName("analyze - mock deals should have required fields")
    void analyze_MockDeals_HaveRequiredFields() {
        // Given
        MockMultipartFile csvFile = new MockMultipartFile(
                "file", "deals.csv", "text/csv", "data".getBytes()
        );

        when(fileService.getFileType(any(MultipartFile.class))).thenReturn("CSV");
        when(fileService.isCsvFile(any(MultipartFile.class))).thenReturn(true);
        doNothing().when(fileService).validateFile(any(MultipartFile.class));

        // When
        AnalyzeResponse response = salesService.analyze(List.of(csvFile));

        // Then
        assertThat(response.getDeals()).isNotEmpty();

        DealAnalysisResponse deal = response.getDeals().get(0);
        assertThat(deal.getDealId()).isNotBlank();
        assertThat(deal.getCompanyName()).isNotBlank();
        assertThat(deal.getContactInfo()).isNotNull();
        assertThat(deal.getProbability()).isNotNull();
        assertThat(deal.getProbability().getSuccessRate()).isBetween(0, 100);
        assertThat(deal.getNextBestActions()).isNotEmpty();
    }

    @Test
    @DisplayName("analyze - summary should calculate correctly")
    void analyze_Summary_CalculatedCorrectly() {
        // Given
        MockMultipartFile csvFile = new MockMultipartFile(
                "file", "deals.csv", "text/csv", "data".getBytes()
        );

        when(fileService.getFileType(any(MultipartFile.class))).thenReturn("CSV");
        when(fileService.isCsvFile(any(MultipartFile.class))).thenReturn(true);
        doNothing().when(fileService).validateFile(any(MultipartFile.class));

        // When
        AnalyzeResponse response = salesService.analyze(List.of(csvFile));

        // Then
        assertThat(response.getSummary()).isNotNull();
        assertThat(response.getSummary().getTotalDeals()).isEqualTo(response.getDeals().size());
        assertThat(response.getSummary().getAvgProbability()).isGreaterThan(0);
        assertThat(response.getSummary().getHighPriorityActions()).isGreaterThanOrEqualTo(0);
    }
}
