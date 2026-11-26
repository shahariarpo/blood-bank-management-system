package com.bloodbank.service;

import com.bloodbank.database.DatabaseConnection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Creates consistent SQLite backups with VACUUM INTO.
 * The backup is a standalone database file and can be opened independently.
 */
public final class BackupService {

    private static final DateTimeFormatter FILE_STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final int EXPECTED_TABLE_COUNT = 8;

    private BackupService() {
        // Service class - no instances.
    }

    public static String suggestedFileName() {
        return "bloodbank-" + LocalDateTime.now().format(FILE_STAMP) + ".db";
    }

    /** Writes a consistent snapshot to the given file. The file must not already exist. */
    public static Path createBackup(Path destination) throws SQLException, IOException {
        if (destination == null) {
            throw new IllegalArgumentException("Choose a backup file location.");
        }

        Path target = destination.toAbsolutePath().normalize();
        Path parent = target.getParent();
        if (parent == null) {
            throw new IllegalArgumentException("The backup path has no parent directory.");
        }
        Files.createDirectories(parent);
        if (Files.exists(target)) {
            throw new IllegalStateException("A file already exists at that path. Choose a new name.");
        }

        // The path is generated/approved by the caller and quotes are escaped defensively.
        String sql = "VACUUM INTO '" + target.toString().replace("'", "''") + "'";
        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }

        if (!Files.exists(target) || Files.size(target) == 0) {
            throw new SQLException("The backup file was not created correctly.");
        }
        return target;
    }

    /** Convenience method used by the console test and the GUI default location. */
    public static Path createTimestampedBackup(Path directory) throws SQLException, IOException {
        if (directory == null) {
            directory = DatabaseConnection.getDatabaseFile().getParent().resolve("backups");
        }
        return createBackup(directory.resolve(suggestedFileName()));
    }

    /** Opens a backup independently and verifies that all application tables are present. */
    public static int verifyBackup(Path backupFile) throws SQLException {
        if (backupFile == null || !Files.exists(backupFile)) {
            throw new SQLException("Backup file not found: " + backupFile);
        }

        String url = "jdbc:sqlite:" + backupFile.toAbsolutePath().normalize();
        String sql = "SELECT COUNT(*) FROM sqlite_master "
                + "WHERE type = 'table' AND name NOT LIKE 'sqlite_%'";
        try (Connection connection = DriverManager.getConnection(url);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            int tables = resultSet.getInt(1);
            if (tables != EXPECTED_TABLE_COUNT) {
                throw new SQLException("Backup has " + tables + " tables; expected "
                        + EXPECTED_TABLE_COUNT + ".");
            }
            return tables;
        }
    }

    /** Reads a single count from a backup, used to prove the snapshot is usable. */
    public static int countInBackup(Path backupFile, String table) throws SQLException {
        if (!List.of("admin", "donors", "donations", "blood_units", "patients",
                "blood_requests", "blood_issues").contains(table)) {
            throw new IllegalArgumentException("Unknown table: " + table);
        }
        String url = "jdbc:sqlite:" + backupFile.toAbsolutePath().normalize();
        try (Connection connection = DriverManager.getConnection(url);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }
}
