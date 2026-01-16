package com.example.sales.model.entity;

import com.example.sales.model.enums.BudgetStatus;
import com.example.sales.model.enums.DealStage;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "deals")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Deal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deal_id", nullable = false)
    private String dealId;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "contact_name", nullable = false)
    private String contactName;

    @Column(name = "contact_email", nullable = false)
    private String contactEmail;

    @Column(name = "contact_title")
    private String contactTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "deal_stage", nullable = false)
    private DealStage dealStage;

    @Column(name = "deal_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal dealValue;

    @Column(name = "product_interest")
    private String productInterest;

    @Column(name = "pain_points", columnDefinition = "TEXT")
    private String painPoints;

    @Column(name = "competition")
    private String competition;

    @Column(name = "decision_maker")
    private String decisionMaker;

    @Enumerated(EnumType.STRING)
    @Column(name = "budget_status", nullable = false)
    private BudgetStatus budgetStatus;

    @Column(name = "sales_rep")
    private String salesRep;

    @Column(name = "region")
    private String region;

    @Column(name = "last_contact")
    private LocalDate lastContact;

    @Column(name = "next_meeting")
    private LocalDate nextMeeting;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
