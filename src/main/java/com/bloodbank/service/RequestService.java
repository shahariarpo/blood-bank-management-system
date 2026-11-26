package com.bloodbank.service;

import com.bloodbank.dao.BloodRequestDAO;
import com.bloodbank.model.BloodRequest;
import com.bloodbank.util.Validators;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Business rules for blood requests and their status transitions. */
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

    public static void approve(int requestId) throws SQLException {
        changeFromPending(requestId, "APPROVED", "approved");
    }

    public static void reject(int requestId) throws SQLException {
        changeFromPending(requestId, "REJECTED", "rejected");
    }

    private static void changeFromPending(int requestId, String target, String action)
            throws SQLException {
        BloodRequest request = BloodRequestDAO.findById(requestId)
                .orElseThrow(() -> new IllegalStateException("Request not found. Refresh the list."));
        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalStateException("Only PENDING requests can be " + action + ".");
        }
        if (!BloodRequestDAO.updateStatus(requestId, target)) {
            throw new IllegalStateException("Request no longer exists. Refresh the list.");
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
}
