# Requirements Document

## Introduction

This document specifies the requirements for implementing a character setup flow in the Clawkin game. The feature introduces a two-step character creation process (gender selection and name input) that occurs between the main menu and game start. This replaces the current behavior where pressing "New Game" immediately spawns the player in the Cottage.

## Glossary

- **Main_Menu_Screen**: The initial screen displayed when the game launches, containing NEW GAME, CONTINUE, and EXIT GAME buttons
- **Character_Setup_Screen**: A new screen that handles character gender selection and name input
- **Game_Screen**: The main gameplay screen where the player controls their character in the game world
- **Player_Profile**: A data model containing the player's chosen name and gender
- **Gender**: An enumeration or string representing the player's character gender (MALE or FEMALE)
- **Save_State**: The serialized game state containing player position, inventory, party, and metadata
- **Scene2D**: LibGDX's UI framework used for building user interfaces
- **TextField**: A Scene2D UI component that accepts keyboard text input
- **Screen_Transition**: The process of switching from one Screen to another using the screen cache system

## Requirements

### Requirement 1: Character Setup Screen Creation

**User Story:** As a player, I want to create a character with a gender and name, so that I can personalize my game experience.

#### Acceptance Criteria

1. THE Character_Setup_Screen SHALL be implemented as a new Scene2D screen class
2. THE Character_Setup_Screen SHALL display a gender selection step as the first step
3. THE Character_Setup_Screen SHALL display a name input step as the second step
4. THE Character_Setup_Screen SHALL use UI styling consistent with the Main_Menu_Screen
5. THE Character_Setup_Screen SHALL support mouse click interactions for all UI elements
6. THE Character_Setup_Screen SHALL support keyboard input for the name TextField

### Requirement 2: Gender Selection Step

**User Story:** As a player, I want to select my character's gender, so that I can choose how my character is represented in the game.

#### Acceptance Criteria

1. THE Character_Setup_Screen SHALL display two selectable options labeled "Male" and "Female"
2. WHEN a player clicks on a gender option, THE Character_Setup_Screen SHALL visually indicate the selection
3. WHEN a player confirms their gender selection, THE Character_Setup_Screen SHALL transition to the name input step
4. THE Character_Setup_Screen SHALL prevent progression to the name input step until a gender is selected
5. THE Character_Setup_Screen SHALL provide a back button that returns to the Main_Menu_Screen

### Requirement 3: Name Input Step

**User Story:** As a player, I want to enter my character's name, so that my character has a unique identity in the game.

#### Acceptance Criteria

1. THE Character_Setup_Screen SHALL display a Scene2D TextField for name input
2. THE Character_Setup_Screen SHALL display a confirm button labeled "Start" or "Confirm"
3. THE Character_Setup_Screen SHALL display a back button that returns to the gender selection step
4. WHEN the player presses Enter in the TextField, THE Character_Setup_Screen SHALL trigger the same action as clicking the confirm button
5. WHEN the player presses Escape, THE Character_Setup_Screen SHALL trigger the same action as clicking the back button
6. THE Character_Setup_Screen SHALL accept keyboard text input in the TextField

### Requirement 4: Name Validation

**User Story:** As a player, I want to be prevented from using invalid names, so that my character has a valid name in the game.

#### Acceptance Criteria

1. WHEN a player attempts to confirm an empty name, THE Character_Setup_Screen SHALL display an error message
2. WHEN a player attempts to confirm a name containing only whitespace, THE Character_Setup_Screen SHALL display an error message
3. THE Character_Setup_Screen SHALL trim leading and trailing whitespace from the entered name before validation
4. THE Character_Setup_Screen SHALL prevent game start until a valid name is provided
5. THE Character_Setup_Screen SHALL display error messages in a visible location near the TextField

### Requirement 5: Player Profile Data Model

**User Story:** As a developer, I want a data model to store player profile information, so that the selected gender and name can be accessed throughout the game.

#### Acceptance Criteria

1. THE Player_Profile SHALL contain a String field for the player name
2. THE Player_Profile SHALL contain a Gender field (enum or String) for the player gender
3. THE Player_Profile SHALL provide getter methods for accessing the player name
4. THE Player_Profile SHALL provide getter methods for accessing the player gender
5. THE Player_Profile SHALL be accessible from the Game_Screen

### Requirement 6: Main Menu Integration

**User Story:** As a player, I want the New Game button to start the character setup process, so that I can create my character before playing.

#### Acceptance Criteria

1. WHEN the player clicks "NEW GAME" on the Main_Menu_Screen, THE Main_Menu_Screen SHALL transition to the Character_Setup_Screen
2. WHEN the player completes character setup, THE Character_Setup_Screen SHALL transition to the Game_Screen
3. THE Main_Menu_Screen SHALL preserve existing CONTINUE button functionality
4. THE Main_Menu_Screen SHALL preserve existing EXIT GAME button functionality
5. THE Main_Menu_Screen SHALL not modify any save state loading logic

### Requirement 7: Game Start Integration

**User Story:** As a player, I want the game to start with my chosen character, so that my character setup choices are reflected in gameplay.

#### Acceptance Criteria

1. WHEN character setup is completed, THE Character_Setup_Screen SHALL store the selected gender in the Player_Profile
2. WHEN character setup is completed, THE Character_Setup_Screen SHALL store the validated name in the Player_Profile
3. WHEN character setup is completed, THE Character_Setup_Screen SHALL invoke the same new game initialization logic used before this feature
4. WHEN the game starts, THE Game_Screen SHALL spawn the player in the original default location (Cottage)
5. THE Character_Setup_Screen SHALL not trigger any save state loading logic

### Requirement 8: Save State Compatibility

**User Story:** As a developer, I want the player profile to be compatible with future save state enhancements, so that player metadata can be persisted later.

#### Acceptance Criteria

1. WHERE the Save_State contains player metadata fields, THE Player_Profile SHALL provide methods to populate those fields
2. WHERE the Save_State does not contain player metadata fields, THE Player_Profile SHALL include TODO comments indicating future save integration points
3. THE Player_Profile SHALL not modify the existing Save_State serialization logic
4. THE Player_Profile SHALL not modify the existing Save_State deserialization logic

### Requirement 9: Screen Transition Management

**User Story:** As a developer, I want screen transitions to use the existing screen cache system, so that the character setup flow integrates cleanly with the existing architecture.

#### Acceptance Criteria

1. THE Character_Setup_Screen SHALL be registered in the Main class screen cache
2. WHEN transitioning to the Character_Setup_Screen, THE Main class SHALL use the setScreen method with the screen class
3. WHEN transitioning from the Character_Setup_Screen, THE Character_Setup_Screen SHALL use the Main class setScreen method
4. THE Character_Setup_Screen SHALL set the input processor to its Stage when shown
5. THE Character_Setup_Screen SHALL dispose of resources properly when the screen is disposed

### Requirement 10: Scope Preservation

**User Story:** As a developer, I want unrelated game systems to remain unchanged, so that the character setup feature does not introduce regressions.

#### Acceptance Criteria

1. THE Character_Setup_Screen SHALL not modify battle system logic
2. THE Character_Setup_Screen SHALL not modify inventory system logic
3. THE Character_Setup_Screen SHALL not modify map transition logic (except where necessary for game start)
4. THE Character_Setup_Screen SHALL not modify the TeamViewer or SummaryScreen
5. THE Character_Setup_Screen SHALL not modify save/load logic beyond adding future-compatible Player_Profile fields
