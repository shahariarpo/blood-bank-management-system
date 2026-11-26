package com.bloodbank.dao;

import com.bloodbank.database.DatabaseConnection;
import com.bloodbank.model.BloodIssue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Issuing history persistence. Inserts join the caller's issuing transaction. */
public final class BloodIssueDAO {

    private static final String SELECT_JOINED =
            "SELECT bi.issue_id, bi.request_id, bi.unit_id, bi.issue_date, "
                    + "p.name, bu.blood_group, bu.component_type "
                    + "FROM blood_issues bi "
                    + "JOIN blood_requests r ON bi.request_id = r.request_id "
                    + "JOIN patients p ON r.patient_id = p.patient_id "
                    + "JOIN blood_units bu ON bi.unit_id = bu.unit_id";

    private BloodIssueDAO() {
        // DAO class - no instances.
    }

    public static int insert(Connection connection, BloodIssue issue) throws SQLException {
        String sql = "INSERT INTO blood_issues (request_id, unit_id, issue_date) "
                + "VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, issue.getRequestId());
            statement.setInt(2, issue.getUnitId());
            statement.setString(3, issue.getIssueDate().toString());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    public static List<BloodIssue> findAll() throws SQLException {
        return query(SELECT_JOINED + " ORDER BY bi.issue_date DESC, bi.issue_id DESC",
                statement -> { });
    }

    public static List<BloodIssue> findByRequestId(int requestId) throws SQLException {
        return query(SELECT_JOINED + " WHERE bi.request_id = ? ORDER BY bi.issue_id",
                statement -> statement.setInt(1, requestId));
    }

    private interface ParameterSetter {
        void set(PreparedStatement statement) throws SQLException;
    }

    private static List<BloodIssue> query(String sql, ParameterSetter setter)
            throws SQLException {
        List<BloodIssue> issues = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            setter.set(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    issues.add(new BloodIssue(
                            resultSet.getInt("issue_id"),
                            resultSet.getInt("request_id"),
                            resultSet.getInt("unit_id"),
                            LocalDate.parse(resultSet.getString("issue_date")),
                            resultSet.getString("name"),
                            resultSet.getString("blood_group"),
                            resultSet.getString("component_type")));
                }
            }
        }
        return issues;
    }
}
