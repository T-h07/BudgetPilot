package com.budgetpilot.persistence;

import com.budgetpilot.util.ValidationUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class DbManager {
    private DbManager() {
    }

    public static Connection open(Path dbPath) throws SQLException {
        ValidationUtils.requireNonNull(dbPath, "dbPath");
        try {
            Files.createDirectories(dbPath.getParent());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to create database directory: " + dbPath.getParent(), ex);
        }

        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath());
        try {
            configureConnection(connection);
            DbMigrations.runMigrations(connection);
            return connection;
        } catch (Exception ex) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
            if (ex instanceof SQLException sqlEx) {
                throw sqlEx;
            }
            throw new SQLException("Failed to initialize SQLite database", ex);
        }
    }

    public static int readUserVersion(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA user_version")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static void configureConnection(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
            stmt.execute("PRAGMA journal_mode = WAL");
            stmt.execute("PRAGMA busy_timeout = 5000");
        }
    }
}
