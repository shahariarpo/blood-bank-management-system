package com.bloodbank.model;

/** Represents a blood donor. */
public class Donor {

    private int donorId;
    private String name;
    private int age;
    private String gender;
    private String bloodGroup;
    private String phone;
    private String address;
    private String createdAt;

    /** Constructor for a new donor before it has an ID. */
    public Donor(String name, int age, String gender, String bloodGroup,
                 String phone, String address) {
        this(0, name, age, gender, bloodGroup, phone, address, null);
    }

    /** Constructor for a donor loaded from the database. */
    public Donor(int donorId, String name, int age, String gender, String bloodGroup,
                 String phone, String address, String createdAt) {
        this.donorId = donorId;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.bloodGroup = bloodGroup;
        this.phone = phone;
        this.address = address;
        this.createdAt = createdAt;
    }

    public int getDonorId() {
        return donorId;
    }

    public void setDonorId(int donorId) {
        this.donorId = donorId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
