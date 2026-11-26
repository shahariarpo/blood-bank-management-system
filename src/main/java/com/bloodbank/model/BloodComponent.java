package com.bloodbank.model;

/**
 * Blood components with illustrative shelf lives.
 * Real expiry is decided by the blood bank's laboratory policy.
 */
public enum BloodComponent {

    WHOLE_BLOOD("Whole blood", 35),
    RED_CELLS("Red cells", 42),
    PLASMA("Plasma", 365),
    PLATELETS("Platelets", 5);

    private final String displayName;
    private final int shelfLifeDays;

    BloodComponent(String displayName, int shelfLifeDays) {
        this.displayName = displayName;
        this.shelfLifeDays = shelfLifeDays;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getShelfLifeDays() {
        return shelfLifeDays;
    }

    @Override
    public String toString() {
        return displayName + " (" + shelfLifeDays + " days)";
    }
}
