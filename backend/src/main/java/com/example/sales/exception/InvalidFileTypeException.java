package com.example.sales.exception;

public class InvalidFileTypeException extends RuntimeException {

    public InvalidFileTypeException(String fileName, String detectedType) {
        super(String.format("Invalid file type for '%s'. Detected: %s. Allowed: CSV, PDF", fileName, detectedType));
    }
}
