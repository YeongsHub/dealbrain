package com.example.sales.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProbabilityResult {
    private Integer successRate;
    private String confidenceLevel;
    private ProbabilityFactors factors;
}
