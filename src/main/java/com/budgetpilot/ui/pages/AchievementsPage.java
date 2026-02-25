package com.budgetpilot.ui.pages;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.model.enums.AchievementCategory;
import com.budgetpilot.model.enums.AchievementStatus;
import com.budgetpilot.model.enums.AchievementTier;
import com.budgetpilot.service.AchievementInsight;
import com.budgetpilot.service.AchievementPageSummary;
import com.budgetpilot.service.AchievementProgress;
import com.budgetpilot.service.AchievementService;
import com.budgetpilot.service.InsightLevel;
import com.budgetpilot.ui.components.DataEmptyState;
import com.budgetpilot.ui.components.MetricRow;
import com.budgetpilot.ui.components.SectionCard;
import com.budgetpilot.ui.components.SummaryStatCard;
import com.budgetpilot.util.UiUtils;
import com.budgetpilot.util.ValidationUtils;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class AchievementsPage extends VBox {
    private final AppContext appContext;
    private final AchievementService achievementService;
    private final Runnable contextListener = this::refreshAll;

    private final SummaryStatCard unlockedCard = new SummaryStatCard();
    private final SummaryStatCard inProgressCard = new SummaryStatCard();
    private final SummaryStatCard completionCard = new SummaryStatCard();
    private final SummaryStatCard goldCard = new SummaryStatCard();
    private final SummaryStatCard closestCard = new SummaryStatCard();

    private final ComboBox<AchievementCategory> categoryFilter = new ComboBox<>();
    private final ComboBox<AchievementStatus> statusFilter = new ComboBox<>();
    private final TextField searchField = new TextField();
    private final Button clearFiltersButton = new Button("Clear Filters");

    private final VBox achievementListBox = new VBox(10);
    private final VBox tierBreakdownBox = new VBox(8);
    private final VBox closestUnlocksBox = new VBox(8);
    private final VBox insightsBox = new VBox(8);

    private List<AchievementProgress> allAchievements = List.of();
    private List<AchievementInsight> insights = List.of();

    public AchievementsPage(AppContext appContext) {
        this.appContext = ValidationUtils.requireNonNull(appContext, "appContext");
        this.achievementService = new AchievementService(ValidationUtils.requireNonNull(appContext.getStore(), "store"));

        setSpacing(UiUtils.SECTION_SPACING);
        setPadding(UiUtils.PAGE_PADDING);
        getStyleClass().addAll("page-root", "page-achievements");

        getChildren().add(UiUtils.createPageHeader(
                "Achievements",
                "Unlock milestones, monitor progress tiers, and stay motivated with actionable achievement insights."
        ));

        setupFilters();

        HBox summaryRow = buildSummaryRow();
        HBox filterBar = buildFilterBar();
        HBox mainRow = buildMainRow();

        getChildren().addAll(summaryRow, filterBar, mainRow);

        appContext.addChangeListener(contextListener);
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                appContext.removeChangeListener(contextListener);
            }
        });

        refreshAll();
    }

    private void setupFilters() {
        searchField.setPromptText("Search achievements...");
        searchField.getStyleClass().addAll("text-input", "form-input");
        clearFiltersButton.getStyleClass().addAll("secondary-button", "btn-secondary");

        configureCategoryFilter();
        configureStatusFilter();

        clearFiltersButton.setOnAction(event -> {
            categoryFilter.getSelectionModel().selectFirst();
            statusFilter.getSelectionModel().selectFirst();
            searchField.clear();
            applyFiltersAndRender();
        });

        categoryFilter.setOnAction(event -> applyFiltersAndRender());
        statusFilter.setOnAction(event -> applyFiltersAndRender());
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFiltersAndRender());
    }

    private HBox buildSummaryRow() {
        HBox row = new HBox(UiUtils.CARD_GAP, unlockedCard, inProgressCard, completionCard, goldCard, closestCard);
        row.getStyleClass().add("achievements-summary-grid");
        for (Node node : row.getChildren()) {
            if (node instanceof Region region) {
                region.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(region, Priority.ALWAYS);
            }
        }
        return row;
    }

    private HBox buildFilterBar() {
        HBox bar = new HBox(10, categoryFilter, statusFilter, searchField, clearFiltersButton);
        bar.getStyleClass().add("achievements-filter-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        return bar;
    }

    private HBox buildMainRow() {
        SectionCard achievementsCard = new SectionCard(
                "Achievement Board",
                "Track locked, in-progress, and unlocked milestones across all modules.",
                achievementListBox
        );

        VBox sideBody = new VBox(
                12,
                sectionHeader("Tier Breakdown"),
                tierBreakdownBox,
                sectionHeader("Closest Unlocks"),
                closestUnlocksBox,
                sectionHeader("Insights"),
                insightsBox
        );
        SectionCard sideCard = new SectionCard(
                "Progress Insights",
                "Completion summary and next best actions.",
                sideBody
        );

        HBox row = new HBox(UiUtils.CARD_GAP, achievementsCard, sideCard);
        HBox.setHgrow(achievementsCard, Priority.ALWAYS);
        HBox.setHgrow(sideCard, Priority.ALWAYS);
        achievementsCard.setMaxWidth(Double.MAX_VALUE);
        sideCard.setMaxWidth(Double.MAX_VALUE);
        return row;
    }
    private void refreshAll() {
        AchievementPageSummary summary = achievementService.getAchievementPageSummary(appContext.getSelectedMonth());
        allAchievements = summary.getAchievements();
        insights = achievementService.getInsights(appContext.getSelectedMonth());

        updateSummaryCards(summary);
        updateTierBreakdown(summary);
        updateClosestUnlocks();
        updateInsights();
        applyFiltersAndRender();
    }

    private void updateSummaryCards(AchievementPageSummary summary) {
        int closestUnlockCount = (int) allAchievements.stream()
                .filter(item -> item.getStatus() == AchievementStatus.IN_PROGRESS)
                .filter(item -> item.getProgressPercent().doubleValue() >= 75)
                .count();

        unlockedCard.setValues(
                "Unlocked",
                String.valueOf(summary.getUnlockedCount()),
                summary.getTotalAchievements() + " total achievements"
        );
        inProgressCard.setValues(
                "In Progress",
                String.valueOf(summary.getInProgressCount()),
                summary.getLockedCount() + " still locked"
        );
        completionCard.setValues(
                "Completion",
                summary.getCompletionPercent().stripTrailingZeros().toPlainString() + "%",
                appContext.getCurrentMonthDisplayText()
        );
        goldCard.setValues(
                "Gold Unlocked",
                String.valueOf(summary.getGoldUnlocked()),
                "Silver " + summary.getSilverUnlocked() + " | Bronze " + summary.getBronzeUnlocked()
        );
        closestCard.setValues(
                "Closest Unlocks",
                String.valueOf(closestUnlockCount),
                "Achievements above 75%"
        );
    }

    private void applyFiltersAndRender() {
        List<AchievementProgress> filtered = allAchievements.stream()
                .filter(this::matchesCategory)
                .filter(this::matchesStatus)
                .filter(this::matchesSearch)
                .sorted(Comparator
                        .comparingInt((AchievementProgress item) -> statusRank(item.getStatus()))
                        .thenComparing(AchievementProgress::getProgressPercent, Comparator.reverseOrder())
                        .thenComparing(AchievementProgress::getTitle))
                .toList();

        achievementListBox.getChildren().clear();
        if (filtered.isEmpty()) {
            achievementListBox.getChildren().add(new DataEmptyState(
                    "No achievements match filters",
                    "Try changing category, status, or search filters."
            ));
            return;
        }

        for (AchievementProgress progress : filtered) {
            achievementListBox.getChildren().add(createAchievementCard(progress));
        }
    }

    private Node createAchievementCard(AchievementProgress progress) {
        VBox card = new VBox(8);
        card.getStyleClass().add("achievement-card");
        if (progress.getStatus() == AchievementStatus.UNLOCKED) {
            card.getStyleClass().add("achievement-card-unlocked");
        }

        Label title = new Label(progress.getTitle());
        title.getStyleClass().add("card-title");

        Label description = new Label(progress.getDescription());
        description.getStyleClass().add("muted-text");
        description.setWrapText(true);

        HBox badges = new HBox(8,
                categoryBadge(progress.getCategory()),
                tierBadge(progress.getTier()),
                statusBadge(progress.getStatus())
        );

        ProgressBar progressBar = new ProgressBar(clamp(progress.getProgressPercent().doubleValue() / 100.0));
        progressBar.getStyleClass().add("achievement-card-progress");
        progressBar.setMaxWidth(Double.MAX_VALUE);

        Label progressText = new Label(progress.getProgressText());
        progressText.getStyleClass().add("muted-text");

        card.getChildren().addAll(title, description, badges, progressBar, progressText);

        if (progress.getStatus() != AchievementStatus.UNLOCKED && !progress.getUnlockHint().isBlank()) {
            Label hint = new Label("Hint: " + progress.getUnlockHint());
            hint.getStyleClass().add("muted-text");
            hint.setWrapText(true);
            card.getChildren().add(hint);
        }

        return card;
    }

    private void updateTierBreakdown(AchievementPageSummary summary) {
        tierBreakdownBox.getChildren().setAll(
                new MetricRow("Total Achievements", String.valueOf(summary.getTotalAchievements())),
                new MetricRow("Bronze Unlocked", String.valueOf(summary.getBronzeUnlocked())),
                new MetricRow("Silver Unlocked", String.valueOf(summary.getSilverUnlocked())),
                new MetricRow("Gold Unlocked", String.valueOf(summary.getGoldUnlocked())),
                new MetricRow("Completion", summary.getCompletionPercent().stripTrailingZeros().toPlainString() + "%")
        );
    }

    private void updateClosestUnlocks() {
        closestUnlocksBox.getChildren().clear();

        List<AchievementProgress> closest = allAchievements.stream()
                .filter(item -> item.getStatus() == AchievementStatus.IN_PROGRESS)
                .sorted(Comparator.comparing(AchievementProgress::getProgressPercent, Comparator.reverseOrder()))
                .limit(3)
                .toList();

        if (closest.isEmpty()) {
            closestUnlocksBox.getChildren().add(UiUtils.createMutedLabel("No in-progress achievements yet."));
            return;
        }

        for (AchievementProgress progress : closest) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("alert-item");
            row.getChildren().addAll(
                    UiUtils.createMutedLabel(progress.getTitle()),
                    spacer(),
                    new Label(progress.getProgressPercent().stripTrailingZeros().toPlainString() + "%")
            );
            ((Label) row.getChildren().get(2)).getStyleClass().add("info-row-value");
            closestUnlocksBox.getChildren().add(row);
        }
    }

    private void updateInsights() {
        insightsBox.getChildren().clear();
        if (insights.isEmpty()) {
            insightsBox.getChildren().add(UiUtils.createMutedLabel("No achievement insights available."));
            return;
        }

        for (AchievementInsight insight : insights) {
            VBox row = new VBox(4);
            row.getStyleClass().add("alert-item");

            Label title = new Label(insight.getTitle());
            title.getStyleClass().add("card-title");
            title.getStyleClass().add(levelClass(insight.getLevel()));

            Label message = UiUtils.createMutedLabel(insight.getMessage());
            message.setWrapText(true);
            row.getChildren().addAll(title, message);

            if (!insight.getActionHint().isBlank()) {
                Label action = UiUtils.createMutedLabel("Action: " + insight.getActionHint());
                action.setWrapText(true);
                row.getChildren().add(action);
            }

            insightsBox.getChildren().add(row);
        }
    }
    private void configureCategoryFilter() {
        categoryFilter.getItems().setAll((AchievementCategory) null);
        categoryFilter.getItems().addAll(AchievementCategory.values());
        categoryFilter.getSelectionModel().selectFirst();
        categoryFilter.getStyleClass().addAll("combo-box", "form-combo");
        categoryFilter.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(AchievementCategory item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item == null ? "All Categories" : item.getLabel());
            }
        });
        categoryFilter.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(AchievementCategory item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item == null ? "All Categories" : item.getLabel());
            }
        });
    }

    private void configureStatusFilter() {
        statusFilter.getItems().setAll((AchievementStatus) null);
        statusFilter.getItems().addAll(AchievementStatus.values());
        statusFilter.getSelectionModel().selectFirst();
        statusFilter.getStyleClass().addAll("combo-box", "form-combo");
        statusFilter.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(AchievementStatus item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item == null ? "All Statuses" : item.getLabel());
            }
        });
        statusFilter.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(AchievementStatus item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item == null ? "All Statuses" : item.getLabel());
            }
        });
    }

    private boolean matchesCategory(AchievementProgress progress) {
        AchievementCategory selected = categoryFilter.getValue();
        return selected == null || progress.getCategory() == selected;
    }

    private boolean matchesStatus(AchievementProgress progress) {
        AchievementStatus selected = statusFilter.getValue();
        return selected == null || progress.getStatus() == selected;
    }

    private boolean matchesSearch(AchievementProgress progress) {
        String search = searchField.getText();
        if (search == null || search.isBlank()) {
            return true;
        }
        String normalized = search.trim().toLowerCase(Locale.ROOT);
        return progress.getTitle().toLowerCase(Locale.ROOT).contains(normalized)
                || progress.getDescription().toLowerCase(Locale.ROOT).contains(normalized);
    }

    private Label categoryBadge(AchievementCategory category) {
        Label label = new Label(category.getLabel());
        label.getStyleClass().addAll("chip", "achievement-category-badge");
        return label;
    }

    private Label tierBadge(AchievementTier tier) {
        Label label = new Label(tier.getLabel());
        label.getStyleClass().add("chip");
        switch (tier) {
            case BRONZE -> label.getStyleClass().add("achievement-tier-bronze");
            case SILVER -> label.getStyleClass().add("achievement-tier-silver");
            case GOLD -> label.getStyleClass().add("achievement-tier-gold");
        }
        return label;
    }

    private Label statusBadge(AchievementStatus status) {
        Label label = new Label(status.getLabel());
        label.getStyleClass().add("chip");
        switch (status) {
            case LOCKED -> label.getStyleClass().add("achievement-status-locked");
            case IN_PROGRESS -> label.getStyleClass().add("achievement-status-progress");
            case UNLOCKED -> label.getStyleClass().add("achievement-status-unlocked");
        }
        return label;
    }

    private int statusRank(AchievementStatus status) {
        return switch (status) {
            case IN_PROGRESS -> 0;
            case UNLOCKED -> 1;
            case LOCKED -> 2;
        };
    }

    private String levelClass(InsightLevel level) {
        return switch (level) {
            case INFO -> "alert-info";
            case WARN -> "alert-warning";
            case SUCCESS -> "alert-success";
        };
    }

    private Label sectionHeader(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("form-label");
        return label;
    }

    private Region spacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }
}
