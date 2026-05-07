# Cheat Console Implementation Guide

## Overview

A developer cheat console has been implemented for debugging and testing purposes.

## How to Use

### Opening the Console

- Press **F12** to toggle the cheat console
- The console will appear at the top-center of the screen
- A text input field will automatically receive focus

### Entering Cheats

1. Type a cheat code into the text field
2. Press **ENTER** to execute the cheat
3. Feedback will appear below the input field:
   - **Green text** = Success
   - **Red text** = Error/Invalid cheat

### Closing the Console

- Press **ESC** to close
- Press **F12** again to toggle closed

## Available Cheat Codes

| Cheat Code | Description                                                              |
| ---------- | ------------------------------------------------------------------------ |
| `money`    | Adds 1,000 coins to your wallet                                          |
| `rich`     | Adds 10,000 coins to your wallet                                         |
| `poor`     | Removes all money from your wallet                                       |
| `heal`     | Fully heals all party members to max HP                                  |
| `items`    | Adds test items: 5x Potion, 2x Elixir, 3x Attack Boost, 3x Defense Boost |
| `whereami` | Displays current map name and player position (check console log)        |
| `help`     | Lists all available cheats (check console log)                           |

## Technical Details

### Files Created

- `core/src/main/java/github/dluckycompany/clawkins/debug/CheatCodeManager.java`
- `core/src/main/java/github/dluckycompany/clawkins/debug/CheatConsoleOverlay.java`

### Integration Points

- **GameScreen.java** - Main integration point
- Uses dedicated `cheatConsoleStage` for rendering
- Pauses gameplay when console is open
- Doesn't interfere with normal input processing

### Key Features

✅ F12 toggle  
✅ Automatic text field focus  
✅ Enter to execute, Escape to close  
✅ Visual feedback (success/error messages)  
✅ Gameplay pause when open  
✅ Independent rendering stage  
✅ Debug logging for troubleshooting

## Troubleshooting

### Console doesn't appear when pressing F12

1. Check the console logs for "CheatConsoleOverlay" messages
2. Verify the game isn't in battle or dialogue mode
3. Make sure you're in the main gameplay screen (not menu)

### Cheats don't work

1. Check spelling (cheats are case-insensitive)
2. Look for error messages in the feedback label
3. Check console logs for execution details

## Disabling Cheats

To disable cheats for production:

1. Open `CheatCodeManager.java`
2. Change `ENABLE_CHEATS = true` to `ENABLE_CHEATS = false`
3. Rebuild the project

## Adding New Cheats

To add a new cheat code:

1. Open `CheatCodeManager.java`
2. Add a new registration in the `registerDefaultCheats()` method:

```java
registerCheat("yourcheat", () -> {
    // Your cheat logic here
    return CheatResult.success("Cheat executed!");
});
```

Example - Add a speed boost cheat:

```java
registerCheat("speed", () -> {
    // Boost player speed
    Entity player = findPlayerEntity();
    if (player != null) {
        Move move = Move.MAPPER.get(player);
        if (move != null) {
            move.setMaxSpeed(move.getMaxSpeed() * 2f);
            return CheatResult.success("Speed doubled!");
        }
    }
    return CheatResult.failure("Player not found");
});
```

## Debug Logging

The cheat console logs important events:

- Console initialization
- Console open/close events
- Cheat execution attempts
- Success/failure results

Check your application logs for messages prefixed with:

- `[CheatConsoleOverlay]`
- `[CheatCodeManager]`
- `[CheatConsole]`

## Notes

- Cheats are **case-insensitive** (MONEY = money = MoNeY)
- Whitespace is automatically trimmed
- Invalid cheats show error messages
- The console pauses gameplay to prevent accidental movement
- Input focus is automatically restored when closing
