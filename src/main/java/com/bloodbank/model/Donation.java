package com.bloodbank.model;

import java.time.LocalDate;

/**
 * Minimal donation model used by the donor profile's history table.
 * Milestone 5 will expand this for full donation management.
 */
public class Donation {

    private int donationId;
    private int donorId;
    private LocalDate donationDate;
    private int quantityMl;
    private String screeningStatus;
    private String notes;

    public Donation(int donationId, int donorId, LocalDate donationDate,
                    int quantityMl, String screeningStatus, String notes) {
        this.donationId = donationId;
        this.donorId = donorId;
        this.donationDate = donationDate;
        this.quantityMl = quantityMl;
        this.screeningStatus = screeningStatus;
        this.notes = notes;
    }

    public int getDonationId() {
        return donationId;
    }

    public int getDonorId() {
        return donorId;
    }

    public LocalDate getDonationDate() {
        return donationDate;
    }

    public int getQuantityMl() {
        return quantityMl;
    }

    public String getScreeningStatus() {
        return screeningStatus;
    }

    public String getNotes() {
        return notes;
    }
}
