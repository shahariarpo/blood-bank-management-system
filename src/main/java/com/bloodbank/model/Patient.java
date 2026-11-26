package com.bloodbank.model;

/** Patient record used by blood requests. */
public class Patient {

    private int patientId;
    private String name;
    private int age;
    private String gender;
    private String bloodGroup;
    private String phone;
    private String hospital;

    public Patient(String name, int age, String gender, String bloodGroup,
                   String phone, String hospital) {
        this(0, name, age, gender, bloodGroup, phone, hospital);
    }

    public Patient(int patientId, String name, int age, String gender, String bloodGroup,
                   String phone, String hospital) {
        this.patientId = patientId;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.bloodGroup = bloodGroup;
        this.phone = phone;
        this.hospital = hospital;
    }

    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getHospital() { return hospital; }
    public void setHospital(String hospital) { this.hospital = hospital; }
}
