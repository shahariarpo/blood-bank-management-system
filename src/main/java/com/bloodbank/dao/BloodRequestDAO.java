package com.bloodbank.dao;

import com.bloodbank.database.DatabaseConnection;
import com.bloodbank.model.BloodRequest;

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
import java.util.Optional;

/** Request persistence with patient details joined for display. */
public final class BloodRequestDAO {

    private static final String SELECT_JOINED =
            "SELECT r.request_id, r.patient_id, p.name, p.blood_group AS patient_blood_group, "
                    + "r.blood_group, r.quantity, r.request_date, r.priority, r.status "
                    + "FROM blood_requests r JOIN patients p ON r.patient_id = p.patient_id";

    private static final List<String> STATUSES = List.of(
            "PENDING", "APPROVED", "PARTIALLY_FULFILLED", "FULFILLED", "REJECTED", "CANCELLED");

    private BloodRequestDAO() {
        // DAO class - no instances.
    }

    public static List<BloodRequest> findAll() throws SQLException {
        return query(SELECT_JOINED + " ORDER BY r.request_date DESC, r.request_id DESC",
                statement -> { });
    }

    public static List<BloodRequest> findByStatus(String status) throws SQLException {
        return query(SELECT_JOINED + " WHERE r.status = ?"
                        + " ORDER BY r.request_date DESC, r.request_id DESC",
                statement -> statement.setString(1, status));
    }

    public static Optional<BloodRequest> findById(int requestId) throws SQLException {
        List<BloodRequest> requests = query(SELECT_JOINED + " WHERE r.request_id = ?",
                statement -> statement.setInt(1, requestId));
        return requests.isEmpty() ? Optional.empty() : Optional.of(requests.get(0));
    }

    public static List<BloodRequest> search(String keyword) throws SQLException {
        String sql = SELECT_JOINED
                + " WHERE p.name LIKE ? OR CAST(r.request_id AS TEXT) LIKE ?"
                + " OR r.blood_group LIKE ? OR r.priority LIKE ? OR r.status LIKE ?"
                + " ORDER BY r.request_date DESC, r.request_id DESC";
        String pattern = "%" + keyword + "%";
        return query(sql, statement -> {
            for (int i = 1; i <= 5; i++) {
                statement.setString(i, pattern);
            }
        });
    }

    public static int insert(BloodRequest request) throws SQLException {
        String sql = "INSERT INTO blood_requests "
                + "(patient_id, blood_group, quantity, request_date, priority, status) "
                + "VALUES (?, ?, ?, ?, ?, 'PENDING')";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, request.getPatientId());
            statement.setString(2, request.getBloodGroup());
            statement.setInt(3, request.getQuantity());
            statement.setString(4, request.getRequestDate().toString());
            statement.setString(5, request.getPriority());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    public static boolean updateStatus(int requestId, String status) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return updateStatus(connection, requestId, status);
        }
    }

    /** Caller-supplied connection allows issuing to update the request in one transaction. */
    public static boolean updateStatus(Connection connection, int requestId, String status)
            throws SQLException {
        if (!STATUSES.contains(status)) {
            throw new IllegalArgumentException("Unknown request status: " + status);
        }
        String sql = "UPDATE blood_requests SET status = ? WHERE request_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setInt(2, requestId);
            return statement.executeUpdate() > 0;
        }
    }

    public static Map<String, Integer> countByStatus() throws SQLException {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String status : STATUSES) {
            counts.put(status, 0);
        }
        String sql = "SELECT status, COUNT(*) AS total FROM blood_requests GROUP BY status";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String status = resultSet.getString("status");
                if (counts.containsKey(status)) {
                    counts.put(status, resultSet.getInt("total"));
                }
            }
        }
        return counts;
    }

    private interface ParameterSetter {
        void set(PreparedStatement statement) throws SQLException;
    }

    private static List<BloodRequest> query(String sql, ParameterSetter setter)
            throws SQLException {
        List<BloodRequest> requests = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            setter.set(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    requests.add(new BloodRequest(
                            resultSet.getInt("request_id"),
                            resultSet.getInt("patient_id"),
                            resultSet.getString("name"),
                            resultSet.getString("patient_blood_group"),
                            resultSet.getString("blood_group"),
                            resultSet.getInt("quantity"),
                            LocalDate.parse(resultSet.getString("request_date")),
                            resultSet.getString("priority"),
                            resultSet.getString("status")));
                }
            }
        }
        return requests;
    }
}
