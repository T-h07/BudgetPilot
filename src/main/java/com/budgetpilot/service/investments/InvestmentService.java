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
import java.util.List;
import java.util.Objects;

public class InvestmentService {
    private final BudgetStore budgetStore;

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
        return budgetStore.listInvestmentTransactions(ValidationUtils.requireNonBlank(investmentId, "investmentId"));
    }

    public List<InvestmentTransaction> listTransactions(String investmentId, YearMonth month) {
        return budgetStore.listInvestmentTransactions(
                ValidationUtils.requireNonBlank(investmentId, "investmentId"),
                ValidationUtils.requireNonNull(month, "month")
        );
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
        ensureCanReduceInvested(investmentId, normalized, "Fee would reduce invested amount below zero.");
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

            BigDecimal contributions = sumByType(allTransactions, InvestmentTransactionType.CONTRIBUTION);
            BigDecimal returns = sumByType(allTransactions, InvestmentTransactionType.RETURN);
            BigDecimal withdrawals = sumByType(allTransactions, InvestmentTransactionType.WITHDRAWAL);
            BigDecimal fees = sumByType(allTransactions, InvestmentTransactionType.FEE);
            BigDecimal adjustments = sumByType(allTransactions, InvestmentTransactionType.ADJUSTMENT);

            BigDecimal investedAmount = MoneyUtils.normalize(contributions
                    .add(adjustments)
                    .subtract(withdrawals)
                    .subtract(fees));
            if (investedAmount.compareTo(BigDecimal.ZERO) < 0) {
                investedAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }

            BigDecimal estimatedValue = MoneyUtils.zeroIfNull(investment.getCurrentEstimatedValue());
            if (estimatedValue.compareTo(BigDecimal.ZERO) == 0 && !allTransactions.isEmpty()) {
                estimatedValue = MoneyUtils.normalize(investedAmount.add(returns));
            }

            BigDecimal netProfit = MoneyUtils.safeSubtract(estimatedValue, investedAmount);
            BigDecimal roiPercent = investedAmount.compareTo(BigDecimal.ZERO) <= 0
                    ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                    : MoneyUtils.normalize(netProfit.multiply(MoneyUtils.HUNDRED)
                    .divide(investedAmount, 2, RoundingMode.HALF_UP));

            BigDecimal progressPercent = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            if (investment.getTargetAmount() != null && investment.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
                progressPercent = MoneyUtils.normalize(investedAmount
                        .multiply(MoneyUtils.HUNDRED)
                        .divide(investment.getTargetAmount(), 2, RoundingMode.HALF_UP));
                if (progressPercent.compareTo(MoneyUtils.HUNDRED) > 0) {
                    progressPercent = MoneyUtils.HUNDRED;
                }
            }

            BigDecimal monthlyContribution = sumByType(monthTransactions, InvestmentTransactionType.CONTRIBUTION);
            BigDecimal monthlyReturn = sumByType(monthTransactions, InvestmentTransactionType.RETURN);

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

        return new InvestmentSummary(
                targetMonth,
                investment,
                position,
                sumByType(monthTransactions, InvestmentTransactionType.CONTRIBUTION),
                sumByType(monthTransactions, InvestmentTransactionType.RETURN),
                sumByType(monthTransactions, InvestmentTransactionType.FEE),
                sumByType(monthTransactions, InvestmentTransactionType.WITHDRAWAL),
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
        if (tx.getType() != null && tx.getType() != InvestmentTransactionType.ADJUSTMENT
                && tx.getAmount() != null && tx.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            result.addError("Amount must be greater than 0.");
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
        getInvestmentOrThrow(targetInvestmentId);

        InvestmentTransaction tx = new InvestmentTransaction();
        tx.setInvestmentId(targetInvestmentId);
        tx.setTransactionDate(txDate);
        tx.setMonth(YearMonth.from(txDate));
        tx.setType(ValidationUtils.requireNonNull(type, "type"));
        tx.setAmount(amount);
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

        BigDecimal contributions = sumByType(allTransactions, InvestmentTransactionType.CONTRIBUTION);
        BigDecimal returns = sumByType(allTransactions, InvestmentTransactionType.RETURN);
        BigDecimal withdrawals = sumByType(allTransactions, InvestmentTransactionType.WITHDRAWAL);
        BigDecimal fees = sumByType(allTransactions, InvestmentTransactionType.FEE);
        BigDecimal adjustments = sumByType(allTransactions, InvestmentTransactionType.ADJUSTMENT);

        BigDecimal invested = MoneyUtils.normalize(contributions.add(adjustments).subtract(withdrawals).subtract(fees));
        if (invested.compareTo(BigDecimal.ZERO) < 0) {
            invested = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal estimated = MoneyUtils.normalize(invested.add(returns));
        investment.setCurrentInvestedAmount(invested);
        investment.setCurrentEstimatedValue(estimated);
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
        Investment investment = getInvestmentOrThrow(investmentId);
        BigDecimal invested = MoneyUtils.zeroIfNull(investment.getCurrentInvestedAmount());
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

    private BigDecimal sumByType(List<InvestmentTransaction> transactions, InvestmentTransactionType type) {
        return MoneyUtils.normalize(transactions.stream()
                .filter(tx -> tx.getType() == type)
                .map(InvestmentTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
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
            normalized = normalized.abs();
        }
        return normalized;
    }

    private BigDecimal normalizeNonZero(BigDecimal amount, String fieldName) {
        BigDecimal normalized = MoneyUtils.normalize(ValidationUtils.requireNonNull(amount, fieldName));
        if (normalized.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException(fieldName + " must not be zero.");
        }
        return normalized;
    }

    private record TransactionLocator(String investmentId) {
    }
}
