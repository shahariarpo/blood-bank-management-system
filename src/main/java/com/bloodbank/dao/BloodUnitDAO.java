package com.bloodbank.dao;

import com.bloodbank.database.DatabaseConnection;
import com.bloodbank.model.BloodUnit;
import com.bloodbank.util.Validators;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Unit persistence: insert, listing, filtering, expiry and status updates. */
public final class BloodUnitDAO {

    private static final String SELECT_JOINED =
            "SELECT bu.unit_id, bu.donation_id, bu.blood_group, bu.component_type, "
                    + "bu.collection_date, bu.expiry_date, bu.status, do.name "
                    + "FROM blood_units bu "
                    + "JOIN donations d ON bu.donation_id = d.donation_id "
                    + "JOIN donors do ON d.donor_id = do.donor_id";

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

    public static List<BloodUnit> findAll() throws SQLException {
        return query(SELECT_JOINED + " ORDER BY bu.expiry_date, bu.unit_id", statement -> { });
    }

    public static Optional<BloodUnit> findById(int unitId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return findById(connection, unitId);
        }
    }

    /** Caller-supplied connection keeps reads inside the caller's transaction. */
    public static Optional<BloodUnit> findById(Connection connection, int unitId)
            throws SQLException {
        List<BloodUnit> units = query(connection, SELECT_JOINED + " WHERE bu.unit_id = ?",
                statement -> statement.setInt(1, unitId));
        return units.isEmpty() ? Optional.empty() : Optional.of(units.get(0));
    }

    /** Optional keyword, blood group and status filters; "ALL" means no filter. */
    public static List<BloodUnit> search(String keyword, String bloodGroup, String status)
            throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_JOINED);
        List<Object> parameters = new ArrayList<>();
        List<String> conditions = new ArrayList<>();

        if (!Validators.isBlank(keyword)) {
            conditions.add("(CAST(bu.unit_id AS TEXT) LIKE ?"
                    + " OR CAST(bu.donation_id AS TEXT) LIKE ?"
                    + " OR do.name LIKE ? OR bu.component_type LIKE ?)");
            String pattern = "%" + keyword.trim() + "%";
            for (int i = 0; i < 4; i++) {
                parameters.add(pattern);
            }
        }
        if (!"ALL".equals(bloodGroup)) {
            conditions.add("bu.blood_group = ?");
            parameters.add(bloodGroup);
        }
        if (!"ALL".equals(status)) {
            conditions.add("bu.status = ?");
            parameters.add(status);
        }
        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }
        sql.append(" ORDER BY bu.expiry_date, bu.unit_id");

        return query(sql.toString(), statement -> bind(statement, parameters));
    }

    /** FEFO: eligible, unreserved units ordered by expiry date, then unit ID. */
    public static List<BloodUnit> findAvailableForIssue(String bloodGroup, int limit,
                                                        LocalDate today) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return findAvailableForIssue(connection, bloodGroup, limit, today);
        }
    }

    public static List<BloodUnit> findAvailableForIssue(Connection connection, String bloodGroup,
                                                        int limit, LocalDate today)
            throws SQLException {
        String sql = SELECT_JOINED
                + " WHERE bu.blood_group = ? AND bu.status = 'AVAILABLE' AND bu.expiry_date > ?"
                + " AND NOT EXISTS (SELECT 1 FROM blood_reservations br"
                + " WHERE br.unit_id = bu.unit_id)"
                + " ORDER BY bu.expiry_date ASC, bu.unit_id ASC LIMIT ?";
        return query(connection, sql, statement -> {
            statement.setString(1, bloodGroup);
            statement.setString(2, today.toString());
            statement.setInt(3, limit);
        });
    }

    /** FEFO across compatible groups; reserved units are held for other requests. */
    public static List<BloodUnit> findAvailableForCompatibleGroups(List<String> bloodGroups,
                                                                   int limit,
                                                                   LocalDate today)
            throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return findAvailableForCompatibleGroups(connection, bloodGroups, limit, today);
        }
    }

    /** Caller-supplied connection sees expiry and reservation changes in one transaction. */
    public static List<BloodUnit> findAvailableForCompatibleGroups(Connection connection,
                                                                   List<String> bloodGroups,
                                                                   int limit,
                                                                   LocalDate today)
            throws SQLException {
        if (bloodGroups == null || bloodGroups.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", Collections.nCopies(bloodGroups.size(), "?"));
        String sql = SELECT_JOINED
                + " WHERE bu.status = 'AVAILABLE' AND bu.expiry_date > ?"
                + " AND bu.blood_group IN (" + placeholders + ")"
                + " AND NOT EXISTS (SELECT 1 FROM blood_reservations br"
                + " WHERE br.unit_id = bu.unit_id)"
                + " ORDER BY bu.expiry_date ASC, bu.unit_id ASC LIMIT ?";
        return query(connection, sql, statement -> {
            statement.setString(1, today.toString());
            for (int i = 0; i < bloodGroups.size(); i++) {
                statement.setString(i + 2, bloodGroups.get(i));
            }
            statement.setInt(bloodGroups.size() + 2, limit);
        });
    }

    /** Flags expired AVAILABLE or QUARANTINED units. ISSUED units are never auto-expired. */
    public static int expireUnits(LocalDate today) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return expireUnits(connection, today);
        }
    }

    /** Caller-supplied connection allows expiry refresh inside an issuing transaction. */
    public static int expireUnits(Connection connection, LocalDate today) throws SQLException {
        String sql = "UPDATE blood_units SET status = 'EXPIRED' "
                + "WHERE status IN ('AVAILABLE', 'QUARANTINED') AND expiry_date <= ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, today.toString());
            return statement.executeUpdate();
        }
    }

    public static boolean updateStatus(int unitId, String status) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return updateStatus(connection, unitId, status);
        }
    }

    /** Caller-supplied connection allows unit status changes to join a transaction. */
    public static boolean updateStatus(Connection connection, int unitId, String status)
            throws SQLException {
        String sql = "UPDATE blood_units SET status = ? WHERE unit_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setInt(2, unitId);
            return statement.executeUpdate() > 0;
        }
    }

    /** Stock is always derived from unit rows, never from a stored counter. */
    public static Map<String, Integer> countAvailableByGroup(LocalDate today)
            throws SQLException {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String group : Validators.BLOOD_GROUPS) {
            counts.put(group, 0);
        }

        String sql = "SELECT blood_group, COUNT(*) AS total FROM blood_units bu "
                + "WHERE bu.status = 'AVAILABLE' AND bu.expiry_date > ?"
                + " AND NOT EXISTS (SELECT 1 FROM blood_reservations br"
                + " WHERE br.unit_id = bu.unit_id)"
                + " GROUP BY blood_group";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, today.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String group = resultSet.getString("blood_group");
                    if (counts.containsKey(group)) {
                        counts.put(group, resultSet.getInt("total"));
                    }
                }
            }
        }
        return counts;
    }

    public static Map<String, Integer> countByStatus() throws SQLException {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("AVAILABLE", 0);
        counts.put("ISSUED", 0);
        counts.put("EXPIRED", 0);
        counts.put("QUARANTINED", 0);

        String sql = "SELECT status, COUNT(*) AS total FROM blood_units GROUP BY status";
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

    public static List<BloodUnit> findExpiringSoon(int days, LocalDate today)
            throws SQLException {
        String sql = SELECT_JOINED
                + " WHERE bu.status = 'AVAILABLE' AND bu.expiry_date BETWEEN ? AND ?"
                + " AND NOT EXISTS (SELECT 1 FROM blood_reservations br"
                + " WHERE br.unit_id = bu.unit_id)"
                + " ORDER BY bu.expiry_date, bu.unit_id";
        return query(sql, statement -> {
            statement.setString(1, today.toString());
            statement.setString(2, today.plusDays(days).toString());
        });
    }

    private static void bind(PreparedStatement statement, List<Object> parameters)
            throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            Object value = parameters.get(i);
            if (value instanceof Integer) {
                statement.setInt(i + 1, (Integer) value);
            } else {
                statement.setString(i + 1, String.valueOf(value));
            }
        }
    }

    private interface ParameterSetter {
        void set(PreparedStatement statement) throws SQLException;
    }

    private static List<BloodUnit> query(String sql, ParameterSetter setter) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return query(connection, sql, setter);
        }
    }

    private static List<BloodUnit> query(Connection connection, String sql, ParameterSetter setter)
            throws SQLException {
        List<BloodUnit> units = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setter.set(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    units.add(new BloodUnit(
                            resultSet.getInt("unit_id"),
                            resultSet.getInt("donation_id"),
                            resultSet.getString("blood_group"),
                            resultSet.getString("component_type"),
                            LocalDate.parse(resultSet.getString("collection_date")),
                            LocalDate.parse(resultSet.getString("expiry_date")),
                            resultSet.getString("status"),
                            resultSet.getString("name")));
                }
            }
        }
        return units;
    }
}
