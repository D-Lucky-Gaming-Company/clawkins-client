package github.dluckycompany.clawkins.model;

/**
 * Represents the player character's gender selection.
 * Used during character setup to determine character representation.
 */
public enum Gender {
    MALE("Male"),
    FEMALE("Female");

    private final String displayName;

    Gender(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the human-readable display name for this gender.
     *
     * @return the display name (e.g., "Male" or "Female")
     */
    public String getDisplayName() {
        return displayName;
    }
}
