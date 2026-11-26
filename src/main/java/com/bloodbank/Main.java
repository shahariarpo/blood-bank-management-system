package com.bloodbank;

import com.bloodbank.dao.AdminDAO;
import com.bloodbank.database.DatabaseConnection;
import com.bloodbank.database.DatabaseInitializer;
import com.bloodbank.model.Admin;
import com.bloodbank.model.BloodComponent;
import com.bloodbank.model.BloodIssue;
import com.bloodbank.model.BloodRequest;
import com.bloodbank.model.BloodUnit;
import com.bloodbank.model.Donation;
import com.bloodbank.model.Donor;
import com.bloodbank.model.Patient;
import com.bloodbank.service.AuthService;
import com.bloodbank.service.BackupService;
import com.bloodbank.service.DonationService;
import com.bloodbank.service.DonorService;
import com.bloodbank.service.InventoryService;
import com.bloodbank.service.IssueService;
import com.bloodbank.service.PatientService;
import com.bloodbank.service.ReportService;
import com.bloodbank.service.RequestService;
import com.bloodbank.service.ReservationService;
import com.bloodbank.ui.LoginFrame;
import com.bloodbank.util.BloodCompatibility;
import com.bloodbank.util.Validators;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Application entry point.
 * Console smoke tests: --test-auth, --test-donors, --test-donations,
 * --test-inventory, --test-requests, --test-reports, --test-reservations.
 */
public class Main {

