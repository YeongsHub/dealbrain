package com.example.sales.model.dto;

import com.opencsv.bean.CsvBindByName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsvDealRow {

    @CsvBindByName(column = "Deal_ID")
    private String dealId;

    @CsvBindByName(column = "Company_Name")
    private String companyName;

    @CsvBindByName(column = "Contact_Name")
    private String contactName;

    @CsvBindByName(column = "Contact_Email")
    private String contactEmail;

    @CsvBindByName(column = "Contact_Title")
    private String contactTitle;

    @CsvBindByName(column = "Deal_Stage")
    private String dealStage;

    @CsvBindByName(column = "Deal_Value")
    private String dealValue;

    @CsvBindByName(column = "Product_Interest")
    private String productInterest;

    @CsvBindByName(column = "Pain_Points")
    private String painPoints;

    @CsvBindByName(column = "Competition")
    private String competition;

    @CsvBindByName(column = "Decision_Maker")
    private String decisionMaker;

    @CsvBindByName(column = "Budget_Status")
    private String budgetStatus;

    @CsvBindByName(column = "Sales_Rep")
    private String salesRep;

    @CsvBindByName(column = "Region")
    private String region;

    @CsvBindByName(column = "Last_Contact")
    private String lastContact;

    @CsvBindByName(column = "Next_Meeting")
    private String nextMeeting;

    @CsvBindByName(column = "Notes")
    private String notes;
}
