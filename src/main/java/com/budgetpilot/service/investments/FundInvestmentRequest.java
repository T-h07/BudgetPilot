package com.budgetpilot.service.investments;

import com.budgetpilot.model.enums.FundingSourceType;
import com.budgetpilot.model.enums.InvestmentTransactionType;
import com.budgetpilot.util.MoneyUtils;
import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FundInvestmentRequest {
    private final String investmentId;
    private final BigDecimal amount;
    private final LocalDate date;
    private final InvestmentTransactionType txType;
    private final FundingSourceType sourceType;
    private final String sourceRefId;
    private final String note;

    public FundInvestmentRequest(
            String investmentId,
            BigDecimal amount,
            LocalDate date,
            InvestmentTransactionType txType,
            FundingSourceType sourceType,
            String sourceRefId,
            String note
    ) {
        this.investmentId = ValidationUtils.requireNonBlank(investmentId, "investmentId");
        this.amount = normalizePositiveAmount(amount);
        this.date = ValidationUtils.requireNonNull(date, "date");
        this.txType = txType == null ? InvestmentTransactionType.CONTRIBUTION : txType;
        if (this.txType != InvestmentTransactionType.CONTRIBUTION) {
            throw new IllegalArgumentException("Only CONTRIBUTION funding is supported.");
        }
        this.sourceType = sourceType == null ? FundingSourceType.FREE_MONEY : sourceType;
        this.sourceRefId = this.sourceType == FundingSourceType.SAVINGS_BUCKET
                ? ValidationUtils.requireNonBlank(sourceRefId, "sourceRefId")
                : null;
        this.note = note == null ? "" : note.trim();
    }

    public String getInvestmentId() {
        return investmentId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public InvestmentTransactionType getTxType() {
        return txType;
    }

    public FundingSourceType getSourceType() {
        return sourceType;
    }

    public String getSourceRefId() {
        return sourceRefId;
    }

    public String getNote() {
        return note;
    }

    private BigDecimal normalizePositiveAmount(BigDecimal amount) {
        BigDecimal normalized = MoneyUtils.normalize(ValidationUtils.requireNonNull(amount, "amount"));
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0.");
        }
        return normalized;
    }
}