    private static final String TEST_ARG = "--test-auth";
    private static final String DONOR_TEST_ARG = "--test-donors";
    private static final String DONATION_TEST_ARG = "--test-donations";
    private static final String INVENTORY_TEST_ARG = "--test-inventory";
    private static final String REQUEST_TEST_ARG = "--test-requests";
    private static final String REPORT_TEST_ARG = "--test-reports";
    private static final String RESERVATION_TEST_ARG = "--test-reservations";

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
            if (args.length > 0 && INVENTORY_TEST_ARG.equals(args[0])) {
                runInventoryTest();
                return;
            }
            if (args.length > 0 && REQUEST_TEST_ARG.equals(args[0])) {
                runRequestTest();
                return;
            }
            if (args.length > 0 && REPORT_TEST_ARG.equals(args[0])) {
                runReportTest();
                return;
            }
            if (args.length > 0 && RESERVATION_TEST_ARG.equals(args[0])) {
                runReservationTest();
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
                    || DONATION_TEST_ARG.equals(args[0])
                    || INVENTORY_TEST_ARG.equals(args[0])
                    || REQUEST_TEST_ARG.equals(args[0])
                    || REPORT_TEST_ARG.equals(args[0])
                    || RESERVATION_TEST_ARG.equals(args[0]));
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
                     "DELETE FROM blood_reservations WHERE unit_id IN "
                             + "(SELECT bu.unit_id FROM blood_units bu "
                             + "JOIN donations d ON bu.donation_id = d.donation_id "
                             + "WHERE d.donor_id = ?)")) {
            statement.setInt(1, donorId);
            statement.executeUpdate();
        }
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

    private static void runInventoryTest() throws SQLException {
        System.out.println();
        System.out.println("Inventory smoke test:");
        cleanupDonationTestData("M6 Test Donor");

        // Use a group with no existing units so the test never depends on user data.
        String testGroup = null;
        for (Map.Entry<String, Integer> entry
                : InventoryService.getAvailableCountsByGroup().entrySet()) {
            if (entry.getValue() == 0) {
                testGroup = entry.getKey();
                break;
            }
        }
        if (testGroup == null) {
            throw new SQLException("Every blood group already has available units. "
                    + "The inventory test needs at least one empty group.");
        }

        Donor donor = DonorService.add(new Donor("M6 Test Donor", 30, "Female", testGroup,
                "5552223333", "Test address"));
        int donorId = donor.getDonorId();
        System.out.println("  DONOR ok - donor_id = " + donorId + ", group = " + testGroup);

        try {
            LocalDate today = LocalDate.now();
            Donation older = DonationService.add(new Donation(donorId, today.minusDays(10),
                    350, "ACCEPTED", "older plasma"), BloodComponent.PLASMA);
            Donation newer = DonationService.add(new Donation(donorId, today.minusDays(2),
                    350, "ACCEPTED", "newer plasma"), BloodComponent.PLASMA);
            Donation expired = DonationService.add(new Donation(donorId, today.minusDays(40),
                    350, "ACCEPTED", "expired whole blood"), BloodComponent.WHOLE_BLOOD);

            InventoryService.refreshStatuses();
            List<BloodUnit> fefo = InventoryService.findAvailableForIssue(testGroup, 1);
            if (fefo.size() != 1 || fefo.get(0).getDonationId() != older.getDonationId()) {
                throw new SQLException("FEFO did not select the earliest-expiring unit.");
            }
            System.out.println("  FEFO ok - oldest expiring unit selected");

            Map<String, Integer> counts = InventoryService.getAvailableCountsByGroup();
            if (counts.get(testGroup) != 2) {
                throw new SQLException("Available counts were not derived from unit rows.");
            }
            System.out.println("  STOCK by group ok - " + testGroup + " available = "
                    + counts.get(testGroup));

            List<BloodUnit> expiredUnits = InventoryService.search(
                    String.valueOf(expired.getDonationId()), testGroup, "EXPIRED");
            if (expiredUnits.size() != 1
                    || expiredUnits.get(0).getDonationId() != expired.getDonationId()) {
                throw new SQLException("Expired unit was not flagged automatically.");
            }
            System.out.println("  EXPIRY detection ok - expired unit flagged");

            int unitId = fefo.get(0).getUnitId();
            InventoryService.setStatus(unitId, "QUARANTINED");
            List<BloodUnit> afterQuarantine = InventoryService.findAvailableForIssue(testGroup, 1);
            if (afterQuarantine.size() != 1
                    || afterQuarantine.get(0).getDonationId() != newer.getDonationId()) {
                throw new SQLException("Quarantined unit was still offered for issuing.");
            }
            System.out.println("  QUARANTINE ok - unit excluded from FEFO");

            InventoryService.setStatus(unitId, "AVAILABLE");
            if (InventoryService.findAvailableForIssue(testGroup, 2).size() != 2) {
                throw new SQLException("Released unit was not available again.");
            }
            System.out.println("  RELEASE ok - unit available again");

            List<BloodUnit> byId = InventoryService.search(
                    String.valueOf(unitId), testGroup, "AVAILABLE");
            if (byId.isEmpty() || byId.get(0).getUnitId() != unitId) {
                throw new SQLException("Unit ID filter did not find the unit.");
            }
            System.out.println("  SEARCH ok - unit ID filter");

            try {
                InventoryService.findAvailableForIssue(testGroup, 3);
                throw new SQLException("Insufficient stock was not detected.");
            } catch (IllegalStateException expected) {
                System.out.println("  INSUFFICIENT stock: rejected");
            }

            try {
                InventoryService.setStatus(unitId, "ISSUED");
                throw new SQLException("Manual ISSUED status change was accepted.");
            } catch (IllegalArgumentException expected) {
                System.out.println("  INVALID status change: rejected");
            }

            List<BloodUnit> both = InventoryService.findAvailableForIssue(testGroup, 2);
            if (both.size() != 2
                    || both.get(0).getDonationId() != older.getDonationId()
                    || both.get(1).getDonationId() != newer.getDonationId()) {
                throw new SQLException("FEFO order across both units was incorrect.");
            }
            System.out.println("Inventory test completed successfully.");
        } finally {
            deleteDonationTestRows(donorId);
        }
    }

    private static void runRequestTest() throws SQLException {
        System.out.println();
        System.out.println("Request and issuing smoke test:");
        cleanupRequestTestData();

        if (!BloodCompatibility.compatibleDonorGroups("O-").equals(List.of("O-"))
                || !BloodCompatibility.compatibleDonorGroups("A+")
                        .containsAll(List.of("A+", "A-", "O+", "O-"))
                || BloodCompatibility.compatibleDonorGroups("A+").contains("B+")) {
            throw new SQLException("Illustrative ABO/Rh compatibility rules are incorrect.");
        }
        System.out.println("  COMPAT ok - illustrative ABO/Rh rules");

        // Choose a patient group whose compatible donor groups currently have no units.
        String patientGroup = null;
        Map<String, Integer> counts = InventoryService.getAvailableCountsByGroup();
        for (String candidate : Validators.BLOOD_GROUPS) {
            boolean empty = true;
            for (String donorGroup : BloodCompatibility.compatibleDonorGroups(candidate)) {
                if (counts.getOrDefault(donorGroup, 0) > 0) {
                    empty = false;
                    break;
                }
            }
            if (empty) {
                patientGroup = candidate;
                break;
            }
        }
        if (patientGroup == null) {
            throw new SQLException("Every patient group has compatible available units. "
                    + "The request test needs at least one empty compatibility set.");
        }

        Patient patient = PatientService.add(new Patient("M7 Test Patient", 40, "Female",
                patientGroup, "5553334444", "Test Hospital"));
        int donorId = 0;
        System.out.println("  PATIENT ok - patient_id = " + patient.getPatientId()
                + ", group = " + patientGroup);

        try {
            BloodRequest blocked = RequestService.add(new BloodRequest(
                    patient.getPatientId(), patientGroup, 2, LocalDate.now(), "NORMAL"));
            try {
                PatientService.delete(patient.getPatientId());
                throw new SQLException("Patient deletion should have been blocked by a request.");
            } catch (IllegalStateException expected) {
                System.out.println("  FK delete protection: OK");
            }

            RequestService.reject(blocked.getRequestId());
            try {
                RequestService.approve(blocked.getRequestId());
                throw new SQLException("A REJECTED request was approved.");
            } catch (IllegalStateException expected) {
                System.out.println("  STATUS guard: rejected request cannot be approved");
            }

            Donor donor = DonorService.add(new Donor("M7 Test Donor", 30, "Male", patientGroup,
                    "5554445555", "Test address"));
            donorId = donor.getDonorId();
            LocalDate collectionDate = LocalDate.now().minusDays(2);
            DonationService.add(new Donation(donorId, collectionDate, 350,
                    "ACCEPTED", "fefo red cells"), BloodComponent.RED_CELLS);
            DonationService.add(new Donation(donorId, collectionDate, 350,
                    "ACCEPTED", "later plasma"), BloodComponent.PLASMA);
            System.out.println("  UNITS ok - donor_id = " + donorId);

            BloodRequest full = RequestService.add(new BloodRequest(
                    patient.getPatientId(), patientGroup, 1, LocalDate.now(), "URGENT"));
            RequestService.approve(full.getRequestId());
            int fulfilledBeforeIssue = ReportService.getDashboardData().getFulfilledRequests();
            IssueService.IssueResult fullResult = IssueService.issue(full.getRequestId(), 1, false);
            String fullStatus = RequestService.getById(full.getRequestId())
                    .orElseThrow().getStatus();
            if (fullResult.getIssuedUnits() != 1 || !"FULFILLED".equals(fullStatus)) {
                throw new SQLException("Full issue did not fulfill the request.");
            }
            BloodIssue firstIssue = IssueService.getIssuingHistory().stream()
                    .filter(issue -> issue.getRequestId() == full.getRequestId())
                    .findFirst()
                    .orElseThrow(() -> new SQLException("Issuing history was not recorded."));
            if (!"RED_CELLS".equals(firstIssue.getComponentType())) {
                throw new SQLException("FEFO did not select the earliest-expiring unit.");
            }
            int fulfilledAfterIssue = ReportService.getDashboardData().getFulfilledRequests();
            if (fulfilledAfterIssue != fulfilledBeforeIssue + 1) {
                throw new SQLException("Dashboard fulfilled count did not reflect the issue.");
            }
            System.out.println("  FULL ISSUE ok - FEFO selected RED_CELLS");
            System.out.println("  DASHBOARD fulfilled count ok - " + fulfilledBeforeIssue
                    + " -> " + fulfilledAfterIssue);

            int issuesAfterFull = countIssuesForRequest(full.getRequestId());
            try {
                IssueService.issue(full.getRequestId(), 1, false);
                throw new SQLException("A fulfilled request was issued again.");
            } catch (IllegalStateException expected) {
                System.out.println("  DUPLICATE issue: rejected");
            }
            if (countIssuesForRequest(full.getRequestId()) != issuesAfterFull) {
                throw new SQLException("A duplicate issue wrote an extra blood_issues row.");
            }

            BloodRequest partial = RequestService.add(new BloodRequest(
                    patient.getPatientId(), patientGroup, 2, LocalDate.now(), "CRITICAL"));
            RequestService.approve(partial.getRequestId());
            try {
                IssueService.issue(partial.getRequestId(), 2, false);
                throw new SQLException("Insufficient stock was issued without partial consent.");
            } catch (IllegalStateException expected) {
                System.out.println("  PARTIAL guard: rejected");
            }
            if (countIssuesForRequest(partial.getRequestId()) != 0) {
                throw new SQLException("Rejected partial issue wrote a blood_issues row.");
            }

            IssueService.IssueResult partialResult =
                    IssueService.issue(partial.getRequestId(), 2, true);
            String partialStatus = RequestService.getById(partial.getRequestId())
                    .orElseThrow().getStatus();
            if (partialResult.getIssuedUnits() != 1
                    || !"PARTIALLY_FULFILLED".equals(partialStatus)) {
                throw new SQLException("Partial issue did not mark the request PARTIALLY_FULFILLED.");
            }
            System.out.println("  PARTIAL ISSUE ok - 1 unit issued, request partially fulfilled");

            int issuesAfterPartial = countIssuesForRequest(partial.getRequestId());
            try {
                IssueService.issue(partial.getRequestId(), 1, false);
                throw new SQLException("Issuing from depleted compatible stock was accepted.");
            } catch (IllegalStateException expected) {
                System.out.println("  DEPLETED stock: rejected");
            }
            if (countIssuesForRequest(partial.getRequestId()) != issuesAfterPartial) {
                throw new SQLException("A rejected issue wrote an extra blood_issues row.");
            }

            System.out.println("Request and issuing test completed successfully.");
        } finally {
            deleteRequestTestRows(patient.getPatientId());
            if (donorId > 0) {
                deleteDonationTestRows(donorId);
            }
        }
    }

    private static int countIssuesForRequest(int requestId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM blood_issues WHERE request_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, requestId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private static void deleteRequestTestRows(int patientId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM blood_reservations WHERE request_id IN "
                             + "(SELECT request_id FROM blood_requests WHERE patient_id = ?)")) {
            statement.setInt(1, patientId);
            statement.executeUpdate();
        }
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM blood_issues WHERE request_id IN "
                             + "(SELECT request_id FROM blood_requests WHERE patient_id = ?)")) {
            statement.setInt(1, patientId);
            statement.executeUpdate();
        }
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM blood_requests WHERE patient_id = ?")) {
            statement.setInt(1, patientId);
            statement.executeUpdate();
        }
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM patients WHERE patient_id = ?")) {
            statement.setInt(1, patientId);
            statement.executeUpdate();
        }
    }

    private static void cleanupRequestTestData() throws SQLException {
        List<Integer> patientIds = new ArrayList<>();
        String select = "SELECT patient_id FROM patients WHERE name = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(select)) {
            statement.setString(1, "M7 Test Patient");
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    patientIds.add(resultSet.getInt("patient_id"));
                }
            }
        }
        for (int patientId : patientIds) {
            deleteRequestTestRows(patientId);
        }
        cleanupDonationTestData("M7 Test Donor");
    }

    private static void runReportTest() throws SQLException, IOException {
        System.out.println();
        System.out.println("Report and backup smoke test:");
        cleanupReportTestData();

        Map<String, Integer> counts = InventoryService.getAvailableCountsByGroup();
        String testGroup = null;
        for (String group : Validators.BLOOD_GROUPS) {
            if (counts.getOrDefault(group, 0) == 0) {
                testGroup = group;
                break;
            }
        }
        if (testGroup == null) {
            throw new SQLException("Every blood group has available stock. "
                    + "The report test needs at least one empty group.");
        }

        ReportService.DashboardData before = ReportService.getDashboardData();
        int donorsBefore = before.getTotalDonors();
        int availableBefore = before.getAvailableUnits();
        int fulfilledBefore = before.getFulfilledRequests();

        Donor donor = DonorService.add(new Donor("M8 Test Donor", 30, "Female", testGroup,
                "5555556666", "Test address"));
        int donorId = donor.getDonorId();
        int patientId = 0;
        Path backup = null;

        try {
            Donation donation = DonationService.add(new Donation(donorId,
                    LocalDate.now().minusDays(1), 350, "ACCEPTED", "report test"),
                    BloodComponent.RED_CELLS);
            ReportService.DashboardData afterDonation = ReportService.getDashboardData();
            if (afterDonation.getTotalDonors() != donorsBefore + 1
                    || afterDonation.getAvailableUnits() != availableBefore + 1
                    || afterDonation.getInventoryByGroup().get(testGroup) != 1) {
                throw new SQLException("Dashboard did not reflect the new donor and unit.");
            }
            System.out.println("  DASHBOARD donor/stock ok - group = " + testGroup);

            Patient patient = PatientService.add(new Patient("M8 Test Patient", 35, "Male",
                    testGroup, "5556667777", "Test Hospital"));
            patientId = patient.getPatientId();
            BloodRequest request = RequestService.add(new BloodRequest(patientId, testGroup,
                    1, LocalDate.now(), "NORMAL"));
            RequestService.approve(request.getRequestId());
            IssueService.issue(request.getRequestId(), 1, false);

            ReportService.DashboardData afterIssue = ReportService.getDashboardData();
            if (afterIssue.getFulfilledRequests() != fulfilledBefore + 1) {
                throw new SQLException("Fulfilled request count was not updated.");
            }
            boolean recentDonation = afterIssue.getRecentDonations().stream()
                    .anyMatch(item -> item.getDonationId() == donation.getDonationId());
            boolean recentRequest = afterIssue.getRecentRequests().stream()
                    .anyMatch(item -> item.getRequestId() == request.getRequestId());
            if (!recentDonation || !recentRequest
                    || afterIssue.getRecentDonations().size() > 5
                    || afterIssue.getRecentRequests().size() > 5) {
                throw new SQLException("Recent activity is missing or exceeds the 5-row limit.");
            }
            System.out.println("  DASHBOARD requests/recent ok");

            ReportService.ReportData report = ReportService.getReportData();
            int stockTotal = 0;
            for (Integer value : report.getInventoryByGroup().values()) {
                stockTotal += value;
            }
            if (report.getTotalDonors() != afterIssue.getTotalDonors()
                    || report.getIssuedUnits() < 1
                    || stockTotal != afterIssue.getAvailableUnits()
                    || report.getDonationHistory().size() < 1
                    || report.getIssuingHistory().size() < 1) {
                throw new SQLException("Report figures are inconsistent with dashboard figures.");
            }
            System.out.println("  REPORT totals/history ok");

            backup = BackupService.createTimestampedBackup(null);
            int tables = BackupService.verifyBackup(backup);
            int donorsInBackup = BackupService.countInBackup(backup, "donors");
            if (tables != 8 || donorsInBackup != afterIssue.getTotalDonors()) {
                throw new SQLException("Backup verification failed.");
            }
            System.out.println("  BACKUP ok - " + backup.getFileName() + ", tables = " + tables);

            System.out.println("Report and backup test completed successfully.");
        } finally {
            if (backup != null) {
                Files.deleteIfExists(backup);
            }
            if (patientId > 0) {
                deleteRequestTestRows(patientId);
            }
            deleteDonationTestRows(donorId);
        }
    }

    private static void cleanupReportTestData() throws SQLException {
        List<Integer> donorIds = new ArrayList<>();
        String donorSelect = "SELECT donor_id FROM donors WHERE name = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(donorSelect)) {
            statement.setString(1, "M8 Test Donor");
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    donorIds.add(resultSet.getInt("donor_id"));
                }
            }
        }
        for (int donorId : donorIds) {
            deleteDonationTestRows(donorId);
        }

        List<Integer> patientIds = new ArrayList<>();
        String patientSelect = "SELECT patient_id FROM patients WHERE name = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(patientSelect)) {
            statement.setString(1, "M8 Test Patient");
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    patientIds.add(resultSet.getInt("patient_id"));
                }
            }
        }
        for (int patientId : patientIds) {
            deleteRequestTestRows(patientId);
        }
    }

    private static void runReservationTest() throws SQLException {
        System.out.println();
        System.out.println("Reservation smoke test:");
        cleanupReservationTestData();
        int reservationsBefore = ReservationService.count();

        String testGroup = null;
        Map<String, Integer> counts = InventoryService.getAvailableCountsByGroup();
        for (String candidate : Validators.BLOOD_GROUPS) {
            boolean empty = true;
            for (String donorGroup : BloodCompatibility.compatibleDonorGroups(candidate)) {
                if (counts.getOrDefault(donorGroup, 0) > 0) {
                    empty = false;
                    break;
                }
            }
            if (empty) {
                testGroup = candidate;
                break;
            }
        }
        if (testGroup == null) {
            throw new SQLException("Every patient group has compatible available units. "
                    + "The reservation test needs at least one empty compatibility set.");
        }

        Donor donor = DonorService.add(new Donor("M9 Test Donor", 30, "Female", testGroup,
                "5557778888", "Test address"));
        int donorId = donor.getDonorId();
        int patientId = 0;

        try {
            DonationService.add(new Donation(donorId, LocalDate.now().minusDays(1), 350,
                    "ACCEPTED", "reservation test"), BloodComponent.RED_CELLS);
            Patient patient = PatientService.add(new Patient("M9 Test Patient", 35, "Male",
                    testGroup, "5558889999", "Test Hospital"));
            patientId = patient.getPatientId();

            int before = availableForGroup(testGroup);
            BloodRequest first = RequestService.add(new BloodRequest(
                    patientId, testGroup, 1, LocalDate.now(), "URGENT"));
            RequestService.ApprovalResult firstApproval = RequestService.approve(first.getRequestId());
            int reservedUnitId = ReservationService.getUnitIdsForRequest(first.getRequestId())
                    .get(0);
            if (firstApproval.getReservedUnits() != 1
                    || ReservationService.count() != reservationsBefore + 1
                    || availableForGroup(testGroup) != before - 1
                    || ReservationService.getRequestIdsByUnit().get(reservedUnitId)
                    != first.getRequestId()) {
                throw new SQLException("Approval did not reserve and hide a compatible unit.");
            }
            System.out.println("  RESERVE ok - available " + before + " -> "
                    + availableForGroup(testGroup) + ", group = " + testGroup);

            BloodRequest second = RequestService.add(new BloodRequest(
                    patientId, testGroup, 1, LocalDate.now(), "NORMAL"));
            RequestService.ApprovalResult secondApproval =
                    RequestService.approve(second.getRequestId());
            if (secondApproval.getReservedUnits() != 0 || !secondApproval.isPartial()
                    || !ReservationService.getUnitIdsForRequest(second.getRequestId()).isEmpty()) {
                throw new SQLException("A reserved unit was offered to a second request.");
            }
            System.out.println("  DOUBLE RESERVE blocked - partial approval warning returned");

            IssueService.issue(first.getRequestId(), 1, false);
            if (ReservationService.count() != reservationsBefore
                    || !"ISSUED".equals(unitStatus(reservedUnitId))
                    || availableForGroup(testGroup) != before - 1) {
                throw new SQLException("Issuing did not consume the reservation.");
            }
            System.out.println("  ISSUE ok - reservation consumed, unit marked ISSUED");

            DonationService.add(new Donation(donorId, LocalDate.now().minusDays(1), 350,
                    "ACCEPTED", "release test"), BloodComponent.PLASMA);
            BloodRequest rejected = RequestService.add(new BloodRequest(
                    patientId, testGroup, 1, LocalDate.now(), "NORMAL"));
            RequestService.approve(rejected.getRequestId());
            int beforeReject = availableForGroup(testGroup);
            RequestService.reject(rejected.getRequestId());
            if (ReservationService.count() != reservationsBefore
                    || availableForGroup(testGroup) != beforeReject + 1) {
                throw new SQLException("Rejecting did not release the reservation.");
            }
            System.out.println("  REJECT ok - reservation released to AVAILABLE");

            DonationService.add(new Donation(donorId, LocalDate.now().minusDays(1), 350,
                    "ACCEPTED", "expiry test"), BloodComponent.RED_CELLS);
            BloodRequest expired = RequestService.add(new BloodRequest(
                    patientId, testGroup, 1, LocalDate.now(), "NORMAL"));
            RequestService.approve(expired.getRequestId());
            int expiringUnitId = ReservationService.getUnitIdsForRequest(
                    expired.getRequestId()).get(0);
            try (Connection connection = DatabaseConnection.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "UPDATE blood_units SET expiry_date = ? WHERE unit_id = ?")) {
                statement.setString(1, LocalDate.now().toString());
                statement.setInt(2, expiringUnitId);
                statement.executeUpdate();
            }
            InventoryService.refreshStatuses();
            if (ReservationService.count() != reservationsBefore
                    || !"EXPIRED".equals(unitStatus(expiringUnitId))) {
                throw new SQLException("Expiry did not release the reservation.");
            }
            System.out.println("  EXPIRY ok - expired reservation released");

            DonationService.add(new Donation(donorId, LocalDate.now().minusDays(1), 350,
                    "ACCEPTED", "cancel test"), BloodComponent.PLASMA);
            BloodRequest cancelled = RequestService.add(new BloodRequest(
                    patientId, testGroup, 1, LocalDate.now(), "NORMAL"));
            RequestService.approve(cancelled.getRequestId());
            RequestService.cancel(cancelled.getRequestId());
            if (ReservationService.count() != reservationsBefore
                    || !"CANCELLED".equals(RequestService.getById(cancelled.getRequestId())
                    .orElseThrow().getStatus())) {
                throw new SQLException("Cancelling did not release the reservation.");
            }
            System.out.println("  CANCEL ok - reservation released, request CANCELLED");

            System.out.println("Reservation test completed successfully.");
        } finally {
            deleteRequestTestRows(patientId);
            deleteDonationTestRows(donorId);
        }
    }

    private static int availableForGroup(String bloodGroup) throws SQLException {
        return InventoryService.getAvailableCountsByGroup().getOrDefault(bloodGroup, 0);
    }

    private static String unitStatus(int unitId) throws SQLException {
        String sql = "SELECT status FROM blood_units WHERE unit_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, unitId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Unit " + unitId + " was not found.");
                }
                return resultSet.getString("status");
            }
        }
    }

    private static void cleanupReservationTestData() throws SQLException {
        for (int donorId : findIds("donors", "donor_id", "M9 Test Donor")) {
            deleteDonationTestRows(donorId);
        }
        for (int patientId : findIds("patients", "patient_id", "M9 Test Patient")) {
            deleteRequestTestRows(patientId);
        }
    }

    private static List<Integer> findIds(String table, String idColumn, String name)
            throws SQLException {
        String sql = "SELECT " + idColumn + " FROM " + table + " WHERE name = ?";
        List<Integer> ids = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ids.add(resultSet.getInt(idColumn));
                }
            }
        }
        return ids;
    }
}
