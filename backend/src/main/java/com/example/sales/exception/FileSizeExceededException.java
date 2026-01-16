package com.example.sales.exception;

public class FileSizeExceededException extends RuntimeException {

    public FileSizeExceededException(String fileName, long fileSize, long maxSize) {
        super(String.format("File '%s' size (%d bytes) exceeds maximum allowed size (%d bytes)",
                fileName, fileSize, maxSize));
    }
}
