package com.bloodbank.model;

import java.time.LocalDate;

/** Represents one recorded blood donation. */
public class Donation {

    private int donationId;
    private int donorId;
    private String donorName;
    private String bloodGroup;
    private LocalDate donationDate;
    private int quantityMl;
    private String screeningStatus;
    private String notes;

    /** Used by the donor profile's read-only history view. */
    public Donation(int donationId, int donorId, LocalDate donationDate,
                    int quantityMl, String screeningStatus, String notes) {
        this(donationId, donorId, null, null, donationDate,
                quantityMl, screeningStatus, notes);
    }

    /** Full constructor used by joined DAO queries. */
    public Donation(int donationId, int donorId, String donorName, String bloodGroup,
                    LocalDate donationDate, int quantityMl,
                    String screeningStatus, String notes) {
        this.donationId = donationId;
        this.donorId = donorId;
        this.donorName = donorName;
        this.bloodGroup = bloodGroup;
        this.donationDate = donationDate;
        this.quantityMl = quantityMl;
        this.screeningStatus = screeningStatus;
        this.notes = notes;
    }

    /** Constructor for a donation that has not been saved yet. */
    public Donation(int donorId, LocalDate donationDate, int quantityMl,
                    String screeningStatus, String notes) {
        this(0, donorId, null, null, donationDate, quantityMl, screeningStatus, notes);
    }

    public int getDonationId() { return donationId; }
    public void setDonationId(int donationId) { this.donationId = donationId; }

    public int getDonorId() { return donorId; }
    public void setDonorId(int donorId) { this.donorId = donorId; }

    public String getDonorName() { return donorName; }
    public void setDonorName(String donorName) { this.donorName = donorName; }

    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }

    public LocalDate getDonationDate() { return donationDate; }
    public void setDonationDate(LocalDate donationDate) { this.donationDate = donationDate; }

    public int getQuantityMl() { return quantityMl; }
    public void setQuantityMl(int quantityMl) { this.quantityMl = quantityMl; }

    public String getScreeningStatus() { return screeningStatus; }
    public void setScreeningStatus(String screeningStatus) {
        this.screeningStatus = screeningStatus;
    }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
