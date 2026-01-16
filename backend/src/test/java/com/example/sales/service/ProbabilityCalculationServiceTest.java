package com.example.sales.service;

import com.example.sales.model.dto.ProbabilityResult;
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

import static org.assertj.core.api.Assertions.assertThat;

class ProbabilityCalculationServiceTest {

    private ProbabilityCalculationService service;
    private User testUser;

    @BeforeEach
    void setUp() {
        service = new ProbabilityCalculationService();
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .build();
    }

    @Nested
    @DisplayName("Base Stage Weights")
    class BaseStageWeights {

        @Test
        @DisplayName("Discovery stage should have low probability")
        void discoveryStage_LowProbability() {
            Deal deal = createBaseDeal(DealStage.DISCOVERY, BudgetStatus.APPROVED);

            ProbabilityResult result = service.calculateProbability(deal);

            // Base 15 * 1.2 (approved) * 1.0 (recent contact) = 18
            assertThat(result.getSuccessRate()).isBetween(15, 30);
        }

        @Test
        @DisplayName("Qualification stage should have medium-low probability")
        void qualificationStage_MediumLowProbability() {
            Deal deal = createBaseDeal(DealStage.QUALIFICATION, BudgetStatus.APPROVED);

            ProbabilityResult result = service.calculateProbability(deal);

            // Base 35 * 1.2 (approved) * 1.0 = 42
            assertThat(result.getSuccessRate()).isBetween(35, 55);
        }

        @Test
        @DisplayName("Proposal stage should have medium probability")
        void proposalStage_MediumProbability() {
            Deal deal = createBaseDeal(DealStage.PROPOSAL, BudgetStatus.APPROVED);

            ProbabilityResult result = service.calculateProbability(deal);

            // Base 55 * 1.2 = 66
            assertThat(result.getSuccessRate()).isBetween(55, 80);
        }

        @Test
        @DisplayName("Negotiation stage should have high probability")
        void negotiationStage_HighProbability() {
            Deal deal = createBaseDeal(DealStage.NEGOTIATION, BudgetStatus.APPROVED);

            ProbabilityResult result = service.calculateProbability(deal);

            // Base 75 * 1.2 = 90
            assertThat(result.getSuccessRate()).isBetween(75, 95);
        }

        @Test
        @DisplayName("Closed Won should have 100% probability")
        void closedWon_FullProbability() {
            Deal deal = createBaseDeal(DealStage.CLOSED_WON, BudgetStatus.APPROVED);

            ProbabilityResult result = service.calculateProbability(deal);

            assertThat(result.getSuccessRate()).isEqualTo(100);
            assertThat(result.getConfidenceLevel()).isEqualTo("Certain");
        }

        @Test
        @DisplayName("Closed Lost should have 0% probability")
        void closedLost_ZeroProbability() {
            Deal deal = createBaseDeal(DealStage.CLOSED_LOST, BudgetStatus.APPROVED);

            ProbabilityResult result = service.calculateProbability(deal);

            assertThat(result.getSuccessRate()).isEqualTo(0);
            assertThat(result.getConfidenceLevel()).isEqualTo("Certain");
        }
    }

    @Nested
    @DisplayName("Budget Multiplier")
    class BudgetMultiplier {

        @Test
        @DisplayName("Approved budget should increase probability (1.2x)")
        void approvedBudget_IncreasesProbability() {
            Deal approvedDeal = createBaseDeal(DealStage.QUALIFICATION, BudgetStatus.APPROVED);
            Deal notConfirmedDeal = createBaseDeal(DealStage.QUALIFICATION, BudgetStatus.NOT_CONFIRMED);

            ProbabilityResult approvedResult = service.calculateProbability(approvedDeal);
            ProbabilityResult notConfirmedResult = service.calculateProbability(notConfirmedDeal);

            assertThat(approvedResult.getSuccessRate()).isGreaterThan(notConfirmedResult.getSuccessRate());
        }

        @Test
        @DisplayName("Executed budget should have highest multiplier (1.3x)")
        void executedBudget_HighestMultiplier() {
            Deal executedDeal = createBaseDeal(DealStage.QUALIFICATION, BudgetStatus.EXECUTED);
            Deal approvedDeal = createBaseDeal(DealStage.QUALIFICATION, BudgetStatus.APPROVED);

            ProbabilityResult executedResult = service.calculateProbability(executedDeal);
            ProbabilityResult approvedResult = service.calculateProbability(approvedDeal);

            assertThat(executedResult.getSuccessRate()).isGreaterThan(approvedResult.getSuccessRate());
        }

        @Test
        @DisplayName("Cancelled budget should drop probability significantly (0x)")
        void cancelledBudget_DropsProbability() {
            Deal deal = createBaseDeal(DealStage.QUALIFICATION, BudgetStatus.CANCELLED);

            ProbabilityResult result = service.calculateProbability(deal);

            // Base 35 * 0.0 = 0, but clamped to minimum 5
            assertThat(result.getSuccessRate()).isEqualTo(5);
        }

