package com.budgetpilot.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AppPaths {
    private static final String APP_DIR_NAME = ".budgetpilot";
    private static final String DB_FILE_NAME = "budgetpilot.db";

    private AppPaths() {
    }

    public static Path getAppDataDir() {
        Path dir = Path.of(System.getProperty("user.home"), APP_DIR_NAME);
        ensureDirectory(dir);
        return dir;
    }

    public static Path getDatabasePath() {
        return getAppDataDir().resolve(DB_FILE_NAME);
    }

    public static Path getBackupsDir() {
        Path dir = getAppDataDir().resolve("backups");
        ensureDirectory(dir);
        return dir;
    }

    private static void ensureDirectory(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to create app directory: " + path, ex);
        }
    }
}
