package com.bloodbank;

import com.bloodbank.dao.AdminDAO;
import com.bloodbank.database.DatabaseConnection;
import com.bloodbank.database.DatabaseInitializer;
import com.bloodbank.model.Admin;
import com.bloodbank.model.BloodComponent;
import com.bloodbank.model.Donation;
import com.bloodbank.model.Donor;
import com.bloodbank.service.AuthService;
import com.bloodbank.service.DonationService;
import com.bloodbank.service.DonorService;
import com.bloodbank.ui.LoginFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Application entry point.
 * Console smoke tests: --test-auth, --test-donors, --test-donations.
 */
public class Main {

    private static final String TEST_ARG = "--test-auth";
    private static final String DONOR_TEST_ARG = "--test-donors";
    private static final String DONATION_TEST_ARG = "--test-donations";

    public static void main(String[] args) {
        try {
            DatabaseInitializer.initialize();
            boolean created = AuthService.ensureDefaultAdmin();

            System.out.println("Database ready: " + DatabaseConnection.getDatabaseFile());
            if (created) {
                System.out.println("Default admin created - username: "
                        + AuthService.DEFAULT_USERNAME + " / password: " + AuthService.DEFAULT_PASSWORD);
            }

            if (args.length > 0 && TEST_ARG.equals(args[0])) {
                runAuthenticationTest();
                return;
            }
            if (args.length > 0 && DONOR_TEST_ARG.equals(args[0])) {
                runDonorTest();
                return;
            }
            if (args.length > 0 && DONATION_TEST_ARG.equals(args[0])) {
                runDonationTest();
                return;
            }

            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            SwingUtilities.invokeLater(() -> {
                if (created) {
                    JOptionPane.showMessageDialog(null,
                            "Default administrator created.\nUsername: "
                                    + AuthService.DEFAULT_USERNAME + "\nPassword: "
                                    + AuthService.DEFAULT_PASSWORD,
                            "First-run setup", JOptionPane.INFORMATION_MESSAGE);
                }
                new LoginFrame();
            });

        } catch (Exception e) {
            System.err.println("Application failed to start: " + e.getMessage());
            e.printStackTrace();
            boolean consoleTest = args.length > 0
                    && (TEST_ARG.equals(args[0]) || DONOR_TEST_ARG.equals(args[0])
                    || DONATION_TEST_ARG.equals(args[0]));
            if (!consoleTest) {
                JOptionPane.showMessageDialog(null,
                        "Startup failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static void runAuthenticationTest() throws SQLException {
        System.out.println();
        System.out.println("Authentication smoke test:");

        Optional<Admin> admin = AuthService.login(
                AuthService.DEFAULT_USERNAME, AuthService.DEFAULT_PASSWORD);
        if (admin.isEmpty() || !AuthService.isLoggedIn()) {
            throw new SQLException("Correct credentials were rejected.");
        }
        System.out.println("  Correct credentials: OK (admin_id = "
                + admin.get().getAdminId() + ")");

        String hash = AdminDAO.findByUsername(AuthService.DEFAULT_USERNAME)
                .orElseThrow().getPasswordHash();
        if (!hash.startsWith("$2")) {
            throw new SQLException("Password is not stored as a BCrypt hash.");
        }
        System.out.println("  Stored password hash: OK (" + hash.substring(0, 7) + "...)");

        AuthService.logout();
        if (AuthService.isLoggedIn()) {
            throw new SQLException("Logout did not clear the session.");
        }
        System.out.println("  Logout: OK");

        if (AuthService.login(AuthService.DEFAULT_USERNAME, "wrong-password").isPresent()) {
            throw new SQLException("Wrong password was accepted.");
        }
        System.out.println("  Wrong password: rejected");

        try {
            AuthService.login("   ", AuthService.DEFAULT_PASSWORD);
            throw new SQLException("Blank username was accepted.");
        } catch (IllegalArgumentException expected) {
            System.out.println("  Blank username: rejected");
        }

        System.out.println("Authentication test completed successfully.");
    }

    private static void runDonorTest() throws SQLException {
        System.out.println();
        System.out.println("Donor smoke test:");
        String phone = "5550001111";
        Donor saved = DonorService.add(
                new Donor("M4 Test Donor", 30, "Female", "O+", phone, "Test address"));
        System.out.println("  ADD ok - donor_id = " + saved.getDonorId());

        try {
            try {
                DonorService.add(new Donor("Too young", 17, "Male", "A+", "5550002222", null));
                throw new SQLException("Age 17 was incorrectly accepted.");
            } catch (IllegalArgumentException expected) {
                System.out.println("  AGE validation: rejected");
            }

            try {
                DonorService.add(new Donor("Duplicate", 30, "Male", "A+", phone, null));
                throw new SQLException("Duplicate phone was incorrectly accepted.");
            } catch (IllegalStateException expected) {
                System.out.println("  DUPLICATE phone: rejected");
            }

            List<Donor> matches = DonorService.search("M4 Test");
            if (matches.isEmpty()) {
                throw new SQLException("Search did not find the new donor.");
            }
            System.out.println("  SEARCH ok - matches = " + matches.size());

            DonorService.update(new Donor(saved.getDonorId(), "M4 Test Donor", 31,
                    "Female", "O+", phone, "Updated address", null));
            Donor reloaded = DonorService.getById(saved.getDonorId()).orElseThrow();
            if (!"Updated address".equals(reloaded.getAddress())) {
                throw new SQLException("Donor update was not persisted.");
            }
            System.out.println("  UPDATE ok");

            // A donation referencing the donor must block deletion (foreign keys).
            try (Connection connection = DatabaseConnection.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO donations (donor_id, donation_date, quantity_ml, "
                                 + "screening_status) VALUES (?, '2026-01-01', 350, 'PENDING')")) {
                statement.setInt(1, saved.getDonorId());
                statement.executeUpdate();
            }
            try {
                DonorService.delete(saved.getDonorId());
                throw new SQLException("Deletion should have been blocked by a donation.");
            } catch (IllegalStateException expected) {
                System.out.println("  FK delete protection: OK");
            }
            deleteTestDonations(saved.getDonorId());
            DonorService.delete(saved.getDonorId());
            System.out.println("  DELETE ok - test data removed");

        } finally {
            // Cleanup even if an assertion above failed.
            deleteTestRows(saved.getDonorId());
        }
        System.out.println("Donor test completed successfully.");
    }

    private static void deleteTestDonations(int donorId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM donations WHERE donor_id = ?")) {
            statement.setInt(1, donorId);
            statement.executeUpdate();
        }
    }

    private static void deleteTestRows(int donorId) throws SQLException {
        deleteTestDonations(donorId);
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM donors WHERE donor_id = ?")) {
            statement.setInt(1, donorId);
            statement.executeUpdate();
        }
    }

    private static void runDonationTest() throws SQLException {
        System.out.println();
        System.out.println("Donation smoke test:");
        cleanupDonationTestData("M5 Test Donor");

        Donor donor = DonorService.add(new Donor("M5 Test Donor", 30, "Male", "A+",
                "5551112222", "Test address"));
        int donorId = donor.getDonorId();
        System.out.println("  DONOR ok - donor_id = " + donorId);

        try {
            LocalDate date = LocalDate.now().minusDays(2);

            Donation pending = DonationService.add(
                    new Donation(donorId, date, 350, "PENDING", "pending test"), null);
            if (countUnits(pending.getDonationId()) != 0) {
                throw new SQLException("A PENDING donation created a blood unit.");
            }
            System.out.println("  PENDING ok - no unit created");

            DonationService.accept(pending.getDonationId(), BloodComponent.PLASMA);
            String plasmaExpiry = firstUnitExpiry(pending.getDonationId());
            String expectedPlasma = date.plusDays(BloodComponent.PLASMA.getShelfLifeDays()).toString();
            if (!expectedPlasma.equals(plasmaExpiry)) {
                throw new SQLException("PLASMA expiry was " + plasmaExpiry
                        + ", expected " + expectedPlasma);
            }
            String acceptedStatus = DonationService.getById(pending.getDonationId())
                    .orElseThrow().getScreeningStatus();
            if (!"ACCEPTED".equals(acceptedStatus)) {
                throw new SQLException("Donation status was not updated to ACCEPTED.");
            }
            System.out.println("  ACCEPT ok - unit expiry = " + plasmaExpiry);

            try {
                DonationService.accept(pending.getDonationId(), BloodComponent.WHOLE_BLOOD);
                throw new SQLException("Duplicate processing was accepted.");
            } catch (IllegalStateException expected) {
                System.out.println("  DUPLICATE process: rejected");
            }

            Donation direct = DonationService.add(new Donation(donorId, date, 300,
                    "ACCEPTED", "accepted at recording"), BloodComponent.RED_CELLS);
            String redExpiry = firstUnitExpiry(direct.getDonationId());
            String expectedRed = date.plusDays(BloodComponent.RED_CELLS.getShelfLifeDays()).toString();
            if (countUnits(direct.getDonationId()) != 1 || !expectedRed.equals(redExpiry)) {
                throw new SQLException("ACCEPTED-on-record did not create the correct unit.");
            }
            System.out.println("  ACCEPTED on record ok - unit expiry = " + redExpiry);

            Donation rejected = DonationService.add(new Donation(donorId, date, 250,
                    "REJECTED", "rejected test"), null);
            if (countUnits(rejected.getDonationId()) != 0) {
                throw new SQLException("A REJECTED donation created a blood unit.");
            }
            try {
                DonationService.accept(rejected.getDonationId(), BloodComponent.WHOLE_BLOOD);
                throw new SQLException("A REJECTED donation was accepted.");
            } catch (IllegalStateException expected) {
                System.out.println("  REJECTED process: rejected");
            }
            System.out.println("  REJECTED ok - no unit created");

            try {
                DonationService.add(new Donation(donorId, LocalDate.now().plusDays(1),
                        350, "PENDING", null), null);
                throw new SQLException("A future donation date was accepted.");
            } catch (IllegalArgumentException expected) {
                System.out.println("  FUTURE date: rejected");
            }

            try {
                DonationService.add(new Donation(donorId, date, 0, "PENDING", null), null);
                throw new SQLException("A zero quantity was accepted.");
            } catch (IllegalArgumentException expected) {
                System.out.println("  INVALID quantity: rejected");
            }

            System.out.println("Donation test completed successfully.");
        } finally {
            deleteDonationTestRows(donorId);
        }
    }

    private static int countUnits(int donationId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM blood_units WHERE donation_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, donationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private static String firstUnitExpiry(int donationId) throws SQLException {
        String sql = "SELECT expiry_date FROM blood_units WHERE donation_id = ? "
                + "ORDER BY unit_id LIMIT 1";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, donationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("No blood unit was created for donation " + donationId);
                }
                return resultSet.getString("expiry_date");
            }
        }
    }

    private static void deleteDonationTestRows(int donorId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM blood_units WHERE donation_id IN "
                             + "(SELECT donation_id FROM donations WHERE donor_id = ?)")) {
            statement.setInt(1, donorId);
            statement.executeUpdate();
        }
        deleteTestDonations(donorId);
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM donors WHERE donor_id = ?")) {
            statement.setInt(1, donorId);
            statement.executeUpdate();
        }
    }

    private static void cleanupDonationTestData(String donorName) throws SQLException {
        List<Integer> donorIds = new ArrayList<>();
        String select = "SELECT donor_id FROM donors WHERE name = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(select)) {
            statement.setString(1, donorName);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    donorIds.add(resultSet.getInt("donor_id"));
                }
            }
        }
        for (int donorId : donorIds) {
            deleteDonationTestRows(donorId);
        }
    }
}
