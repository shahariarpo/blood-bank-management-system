package com.bloodbank.dao;

import com.bloodbank.database.DatabaseConnection;
import com.bloodbank.model.Donor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** SQL for donor registration, searching and editing. */
public final class DonorDAO {

    private DonorDAO() {
        // DAO class - no instances.
    }

    public static List<Donor> findAll() throws SQLException {
        String sql = "SELECT donor_id, name, age, gender, blood_group, phone, address, created_at "
                + "FROM donors ORDER BY name COLLATE NOCASE";
        return query(sql, statement -> { });
    }

    /** Searches name, ID, phone and blood group with one LIKE pattern. */
    public static List<Donor> search(String keyword) throws SQLException {
        String sql = "SELECT donor_id, name, age, gender, blood_group, phone, address, created_at "
                + "FROM donors WHERE name LIKE ? OR CAST(donor_id AS TEXT) LIKE ? "
                + "OR phone LIKE ? OR blood_group LIKE ? ORDER BY name COLLATE NOCASE";
        String pattern = "%" + keyword + "%";
        return query(sql, statement -> {
            for (int i = 1; i <= 4; i++) {
                statement.setString(i, pattern);
            }
        });
    }

    public static Optional<Donor> findById(int donorId) throws SQLException {
        String sql = "SELECT donor_id, name, age, gender, blood_group, phone, address, created_at "
                + "FROM donors WHERE donor_id = ?";
        List<Donor> donors = query(sql, statement -> statement.setInt(1, donorId));
        return donors.isEmpty() ? Optional.empty() : Optional.of(donors.get(0));
    }

    /** Used by the service for duplicate detection; excludedId is 0 when adding. */
    public static boolean existsByPhone(String phone, int excludedId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM donors WHERE phone = ? AND donor_id <> ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, phone);
            statement.setInt(2, excludedId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    public static int insert(Donor donor) throws SQLException {
        String sql = "INSERT INTO donors (name, age, gender, blood_group, phone, address) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            setDonorValues(statement, donor);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    public static boolean update(Donor donor) throws SQLException {
        String sql = "UPDATE donors SET name = ?, age = ?, gender = ?, blood_group = ?, "
                + "phone = ?, address = ? WHERE donor_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            setDonorValues(statement, donor);
            statement.setInt(7, donor.getDonorId());
            return statement.executeUpdate() > 0;
        }
    }

    /** Throws SQLException if a donation still references this donor (foreign key). */
    public static boolean delete(int donorId) throws SQLException {
        String sql = "DELETE FROM donors WHERE donor_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, donorId);
            return statement.executeUpdate() > 0;
        }
    }

    private static void setDonorValues(PreparedStatement statement, Donor donor)
            throws SQLException {
        statement.setString(1, donor.getName());
        statement.setInt(2, donor.getAge());
        statement.setString(3, donor.getGender());
        statement.setString(4, donor.getBloodGroup());
        statement.setString(5, donor.getPhone());
        statement.setString(6, donor.getAddress());
    }

    /** Small functional interface so findAll/search can share row mapping. */
    private interface ParameterSetter {
        void set(PreparedStatement statement) throws SQLException;
    }

    private static List<Donor> query(String sql, ParameterSetter setter) throws SQLException {
        List<Donor> donors = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            setter.set(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    donors.add(new Donor(
                            resultSet.getInt("donor_id"),
                            resultSet.getString("name"),
                            resultSet.getInt("age"),
                            resultSet.getString("gender"),
                            resultSet.getString("blood_group"),
                            resultSet.getString("phone"),
                            resultSet.getString("address"),
                            resultSet.getString("created_at")));
                }
            }
        }
        return donors;
    }
}
