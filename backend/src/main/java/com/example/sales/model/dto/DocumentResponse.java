package com.example.sales.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {
    private Long id;
    private String fileName;
    private String documentType;
    private String processingStatus;
    private Integer totalPages;
    private Integer totalChunks;
    private Long fileSize;
    private LocalDateTime createdAt;
    private String dealId;
}
