# Player Profile Integration - Complete Implementation Handoff

**Date**: 2026-05-11  
**Status**: ✅ Complete and Functional  
**Feature**: PlayerProfile System for Character Name and Gender

---

## Overview

The PlayerProfile system captures the player's chosen character name and gender from the CharacterSetupScreen and makes this information available throughout the entire game. The profile is stored centrally in Main.java and passed to GameScreen, ensuring consistent access to player identity across all game systems.

### Key Features

- **Centralized Storage**: PlayerProfile stored in Main.java for global access
- **Character Setup Integration**: Name and gender captured during character creation
- **GameScreen Integration**: Profile passed to GameScreen constructor
- **Type-Safe Gender Enum**: Male/Female options with display names
- **Save State Ready**: TODO comments for future save/load integration

---

## Architecture

### Core Components

1. **PlayerProfile.java** (`model` package) - Immutable data class storing name and gender
2. **Gender.java** (`model` package) - Enum for gender selection (MALE, FEMALE)
3. **Main.java** - Central storage and management of PlayerProfile
4. **CharacterSetupScreen.java** - Creates and stores PlayerProfile after character creation
5. **GameScreen.java** - Receives and uses PlayerProfile throughout gameplay

### File Locations

```
core/src/main/java/github/dluckycompany/clawkins/
├── model/
│   ├── PlayerProfile.java
│   └── Gender.java
├── ui/
│   └── CharacterSetupScreen.java
├── Main.java
└── GameScreen.java
```

---

## PlayerProfile Class

### Structure

```java
package github.dluckycompany.clawkins.model;

public class PlayerProfile {
    private final String name;
    private final Gender gender;

    public PlayerProfile(String name, Gender gender) {
        this.name = name;
        this.gender = gender;
    }

    public String getName() {
        return name;
    }

    public Gender getGender() {
        return gender;
    }
}
```

### Design Principles

- **Immutable**: Once created, profile cannot be changed (final fields, no setters)
- **Non-null**: Constructor expects non-null, non-empty name and non-null gender
- **Simple**: No complex logic, just data storage
- **Future-Ready**: TODO comments for save state integration

---

## Gender Enum

### Structure

```java
package github.dluckycompany.clawkins.model;

public enum Gender {
    MALE("Male"),
    FEMALE("Female");

    private final String displayName;

    Gender(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
```

### Usage

```java
// In character setup
selectedGender = Gender.MALE;  // or Gender.FEMALE

// Display in UI
String display = selectedGender.getDisplayName();  // "Male" or "Female"

// Save to file
String saved = selectedGender.name();  // "MALE" or "FEMALE"

// Load from file
Gender loaded = Gender.valueOf("MALE");  // Gender.MALE
```

---

## Main.java Integration

### Field Declaration

```java
private PlayerProfile playerProfile;  // Stores player name and gender from character setup
```

### Getter and Setter

```java
/**
 * Gets the current player profile (name and gender from character setup).
 *
 * @return the player profile, or null if not yet created
 */
public PlayerProfile getPlayerProfile() {
    return playerProfile;
}

/**
 * Sets the player profile (called by CharacterSetupScreen after character creation).
 *
 * @param profile the player profile to store
 */
public void setPlayerProfile(PlayerProfile profile) {
    this.playerProfile = profile;
    Gdx.app.log("Main", "Player profile set: " +
        (profile != null ? profile.getName() + " (" + profile.getGender() + ")" : "null"));
}
```

### GameScreen Creation

```java
public void rebuildGameScreenForFreshSession() {
    Screen existingGameScreen = screenCache.remove(GameScreen.class);
    if (existingGameScreen != null) {
        existingGameScreen.dispose();
    }
    // Create new GameScreen with current player profile
    addScreen(new GameScreen(this, playerProfile));
}
```

### Initial Setup

```java
@Override
public void create() {
    // ... other initialization ...

    addScreen(new CharacterSetupScreen(this, batch));
    addScreen(new GameScreen(this, null));  // Initial GameScreen with no profile

    // ... rest of setup ...
}
```

---

## CharacterSetupScreen Integration

