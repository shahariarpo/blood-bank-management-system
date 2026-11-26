package com.bloodbank.service;

import com.bloodbank.dao.PatientDAO;
import com.bloodbank.model.Patient;
import com.bloodbank.util.Validators;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Business rules for patient records. */
public final class PatientService {

    private PatientService() {
        // Service class - no instances.
    }

    public static List<Patient> getAll() throws SQLException {
        return PatientDAO.findAll();
    }

    public static List<Patient> search(String keyword) throws SQLException {
        return Validators.isBlank(keyword)
                ? PatientDAO.findAll()
                : PatientDAO.search(keyword.trim());
    }

    public static Optional<Patient> getById(int patientId) throws SQLException {
        return PatientDAO.findById(patientId);
    }

    public static Patient add(Patient patient) throws SQLException {
        validate(patient);
        checkDuplicatePhone(patient.getPhone(), 0);
        patient.setPatientId(PatientDAO.insert(patient));
        return patient;
    }

    public static Patient update(Patient patient) throws SQLException {
        validate(patient);
        checkDuplicatePhone(patient.getPhone(), patient.getPatientId());
        if (!PatientDAO.update(patient)) {
            throw new IllegalStateException("Patient no longer exists. Refresh the list.");
        }
        return patient;
    }

    public static void delete(int patientId) throws SQLException {
        try {
            if (!PatientDAO.delete(patientId)) {
                throw new IllegalStateException("Patient no longer exists. Refresh the list.");
            }
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("FOREIGN KEY")) {
                throw new IllegalStateException(
                        "Cannot delete this patient because blood requests still reference them.",
                        e);
            }
            throw e;
        }
    }

    private static void validate(Patient patient) {
        if (patient == null) {
            throw new IllegalArgumentException("No patient data was provided.");
        }
        patient.setName(patient.getName() == null ? "" : patient.getName().trim());
        patient.setGender(patient.getGender() == null ? "" : patient.getGender().trim());
        patient.setBloodGroup(patient.getBloodGroup() == null ? "" : patient.getBloodGroup().trim());
        patient.setPhone(normalizePhone(patient.getPhone()));
        patient.setHospital(Validators.isBlank(patient.getHospital())
                ? null : patient.getHospital().trim());

        if (Validators.isBlank(patient.getName()) || patient.getName().length() > 100) {
            throw new IllegalArgumentException("Name is required (maximum 100 characters).");
        }
        if (patient.getAge() < 0 || patient.getAge() > 120) {
            throw new IllegalArgumentException("Age must be between 0 and 120.");
        }
        if (!List.of("Male", "Female", "Other").contains(patient.getGender())) {
            throw new IllegalArgumentException("Select a valid gender.");
        }
        if (!Validators.isValidBloodGroup(patient.getBloodGroup())) {
            throw new IllegalArgumentException("Select a valid blood group.");
        }
        if (!patient.getPhone().matches("[0-9]{7,15}")) {
            throw new IllegalArgumentException("Phone must contain 7 to 15 digits.");
        }
    }

    private static void checkDuplicatePhone(String phone, int excludedPatientId)
            throws SQLException {
        if (PatientDAO.existsByPhone(phone, excludedPatientId)) {
            throw new IllegalStateException(
                    "Another patient is already registered with this phone number.");
        }
    }

    private static String normalizePhone(String phone) {
        return phone == null ? "" : phone.replaceAll("[\\s()+-]", "").trim();
    }
}
