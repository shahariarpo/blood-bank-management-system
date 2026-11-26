package com.bloodbank.service;

import com.bloodbank.dao.DonationDAO;
import com.bloodbank.model.BloodComponent;
import com.bloodbank.model.Donation;
import com.bloodbank.util.Validators;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Business rules for donations and screening decisions. */
public final class DonationService {

    private DonationService() {
        // Service class - no instances.
    }

    public static List<Donation> getAll() throws SQLException {
        return DonationDAO.findAll();
    }

    public static List<Donation> search(String keyword) throws SQLException {
        return Validators.isBlank(keyword)
                ? DonationDAO.findAll()
                : DonationDAO.search(keyword.trim());
    }

    public static List<Donation> findByDonorId(int donorId) throws SQLException {
        return DonationDAO.findByDonorId(donorId);
    }

    public static Optional<Donation> getById(int donationId) throws SQLException {
        return DonationDAO.findById(donationId);
    }

    /**
     * Records a donation. Choosing ACCEPTED also creates the blood unit,
     * using the component's shelf life to calculate expiry.
     */
    public static Donation add(Donation donation, BloodComponent component)
            throws SQLException {
        validate(donation);
        donation.setNotes(Validators.isBlank(donation.getNotes())
                ? null : donation.getNotes().trim());

        int id;
        if ("ACCEPTED".equals(donation.getScreeningStatus())) {
            if (component == null) {
                throw new IllegalArgumentException("Select a component for an accepted donation.");
            }
            id = DonationDAO.insertWithAcceptance(donation, component.name(),
                    expiryDate(donation.getDonationDate(), component));
        } else {
            id = DonationDAO.insert(donation);
        }
        donation.setDonationId(id);
        return donation;
    }

    /** Accepts a PENDING donation exactly once and creates its unit. */
    public static void accept(int donationId, BloodComponent component) throws SQLException {
        if (component == null) {
            throw new IllegalArgumentException("Select a component type.");
        }
        Donation donation = DonationDAO.findById(donationId)
                .orElseThrow(() -> new IllegalStateException("Donation not found."));
        DonationDAO.accept(donationId, component.name(),
                expiryDate(donation.getDonationDate(), component));
    }

    public static void reject(int donationId) throws SQLException {
        if (!DonationDAO.reject(donationId)) {
            throw new IllegalStateException(
                    "Only PENDING donations can be rejected. Refresh the list.");
        }
    }

    private static LocalDate expiryDate(LocalDate collectionDate, BloodComponent component) {
        return collectionDate.plusDays(component.getShelfLifeDays());
    }

    private static void validate(Donation donation) {
        if (donation == null) {
            throw new IllegalArgumentException("No donation data was provided.");
        }
        if (donation.getDonorId() <= 0) {
            throw new IllegalArgumentException("Select a donor.");
        }
        if (donation.getDonationDate() == null) {
            throw new IllegalArgumentException("Enter a donation date.");
        }
        if (donation.getDonationDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Donation date cannot be in the future.");
        }
        if (donation.getQuantityMl() < 1 || donation.getQuantityMl() > 2000) {
            throw new IllegalArgumentException("Quantity must be between 1 and 2000 ml.");
        }
        if (!List.of("PENDING", "ACCEPTED", "REJECTED")
                .contains(donation.getScreeningStatus())) {
            throw new IllegalArgumentException("Select a valid screening status.");
        }
    }
}
