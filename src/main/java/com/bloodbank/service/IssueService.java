package com.bloodbank.service;

import com.bloodbank.dao.BloodIssueDAO;
import com.bloodbank.dao.BloodRequestDAO;
import com.bloodbank.dao.BloodReservationDAO;
import com.bloodbank.dao.BloodUnitDAO;
import com.bloodbank.database.DatabaseConnection;
import com.bloodbank.model.BloodIssue;
import com.bloodbank.model.BloodRequest;
import com.bloodbank.model.BloodUnit;
import com.bloodbank.util.BloodCompatibility;
import com.bloodbank.util.Validators;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Transactional issuing: reserved units are consumed first, then unreserved compatible
 * units are selected FEFO. Units, blood_issues rows, reservations and request status
 * all commit together or roll back together.
 *
 * Compatibility is illustrative only; it is not clinical authorization.
 */
public final class IssueService {

    private IssueService() {
        // Service class - no instances.
    }

    /** Preview of eligible units. Reserved units for this request come first. */
    public static List<BloodUnit> findEligibleUnits(int requestId) throws SQLException {
        BloodRequest request = BloodRequestDAO.findById(requestId)
                .orElseThrow(() -> new IllegalStateException("Request not found."));
        if (!RequestService.ISSUABLE_STATUSES.contains(request.getStatus())) {
            throw new IllegalStateException(
                    "Only APPROVED or partially fulfilled requests can be issued.");
        }

        InventoryService.refreshStatuses();
        LocalDate today = LocalDate.now();
        List<String> compatibleGroups =
                BloodCompatibility.compatibleDonorGroups(request.getBloodGroup());
        List<BloodUnit> units = new ArrayList<>();

        for (int unitId : ReservationService.getUnitIdsForRequest(requestId)) {
            BloodUnit unit = BloodUnitDAO.findById(unitId).orElse(null);
            if (unit != null && "AVAILABLE".equals(unit.getStatus())
                    && unit.getExpiryDate().isAfter(today)
                    && compatibleGroups.contains(unit.getBloodGroup())) {
                units.add(unit);
            }
        }
        units.sort(Comparator.comparing(BloodUnit::getExpiryDate)
                .thenComparingInt(BloodUnit::getUnitId));
        units.addAll(findEligibleUnits(request.getBloodGroup()));
        return units;
    }

    /** Preview of unreserved, unexpired compatible units. No database changes. */
    public static List<BloodUnit> findEligibleUnits(String bloodGroup) throws SQLException {
        if (!Validators.isValidBloodGroup(bloodGroup)) {
            throw new IllegalArgumentException("Select a valid blood group.");
        }
        InventoryService.refreshStatuses();
        List<String> compatibleGroups = BloodCompatibility.compatibleDonorGroups(bloodGroup);
        if (compatibleGroups.isEmpty()) {
            return List.of();
        }
        return BloodUnitDAO.findAvailableForCompatibleGroups(
                compatibleGroups, 1000, LocalDate.now());
    }

