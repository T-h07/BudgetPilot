package com.budgetpilot.service;

import com.budgetpilot.model.SavingsBucket;
import com.budgetpilot.model.SavingsEntry;
import com.budgetpilot.model.enums.SavingsEntryType;
import com.budgetpilot.store.BudgetStore;
import com.budgetpilot.util.MoneyUtils;
import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

public class SavingsService {
    private final BudgetStore budgetStore;

    public SavingsService(BudgetStore budgetStore) {
        this.budgetStore = ValidationUtils.requireNonNull(budgetStore, "budgetStore");
    }

    public List<SavingsBucket> listBuckets() {
        return budgetStore.listSavingsBuckets().stream()
                .sorted(Comparator.comparing(SavingsBucket::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public void saveBucket(SavingsBucket bucket) {
        SavingsValidationResult validation = validateBucket(bucket);
        if (!validation.isValid()) {
            throw new IllegalArgumentException(validation.getPrimaryError());
        }

        SavingsBucket copy = bucket.copy();
        copy.setName(copy.getName());
        copy.setTargetAmount(copy.getTargetAmount());
        copy.setNotes(copy.getNotes());
        budgetStore.saveSavingsBucket(copy);
    }

    public void deleteBucket(String bucketId) {
        String targetBucketId = ValidationUtils.requireNonBlank(bucketId, "bucketId");
        for (SavingsEntry entry : budgetStore.listSavingsEntries(targetBucketId)) {
            budgetStore.deleteSavingsEntry(entry.getId());
        }
        budgetStore.deleteSavingsBucket(targetBucketId);
    }

    public void addContribution(String bucketId, BigDecimal amount, LocalDate date, String note) {
        BigDecimal normalizedAmount = requirePositiveAmount(amount, "Contribution amount");
        recordEntry(bucketId, normalizedAmount, date, SavingsEntryType.CONTRIBUTION, note);
    }

    public void withdraw(String bucketId, BigDecimal amount, LocalDate date, String note) {
        BigDecimal normalizedAmount = requirePositiveAmount(amount, "Withdrawal amount").negate();
        recordEntry(bucketId, normalizedAmount, date, SavingsEntryType.WITHDRAWAL, note);
    }

    public void addAdjustment(String bucketId, BigDecimal amount, LocalDate date, String note) {
        BigDecimal normalizedAmount = normalizeNonZero(amount, "Adjustment amount");
        recordEntry(bucketId, normalizedAmount, date, SavingsEntryType.ADJUSTMENT, note);
    }

    public List<SavingsEntry> listBucketEntries(String bucketId) {
        return budgetStore.listSavingsEntries(ValidationUtils.requireNonBlank(bucketId, "bucketId"));
    }

    public List<SavingsEntry> listBucketEntries(String bucketId, YearMonth month) {
        return budgetStore.listSavingsEntries(
                ValidationUtils.requireNonBlank(bucketId, "bucketId"),
                ValidationUtils.requireNonNull(month, "month")
        );
    }

    public void deleteEntry(String bucketId, String entryId) {
        String targetBucketId = ValidationUtils.requireNonBlank(bucketId, "bucketId");
        String targetEntryId = ValidationUtils.requireNonBlank(entryId, "entryId");

        SavingsBucket bucket = getBucketOrThrow(targetBucketId);
        SavingsEntry entry = listBucketEntries(targetBucketId).stream()
                .filter(item -> targetEntryId.equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Savings entry not found."));

        BigDecimal revertedBalance = MoneyUtils.normalize(bucket.getCurrentAmount().subtract(entry.getAmount()));
        if (revertedBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Cannot delete this entry because it would create a negative bucket balance.");
        }

        bucket.setCurrentAmount(revertedBalance);
        budgetStore.saveSavingsBucket(bucket);
        budgetStore.deleteSavingsEntry(targetEntryId);
    }

    public SavingsSummary getSavingsSummary(YearMonth month) {
        YearMonth targetMonth = ValidationUtils.requireNonNull(month, "month");
        List<SavingsBucket> buckets = listBuckets();
        List<SavingsEntry> monthEntries = budgetStore.listAllSavingsEntries(targetMonth);

        BigDecimal totalCurrent = getTotalCurrentSavings();
        BigDecimal monthlyContributions = getMonthlyContributionsTotal(targetMonth);
        BigDecimal monthlyWithdrawals = getMonthlyWithdrawalsTotal(targetMonth);
        BigDecimal monthlyNet = MoneyUtils.normalize(monthEntries.stream()
                .map(SavingsEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        int activeBucketCount = (int) buckets.stream().filter(SavingsBucket::isActive).count();
        int bucketCount = buckets.size();

        BigDecimal totalTargetAmount = MoneyUtils.normalize(buckets.stream()
                .filter(SavingsBucket::isActive)
                .map(SavingsBucket::getTargetAmount)
                .filter(target -> target != null && target.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        BigDecimal targetCoveragePercent = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (totalTargetAmount.compareTo(BigDecimal.ZERO) > 0) {
            targetCoveragePercent = MoneyUtils.normalize(
                    totalCurrent.multiply(MoneyUtils.HUNDRED).divide(totalTargetAmount, 2, RoundingMode.HALF_UP)
            );
            if (targetCoveragePercent.compareTo(MoneyUtils.HUNDRED) > 0) {
                targetCoveragePercent = MoneyUtils.HUNDRED;
            }
        }

        return new SavingsSummary(
                targetMonth,
                totalCurrent,
                monthlyContributions,
                monthlyWithdrawals,
                monthlyNet,
                activeBucketCount,
                bucketCount,
                totalTargetAmount,
                targetCoveragePercent
        );
    }

    public SavingsBucketSummary getBucketSummary(String bucketId, YearMonth month) {
        String targetBucketId = ValidationUtils.requireNonBlank(bucketId, "bucketId");
        YearMonth targetMonth = ValidationUtils.requireNonNull(month, "month");

        SavingsBucket bucket = getBucketOrThrow(targetBucketId);
        List<SavingsEntry> monthEntries = listBucketEntries(targetBucketId, targetMonth);

        BigDecimal monthlyContributions = sumByType(monthEntries, SavingsEntryType.CONTRIBUTION);
        BigDecimal monthlyWithdrawals = sumByType(monthEntries, SavingsEntryType.WITHDRAWAL).abs();
        BigDecimal monthlyNet = MoneyUtils.normalize(monthEntries.stream()
                .map(SavingsEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        BigDecimal progressPercent = calculateProgressPercent(bucket.getCurrentAmount(), bucket.getTargetAmount());

        return new SavingsBucketSummary(
                bucket.getId(),
                bucket.getName(),
                bucket.getCurrentAmount(),
                bucket.getTargetAmount(),
                progressPercent,
                monthlyContributions,
                monthlyWithdrawals,
                monthlyNet,
                monthEntries.size(),
                bucket.isActive()
        );
    }

    public BigDecimal getTotalCurrentSavings() {
        return MoneyUtils.normalize(listBuckets().stream()
                .filter(SavingsBucket::isActive)
                .map(SavingsBucket::getCurrentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public BigDecimal getMonthlyContributionsTotal(YearMonth month) {
        return sumByType(
                budgetStore.listAllSavingsEntries(ValidationUtils.requireNonNull(month, "month")),
                SavingsEntryType.CONTRIBUTION
        );
    }

    public BigDecimal getMonthlyWithdrawalsTotal(YearMonth month) {
        return sumByType(
                budgetStore.listAllSavingsEntries(ValidationUtils.requireNonNull(month, "month")),
                SavingsEntryType.WITHDRAWAL
        ).abs();
    }

    public int getActiveBucketCount() {
        return (int) listBuckets().stream().filter(SavingsBucket::isActive).count();
    }

    public SavingsValidationResult validateBucket(SavingsBucket bucket) {
        SavingsValidationResult result = new SavingsValidationResult();
        if (bucket == null) {
            result.addError("Savings bucket is required.");
            return result;
        }
        if (bucket.getName() == null || bucket.getName().isBlank()) {
            result.addError("Bucket name is required.");
        }
        if (bucket.getTargetAmount() != null && bucket.getTargetAmount().compareTo(BigDecimal.ZERO) < 0) {
            result.addError("Target amount must be non-negative.");
        }
        if (bucket.getCurrentAmount() == null || bucket.getCurrentAmount().compareTo(BigDecimal.ZERO) < 0) {
            result.addError("Current amount must be non-negative.");
        }
        return result;
    }

    private void recordEntry(String bucketId, BigDecimal signedAmount, LocalDate date, SavingsEntryType entryType, String note) {
        String targetBucketId = ValidationUtils.requireNonBlank(bucketId, "bucketId");
        LocalDate entryDate = ValidationUtils.requireNonNull(date, "date");
        BigDecimal normalizedAmount = normalizeNonZero(signedAmount, "amount");

        SavingsBucket bucket = getBucketOrThrow(targetBucketId);
        BigDecimal updatedBalance = MoneyUtils.normalize(bucket.getCurrentAmount().add(normalizedAmount));
        if (updatedBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Cannot withdraw more than current savings balance.");
        }

        SavingsEntry entry = new SavingsEntry();
        entry.setBucketId(targetBucketId);
        entry.setMonth(YearMonth.from(entryDate));
        entry.setEntryDate(entryDate);
        entry.setAmount(normalizedAmount);
        entry.setEntryType(ValidationUtils.requireNonNull(entryType, "entryType"));
        entry.setNote(note);

        budgetStore.saveSavingsEntry(entry);

        bucket.setCurrentAmount(updatedBalance);
        budgetStore.saveSavingsBucket(bucket);
    }

    private SavingsBucket getBucketOrThrow(String bucketId) {
        return listBuckets().stream()
                .filter(bucket -> bucket.getId().equals(bucketId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Savings bucket not found."));
    }

    private BigDecimal sumByType(List<SavingsEntry> entries, SavingsEntryType type) {
        BigDecimal total = entries.stream()
                .filter(entry -> entry.getEntryType() == type)
                .map(SavingsEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (type == SavingsEntryType.WITHDRAWAL) {
            total = total.abs();
        }
        return MoneyUtils.normalize(total);
    }

    private BigDecimal calculateProgressPercent(BigDecimal currentAmount, BigDecimal targetAmount) {
        BigDecimal current = MoneyUtils.zeroIfNull(currentAmount);
        BigDecimal target = MoneyUtils.zeroIfNull(targetAmount);
        if (target.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal progress = MoneyUtils.normalize(
                current.multiply(MoneyUtils.HUNDRED).divide(target, 2, RoundingMode.HALF_UP)
        );
        return progress.compareTo(MoneyUtils.HUNDRED) > 0 ? MoneyUtils.HUNDRED : progress;
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
