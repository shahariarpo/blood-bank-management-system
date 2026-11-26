package com.bloodbank.service;

import com.bloodbank.dao.BloodRequestDAO;
import com.bloodbank.dao.BloodUnitDAO;
import com.bloodbank.dao.DonationDAO;
import com.bloodbank.dao.DonorDAO;
import com.bloodbank.dao.PatientDAO;
import com.bloodbank.model.BloodIssue;
import com.bloodbank.model.BloodRequest;
import com.bloodbank.model.BloodReservation;
import com.bloodbank.model.Donation;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates live figures for the dashboard and reports. Every value comes from
 * SQLite through the DAO layer; there are no hardcoded or cached counters.
 */
public final class ReportService {

    private ReportService() {
        // Service class - no instances.
    }

    public static DashboardData getDashboardData() throws SQLException {
        InventoryService.refreshStatuses();

        Map<String, Integer> inventory = BloodUnitDAO.countAvailableByGroup(LocalDate.now());
        int availableUnits = inventory.values().stream().mapToInt(Integer::intValue).sum();
        Map<String, Integer> unitStatuses = BloodUnitDAO.countByStatus();
        Map<String, Integer> requestStatuses = BloodRequestDAO.countByStatus();

        return new DashboardData(
                DonorDAO.count(),
                availableUnits,
                requestStatuses.get("PENDING"),
                requestStatuses.get("FULFILLED"),
                unitStatuses.get("EXPIRED"),
                inventory,
                DonationDAO.findRecent(5),
                BloodRequestDAO.findRecent(5));
    }

    public static ReportData getReportData() throws SQLException {
        InventoryService.refreshStatuses();

        Map<String, Integer> inventory = BloodUnitDAO.countAvailableByGroup(LocalDate.now());
        int availableUnits = inventory.values().stream().mapToInt(Integer::intValue).sum();
        int reservedUnits = ReservationService.count();

        Map<String, Integer> unitStatuses = new LinkedHashMap<>(BloodUnitDAO.countByStatus());
        unitStatuses.put("AVAILABLE", availableUnits);
        unitStatuses.put("RESERVED", reservedUnits);
        Map<String, Integer> requestStatuses = BloodRequestDAO.countByStatus();
        List<Donation> donations = DonationDAO.findAll();

        return new ReportData(
                DonorDAO.count(),
                PatientDAO.count(),
                donations.size(),
                countWithStatus(donations, "ACCEPTED"),
                unitStatuses.get("ISSUED"),
                unitStatuses.get("EXPIRED"),
                unitStatuses.get("QUARANTINED"),
                reservedUnits,
                requestStatuses.get("PENDING"),
                requestStatuses.get("FULFILLED"),
                requestStatuses.get("PARTIALLY_FULFILLED"),
                requestStatuses.get("REJECTED"),
                inventory,
                unitStatuses,
                requestStatuses,
                donations,
                BloodRequestDAO.findAll(),
                IssueService.getIssuingHistory(),
                ReservationService.getAll());
    }

    private static int countWithStatus(List<Donation> donations, String status) {
        int total = 0;
        for (Donation donation : donations) {
            if (status.equals(donation.getScreeningStatus())) {
                total++;
            }
        }
        return total;
    }

    /** Figures shown on the dashboard cards and tables. */
    public static final class DashboardData {
        private final int totalDonors;
        private final int availableUnits;
        private final int pendingRequests;
        private final int fulfilledRequests;
        private final int expiredUnits;
        private final Map<String, Integer> inventoryByGroup;
        private final List<Donation> recentDonations;
        private final List<BloodRequest> recentRequests;

        private DashboardData(int totalDonors, int availableUnits, int pendingRequests,
                              int fulfilledRequests, int expiredUnits,
                              Map<String, Integer> inventoryByGroup,
                              List<Donation> recentDonations, List<BloodRequest> recentRequests) {
            this.totalDonors = totalDonors;
            this.availableUnits = availableUnits;
            this.pendingRequests = pendingRequests;
            this.fulfilledRequests = fulfilledRequests;
            this.expiredUnits = expiredUnits;
            this.inventoryByGroup = inventoryByGroup;
            this.recentDonations = recentDonations;
            this.recentRequests = recentRequests;
        }