### Profile Creation

```java
private void completeSetup() {
    // Get text from nameField
    String enteredName = nameField.getText();

    // Validate name
    if (!validateName(enteredName)) {
        AudioService audio = game.getAudioService();
        if (audio != null) audio.playSound(SoundEffect.UI_ERROR);
        return;
    }

    // Play success sound
    AudioService audio = game.getAudioService();
    if (audio != null) audio.playSound(SoundEffect.UI_SELECT);

    // Trim the name and store in playerName field
    playerName = enteredName.trim();

    // Create new PlayerProfile with playerName and selectedGender
    profile = new PlayerProfile(playerName, selectedGender);

    // Store profile in Main for access throughout the game
    game.setPlayerProfile(profile);

    handleIntroSequenceEvent(profile);

    // Log for debugging
    Gdx.app.log("CharacterSetup", "Character created: " + playerName + " (" + selectedGender + ")");

    // Start welcome transition sequence
    showWelcomeTransition();
}
```

### Transition to GameScreen

```java
private void showWelcomeTransition() {
    // ... fade animations ...

    // Step 5: Start game
    Actions.run(() -> {
        Gdx.app.log("CharacterSetup", "Transition complete, starting game");

        // Rebuild GameScreen with the new player profile
        game.rebuildGameScreenForFreshSession();

        // Get the newly created GameScreen and set the profile
        GameScreen gameScreen = game.getScreen(GameScreen.class);
        gameScreen.setPlayerProfile(profile);

        // Transition to game
        game.setScreen(GameScreen.class);
    })
}
```

---

## GameScreen Integration

### Field Declaration

```java
// Player profile from character setup (name and gender)
private PlayerProfile playerProfile;
```

### Constructor

```java
public GameScreen(Main game, PlayerProfile playerProfile) {
    this.game = game;
    this.playerProfile = playerProfile;
    AssetService assetService = game.getAssetService();
    // ... rest of initialization ...

    // Log player profile information
    if (playerProfile != null) {
        Gdx.app.log("GameScreen", "Initialized with player: " +
            playerProfile.getName() + " (" + playerProfile.getGender().getDisplayName() + ")");
    } else {
        Gdx.app.log("GameScreen", "Initialized without player profile (loading from save or initial screen)");
    }
}
```

### Getter and Setter

```java
/**
 * Sets the player profile created during character setup.
 *
 * @param profile the player profile to set
 */
public void setPlayerProfile(PlayerProfile profile) {
    this.playerProfile = profile;
}

/**
 * Gets the player profile created during character setup.
 *
 * @return the player profile, or null if not set
 */
public PlayerProfile getPlayerProfile() {
    return playerProfile;
}
```

---

## Usage Throughout the Game

### Accessing Player Name

```java
// In any class with access to GameScreen
GameScreen gameScreen = game.getScreen(GameScreen.class);
PlayerProfile profile = gameScreen.getPlayerProfile();

if (profile != null) {
    String playerName = profile.getName();
    // Use player name in dialogue, UI, etc.
}
```

### Accessing Player Gender

```java
// In any class with access to GameScreen
GameScreen gameScreen = game.getScreen(GameScreen.class);
PlayerProfile profile = gameScreen.getPlayerProfile();

if (profile != null) {
    Gender gender = profile.getGender();

    // Use gender for pronoun selection
    String pronoun = (gender == Gender.MALE) ? "he" : "she";
    String possessive = (gender == Gender.MALE) ? "his" : "her";

    // Use gender for sprite selection
    String spritePrefix = (gender == Gender.MALE) ? "player_male" : "player_female";
}
```

### Dialogue System Integration

```java
// Example: Personalized dialogue
String dialogue = "Welcome, " + profile.getName() + "! " +
    "The kingdom needs " + (profile.getGender() == Gender.MALE ? "a hero" : "a heroine") + " like you!";
```

---

## Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│ 1. Main Menu Screen                                         │
│    User clicks "NEW GAME"                                   │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. Character Setup Screen - Gender Selection               │
│    User selects MALE or FEMALE                             │
│    selectedGender = Gender.MALE (or FEMALE)                │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. Character Setup Screen - Name Input                     │
│    User enters name: "Alice"                               │
│    playerName = "Alice"                                    │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. Character Setup Screen - Profile Creation               │
│    profile = new PlayerProfile("Alice", Gender.FEMALE)     │
│    game.setPlayerProfile(profile)                          │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ 5. Main - Rebuild GameScreen                               │
│    game.rebuildGameScreenForFreshSession()                 │
│    new GameScreen(this, playerProfile)                     │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ 6. Character Setup Screen - Set Profile on GameScreen      │
│    gameScreen.setPlayerProfile(profile)                    │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ 7. GameScreen - Profile Available                          │
│    playerProfile = profile                                 │
│    Log: "Initialized with player: Alice (Female)"          │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ 8. Gameplay - Profile Accessible Everywhere                │
│    gameScreen.getPlayerProfile().getName()  → "Alice"      │
│    gameScreen.getPlayerProfile().getGender() → FEMALE      │
└─────────────────────────────────────────────────────────────┘
```

---

## Dialogue System Integration

### Problem

The dialogue system uses `{player}` placeholder in dialogue JSON files, which gets replaced with the player's name. Previously, this name came from the Tiled map's "Name" property (hardcoded as "Jacob" in `test_world.tmx` and `cottage_sample.tmx`).

### Solution

Updated the system to use the player name from character setup instead of the hardcoded map value.

#### Implementation

**1. TiledObjectConfigurator.java**

Added player name override functionality:

```java
private String playerNameOverride;  // Override for player name from character setup

/**
 * Sets the player name override from character setup.
 * This overrides the "Name" property from Tiled maps.
 *
 * @param playerName the player name from character setup, or null to use map default
 */
public void setPlayerNameOverride(String playerName) {
    this.playerNameOverride = playerName;
    Gdx.app.log(TAG, "Player name override set to: " + playerName);
}
```

Updated player entity creation to use override:

```java
// Use player name from character setup if available, otherwise use map property
String playerName = (playerNameOverride != null && !playerNameOverride.isEmpty())
    ? playerNameOverride
    : getStringProperty(tileMapObject, "Name", "Player");

Gdx.app.log(TAG, "Player name: " + playerName +
    (playerNameOverride != null ? " (from character setup)" : " (from map)"));
```

**2. GameScreen.java**

Set the player name override when creating TiledObjectConfigurator:

```java
// Tiled map services
this.tiledService = new TiledService(assetService);
this.tiledObjectConfigurator = new TiledObjectConfigurator(engine, assetService, playerBattleState);

// Set player name from character setup if available
if (playerProfile != null) {
    tiledObjectConfigurator.setPlayerNameOverride(playerProfile.getName());
}
```

#### Flow

```
┌─────────────────────────────────────────────────────────────┐
│ 1. Character Setup                                          │
│    User enters name: "Alice"                                │
│    PlayerProfile created with name "Alice"                  │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. GameScreen Constructor                                   │
│    playerProfile = profile                                  │
│    tiledObjectConfigurator.setPlayerNameOverride("Alice")   │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. Map Loading (TiledObjectConfigurator)                    │
│    PLAYER object found in Tiled map                         │
│    playerName = playerNameOverride ("Alice")                │
│    entity.add(new PlayerProfile("Alice"))                   │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. Dialogue System (InteractionSystem)                      │
│    Reads dialogue JSON: "Hello {player}"                    │
│    Replaces {player} with PlayerProfile component name      │
│    Displays: "Hello Alice"                                  │
└─────────────────────────────────────────────────────────────┘
```

#### Backward Compatibility

- If no PlayerProfile is set (e.g., loading from old save), falls back to Tiled map "Name" property
- Existing maps with "Name" property still work as fallback
- No breaking changes to existing dialogue system

---

## Future Enhancements (TODO)

### Save State Integration

The PlayerProfile class has TODO comments for future save state integration:

```java
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
```

### Implementation Steps for Save/Load

1. **Add fields to SaveState.java**:

   ```java
   private String playerName;
   private String playerGender;  // Store as string: "MALE" or "FEMALE"
   ```

2. **Add getters/setters to SaveState.java**:

   ```java
   public String getPlayerName() { return playerName; }
   public void setPlayerName(String name) { this.playerName = name; }
   public String getPlayerGender() { return playerGender; }
   public void setPlayerGender(String gender) { this.playerGender = gender; }
   ```

3. **Update SaveStateManager to serialize/deserialize**:

   ```java
   // In save method
   saveState.setPlayerName(profile.getName());
   saveState.setPlayerGender(profile.getGender().name());

   // In load method
   PlayerProfile profile = new PlayerProfile(
       saveState.getPlayerName(),
       Gender.valueOf(saveState.getPlayerGender())
   );
   ```

4. **Update GameScreen.applySaveState()**:

   ```java
   private void applySaveState(SaveState saveState) {
       // ... existing code ...

       // Load player profile
       if (saveState.getPlayerName() != null && saveState.getPlayerGender() != null) {
           this.playerProfile = new PlayerProfile(
               saveState.getPlayerName(),
               Gender.valueOf(saveState.getPlayerGender())
           );
       }
   }
   ```

### Additional Gender Options

If more gender options are needed in the future:

```java
public enum Gender {
    MALE("Male"),
    FEMALE("Female"),
    NON_BINARY("Non-Binary"),
    OTHER("Other");