    /**
     * Issues up to {@code quantity} units for a request.
     * If fewer eligible units exist, partial issue must be explicitly allowed.
     */
    public static IssueResult issue(int requestId, int quantity, boolean allowPartial)
            throws SQLException {
        if (requestId <= 0) {
            throw new IllegalArgumentException("Select a request.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity to issue must be positive.");
        }

        try (Connection connection = DatabaseConnection.getConnection()) {
            boolean oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                LocalDate today = LocalDate.now();
                BloodUnitDAO.expireUnits(connection, today);
                BloodReservationDAO.releaseExpired(connection, today);

                BloodRequest request = BloodRequestDAO.findById(connection, requestId)
                        .orElseThrow(() -> new IllegalStateException(
                                "Request not found. Refresh the list."));
                if (!RequestService.ISSUABLE_STATUSES.contains(request.getStatus())) {
                    throw new IllegalStateException(
                            "Only APPROVED or partially fulfilled requests can be issued.");
                }

                int alreadyIssued = BloodIssueDAO.findByRequestId(requestId).size();
                int remaining = request.getQuantity() - alreadyIssued;
                if (remaining <= 0) {
                    throw new IllegalStateException("This request is already fully issued.");
                }
                int needed = Math.min(quantity, remaining);

                List<String> compatibleGroups =
                        BloodCompatibility.compatibleDonorGroups(request.getBloodGroup());
                List<BloodUnit> units = collectUnits(connection, requestId, compatibleGroups,
                        needed, today);

                if (units.isEmpty()) {
                    throw new IllegalStateException(
                            "No compatible, unexpired AVAILABLE units are available.");
                }
                if (units.size() < needed && !allowPartial) {
                    throw new IllegalStateException("Only " + units.size()
                            + " compatible unit(s) are available. Confirm a partial issue to continue.");
                }

                for (BloodUnit unit : units) {
                    if (unit.getExpiryDate().isBefore(today) || unit.getExpiryDate().isEqual(today)) {
                        throw new IllegalStateException("A selected unit has expired. Refresh the list.");
                    }
                    if (!BloodUnitDAO.updateStatus(connection, unit.getUnitId(), "ISSUED")) {
                        throw new IllegalStateException("Unit " + unit.getUnitId()
                                + " is no longer available. The issue was rolled back.");
                    }
                    BloodReservationDAO.deleteByUnitId(connection, unit.getUnitId());
                    BloodIssueDAO.insert(connection,
                            new BloodIssue(requestId, unit.getUnitId(), today));
                }

                int totalAfterIssue = alreadyIssued + units.size();
                String finalStatus = totalAfterIssue >= request.getQuantity()
                        ? "FULFILLED" : "PARTIALLY_FULFILLED";
                if (!BloodRequestDAO.updateStatus(connection, requestId, finalStatus)) {
                    throw new IllegalStateException("Request no longer exists. Refresh the list.");
                }

                connection.commit();
                return new IssueResult(units.size(), finalStatus, units);
            } catch (SQLException | IllegalStateException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(oldAutoCommit);
            }
        }
    }

    /** Reserved units first, then unreserved FEFO units to fill the remaining quantity. */
    private static List<BloodUnit> collectUnits(Connection connection, int requestId,
                                                 List<String> compatibleGroups, int needed,
                                                 LocalDate today) throws SQLException {
        List<BloodUnit> units = new ArrayList<>();
        for (int unitId : BloodReservationDAO.findUnitIdsByRequestId(connection, requestId)) {
            BloodUnit unit = BloodUnitDAO.findById(connection, unitId).orElse(null);
            if (unit != null && "AVAILABLE".equals(unit.getStatus())
                    && unit.getExpiryDate().isAfter(today)
                    && compatibleGroups.contains(unit.getBloodGroup())) {
                units.add(unit);
            }
            if (units.size() >= needed) {
                break;
            }
        }
        units.sort(Comparator.comparing(BloodUnit::getExpiryDate)
                .thenComparingInt(BloodUnit::getUnitId));

        int remaining = needed - units.size();
        if (remaining > 0 && !compatibleGroups.isEmpty()) {
            units.addAll(BloodUnitDAO.findAvailableForCompatibleGroups(
                    connection, compatibleGroups, remaining, today));
        }
        return units;
    }

    public static List<BloodIssue> getIssuingHistory() throws SQLException {
        return BloodIssueDAO.findAll();
    }

    /** Result returned after a successful issue. */
    public static final class IssueResult {
        private final int issuedUnits;
        private final String requestStatus;
        private final List<BloodUnit> units;

        private IssueResult(int issuedUnits, String requestStatus, List<BloodUnit> units) {
            this.issuedUnits = issuedUnits;
            this.requestStatus = requestStatus;
            this.units = units;
        }

        public int getIssuedUnits() { return issuedUnits; }
        public String getRequestStatus() { return requestStatus; }
        public List<BloodUnit> getUnits() { return units; }
    }
}
