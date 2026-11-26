package com.bloodbank.dao;

import com.bloodbank.database.DatabaseConnection;
import com.bloodbank.model.Admin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/** All SQL for the admin table lives here, never in the UI or service. */
public final class AdminDAO {

    private AdminDAO() {
        // DAO class - no instances.
    }

    public static int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM admin";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    public static Optional<Admin> findByUsername(String username) throws SQLException {
        String sql = "SELECT admin_id, username, password_hash FROM admin WHERE username = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(new Admin(
                            resultSet.getInt("admin_id"),
                            resultSet.getString("username"),
                            resultSet.getString("password_hash")));
                }
                return Optional.empty();
            }
        }
    }

    public static int insert(Admin admin) throws SQLException {
        String sql = "INSERT INTO admin (username, password_hash) VALUES (?, ?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, admin.getUsername());
            statement.setString(2, admin.getPasswordHash());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }
}
