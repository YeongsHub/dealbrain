package com.example.sales.service;

import com.example.sales.model.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalesService {

    private final FileService fileService;

    public AnalyzeResponse analyze(List<MultipartFile> files) {
        List<FileInfo> fileInfos = new ArrayList<>();
        List<DealAnalysisResponse> deals = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                fileService.validateFile(file);

                FileInfo fileInfo = FileInfo.builder()
                        .fileName(file.getOriginalFilename())
                        .fileType(fileService.getFileType(file))
                        .fileSize(file.getSize())
                        .status("PROCESSED")
                        .build();
                fileInfos.add(fileInfo);

                // Mock: Generate sample deals for each file
                if (fileService.isCsvFile(file)) {
                    deals.addAll(generateMockDeals());
                }
                // PDF files will be processed for RAG in Stage 4

            } catch (Exception e) {
                log.error("Failed to process file: {}", file.getOriginalFilename(), e);
                FileInfo failedFile = FileInfo.builder()
                        .fileName(file.getOriginalFilename())
                        .fileType("UNKNOWN")
                        .fileSize(file.getSize())
                        .status("FAILED")
                        .build();
                fileInfos.add(failedFile);
            }
        }

        AnalysisSummary summary = calculateSummary(deals);

        return AnalyzeResponse.builder()
                .files(fileInfos)
                .deals(deals)
                .summary(summary)
                .build();
    }

    private List<DealAnalysisResponse> generateMockDeals() {
        List<DealAnalysisResponse> deals = new ArrayList<>();

        // Mock Deal 1
        deals.add(DealAnalysisResponse.builder()
                .dealId("DEAL-0001")
                .companyName("Samsung Electronics")
                .contactInfo(ContactInfo.builder()
                        .name("James Kim")
                        .email("james.kim@samsung.com")
                        .title("Senior Manager")
                        .build())
                .dealStage("Qualification")
                .dealValue(500000L)
                .currency("KRW")
                .lastContact(LocalDate.now().minusDays(3))
                .nextMeeting(LocalDate.now().plusDays(4))
                .probability(ProbabilityResult.builder()
                        .successRate(35)
                        .confidenceLevel("Medium")
                        .factors(ProbabilityFactors.builder()
                                .positive(List.of(
                                        "Budget approved",
                                        "Technical review scheduled",
                                        "POC requested by customer"
                                ))
                                .negative(List.of(
                                        "Still in early Qualification stage",
                                        "Strong competitor (Microsoft Azure) present"
                                ))
                                .build())
                        .build())
                .painPoints("Data silos causing slow analytics performance")
                .competition("Microsoft Azure")
                .decisionMaker("CTO")
                .budgetStatus("Approved")
                .nextBestActions(List.of(
                        NextBestAction.builder()
                                .priority(1)
                                .action("Schedule POC kick-off meeting within 5 days")
                                .rationale("Customer explicitly requested POC - demonstrate value quickly")
                                .deadline(LocalDate.now().plusDays(5))
                                .build(),
                        NextBestAction.builder()
                                .priority(2)
                                .action("Prepare comparative analysis vs Microsoft Azure")
                                .rationale("Address competition proactively")
                                .deadline(LocalDate.now().plusDays(7))
                                .build(),
                        NextBestAction.builder()
                                .priority(3)
                                .action("Identify additional stakeholders beyond CTO")
                                .rationale("Expand influence to de-risk single-threaded dependency")
                                .deadline(LocalDate.now().plusDays(10))
                                .build()
                ))
                .salesRep("John Smith")
                .region("Seoul")
                .notes("Technical review meeting scheduled. POC requested.")
                .build());

        // Mock Deal 2
        deals.add(DealAnalysisResponse.builder()
                .dealId("DEAL-0002")
                .companyName("LG Display")
                .contactInfo(ContactInfo.builder()
                        .name("Sarah Lee")
                        .email("sarah.lee@lgdisplay.com")
                        .title("IT Director")
                        .build())
                .dealStage("Proposal")
                .dealValue(850000L)
                .currency("KRW")
                .lastContact(LocalDate.now().minusDays(7))
                .nextMeeting(LocalDate.now().plusDays(2))
                .probability(ProbabilityResult.builder()
                        .successRate(55)
                        .confidenceLevel("Medium")
                        .factors(ProbabilityFactors.builder()
                                .positive(List.of(
                                        "Proposal submitted and under review",
                                        "Budget confirmed",
                                        "Strong technical fit identified"
                                ))
                                .negative(List.of(
                                        "Procurement process may take longer",
                                        "Internal reorganization pending"
                                ))
                                .build())
                        .build())
                .painPoints("Legacy system integration challenges")
                .competition("Oracle Cloud")
                .decisionMaker("CIO")
                .budgetStatus("Under Review")
                .nextBestActions(List.of(
                        NextBestAction.builder()
                                .priority(1)
                                .action("Follow up on proposal review status")
                                .rationale("7 days since last contact - maintain momentum")
                                .deadline(LocalDate.now().plusDays(1))
                                .build(),
                        NextBestAction.builder()
                                .priority(2)
                                .action("Prepare executive summary for CIO presentation")
                                .rationale("Scheduled meeting in 2 days requires preparation")
                                .deadline(LocalDate.now().plusDays(1))
                                .build()
                ))
                .salesRep("Emily Park")
                .region("Gyeonggi")
                .notes("Awaiting final budget approval. CIO meeting scheduled.")
                .build());

        return deals;
    }

    private AnalysisSummary calculateSummary(List<DealAnalysisResponse> deals) {
        if (deals.isEmpty()) {
            return AnalysisSummary.builder()
                    .totalDeals(0)
                    .avgProbability(0.0)
                    .highPriorityActions(0)
                    .build();
        }

        double avgProb = deals.stream()
                .mapToInt(d -> d.getProbability().getSuccessRate())
                .average()
                .orElse(0.0);

        int highPriorityCount = (int) deals.stream()
                .flatMap(d -> d.getNextBestActions().stream())
                .filter(a -> a.getPriority() == 1)
                .count();

        return AnalysisSummary.builder()
                .totalDeals(deals.size())
                .avgProbability(Math.round(avgProb * 10.0) / 10.0)
                .highPriorityActions(highPriorityCount)
                .build();
    }
}
