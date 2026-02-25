package com.budgetpilot.ui.pages;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.ui.components.SectionCard;
import com.budgetpilot.util.UiUtils;
import javafx.scene.layout.VBox;

public class InvestmentsPage extends VBox {
    public InvestmentsPage(AppContext appContext) {
        setSpacing(UiUtils.SECTION_SPACING);
        setPadding(UiUtils.PAGE_PADDING);
        getStyleClass().add("page-root");

        getChildren().add(UiUtils.createPageHeader(
                "Investments",
                "Review portfolio allocation and monthly investing contributions."
        ));

        SectionCard portfolioSnapshot = new SectionCard(
                "Portfolio Snapshot",
                "Track allocation mix and monthly contribution momentum.",
                UiUtils.createInfoLines(
                        "Asset-class breakdown cards and trend visuals will be introduced next.",
                        "Broker account syncing and import support is reserved for future phases."
                )
        );

        SectionCard riskGuide = new SectionCard(
                "Risk & Strategy",
                "Set target allocation and monitor drift over time.",
                UiUtils.createInfoLines(
                        "Portfolio drift alerts and rebalance prompts are planned.",
                        "Long-term strategy templates will be optional in advanced mode."
                )
        );

        getChildren().add(UiUtils.createTwoColumn(portfolioSnapshot, riskGuide));
    }
}
