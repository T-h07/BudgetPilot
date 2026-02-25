package com.budgetpilot.ui.pages;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.ui.components.SectionCard;
import com.budgetpilot.util.UiUtils;
import javafx.scene.layout.VBox;

public class GoalsPage extends VBox {
    public GoalsPage(AppContext appContext) {
        setSpacing(UiUtils.SECTION_SPACING);
        setPadding(UiUtils.PAGE_PADDING);
        getStyleClass().add("page-root");

        getChildren().add(UiUtils.createPageHeader(
                "Goals",
                "Fund short and long-term goals with structured monthly contributions."
        ));

        SectionCard goalsRoadmap = new SectionCard(
                "Goals Roadmap",
                "Track every target by due date, amount, and progress pace.",
                UiUtils.createInfoLines(
                        "Multi-goal prioritization and trade-off recommendations are planned.",
                        "Goal templates for travel, debt payoff, and major purchases will be added."
                )
        );

        SectionCard fundingSchedule = new SectionCard(
                "Funding Schedule",
                "View projected completion dates under current monthly contributions.",
                UiUtils.createInfoLines(
                        "What-if modeling and scenario comparison will be included in BP-PT3."
                )
        );

        getChildren().add(UiUtils.createTwoColumn(goalsRoadmap, fundingSchedule));
    }
}
