# Intro Exposition - Fade Transitions Implementation

## Changes Made

Replaced the typewriter text reveal effect with smooth fade-in/fade-out transitions for a more cinematic presentation.

## Previous Behavior

- Text appeared character-by-character (typewriter effect)
- 45 characters per second reveal speed
- Player could skip typewriter or advance when complete

## New Behavior

Each line of exposition text now follows this sequence:

### 1. **Fade In** (0.8 seconds)
- Text alpha goes from 0.0 to 1.0
- Smooth appearance of text

### 2. **Hold** (3.0 seconds)
- Text remains fully visible
- Player can read the line

### 3. **Fade Out** (0.8 seconds)
- Text alpha goes from 1.0 to 0.0
- Smooth disappearance of text
- Automatically advances to next line

### 4. **Final Transition** (after all lines)
- Fade out: 1.0 second (keep black)
- Hold: 0.3 seconds (black screen)
- Fade in: 0.8 seconds (reveal gameplay)
- Triggers tutorial dialogue

## Input Handling

Player can press the confirm key to skip phases:

- **During Fade In**: Skip to Hold phase (text fully visible)
- **During Hold**: Skip to Fade Out phase
- **During Fade Out**: Skip to next line immediately

This allows players to:
- Read at their own pace
- Skip through quickly if desired
- Still feel the cinematic presentation

## Technical Implementation

### State Machine

```
TextFadePhase enum:
- FADE_IN
- HOLD
- FADE_OUT
```

### Timing Constants

```java
TEXT_FADE_IN_DURATION = 0.8f    // Fade in time
TEXT_HOLD_DURATION = 3.0f       // Display time
TEXT_FADE_OUT_DURATION = 0.8f   // Fade out time
```

### Alpha Calculation

**Fade In:**
```java
textAlpha = Math.min(1f, textFadeTimer / TEXT_FADE_IN_DURATION);
```

**Hold:**
```java
textAlpha = 1f;
```

**Fade Out:**
```java
textAlpha = Math.max(0f, 1f - (textFadeTimer / TEXT_FADE_OUT_DURATION));
```

## Visual Flow

```
Line 1: Fade In → Hold → Fade Out
    ↓
Line 2: Fade In → Hold → Fade Out
    ↓
Line 3: Fade In → Hold → Fade Out
    ↓
... (all 7 lines)
    ↓
Final Transition: Black → Hold → Fade to Gameplay
    ↓
Tutorial Dialogue
```

## Removed Code

- `TYPEWRITER_CHARS_PER_SECOND` constant
- `visibleChars` tracking
- `typewriterCarry` accumulator
- `currentVisibleText` vs `currentFullText` distinction
- `tickTypewriter()` method
- `isLineFullyRevealed()` method
- `revealLineImmediately()` method

## Added Code

- `TextFadePhase` enum
- `textFadePhase` state variable
- `textFadeTimer` for phase timing
- `textAlpha` for text transparency
- `backgroundAlpha` for final transition
- `updateTextFade()` method
- `handleInput()` method for skip logic
- `updateFinalTransition()` method

## Benefits

### 1. **More Cinematic**
- Smooth, professional fade transitions
- Better suited for narrative exposition
- Less "game-y", more "movie-like"

### 2. **Better Pacing**
- Consistent timing for each line
- No waiting for slow typewriter
- Player can still skip if desired

### 3. **Cleaner Code**
- Simpler state machine
- No character-by-character tracking
- Easier to adjust timing

### 4. **Performance**
- No per-character sound effects
- No substring operations per frame
- Simpler rendering logic

## Timing Breakdown

For 7 lines of exposition:

**Minimum time (if player skips everything):**
- ~0 seconds (instant skip through all phases)

**Maximum time (if player waits for all fades):**
- Per line: 0.8s (fade in) + 3.0s (hold) + 0.8s (fade out) = 4.6s
- 7 lines × 4.6s = 32.2 seconds
- Final transition: 1.0s + 0.3s + 0.8s = 2.1 seconds
- **Total: ~34.3 seconds**

**Typical time (player reads at normal pace):**
- ~20-25 seconds (skipping some holds)

## Customization

To adjust timing, modify these constants:

```java
// Faster pacing
TEXT_FADE_IN_DURATION = 0.5f
TEXT_HOLD_DURATION = 2.0f
TEXT_FADE_OUT_DURATION = 0.5f

// Slower, more dramatic
TEXT_FADE_IN_DURATION = 1.2f
TEXT_HOLD_DURATION = 4.0f
TEXT_FADE_OUT_DURATION = 1.2f
```

## Testing

✅ Text fades in smoothly  
✅ Text holds for readable duration  
✅ Text fades out smoothly  
✅ Automatically advances to next line  
✅ Player can skip any phase  
✅ Final transition works correctly  
✅ Tutorial dialogue appears after intro  

## Summary

The intro exposition now uses elegant fade transitions instead of typewriter effects, creating a more cinematic and polished opening sequence. The timing is balanced to be readable but not too slow, and players can skip through at their own pace.
