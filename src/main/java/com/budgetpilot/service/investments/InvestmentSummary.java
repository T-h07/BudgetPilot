package com.budgetpilot.service.investments;

import com.budgetpilot.model.Investment;
import com.budgetpilot.model.InvestmentTransaction;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

public class InvestmentSummary {
    private final YearMonth month;
    private final Investment investment;
    private final InvestmentPositionSummary position;
    private final BigDecimal monthlyContributionTotal;
    private final BigDecimal monthlyReturnTotal;
    private final BigDecimal monthlyFeeTotal;
    private final BigDecimal monthlyWithdrawalTotal;
    private final List<InvestmentTransaction> transactions;

    public InvestmentSummary(
            YearMonth month,
            Investment investment,
            InvestmentPositionSummary position,
            BigDecimal monthlyContributionTotal,
            BigDecimal monthlyReturnTotal,
            BigDecimal monthlyFeeTotal,
            BigDecimal monthlyWithdrawalTotal,
            List<InvestmentTransaction> transactions
    ) {
        this.month = Objects.requireNonNull(month, "month");
        this.investment = Objects.requireNonNull(investment, "investment");
        this.position = Objects.requireNonNull(position, "position");
        this.monthlyContributionTotal = Objects.requireNonNull(monthlyContributionTotal, "monthlyContributionTotal");
        this.monthlyReturnTotal = Objects.requireNonNull(monthlyReturnTotal, "monthlyReturnTotal");
        this.monthlyFeeTotal = Objects.requireNonNull(monthlyFeeTotal, "monthlyFeeTotal");
        this.monthlyWithdrawalTotal = Objects.requireNonNull(monthlyWithdrawalTotal, "monthlyWithdrawalTotal");
        this.transactions = List.copyOf(transactions == null ? List.of() : transactions);
    }

    public YearMonth getMonth() {
        return month;
    }

    public Investment getInvestment() {
        return investment;
    }

    public InvestmentPositionSummary getPosition() {
        return position;
    }

    public BigDecimal getMonthlyContributionTotal() {
        return monthlyContributionTotal;
    }

    public BigDecimal getMonthlyReturnTotal() {
        return monthlyReturnTotal;
    }

    public BigDecimal getMonthlyFeeTotal() {
        return monthlyFeeTotal;
    }

    public BigDecimal getMonthlyWithdrawalTotal() {
        return monthlyWithdrawalTotal;
    }

    public List<InvestmentTransaction> getTransactions() {
        return transactions;
    }
}
