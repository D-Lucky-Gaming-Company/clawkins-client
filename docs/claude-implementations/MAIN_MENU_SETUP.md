# Main Menu Screen Setup

## Files Created

- `MainMenuScreen.java` — The complete main menu screen implementation

## How to Wire It Into Your Game

### 1. Update Your Main Game Class

Edit your main `Game` class (e.g., `MyGdxGame.java` or similar):

```java
import github.dluckycompany.clawkins.ui.MainMenuScreen;

public class MyGdxGame extends Game {
    private SpriteBatch batch;
    private MainMenuScreen mainMenuScreen;
    private GameScreen gameScreen;  // your actual game screen

    @Override
    public void create() {
        batch = new SpriteBatch();

        // Create menu with callbacks
        mainMenuScreen = new MainMenuScreen(
            batch,
            () -> {
                // NEW GAME callback
                System.out.println("Start Game");
                setScreen(new GameScreen(batch));  // transition to game
            },
            () -> {
                // CONTINUE callback
                System.out.println("Continue Game");
                // Load saved game state here
                setScreen(new GameScreen(batch));
            },
            () -> {
                // EXIT GAME callback
                System.out.println("Exit Game");
                Gdx.app.exit();
            }
        );

        setScreen(mainMenuScreen);
    }

    @Override
    public void dispose() {
        batch.dispose();
        if (mainMenuScreen != null) mainMenuScreen.dispose();
        if (gameScreen != null) gameScreen.dispose();
    }
}
```

## Features

| Feature | Details |
|---------|---------|
| **Title** | "CLAWKINS" in large gold text with "- DAWN OF THE PRIMAL -" subtitle |
| **Buttons** | NEW GAME, CONTINUE, EXIT GAME — vertically stacked, gold with hover effects |
| **Styling** | Dark fantasy theme with gradient background and vignette edges |
| **Animation** | Fade-in effect on load (1 second) |
| **Keyboard** | ESC key exits the game |
| **Responsive** | Scales to any screen size via ScreenViewport and Table layout |

## Customization

### Change Font

Replace the font path in `loadFonts()`:

```java
FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
    Gdx.files.internal("font/YOUR_FONT_HERE.otf"));
```

### Change Button Color

In `createButtonStyle()`, modify the color constants:

```java
Color.valueOf("#C58A2B")   // Main gold (change this)
Color.valueOf("#D4A035")   // Hover (lighter)
Color.valueOf("#A66F1F")   // Pressed (darker)
```

### Change Title Text

In `buildUI()`:

```java
Label title = new Label("YOUR TITLE HERE", new Label.LabelStyle(titleFont, Color.WHITE));
```

### Disable Fade-In Animation

Set `FADE_IN_DURATION = 0f` at the top of the class.

## Expected Console Output

When buttons are clicked:

```
[MainMenu] NEW GAME clicked
[MainMenu] CONTINUE clicked
[MainMenu] EXIT GAME clicked
```

## Dependencies

- LibGDX (Scene2D, freetype fonts)
- Font file: `assets/font/earthbound-dialogue-gold.otf` (or your custom font)

The font is already available in your project (used by `BattleOverlay`), so no additional assets needed!
