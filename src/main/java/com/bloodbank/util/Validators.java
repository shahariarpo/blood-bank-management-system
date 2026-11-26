package com.bloodbank.util;

import java.util.List;

/** Small, reusable validation helpers shared by services and UI forms. */
public final class Validators {

    public static final List<String> BLOOD_GROUPS =
            List.of("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");

    private Validators() {
        // Utility class - no instances.
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /** 3-30 letters, digits or underscores. */
    public static boolean isValidUsername(String username) {
        return !isBlank(username) && username.trim().matches("[A-Za-z0-9_]{3,30}");
    }

    /** Minimum 6 characters for a password. */
    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    public static boolean isValidBloodGroup(String bloodGroup) {
        return BLOOD_GROUPS.contains(bloodGroup);
    }
}
