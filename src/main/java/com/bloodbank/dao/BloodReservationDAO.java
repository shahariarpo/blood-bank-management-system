package com.bloodbank.dao;

import com.bloodbank.database.DatabaseConnection;
import com.bloodbank.model.BloodReservation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Approval reservations: one unit can be held for only one request at a time. */
public final class BloodReservationDAO {

    private static final String SELECT_JOINED =
            "SELECT br.reservation_id, br.request_id, br.unit_id, br.reserved_on, "
                    + "p.name, bu.blood_group, bu.component_type, bu.expiry_date "
                    + "FROM blood_reservations br "
                    + "JOIN blood_requests r ON br.request_id = r.request_id "
                    + "JOIN patients p ON r.patient_id = p.patient_id "
                    + "JOIN blood_units bu ON br.unit_id = bu.unit_id";

    private BloodReservationDAO() {
        // DAO class - no instances.
    }

    /** Caller-supplied connection allows reservation to join approval or issuing. */
    public static void insert(Connection connection, int requestId, int unitId)
            throws SQLException {
        String sql = "INSERT INTO blood_reservations (request_id, unit_id) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, requestId);
            statement.setInt(2, unitId);
            statement.executeUpdate();
        }
    }

    public static void deleteByRequestId(Connection connection, int requestId)
            throws SQLException {
        String sql = "DELETE FROM blood_reservations WHERE request_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, requestId);
            statement.executeUpdate();
        }
    }

    public static void deleteByRequestId(int requestId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            deleteByRequestId(connection, requestId);
        }
    }

    public static void deleteByUnitId(Connection connection, int unitId) throws SQLException {
        String sql = "DELETE FROM blood_reservations WHERE unit_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, unitId);
            statement.executeUpdate();
        }
    }

    /** Releases holds on expired, issued or quarantined units. */
    public static int releaseExpired(Connection connection, LocalDate today) throws SQLException {
        String sql = "DELETE FROM blood_reservations WHERE unit_id IN ("
                + "SELECT unit_id FROM blood_units "
                + "WHERE expiry_date <= ? OR status <> 'AVAILABLE')";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, today.toString());
            return statement.executeUpdate();
        }
    }

    public static List<Integer> findUnitIdsByRequestId(int requestId) throws SQLException {
        String sql = "SELECT unit_id FROM blood_reservations "
                + "WHERE request_id = ? ORDER BY unit_id";
        List<Integer> unitIds = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, requestId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    unitIds.add(resultSet.getInt("unit_id"));
                }
            }
        }
        return unitIds;
    }

    public static List<Integer> findUnitIdsByRequestId(Connection connection, int requestId)
            throws SQLException {
        String sql = "SELECT unit_id FROM blood_reservations "
                + "WHERE request_id = ? ORDER BY unit_id";
        List<Integer> unitIds = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, requestId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    unitIds.add(resultSet.getInt("unit_id"));
                }
            }
        }
        return unitIds;
    }

    /** unit_id → request_id for the current holds. */
    public static Map<Integer, Integer> findRequestIdsByUnit() throws SQLException {
        String sql = "SELECT unit_id, request_id FROM blood_reservations";
        Map<Integer, Integer> result = new LinkedHashMap<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                result.put(resultSet.getInt("unit_id"), resultSet.getInt("request_id"));
            }
        }
        return result;
    }

    /** request_id → reserved unit count. */
    public static Map<Integer, Integer> countByRequest() throws SQLException {
        String sql = "SELECT request_id, COUNT(*) AS total "
                + "FROM blood_reservations GROUP BY request_id";
        Map<Integer, Integer> result = new LinkedHashMap<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                result.put(resultSet.getInt("request_id"), resultSet.getInt("total"));
            }
        }
        return result;
    }

    public static int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM blood_reservations";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    public static List<BloodReservation> findAll() throws SQLException {
        String sql = SELECT_JOINED + " ORDER BY br.reserved_on, br.reservation_id";
        List<BloodReservation> reservations = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                reservations.add(new BloodReservation(
                        resultSet.getInt("reservation_id"),
                        resultSet.getInt("request_id"),
                        resultSet.getInt("unit_id"),
                        resultSet.getString("reserved_on"),
                        resultSet.getString("name"),
                        resultSet.getString("blood_group"),
                        resultSet.getString("component_type"),
                        LocalDate.parse(resultSet.getString("expiry_date"))));
            }
        }
        return reservations;
    }
}
