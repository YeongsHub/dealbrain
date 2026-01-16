package com.example.sales.service;

import com.example.sales.model.dto.ProbabilityFactors;
import com.example.sales.model.dto.ProbabilityResult;
import com.example.sales.model.entity.Deal;
import com.example.sales.model.enums.BudgetStatus;
import com.example.sales.model.enums.DealStage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class ProbabilityCalculationService {

    private static final int MIN_PROBABILITY = 5;
    private static final int MAX_PROBABILITY = 95;
    private static final Set<String> C_LEVEL_TITLES = Set.of("CEO", "CIO", "CTO", "CFO", "COO", "CMO");

    public ProbabilityResult calculateProbability(Deal deal) {
        // Closed deals have fixed probabilities
        if (deal.getDealStage() == DealStage.CLOSED_WON) {
            return buildClosedWonResult();
        }
        if (deal.getDealStage() == DealStage.CLOSED_LOST) {
            return buildClosedLostResult();
        }

        List<String> positiveFactors = new ArrayList<>();
        List<String> negativeFactors = new ArrayList<>();

        // Base probability from deal stage
        double baseWeight = deal.getDealStage().getBaseWeight();

        // Budget multiplier
        double budgetMultiplier = deal.getBudgetStatus().getMultiplier();
        addBudgetFactors(deal.getBudgetStatus(), positiveFactors, negativeFactors);

        // Recency factor
        double recencyFactor = calculateRecencyFactor(deal.getLastContact());
        addRecencyFactors(deal.getLastContact(), positiveFactors, negativeFactors);

        // Calculate base adjusted probability
        double adjustedProb = baseWeight * budgetMultiplier * recencyFactor;

        // Engagement bonuses
        int engagementBonus = 0;

        // C-level decision maker bonus
        if (isCLevelDecisionMaker(deal.getDecisionMaker())) {
            engagementBonus += 10;
            positiveFactors.add("C-level decision maker (" + deal.getDecisionMaker() + ") engaged");
        }

        // Meeting scheduled bonus
        if (deal.getNextMeeting() != null && deal.getNextMeeting().isAfter(LocalDate.now().minusDays(1))) {
            engagementBonus += 5;
            positiveFactors.add("Meeting scheduled for " + deal.getNextMeeting());
        }

        // POC/Demo mention bonus
        if (containsPocOrDemo(deal.getNotes())) {
            engagementBonus += 5;
            positiveFactors.add("POC/Demo activity identified");
        }

        // Add stage-based factors
        addStageFactors(deal.getDealStage(), positiveFactors, negativeFactors);

        // Add competition factors
        if (deal.getCompetition() != null && !deal.getCompetition().isBlank()) {
            negativeFactors.add("Competition present: " + deal.getCompetition());
        }

        // Calculate final probability
        double finalProb = adjustedProb + engagementBonus;
        int clampedProb = clamp((int) Math.round(finalProb), MIN_PROBABILITY, MAX_PROBABILITY);

        // Determine confidence level
        String confidenceLevel = determineConfidenceLevel(clampedProb, positiveFactors.size(), negativeFactors.size());

        return ProbabilityResult.builder()
                .successRate(clampedProb)
                .confidenceLevel(confidenceLevel)
                .factors(ProbabilityFactors.builder()
                        .positive(positiveFactors)
                        .negative(negativeFactors)
                        .build())
                .build();
    }

    private ProbabilityResult buildClosedWonResult() {
        return ProbabilityResult.builder()
                .successRate(100)
                .confidenceLevel("Certain")
                .factors(ProbabilityFactors.builder()
                        .positive(List.of("Deal closed successfully"))
                        .negative(List.of())
                        .build())
                .build();
    }

    private ProbabilityResult buildClosedLostResult() {
        return ProbabilityResult.builder()
                .successRate(0)
                .confidenceLevel("Certain")
                .factors(ProbabilityFactors.builder()
                        .positive(List.of())
                        .negative(List.of("Deal was lost"))
                        .build())
                .build();
    }

    double calculateRecencyFactor(LocalDate lastContact) {
        if (lastContact == null) {
            return 0.60; // No contact information - worst case
        }

        long daysSinceContact = ChronoUnit.DAYS.between(lastContact, LocalDate.now());

        if (daysSinceContact <= 7) {
            return 1.0;
        } else if (daysSinceContact <= 14) {
            return 0.95;
        } else if (daysSinceContact <= 30) {
            return 0.90;
        } else if (daysSinceContact <= 60) {
            return 0.75;
        } else {
            return 0.60;
        }
    }

    private void addRecencyFactors(LocalDate lastContact, List<String> positive, List<String> negative) {
        if (lastContact == null) {
            negative.add("No recent contact information available");
            return;
        }

        long daysSinceContact = ChronoUnit.DAYS.between(lastContact, LocalDate.now());

        if (daysSinceContact <= 7) {
            positive.add("Recent contact within last week");
        } else if (daysSinceContact <= 14) {
            positive.add("Contact within last two weeks");
        } else if (daysSinceContact <= 30) {
            negative.add("No contact for " + daysSinceContact + " days");
        } else {
            negative.add("Stale deal - no contact for " + daysSinceContact + " days");
        }
    }

    private void addBudgetFactors(BudgetStatus status, List<String> positive, List<String> negative) {
        switch (status) {
            case APPROVED -> positive.add("Budget approved");
            case EXECUTED -> positive.add("Budget executed - strong commitment");
            case UNDER_REVIEW -> positive.add("Budget under review - awaiting approval");
            case NOT_CONFIRMED -> negative.add("Budget not confirmed yet");
            case CANCELLED -> negative.add("Budget cancelled - deal at risk");
        }
    }

    private void addStageFactors(DealStage stage, List<String> positive, List<String> negative) {
        switch (stage) {
            case DISCOVERY -> negative.add("Early Discovery stage - needs qualification");
            case QUALIFICATION -> positive.add("In Qualification - assessing fit");
            case PROPOSAL -> positive.add("Proposal submitted - active engagement");
            case NEGOTIATION -> positive.add("In Negotiation - near closing");
            default -> {} // CLOSED_WON and CLOSED_LOST handled separately
        }
    }

    private boolean isCLevelDecisionMaker(String decisionMaker) {
        if (decisionMaker == null || decisionMaker.isBlank()) {
            return false;
        }
        String normalized = decisionMaker.trim().toUpperCase();
        return C_LEVEL_TITLES.contains(normalized);
    }

    private boolean containsPocOrDemo(String notes) {
        if (notes == null || notes.isBlank()) {
            return false;
        }
        String lowerNotes = notes.toLowerCase();
        return lowerNotes.contains("poc") || lowerNotes.contains("demo") ||
               lowerNotes.contains("proof of concept") || lowerNotes.contains("demonstration");
    }

    private String determineConfidenceLevel(int probability, int positiveCount, int negativeCount) {
        // Confidence is based on both probability and factor balance
        int factorBalance = positiveCount - negativeCount;

        if (probability >= 70 && factorBalance >= 2) {
            return "High";
        } else if (probability <= 30 && factorBalance <= -2) {
            return "Low";
        } else if (probability >= 50 && factorBalance >= 0) {
            return "Medium-High";
        } else if (probability < 50 && factorBalance < 0) {
            return "Medium-Low";
        } else {
            return "Medium";
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
