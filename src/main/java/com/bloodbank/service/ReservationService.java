package com.bloodbank.service;

import com.bloodbank.dao.BloodReservationDAO;
import com.bloodbank.dao.BloodUnitDAO;
import com.bloodbank.database.DatabaseConnection;
import com.bloodbank.model.BloodReservation;
import com.bloodbank.model.BloodUnit;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** System-controlled holds created when a request is approved. */
public final class ReservationService {

    /** Derived status shown in the UI; the stored unit status remains AVAILABLE. */
    public static final String STATUS = "RESERVED";

    private ReservationService() {
        // Service class - no instances.
    }

    /** Drops holds whose units expired or left AVAILABLE status. */
    public static void refreshReservations() throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            BloodReservationDAO.releaseExpired(connection, LocalDate.now());
        }
    }

    /** Reserves up to {@code needed} unreserved FEFO units inside the caller's transaction. */
    public static int reserve(Connection connection, int requestId, List<String> compatibleGroups,
                              int needed, LocalDate today) throws SQLException {
        if (needed <= 0 || compatibleGroups.isEmpty()) {
            return 0;
        }
        List<BloodUnit> units = BloodUnitDAO.findAvailableForCompatibleGroups(
                connection, compatibleGroups, needed, today);
        for (BloodUnit unit : units) {
            BloodReservationDAO.insert(connection, requestId, unit.getUnitId());
        }
        return units.size();
    }

    public static void releaseForRequest(int requestId) throws SQLException {
        BloodReservationDAO.deleteByRequestId(requestId);
    }

    public static void releaseForRequest(Connection connection, int requestId)
            throws SQLException {
        BloodReservationDAO.deleteByRequestId(connection, requestId);
    }

    public static void releaseForUnit(Connection connection, int unitId) throws SQLException {
        BloodReservationDAO.deleteByUnitId(connection, unitId);
    }

    public static List<Integer> getUnitIdsForRequest(int requestId) throws SQLException {
        return BloodReservationDAO.findUnitIdsByRequestId(requestId);
    }

    public static List<Integer> getUnitIdsForRequest(Connection connection, int requestId)
            throws SQLException {
        return BloodReservationDAO.findUnitIdsByRequestId(connection, requestId);
    }

    /** unit_id → request_id for Inventory's derived status column. */
    public static Map<Integer, Integer> getRequestIdsByUnit() throws SQLException {
        refreshReservations();
        return BloodReservationDAO.findRequestIdsByUnit();
    }

    /** request_id → reserved unit count for the Requests table. */
    public static Map<Integer, Integer> getCountsByRequest() throws SQLException {
        refreshReservations();
        return BloodReservationDAO.countByRequest();
    }

    public static int count() throws SQLException {
        refreshReservations();
        return BloodReservationDAO.count();
    }

    public static List<BloodReservation> getAll() throws SQLException {
        refreshReservations();
        return BloodReservationDAO.findAll();
    }
}
