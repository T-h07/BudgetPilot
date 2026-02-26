package com.budgetpilot.ui;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.core.PageId;
import com.budgetpilot.util.MonthUtils;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class TopBar extends HBox {
    private final Label pageTitleLabel = new Label();
    private final ComboBox<YearMonth> monthSelector = new ComboBox<>();
    private boolean updatingMonthSelector;

    public TopBar(AppContext appContext) {
        Objects.requireNonNull(appContext, "appContext must not be null");

        setSpacing(16);
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(16, 24, 16, 24));
        getStyleClass().add("topbar");

        pageTitleLabel.getStyleClass().add("topbar-page-title");

        Label appTagline = new Label("Monthly financial operating system");
        appTagline.getStyleClass().add("muted-text");

        Label persistenceWarning = new Label();
        persistenceWarning.getStyleClass().addAll("banner-warning", "topbar-persistence-warning");
        persistenceWarning.textProperty().bind(
                Bindings.createStringBinding(
                        () -> {
                            if (appContext.isPersistenceAvailable()) {
                                return "";
                            }
                            return appContext.getPersistenceStatus() == null
                                    ? ""
                                    : appContext.getPersistenceStatus().getMessage();
                        },
                        appContext.persistenceStatusProperty()
                )
        );
        persistenceWarning.visibleProperty().bind(
                Bindings.createBooleanBinding(
                        () -> !appContext.isPersistenceAvailable(),
                        appContext.persistenceStatusProperty()
                )
        );
        persistenceWarning.managedProperty().bind(persistenceWarning.visibleProperty());

        VBox leftSection = new VBox(4, pageTitleLabel, appTagline, persistenceWarning);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button prevMonthButton = createMonthNavButton("topbar-arrow-left");
        prevMonthButton.setFocusTraversable(false);
        prevMonthButton.setOnAction(event -> appContext.setSelectedMonth(appContext.getSelectedMonth().minusMonths(1)));

        monthSelector.getStyleClass().addAll("combo-box", "form-combo", "topbar-month-selector");
        monthSelector.setVisibleRowCount(12);
        monthSelector.setPrefWidth(190);
        monthSelector.setCellFactory(listView -> createMonthCell());
        monthSelector.setButtonCell(createMonthCell());
        refreshMonthSelectorOptions(appContext.getSelectedMonth());
        monthSelector.getSelectionModel().select(appContext.getSelectedMonth());
        monthSelector.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (updatingMonthSelector || newValue == null) {
                return;
            }
            appContext.setSelectedMonth(newValue);
        });

        Button nextMonthButton = createMonthNavButton("topbar-arrow-right");
        nextMonthButton.setFocusTraversable(false);
        nextMonthButton.setOnAction(event -> {
            YearMonth selected = appContext.getSelectedMonth();
            YearMonth now = MonthUtils.currentMonth();
            if (selected != null && selected.isBefore(now)) {
                appContext.setSelectedMonth(selected.plusMonths(1));
            }
        });

        BooleanBinding canNavigateForward = Bindings.createBooleanBinding(
                () -> {
                    YearMonth selected = appContext.getSelectedMonth();
                    return selected != null && selected.isBefore(MonthUtils.currentMonth());
                },
                appContext.selectedMonthProperty()
        );
        nextMonthButton.disableProperty().bind(canNavigateForward.not());

        Button currentMonthButton = new Button("Current");
        currentMonthButton.getStyleClass().addAll("secondary-button", "btn-secondary");
        currentMonthButton.setFocusTraversable(false);
        currentMonthButton.setOnAction(event -> appContext.setSelectedMonth(MonthUtils.currentMonth()));

        Label historyBadge = new Label();
        historyBadge.getStyleClass().addAll("month-pill", "topbar-history-badge");
        historyBadge.textProperty().bind(Bindings.createStringBinding(
                () -> "Viewing: " + appContext.getCurrentMonthDisplayText() + " (History)",
                appContext.selectedMonthProperty()
        ));
        historyBadge.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> appContext.getSelectedMonth() != null
                        && appContext.getSelectedMonth().isBefore(MonthUtils.currentMonth()),
                appContext.selectedMonthProperty()
        ));
        historyBadge.managedProperty().bind(historyBadge.visibleProperty());

        appContext.selectedMonthProperty().addListener((obs, oldMonth, newMonth) -> {
            if (newMonth == null) {
                return;
            }
            refreshMonthSelectorOptions(newMonth);
            updatingMonthSelector = true;
            monthSelector.getSelectionModel().select(newMonth);
            updatingMonthSelector = false;
        });

        Label profileInitials = new Label();
        profileInitials.getStyleClass().add("profile-initials");
        profileInitials.textProperty().bind(
                Bindings.createStringBinding(
                        appContext::getCurrentUserInitials,
                        appContext.currentUserProperty()
                )
        );

        StackPane profileBadge = new StackPane(profileInitials);
        profileBadge.getStyleClass().add("profile-badge");
        profileBadge.setPrefSize(36, 36);

        getChildren().addAll(
                leftSection,
                spacer,
                prevMonthButton,
                monthSelector,
                nextMonthButton,
                currentMonthButton,
                historyBadge,
                profileBadge
        );
    }

    public void setActivePage(PageId pageId) {
        if (pageId != null) {
            pageTitleLabel.setText(pageId.getDisplayLabel());
        }
    }

    private void refreshMonthSelectorOptions(YearMonth selectedMonth) {
        List<YearMonth> options = buildMonthOptions(selectedMonth);
        updatingMonthSelector = true;
        monthSelector.getItems().setAll(options);
        if (selectedMonth != null) {
            monthSelector.getSelectionModel().select(selectedMonth);
        }
        updatingMonthSelector = false;
    }

    private List<YearMonth> buildMonthOptions(YearMonth selectedMonth) {
        YearMonth now = MonthUtils.currentMonth();
        List<YearMonth> months = new ArrayList<>();
        for (int i = 0; i <= 24; i++) {
            months.add(now.minusMonths(i));
        }
        if (selectedMonth != null && !months.contains(selectedMonth)) {
            months.add(selectedMonth);
        }
        months.sort(Comparator.reverseOrder());
        return months;
    }

    private ListCell<YearMonth> createMonthCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(YearMonth item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : MonthUtils.format(item));
            }
        };
    }

    private Button createMonthNavButton(String arrowClass) {
        Button button = new Button();
        button.getStyleClass().addAll("secondary-button", "btn-secondary", "topbar-month-nav-btn");

        Region arrow = new Region();
        arrow.getStyleClass().addAll("topbar-nav-arrow", arrowClass);
        arrow.setMinSize(12, 12);
        arrow.setPrefSize(12, 12);
        arrow.setMaxSize(12, 12);

        button.setGraphic(new StackPane(arrow));
        button.setText("");
        return button;
    }
}
