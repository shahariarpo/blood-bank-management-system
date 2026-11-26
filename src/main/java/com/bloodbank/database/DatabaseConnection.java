package com.bloodbank.database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Opens JDBC connections to the local SQLite database.
 *
 * <p>A new connection is created for every call. SQLite connections are cheap,
 * and a fresh connection per operation keeps the code safe when SwingWorker
 * threads are introduced later. Callers must close the connection using
 * try-with-resources.</p>
 */
public final class DatabaseConnection {

    private static final Path DATA_DIRECTORY = Paths.get("data");
    private static final Path DATABASE_FILE = DATA_DIRECTORY.resolve("bloodbank.db");
    private static final String JDBC_URL =
            "jdbc:sqlite:" + DATABASE_FILE.toAbsolutePath().normalize();

    private DatabaseConnection() {
        // Utility class - no instances.
    }

    /**
     * Returns a ready-to-use connection with SQLite foreign key enforcement ON.
     * The PRAGMA is per-connection, so it must be set every time.
     */
    public static Connection getConnection() throws SQLException {
        try {
            Files.createDirectories(DATA_DIRECTORY);
        } catch (IOException e) {
            throw new SQLException(
                    "Could not create data directory: " + DATA_DIRECTORY.toAbsolutePath(), e);
        }

        Connection connection = DriverManager.getConnection(JDBC_URL);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON;");
        } catch (SQLException e) {
            connection.close();
            throw e;
        }
        return connection;
    }

    /** Useful for showing the user where the database file lives. */
    public static Path getDatabaseFile() {
        return DATABASE_FILE.toAbsolutePath().normalize();
    }
}
