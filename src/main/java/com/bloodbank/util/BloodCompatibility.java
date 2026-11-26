package com.bloodbank.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Illustrative ABO/Rh compatibility for demo selection only.
 * This is NOT clinical authorization: real transfusion requires laboratory
 * crossmatching, antibody screening and clinical approval.
 */
public final class BloodCompatibility {

    private static final Map<String, List<String>> ABO_DONORS_FOR_RECIPIENT = Map.of(
            "O", List.of("O"),
            "A", List.of("A", "O"),
            "B", List.of("B", "O"),
            "AB", List.of("AB", "A", "B", "O"));

    private BloodCompatibility() {
        // Utility class - no instances.
    }

    /** Donor groups that may be considered for the given patient group, in FEFO selection. */
    public static List<String> compatibleDonorGroups(String patientBloodGroup) {
        if (!Validators.isValidBloodGroup(patientBloodGroup)) {
            throw new IllegalArgumentException("Select a valid patient blood group.");
        }

        String patientAbo = aboPart(patientBloodGroup);
        boolean patientPositive = patientBloodGroup.endsWith("+");
        List<String> result = new ArrayList<>();

        for (String donorGroup : Validators.BLOOD_GROUPS) {
            String donorAbo = aboPart(donorGroup);
            boolean donorPositive = donorGroup.endsWith("+");
            boolean aboOk = ABO_DONORS_FOR_RECIPIENT.get(patientAbo).contains(donorAbo);
            boolean rhOk = patientPositive || !donorPositive;
            if (aboOk && rhOk) {
                result.add(donorGroup);
            }
        }
        return List.copyOf(result);
    }

    public static boolean canDonateTo(String donorBloodGroup, String patientBloodGroup) {
        return compatibleDonorGroups(patientBloodGroup).contains(donorBloodGroup);
    }

    private static String aboPart(String bloodGroup) {
        return bloodGroup.substring(0, bloodGroup.length() - 1);
    }
}
