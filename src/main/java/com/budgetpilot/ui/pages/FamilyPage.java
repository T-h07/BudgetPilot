package com.budgetpilot.ui.pages;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.ui.components.SectionCard;
import com.budgetpilot.util.UiUtils;
import javafx.scene.layout.VBox;

public class FamilyPage extends VBox {
    public FamilyPage(AppContext appContext) {
        setSpacing(UiUtils.SECTION_SPACING);
        setPadding(UiUtils.PAGE_PADDING);
        getStyleClass().add("page-root");

        getChildren().add(UiUtils.createPageHeader(
                "Family",
                "Manage dependent costs, allowances, and shared monthly obligations."
        ));

        SectionCard dependents = new SectionCard(
                "Dependents & Allowances",
                "Track recurring support, child expenses, and discretionary family spending.",
                UiUtils.createInfoLines(
                        "Allowance schedules and dependent profiles will be configurable in BP-PT2.",
                        "Shared planning for household responsibilities is in scope for BP-PT3."
                )
        );

        SectionCard careCosts = new SectionCard(
                "Medical & Care Costs",
                "Group non-routine family costs into forecastable categories.",
                UiUtils.createInfoLines(
                        "Coverage tracking and recurring care reminders are part of future development."
                )
        );

        getChildren().add(UiUtils.createTwoColumn(dependents, careCosts));
    }
}
