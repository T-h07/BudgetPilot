package com.budgetpilot.ui.pages;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.ui.components.SectionCard;
import com.budgetpilot.util.UiUtils;
import javafx.scene.layout.VBox;

public class AchievementsPage extends VBox {
    public AchievementsPage(AppContext appContext) {
        setSpacing(UiUtils.SECTION_SPACING);
        setPadding(UiUtils.PAGE_PADDING);
        getStyleClass().add("page-root");

        getChildren().add(UiUtils.createPageHeader(
                "Achievements",
                "Reward consistency, budgeting discipline, and long-term financial momentum."
        ));

        SectionCard streaks = new SectionCard(
                "Consistency Streaks",
                "Celebrate consecutive months of on-plan spending and savings discipline.",
                UiUtils.createInfoLines(
                        "Badge logic and score milestones will be enabled in BP-PT2.",
                        "Streak recovery guidance is planned for high-volatility months."
                )
        );

        SectionCard milestones = new SectionCard(
                "Milestones",
                "Track unlocked financial habits and progression tiers.",
                UiUtils.createInfoLines(
                        "Achievement sharing and custom challenge packs are planned for future phases."
                )
        );

        getChildren().add(UiUtils.createTwoColumn(streaks, milestones));
    }
}
