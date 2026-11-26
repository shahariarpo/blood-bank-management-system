package com.bloodbank.dao;

import com.bloodbank.database.DatabaseConnection;
import com.bloodbank.model.Patient;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Patient persistence with CRUD and search. */
public final class PatientDAO {

    private static final String COLUMNS =
            "patient_id, name, age, gender, blood_group, phone, hospital";

    private PatientDAO() {
        // DAO class - no instances.
    }

    public static List<Patient> findAll() throws SQLException {
        return query("SELECT " + COLUMNS + " FROM patients ORDER BY name, patient_id",
                statement -> { });
    }

    public static Optional<Patient> findById(int patientId) throws SQLException {
        List<Patient> patients = query("SELECT " + COLUMNS + " FROM patients WHERE patient_id = ?",
                statement -> statement.setInt(1, patientId));
        return patients.isEmpty() ? Optional.empty() : Optional.of(patients.get(0));
    }

    public static int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM patients";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    public static List<Patient> search(String keyword) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM patients "
                + "WHERE name LIKE ? OR CAST(patient_id AS TEXT) LIKE ? "
                + "OR phone LIKE ? OR blood_group LIKE ? "
                + "OR COALESCE(hospital, '') LIKE ? "
                + "ORDER BY name, patient_id";
        String pattern = "%" + keyword + "%";
        return query(sql, statement -> {
            for (int i = 1; i <= 5; i++) {
                statement.setString(i, pattern);
            }
        });
    }

    public static int insert(Patient patient) throws SQLException {
        String sql = "INSERT INTO patients (name, age, gender, blood_group, phone, hospital) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {
            setValues(statement, patient);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    public static boolean update(Patient patient) throws SQLException {
        String sql = "UPDATE patients SET name = ?, age = ?, gender = ?, blood_group = ?, "
                + "phone = ?, hospital = ? WHERE patient_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            setValues(statement, patient);
            statement.setInt(7, patient.getPatientId());
            return statement.executeUpdate() > 0;
        }
    }

    public static boolean delete(int patientId) throws SQLException {
        String sql = "DELETE FROM patients WHERE patient_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, patientId);
            return statement.executeUpdate() > 0;
        }
    }

    public static boolean existsByPhone(String phone, int excludedPatientId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM patients WHERE phone = ? AND patient_id <> ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, phone);
            statement.setInt(2, excludedPatientId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    private static void setValues(PreparedStatement statement, Patient patient)
            throws SQLException {
        statement.setString(1, patient.getName());
        statement.setInt(2, patient.getAge());
        statement.setString(3, patient.getGender());
        statement.setString(4, patient.getBloodGroup());
        statement.setString(5, patient.getPhone());
        statement.setString(6, patient.getHospital());
    }

    private interface ParameterSetter {
        void set(PreparedStatement statement) throws SQLException;
    }

    private static List<Patient> query(String sql, ParameterSetter setter) throws SQLException {
        List<Patient> patients = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            setter.set(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    patients.add(new Patient(
                            resultSet.getInt("patient_id"),
                            resultSet.getString("name"),
                            resultSet.getInt("age"),
                            resultSet.getString("gender"),
                            resultSet.getString("blood_group"),
                            resultSet.getString("phone"),
                            resultSet.getString("hospital")));
                }
            }
        }
        return patients;
    }
}
