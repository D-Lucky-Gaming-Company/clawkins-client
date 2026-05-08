# Implementation Plan: Character Setup Flow

## Overview

This implementation plan breaks down the character setup flow feature into discrete coding tasks. The feature introduces a two-step character creation process (gender selection and name input) that integrates between the main menu and game start. Each task builds incrementally on previous work, with checkpoints to validate progress.

## Tasks

- [x] 1. Create Gender enum and PlayerProfile data model
  - Create `Gender.java` enum in `core/src/main/java/github/dluckycompany/clawkins/model/` package
  - Define MALE and FEMALE enum values with displayName field
  - Implement `getDisplayName()` method
  - Create `PlayerProfile.java` class in the same package
  - Add String name and Gender gender fields
  - Implement constructor and getter methods
  - Add TODO comments for future save state integration methods
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 8.2_

- [ ]\* 1.1 Write unit tests for Gender and PlayerProfile
  - Test Gender enum values and display names
  - Test PlayerProfile constructor and getters
  - Test PlayerProfile with both MALE and FEMALE genders
  - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [x] 2. Create CharacterSetupScreen class structure
  - [x] 2.1 Create CharacterSetupScreen.java implementing Screen interface
    - Create file in `core/src/main/java/github/dluckycompany/clawkins/ui/` package
    - Implement all Screen interface methods (show, render, resize, pause, resume, hide, dispose)
    - Add Main game reference and Batch reference as constructor parameters
    - Add Stage field for Scene2D UI
    - Add SetupStep enum (GENDER_SELECTION, NAME_INPUT) as inner enum
    - Add state fields: currentStep, selectedGender, playerName, profile
    - Add UI component fields: genderTable, nameTable, nameField, errorLabel
    - Add font fields: titleFont, buttonFont, labelFont
    - _Requirements: 1.1, 1.3, 9.1, 9.4, 9.5_
  - [x] 2.2 Implement basic render method with background
    - Implement `render(float delta)` to clear screen and draw background
    - Add `drawBackground()` helper method matching MainMenuScreen style
    - Initialize Stage in constructor
    - Call `stage.act(delta)` and `stage.draw()` in render method
    - _Requirements: 1.1, 1.4_
  - [x] 2.3 Implement show() and dispose() lifecycle methods
    - Set input processor to stage in `show()`
    - Reset state to GENDER_SELECTION in `show()`
    - Clear selectedGender and playerName in `show()`
    - Dispose stage, fonts, and textures in `dispose()`
    - _Requirements: 9.4, 9.5_

- [x] 3. Implement gender selection UI
  - [x] 3.1 Create font loading method
    - Implement `loadFonts()` method using FreeTypeFontGenerator
    - Load earthbound-dialogue-gold.otf font (matching MainMenuScreen)
    - Generate titleFont (size 48), buttonFont (size 32), labelFont (size 24)
    - Call from constructor
    - _Requirements: 1.4_
  - [x] 3.2 Implement buildGenderSelectionUI() method
    - Create Table layout with Scene2D
    - Add "CHARACTER CREATION" title label using titleFont
    - Add "Select Your Gender" instruction label
    - Create "Male" button using createButtonStyle()
    - Create "Female" button using createButtonStyle()
    - Add click listeners to set selectedGender and call transitionToNameInput()
    - Create "Back" button that transitions to MainMenuScreen
    - Add all elements to genderTable and add to stage
    - _Requirements: 1.2, 2.1, 2.2, 2.5_
  - [x] 3.3 Implement createButtonStyle() helper method
    - Create TextButtonStyle matching MainMenuScreen (gold/orange theme)
    - Set up button fonts, colors, and hover effects
    - Return reusable style
    - _Requirements: 1.4, 1.5_
  - [x] 3.4 Implement transitionToNameInput() method
    - Clear stage
    - Set currentStep to NAME_INPUT
    - Call buildNameInputUI()
    - _Requirements: 2.3_

