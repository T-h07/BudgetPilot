package com.budgetpilot.ui.pages;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.ui.components.SectionCard;
import com.budgetpilot.util.UiUtils;
import javafx.scene.layout.VBox;

public class OnboardingPage extends VBox {
    public OnboardingPage(AppContext appContext) {
        setSpacing(UiUtils.SECTION_SPACING);
        setPadding(UiUtils.PAGE_PADDING);
        getStyleClass().add("page-root");

        getChildren().add(UiUtils.createPageHeader(
                "Onboarding",
                "Guide new users through setup of monthly budgeting and financial goals."
        ));

        SectionCard setupFlow = new SectionCard(
                "Setup Flow",
                "Future onboarding will collect income, recurring bills, and baseline savings goals.",
                UiUtils.createInfoLines(
                        "Step 1: Personal profile and budgeting style.",
                        "Step 2: Income and obligations.",
                        "Step 3: Savings and goals configuration.",
                        "Step 4: Optional family and habit modules."
                )
        );

        SectionCard completionChecklist = new SectionCard(
                "Completion Checklist",
                "Track setup quality before users enter the live dashboard.",
                UiUtils.createInfoLines(
                        "Checklist scoring and setup recommendations are planned for BP-PT2."
                )
        );

        getChildren().add(UiUtils.createTwoColumn(setupFlow, completionChecklist));
    }
}