        public int getTotalDonors() { return totalDonors; }
        public int getAvailableUnits() { return availableUnits; }
        public int getPendingRequests() { return pendingRequests; }
        public int getFulfilledRequests() { return fulfilledRequests; }
        public int getExpiredUnits() { return expiredUnits; }
        public Map<String, Integer> getInventoryByGroup() { return inventoryByGroup; }
        public List<Donation> getRecentDonations() { return recentDonations; }
        public List<BloodRequest> getRecentRequests() { return recentRequests; }
    }

    /** Complete data set used by the reports screen. */
    public static final class ReportData {
        private final int totalDonors;
        private final int totalPatients;
        private final int totalDonations;
        private final int acceptedDonations;
        private final int issuedUnits;
        private final int expiredUnits;
        private final int quarantinedUnits;
        private final int reservedUnits;
        private final int pendingRequests;
        private final int fulfilledRequests;
        private final int partiallyFulfilledRequests;
        private final int rejectedRequests;
        private final Map<String, Integer> inventoryByGroup;
        private final Map<String, Integer> unitStatuses;
        private final Map<String, Integer> requestStatuses;
        private final List<Donation> donationHistory;
        private final List<BloodRequest> requestHistory;
        private final List<BloodIssue> issuingHistory;
        private final List<BloodReservation> reservations;

        private ReportData(int totalDonors, int totalPatients, int totalDonations,
                           int acceptedDonations, int issuedUnits, int expiredUnits,
                           int quarantinedUnits, int reservedUnits, int pendingRequests,
                           int fulfilledRequests, int partiallyFulfilledRequests,
                           int rejectedRequests,
                           Map<String, Integer> inventoryByGroup,
                           Map<String, Integer> unitStatuses,
                           Map<String, Integer> requestStatuses,
                           List<Donation> donationHistory, List<BloodRequest> requestHistory,
                           List<BloodIssue> issuingHistory,
                           List<BloodReservation> reservations) {
            this.totalDonors = totalDonors;
            this.totalPatients = totalPatients;
            this.totalDonations = totalDonations;
            this.acceptedDonations = acceptedDonations;
            this.issuedUnits = issuedUnits;
            this.expiredUnits = expiredUnits;
            this.quarantinedUnits = quarantinedUnits;
            this.reservedUnits = reservedUnits;
            this.pendingRequests = pendingRequests;
            this.fulfilledRequests = fulfilledRequests;
            this.partiallyFulfilledRequests = partiallyFulfilledRequests;
            this.rejectedRequests = rejectedRequests;
            this.inventoryByGroup = inventoryByGroup;
            this.unitStatuses = unitStatuses;
            this.requestStatuses = requestStatuses;
            this.donationHistory = donationHistory;
            this.requestHistory = requestHistory;
            this.issuingHistory = issuingHistory;
            this.reservations = reservations;
        }

        public int getTotalDonors() { return totalDonors; }
        public int getTotalPatients() { return totalPatients; }
        public int getTotalDonations() { return totalDonations; }
        public int getAcceptedDonations() { return acceptedDonations; }
        public int getIssuedUnits() { return issuedUnits; }
        public int getExpiredUnits() { return expiredUnits; }
        public int getQuarantinedUnits() { return quarantinedUnits; }
        public int getReservedUnits() { return reservedUnits; }
        public int getPendingRequests() { return pendingRequests; }
        public int getFulfilledRequests() { return fulfilledRequests; }
        public int getPartiallyFulfilledRequests() { return partiallyFulfilledRequests; }
        public int getRejectedRequests() { return rejectedRequests; }
        public Map<String, Integer> getInventoryByGroup() { return inventoryByGroup; }
        public Map<String, Integer> getUnitStatuses() { return unitStatuses; }
        public Map<String, Integer> getRequestStatuses() { return requestStatuses; }
        public List<Donation> getDonationHistory() { return donationHistory; }
        public List<BloodRequest> getRequestHistory() { return requestHistory; }
        public List<BloodIssue> getIssuingHistory() { return issuingHistory; }
        public List<BloodReservation> getReservations() { return reservations; }
    }
}
