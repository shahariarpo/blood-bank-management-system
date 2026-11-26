package com.bloodbank.model;

import java.time.LocalDate;

/** One individual unit created from an accepted donation. */
public class BloodUnit {

    private int unitId;
    private int donationId;
    private String bloodGroup;
    private String componentType;
    private LocalDate collectionDate;
    private LocalDate expiryDate;
    private String status;

    public BloodUnit(int unitId, int donationId, String bloodGroup, String componentType,
                     LocalDate collectionDate, LocalDate expiryDate, String status) {
        this.unitId = unitId;
        this.donationId = donationId;
        this.bloodGroup = bloodGroup;
        this.componentType = componentType;
        this.collectionDate = collectionDate;
        this.expiryDate = expiryDate;
        this.status = status;
    }

    public int getUnitId() { return unitId; }
    public int getDonationId() { return donationId; }
    public String getBloodGroup() { return bloodGroup; }
    public String getComponentType() { return componentType; }
    public LocalDate getCollectionDate() { return collectionDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public String getStatus() { return status; }
}
