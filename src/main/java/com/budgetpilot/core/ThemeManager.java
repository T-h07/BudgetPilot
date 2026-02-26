package com.budgetpilot.core;

import javafx.scene.Scene;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ThemeManager {
    private static final String DARK_THEME_PATH = "/css/theme/dark.css";
    private static final String LIGHT_THEME_PATH = "/css/theme/light.css";

    private ThemeManager() {
    }

    public static void apply(Scene scene, Theme theme) {
        if (scene == null) {
            return;
        }
        Theme normalized = theme == null ? Theme.DARK : theme;
        String targetThemeUrl = resolveThemeUrl(normalized);
        String darkThemeUrl = resolveThemeUrl(Theme.DARK);
        String lightThemeUrl = resolveThemeUrl(Theme.LIGHT);

        List<String> stylesheets = new ArrayList<>(scene.getStylesheets());
        stylesheets.removeIf(url -> Objects.equals(url, darkThemeUrl) || Objects.equals(url, lightThemeUrl));
        stylesheets.add(0, targetThemeUrl);
        scene.getStylesheets().setAll(stylesheets);
    }

    private static String resolveThemeUrl(Theme theme) {
        String resourcePath = theme == Theme.LIGHT ? LIGHT_THEME_PATH : DARK_THEME_PATH;
        var resource = ThemeManager.class.getResource(resourcePath);
        if (resource == null) {
            throw new IllegalStateException("Missing theme stylesheet: " + resourcePath);
        }
        return resource.toExternalForm();
    }
}
