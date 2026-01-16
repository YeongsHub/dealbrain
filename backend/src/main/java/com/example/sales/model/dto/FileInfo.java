package com.example.sales.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileInfo {
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String status;

    public enum Status {
        PROCESSED, FAILED, SKIPPED
    }
}
