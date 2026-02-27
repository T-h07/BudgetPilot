package com.budgetpilot.service.goals;

import com.budgetpilot.model.Goal;
import com.budgetpilot.model.GoalContribution;
import com.budgetpilot.model.enums.GoalContributionType;
import com.budgetpilot.model.enums.GoalFundingSource;
import com.budgetpilot.service.savings.SavingsService;
import com.budgetpilot.store.BudgetStore;
import com.budgetpilot.util.MoneyUtils;
import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GoalService {
    private final BudgetStore budgetStore;
    private final SavingsService savingsService;

    public GoalService(BudgetStore budgetStore) {
        this.budgetStore = ValidationUtils.requireNonNull(budgetStore, "budgetStore");
        this.savingsService = new SavingsService(this.budgetStore);
    }

    public List<Goal> listGoals() {
        return budgetStore.listGoals().stream()
                .sorted(Comparator
                        .comparingInt(Goal::getPriority)
                        .thenComparing(Goal::isActive, Comparator.reverseOrder())
                        .thenComparing(Goal::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public void saveGoal(Goal goal) {
        GoalValidationResult validation = validateGoal(goal);
        if (!validation.isValid()) {
            throw new IllegalArgumentException(validation.getPrimaryError());
        }

        Goal copy = goal.copy();
        copy.setName(copy.getName());
        copy.setTargetAmount(copy.getTargetAmount());
        copy.setCurrentAmount(copy.getCurrentAmount());
        copy.setPriority(copy.getPriority());
        copy.setNotes(copy.getNotes());

        budgetStore.saveGoal(copy);
    }

    public void deleteGoal(String goalId) {
        String targetGoalId = ValidationUtils.requireNonBlank(goalId, "goalId");
        for (GoalContribution contribution : budgetStore.listGoalContributions(targetGoalId)) {
            budgetStore.deleteGoalContribution(contribution.getId());
        }
        budgetStore.deleteGoal(targetGoalId);
    }

    public void contribute(String goalId, BigDecimal amount, LocalDate date, String note) {
        contributeWithFunding(goalId, amount, date, note, GoalFundingSource.FREE_MONEY, null);
    }

    public void contributeWithFunding(
            String goalId,
            BigDecimal amount,
            LocalDate date,
            String note,
            GoalFundingSource sourceType,
            String sourceRefId
    ) {
        String targetGoalId = ValidationUtils.requireNonBlank(goalId, "goalId");
        LocalDate contributionDate = ValidationUtils.requireNonNull(date, "date");
        BigDecimal normalized = requirePositiveAmount(amount, "Contribution amount");
        GoalFundingSource fundingSource = sourceType == null ? GoalFundingSource.FREE_MONEY : sourceType;

        if (fundingSource == GoalFundingSource.SAVINGS_BUCKET) {
            String bucketId = ValidationUtils.requireNonBlank(sourceRefId, "sourceRefId");
            savingsService.transferToGoal(bucketId, targetGoalId, normalized, contributionDate, note);
            recordContribution(
                    targetGoalId,
                    normalized,
                    contributionDate,
                    GoalContributionType.CONTRIBUTION,
                    note,
                    GoalFundingSource.SAVINGS_BUCKET,
                    bucketId
            );
            return;
        }

        recordContribution(
                targetGoalId,
                normalized,
                contributionDate,
                GoalContributionType.CONTRIBUTION,
                note,
                fundingSource,
                sourceRefId
        );
    }

    public void contributeFromSavingsBucket(
            String goalId,
            String bucketId,
            BigDecimal amount,
            LocalDate date,
            String note
    ) {
        contributeWithFunding(goalId, amount, date, note, GoalFundingSource.SAVINGS_BUCKET, bucketId);
    }

    public void withdraw(String goalId, BigDecimal amount, LocalDate date, String note) {
        BigDecimal normalized = requirePositiveAmount(amount, "Withdrawal amount").negate();
        recordContribution(
                goalId,
                normalized,
                date,
                GoalContributionType.WITHDRAWAL,
                note,
                GoalFundingSource.FREE_MONEY,
                null
        );
    }

    public void adjust(String goalId, BigDecimal amount, LocalDate date, String note) {
        BigDecimal normalized = normalizeNonZero(amount, "Adjustment amount");
        recordContribution(
                goalId,
                normalized,
                date,
                GoalContributionType.ADJUSTMENT,
                note,
                GoalFundingSource.MANUAL,
                null
        );
    }

    public List<GoalContribution> listContributions(String goalId) {
        return budgetStore.listGoalContributions(ValidationUtils.requireNonBlank(goalId, "goalId"));
    }

    public List<GoalContribution> listContributions(String goalId, YearMonth month) {
        return budgetStore.listGoalContributions(
                ValidationUtils.requireNonBlank(goalId, "goalId"),
                ValidationUtils.requireNonNull(month, "month")
        );
    }

    public void deleteContribution(String goalId, String contributionId) {
        String targetGoalId = ValidationUtils.requireNonBlank(goalId, "goalId");
        String targetContributionId = ValidationUtils.requireNonBlank(contributionId, "contributionId");

        Goal goal = getGoalOrThrow(targetGoalId);
        GoalContribution contribution = listContributions(targetGoalId).stream()
                .filter(item -> targetContributionId.equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Goal contribution not found."));

        BigDecimal revertedAmount = MoneyUtils.normalize(goal.getCurrentAmount().subtract(contribution.getAmount()));
        if (revertedAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Cannot delete this contribution because it would create a negative goal balance.");
        }

        goal.setCurrentAmount(revertedAmount);
        budgetStore.saveGoal(goal);
        budgetStore.deleteGoalContribution(targetContributionId);
    }

    public GoalProgressSummary getGoalProgress(String goalId, YearMonth month) {
        String targetGoalId = ValidationUtils.requireNonBlank(goalId, "goalId");
        YearMonth targetMonth = ValidationUtils.requireNonNull(month, "month");

        Goal goal = getGoalOrThrow(targetGoalId);
        List<GoalContribution> monthContributions = listContributions(targetGoalId, targetMonth);

        BigDecimal monthlyContributions = sumByType(monthContributions, GoalContributionType.CONTRIBUTION);
        BigDecimal monthlyWithdrawals = sumByType(monthContributions, GoalContributionType.WITHDRAWAL).abs();
        BigDecimal monthlyNet = MoneyUtils.normalize(monthContributions.stream()
                .map(GoalContribution::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        BigDecimal remaining = MoneyUtils.safeSubtract(goal.getTargetAmount(), goal.getCurrentAmount());
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            remaining = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal progressPercent;
        if (goal.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            progressPercent = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            progressPercent = MoneyUtils.normalize(
                    goal.getCurrentAmount()
                            .multiply(MoneyUtils.HUNDRED)
                            .divide(goal.getTargetAmount(), 2, RoundingMode.HALF_UP)
            );
            if (progressPercent.compareTo(MoneyUtils.HUNDRED) > 0) {
                progressPercent = MoneyUtils.HUNDRED;
            }
        }

        return new GoalProgressSummary(
                goal.getId(),
                goal.getName(),
                goal.getGoalType(),
                goal.getCurrentAmount(),
                goal.getTargetAmount(),
                remaining,
                progressPercent,
                monthlyContributions,
                monthlyWithdrawals,
                monthlyNet,
                estimateCompletionText(goal),
                goal.getPriority(),
                goal.isActive(),
                goal.getTargetDate()
        );
    }

    public List<GoalProgressSummary> listGoalProgressSummaries(YearMonth month) {
        YearMonth targetMonth = ValidationUtils.requireNonNull(month, "month");
        return listGoals().stream()
                .map(goal -> getGoalProgress(goal.getId(), targetMonth))
                .toList();
    }

    public GoalSummary getGoalsSummary(YearMonth month) {
        YearMonth targetMonth = ValidationUtils.requireNonNull(month, "month");
        List<GoalProgressSummary> progressSummaries = listGoalProgressSummaries(targetMonth);
        List<GoalContribution> monthContributions = budgetStore.listAllGoalContributions(targetMonth);

        BigDecimal totalCurrent = MoneyUtils.normalize(progressSummaries.stream()
                .filter(GoalProgressSummary::isActive)
                .map(GoalProgressSummary::getCurrentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        BigDecimal totalTarget = MoneyUtils.normalize(progressSummaries.stream()
                .filter(GoalProgressSummary::isActive)
                .map(GoalProgressSummary::getTargetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        BigDecimal totalRemaining = MoneyUtils.safeSubtract(totalTarget, totalCurrent);
        if (totalRemaining.compareTo(BigDecimal.ZERO) < 0) {
            totalRemaining = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal monthlyContributionTotal = sumByType(monthContributions, GoalContributionType.CONTRIBUTION);
        BigDecimal monthlyWithdrawalTotal = sumByType(monthContributions, GoalContributionType.WITHDRAWAL).abs();
        BigDecimal monthlyNet = MoneyUtils.normalize(monthContributions.stream()
                .map(GoalContribution::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        int activeGoalCount = (int) progressSummaries.stream().filter(GoalProgressSummary::isActive).count();
        int completedGoalCount = (int) progressSummaries.stream()
                .filter(GoalProgressSummary::isActive)
                .filter(summary -> summary.getTargetAmount().compareTo(BigDecimal.ZERO) > 0)
                .filter(summary -> summary.getCurrentAmount().compareTo(summary.getTargetAmount()) >= 0)
                .count();

        BigDecimal progressPercent = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (totalTarget.compareTo(BigDecimal.ZERO) > 0) {
            progressPercent = MoneyUtils.normalize(
                    totalCurrent.multiply(MoneyUtils.HUNDRED).divide(totalTarget, 2, RoundingMode.HALF_UP)
            );
            if (progressPercent.compareTo(MoneyUtils.HUNDRED) > 0) {
                progressPercent = MoneyUtils.HUNDRED;
            }
        }

        return new GoalSummary(
                targetMonth,
                totalCurrent,
                totalTarget,
                totalRemaining,
                monthlyContributionTotal,
                monthlyWithdrawalTotal,
                monthlyNet,
                activeGoalCount,
                completedGoalCount,
                progressPercent
        );
    }

    public BigDecimal getTotalGoalsCurrentAmount() {
        return MoneyUtils.normalize(listGoals().stream()
                .filter(Goal::isActive)
                .map(Goal::getCurrentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public BigDecimal getTotalGoalsTargetAmount() {
        return MoneyUtils.normalize(listGoals().stream()
                .filter(Goal::isActive)
                .map(Goal::getTargetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public int getActiveGoalsCount() {
        return (int) listGoals().stream().filter(Goal::isActive).count();
    }

    public BigDecimal getMonthlyNetAllocationsTotal(YearMonth month) {
        YearMonth targetMonth = ValidationUtils.requireNonNull(month, "month");
        List<GoalContribution> entries = budgetStore.listAllGoalContributions(targetMonth);
        BigDecimal contributions = sumByType(entries, GoalContributionType.CONTRIBUTION);
        BigDecimal withdrawals = sumByType(entries, GoalContributionType.WITHDRAWAL).abs();
        return MoneyUtils.normalize(contributions.subtract(withdrawals));
    }

    public GoalValidationResult validateGoal(Goal goal) {
        GoalValidationResult result = new GoalValidationResult();
        if (goal == null) {
            result.addError("Goal is required.");
            return result;
        }
        if (goal.getName() == null || goal.getName().isBlank()) {
            result.addError("Goal name is required.");
        }
        if (goal.getGoalType() == null) {
            result.addError("Goal type is required.");
        }
        if (goal.getTargetAmount() == null || goal.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            result.addError("Target amount must be greater than 0.");
        }
        if (goal.getCurrentAmount() == null || goal.getCurrentAmount().compareTo(BigDecimal.ZERO) < 0) {
            result.addError("Current amount must be non-negative.");
        }
        if (goal.getPriority() < 1 || goal.getPriority() > 5) {
            result.addError("Priority must be between 1 and 5.");
        }
        return result;
    }

    private void recordContribution(
            String goalId,
            BigDecimal signedAmount,
            LocalDate date,
            GoalContributionType contributionType,
            String note,
            GoalFundingSource sourceType,
            String sourceRefId
    ) {
        String targetGoalId = ValidationUtils.requireNonBlank(goalId, "goalId");
        LocalDate contributionDate = ValidationUtils.requireNonNull(date, "date");
        BigDecimal normalizedAmount = normalizeNonZero(signedAmount, "amount");
        GoalFundingSource fundingSource = sourceType == null ? GoalFundingSource.FREE_MONEY : sourceType;

        Goal goal = getGoalOrThrow(targetGoalId);
        BigDecimal updatedAmount = MoneyUtils.normalize(goal.getCurrentAmount().add(normalizedAmount));
        if (updatedAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Cannot withdraw more than current goal balance.");
        }

        GoalContribution contribution = new GoalContribution();
        contribution.setGoalId(targetGoalId);
        contribution.setMonth(YearMonth.from(contributionDate));
        contribution.setContributionDate(contributionDate);
        contribution.setAmount(normalizedAmount);
        contribution.setType(ValidationUtils.requireNonNull(contributionType, "contributionType"));
        contribution.setSourceType(fundingSource);
        contribution.setSourceRefId(sourceRefId);
        contribution.setNote(note);

        budgetStore.saveGoalContribution(contribution);

        goal.setCurrentAmount(updatedAmount);
        budgetStore.saveGoal(goal);
    }

    private Goal getGoalOrThrow(String goalId) {
        return listGoals().stream()
                .filter(goal -> goal.getId().equals(goalId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Goal not found."));
    }

    private BigDecimal sumByType(List<GoalContribution> contributions, GoalContributionType type) {
        BigDecimal total = contributions.stream()
                .filter(contribution -> contribution.getType() == type)
                .map(GoalContribution::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (type == GoalContributionType.WITHDRAWAL) {
            total = total.abs();
        }
        return MoneyUtils.normalize(total);
    }

    private String estimateCompletionText(Goal goal) {
        if (goal.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return "No estimate yet";
        }

        BigDecimal remaining = MoneyUtils.safeSubtract(goal.getTargetAmount(), goal.getCurrentAmount());
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            return "Completed";
        }

        List<GoalContribution> contributions = listContributions(goal.getId());
        if (contributions.isEmpty()) {
            return "No estimate yet";
        }

        Map<YearMonth, BigDecimal> monthlyTotals = contributions.stream()
                .collect(Collectors.groupingBy(
                        GoalContribution::getMonth,
                        Collectors.mapping(GoalContribution::getAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

        List<BigDecimal> positiveMonths = monthlyTotals.values().stream()
                .map(MoneyUtils::normalize)
                .filter(total -> total.compareTo(BigDecimal.ZERO) > 0)
                .toList();

        if (positiveMonths.isEmpty()) {
            return "No estimate yet";
        }

        BigDecimal averageMonthlyContribution = positiveMonths.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(positiveMonths.size()), 2, RoundingMode.HALF_UP);

        if (averageMonthlyContribution.compareTo(BigDecimal.ZERO) <= 0) {
            return "No estimate yet";
        }

        long monthsRemaining = (long) Math.ceil(
                remaining.divide(averageMonthlyContribution, 4, RoundingMode.HALF_UP).doubleValue()
        );

        if (monthsRemaining <= 1) {
            return "Est. < 1 month";
        }
        return "Est. " + monthsRemaining + " months";
    }

    private BigDecimal requirePositiveAmount(BigDecimal amount, String fieldName) {
        BigDecimal normalized = normalizeNonZero(amount, fieldName);
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            return normalized.abs();
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
}
