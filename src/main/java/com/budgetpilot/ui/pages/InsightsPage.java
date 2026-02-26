package com.budgetpilot.ui.pages;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.service.insights.InsightItem;
import com.budgetpilot.service.insights.InsightLevel;
import com.budgetpilot.service.insights.InsightsService;
import com.budgetpilot.ui.components.DataEmptyState;
import com.budgetpilot.ui.components.SectionCard;
import com.budgetpilot.ui.components.StatusBadge;
import com.budgetpilot.util.MonthUtils;
import com.budgetpilot.util.UiUtils;
import com.budgetpilot.util.ValidationUtils;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.YearMonth;
import java.util.List;
import java.util.Locale;

public class InsightsPage extends VBox {
    private static final String ALL_SOURCES = "__all_sources__";

    private final AppContext appContext;
    private final InsightsService insightsService;
    private final Runnable contextListener = this::refreshAll;

    private final Label monthContextLabel = new Label();
    private final ComboBox<InsightLevel> levelFilter = new ComboBox<>();
    private final ComboBox<String> sourceFilter = new ComboBox<>();
    private final Button clearFiltersButton = new Button("Clear Filters");
    private final VBox insightsList = new VBox(10);

    private List<InsightItem> allInsights = List.of();

    public InsightsPage(AppContext appContext) {
        this.appContext = ValidationUtils.requireNonNull(appContext, "appContext");
        this.insightsService = new InsightsService(ValidationUtils.requireNonNull(appContext.getStore(), "store"));

        setSpacing(UiUtils.SECTION_SPACING);
        setPadding(UiUtils.PAGE_PADDING);
        getStyleClass().addAll("page-root", "page-insights");

        getChildren().add(UiUtils.createPageHeader(
                "Insights Center",
                "Prioritized, month-aware actions from your planner, spending, habits, forecast, and recurring templates."
        ));

        monthContextLabel.getStyleClass().addAll("muted-text", "insights-month-context");
        setupFilters();

        HBox filterBar = new HBox(10, levelFilter, sourceFilter, clearFiltersButton);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.getStyleClass().add("insights-filter-bar");

        SectionCard listCard = new SectionCard(
                "Notifications",
                "Most urgent items appear first. Use filters to focus by severity or source.",
                insightsList
        );
        listCard.getStyleClass().add("insights-list-card");

        getChildren().addAll(monthContextLabel, filterBar, listCard);

        appContext.addChangeListener(contextListener);
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                appContext.removeChangeListener(contextListener);
            }
        });

        refreshAll();
    }

    private void setupFilters() {
        levelFilter.getStyleClass().addAll("combo-box", "form-combo");
        levelFilter.getItems().setAll((InsightLevel) null);
        levelFilter.getItems().addAll(InsightLevel.values());
        levelFilter.getSelectionModel().selectFirst();
        levelFilter.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(InsightLevel item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item == null ? "All Levels" : formatLevel(item));
            }
        });
        levelFilter.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(InsightLevel item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item == null ? "All Levels" : formatLevel(item));
            }
        });
        levelFilter.setOnAction(event -> applyFiltersAndRender());

        sourceFilter.getStyleClass().addAll("combo-box", "form-combo");
        sourceFilter.getItems().setAll(ALL_SOURCES);
        sourceFilter.getSelectionModel().selectFirst();
        sourceFilter.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(ALL_SOURCES.equals(item) ? "All Sources" : formatSource(item));
            }
        });
        sourceFilter.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(ALL_SOURCES.equals(item) ? "All Sources" : formatSource(item));
            }
        });
        sourceFilter.setOnAction(event -> applyFiltersAndRender());

        clearFiltersButton.getStyleClass().addAll("secondary-button", "btn-secondary");
        clearFiltersButton.setOnAction(event -> {
            levelFilter.getSelectionModel().selectFirst();
            sourceFilter.getSelectionModel().selectFirst();
            applyFiltersAndRender();
        });
    }

    private void refreshAll() {
        YearMonth month = appContext.getSelectedMonth();
        allInsights = insightsService.buildInsights(month);
        monthContextLabel.setText("Insights for " + MonthUtils.format(month));
        refreshSourceOptions();
        applyFiltersAndRender();
    }

    private void refreshSourceOptions() {
        String selected = sourceFilter.getValue();
        List<String> availableSources = allInsights.stream()
                .map(InsightItem::getSource)
                .distinct()
                .sorted()
                .toList();

        sourceFilter.getItems().setAll(ALL_SOURCES);
        sourceFilter.getItems().addAll(availableSources);
        if (selected != null && sourceFilter.getItems().contains(selected)) {
            sourceFilter.getSelectionModel().select(selected);
        } else {
            sourceFilter.getSelectionModel().selectFirst();
        }
    }

    private void applyFiltersAndRender() {
        InsightLevel selectedLevel = levelFilter.getValue();
        String selectedSource = sourceFilter.getValue();
        YearMonth month = appContext.getSelectedMonth();

        List<InsightItem> filtered = allInsights.stream()
                .filter(item -> selectedLevel == null || item.getLevel() == selectedLevel)
                .filter(item -> selectedSource == null
                        || ALL_SOURCES.equals(selectedSource)
                        || item.getSource().equalsIgnoreCase(selectedSource))
                .toList();

        insightsList.getChildren().clear();
        if (filtered.isEmpty()) {
            insightsList.getChildren().add(new DataEmptyState(
                    "No critical insights. You're on track.",
                    "No insights match current filters for " + MonthUtils.format(month) + "."
            ));
            return;
        }

        for (InsightItem insight : filtered) {
            insightsList.getChildren().add(createInsightCard(insight));
        }
    }

    private Node createInsightCard(InsightItem insight) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("insight-card", "insight-card-" + insight.getLevel().name().toLowerCase(Locale.ROOT));

        Region levelStripe = new Region();
        levelStripe.getStyleClass().addAll("insight-level-stripe", "insight-level-" + insight.getLevel().name().toLowerCase(Locale.ROOT));
        levelStripe.setMinWidth(4);
        levelStripe.setPrefWidth(4);

        Label titleLabel = new Label(insight.getTitle());
        titleLabel.getStyleClass().add("card-title");

        Label sourceLabel = new Label(formatSource(insight.getSource()));
        sourceLabel.getStyleClass().addAll("chip", "insight-source-chip");

        VBox titleGroup = new VBox(6, titleLabel, sourceLabel);

        StatusBadge badge = new StatusBadge();
        badge.setText(formatLevel(insight.getLevel()));
        badge.setStatus(statusFromLevel(insight.getLevel()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox head = new HBox(10, levelStripe, titleGroup, spacer, badge);
        head.setAlignment(Pos.CENTER_LEFT);

        Label messageLabel = UiUtils.createMutedLabel(insight.getMessage());
        messageLabel.setWrapText(true);

        card.getChildren().addAll(head, messageLabel);

        if (insight.getActionTarget() != null && !insight.getActionLabel().isBlank()) {
            Button actionButton = new Button(insight.getActionLabel());
            actionButton.getStyleClass().addAll("secondary-button", "btn-secondary", "btn-small");
            actionButton.setOnAction(event -> appContext.navigate(insight.getActionTarget()));
            card.getChildren().add(actionButton);
        }

        return card;
    }

    private String formatLevel(InsightLevel level) {
        return switch (level) {
            case DANGER -> "Danger";
            case WARNING -> "Warning";
            case INFO -> "Info";
            case SUCCESS -> "Success";
        };
    }

    private String statusFromLevel(InsightLevel level) {
        return switch (level) {
            case DANGER -> "danger";
            case WARNING -> "warn";
            case INFO -> "info";
            case SUCCESS -> "good";
        };
    }

    private String formatSource(String source) {
        if (source == null || source.isBlank()) {
            return "General";
        }
        String normalized = source.trim().toLowerCase(Locale.ROOT);
        return normalized.substring(0, 1).toUpperCase(Locale.ROOT) + normalized.substring(1);
    }
}
