package com.bloodbank.model;

import java.time.LocalDate;

/** One issued blood unit linked to a request. */
public class BloodIssue {

    private int issueId;
    private int requestId;
    private int unitId;
    private LocalDate issueDate;
    private String patientName;
    private String bloodGroup;
    private String componentType;

    public BloodIssue(int requestId, int unitId, LocalDate issueDate) {
        this(0, requestId, unitId, issueDate, null, null, null);
    }

    /** Full constructor used by joined issuing-history queries. */
    public BloodIssue(int issueId, int requestId, int unitId, LocalDate issueDate,
                      String patientName, String bloodGroup, String componentType) {
        this.issueId = issueId;
        this.requestId = requestId;
        this.unitId = unitId;
        this.issueDate = issueDate;
        this.patientName = patientName;
        this.bloodGroup = bloodGroup;
        this.componentType = componentType;
    }

    public int getIssueId() { return issueId; }
    public int getRequestId() { return requestId; }
    public int getUnitId() { return unitId; }
    public LocalDate getIssueDate() { return issueDate; }
    public String getPatientName() { return patientName; }
    public String getBloodGroup() { return bloodGroup; }
    public String getComponentType() { return componentType; }
}
