# Clawkin Game Documentation

## Developer Tools

### Cheat Console

A debug console for testing and development purposes.

- **[Cheat Console Guide](CHEAT_CONSOLE_GUIDE.md)** - How to use the cheat console
- **[Cheat Console Test Guide](CHEAT_CONSOLE_TEST_GUIDE.md)** - Testing and verification guide

#### Quick Start

1. Press **F12** to open the cheat console
2. Type a cheat code (e.g., `money`, `heal`, `items`)
3. Press **ENTER** to execute
4. Press **ESC** or **F12** to close

#### Available Cheats

- `money` - Add 1,000 coins
- `rich` - Add 10,000 coins
- `poor` - Remove all money
- `heal` - Heal all party members
- `items` - Add test items
- `whereami` - Show current location
- `help` - List all cheats

## Project Structure

```
clawkins-client/
├── core/
│   └── src/main/java/github/dluckycompany/clawkins/
│       ├── debug/              # Debug tools (cheat console)
│       ├── battle/             # Battle system
│       ├── character/          # Character/Clawkin classes
│       ├── component/          # ECS components
│       ├── system/             # ECS systems
│       ├── ui/                 # UI components
│       ├── item/               # Items and inventory
│       └── ...
├── assets/                     # Game assets (audio, graphics, maps)
└── docs/                       # Documentation
```

## Notes

- The cheat console is for **development and testing only**
- All cheats modify the actual game state
- Changes persist for the current game session
- Input is completely isolated when console is open (no character movement while typing)
