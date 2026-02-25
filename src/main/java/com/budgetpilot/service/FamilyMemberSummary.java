package com.budgetpilot.service;

import com.budgetpilot.model.enums.RelationshipType;

import java.math.BigDecimal;
import java.util.Objects;

public class FamilyMemberSummary {
    private final String memberId;
    private final String memberName;
    private final RelationshipType relationshipType;
    private final BigDecimal monthlyTotal;
    private final BigDecimal allowanceTotal;
    private final BigDecimal medicalTotal;
    private final BigDecimal supportTotal;
    private final int expenseCount;
    private final boolean active;

    public FamilyMemberSummary(
            String memberId,
            String memberName,
            RelationshipType relationshipType,
            BigDecimal monthlyTotal,
            BigDecimal allowanceTotal,
            BigDecimal medicalTotal,
            BigDecimal supportTotal,
            int expenseCount,
            boolean active
    ) {
        this.memberId = Objects.requireNonNull(memberId, "memberId");
        this.memberName = Objects.requireNonNull(memberName, "memberName");
        this.relationshipType = Objects.requireNonNull(relationshipType, "relationshipType");
        this.monthlyTotal = Objects.requireNonNull(monthlyTotal, "monthlyTotal");
        this.allowanceTotal = Objects.requireNonNull(allowanceTotal, "allowanceTotal");
        this.medicalTotal = Objects.requireNonNull(medicalTotal, "medicalTotal");
        this.supportTotal = Objects.requireNonNull(supportTotal, "supportTotal");
        this.expenseCount = expenseCount;
        this.active = active;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getMemberName() {
        return memberName;
    }

    public RelationshipType getRelationshipType() {
        return relationshipType;
    }

    public BigDecimal getMonthlyTotal() {
        return monthlyTotal;
    }

    public BigDecimal getAllowanceTotal() {
        return allowanceTotal;
    }

    public BigDecimal getMedicalTotal() {
        return medicalTotal;
    }

    public BigDecimal getSupportTotal() {
        return supportTotal;
    }

    public int getExpenseCount() {
        return expenseCount;
    }

    public boolean isActive() {
        return active;
    }
}
