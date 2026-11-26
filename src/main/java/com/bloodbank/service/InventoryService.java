package com.bloodbank.service;

import com.bloodbank.dao.BloodUnitDAO;
import com.bloodbank.model.BloodUnit;
import com.bloodbank.util.Validators;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Business rules for stock, expiry monitoring, status changes and FEFO selection. */
public final class InventoryService {

    public static final List<String> STATUSES =
            List.of("AVAILABLE", "ISSUED", "EXPIRED", "QUARANTINED");

    private InventoryService() {
        // Service class - no instances.
    }

    /** Brings stored statuses in line with today's date before any stock is shown. */
    public static void refreshStatuses() throws SQLException {
        BloodUnitDAO.expireUnits(LocalDate.now());
    }

    public static List<BloodUnit> getAll() throws SQLException {
        refreshStatuses();
        return BloodUnitDAO.findAll();
    }

    public static List<BloodUnit> search(String keyword, String bloodGroup, String status)
            throws SQLException {
        refreshStatuses();
        return BloodUnitDAO.search(keyword, bloodGroup, status);
    }

    public static Map<String, Integer> getAvailableCountsByGroup() throws SQLException {
        refreshStatuses();
        return BloodUnitDAO.countAvailableByGroup(LocalDate.now());
    }

    public static Map<String, Integer> getStatusCounts() throws SQLException {
        refreshStatuses();
        return BloodUnitDAO.countByStatus();
    }

    public static List<BloodUnit> findExpiringSoon(int days) throws SQLException {
        if (days < 1 || days > 365) {
            throw new IllegalArgumentException("Expiry window must be between 1 and 365 days.");
        }
        refreshStatuses();
        return BloodUnitDAO.findExpiringSoon(days, LocalDate.now());
    }

    /**
     * Manual status changes are limited to quarantine and release.
     * ISSUED and EXPIRED are controlled by issuing and expiry rules.
     */
    public static void setStatus(int unitId, String status) throws SQLException {
        if (!List.of("AVAILABLE", "QUARANTINED").contains(status)) {
            throw new IllegalArgumentException(
                    "Only AVAILABLE and QUARANTINED can be set manually.");
        }

        BloodUnit unit = BloodUnitDAO.findById(unitId)
                .orElseThrow(() -> new IllegalStateException("Unit not found. Refresh the list."));
        if (isExpired(unit)) {
            throw new IllegalStateException("This unit has expired and cannot be changed manually.");
        }
        if ("ISSUED".equals(unit.getStatus()) || "EXPIRED".equals(unit.getStatus())) {
            throw new IllegalStateException("This unit's status is controlled by the system.");
        }
        if (unit.getStatus().equals(status)) {
            return;
        }
        if (!BloodUnitDAO.updateStatus(unitId, status)) {
            throw new IllegalStateException("Unit no longer exists. Refresh the list.");
        }
    }

    /**
     * FEFO selection for a future request: only unexpired AVAILABLE units.
     * This does not issue anything; Milestone 7 will wrap issuing in a transaction.
     */
    public static List<BloodUnit> findAvailableForIssue(String bloodGroup, int quantity)
            throws SQLException {
        if (!Validators.isValidBloodGroup(bloodGroup)) {
            throw new IllegalArgumentException("Select a valid blood group.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive.");
        }

        refreshStatuses();
        List<BloodUnit> units = BloodUnitDAO.findAvailableForIssue(
                bloodGroup, quantity, LocalDate.now());
        if (units.size() < quantity) {
            throw new IllegalStateException("Only " + units.size() + " available unit(s) of "
                    + bloodGroup + " were found.");
        }
        return units;
    }

    public static boolean isExpired(BloodUnit unit) {
        return unit != null && !unit.getExpiryDate().isAfter(LocalDate.now());
    }
}
