package com.bloodbank.service;

import com.bloodbank.dao.BloodIssueDAO;
import com.bloodbank.dao.BloodRequestDAO;
import com.bloodbank.dao.BloodReservationDAO;
import com.bloodbank.dao.BloodUnitDAO;
import com.bloodbank.database.DatabaseConnection;
import com.bloodbank.model.BloodRequest;
import com.bloodbank.util.BloodCompatibility;
import com.bloodbank.util.Validators;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Business rules for blood requests, approval reservations and status transitions. */
public final class RequestService {

    public static final List<String> PRIORITIES = List.of("NORMAL", "URGENT", "CRITICAL");
    public static final List<String> STATUSES = List.of(
            "PENDING", "APPROVED", "PARTIALLY_FULFILLED", "FULFILLED", "REJECTED", "CANCELLED");
    public static final List<String> ISSUABLE_STATUSES =
            List.of("APPROVED", "PARTIALLY_FULFILLED");

    private RequestService() {
        // Service class - no instances.
    }

    public static List<BloodRequest> getAll() throws SQLException {
        return BloodRequestDAO.findAll();
    }

    public static List<BloodRequest> search(String keyword) throws SQLException {
        return Validators.isBlank(keyword)
                ? BloodRequestDAO.findAll()
                : BloodRequestDAO.search(keyword.trim());
    }

    public static List<BloodRequest> findByStatus(String status) throws SQLException {
        return BloodRequestDAO.findByStatus(status);
    }

    public static Optional<BloodRequest> getById(int requestId) throws SQLException {
        return BloodRequestDAO.findById(requestId);
    }

    public static Map<String, Integer> getStatusCounts() throws SQLException {
        return BloodRequestDAO.countByStatus();
    }

    public static BloodRequest add(BloodRequest request) throws SQLException {
        validate(request);
        if (PatientService.getById(request.getPatientId()).isEmpty()) {
            throw new IllegalStateException("The selected patient no longer exists.");
        }
        request.setRequestId(BloodRequestDAO.insert(request));
        request.setStatus("PENDING");
        return request;
    }

    /** Approves a PENDING request and reserves FEFO-matched units in one transaction. */
    public static ApprovalResult approve(int requestId) throws SQLException {
        return inTransaction(connection -> {
            BloodRequest request = BloodRequestDAO.findById(connection, requestId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Request not found. Refresh the list."));
            if (!"PENDING".equals(request.getStatus())) {
                throw new IllegalStateException("Only PENDING requests can be approved.");
            }

            LocalDate today = LocalDate.now();
            BloodUnitDAO.expireUnits(connection, today);
            BloodReservationDAO.releaseExpired(connection, today);

            int reserved = ReservationService.reserve(connection, requestId,
                    BloodCompatibility.compatibleDonorGroups(request.getBloodGroup()),
                    request.getQuantity(), today);
            if (!BloodRequestDAO.updateStatus(connection, requestId, "APPROVED")) {
                throw new IllegalStateException("Request no longer exists. Refresh the list.");
            }
            return new ApprovalResult(requestId, request.getQuantity(), reserved);
        });
    }

    /** Rejects a pending or approved request and releases any reservation. */
    public static void reject(int requestId) throws SQLException {
        inTransaction(connection -> {
            BloodRequest request = BloodRequestDAO.findById(connection, requestId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Request not found. Refresh the list."));
            if (!List.of("PENDING", "APPROVED").contains(request.getStatus())) {
                throw new IllegalStateException("This request can no longer be rejected.");
            }
            if (!BloodIssueDAO.findByRequestId(requestId).isEmpty()) {
                throw new IllegalStateException(
                        "A request with issued blood cannot be rejected.");
            }
            BloodReservationDAO.deleteByRequestId(connection, requestId);
            if (!BloodRequestDAO.updateStatus(connection, requestId, "REJECTED")) {
                throw new IllegalStateException("Request no longer exists. Refresh the list.");
            }
            return null;
        });
    }

    /** Cancels a request before it is fully issued and releases any reservation. */
    public static void cancel(int requestId) throws SQLException {
        inTransaction(connection -> {
            BloodRequest request = BloodRequestDAO.findById(connection, requestId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Request not found. Refresh the list."));
            if (!List.of("PENDING", "APPROVED", "PARTIALLY_FULFILLED")
                    .contains(request.getStatus())) {
                throw new IllegalStateException("This request can no longer be cancelled.");
            }
            if (!BloodIssueDAO.findByRequestId(requestId).isEmpty()) {
                throw new IllegalStateException(
                        "A request with issued blood cannot be cancelled.");
            }
            BloodReservationDAO.deleteByRequestId(connection, requestId);
            if (!BloodRequestDAO.updateStatus(connection, requestId, "CANCELLED")) {
                throw new IllegalStateException("Request no longer exists. Refresh the list.");
            }
            return null;
        });
    }

    private static <T> T inTransaction(TransactionAction<T> action) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            boolean oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                T result = action.run(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(oldAutoCommit);
            }
        }
    }

    private static void validate(BloodRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("No request data was provided.");
        }
        if (request.getPatientId() <= 0) {
            throw new IllegalArgumentException("Select a patient.");
        }
        if (!Validators.isValidBloodGroup(request.getBloodGroup())) {
            throw new IllegalArgumentException("Select a valid requested blood group.");
        }
        if (request.getQuantity() < 1 || request.getQuantity() > 1000) {
            throw new IllegalArgumentException("Quantity must be between 1 and 1000 units.");
        }
        if (request.getRequestDate() == null) {
            throw new IllegalArgumentException("Enter a request date.");
        }
        if (request.getRequestDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Request date cannot be in the future.");
        }
        if (!PRIORITIES.contains(request.getPriority())) {
            throw new IllegalArgumentException("Select a valid priority.");
        }
    }

    @FunctionalInterface
    private interface TransactionAction<T> {
        T run(Connection connection) throws SQLException;
    }

    /** Outcome shown to the user after approval. */
    public static final class ApprovalResult {
        private final int requestId;
        private final int requestedUnits;
        private final int reservedUnits;

        private ApprovalResult(int requestId, int requestedUnits, int reservedUnits) {
            this.requestId = requestId;
            this.requestedUnits = requestedUnits;
            this.reservedUnits = reservedUnits;
        }

        public int getRequestId() { return requestId; }
        public int getRequestedUnits() { return requestedUnits; }
        public int getReservedUnits() { return reservedUnits; }
        public boolean isPartial() { return reservedUnits < requestedUnits; }

        public String message() {
            if (reservedUnits == 0) {
                return "Request #" + requestId + " approved, but no compatible units were "
                        + "available to reserve. Issue will fail until stock arrives.";
            }
            if (isPartial()) {
                return "Request #" + requestId + " approved. Reserved " + reservedUnits
                        + " of " + requestedUnits + " requested unit(s).";
            }
            return "Request #" + requestId + " approved. Reserved " + reservedUnits
                    + " unit(s).";
        }
    }
}
