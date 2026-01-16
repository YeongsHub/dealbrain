package com.example.sales.service;

import com.example.sales.model.dto.*;
import com.example.sales.model.entity.Deal;
import com.example.sales.model.entity.User;
import com.example.sales.model.enums.BudgetStatus;
import com.example.sales.model.enums.DealStage;
import com.example.sales.repository.DealRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesServiceTest {

    @Mock
    private FileService fileService;

    @Mock
    private CsvParsingService csvParsingService;

    @Mock
    private ProbabilityCalculationService probabilityCalculationService;

    @Mock
    private NbaGenerationService nbaGenerationService;

    @Mock
    private DealRepository dealRepository;

    @InjectMocks
    private SalesService salesService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .build();
    }

    @Test
    @DisplayName("analyze - should process CSV file and return analyzed deals")
    void analyze_CsvFile_ReturnsAnalyzedDeals() {
        // Given
        MockMultipartFile csvFile = new MockMultipartFile(
                "file",
                "deals.csv",
                "text/csv",
                "Deal_ID,Company_Name\nDEAL-001,Samsung".getBytes()
        );

        Deal mockDeal = createMockDeal();
        List<Deal> parsedDeals = List.of(mockDeal);

        when(fileService.getFileType(any(MultipartFile.class))).thenReturn("CSV");
        when(fileService.isCsvFile(any(MultipartFile.class))).thenReturn(true);
        doNothing().when(fileService).validateFile(any(MultipartFile.class));
        when(csvParsingService.parseCsvFile(any(MultipartFile.class), eq(testUser))).thenReturn(parsedDeals);
        when(dealRepository.saveAll(anyList())).thenReturn(parsedDeals);
        when(probabilityCalculationService.calculateProbability(any(Deal.class)))
                .thenReturn(createMockProbabilityResult());
        when(nbaGenerationService.generateActions(any(Deal.class)))
                .thenReturn(createMockActions());

        // When
        AnalyzeResponse response = salesService.analyze(List.of(csvFile), testUser);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getFiles()).hasSize(1);
        assertThat(response.getFiles().get(0).getStatus()).isEqualTo("PROCESSED");
        assertThat(response.getDeals()).hasSize(1);

        verify(fileService).validateFile(csvFile);
        verify(csvParsingService).parseCsvFile(csvFile, testUser);
        verify(dealRepository).saveAll(parsedDeals);
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
        when(dealRepository.saveAll(anyList())).thenReturn(List.of());

        // When
        AnalyzeResponse response = salesService.analyze(List.of(pdfFile), testUser);

        // Then
        assertThat(response.getFiles()).hasSize(1);
        assertThat(response.getFiles().get(0).getFileType()).isEqualTo("PDF");
        assertThat(response.getFiles().get(0).getStatus()).isEqualTo("PROCESSED");
        // PDF files don't generate deals (will be used for RAG in Stage 4)
        assertThat(response.getDeals()).isEmpty();

        verify(csvParsingService, never()).parseCsvFile(any(), any());
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

        Deal mockDeal = createMockDeal();
        List<Deal> parsedDeals = List.of(mockDeal);

        when(fileService.getFileType(csvFile)).thenReturn("CSV");
        when(fileService.getFileType(pdfFile)).thenReturn("PDF");
        when(fileService.isCsvFile(csvFile)).thenReturn(true);
        when(fileService.isCsvFile(pdfFile)).thenReturn(false);
        doNothing().when(fileService).validateFile(any(MultipartFile.class));
        when(csvParsingService.parseCsvFile(eq(csvFile), eq(testUser))).thenReturn(parsedDeals);
        when(dealRepository.saveAll(anyList())).thenReturn(parsedDeals);
        when(probabilityCalculationService.calculateProbability(any(Deal.class)))
                .thenReturn(createMockProbabilityResult());
        when(nbaGenerationService.generateActions(any(Deal.class)))
                .thenReturn(createMockActions());

        // When
        AnalyzeResponse response = salesService.analyze(List.of(csvFile, pdfFile), testUser);

        // Then
        assertThat(response.getFiles()).hasSize(2);
        verify(fileService, times(2)).validateFile(any(MultipartFile.class));
    }

    @Test
    @DisplayName("analyze - analyzed deals should have all required fields")
    void analyze_AnalyzedDeals_HaveRequiredFields() {
        // Given
        MockMultipartFile csvFile = new MockMultipartFile(
                "file", "deals.csv", "text/csv", "data".getBytes()
        );

        Deal mockDeal = createMockDeal();
        List<Deal> parsedDeals = List.of(mockDeal);

        when(fileService.getFileType(any(MultipartFile.class))).thenReturn("CSV");
        when(fileService.isCsvFile(any(MultipartFile.class))).thenReturn(true);
        doNothing().when(fileService).validateFile(any(MultipartFile.class));
        when(csvParsingService.parseCsvFile(any(MultipartFile.class), eq(testUser))).thenReturn(parsedDeals);
        when(dealRepository.saveAll(anyList())).thenReturn(parsedDeals);
        when(probabilityCalculationService.calculateProbability(any(Deal.class)))
                .thenReturn(createMockProbabilityResult());
        when(nbaGenerationService.generateActions(any(Deal.class)))
                .thenReturn(createMockActions());

        // When
        AnalyzeResponse response = salesService.analyze(List.of(csvFile), testUser);

        // Then
        assertThat(response.getDeals()).hasSize(1);

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

        Deal mockDeal1 = createMockDeal();
        Deal mockDeal2 = createMockDeal();
        mockDeal2.setDealId("DEAL-002");
        List<Deal> parsedDeals = List.of(mockDeal1, mockDeal2);

        ProbabilityResult prob1 = ProbabilityResult.builder()
                .successRate(40)
                .confidenceLevel("Medium")
                .factors(ProbabilityFactors.builder().positive(List.of()).negative(List.of()).build())
                .build();
        ProbabilityResult prob2 = ProbabilityResult.builder()
                .successRate(60)
                .confidenceLevel("Medium")
                .factors(ProbabilityFactors.builder().positive(List.of()).negative(List.of()).build())
                .build();

        when(fileService.getFileType(any(MultipartFile.class))).thenReturn("CSV");
        when(fileService.isCsvFile(any(MultipartFile.class))).thenReturn(true);
        doNothing().when(fileService).validateFile(any(MultipartFile.class));
        when(csvParsingService.parseCsvFile(any(MultipartFile.class), eq(testUser))).thenReturn(parsedDeals);
        when(dealRepository.saveAll(anyList())).thenReturn(parsedDeals);
        when(probabilityCalculationService.calculateProbability(mockDeal1)).thenReturn(prob1);
        when(probabilityCalculationService.calculateProbability(mockDeal2)).thenReturn(prob2);
        when(nbaGenerationService.generateActions(any(Deal.class)))
                .thenReturn(List.of(NextBestAction.builder()
                        .priority(1)
                        .action("Test action")
                        .rationale("Test rationale")
                        .deadline(LocalDate.now().plusDays(5))
                        .build()));

        // When
        AnalyzeResponse response = salesService.analyze(List.of(csvFile), testUser);

        // Then
        assertThat(response.getSummary()).isNotNull();
        assertThat(response.getSummary().getTotalDeals()).isEqualTo(2);
        assertThat(response.getSummary().getAvgProbability()).isEqualTo(50.0); // (40 + 60) / 2
        assertThat(response.getSummary().getHighPriorityActions()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("analyze - empty deals list should return zero summary")
    void analyze_EmptyDeals_ZeroSummary() {
        // Given
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file", "notes.pdf", "application/pdf", "data".getBytes()
        );

        when(fileService.getFileType(any(MultipartFile.class))).thenReturn("PDF");
        when(fileService.isCsvFile(any(MultipartFile.class))).thenReturn(false);
        doNothing().when(fileService).validateFile(any(MultipartFile.class));
        when(dealRepository.saveAll(anyList())).thenReturn(List.of());

        // When
        AnalyzeResponse response = salesService.analyze(List.of(pdfFile), testUser);

        // Then
        assertThat(response.getSummary().getTotalDeals()).isEqualTo(0);
        assertThat(response.getSummary().getAvgProbability()).isEqualTo(0.0);
        assertThat(response.getSummary().getHighPriorityActions()).isEqualTo(0);
    }

    @Test
    @DisplayName("analyze - should format deal stage correctly")
    void analyze_FormatsDealStageCorrectly() {
        // Given
        MockMultipartFile csvFile = new MockMultipartFile(
                "file", "deals.csv", "text/csv", "data".getBytes()
        );

        Deal mockDeal = createMockDeal();
        mockDeal.setDealStage(DealStage.CLOSED_WON);
        List<Deal> parsedDeals = List.of(mockDeal);

        when(fileService.getFileType(any(MultipartFile.class))).thenReturn("CSV");
        when(fileService.isCsvFile(any(MultipartFile.class))).thenReturn(true);
        doNothing().when(fileService).validateFile(any(MultipartFile.class));
        when(csvParsingService.parseCsvFile(any(MultipartFile.class), eq(testUser))).thenReturn(parsedDeals);
        when(dealRepository.saveAll(anyList())).thenReturn(parsedDeals);
        when(probabilityCalculationService.calculateProbability(any(Deal.class)))
                .thenReturn(createMockProbabilityResult());
        when(nbaGenerationService.generateActions(any(Deal.class))).thenReturn(List.of());

        // When
        AnalyzeResponse response = salesService.analyze(List.of(csvFile), testUser);

        // Then
        assertThat(response.getDeals().get(0).getDealStage()).isEqualTo("Closed Won");
    }

    @Test
    @DisplayName("analyze - should format budget status correctly")
    void analyze_FormatsBudgetStatusCorrectly() {
        // Given
        MockMultipartFile csvFile = new MockMultipartFile(
                "file", "deals.csv", "text/csv", "data".getBytes()
        );

        Deal mockDeal = createMockDeal();
        mockDeal.setBudgetStatus(BudgetStatus.UNDER_REVIEW);
        List<Deal> parsedDeals = List.of(mockDeal);

        when(fileService.getFileType(any(MultipartFile.class))).thenReturn("CSV");
        when(fileService.isCsvFile(any(MultipartFile.class))).thenReturn(true);
        doNothing().when(fileService).validateFile(any(MultipartFile.class));
        when(csvParsingService.parseCsvFile(any(MultipartFile.class), eq(testUser))).thenReturn(parsedDeals);
        when(dealRepository.saveAll(anyList())).thenReturn(parsedDeals);
        when(probabilityCalculationService.calculateProbability(any(Deal.class)))
                .thenReturn(createMockProbabilityResult());
        when(nbaGenerationService.generateActions(any(Deal.class))).thenReturn(List.of());

        // When
        AnalyzeResponse response = salesService.analyze(List.of(csvFile), testUser);

        // Then
        assertThat(response.getDeals().get(0).getBudgetStatus()).isEqualTo("Under Review");
    }

    private Deal createMockDeal() {
        return Deal.builder()
                .id(1L)
                .dealId("DEAL-001")
                .companyName("Samsung Electronics")
                .contactName("James Kim")
                .contactEmail("james.kim@samsung.com")
                .contactTitle("Senior Manager")
                .dealStage(DealStage.QUALIFICATION)
                .dealValue(BigDecimal.valueOf(500000))
                .productInterest("Enterprise AI Suite")
                .painPoints("Data silos causing slow analytics")
                .competition("Microsoft Azure")
                .decisionMaker("CTO")
                .budgetStatus(BudgetStatus.APPROVED)
                .salesRep("John Smith")
                .region("Seoul")
                .lastContact(LocalDate.now().minusDays(3))
                .nextMeeting(LocalDate.now().plusDays(4))
                .notes("Technical review meeting scheduled. POC requested.")
                .user(testUser)
                .build();
    }

    private ProbabilityResult createMockProbabilityResult() {
        return ProbabilityResult.builder()
                .successRate(45)
                .confidenceLevel("Medium")
                .factors(ProbabilityFactors.builder()
                        .positive(List.of("Budget approved", "Meeting scheduled"))
                        .negative(List.of("Competition present"))
                        .build())
                .build();
    }

    private List<NextBestAction> createMockActions() {
        return List.of(
                NextBestAction.builder()
                        .priority(1)
                        .action("Schedule POC kick-off meeting")
                        .rationale("Customer requested POC")
                        .deadline(LocalDate.now().plusDays(5))
                        .build(),
                NextBestAction.builder()
                        .priority(2)
                        .action("Prepare competitive analysis")
                        .rationale("Address competition proactively")
                        .deadline(LocalDate.now().plusDays(7))
                        .build()
        );
    }
}
