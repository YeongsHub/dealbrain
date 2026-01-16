package com.example.sales.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DealAnalysisResponse {
    private String dealId;
    private String companyName;
    private ContactInfo contactInfo;
    private String dealStage;
    private Long dealValue;
    private String currency;
    private LocalDate lastContact;
    private LocalDate nextMeeting;
    private ProbabilityResult probability;
    private String painPoints;
    private String competition;
    private String decisionMaker;
    private String budgetStatus;
    private List<NextBestAction> nextBestActions;
    private String salesRep;
    private String region;
    private String notes;
}
