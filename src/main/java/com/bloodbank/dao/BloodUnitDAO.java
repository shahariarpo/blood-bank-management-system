package com.bloodbank.dao;

import com.bloodbank.database.DatabaseConnection;
import com.bloodbank.model.BloodUnit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Minimal unit persistence needed by donation processing.
 * Milestone 6 expands this class with listing and filtering.
 */
public final class BloodUnitDAO {

    private BloodUnitDAO() {
        // DAO class - no instances.
    }

    /** Uses a caller-supplied connection so insertion can join a transaction. */
    public static int insert(Connection connection, BloodUnit unit) throws SQLException {
        String sql = "INSERT INTO blood_units (donation_id, blood_group, component_type, "
                + "collection_date, expiry_date, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, unit.getDonationId());
            statement.setString(2, unit.getBloodGroup());
            statement.setString(3, unit.getComponentType());
            statement.setString(4, unit.getCollectionDate().toString());
            statement.setString(5, unit.getExpiryDate().toString());
            statement.setString(6, unit.getStatus());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    /** Checks whether a donation has already produced a unit (duplicate processing guard). */
    public static boolean existsForDonation(Connection connection, int donationId)
            throws SQLException {
        String sql = "SELECT COUNT(*) FROM blood_units WHERE donation_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, donationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    public static boolean existsForDonation(int donationId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return existsForDonation(connection, donationId);
        }
    }
}
