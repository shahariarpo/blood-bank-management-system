package com.bloodbank.dao;

import com.bloodbank.database.DatabaseConnection;
import com.bloodbank.model.Donation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Read-only donation lookup used by donor profiles; expanded in Milestone 5. */
public final class DonationDAO {

    private DonationDAO() {
        // DAO class - no instances.
    }

    public static List<Donation> findByDonorId(int donorId) throws SQLException {
        String sql = "SELECT donation_id, donor_id, donation_date, quantity_ml, "
                + "screening_status, notes FROM donations WHERE donor_id = ? "
                + "ORDER BY donation_date DESC, donation_id DESC";
        List<Donation> donations = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, donorId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    donations.add(new Donation(
                            resultSet.getInt("donation_id"),
                            resultSet.getInt("donor_id"),
                            LocalDate.parse(resultSet.getString("donation_date")),
                            resultSet.getInt("quantity_ml"),
                            resultSet.getString("screening_status"),
                            resultSet.getString("notes")));
                }
            }
        }
        return donations;
    }
}