- [ ] 4. Checkpoint - Test gender selection step
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Implement name input UI
  - [x] 5.1 Implement buildNameInputUI() method
    - Create Table layout with Scene2D
    - Add "CHARACTER CREATION" title label
    - Add "Enter Your Name" instruction label
    - Create TextField for name input using TextFieldStyle
    - Set TextField to accept keyboard input
    - Create errorLabel (initially empty, red color #FF4444)
    - Create "Back" button that calls transitionToGenderSelection()
    - Create "Start" button that calls completeSetup()
    - Add all elements to nameTable and add to stage
    - Set stage keyboard focus to TextField
    - _Requirements: 1.3, 3.1, 3.2, 3.3, 3.6, 4.5_
  - [x] 5.2 Implement transitionToGenderSelection() method
    - Clear stage
    - Set currentStep to GENDER_SELECTION
    - Clear errorLabel
    - Call buildGenderSelectionUI()
    - _Requirements: 3.3_
  - [x] 5.3 Add keyboard shortcut handling in render()
    - Check if Enter key pressed when currentStep is NAME_INPUT
    - If Enter pressed, call completeSetup()
    - Check if Escape key pressed
    - If Escape pressed and currentStep is NAME_INPUT, call transitionToGenderSelection()
    - If Escape pressed and currentStep is GENDER_SELECTION, transition to MainMenuScreen
    - _Requirements: 3.4, 3.5_

- [x] 6. Implement name validation
  - [x] 6.1 Implement validateName(String name) method
    - Check if name is null, show error "Name cannot be empty" and return false
    - Trim the name
    - Check if trimmed name is empty, show error "Name cannot be empty" and return false
    - Return true if validation passes
    - _Requirements: 4.1, 4.2, 4.3, 4.4_
  - [x] 6.2 Implement showError(String message) method
    - Set errorLabel text to message
    - Set errorLabel color to red (#FF4444)
    - Make errorLabel visible
    - _Requirements: 4.5_
  - [x] 6.3 Implement clearError() method
    - Clear errorLabel text
    - Hide errorLabel
    - _Requirements: 4.5_
  - [x] 6.4 Add TextField listener to clear errors on input
    - Add TextFieldListener to nameField
    - Call clearError() when text changes
    - _Requirements: 4.5_

- [ ]\* 6.5 Write unit tests for name validation
  - Test empty name rejection
  - Test whitespace-only name rejection (spaces, tabs, newlines)
  - Test valid name acceptance
  - Test name trimming behavior
  - _Requirements: 4.1, 4.2, 4.3_

- [x] 7. Implement character setup completion
  - [x] 7.1 Implement completeSetup() method
    - Get text from nameField
    - Call validateName() with the text
    - If validation fails, return early (error already shown)
    - Trim the name and store in playerName field
    - Create new PlayerProfile with playerName and selectedGender
    - Store profile in profile field
    - Call game.setScreen(GameScreen.class) to transition
    - _Requirements: 7.1, 7.2, 7.3_
  - [x] 7.2 Add getPlayerProfile() method to CharacterSetupScreen
    - Return the profile field
    - Used for passing profile to GameScreen
    - _Requirements: 5.5_

- [x] 8. Integrate with Main class
  - [x] 8.1 Register CharacterSetupScreen in Main.create()
    - Add CharacterSetupScreen to screen cache in Main.create() method
    - Pass Main instance and batch to constructor
    - _Requirements: 9.1, 9.2_
  - [x] 8.2 Modify Main.startNewGame() method
    - Change screen transition from GameScreen to CharacterSetupScreen
    - Remove or comment out direct GameScreen initialization
    - _Requirements: 6.1_

- [x] 9. Integrate with GameScreen
  - [x] 9.1 Add PlayerProfile field to GameScreen
    - Add private PlayerProfile playerProfile field
    - Add setPlayerProfile(PlayerProfile profile) method
    - Add getPlayerProfile() method
    - _Requirements: 5.5, 7.1, 7.2_
  - [x] 9.2 Modify GameScreen.show() to accept PlayerProfile
    - Check if CharacterSetupScreen has a profile
    - If profile exists, call setPlayerProfile() with it
    - Preserve existing game initialization logic (spawn at Cottage)
    - Do not modify save state loading logic
    - _Requirements: 7.3, 7.4, 7.5_

- [ ] 10. Checkpoint - Test complete flow
  - Ensure all tests pass, ask the user if questions arise.

- [ ]\* 11. Write integration tests
  - [ ]\* 11.1 Test NEW GAME button transitions to CharacterSetupScreen
    - Verify MainMenuScreen.clickNewGame() transitions to CharacterSetupScreen
    - _Requirements: 6.1_
  - [ ]\* 11.2 Test character setup completion transitions to GameScreen
    - Complete full character setup flow
    - Verify transition to GameScreen
    - Verify PlayerProfile is accessible from GameScreen
    - _Requirements: 6.2, 7.1, 7.2_
  - [ ]\* 11.3 Test back navigation from CharacterSetupScreen
    - Test back from gender selection returns to MainMenuScreen
    - Test back from name input returns to gender selection
    - _Requirements: 2.5, 3.3_
  - [ ]\* 11.4 Test game initialization after character setup
    - Complete character setup
    - Verify player spawns at Cottage (MapAsset.COTTAGE_SAMPLE)
    - Verify PlayerProfile contains correct name and gender
    - _Requirements: 7.4, 5.5_
  - [ ]\* 11.5 Test existing functionality preservation (regression tests)
    - Test CONTINUE button still transitions to SaveStateScreen
    - Test EXIT GAME button still exits application
    - Test save state loading still works correctly
    - _Requirements: 6.3, 6.4, 6.5, 10.5_

- [ ] 12. Final checkpoint - Verify all requirements
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- The design document uses Java, so all implementation will be in Java
- This is a LibGDX game using Scene2D for UI
- Font file: earthbound-dialogue-gold.otf (already used in MainMenuScreen)
- Color scheme: Gold/orange theme matching MainMenuScreen
- No new external dependencies required
- Property-based testing is not applicable for this UI-focused feature
- Manual testing checklist is provided in the design document
