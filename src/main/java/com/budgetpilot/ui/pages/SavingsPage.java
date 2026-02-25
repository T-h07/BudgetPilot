package com.budgetpilot.ui.pages;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.ui.components.SectionCard;
import com.budgetpilot.util.UiUtils;
import javafx.scene.layout.VBox;

public class SavingsPage extends VBox {
    public SavingsPage(AppContext appContext) {
        setSpacing(UiUtils.SECTION_SPACING);
        setPadding(UiUtils.PAGE_PADDING);
        getStyleClass().add("page-root");

        getChildren().add(UiUtils.createPageHeader(
                "Savings",
                "Manage savings buckets and contributions with long-term visibility."
        ));

        SectionCard buckets = new SectionCard(
                "Savings Buckets",
                "Organize emergency, lifestyle, and reserve funds by purpose.",
                UiUtils.createInfoLines(
                        "Bucket rules and transfer scheduling will be introduced in BP-PT2.",
                        "Progress forecasting will show target completion windows."
                )
        );

        SectionCard contributionPlan = new SectionCard(
                "Contribution Plan",
                "Track planned vs actual savings flow across the month.",
                UiUtils.createInfoLines(
                        "Auto-allocation by payday and income events is queued for upcoming phases."
                )
        );

        getChildren().add(UiUtils.createTwoColumn(buckets, contributionPlan));
    }
}
