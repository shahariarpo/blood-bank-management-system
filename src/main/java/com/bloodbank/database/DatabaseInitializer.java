package com.bloodbank.database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates the eight application tables by running schema.sql from the classpath.
 * Because the script uses IF NOT EXISTS, this can be called on every startup.
 */
public final class DatabaseInitializer {

    private static final String SCHEMA_RESOURCE = "/database/schema.sql";

    private DatabaseInitializer() {
        // Utility class - no instances.
    }

    public static void initialize() throws SQLException, IOException {
        try (InputStream schema = DatabaseInitializer.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (schema == null) {
                throw new IOException("Missing classpath resource: " + SCHEMA_RESOURCE);
            }
            try (Connection connection = DatabaseConnection.getConnection()) {
                executeScript(connection, schema);
            }
        }
    }

    /**
     * Reads the SQL file line by line and executes one statement at a time.
     * Keeping this explicit (instead of relying on a driver-specific batch
     * feature) makes it easy to understand and works with plain JDBC.
     */
    private static void executeScript(Connection connection, InputStream input)
            throws SQLException, IOException {

        StringBuilder statement = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String cleanLine = removeComment(line).trim();
                if (cleanLine.isEmpty()) {
                    continue;
                }
                statement.append(cleanLine).append('\n');

                if (cleanLine.endsWith(";")) {
                    execute(connection, statement.toString());
                    statement.setLength(0);
                }
            }
        }

        if (!statement.toString().trim().isEmpty()) {
            execute(connection, statement.toString());
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql.trim());
        }
    }

    /** Removes -- line comments. The schema has no -- inside string literals. */
    private static String removeComment(String line) {
        int commentStart = line.indexOf("--");
        return commentStart >= 0 ? line.substring(0, commentStart) : line;
    }
}
