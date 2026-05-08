package github.dluckycompany.clawkins.model;

/**
 * Stores player profile information including character name and gender.
 * This data model is created during character setup and accessible throughout the game.
 */
public class PlayerProfile {
    private final String name;
    private final Gender gender;

    /**
     * Creates a new player profile with the specified name and gender.
     *
     * @param name the player's chosen character name (non-null, non-empty after trim)
     * @param gender the player's chosen character gender (non-null)
     */
    public PlayerProfile(String name, Gender gender) {
        this.name = name;
        this.gender = gender;
    }

    /**
     * Returns the player's character name.
     *
     * @return the character name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the player's character gender.
     *
     * @return the character gender
     */
    public Gender getGender() {
        return gender;
    }

    // TODO: Future save state integration
    // When player metadata is added to SaveState, implement these methods:
    //
    // /**
    //  * Populates the given save state with player profile data.
    //  *
    //  * @param saveState the save state to populate
    //  */
    // public void populateSaveState(SaveState saveState) {
    //     saveState.setPlayerName(name);
    //     saveState.setPlayerGender(gender.name());
    // }
    //
    // /**
    //  * Creates a PlayerProfile from save state data.
    //  *
    //  * @param saveState the save state to load from
    //  * @return a new PlayerProfile instance
    //  */
    // public static PlayerProfile fromSaveState(SaveState saveState) {
    //     return new PlayerProfile(
    //         saveState.getPlayerName(),
    //         Gender.valueOf(saveState.getPlayerGender())
    //     );
    // }
}
