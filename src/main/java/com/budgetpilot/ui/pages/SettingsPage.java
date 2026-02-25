package com.budgetpilot.ui.pages;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.ui.components.SectionCard;
import com.budgetpilot.util.UiUtils;
import javafx.scene.layout.VBox;

public class SettingsPage extends VBox {
    public SettingsPage(AppContext appContext) {
        setSpacing(UiUtils.SECTION_SPACING);
        setPadding(UiUtils.PAGE_PADDING);
        getStyleClass().add("page-root");

        getChildren().add(UiUtils.createPageHeader(
                "Settings",
                "Configure profile preferences, defaults, and app behavior."
        ));

        SectionCard profilePreferences = new SectionCard(
                "Profile & Preferences",
                "Manage identity, locale, and default monthly planning options.",
                UiUtils.createInfoLines(
                        "Currency, date format, and account preferences will be editable soon.",
                        "Notification channels and alert sensitivity settings are planned."
                )
        );

        SectionCard workspaceOptions = new SectionCard(
                "Workspace Options",
                "Tune dashboard density, reminders, and module visibility.",
                UiUtils.createInfoLines(
                        "Theme variants and personalization controls are planned for BP-PT3."
                )
        );

        getChildren().add(UiUtils.createTwoColumn(profilePreferences, workspaceOptions));
    }
}
