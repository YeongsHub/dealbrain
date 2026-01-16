package com.example.sales.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        return buildErrorResponse("EMAIL_ALREADY_EXISTS", ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return buildErrorResponse("INVALID_CREDENTIALS", ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(InvalidFileTypeException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidFileType(InvalidFileTypeException ex) {
        return buildErrorResponse("INVALID_FILE_TYPE", ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(FileSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleFileSizeExceeded(FileSizeExceededException ex) {
        return buildErrorResponse("FILE_SIZE_EXCEEDED", ex.getMessage(), HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @ExceptionHandler(CsvParsingException.class)
    public ResponseEntity<Map<String, Object>> handleCsvParsingException(CsvParsingException ex) {
        return buildErrorResponse("CSV_PARSING_ERROR", ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(CsvValidationException.class)
    public ResponseEntity<Map<String, Object>> handleCsvValidationException(CsvValidationException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("code", "CSV_VALIDATION_ERROR");
        error.put("message", ex.getMessage());
        error.put("rowNumber", ex.getRowNumber());
        error.put("fieldErrors", ex.getFieldErrors());
        error.put("timestamp", Instant.now().toString());

        Map<String, Object> response = new HashMap<>();
        response.put("error", error);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        Map<String, Object> error = new HashMap<>();
        error.put("code", "VALIDATION_ERROR");
        error.put("message", "Validation failed");
        error.put("details", fieldErrors);
        error.put("timestamp", Instant.now().toString());

        Map<String, Object> response = new HashMap<>();
        response.put("error", error);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        return buildErrorResponse("INTERNAL_ERROR", "An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(String code, String message, HttpStatus status) {
        Map<String, Object> error = new HashMap<>();
        error.put("code", code);
        error.put("message", message);
        error.put("timestamp", Instant.now().toString());

        Map<String, Object> response = new HashMap<>();
        response.put("error", error);

        return ResponseEntity.status(status).body(response);
    }
}
