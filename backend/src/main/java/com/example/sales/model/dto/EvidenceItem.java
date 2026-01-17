package com.example.sales.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvidenceItem {
    private String source;
    private String excerpt;
    private Integer page;
    private Double relevanceScore;
}
