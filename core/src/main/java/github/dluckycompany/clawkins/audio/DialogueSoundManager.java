package github.dluckycompany.clawkins.audio;

import java.util.Random;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.TimeUtils;

public class DialogueSoundManager implements Disposable {
    private static final String LONG_PATH = "audio/soundEffects/SFX_AmbroggioMusic/Garble Long.mp3";

    private static final float MIN_PITCH = 1.02f;
    private static final float MAX_PITCH = 1.22f;
    private static final float MIN_VOLUME = 0.35f;
    private static final float MAX_VOLUME = 0.55f;
    private static final float MIN_PAN = -0.2f;
    private static final float MAX_PAN = 0.2f;
    private static final long MIN_INTERVAL_MS = 45L;

    private final Random random = new Random();
    private Sound longGarble;

    private int validSinceLast = 0;
    private int nextTriggerCount = 2;
    private long lastPlayMs = 0L;

    public DialogueSoundManager() {
        this.longGarble = loadSound(LONG_PATH);
        this.nextTriggerCount = pickNextTriggerCount();
    }

    public void onCharacterRevealed(char c, int visibleCharacterIndex) {
        if (shouldIgnore(c)) {
            return;
        }

        validSinceLast++;
        if (validSinceLast < nextTriggerCount) {
            return;
        }

        long now = TimeUtils.millis();
        if (now - lastPlayMs < MIN_INTERVAL_MS) {
            return;
        }

        Sound sound = pickSound();
        if (sound == null) {
            return;
        }

        float volume = randomBetween(MIN_VOLUME, MAX_VOLUME);
        float pitch = randomBetween(MIN_PITCH, MAX_PITCH);
        float pan = randomBetween(MIN_PAN, MAX_PAN);
        sound.play(volume, pitch, pan);

        lastPlayMs = now;
        validSinceLast = 0;
        nextTriggerCount = pickNextTriggerCount();
    }

    public void stop() {
        stopSound(longGarble);
        validSinceLast = 0;
        nextTriggerCount = pickNextTriggerCount();
        lastPlayMs = 0L;
    }

    @Override
    public void dispose() {
        disposeSound(longGarble);
        longGarble = null;
    }

    private Sound loadSound(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        FileHandle handle = Gdx.files.internal(path);
        if (!handle.exists()) {
            Gdx.app.log("DialogueSoundManager", "Sound file not found: " + path);
            return null;
        }
        return Gdx.audio.newSound(handle);
    }

    private int pickNextTriggerCount() {
        return 2 + random.nextInt(3);
    }

    private Sound pickSound() {
        return longGarble;
    }

    private static float randomBetween(float min, float max) {
        return min + (float) Math.random() * (max - min);
    }

    private static boolean shouldIgnore(char c) {
        return Character.isWhitespace(c)
                || c == '.' || c == ',' || c == '!' || c == '?' || c == ':' || c == ';';
    }

    private static void stopSound(Sound sound) {
        if (sound != null) {
            sound.stop();
        }
    }

    private static void disposeSound(Sound sound) {
        if (sound != null) {
            sound.dispose();
        }
    }
}
