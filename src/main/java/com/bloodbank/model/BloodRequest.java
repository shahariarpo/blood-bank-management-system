package com.bloodbank.model;

import java.time.LocalDate;

/** One blood request raised for a patient. */
public class BloodRequest {

    private int requestId;
    private int patientId;
    private String patientName;
    private String patientBloodGroup;
    private String bloodGroup;
    private int quantity;
    private LocalDate requestDate;
    private String priority;
    private String status;

    /** Used for a request that has not been saved yet. */
    public BloodRequest(int patientId, String bloodGroup, int quantity,
                        LocalDate requestDate, String priority) {
        this(0, patientId, null, null, bloodGroup, quantity, requestDate, priority, "PENDING");
    }

    /** Full constructor used by joined DAO queries. */
    public BloodRequest(int requestId, int patientId, String patientName,
                        String patientBloodGroup, String bloodGroup, int quantity,
                        LocalDate requestDate, String priority, String status) {
        this.requestId = requestId;
        this.patientId = patientId;
        this.patientName = patientName;
        this.patientBloodGroup = patientBloodGroup;
        this.bloodGroup = bloodGroup;
        this.quantity = quantity;
        this.requestDate = requestDate;
        this.priority = priority;
        this.status = status;
    }

    public int getRequestId() { return requestId; }
    public void setRequestId(int requestId) { this.requestId = requestId; }

    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }

    public String getPatientName() { return patientName; }
    public String getPatientBloodGroup() { return patientBloodGroup; }

    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public LocalDate getRequestDate() { return requestDate; }
    public void setRequestDate(LocalDate requestDate) { this.requestDate = requestDate; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