    // ... rest of implementation ...
}
```

Update CharacterSetupScreen to add more buttons for additional options.

---

## Testing Checklist

### Character Creation Flow

- [x] Gender selection screen displays correctly
- [x] Male button selects Gender.MALE
- [x] Female button selects Gender.FEMALE
- [x] Name input screen displays after gender selection
- [x] Name validation works (non-empty, trimmed)
- [x] Default names populate correctly (Mark for Male, Alice for Female)
- [x] Profile created with correct name and gender
- [x] Profile stored in Main.java
- [x] Welcome transition displays player name
- [x] GameScreen receives profile in constructor
- [x] GameScreen logs profile information

### Profile Access

- [x] `game.getPlayerProfile()` returns correct profile
- [x] `gameScreen.getPlayerProfile()` returns correct profile
- [x] Profile name accessible via `profile.getName()`
- [x] Profile gender accessible via `profile.getGender()`
- [x] Gender display name works: `gender.getDisplayName()`

### Edge Cases

- [x] Empty name rejected with error message
- [x] Whitespace-only name rejected
- [x] Name trimmed correctly (leading/trailing spaces removed)
- [x] Profile null-safe (checks before access)
- [x] GameScreen handles null profile gracefully (initial screen, load from save)

---

## Compilation Status

✅ **Successful** - No errors

```
BUILD SUCCESSFUL in 33s
```

---

## Compatibility

- ✅ Works with existing character setup flow
- ✅ Compatible with Main.java screen management
- ✅ Integrates with GameScreen initialization
- ✅ Ready for save/load integration (TODO comments in place)
- ✅ No breaking changes to existing systems

---

## Performance Notes

- Minimal memory footprint (two fields: String + enum)
- No performance impact - simple data storage
- Immutable design prevents accidental modification
- No complex logic or calculations

---

## Code Ownership

**Primary Files**:

- `PlayerProfile.java` - Immutable data class
- `Gender.java` - Gender enum

**Modified Files**:

- `Main.java` - Added playerProfile field and getter/setter
- `CharacterSetupScreen.java` - Creates and stores profile
- `GameScreen.java` - Receives and uses profile

---

## Summary

The PlayerProfile system provides a clean, type-safe way to store and access the player's character name and gender throughout the game. The profile is created during character setup, stored centrally in Main.java, and passed to GameScreen for use in gameplay, dialogue, UI, and other systems.

**Key Achievements**:

- ✅ Centralized player identity storage
- ✅ Type-safe gender enum
- ✅ Immutable profile design
- ✅ Integration with character setup flow
- ✅ GameScreen constructor integration
- ✅ Logging for debugging
- ✅ Ready for save/load integration
- ✅ Clean, maintainable code

**Status**: Production-ready ✅
