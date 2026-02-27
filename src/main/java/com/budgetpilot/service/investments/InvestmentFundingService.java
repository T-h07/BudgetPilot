package com.budgetpilot.service.investments;

import com.budgetpilot.model.Investment;
import com.budgetpilot.model.SavingsBucket;
import com.budgetpilot.model.SavingsEntry;
import com.budgetpilot.model.enums.FundingSourceType;
import com.budgetpilot.service.balance.MonthlyBalanceService;
import com.budgetpilot.service.balance.MonthlyBalanceSnapshot;
import com.budgetpilot.service.savings.SavingsService;
import com.budgetpilot.store.BudgetStore;
import com.budgetpilot.util.MoneyUtils;
import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class InvestmentFundingService {
    private final InvestmentService investmentService;
    private final SavingsService savingsService;
    private final MonthlyBalanceService monthlyBalanceService;

    public InvestmentFundingService(BudgetStore budgetStore) {
        BudgetStore store = ValidationUtils.requireNonNull(budgetStore, "budgetStore");
        this.investmentService = new InvestmentService(store);
        this.savingsService = new SavingsService(store);
        this.monthlyBalanceService = new MonthlyBalanceService(store);
    }

    public MonthlyBalanceSnapshot previewFreeMoneyContributionImpact(FundInvestmentRequest request) {
        FundInvestmentRequest req = ValidationUtils.requireNonNull(request, "request");
        if (req.getSourceType() != FundingSourceType.FREE_MONEY) {
            return monthlyBalanceService.buildSnapshot(YearMonth.from(req.getDate()));
        }
        return monthlyBalanceService.buildProjectedSnapshot(
                YearMonth.from(req.getDate()),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                req.getAmount()
        );
    }

    public void addContribution(FundInvestmentRequest request) {
        FundInvestmentRequest req = ValidationUtils.requireNonNull(request, "request");
        Investment investment = getInvestmentOrThrow(req.getInvestmentId());

        if (req.getSourceType() == FundingSourceType.SAVINGS_BUCKET) {
            addContributionFromSavings(req, investment);
            return;
        }

        investmentService.addContribution(
                req.getInvestmentId(),
                req.getAmount(),
                req.getDate(),
                req.getNote()
        );
    }

    private void addContributionFromSavings(FundInvestmentRequest request, Investment investment) {
        String bucketId = ValidationUtils.requireNonBlank(request.getSourceRefId(), "sourceRefId");
        SavingsBucket bucket = getSavingsBucketOrThrow(bucketId);
        ensureSufficientSavingsBalance(bucket, request.getAmount());

        Set<String> existingEntryIds = savingsService.listBucketEntries(bucketId).stream()
                .map(SavingsEntry::getId)
                .collect(Collectors.toSet());

        String transferNote = buildTransferNote(investment.getName(), request.getNote());
        savingsService.withdraw(bucketId, request.getAmount(), request.getDate(), transferNote);

        try {
            investmentService.addContribution(
                    request.getInvestmentId(),
                    request.getAmount(),
                    request.getDate(),
                    request.getNote()
            );
        } catch (RuntimeException ex) {
            rollbackSavingsTransfer(bucketId, existingEntryIds);
            throw ex;
        }
    }

    private void rollbackSavingsTransfer(String bucketId, Set<String> existingEntryIds) {
        List<SavingsEntry> newEntries = savingsService.listBucketEntries(bucketId).stream()
                .filter(entry -> !existingEntryIds.contains(entry.getId()))
                .toList();
        for (SavingsEntry entry : newEntries) {
            try {
                savingsService.deleteEntry(bucketId, entry.getId());
            } catch (RuntimeException rollbackEx) {
                System.err.println("Failed rolling back savings transfer entry " + entry.getId() + ": " + rollbackEx.getMessage());
            }
        }
    }

    private Investment getInvestmentOrThrow(String investmentId) {
        String targetId = ValidationUtils.requireNonBlank(investmentId, "investmentId");
        return investmentService.listInvestments().stream()
                .filter(investment -> targetId.equals(investment.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Investment not found."));
    }

    private SavingsBucket getSavingsBucketOrThrow(String bucketId) {
        String targetId = ValidationUtils.requireNonBlank(bucketId, "bucketId");
        SavingsBucket bucket = savingsService.listBuckets().stream()
                .filter(entry -> targetId.equals(entry.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Savings bucket not found."));
        if (!bucket.isActive()) {
            throw new IllegalArgumentException("Selected savings bucket is inactive.");
        }
        return bucket;
    }

    private void ensureSufficientSavingsBalance(SavingsBucket bucket, BigDecimal amount) {
        BigDecimal current = MoneyUtils.zeroIfNull(bucket.getCurrentAmount());
        if (current.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient savings balance.");
        }
    }

    private String buildTransferNote(String investmentName, String userNote) {
        String targetName = investmentName == null || investmentName.isBlank() ? "Investment" : investmentName.trim();
        if (userNote == null || userNote.isBlank()) {
            return "Investment funding: " + targetName;
        }
        return "Investment funding: " + targetName + " | " + userNote.trim();
    }
}
