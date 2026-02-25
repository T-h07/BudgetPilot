package com.budgetpilot.ui.pages;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.ui.components.KpiTile;
import com.budgetpilot.ui.components.SectionCard;
import com.budgetpilot.util.UiUtils;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class DashboardPage extends VBox {
    public DashboardPage(AppContext appContext) {
        setSpacing(UiUtils.SECTION_SPACING);
        setPadding(UiUtils.PAGE_PADDING);
        getStyleClass().add("page-root");

        String monthText = UiUtils.formatMonth(appContext.getCurrentMonth());
        getChildren().add(UiUtils.createPageHeader(
                "Dashboard",
                "Live cockpit for " + monthText + ". Plan, track, and forecast every dollar before month-end."
        ));

        GridPane kpiGrid = new GridPane();
        kpiGrid.getStyleClass().add("kpi-grid");
        kpiGrid.setHgap(UiUtils.CARD_GAP);
        kpiGrid.setVgap(UiUtils.CARD_GAP);

        kpiGrid.add(new KpiTile("Money Remaining", "$2,480", "62% of planned free cash still available"), 0, 0);
        kpiGrid.add(new KpiTile("Spent This Month", "$1,620", "Essentials are within healthy range"), 1, 0);
        kpiGrid.add(new KpiTile("Savings", "$740", "+8% contribution pace vs last month"), 2, 0);
        kpiGrid.add(new KpiTile("Goals Progress", "41%", "2 of 5 goals are currently on schedule"), 3, 0);

        for (int i = 0; i < 4; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(25);
            column.setHgrow(Priority.ALWAYS);
            kpiGrid.getColumnConstraints().add(column);
        }

        SectionCard spendingOverview = new SectionCard(
                "Spending Overview",
                "High-level snapshot of category pressure and monthly burn.",
                createSummaryRows(new String[][]{
                        {"Essentials", "$1,040 / $1,500"},
                        {"Lifestyle", "$360 / $700"},
                        {"Transport", "$220 / $300"},
                        {"Subscriptions", "$74 / $110"}
                })
        );

        SectionCard plannerSummary = new SectionCard(
                "Planner Summary",
                "Plan adherence and reallocation room for the remaining weeks.",
                createSummaryRows(new String[][]{
                        {"Planned Income", "$4,100"},
                        {"Planned Bills", "$1,950"},
                        {"Unallocated Balance", "$510"},
                        {"Forecast Buffer", "$290"}
                })
        );

        SectionCard alertsInsights = new SectionCard(
                "Alerts & Insights",
                "System-generated guidance and upcoming pressure points.",
                UiUtils.createInfoLines(
                        "Dining category is trending 14% above your pacing target.",
                        "Rent, utilities, and internet are all posted and reconciled.",
                        "Savings goal \"Emergency Fund\" can be completed 9 days earlier at current pace."
                )
        );

        HBox summaryRow = UiUtils.createTwoColumn(spendingOverview, plannerSummary);
        getChildren().addAll(kpiGrid, summaryRow, alertsInsights);
    }

    private Node createSummaryRows(String[][] rows) {
        VBox container = new VBox(10);
        for (String[] row : rows) {
            HBox line = new HBox();
            line.setAlignment(Pos.CENTER_LEFT);

            Label label = UiUtils.createMutedLabel(row[0]);
            Label value = new Label(row[1]);
            value.getStyleClass().add("info-row-value");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            line.getChildren().addAll(label, spacer, value);
            container.getChildren().add(line);
        }
        return container;
    }
}
