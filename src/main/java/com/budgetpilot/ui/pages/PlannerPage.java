package com.budgetpilot.ui.pages;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.ui.components.SectionCard;
import com.budgetpilot.util.UiUtils;
import javafx.scene.layout.VBox;

public class PlannerPage extends VBox {
    public PlannerPage(AppContext appContext) {
        setSpacing(UiUtils.SECTION_SPACING);
        setPadding(UiUtils.PAGE_PADDING);
        getStyleClass().add("page-root");

        getChildren().add(UiUtils.createPageHeader(
                "Planner",
                "Plan your monthly budget and weekly allocations before spending begins."
        ));

        SectionCard monthlyBlueprint = new SectionCard(
                "Monthly Blueprint",
                "Define income, fixed bills, essentials, and discretionary envelopes.",
                UiUtils.createInfoLines(
                        "Rule-based allocations will auto-balance plan buckets in BP-PT2.",
                        "Planner history and month-to-month carry-over is part of BP-PT3."
                )
        );

        SectionCard weeklyAllocations = new SectionCard(
                "Weekly Allocations",
                "Translate monthly intent into weekly control points.",
                UiUtils.createInfoLines(
                        "Week pacing widgets and overrun recovery suggestions are planned.",
                        "Future versions will include optional payday-based budget splits."
                )
        );

        getChildren().add(UiUtils.createTwoColumn(monthlyBlueprint, weeklyAllocations));
    }
}
