package com.bloodbank.dao;

import com.bloodbank.database.DatabaseConnection;
import com.bloodbank.model.BloodUnit;
import com.bloodbank.model.Donation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Donation history plus transaction-safe screening processing. */
public final class DonationDAO {

    private static final String SELECT_JOINED =
            "SELECT d.donation_id, d.donor_id, do.name, do.blood_group, "
                    + "d.donation_date, d.quantity_ml, d.screening_status, d.notes "
                    + "FROM donations d JOIN donors do ON d.donor_id = do.donor_id";

    private DonationDAO() {
        // DAO class - no instances.
    }

    public static List<Donation> findAll() throws SQLException {
        return query(SELECT_JOINED
                + " ORDER BY d.donation_date DESC, d.donation_id DESC", statement -> { });
    }

    public static List<Donation> findByDonorId(int donorId) throws SQLException {
        return query(SELECT_JOINED + " WHERE d.donor_id = ?"
                        + " ORDER BY d.donation_date DESC, d.donation_id DESC",
                statement -> statement.setInt(1, donorId));
    }

    public static List<Donation> search(String keyword) throws SQLException {
        String pattern = "%" + keyword + "%";
        return query(SELECT_JOINED
                        + " WHERE do.name LIKE ? OR CAST(d.donation_id AS TEXT) LIKE ?"
                        + " OR d.screening_status LIKE ? OR d.donation_date LIKE ?"
                        + " ORDER BY d.donation_date DESC, d.donation_id DESC",
                statement -> {
                    for (int i = 1; i <= 4; i++) {
                        statement.setString(i, pattern);
                    }
                });
    }

    public static Optional<Donation> findById(int donationId) throws SQLException {
        List<Donation> donations = query(SELECT_JOINED + " WHERE d.donation_id = ?",
                statement -> statement.setInt(1, donationId));
        return donations.isEmpty() ? Optional.empty() : Optional.of(donations.get(0));
    }

    /** Inserts a donation that is not accepted (PENDING or REJECTED). */
    public static int insert(Donation donation) throws SQLException {
        if ("ACCEPTED".equals(donation.getScreeningStatus())) {
            throw new IllegalArgumentException("Accepted donations must use insertWithAcceptance.");
        }
        String sql = "INSERT INTO donations (donor_id, donation_date, quantity_ml, "
                + "screening_status, notes) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            setDonationValues(statement, donation);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    /**
     * Inserts an accepted donation and its single blood unit in one transaction.
     * One donation produces one unit; the quantity is recorded on the donation.
     */
    public static int insertWithAcceptance(Donation donation, String componentType,
                                           LocalDate expiryDate) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            boolean oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                String bloodGroup = findBloodGroup(connection, donation.getDonorId());
                if (bloodGroup == null) {
                    throw new IllegalStateException("The selected donor no longer exists.");
                }

                String insertSql = "INSERT INTO donations (donor_id, donation_date, "
                        + "quantity_ml, screening_status, notes) VALUES (?, ?, ?, 'PENDING', ?)";
                int donationId;
                try (PreparedStatement statement = connection.prepareStatement(
                        insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setInt(1, donation.getDonorId());
                    statement.setString(2, donation.getDonationDate().toString());
                    statement.setInt(3, donation.getQuantityMl());
                    statement.setString(4, donation.getNotes());
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        keys.next();
                        donationId = keys.getInt(1);
                    }
                }

                acceptInsideTransaction(connection, donationId, bloodGroup,
                        donation.getDonationDate(), componentType, expiryDate);
                connection.commit();
                return donationId;
            } catch (SQLException | IllegalStateException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(oldAutoCommit);
            }
        }
    }

    /** Processes a pending donation and creates its unit atomically. */
    public static void accept(int donationId, String componentType, LocalDate expiryDate)
            throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            boolean oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                String status = null;
                String bloodGroup = null;
                LocalDate collectionDate = null;
                String sql = "SELECT d.screening_status, do.blood_group, d.donation_date "
                        + "FROM donations d JOIN donors do ON d.donor_id = do.donor_id "
                        + "WHERE d.donation_id = ?";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setInt(1, donationId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            status = resultSet.getString("screening_status");
                            bloodGroup = resultSet.getString("blood_group");
                            collectionDate = LocalDate.parse(resultSet.getString("donation_date"));
                        }
                    }
                }

                if (status == null) {
                    throw new IllegalStateException("Donation not found. Refresh the list.");
                }
                if (!"PENDING".equals(status)) {
                    throw new IllegalStateException("Only PENDING donations can be processed.");
                }
                if (BloodUnitDAO.existsForDonation(connection, donationId)) {
                    throw new IllegalStateException("This donation has already been processed.");
                }

                acceptInsideTransaction(connection, donationId, bloodGroup,
                        collectionDate, componentType, expiryDate);
                connection.commit();
            } catch (SQLException | IllegalStateException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(oldAutoCommit);
            }
        }
    }

    public static boolean reject(int donationId) throws SQLException {
        String sql = "UPDATE donations SET screening_status = 'REJECTED' "
                + "WHERE donation_id = ? AND screening_status = 'PENDING'";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, donationId);
            return statement.executeUpdate() > 0;
        }
    }

    private static void acceptInsideTransaction(Connection connection, int donationId,
                                                String bloodGroup, LocalDate collectionDate,
                                                String componentType, LocalDate expiryDate)
            throws SQLException {
        String update = "UPDATE donations SET screening_status = 'ACCEPTED' "
                + "WHERE donation_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(update)) {
            statement.setInt(1, donationId);
            statement.executeUpdate();
        }
        BloodUnitDAO.insert(connection, new BloodUnit(0, donationId, bloodGroup,
                componentType, collectionDate, expiryDate, "AVAILABLE"));
    }

    private static String findBloodGroup(Connection connection, int donorId)
            throws SQLException {
        String sql = "SELECT blood_group FROM donors WHERE donor_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, donorId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString("blood_group") : null;
            }
        }
    }

    private static void setDonationValues(PreparedStatement statement, Donation donation)
            throws SQLException {
        statement.setInt(1, donation.getDonorId());
        statement.setString(2, donation.getDonationDate().toString());
        statement.setInt(3, donation.getQuantityMl());
        statement.setString(4, donation.getScreeningStatus());
        statement.setString(5, donation.getNotes());
    }

    private interface ParameterSetter {
        void set(PreparedStatement statement) throws SQLException;
    }

    private static List<Donation> query(String sql, ParameterSetter setter) throws SQLException {
        List<Donation> donations = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            setter.set(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    donations.add(new Donation(
                            resultSet.getInt("donation_id"),
                            resultSet.getInt("donor_id"),
                            resultSet.getString("name"),
                            resultSet.getString("blood_group"),
                            LocalDate.parse(resultSet.getString("donation_date")),
                            resultSet.getInt("quantity_ml"),
                            resultSet.getString("screening_status"),
                            resultSet.getString("notes")));
                }
            }
        }
        return donations;
    }
}
