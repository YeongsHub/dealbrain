package com.example.sales.model.enums;

import lombok.Getter;

@Getter
public enum BudgetStatus {
    APPROVED(1.2),
    UNDER_REVIEW(1.0),
    NOT_CONFIRMED(0.7),
    CANCELLED(0.0),
    EXECUTED(1.3);

    private final double multiplier;

    BudgetStatus(double multiplier) {
        this.multiplier = multiplier;
    }
}
