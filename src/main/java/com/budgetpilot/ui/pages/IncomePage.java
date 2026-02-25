package com.budgetpilot.ui.pages;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.ui.components.SectionCard;
import com.budgetpilot.util.UiUtils;
import javafx.scene.layout.VBox;

public class IncomePage extends VBox {
    public IncomePage(AppContext appContext) {
        setSpacing(UiUtils.SECTION_SPACING);
        setPadding(UiUtils.PAGE_PADDING);
        getStyleClass().add("page-root");

        getChildren().add(UiUtils.createPageHeader(
                "Income",
                "Monitor income streams and payout timing to stabilize monthly planning."
        ));

        SectionCard incomeStreams = new SectionCard(
                "Income Streams",
                "Organize salary, freelance, and side-income sources.",
                UiUtils.createInfoLines(
                        "Recurring pay templates and variability tracking are planned for BP-PT2.",
                        "Net vs gross handling and tax reserve helpers will be added later."
                )
        );

        SectionCard payoutCalendar = new SectionCard(
                "Payout Calendar",
                "Map expected deposits against budget obligations.",
                UiUtils.createInfoLines(
                        "Calendar-aware forecast balancing and cash gap warnings are upcoming."
                )
        );

        getChildren().add(UiUtils.createTwoColumn(incomeStreams, payoutCalendar));
    }
}
