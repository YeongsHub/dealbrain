package com.example.sales.service;

import com.example.sales.exception.CsvParsingException;
import com.example.sales.exception.CsvValidationException;
import com.example.sales.model.entity.Deal;
import com.example.sales.model.entity.User;
import com.example.sales.model.enums.BudgetStatus;
import com.example.sales.model.enums.DealStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class CsvParsingServiceTest {

    private CsvParsingService service;
    private User testUser;

    @BeforeEach
    void setUp() {
        service = new CsvParsingService();
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .build();
    }

    @Nested
    @DisplayName("Valid CSV Parsing")
    class ValidCsvParsing {

        @Test
        @DisplayName("Should parse valid CSV with all required fields")
        void parseValidCsv() {
            String csvContent = """
                Deal_ID,Company_Name,Contact_Name,Contact_Email,Contact_Title,Deal_Stage,Deal_Value,Product_Interest,Pain_Points,Competition,Decision_Maker,Budget_Status,Sales_Rep,Region,Last_Contact,Next_Meeting,Notes
                DEAL-001,Samsung,James Kim,james@samsung.com,Manager,QUALIFICATION,500000,Enterprise AI,Data silos,Microsoft,CTO,APPROVED,John Smith,Seoul,2026-01-10,2026-01-20,Technical review scheduled
                """;

            MockMultipartFile file = createCsvFile(csvContent);

            List<Deal> deals = service.parseCsvFile(file, testUser);

            assertThat(deals).hasSize(1);
            Deal deal = deals.get(0);
            assertThat(deal.getDealId()).isEqualTo("DEAL-001");
            assertThat(deal.getCompanyName()).isEqualTo("Samsung");
            assertThat(deal.getContactName()).isEqualTo("James Kim");
            assertThat(deal.getContactEmail()).isEqualTo("james@samsung.com");
            assertThat(deal.getDealStage()).isEqualTo(DealStage.QUALIFICATION);
            assertThat(deal.getDealValue()).isEqualByComparingTo(BigDecimal.valueOf(500000));
            assertThat(deal.getBudgetStatus()).isEqualTo(BudgetStatus.APPROVED);
            assertThat(deal.getUser()).isEqualTo(testUser);
        }

        @Test
        @DisplayName("Should parse multiple rows correctly")
        void parseMultipleRows() {
            String csvContent = """
                Deal_ID,Company_Name,Contact_Name,Contact_Email,Deal_Stage,Deal_Value,Budget_Status
                DEAL-001,Samsung,James Kim,james@samsung.com,QUALIFICATION,500000,APPROVED
                DEAL-002,LG,Sarah Lee,sarah@lg.com,PROPOSAL,750000,UNDER_REVIEW
                DEAL-003,Hyundai,Mike Park,mike@hyundai.com,NEGOTIATION,1000000,EXECUTED
                """;

            MockMultipartFile file = createCsvFile(csvContent);

            List<Deal> deals = service.parseCsvFile(file, testUser);

            assertThat(deals).hasSize(3);
            assertThat(deals).extracting(Deal::getDealId)
                    .containsExactly("DEAL-001", "DEAL-002", "DEAL-003");
            assertThat(deals).extracting(Deal::getDealStage)
                    .containsExactly(DealStage.QUALIFICATION, DealStage.PROPOSAL, DealStage.NEGOTIATION);
        }

        @Test
        @DisplayName("Should handle optional fields being empty")
        void handleOptionalFields() {
            String csvContent = """
                Deal_ID,Company_Name,Contact_Name,Contact_Email,Deal_Stage,Deal_Value,Budget_Status
                DEAL-001,Samsung,James Kim,james@samsung.com,QUALIFICATION,500000,APPROVED
                """;

            MockMultipartFile file = createCsvFile(csvContent);

            List<Deal> deals = service.parseCsvFile(file, testUser);

            assertThat(deals).hasSize(1);
            Deal deal = deals.get(0);
            assertThat(deal.getContactTitle()).isNull();
            assertThat(deal.getProductInterest()).isNull();
            assertThat(deal.getPainPoints()).isNull();
            assertThat(deal.getCompetition()).isNull();
            assertThat(deal.getNextMeeting()).isNull();
        }

        @Test
        @DisplayName("Should parse dates correctly")
        void parseDatesCorrectly() {
            String csvContent = """
                Deal_ID,Company_Name,Contact_Name,Contact_Email,Deal_Stage,Deal_Value,Budget_Status,Last_Contact,Next_Meeting
                DEAL-001,Samsung,James Kim,james@samsung.com,QUALIFICATION,500000,APPROVED,2026-01-10,2026-01-20
                """;

            MockMultipartFile file = createCsvFile(csvContent);

            List<Deal> deals = service.parseCsvFile(file, testUser);

            assertThat(deals).hasSize(1);
            Deal deal = deals.get(0);
            assertThat(deal.getLastContact()).isEqualTo(LocalDate.of(2026, 1, 10));
            assertThat(deal.getNextMeeting()).isEqualTo(LocalDate.of(2026, 1, 20));
        }

        @Test
        @DisplayName("Should trim whitespace from fields")
        void trimWhitespace() {
            String csvContent = """
                Deal_ID,Company_Name,Contact_Name,Contact_Email,Deal_Stage,Deal_Value,Budget_Status
                  DEAL-001  ,  Samsung  ,  James Kim  ,  james@samsung.com  ,QUALIFICATION,  500000  ,APPROVED
                """;

            MockMultipartFile file = createCsvFile(csvContent);

            List<Deal> deals = service.parseCsvFile(file, testUser);

            assertThat(deals).hasSize(1);
            Deal deal = deals.get(0);
            assertThat(deal.getDealId()).isEqualTo("DEAL-001");
            assertThat(deal.getCompanyName()).isEqualTo("Samsung");
        }
    }

    @Nested
    @DisplayName("Enum Parsing")
    class EnumParsing {

        @Test
        @DisplayName("Should parse all DealStage values")
        void parseAllDealStages() {
            String csvContent = """
                Deal_ID,Company_Name,Contact_Name,Contact_Email,Deal_Stage,Deal_Value,Budget_Status
                DEAL-001,Company1,Contact1,c1@test.com,DISCOVERY,100000,APPROVED
                DEAL-002,Company2,Contact2,c2@test.com,QUALIFICATION,200000,APPROVED
                DEAL-003,Company3,Contact3,c3@test.com,PROPOSAL,300000,APPROVED
                DEAL-004,Company4,Contact4,c4@test.com,NEGOTIATION,400000,APPROVED
                DEAL-005,Company5,Contact5,c5@test.com,CLOSED_WON,500000,APPROVED
                DEAL-006,Company6,Contact6,c6@test.com,CLOSED_LOST,600000,APPROVED
                """;

            MockMultipartFile file = createCsvFile(csvContent);

            List<Deal> deals = service.parseCsvFile(file, testUser);

            assertThat(deals).extracting(Deal::getDealStage)
                    .containsExactly(
                            DealStage.DISCOVERY,
                            DealStage.QUALIFICATION,
                            DealStage.PROPOSAL,
                            DealStage.NEGOTIATION,
                            DealStage.CLOSED_WON,
                            DealStage.CLOSED_LOST
                    );
        }

        @Test
        @DisplayName("Should parse all BudgetStatus values")
        void parseAllBudgetStatuses() {
            String csvContent = """
                Deal_ID,Company_Name,Contact_Name,Contact_Email,Deal_Stage,Deal_Value,Budget_Status
                DEAL-001,Company1,Contact1,c1@test.com,QUALIFICATION,100000,APPROVED
                DEAL-002,Company2,Contact2,c2@test.com,QUALIFICATION,200000,UNDER_REVIEW
                DEAL-003,Company3,Contact3,c3@test.com,QUALIFICATION,300000,NOT_CONFIRMED
                DEAL-004,Company4,Contact4,c4@test.com,QUALIFICATION,400000,CANCELLED
                DEAL-005,Company5,Contact5,c5@test.com,QUALIFICATION,500000,EXECUTED
                """;

            MockMultipartFile file = createCsvFile(csvContent);

            List<Deal> deals = service.parseCsvFile(file, testUser);

            assertThat(deals).extracting(Deal::getBudgetStatus)
                    .containsExactly(
                            BudgetStatus.APPROVED,
                            BudgetStatus.UNDER_REVIEW,
                            BudgetStatus.NOT_CONFIRMED,
                            BudgetStatus.CANCELLED,
                            BudgetStatus.EXECUTED
                    );
        }

        @Test
        @DisplayName("Should handle case-insensitive enum values")
        void caseInsensitiveEnums() {
            String csvContent = """
                Deal_ID,Company_Name,Contact_Name,Contact_Email,Deal_Stage,Deal_Value,Budget_Status
                DEAL-001,Samsung,James,james@test.com,qualification,500000,approved
                DEAL-002,LG,Sarah,sarah@test.com,Proposal,600000,Under_Review
                """;

            MockMultipartFile file = createCsvFile(csvContent);

            List<Deal> deals = service.parseCsvFile(file, testUser);

            assertThat(deals).hasSize(2);
            assertThat(deals.get(0).getDealStage()).isEqualTo(DealStage.QUALIFICATION);
            assertThat(deals.get(1).getDealStage()).isEqualTo(DealStage.PROPOSAL);
        }

        @Test
        @DisplayName("Should handle common variations of Closed Won/Lost")
        void handleClosedVariations() {
            String csvContent = """
                Deal_ID,Company_Name,Contact_Name,Contact_Email,Deal_Stage,Deal_Value,Budget_Status
                DEAL-001,Company1,Contact1,c1@test.com,Won,100000,APPROVED
                DEAL-002,Company2,Contact2,c2@test.com,Lost,200000,APPROVED
                """;

            MockMultipartFile file = createCsvFile(csvContent);

            List<Deal> deals = service.parseCsvFile(file, testUser);

            assertThat(deals.get(0).getDealStage()).isEqualTo(DealStage.CLOSED_WON);
            assertThat(deals.get(1).getDealStage()).isEqualTo(DealStage.CLOSED_LOST);
        }
    }

    @Nested
    @DisplayName("Validation Errors")
    class ValidationErrors {

        @Test
        @DisplayName("Should throw CsvValidationException for missing Deal_ID")
        void missingDealId() {
            String csvContent = """
                Deal_ID,Company_Name,Contact_Name,Contact_Email,Deal_Stage,Deal_Value,Budget_Status
                ,Samsung,James,james@test.com,QUALIFICATION,500000,APPROVED
                """;

            MockMultipartFile file = createCsvFile(csvContent);

            assertThatThrownBy(() -> service.parseCsvFile(file, testUser))
                    .isInstanceOf(CsvValidationException.class)
                    .hasMessageContaining("Deal_ID");
        }

        @Test
        @DisplayName("Should throw CsvValidationException for missing Company_Name")
        void missingCompanyName() {
            String csvContent = """
                Deal_ID,Company_Name,Contact_Name,Contact_Email,Deal_Stage,Deal_Value,Budget_Status
                DEAL-001,,James,james@test.com,QUALIFICATION,500000,APPROVED
                """;

            MockMultipartFile file = createCsvFile(csvContent);

            assertThatThrownBy(() -> service.parseCsvFile(file, testUser))
                    .isInstanceOf(CsvValidationException.class)
                    .hasMessageContaining("Company_Name");
        }

        @Test
        @DisplayName("Should throw CsvValidationException for invalid DealStage")
        void invalidDealStage() {
            String csvContent = """
                Deal_ID,Company_Name,Contact_Name,Contact_Email,Deal_Stage,Deal_Value,Budget_Status
                DEAL-001,Samsung,James,james@test.com,INVALID_STAGE,500000,APPROVED
                """;

            MockMultipartFile file = createCsvFile(csvContent);

            assertThatThrownBy(() -> service.parseCsvFile(file, testUser))
                    .isInstanceOf(CsvValidationException.class)
                    .hasMessageContaining("Deal_Stage");
        }

        @Test
        @DisplayName("Should throw CsvValidationException for invalid BudgetStatus")
        void invalidBudgetStatus() {
            String csvContent = """
                Deal_ID,Company_Name,Contact_Name,Contact_Email,Deal_Stage,Deal_Value,Budget_Status
                DEAL-001,Samsung,James,james@test.com,QUALIFICATION,500000,INVALID_STATUS
                """;

            MockMultipartFile file = createCsvFile(csvContent);

            assertThatThrownBy(() -> service.parseCsvFile(file, testUser))
                    .isInstanceOf(CsvValidationException.class)
                    .hasMessageContaining("Budget_Status");
        }

        @Test
        @DisplayName("Should throw CsvValidationException for invalid Deal_Value")
        void invalidDealValue() {
            String csvContent = """
                Deal_ID,Company_Name,Contact_Name,Contact_Email,Deal_Stage,Deal_Value,Budget_Status
                DEAL-001,Samsung,James,james@test.com,QUALIFICATION,not_a_number,APPROVED
                """;

            MockMultipartFile file = createCsvFile(csvContent);

            assertThatThrownBy(() -> service.parseCsvFile(file, testUser))
                    .isInstanceOf(CsvValidationException.class)
                    .hasMessageContaining("Deal_Value");
        }

        @Test
        @DisplayName("Should throw CsvValidationException for invalid date format")
        void invalidDateFormat() {
            String csvContent = """
                Deal_ID,Company_Name,Contact_Name,Contact_Email,Deal_Stage,Deal_Value,Budget_Status,Last_Contact
                DEAL-001,Samsung,James,james@test.com,QUALIFICATION,500000,APPROVED,01/10/2026
                """;

            MockMultipartFile file = createCsvFile(csvContent);

            assertThatThrownBy(() -> service.parseCsvFile(file, testUser))
                    .isInstanceOf(CsvValidationException.class)
                    .hasMessageContaining("Last_Contact");
        }

        @Test
        @DisplayName("Should include row number in validation exception")
        void includeRowNumber() {
            String csvContent = """
                Deal_ID,Company_Name,Contact_Name,Contact_Email,Deal_Stage,Deal_Value,Budget_Status
                DEAL-001,Samsung,James,james@test.com,QUALIFICATION,500000,APPROVED
                DEAL-002,,James,james@test.com,QUALIFICATION,500000,APPROVED
                """;

            MockMultipartFile file = createCsvFile(csvContent);

            assertThatThrownBy(() -> service.parseCsvFile(file, testUser))
                    .isInstanceOf(CsvValidationException.class)
                    .hasMessageContaining("Row 3"); // Row 1 is header, row 2 is valid, row 3 has error
        }

        @Test
        @DisplayName("Should collect multiple field errors for a row")
        void multipleFieldErrors() {
            String csvContent = """
                Deal_ID,Company_Name,Contact_Name,Contact_Email,Deal_Stage,Deal_Value,Budget_Status
                ,,,,INVALID_STAGE,not_a_number,INVALID_STATUS
                """;

            MockMultipartFile file = createCsvFile(csvContent);

            assertThatThrownBy(() -> service.parseCsvFile(file, testUser))
                    .isInstanceOf(CsvValidationException.class)
                    .satisfies(ex -> {
                        CsvValidationException csvEx = (CsvValidationException) ex;
                        assertThat(csvEx.getFieldErrors()).containsKey("Deal_ID");
                        assertThat(csvEx.getFieldErrors()).containsKey("Company_Name");
                    });
        }
    }

    @Nested
    @DisplayName("Parsing Errors")
    class ParsingErrors {

        @Test
        @DisplayName("Should handle empty CSV file")
        void emptyCsvFile() {
            String csvContent = """
                Deal_ID,Company_Name,Contact_Name,Contact_Email,Deal_Stage,Deal_Value,Budget_Status
                """;

            MockMultipartFile file = createCsvFile(csvContent);

            List<Deal> deals = service.parseCsvFile(file, testUser);

            assertThat(deals).isEmpty();
        }

        @Test
        @DisplayName("Should handle file with only headers")
        void onlyHeaders() {
            String csvContent = "Deal_ID,Company_Name,Contact_Name,Contact_Email,Deal_Stage,Deal_Value,Budget_Status\n";

            MockMultipartFile file = createCsvFile(csvContent);

            List<Deal> deals = service.parseCsvFile(file, testUser);

            assertThat(deals).isEmpty();
        }
    }

    @Nested
    @DisplayName("Deal Value Handling")
    class DealValueHandling {

        @Test
        @DisplayName("Should handle deal values with currency symbols")
        void handleCurrencySymbols() {
            String csvContent = """
                Deal_ID,Company_Name,Contact_Name,Contact_Email,Deal_Stage,Deal_Value,Budget_Status
                DEAL-001,Samsung,James,james@test.com,QUALIFICATION,$500000,APPROVED
                """;

            MockMultipartFile file = createCsvFile(csvContent);

            List<Deal> deals = service.parseCsvFile(file, testUser);

            assertThat(deals.get(0).getDealValue()).isEqualByComparingTo(BigDecimal.valueOf(500000));
        }

        @Test
        @DisplayName("Should handle deal values with commas")
        void handleCommasInValue() {
            // Note: This depends on how the CSV is quoted
            String csvContent = """
                Deal_ID,Company_Name,Contact_Name,Contact_Email,Deal_Stage,Deal_Value,Budget_Status
                DEAL-001,Samsung,James,james@test.com,QUALIFICATION,500000.50,APPROVED
                """;

            MockMultipartFile file = createCsvFile(csvContent);

            List<Deal> deals = service.parseCsvFile(file, testUser);

            assertThat(deals.get(0).getDealValue()).isEqualByComparingTo(new BigDecimal("500000.50"));
        }
    }

    private MockMultipartFile createCsvFile(String content) {
        return new MockMultipartFile(
                "file",
                "test.csv",
                "text/csv",
                content.getBytes()
        );
    }
}
