package com.bloodbank.service;

import com.bloodbank.dao.DonorDAO;
import com.bloodbank.model.Donor;
import com.bloodbank.util.Validators;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Business rules for donors: validation, duplicates and friendly errors. */
public final class DonorService {

    private DonorService() {
        // Service class - no instances.
    }

    public static List<Donor> getAll() throws SQLException {
        return DonorDAO.findAll();
    }

    public static List<Donor> search(String keyword) throws SQLException {
        return Validators.isBlank(keyword)
                ? DonorDAO.findAll()
                : DonorDAO.search(keyword.trim());
    }

    public static Optional<Donor> getById(int donorId) throws SQLException {
        return DonorDAO.findById(donorId);
    }

    public static Donor add(Donor donor) throws SQLException {
        validate(donor);
        checkDuplicatePhone(donor.getPhone(), 0);

        int id = DonorDAO.insert(donor);
        donor.setDonorId(id);
        return donor;
    }

    public static Donor update(Donor donor) throws SQLException {
        validate(donor);
        checkDuplicatePhone(donor.getPhone(), donor.getDonorId());

        if (!DonorDAO.update(donor)) {
            throw new IllegalStateException("Donor no longer exists. Refresh the list and try again.");
        }
        return donor;
    }

    public static void delete(int donorId) throws SQLException {
        try {
            if (!DonorDAO.delete(donorId)) {
                throw new IllegalStateException("Donor no longer exists. Refresh the list.");
            }
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("FOREIGN KEY")) {
                throw new IllegalStateException(
                        "Cannot delete this donor because donation records still reference them.",
                        e);
            }
            throw e;
        }
    }

    private static void validate(Donor donor) {
        if (donor == null) {
            throw new IllegalArgumentException("No donor data was provided.");
        }
        donor.setName(donor.getName() == null ? "" : donor.getName().trim());
        donor.setGender(donor.getGender() == null ? "" : donor.getGender().trim());
        donor.setBloodGroup(donor.getBloodGroup() == null ? "" : donor.getBloodGroup().trim());
        donor.setPhone(normalizePhone(donor.getPhone()));
        donor.setAddress(Validators.isBlank(donor.getAddress())
                ? null : donor.getAddress().trim());

        if (Validators.isBlank(donor.getName()) || donor.getName().length() > 100) {
            throw new IllegalArgumentException("Name is required (maximum 100 characters).");
        }
        if (donor.getAge() < 18 || donor.getAge() > 100) {
            throw new IllegalArgumentException("Age must be between 18 and 100.");
        }
        if (!List.of("Male", "Female", "Other").contains(donor.getGender())) {
            throw new IllegalArgumentException("Select a valid gender.");
        }
        if (!Validators.isValidBloodGroup(donor.getBloodGroup())) {
            throw new IllegalArgumentException("Select a valid blood group.");
        }
        if (!donor.getPhone().matches("[0-9]{7,15}")) {
            throw new IllegalArgumentException("Phone must contain 7 to 15 digits.");
        }
    }

    private static void checkDuplicatePhone(String phone, int excludedId) throws SQLException {
        if (DonorDAO.existsByPhone(phone, excludedId)) {
            throw new IllegalStateException("Another donor is already registered with this phone number.");
        }
    }

    private static String normalizePhone(String phone) {
        return phone == null ? "" : phone.replaceAll("[\\s()+-]", "").trim();
    }
}
