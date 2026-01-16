package com.example.sales.service;

import com.example.sales.model.dto.NextBestAction;
import com.example.sales.model.entity.Deal;
import com.example.sales.model.enums.BudgetStatus;
import com.example.sales.model.enums.DealStage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
public class NbaGenerationService {

    private static final int MAX_ACTIONS = 3;

    public List<NextBestAction> generateActions(Deal deal) {
        // Closed deals don't need actions
        if (deal.getDealStage() == DealStage.CLOSED_WON ||
            deal.getDealStage() == DealStage.CLOSED_LOST) {
            return List.of();
        }

        List<NextBestAction> actions = new ArrayList<>();

        // Generate high priority actions first
        addHighPriorityActions(deal, actions);

        // Then medium priority actions
        addMediumPriorityActions(deal, actions);

        // Finally standard stage-based actions
        addStandardActions(deal, actions);

        // Sort by priority and limit to max actions
        return actions.stream()
                .sorted(Comparator.comparing(NextBestAction::getPriority))
                .limit(MAX_ACTIONS)
                .toList();
    }

    private void addHighPriorityActions(Deal deal, List<NextBestAction> actions) {
        LocalDate today = LocalDate.now();

        // High Priority 1: No meeting scheduled AND last contact > 14 days
        boolean noMeeting = deal.getNextMeeting() == null || deal.getNextMeeting().isBefore(today);
        long daysSinceContact = deal.getLastContact() != null ?
                ChronoUnit.DAYS.between(deal.getLastContact(), today) : Long.MAX_VALUE;

        if (noMeeting && daysSinceContact > 14) {
            actions.add(NextBestAction.builder()
                    .priority(1)
                    .action("Schedule meeting immediately")
                    .rationale("No meeting scheduled and " + daysSinceContact + " days since last contact - deal going cold")
                    .deadline(today.plusDays(3))
                    .build());
        }

        // High Priority 2: Negotiation stage but budget not confirmed
        if (deal.getDealStage() == DealStage.NEGOTIATION &&
            deal.getBudgetStatus() == BudgetStatus.NOT_CONFIRMED) {
            actions.add(NextBestAction.builder()
                    .priority(1)
                    .action("Secure budget approval urgently")
                    .rationale("In Negotiation but budget not confirmed - closing at risk")
                    .deadline(today.plusDays(5))
                    .build());
        }

        // High Priority 3: Budget cancelled
        if (deal.getBudgetStatus() == BudgetStatus.CANCELLED) {
            actions.add(NextBestAction.builder()
                    .priority(1)
                    .action("Investigate budget cancellation")
                    .rationale("Budget cancelled - understand reasons and identify recovery path")
                    .deadline(today.plusDays(2))
                    .build());
        }
    }

    private void addMediumPriorityActions(Deal deal, List<NextBestAction> actions) {
        LocalDate today = LocalDate.now();
        String notes = deal.getNotes() != null ? deal.getNotes().toLowerCase() : "";
        String painPoints = deal.getPainPoints() != null ? deal.getPainPoints().toLowerCase() : "";

        // Medium Priority: Competition mentioned in pain points or notes
        if (deal.getCompetition() != null && !deal.getCompetition().isBlank()) {
            String competition = deal.getCompetition();
            if (painPoints.contains(competition.toLowerCase()) || notes.contains("competitive")) {
                actions.add(NextBestAction.builder()
                        .priority(2)
                        .action("Prepare competitive differentiation vs " + competition)
                        .rationale("Competition actively referenced - need to address proactively")
                        .deadline(today.plusDays(7))
                        .build());
            }
        }

        // Medium Priority: POC mentioned in notes
        if (notes.contains("poc") || notes.contains("proof of concept")) {
            actions.add(NextBestAction.builder()
                    .priority(2)
                    .action("Execute POC within 7 days")
                    .rationale("POC requested/mentioned - demonstrate value quickly")
                    .deadline(today.plusDays(7))
                    .build());
        }

        // Medium Priority: Demo mentioned in notes
        if (notes.contains("demo") || notes.contains("demonstration")) {
            if (!notes.contains("completed demo") && !notes.contains("demo completed")) {
                actions.add(NextBestAction.builder()
                        .priority(2)
                        .action("Schedule or follow up on demo")
                        .rationale("Demo mentioned - ensure it gets scheduled/completed")
                        .deadline(today.plusDays(5))
                        .build());
            }
        }

        // Medium Priority: Budget under review for Proposal/Negotiation stages
        if ((deal.getDealStage() == DealStage.PROPOSAL || deal.getDealStage() == DealStage.NEGOTIATION) &&
            deal.getBudgetStatus() == BudgetStatus.UNDER_REVIEW) {
            actions.add(NextBestAction.builder()
                    .priority(2)
                    .action("Follow up on budget approval status")
                    .rationale("Budget under review - track progress to avoid delays")
                    .deadline(today.plusDays(5))
                    .build());
        }
    }

    private void addStandardActions(Deal deal, List<NextBestAction> actions) {
        LocalDate today = LocalDate.now();

        // Standard stage-based actions
        switch (deal.getDealStage()) {
            case DISCOVERY -> {
                actions.add(NextBestAction.builder()
                        .priority(3)
                        .action("Gather detailed requirements")
                        .rationale("Discovery stage - need to understand customer needs thoroughly")
                        .deadline(today.plusDays(10))
                        .build());
                if (deal.getDecisionMaker() == null || deal.getDecisionMaker().isBlank()) {
                    actions.add(NextBestAction.builder()
                            .priority(3)
                            .action("Identify key decision makers")
                            .rationale("No decision maker identified - critical for deal progression")
                            .deadline(today.plusDays(7))
                            .build());
                }
            }
            case QUALIFICATION -> {
                actions.add(NextBestAction.builder()
                        .priority(3)
                        .action("Confirm budget and timeline")
                        .rationale("Qualification stage - validate customer commitment")
                        .deadline(today.plusDays(10))
                        .build());
            }
            case PROPOSAL -> {
                long daysSinceContact = deal.getLastContact() != null ?
                        ChronoUnit.DAYS.between(deal.getLastContact(), today) : 0;
                if (daysSinceContact >= 7) {
                    actions.add(NextBestAction.builder()
                            .priority(3)
                            .action("Follow up on proposal status")
                            .rationale("Proposal submitted " + daysSinceContact + " days ago - check for feedback")
                            .deadline(today.plusDays(3))
                            .build());
                } else {
                    actions.add(NextBestAction.builder()
                            .priority(3)
                            .action("Prepare for proposal questions")
                            .rationale("Proposal recently submitted - anticipate follow-up queries")
                            .deadline(today.plusDays(5))
                            .build());
                }
            }
            case NEGOTIATION -> {
                actions.add(NextBestAction.builder()
                        .priority(3)
                        .action("Prepare contract terms")
                        .rationale("Negotiation stage - ready final documentation")
                        .deadline(today.plusDays(7))
                        .build());
            }
            default -> {
                // CLOSED_WON and CLOSED_LOST handled at the beginning
            }
        }
    }
}
