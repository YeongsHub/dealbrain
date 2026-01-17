package com.example.sales.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatQueryResponse {
    private String query;
    private String answer;
    private List<EvidenceItem> evidence;
    private String confidence;
}
