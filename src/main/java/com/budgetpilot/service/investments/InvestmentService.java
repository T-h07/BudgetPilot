package com.budgetpilot.service.investments;

import com.budgetpilot.model.Investment;
import com.budgetpilot.model.InvestmentTransaction;
import com.budgetpilot.model.enums.InvestmentStatus;
import com.budgetpilot.model.enums.InvestmentTransactionType;
import com.budgetpilot.store.BudgetStore;
import com.budgetpilot.util.MoneyUtils;
import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class InvestmentService {
    private final BudgetStore budgetStore;
    private final Set<String> legacyNegativeAmountWarnings = new HashSet<>();

    public InvestmentService(BudgetStore budgetStore) {
        this.budgetStore = ValidationUtils.requireNonNull(budgetStore, "budgetStore");
    }

    public List<Investment> listInvestments() {
        return budgetStore.listInvestments().stream()
                .sorted(Comparator
                        .comparing(Investment::isActive, Comparator.reverseOrder())
                        .thenComparingInt(this::statusRank)
                        .thenComparing(Investment::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public void saveInvestment(Investment investment) {
        InvestmentValidationResult validation = validateInvestment(investment);
        if (!validation.isValid()) {
            throw new IllegalArgumentException(validation.getPrimaryError());
        }

        Investment copy = investment.copy();
        copy.setName(copy.getName());
        copy.setType(copy.getType());
        copy.setKind(copy.getKind());
        copy.setStatus(copy.getStatus());
        copy.setPriority(copy.getPriority());
        copy.setCurrentInvestedAmount(MoneyUtils.zeroIfNull(copy.getCurrentInvestedAmount()));
        copy.setCurrentEstimatedValue(MoneyUtils.zeroIfNull(copy.getCurrentEstimatedValue()));
        budgetStore.saveInvestment(copy);
    }

    public void deleteInvestment(String investmentId) {
        String targetId = ValidationUtils.requireNonBlank(investmentId, "investmentId");
        for (InvestmentTransaction tx : budgetStore.listInvestmentTransactions(targetId)) {
            budgetStore.deleteInvestmentTransaction(tx.getId());
        }
        budgetStore.deleteInvestment(targetId);
    }

    public List<InvestmentTransaction> listTransactions(String investmentId) {
        String targetId = ValidationUtils.requireNonBlank(investmentId, "investmentId");
        return normalizeTransactionsForRead(budgetStore.listInvestmentTransactions(targetId));
    }

    public List<InvestmentTransaction> listTransactions(String investmentId, YearMonth month) {
        String targetId = ValidationUtils.requireNonBlank(investmentId, "investmentId");
        YearMonth targetMonth = ValidationUtils.requireNonNull(month, "month");
        return normalizeTransactionsForRead(budgetStore.listInvestmentTransactions(targetId, targetMonth));
    }

    public void addContribution(String investmentId, BigDecimal amount, LocalDate date, String note) {
        recordTransaction(investmentId, normalizePositive(amount, "Contribution amount"), date,
                InvestmentTransactionType.CONTRIBUTION, note);
    }

    public void addReturn(String investmentId, BigDecimal amount, LocalDate date, String note) {
        recordTransaction(investmentId, normalizePositive(amount, "Return amount"), date,
                InvestmentTransactionType.RETURN, note);
    }

    public void addWithdrawal(String investmentId, BigDecimal amount, LocalDate date, String note) {
        BigDecimal normalized = normalizePositive(amount, "Withdrawal amount");
        ensureCanReduceInvested(investmentId, normalized, "Cannot withdraw more than invested amount.");
        recordTransaction(investmentId, normalized, date, InvestmentTransactionType.WITHDRAWAL, note);
    }

    public void addFee(String investmentId, BigDecimal amount, LocalDate date, String note) {
        BigDecimal normalized = normalizePositive(amount, "Fee amount");
        recordTransaction(investmentId, normalized, date, InvestmentTransactionType.FEE, note);
    }

    public void addAdjustment(String investmentId, BigDecimal signedAmount, LocalDate date, String note) {
        BigDecimal normalized = normalizeNonZero(signedAmount, "Adjustment amount");
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            ensureCanReduceInvested(investmentId, normalized.abs(), "Adjustment would reduce invested amount below zero.");
        }
        recordTransaction(investmentId, normalized, date, InvestmentTransactionType.ADJUSTMENT, note);
    }

    public void deleteTransaction(String transactionId) {
        String txId = ValidationUtils.requireNonBlank(transactionId, "transactionId");
        TransactionLocator locator = findTransaction(txId);
        budgetStore.deleteInvestmentTransaction(txId);
        recomputeInvestmentCachedAmounts(locator.investmentId());
    }

    public InvestmentPageSummary getInvestmentPageSummary(YearMonth month) {
        YearMonth targetMonth = ValidationUtils.requireNonNull(month, "month");
        List<InvestmentPositionSummary> positions = listPositionSummaries(targetMonth);

        BigDecimal totalInvested = MoneyUtils.normalize(positions.stream()
                .map(InvestmentPositionSummary::getInvestedAmountTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal totalEstimated = MoneyUtils.normalize(positions.stream()
                .map(InvestmentPositionSummary::getCurrentEstimatedValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal totalNetProfit = MoneyUtils.normalize(positions.stream()
                .map(InvestmentPositionSummary::getNetProfitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal monthlyContributions = MoneyUtils.normalize(positions.stream()
                .map(InvestmentPositionSummary::getMonthlyContributionTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal monthlyReturns = MoneyUtils.normalize(positions.stream()
                .map(InvestmentPositionSummary::getMonthlyReturnTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        int activeCount = (int) positions.stream().filter(InvestmentPositionSummary::isActive).count();
        int completedCount = (int) positions.stream()
                .filter(position -> position.getStatus() == InvestmentStatus.COMPLETED)
                .count();

        List<BigDecimal> roiValues = positions.stream()
                .map(InvestmentPositionSummary::getRoiPercent)
                .filter(Objects::nonNull)
                .toList();

        BigDecimal avgRoi = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (!roiValues.isEmpty()) {
            avgRoi = MoneyUtils.normalize(roiValues.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(roiValues.size()), 2, RoundingMode.HALF_UP));
        }

        return new InvestmentPageSummary(
                targetMonth,
                totalInvested,
                totalEstimated,
                totalNetProfit,
                monthlyContributions,
                monthlyReturns,
                activeCount,
                completedCount,
                avgRoi,
                positions
        );
    }

    public List<InvestmentPositionSummary> listPositionSummaries(YearMonth month) {
        YearMonth targetMonth = ValidationUtils.requireNonNull(month, "month");
        List<InvestmentPositionSummary> positions = new ArrayList<>();

        for (Investment investment : listInvestments()) {
            List<InvestmentTransaction> allTransactions = listTransactions(investment.getId());
            List<InvestmentTransaction> monthTransactions = listTransactions(investment.getId(), targetMonth);
            InvestmentTotals totals = aggregateTotals(allTransactions);
            InvestmentTotals monthTotals = aggregateTotals(monthTransactions);

            BigDecimal contributions = totals.contributions();
            BigDecimal returns = totals.returns();
            BigDecimal withdrawals = totals.withdrawals();
            BigDecimal fees = totals.fees();
            BigDecimal adjustments = totals.adjustments();
            BigDecimal investedAmount = computeEffectiveInvestedAmount(totals);

            BigDecimal estimatedValue = MoneyUtils.zeroIfNull(investment.getCurrentEstimatedValue());
            if (estimatedValue.compareTo(BigDecimal.ZERO) == 0 && !allTransactions.isEmpty()) {
                estimatedValue = investedAmount;
            }

            // Net profit estimate rule:
            // (Current Value + Withdrawals + Returns) - (Contributions + Fees), with adjustments applied as signed corrections.
            BigDecimal netProfit = MoneyUtils.normalize(
                    estimatedValue
                            .add(withdrawals)
                            .add(returns)
                            .subtract(contributions)
                            .subtract(fees)
                            .subtract(adjustments)
            );
            BigDecimal roiPercent = contributions.compareTo(BigDecimal.ZERO) <= 0
                    ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                    : MoneyUtils.normalize(netProfit.multiply(MoneyUtils.HUNDRED)
                    .divide(contributions, 2, RoundingMode.HALF_UP));

            BigDecimal progressPercent = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            if (investment.getTargetAmount() != null && investment.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
                progressPercent = MoneyUtils.normalize(investedAmount
                        .multiply(MoneyUtils.HUNDRED)
                        .divide(investment.getTargetAmount(), 2, RoundingMode.HALF_UP));
                if (progressPercent.compareTo(MoneyUtils.HUNDRED) > 0) {
                    progressPercent = MoneyUtils.HUNDRED;
                }
            }

            BigDecimal monthlyContribution = monthTotals.contributions();
            BigDecimal monthlyReturn = monthTotals.returns();

            positions.add(new InvestmentPositionSummary(
                    investment.getId(),
                    investment.getName(),
                    investment.getType(),
                    investment.getKind(),
                    investment.getStatus(),
                    investment.getTargetAmount(),
                    investedAmount,
                    returns,
                    fees,
                    withdrawals,
                    estimatedValue,
                    netProfit,
                    roiPercent,
                    progressPercent,
                    investment.getExpectedProfitAmount(),
                    investment.getExpectedReturnDate(),
                    toDaysRemainingText(investment.getExpectedReturnDate()),
                    monthlyContribution,
                    monthlyReturn,
                    allTransactions.size(),
                    investment.isActive()
            ));
        }

        positions.sort(Comparator
                .comparing(InvestmentPositionSummary::isActive, Comparator.reverseOrder())
                .thenComparing(this::statusRank)
                .thenComparing(InvestmentPositionSummary::getName, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(positions);
    }

    public InvestmentSummary getInvestmentSummary(String investmentId, YearMonth month) {
        String targetInvestmentId = ValidationUtils.requireNonBlank(investmentId, "investmentId");
        YearMonth targetMonth = ValidationUtils.requireNonNull(month, "month");

        Investment investment = getInvestmentOrThrow(targetInvestmentId);
        InvestmentPositionSummary position = listPositionSummaries(targetMonth).stream()
                .filter(item -> item.getInvestmentId().equals(targetInvestmentId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Investment summary not found."));

        List<InvestmentTransaction> monthTransactions = listTransactions(targetInvestmentId, targetMonth);
        InvestmentTotals monthTotals = aggregateTotals(monthTransactions);

        return new InvestmentSummary(
                targetMonth,
                investment,
                position,
                monthTotals.contributions(),
                monthTotals.returns(),
                monthTotals.fees(),
                monthTotals.withdrawals(),
                listTransactions(targetInvestmentId)
        );
    }

    public InvestmentValidationResult validateInvestment(Investment investment) {
        InvestmentValidationResult result = new InvestmentValidationResult();
        if (investment == null) {
            result.addError("Investment is required.");
            return result;
        }
        if (investment.getName() == null || investment.getName().isBlank()) {
            result.addError("Investment name is required.");
        }
        if (investment.getType() == null) {
            result.addError("Investment type is required.");
        }
        if (investment.getKind() == null) {
            result.addError("Investment kind is required.");
        }
        if (investment.getStatus() == null) {
            result.addError("Investment status is required.");
        }
        if (investment.getStartDate() == null) {
            result.addError("Start date is required.");
        }
        if (investment.getPriority() < 1 || investment.getPriority() > 5) {
            result.addError("Priority must be between 1 and 5.");
        }
        validateNonNegative(result, investment.getCurrentInvestedAmount(), "Current invested amount must be non-negative.");
        validateNonNegative(result, investment.getCurrentEstimatedValue(), "Current estimated value must be non-negative.");
        validateNonNegativeNullable(result, investment.getTargetAmount(), "Target amount must be non-negative.");
        validateNonNegativeNullable(result, investment.getExpectedProfitAmount(), "Expected profit amount must be non-negative.");
        validateNonNegativeNullable(result, investment.getExpectedProfitPercent(), "Expected profit percent must be non-negative.");
        return result;
    }

    public BigDecimal getMonthlyNetAllocationsTotal(YearMonth month) {
        YearMonth targetMonth = ValidationUtils.requireNonNull(month, "month");
        List<InvestmentTransaction> monthTransactions = normalizeTransactionsForRead(
                budgetStore.listAllInvestmentTransactions(targetMonth)
        );
        InvestmentTotals totals = aggregateTotals(monthTransactions);
        return MoneyUtils.normalize(
                totals.contributions()
                        .subtract(totals.withdrawals())
                        .add(totals.adjustments())
        );
    }

    public InvestmentValidationResult validateTransaction(InvestmentTransaction tx) {
        InvestmentValidationResult result = new InvestmentValidationResult();
        if (tx == null) {
            result.addError("Transaction is required.");
            return result;
        }
        if (tx.getInvestmentId() == null || tx.getInvestmentId().isBlank()) {
            result.addError("Investment is required.");
        }
        if (tx.getMonth() == null) {
            result.addError("Month is required.");
        }
        if (tx.getTransactionDate() == null) {
            result.addError("Date is required.");
        }
        if (tx.getType() == null) {
            result.addError("Transaction type is required.");
        }
        if (tx.getAmount() == null || tx.getAmount().compareTo(BigDecimal.ZERO) == 0) {
            result.addError("Transaction amount is required.");
        }
        if (tx.getType() != null && tx.getAmount() != null) {
            int amountSign = tx.getAmount().compareTo(BigDecimal.ZERO);
            if (tx.getType() == InvestmentTransactionType.ADJUSTMENT && amountSign == 0) {
                result.addError("Adjustment amount must not be zero.");
            }
            if (tx.getType() != InvestmentTransactionType.ADJUSTMENT && amountSign <= 0) {
                result.addError("Amount must be greater than 0.");
            }
        }
        return result;
    }

    private void recordTransaction(
            String investmentId,
            BigDecimal amount,
            LocalDate date,
            InvestmentTransactionType type,
            String note
    ) {
        String targetInvestmentId = ValidationUtils.requireNonBlank(investmentId, "investmentId");
        LocalDate txDate = ValidationUtils.requireNonNull(date, "date");
        InvestmentTransactionType txType = ValidationUtils.requireNonNull(type, "type");
        BigDecimal normalizedAmount = normalizeAmountForStorage(amount, txType);
        getInvestmentOrThrow(targetInvestmentId);

        InvestmentTransaction tx = new InvestmentTransaction();
        tx.setInvestmentId(targetInvestmentId);
        tx.setTransactionDate(txDate);
        tx.setMonth(YearMonth.from(txDate));
        tx.setType(txType);
        tx.setAmount(normalizedAmount);
        tx.setNote(note);

        InvestmentValidationResult validation = validateTransaction(tx);
        if (!validation.isValid()) {
            throw new IllegalArgumentException(validation.getPrimaryError());
        }

        budgetStore.saveInvestmentTransaction(tx);
        recomputeInvestmentCachedAmounts(targetInvestmentId);
    }

    private void recomputeInvestmentCachedAmounts(String investmentId) {
        Investment investment = getInvestmentOrThrow(investmentId);
        List<InvestmentTransaction> allTransactions = listTransactions(investmentId);
        InvestmentTotals totals = aggregateTotals(allTransactions);
        BigDecimal invested = computeEffectiveInvestedAmount(totals);
        investment.setCurrentInvestedAmount(invested);
        budgetStore.saveInvestment(investment);
    }

    private TransactionLocator findTransaction(String transactionId) {
        for (Investment investment : listInvestments()) {
            for (InvestmentTransaction tx : listTransactions(investment.getId())) {
                if (tx.getId().equals(transactionId)) {
                    return new TransactionLocator(investment.getId());
                }
            }
        }
        throw new IllegalArgumentException("Investment transaction not found.");
    }

    private void ensureCanReduceInvested(String investmentId, BigDecimal reduction, String errorMessage) {
        BigDecimal invested = computeEffectiveInvestedAmount(aggregateTotals(listTransactions(investmentId)));
        if (invested.subtract(reduction).compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private Investment getInvestmentOrThrow(String investmentId) {
        return listInvestments().stream()
                .filter(item -> item.getId().equals(investmentId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Investment not found."));
    }

    private List<InvestmentTransaction> normalizeTransactionsForRead(List<InvestmentTransaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return List.of();
        }
        List<InvestmentTransaction> normalized = new ArrayList<>(transactions.size());
        for (InvestmentTransaction tx : transactions) {
            normalized.add(normalizeTransactionForRead(tx));
        }
        return List.copyOf(normalized);
    }

    private InvestmentTransaction normalizeTransactionForRead(InvestmentTransaction tx) {
        InvestmentTransaction copy = ValidationUtils.requireNonNull(tx, "tx").copy();
        if (copy.getType() == null || copy.getType() == InvestmentTransactionType.ADJUSTMENT) {
            return copy;
        }

        BigDecimal amount = MoneyUtils.zeroIfNull(copy.getAmount());
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            logLegacyNegativeAmount(copy);
            copy.setAmount(amount.abs());
            if (tx.getUpdatedAt() != null) {
                copy.setUpdatedAt(tx.getUpdatedAt());
            }
        }
        return copy;
    }

    private InvestmentTotals aggregateTotals(List<InvestmentTransaction> transactions) {
        BigDecimal contributions = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal returns = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal withdrawals = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal fees = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal adjustments = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        for (InvestmentTransaction tx : transactions == null ? List.<InvestmentTransaction>of() : transactions) {
            if (tx == null || tx.getType() == null) {
                continue;
            }
            BigDecimal amount = normalizedAmountByType(tx);
            switch (tx.getType()) {
                case CONTRIBUTION -> contributions = MoneyUtils.normalize(contributions.add(amount));
                case RETURN -> returns = MoneyUtils.normalize(returns.add(amount));
                case WITHDRAWAL -> withdrawals = MoneyUtils.normalize(withdrawals.add(amount));
                case FEE -> fees = MoneyUtils.normalize(fees.add(amount));
                case ADJUSTMENT -> adjustments = MoneyUtils.normalize(adjustments.add(amount));
            }
        }
        return new InvestmentTotals(contributions, returns, withdrawals, fees, adjustments);
    }

    private BigDecimal normalizedAmountByType(InvestmentTransaction tx) {
        BigDecimal amount = MoneyUtils.zeroIfNull(tx.getAmount());
        if (tx.getType() == InvestmentTransactionType.ADJUSTMENT) {
            return amount;
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            logLegacyNegativeAmount(tx);
            return amount.abs();
        }
        return amount;
    }

    private BigDecimal computeEffectiveInvestedAmount(InvestmentTotals totals) {
        BigDecimal invested = MoneyUtils.normalize(
                totals.contributions()
                        .subtract(totals.withdrawals())
                        .add(totals.adjustments())
        );
        if (invested.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return invested;
    }

    private void logLegacyNegativeAmount(InvestmentTransaction tx) {
        String warningKey = tx.getId() + "|" + tx.getType();
        if (!legacyNegativeAmountWarnings.add(warningKey)) {
            return;
        }
        System.err.println(
                "Normalized legacy negative investment transaction amount for txId="
                        + tx.getId()
                        + ", type="
                        + tx.getType()
                        + "."
        );
    }

    private int statusRank(Investment investment) {
        return statusRank(investment.getStatus());
    }

    private int statusRank(InvestmentPositionSummary position) {
        return statusRank(position.getStatus());
    }

    private int statusRank(InvestmentStatus status) {
        return switch (status) {
            case ACTIVE -> 0;
            case PLANNED -> 1;
            case COMPLETED -> 2;
            case CANCELLED -> 3;
        };
    }

    private String toDaysRemainingText(LocalDate expectedReturnDate) {
        if (expectedReturnDate == null) {
            return "No return date";
        }

        long days = ChronoUnit.DAYS.between(LocalDate.now(), expectedReturnDate);
        if (days < 0) {
            return "Past due by " + Math.abs(days) + " days";
        }
        if (days == 0) {
            return "Due today";
        }
        if (days == 1) {
            return "1 day remaining";
        }
        return days + " days remaining";
    }

    private void validateNonNegative(InvestmentValidationResult result, BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            result.addError(message);
        }
    }

    private void validateNonNegativeNullable(InvestmentValidationResult result, BigDecimal value, String message) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            result.addError(message);
        }
    }

    private BigDecimal normalizePositive(BigDecimal amount, String fieldName) {
        BigDecimal normalized = normalizeNonZero(amount, fieldName);
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than 0.");
        }
        return normalized;
    }

    private BigDecimal normalizeAmountForStorage(BigDecimal amount, InvestmentTransactionType type) {
        if (type == InvestmentTransactionType.ADJUSTMENT) {
            return normalizeNonZero(amount, "Adjustment amount");
        }
        return normalizePositive(amount, type.getLabel() + " amount");
    }

    private BigDecimal normalizeNonZero(BigDecimal amount, String fieldName) {
        BigDecimal normalized = MoneyUtils.normalize(ValidationUtils.requireNonNull(amount, fieldName));
        if (normalized.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException(fieldName + " must not be zero.");
        }
        return normalized;
    }

    private record InvestmentTotals(
            BigDecimal contributions,
            BigDecimal returns,
            BigDecimal withdrawals,
            BigDecimal fees,
            BigDecimal adjustments
    ) {
    }

    private record TransactionLocator(String investmentId) {
    }
}
