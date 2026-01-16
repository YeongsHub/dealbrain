package com.example.sales.service;

import com.example.sales.model.dto.NextBestAction;
import com.example.sales.model.entity.Deal;
import com.example.sales.model.entity.User;
import com.example.sales.model.enums.BudgetStatus;
import com.example.sales.model.enums.DealStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NbaGenerationServiceTest {

    private NbaGenerationService service;
    private User testUser;

    @BeforeEach
    void setUp() {
        service = new NbaGenerationService();
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .build();
    }

    @Nested
    @DisplayName("Closed Deals")
    class ClosedDeals {

        @Test
        @DisplayName("Closed Won should return no actions")
        void closedWon_NoActions() {
            Deal deal = createBaseDeal(DealStage.CLOSED_WON, BudgetStatus.APPROVED);

            List<NextBestAction> actions = service.generateActions(deal);

            assertThat(actions).isEmpty();
        }

        @Test
        @DisplayName("Closed Lost should return no actions")
        void closedLost_NoActions() {
            Deal deal = createBaseDeal(DealStage.CLOSED_LOST, BudgetStatus.APPROVED);

            List<NextBestAction> actions = service.generateActions(deal);

            assertThat(actions).isEmpty();
        }
    }

    @Nested
    @DisplayName("High Priority Actions")
    class HighPriorityActions {

        @Test
        @DisplayName("No meeting + stale contact should trigger immediate meeting action")
        void noMeetingStaleContact_ImmediateMeetingAction() {
            Deal deal = createBaseDeal(DealStage.QUALIFICATION, BudgetStatus.APPROVED);
            deal.setLastContact(LocalDate.now().minusDays(20));
            deal.setNextMeeting(null);

            List<NextBestAction> actions = service.generateActions(deal);

            assertThat(actions).isNotEmpty();
            assertThat(actions.get(0).getPriority()).isEqualTo(1);
            assertThat(actions.get(0).getAction()).containsIgnoringCase("meeting");
        }

        @Test
        @DisplayName("Negotiation with unconfirmed budget should trigger budget approval action")
        void negotiationUnconfirmedBudget_BudgetApprovalAction() {
            Deal deal = createBaseDeal(DealStage.NEGOTIATION, BudgetStatus.NOT_CONFIRMED);

            List<NextBestAction> actions = service.generateActions(deal);

            assertThat(actions).isNotEmpty();
            boolean hasBudgetAction = actions.stream()
                    .filter(a -> a.getPriority() == 1)
                    .anyMatch(a -> a.getAction().toLowerCase().contains("budget"));
            assertThat(hasBudgetAction).isTrue();
        }

        @Test
        @DisplayName("Cancelled budget should trigger investigation action")
        void cancelledBudget_InvestigationAction() {
            Deal deal = createBaseDeal(DealStage.PROPOSAL, BudgetStatus.CANCELLED);

            List<NextBestAction> actions = service.generateActions(deal);

            assertThat(actions).isNotEmpty();
            boolean hasInvestigateAction = actions.stream()
                    .filter(a -> a.getPriority() == 1)
                    .anyMatch(a -> a.getAction().toLowerCase().contains("investigate") ||
                            a.getAction().toLowerCase().contains("cancellation"));
            assertThat(hasInvestigateAction).isTrue();
        }
    }

    @Nested
    @DisplayName("Medium Priority Actions")
    class MediumPriorityActions {

        @Test
        @DisplayName("POC mention should trigger POC execution action")
        void pocMention_PocExecutionAction() {
            Deal deal = createBaseDeal(DealStage.QUALIFICATION, BudgetStatus.APPROVED);
            deal.setNotes("Customer requested POC");

            List<NextBestAction> actions = service.generateActions(deal);

            boolean hasPocAction = actions.stream()
                    .filter(a -> a.getPriority() == 2)
                    .anyMatch(a -> a.getAction().toLowerCase().contains("poc"));
            assertThat(hasPocAction).isTrue();
        }

        @Test
        @DisplayName("Demo mention should trigger demo scheduling action")
        void demoMention_DemoSchedulingAction() {
            Deal deal = createBaseDeal(DealStage.QUALIFICATION, BudgetStatus.APPROVED);
            deal.setNotes("Demo requested by customer");

            List<NextBestAction> actions = service.generateActions(deal);

            boolean hasDemoAction = actions.stream()
                    .filter(a -> a.getPriority() == 2)
                    .anyMatch(a -> a.getAction().toLowerCase().contains("demo"));
            assertThat(hasDemoAction).isTrue();
        }

        @Test
        @DisplayName("Budget under review in late stages should trigger follow-up")
        void budgetUnderReview_FollowUpAction() {
            Deal deal = createBaseDeal(DealStage.PROPOSAL, BudgetStatus.UNDER_REVIEW);

            List<NextBestAction> actions = service.generateActions(deal);

            boolean hasBudgetFollowUp = actions.stream()
                    .filter(a -> a.getPriority() == 2)
                    .anyMatch(a -> a.getAction().toLowerCase().contains("budget"));
            assertThat(hasBudgetFollowUp).isTrue();
        }
    }

    @Nested
    @DisplayName("Standard Stage-Based Actions")
    class StandardActions {

        @Test
        @DisplayName("Discovery stage should suggest requirements gathering")
        void discoveryStage_RequirementsGatheringAction() {
            Deal deal = createBaseDeal(DealStage.DISCOVERY, BudgetStatus.APPROVED);

            List<NextBestAction> actions = service.generateActions(deal);

            boolean hasRequirementsAction = actions.stream()
                    .anyMatch(a -> a.getAction().toLowerCase().contains("requirements"));
            assertThat(hasRequirementsAction).isTrue();
        }

        @Test
        @DisplayName("Discovery with no decision maker should suggest identification")
        void discoveryNoDecisionMaker_IdentifyDecisionMakerAction() {
            Deal deal = createBaseDeal(DealStage.DISCOVERY, BudgetStatus.APPROVED);
            deal.setDecisionMaker(null);

            List<NextBestAction> actions = service.generateActions(deal);

            boolean hasDecisionMakerAction = actions.stream()
                    .anyMatch(a -> a.getAction().toLowerCase().contains("decision maker"));
            assertThat(hasDecisionMakerAction).isTrue();
        }

        @Test
        @DisplayName("Qualification stage should suggest budget/timeline confirmation")
        void qualificationStage_BudgetTimelineConfirmation() {
            Deal deal = createBaseDeal(DealStage.QUALIFICATION, BudgetStatus.APPROVED);

            List<NextBestAction> actions = service.generateActions(deal);

            boolean hasConfirmAction = actions.stream()
                    .anyMatch(a -> a.getAction().toLowerCase().contains("confirm") ||
                            a.getAction().toLowerCase().contains("budget"));
            assertThat(hasConfirmAction).isTrue();
        }

        @Test
        @DisplayName("Proposal stage should suggest proposal follow-up")
        void proposalStage_ProposalFollowUp() {
            Deal deal = createBaseDeal(DealStage.PROPOSAL, BudgetStatus.APPROVED);
            deal.setLastContact(LocalDate.now().minusDays(8));

            List<NextBestAction> actions = service.generateActions(deal);

            boolean hasFollowUpAction = actions.stream()
                    .anyMatch(a -> a.getAction().toLowerCase().contains("proposal") ||
                            a.getAction().toLowerCase().contains("follow up"));
            assertThat(hasFollowUpAction).isTrue();
        }

        @Test
        @DisplayName("Negotiation stage should suggest contract preparation")
        void negotiationStage_ContractPreparation() {
            Deal deal = createBaseDeal(DealStage.NEGOTIATION, BudgetStatus.APPROVED);

            List<NextBestAction> actions = service.generateActions(deal);

            boolean hasContractAction = actions.stream()
                    .anyMatch(a -> a.getAction().toLowerCase().contains("contract"));
            assertThat(hasContractAction).isTrue();
        }
    }

    @Nested
    @DisplayName("Action Limits and Ordering")
    class ActionLimitsAndOrdering {

        @Test
        @DisplayName("Should return maximum 3 actions")
        void maximumThreeActions() {
            Deal deal = createBaseDeal(DealStage.DISCOVERY, BudgetStatus.NOT_CONFIRMED);
            deal.setLastContact(LocalDate.now().minusDays(30));
            deal.setNextMeeting(null);
            deal.setNotes("POC and demo requested");
            deal.setDecisionMaker(null);

            List<NextBestAction> actions = service.generateActions(deal);

            assertThat(actions).hasSizeLessThanOrEqualTo(3);
        }

        @Test
        @DisplayName("Actions should be sorted by priority")
        void actionsSortedByPriority() {
            Deal deal = createBaseDeal(DealStage.QUALIFICATION, BudgetStatus.APPROVED);
            deal.setLastContact(LocalDate.now().minusDays(20));
            deal.setNextMeeting(null);
            deal.setNotes("POC requested");

            List<NextBestAction> actions = service.generateActions(deal);

            for (int i = 0; i < actions.size() - 1; i++) {
                assertThat(actions.get(i).getPriority())
                        .isLessThanOrEqualTo(actions.get(i + 1).getPriority());
            }
        }

        @Test
        @DisplayName("High priority actions should come before standard actions")
        void highPriorityFirst() {
            Deal deal = createBaseDeal(DealStage.QUALIFICATION, BudgetStatus.APPROVED);
            deal.setLastContact(LocalDate.now().minusDays(20));
            deal.setNextMeeting(null);

            List<NextBestAction> actions = service.generateActions(deal);

            assertThat(actions).isNotEmpty();
            assertThat(actions.get(0).getPriority()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Action Metadata")
    class ActionMetadata {

        @Test
        @DisplayName("All actions should have deadline")
        void actionsHaveDeadline() {
            Deal deal = createBaseDeal(DealStage.QUALIFICATION, BudgetStatus.APPROVED);

            List<NextBestAction> actions = service.generateActions(deal);

            assertThat(actions).allMatch(a -> a.getDeadline() != null);
        }

        @Test
        @DisplayName("All actions should have rationale")
        void actionsHaveRationale() {
            Deal deal = createBaseDeal(DealStage.QUALIFICATION, BudgetStatus.APPROVED);

            List<NextBestAction> actions = service.generateActions(deal);

            assertThat(actions).allMatch(a -> a.getRationale() != null && !a.getRationale().isBlank());
        }

        @Test
        @DisplayName("All actions should have action description")
        void actionsHaveDescription() {
            Deal deal = createBaseDeal(DealStage.QUALIFICATION, BudgetStatus.APPROVED);

            List<NextBestAction> actions = service.generateActions(deal);

            assertThat(actions).allMatch(a -> a.getAction() != null && !a.getAction().isBlank());
        }

        @Test
        @DisplayName("Deadlines should be in the future")
        void deadlinesInFuture() {
            Deal deal = createBaseDeal(DealStage.QUALIFICATION, BudgetStatus.APPROVED);

            List<NextBestAction> actions = service.generateActions(deal);

            LocalDate today = LocalDate.now();
            assertThat(actions).allMatch(a -> !a.getDeadline().isBefore(today));
        }
    }

    private Deal createBaseDeal(DealStage stage, BudgetStatus budgetStatus) {
        return Deal.builder()
                .id(1L)
                .dealId("DEAL-001")
                .companyName("Test Company")
                .contactName("John Doe")
                .contactEmail("john@test.com")
                .contactTitle("Manager")
                .dealStage(stage)
                .dealValue(BigDecimal.valueOf(100000))
                .budgetStatus(budgetStatus)
                .lastContact(LocalDate.now().minusDays(5))
                .decisionMaker("Director")
                .user(testUser)
                .build();
    }
}
