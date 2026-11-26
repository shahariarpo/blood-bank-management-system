package com.bloodbank.model;

import java.time.LocalDate;

/** One blood unit reserved for a request after approval. */
public class BloodReservation {

    private final int reservationId;
    private final int requestId;
    private final int unitId;
    private final String reservedOn;
    private final String patientName;
    private final String bloodGroup;
    private final String componentType;
    private final LocalDate expiryDate;

    public BloodReservation(int reservationId, int requestId, int unitId, String reservedOn,
                            String patientName, String bloodGroup, String componentType,
                            LocalDate expiryDate) {
        this.reservationId = reservationId;
        this.requestId = requestId;
        this.unitId = unitId;
        this.reservedOn = reservedOn;
        this.patientName = patientName;
        this.bloodGroup = bloodGroup;
        this.componentType = componentType;
        this.expiryDate = expiryDate;
    }

    public int getReservationId() { return reservationId; }
    public int getRequestId() { return requestId; }
    public int getUnitId() { return unitId; }
    public String getReservedOn() { return reservedOn; }
    public String getPatientName() { return patientName; }
    public String getBloodGroup() { return bloodGroup; }
    public String getComponentType() { return componentType; }
    public LocalDate getExpiryDate() { return expiryDate; }
}
