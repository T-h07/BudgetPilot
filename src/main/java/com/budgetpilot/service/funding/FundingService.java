package com.budgetpilot.service.funding;

import com.budgetpilot.model.enums.GoalFundingSource;
import com.budgetpilot.service.goals.GoalService;
import com.budgetpilot.store.BudgetStore;
import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FundingService {
    private final GoalService goalService;

    public FundingService(BudgetStore budgetStore) {
        this.goalService = new GoalService(ValidationUtils.requireNonNull(budgetStore, "budgetStore"));
    }

    public void fundGoal(
            String goalId,
            BigDecimal amount,
            LocalDate date,
            String note,
            GoalFundingSource sourceType,
            String sourceRefId
    ) {
        GoalFundingSource safeSource = sourceType == null ? GoalFundingSource.FREE_MONEY : sourceType;
        String effectiveRefId = safeSource == GoalFundingSource.SAVINGS_BUCKET ? sourceRefId : null;
        goalService.contributeWithFunding(goalId, amount, date, note, safeSource, effectiveRefId);
    }
}
