package com.example.sales.service;

import com.example.sales.exception.CsvParsingException;
import com.example.sales.exception.CsvValidationException;
import com.example.sales.exception.CsvValidationException.RowError;
import com.example.sales.model.dto.CsvDealRow;
import com.example.sales.model.entity.Deal;
import com.example.sales.model.entity.User;
import com.example.sales.model.enums.BudgetStatus;
import com.example.sales.model.enums.DealStage;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@Slf4j
public class CsvParsingService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public List<Deal> parseCsvFile(MultipartFile file, User user) {
        List<CsvDealRow> rows = parseRows(file);
        return mapAndValidateRows(rows, user);
    }

    private List<CsvDealRow> parseRows(MultipartFile file) {
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            CsvToBean<CsvDealRow> csvToBean = new CsvToBeanBuilder<CsvDealRow>(reader)
                    .withType(CsvDealRow.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .withIgnoreEmptyLine(true)
                    .build();

            return csvToBean.parse();
        } catch (Exception e) {
            log.error("Failed to parse CSV file: {}", file.getOriginalFilename(), e);
            throw new CsvParsingException("Failed to parse CSV file: " + e.getMessage(), e);
        }
    }

    private List<Deal> mapAndValidateRows(List<CsvDealRow> rows, User user) {
        List<Deal> deals = new ArrayList<>();
        List<RowError> errors = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            int rowNumber = i + 2; // +2 because row 1 is header, and index starts at 0
            CsvDealRow row = rows.get(i);

            Map<String, String> fieldErrors = validateRow(row);
            if (!fieldErrors.isEmpty()) {
                errors.add(new RowError(rowNumber, fieldErrors));
                continue;
            }

            try {
                Deal deal = mapRowToDeal(row, user);
                deals.add(deal);
            } catch (Exception e) {
                log.error("Failed to map row {} to Deal", rowNumber, e);
                errors.add(new RowError(rowNumber, Map.of("mapping", e.getMessage())));
            }
        }

        if (!errors.isEmpty()) {
            throw new CsvValidationException(errors);
        }

        return deals;
    }

    private Map<String, String> validateRow(CsvDealRow row) {
        Map<String, String> errors = new LinkedHashMap<>();

        // Required fields validation
        if (isBlank(row.getDealId())) {
            errors.put("Deal_ID", "Deal ID is required");
        }
        if (isBlank(row.getCompanyName())) {
            errors.put("Company_Name", "Company Name is required");
        }
        if (isBlank(row.getContactName())) {
            errors.put("Contact_Name", "Contact Name is required");
        }
        if (isBlank(row.getContactEmail())) {
            errors.put("Contact_Email", "Contact Email is required");
        }
        if (isBlank(row.getDealStage())) {
            errors.put("Deal_Stage", "Deal Stage is required");
        } else if (!isValidDealStage(row.getDealStage())) {
            errors.put("Deal_Stage", "Invalid Deal Stage: " + row.getDealStage() +
                    ". Valid values: " + Arrays.toString(DealStage.values()));
        }
        if (isBlank(row.getDealValue())) {
            errors.put("Deal_Value", "Deal Value is required");
        } else if (!isValidNumber(row.getDealValue())) {
            errors.put("Deal_Value", "Deal Value must be a valid number");
        }
        if (isBlank(row.getBudgetStatus())) {
            errors.put("Budget_Status", "Budget Status is required");
        } else if (!isValidBudgetStatus(row.getBudgetStatus())) {
            errors.put("Budget_Status", "Invalid Budget Status: " + row.getBudgetStatus() +
                    ". Valid values: " + Arrays.toString(BudgetStatus.values()));
        }

        // Date validation for optional fields
        if (!isBlank(row.getLastContact()) && !isValidDate(row.getLastContact())) {
            errors.put("Last_Contact", "Invalid date format. Expected: yyyy-MM-dd");
        }
        if (!isBlank(row.getNextMeeting()) && !isValidDate(row.getNextMeeting())) {
            errors.put("Next_Meeting", "Invalid date format. Expected: yyyy-MM-dd");
        }

        return errors;
    }

    private Deal mapRowToDeal(CsvDealRow row, User user) {
        return Deal.builder()
                .dealId(row.getDealId().trim())
                .companyName(row.getCompanyName().trim())
                .contactName(row.getContactName().trim())
                .contactEmail(row.getContactEmail().trim())
                .contactTitle(trimOrNull(row.getContactTitle()))
                .dealStage(parseDealStage(row.getDealStage()))
                .dealValue(new BigDecimal(row.getDealValue().trim().replaceAll("[^\\d.]", "")))
                .productInterest(trimOrNull(row.getProductInterest()))
                .painPoints(trimOrNull(row.getPainPoints()))
                .competition(trimOrNull(row.getCompetition()))
                .decisionMaker(trimOrNull(row.getDecisionMaker()))
                .budgetStatus(parseBudgetStatus(row.getBudgetStatus()))
                .salesRep(trimOrNull(row.getSalesRep()))
                .region(trimOrNull(row.getRegion()))
                .lastContact(parseDate(row.getLastContact()))
                .nextMeeting(parseDate(row.getNextMeeting()))
                .notes(trimOrNull(row.getNotes()))
                .user(user)
                .build();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trimOrNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isValidDealStage(String value) {
        return parseDealStageOrNull(value) != null;
    }

    private boolean isValidBudgetStatus(String value) {
        return parseBudgetStatusOrNull(value) != null;
    }

    private boolean isValidNumber(String value) {
        try {
            new BigDecimal(value.trim().replaceAll("[^\\d.]", ""));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidDate(String value) {
        try {
            LocalDate.parse(value.trim(), DATE_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private DealStage parseDealStage(String value) {
        DealStage stage = parseDealStageOrNull(value);
        if (stage == null) {
            throw new IllegalArgumentException("Invalid deal stage: " + value);
        }
        return stage;
    }

    private DealStage parseDealStageOrNull(String value) {
        if (isBlank(value)) {
            return null;
        }
        String normalized = value.trim().toUpperCase().replace(" ", "_");
        try {
            return DealStage.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // Try fuzzy matching for common variations
            return switch (normalized) {
                case "CLOSED-WON", "CLOSEDWON", "WON" -> DealStage.CLOSED_WON;
                case "CLOSED-LOST", "CLOSEDLOST", "LOST" -> DealStage.CLOSED_LOST;
                default -> null;
            };
        }
    }

    private BudgetStatus parseBudgetStatus(String value) {
        BudgetStatus status = parseBudgetStatusOrNull(value);
        if (status == null) {
            throw new IllegalArgumentException("Invalid budget status: " + value);
        }
        return status;
    }

    private BudgetStatus parseBudgetStatusOrNull(String value) {
        if (isBlank(value)) {
            return null;
        }
        String normalized = value.trim().toUpperCase().replace(" ", "_");
        try {
            return BudgetStatus.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // Try fuzzy matching for common variations
            return switch (normalized) {
                case "UNDER-REVIEW", "UNDERREVIEW", "PENDING", "REVIEW" -> BudgetStatus.UNDER_REVIEW;
                case "NOT-CONFIRMED", "NOTCONFIRMED", "UNCONFIRMED" -> BudgetStatus.NOT_CONFIRMED;
                default -> null;
            };
        }
    }

    private LocalDate parseDate(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