        @Test
        @DisplayName("Not confirmed budget should reduce probability (0.7x)")
        void notConfirmedBudget_ReducesProbability() {
            Deal notConfirmedDeal = createBaseDeal(DealStage.QUALIFICATION, BudgetStatus.NOT_CONFIRMED);
            Deal underReviewDeal = createBaseDeal(DealStage.QUALIFICATION, BudgetStatus.UNDER_REVIEW);

            ProbabilityResult notConfirmedResult = service.calculateProbability(notConfirmedDeal);
            ProbabilityResult underReviewResult = service.calculateProbability(underReviewDeal);

            assertThat(notConfirmedResult.getSuccessRate()).isLessThan(underReviewResult.getSuccessRate());
        }
    }

    @Nested
    @DisplayName("Recency Factor")
    class RecencyFactor {

        @Test
        @DisplayName("Contact within 7 days should have full recency (1.0)")
        void recentContact_FullRecency() {
            double factor = service.calculateRecencyFactor(LocalDate.now().minusDays(3));

            assertThat(factor).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Contact 8-14 days ago should have 0.95 recency")
        void twoWeeksContact_SlightReduction() {
            double factor = service.calculateRecencyFactor(LocalDate.now().minusDays(10));

            assertThat(factor).isEqualTo(0.95);
        }

        @Test
        @DisplayName("Contact 15-30 days ago should have 0.90 recency")
        void monthOldContact_ModerateReduction() {
            double factor = service.calculateRecencyFactor(LocalDate.now().minusDays(20));

            assertThat(factor).isEqualTo(0.90);
        }

        @Test
        @DisplayName("Contact 31-60 days ago should have 0.75 recency")
        void staleContact_SignificantReduction() {
            double factor = service.calculateRecencyFactor(LocalDate.now().minusDays(45));

            assertThat(factor).isEqualTo(0.75);
        }

        @Test
        @DisplayName("Contact over 60 days ago should have 0.60 recency")
        void veryStaleContact_LowestRecency() {
            double factor = service.calculateRecencyFactor(LocalDate.now().minusDays(90));

            assertThat(factor).isEqualTo(0.60);
        }

        @Test
        @DisplayName("Null contact date should use worst case (0.60)")
        void nullContact_WorstCase() {
            double factor = service.calculateRecencyFactor(null);

            assertThat(factor).isEqualTo(0.60);
        }
    }

    @Nested
    @DisplayName("Engagement Bonus")
    class EngagementBonus {

        @Test
        @DisplayName("C-level decision maker should add +10 bonus")
        void cLevelDecisionMaker_AddsBonus() {
            Deal withCLevel = createBaseDeal(DealStage.QUALIFICATION, BudgetStatus.UNDER_REVIEW);
            withCLevel.setDecisionMaker("CTO");

            Deal withoutCLevel = createBaseDeal(DealStage.QUALIFICATION, BudgetStatus.UNDER_REVIEW);
            withoutCLevel.setDecisionMaker("Manager");

            ProbabilityResult withCLevelResult = service.calculateProbability(withCLevel);
            ProbabilityResult withoutCLevelResult = service.calculateProbability(withoutCLevel);

            assertThat(withCLevelResult.getSuccessRate()).isGreaterThan(withoutCLevelResult.getSuccessRate());
            assertThat(withCLevelResult.getFactors().getPositive())
                    .anyMatch(f -> f.contains("C-level decision maker"));
        }

        @Test
        @DisplayName("Scheduled meeting should add +5 bonus")
        void scheduledMeeting_AddsBonus() {
            Deal withMeeting = createBaseDeal(DealStage.QUALIFICATION, BudgetStatus.UNDER_REVIEW);
            withMeeting.setNextMeeting(LocalDate.now().plusDays(5));

            Deal withoutMeeting = createBaseDeal(DealStage.QUALIFICATION, BudgetStatus.UNDER_REVIEW);

            ProbabilityResult withMeetingResult = service.calculateProbability(withMeeting);
            ProbabilityResult withoutMeetingResult = service.calculateProbability(withoutMeeting);

            assertThat(withMeetingResult.getSuccessRate()).isGreaterThan(withoutMeetingResult.getSuccessRate());
            assertThat(withMeetingResult.getFactors().getPositive())
                    .anyMatch(f -> f.contains("Meeting scheduled"));
        }

