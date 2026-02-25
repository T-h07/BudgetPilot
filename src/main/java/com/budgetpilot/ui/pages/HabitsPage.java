package com.budgetpilot.ui.pages;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.ui.components.SectionCard;
import com.budgetpilot.util.UiUtils;
import javafx.scene.layout.VBox;

public class HabitsPage extends VBox {
    public HabitsPage(AppContext appContext) {
        setSpacing(UiUtils.SECTION_SPACING);
        setPadding(UiUtils.PAGE_PADDING);
        getStyleClass().add("page-root");

        getChildren().add(UiUtils.createPageHeader(
                "Habits",
                "Build better spending behavior with actionable control loops."
        ));

        SectionCard behaviorSignals = new SectionCard(
                "Behavior Signals",
                "Identify repeating triggers behind avoidable expenses.",
                UiUtils.createInfoLines(
                        "Pattern detection and behavior tagging are planned in BP-PT2.",
                        "The app will surface high-impact habit opportunities by category."
                )
        );

        SectionCard guardrails = new SectionCard(
                "Guardrails",
                "Set personal limits and cooling-off rules for discretionary purchases.",
                UiUtils.createInfoLines(
                        "Optional lock rules and reminder nudges are on the next roadmap slice."
                )
        );

        getChildren().add(UiUtils.createTwoColumn(behaviorSignals, guardrails));
    }
}
