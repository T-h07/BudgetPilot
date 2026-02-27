package com.budgetpilot.service;

import java.nio.file.Path;

public class PersistenceStatus {
    private final boolean persistent;
    private final String message;
    private final Path databasePath;
    private final Path backupsPath;

    public PersistenceStatus(boolean persistent, String message, Path databasePath, Path backupsPath) {
        this.persistent = persistent;
        this.message = message == null ? "" : message;
        this.databasePath = databasePath;
        this.backupsPath = backupsPath;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public String getMessage() {
        return message;
    }

    public Path getDatabasePath() {
        return databasePath;
    }

    public Path getBackupsPath() {
        return backupsPath;
    }
}
