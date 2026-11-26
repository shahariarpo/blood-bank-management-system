package com.bloodbank.model;

/** Represents the single administrator account. */
public class Admin {

    private int adminId;
    private String username;
    private String passwordHash;

    public Admin(int adminId, String username, String passwordHash) {
        this.adminId = adminId;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public int getAdminId() {
        return adminId;
    }

    public void setAdminId(int adminId) {
        this.adminId = adminId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /** Never include the password hash in toString - it could leak into logs. */
    @Override
    public String toString() {
        return "Admin{adminId=" + adminId + ", username='" + username + "'}";
    }
}
