package com.example.sales.exception;

import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class CsvValidationException extends RuntimeException {

    private final int rowNumber;
    private final Map<String, String> fieldErrors;

    public CsvValidationException(int rowNumber, Map<String, String> fieldErrors) {
        super(buildMessage(rowNumber, fieldErrors));
        this.rowNumber = rowNumber;
        this.fieldErrors = fieldErrors;
    }

    public CsvValidationException(List<RowError> rowErrors) {
        super(buildMessage(rowErrors));
        this.rowNumber = rowErrors.isEmpty() ? 0 : rowErrors.get(0).rowNumber();
        this.fieldErrors = rowErrors.isEmpty() ? Map.of() : rowErrors.get(0).fieldErrors();
    }

    private static String buildMessage(int rowNumber, Map<String, String> fieldErrors) {
        return String.format("Validation failed at row %d: %s", rowNumber, fieldErrors);
    }

    private static String buildMessage(List<RowError> rowErrors) {
        if (rowErrors.isEmpty()) {
            return "Validation failed";
        }
        StringBuilder sb = new StringBuilder("CSV validation failed with errors: ");
        for (RowError error : rowErrors) {
            sb.append(String.format("[Row %d: %s] ", error.rowNumber(), error.fieldErrors()));
        }
        return sb.toString().trim();
    }

    public record RowError(int rowNumber, Map<String, String> fieldErrors) {}
}
