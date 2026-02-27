package com.budgetpilot.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DbMigrations {
    private static final Map<Integer, String> MIGRATIONS = new LinkedHashMap<>();

    static {
        MIGRATIONS.put(1, "/db/schema_v1.sql");
        MIGRATIONS.put(2, "/db/schema_v2.sql");
        MIGRATIONS.put(3, "/db/schema_v3.sql");
        MIGRATIONS.put(4, "/db/schema_v4.sql");
        MIGRATIONS.put(5, "/db/schema_v5.sql");
        MIGRATIONS.put(6, "/db/schema_v6.sql");
        MIGRATIONS.put(7, "/db/schema_v7.sql");
        MIGRATIONS.put(8, "/db/schema_v8.sql");
    }

    private DbMigrations() {
    }

    public static int latestVersion() {
        return MIGRATIONS.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    public static void runMigrations(Connection connection) throws SQLException {
        int currentVersion = getUserVersion(connection);

        for (Map.Entry<Integer, String> migration : MIGRATIONS.entrySet()) {
            int version = migration.getKey();
            if (version <= currentVersion) {
                continue;
            }

            String sql = readMigrationSql(migration.getValue());
            executeSqlScript(connection, sql);
            setUserVersion(connection, version);
            currentVersion = version;
        }
    }

    private static int getUserVersion(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA user_version")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static void setUserVersion(Connection connection, int version) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA user_version = " + version);
        }
    }

    private static String readMigrationSql(String resourcePath) {
        try (InputStream inputStream = DbMigrations.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException("Migration resource not found: " + resourcePath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read migration resource: " + resourcePath, e);
        }
    }

    private static void executeSqlScript(Connection connection, String script) throws SQLException {
        String[] statements = script.split(";\\s*(\\r?\\n|$)");
        try (Statement stmt = connection.createStatement()) {
            for (String raw : statements) {
                String sql = raw.trim();
                if (sql.isEmpty()) {
                    continue;
                }
                stmt.execute(sql);
            }
        }
    }
}
