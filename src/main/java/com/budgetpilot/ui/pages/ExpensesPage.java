package com.budgetpilot.ui.pages;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.ui.components.SectionCard;
import com.budgetpilot.util.UiUtils;
import javafx.scene.layout.VBox;

public class ExpensesPage extends VBox {
    public ExpensesPage(AppContext appContext) {
        setSpacing(UiUtils.SECTION_SPACING);
        setPadding(UiUtils.PAGE_PADDING);
        getStyleClass().add("page-root");

        String monthText = UiUtils.formatMonth(appContext.getCurrentMonth());
        getChildren().add(UiUtils.createPageHeader(
                "Expenses",
                "Track and manage monthly expenses for " + monthText + " with category-level clarity."
        ));

        SectionCard transactionFeed = new SectionCard(
                "Transaction Feed",
                "Capture purchases, recurring charges, and cash expenses in one timeline.",
                UiUtils.createInfoLines(
                        "Smart categorization and receipt linking will be added in BP-PT2.",
                        "Filtering by account, category, and tags is planned for this module."
                )
        );

        SectionCard categoryHealth = new SectionCard(
                "Category Health",
                "Watch budget pressure by category before overspending occurs.",
                UiUtils.createInfoLines(
                        "Burn-rate indicators and anomaly highlights will surface here.",
                        "BudgetPilot will show warning thresholds by weekly pace."
                )
        );

        getChildren().add(UiUtils.createTwoColumn(transactionFeed, categoryHealth));
    }
}
