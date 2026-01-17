package com.example.sales.service;

import com.example.sales.model.dto.*;
import com.example.sales.model.entity.Deal;
import com.example.sales.model.entity.Document;
import com.example.sales.model.entity.User;
import com.example.sales.repository.DealRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalesService {

    private final FileService fileService;
    private final CsvParsingService csvParsingService;
    private final ProbabilityCalculationService probabilityCalculationService;
    private final NbaGenerationService nbaGenerationService;
    private final DealRepository dealRepository;
    private final DocumentProcessingService documentProcessingService;

    @Transactional
    public AnalyzeResponse analyze(List<MultipartFile> files, User user) {
        List<FileInfo> fileInfos = new ArrayList<>();
        List<DealAnalysisResponse> dealResponses = new ArrayList<>();
        List<Deal> allDeals = new ArrayList<>();

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

                // Parse CSV files and extract deals
                if (fileService.isCsvFile(file)) {
                    List<Deal> parsedDeals = csvParsingService.parseCsvFile(file, user);
                    allDeals.addAll(parsedDeals);
                }

                // Process PDF files for RAG
                if (fileService.isPdfFile(file)) {
                    Document document = documentProcessingService.createDocument(file, user, null);
                    documentProcessingService.processDocumentAsync(document.getId(), file, user);
                    // Update status to indicate async processing
                    fileInfos.get(fileInfos.size() - 1).setStatus("PROCESSING");
                    log.info("Started async processing for PDF: {}", file.getOriginalFilename());
                }

            } catch (Exception e) {
                log.error("Failed to process file: {}", file.getOriginalFilename(), e);
                FileInfo failedFile = FileInfo.builder()
                        .fileName(file.getOriginalFilename())
                        .fileType("UNKNOWN")
                        .fileSize(file.getSize())
                        .status("FAILED")
                        .build();
                fileInfos.add(failedFile);
                throw e; // Re-throw to allow transaction rollback
            }
        }

        // Save all parsed deals to database
        List<Deal> savedDeals = dealRepository.saveAll(allDeals);
        log.info("Saved {} deals for user {}", savedDeals.size(), user.getEmail());

        // Calculate probability and generate NBA for each deal
        for (Deal deal : savedDeals) {
            DealAnalysisResponse response = convertToDealAnalysisResponse(deal);
            dealResponses.add(response);
        }

        AnalysisSummary summary = calculateSummary(dealResponses);

        return AnalyzeResponse.builder()
                .files(fileInfos)
                .deals(dealResponses)
                .summary(summary)
                .build();
    }

    private DealAnalysisResponse convertToDealAnalysisResponse(Deal deal) {
        ProbabilityResult probability = probabilityCalculationService.calculateProbability(deal);
        List<NextBestAction> actions = nbaGenerationService.generateActions(deal);

        return DealAnalysisResponse.builder()
                .dealId(deal.getDealId())
                .companyName(deal.getCompanyName())
                .contactInfo(ContactInfo.builder()
                        .name(deal.getContactName())
                        .email(deal.getContactEmail())
                        .title(deal.getContactTitle())
                        .build())
                .dealStage(formatDealStage(deal.getDealStage().name()))
                .dealValue(deal.getDealValue().longValue())
                .currency("KRW")
                .lastContact(deal.getLastContact())
                .nextMeeting(deal.getNextMeeting())
                .probability(probability)
                .painPoints(deal.getPainPoints())
                .competition(deal.getCompetition())
                .decisionMaker(deal.getDecisionMaker())
                .budgetStatus(formatBudgetStatus(deal.getBudgetStatus().name()))
                .nextBestActions(actions)
                .salesRep(deal.getSalesRep())
                .region(deal.getRegion())
                .notes(deal.getNotes())
                .build();
    }

    private String formatDealStage(String stage) {
        // Convert CLOSED_WON -> Closed Won, QUALIFICATION -> Qualification
        return switch (stage) {
            case "CLOSED_WON" -> "Closed Won";
            case "CLOSED_LOST" -> "Closed Lost";
            default -> stage.substring(0, 1) + stage.substring(1).toLowerCase();
        };
    }

    private String formatBudgetStatus(String status) {
        // Convert UNDER_REVIEW -> Under Review, NOT_CONFIRMED -> Not Confirmed
        return switch (status) {
            case "UNDER_REVIEW" -> "Under Review";
            case "NOT_CONFIRMED" -> "Not Confirmed";
            default -> status.substring(0, 1) + status.substring(1).toLowerCase();
        };
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
