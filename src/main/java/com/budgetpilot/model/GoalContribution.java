package com.budgetpilot.model;

import com.budgetpilot.model.enums.GoalContributionType;
import com.budgetpilot.model.enums.GoalFundingSource;
import com.budgetpilot.util.MonthUtils;
import com.budgetpilot.util.MoneyUtils;
import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

public class GoalContribution {
    private String id;
    private String goalId;
    private YearMonth month;
    private LocalDate contributionDate;
    private BigDecimal amount;
    private GoalContributionType type;
    private GoalFundingSource sourceType;
    private String sourceRefId;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public GoalContribution() {
        LocalDateTime now = LocalDateTime.now();
        this.id = UUID.randomUUID().toString();
        this.goalId = "";
        this.month = MonthUtils.currentMonth();
        this.contributionDate = LocalDate.now();
        this.amount = new BigDecimal("1.00");
        this.type = GoalContributionType.CONTRIBUTION;
        this.sourceType = GoalFundingSource.FREE_MONEY;
        this.sourceRefId = "";
        this.note = "";
        this.createdAt = now;
        this.updatedAt = now;
    }

    public GoalContribution(
            String goalId,
            YearMonth month,
            LocalDate contributionDate,
            BigDecimal amount,
            GoalContributionType type
    ) {
        this();
        setGoalId(goalId);
        setMonth(month);
        setContributionDate(contributionDate);
        setAmount(amount);
        setType(type);
    }

    public GoalContribution(GoalContribution other) {
        ValidationUtils.requireNonNull(other, "other");
        this.id = other.id;
        this.goalId = other.goalId;
        this.month = other.month;
        this.contributionDate = other.contributionDate;
        this.amount = other.amount;
        this.type = other.type;
        this.sourceType = other.sourceType == null ? GoalFundingSource.FREE_MONEY : other.sourceType;
        this.sourceRefId = other.sourceRefId == null ? "" : other.sourceRefId;
        this.note = other.note;
        this.createdAt = other.createdAt;
        this.updatedAt = other.updatedAt;
    }

    public GoalContribution copy() {
        return new GoalContribution(this);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = (id == null || id.isBlank()) ? UUID.randomUUID().toString() : id.trim();
        touch();
    }

    public String getGoalId() {
        return goalId;
    }

    public void setGoalId(String goalId) {
        this.goalId = ValidationUtils.requireNonBlank(goalId, "goalId");
        touch();
    }

    public YearMonth getMonth() {
        return month;
    }

    public void setMonth(YearMonth month) {
        this.month = ValidationUtils.requireNonNull(month, "month");
        touch();
    }

    public LocalDate getContributionDate() {
        return contributionDate;
    }

    public void setContributionDate(LocalDate contributionDate) {
        this.contributionDate = ValidationUtils.requireNonNull(contributionDate, "contributionDate");
        touch();
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        BigDecimal normalized = MoneyUtils.normalize(ValidationUtils.requireNonNull(amount, "amount"));
        if (normalized.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("amount must not be zero");
        }
        this.amount = normalized;
        touch();
    }

    public GoalContributionType getType() {
        return type;
    }

    public void setType(GoalContributionType type) {
        this.type = ValidationUtils.requireNonNull(type, "type");
        touch();
    }

    public GoalFundingSource getSourceType() {
        return sourceType;
    }

    public void setSourceType(GoalFundingSource sourceType) {
        this.sourceType = ValidationUtils.requireNonNull(sourceType, "sourceType");
        touch();
    }

    public String getSourceRefId() {
        return sourceRefId;
    }

    public void setSourceRefId(String sourceRefId) {
        this.sourceRefId = sourceRefId == null ? "" : sourceRefId.trim();
        touch();
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note == null ? "" : note.trim();
        touch();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = ValidationUtils.requireNonNull(createdAt, "createdAt");
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = ValidationUtils.requireNonNull(updatedAt, "updatedAt");
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "GoalContribution{" +
                "id='" + id + '\'' +
                ", goalId='" + goalId + '\'' +
                ", month=" + month +
                ", contributionDate=" + contributionDate +
                ", amount=" + amount +
                ", type=" + type +
                ", sourceType=" + sourceType +
                ", sourceRefId='" + sourceRefId + '\'' +
                '}';
    }
}