        @Test
        @DisplayName("POC mention in notes should add +5 bonus")
        void pocMention_AddsBonus() {
            Deal withPoc = createBaseDeal(DealStage.QUALIFICATION, BudgetStatus.UNDER_REVIEW);
            withPoc.setNotes("Customer requested POC for the solution");

            Deal withoutPoc = createBaseDeal(DealStage.QUALIFICATION, BudgetStatus.UNDER_REVIEW);
            withoutPoc.setNotes("Standard follow-up meeting");

            ProbabilityResult withPocResult = service.calculateProbability(withPoc);
            ProbabilityResult withoutPocResult = service.calculateProbability(withoutPoc);

            assertThat(withPocResult.getSuccessRate()).isGreaterThan(withoutPocResult.getSuccessRate());
            assertThat(withPocResult.getFactors().getPositive())
                    .anyMatch(f -> f.contains("POC/Demo activity"));
        }

        @Test
        @DisplayName("Demo mention should also add +5 bonus")
        void demoMention_AddsBonus() {
            Deal withDemo = createBaseDeal(DealStage.QUALIFICATION, BudgetStatus.UNDER_REVIEW);
            withDemo.setNotes("Demonstration scheduled for next week");

            ProbabilityResult result = service.calculateProbability(withDemo);

            assertThat(result.getFactors().getPositive())
                    .anyMatch(f -> f.contains("POC/Demo activity"));
        }
    }

    @Nested
    @DisplayName("Probability Clamping")
    class ProbabilityClamping {

        @Test
        @DisplayName("Probability should not go below 5%")
        void minimumClamp() {
            Deal deal = createBaseDeal(DealStage.DISCOVERY, BudgetStatus.CANCELLED);
            deal.setLastContact(LocalDate.now().minusDays(100));

            ProbabilityResult result = service.calculateProbability(deal);

            assertThat(result.getSuccessRate()).isGreaterThanOrEqualTo(5);
        }

        @Test
        @DisplayName("Probability should not exceed 95% for active deals")
        void maximumClamp() {
            Deal deal = createBaseDeal(DealStage.NEGOTIATION, BudgetStatus.EXECUTED);
            deal.setDecisionMaker("CEO");
            deal.setNextMeeting(LocalDate.now().plusDays(1));
            deal.setNotes("POC completed successfully, demo went great");

            ProbabilityResult result = service.calculateProbability(deal);

            assertThat(result.getSuccessRate()).isLessThanOrEqualTo(95);
        }
    }

    @Nested
    @DisplayName("Factors Generation")
    class FactorsGeneration {

        @Test
        @DisplayName("Should generate positive factors for good conditions")
        void positiveFactors() {
            Deal deal = createBaseDeal(DealStage.PROPOSAL, BudgetStatus.APPROVED);
            deal.setLastContact(LocalDate.now().minusDays(2));
            deal.setDecisionMaker("CIO");

            ProbabilityResult result = service.calculateProbability(deal);

            assertThat(result.getFactors().getPositive()).isNotEmpty();
            assertThat(result.getFactors().getPositive())
                    .anyMatch(f -> f.contains("Budget approved"));
            assertThat(result.getFactors().getPositive())
                    .anyMatch(f -> f.contains("Recent contact"));
        }

        @Test
        @DisplayName("Should generate negative factors for bad conditions")
        void negativeFactors() {
            Deal deal = createBaseDeal(DealStage.DISCOVERY, BudgetStatus.NOT_CONFIRMED);
            deal.setLastContact(LocalDate.now().minusDays(45));
            deal.setCompetition("Microsoft Azure");

            ProbabilityResult result = service.calculateProbability(deal);

            assertThat(result.getFactors().getNegative()).isNotEmpty();
            assertThat(result.getFactors().getNegative())
                    .anyMatch(f -> f.contains("Budget not confirmed"));
            assertThat(result.getFactors().getNegative())
                    .anyMatch(f -> f.contains("Competition present"));
        }
    }

    @Nested
    @DisplayName("Confidence Level")
    class ConfidenceLevel {

        @Test
        @DisplayName("High probability with positive factors should have High confidence")
        void highConfidence() {
            Deal deal = createBaseDeal(DealStage.NEGOTIATION, BudgetStatus.APPROVED);
            deal.setDecisionMaker("CEO");
            deal.setNextMeeting(LocalDate.now().plusDays(3));

            ProbabilityResult result = service.calculateProbability(deal);

            assertThat(result.getConfidenceLevel()).isIn("High", "Medium-High");
        }

        @Test
        @DisplayName("Low probability with negative factors should have Low confidence")
        void lowConfidence() {
            Deal deal = createBaseDeal(DealStage.DISCOVERY, BudgetStatus.CANCELLED);
            deal.setLastContact(LocalDate.now().minusDays(90));

            ProbabilityResult result = service.calculateProbability(deal);

            assertThat(result.getConfidenceLevel()).isIn("Low", "Medium-Low");
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
                .lastContact(LocalDate.now().minusDays(3))
                .user(testUser)
                .build();
    }
}
