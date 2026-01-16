package com.example.sales.model.enums;

import lombok.Getter;

@Getter
public enum DealStage {
    DISCOVERY(15),
    QUALIFICATION(35),
    PROPOSAL(55),
    NEGOTIATION(75),
    CLOSED_WON(100),
    CLOSED_LOST(0);

    private final int baseWeight;

    DealStage(int baseWeight) {
        this.baseWeight = baseWeight;
    }
}
