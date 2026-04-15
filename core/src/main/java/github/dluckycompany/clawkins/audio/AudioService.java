package github.dluckycompany.clawkins.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.utils.Disposable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Centralized audio manager. Safe to use before audio assets exist:
 * missing file paths are ignored without crashing.
 */
public class AudioService implements Disposable {
    private final Map<MusicTrack, String> musicPaths = new EnumMap<>(MusicTrack.class);
    private final Map<SoundEffect, String> soundPaths = new EnumMap<>(SoundEffect.class);
    private final Map<MusicTrack, Music> musicCache = new EnumMap<>(MusicTrack.class);
    private final Map<SoundEffect, Sound> soundCache = new EnumMap<>(SoundEffect.class);

    private MusicTrack currentTrack;
    private Music currentMusic;
    private float musicVolume = 0.6f;
    private float soundVolume = 0.8f;
    private TiledMap currentMap;

    public void registerMusic(MusicTrack track, String internalPath) {
        musicPaths.put(track, internalPath);
    }

    public void registerSound(SoundEffect effect, String internalPath) {
        soundPaths.put(effect, internalPath);
    }

    public void setMusicVolume(float musicVolume) {
        this.musicVolume = Math.max(0f, Math.min(1f, musicVolume));
        if (currentMusic != null) {
            currentMusic.setVolume(this.musicVolume);
        }
    }

    public void setSoundVolume(float soundVolume) {
        this.soundVolume = Math.max(0f, Math.min(1f, soundVolume));
    }

    public void setMap(TiledMap tiledMap) {
        this.currentMap = tiledMap;
    }

    public void playCurrentMapMusic() {
        playCurrentMapMusic(true);
    }

    public void onEvent(AudioEventType eventType) {
        switch (eventType) {
            case MAP_CHANGED -> playCurrentMapMusic(false);
            case ENCOUNTER_STARTED -> playSound(SoundEffect.ENCOUNTER);
            case BATTLE_STARTED -> playMusic(MusicTrack.BATTLE, true);
            case BATTLE_VICTORY -> playMusic(MusicTrack.VICTORY, false);
            case BATTLE_DEFEAT -> playMusic(MusicTrack.DEFEAT, false);
            case BATTLE_ENDED -> playCurrentMapMusic();
            case UI_CONFIRM -> playSound(SoundEffect.CONFIRM);
        }
    }

    private void playCurrentMapMusic(boolean fallbackToExplorationWhenMissing) {
        if (currentMap == null) {
            if (fallbackToExplorationWhenMissing) {
                playMusic(MusicTrack.EXPLORATION, true);
            }
            return;
        }

        MusicTrack mapTrack = mapMusicTrackOrNull(currentMap);
        if (mapTrack != null) {
            playMusic(mapTrack, true);
            return;
        }

        if (fallbackToExplorationWhenMissing) {
            playMusic(MusicTrack.EXPLORATION, true);
        }
    }

    public void playMusic(MusicTrack track, boolean looping) {
        if (track == null) {
            return;
        }
        if (currentTrack == track && currentMusic != null) {
            currentMusic.setLooping(looping);
            currentMusic.setVolume(musicVolume);
            if (!currentMusic.isPlaying()) {
                currentMusic.play();
            }
            return;
        }

        Music nextMusic = resolveMusic(track);
        if (nextMusic == null) {
            return;
        }
        if (currentMusic != null) {
            currentMusic.stop();
        }
        currentTrack = track;
        currentMusic = nextMusic;
        currentMusic.setLooping(looping);
        currentMusic.setVolume(musicVolume);
        currentMusic.play();
    }

    public void playSound(SoundEffect effect) {
        Sound sound = resolveSound(effect);
        if (sound == null) {
            return;
        }
        sound.play(soundVolume);
    }

    private Music resolveMusic(MusicTrack track) {
        Music cached = musicCache.get(track);
        if (cached != null) {
            return cached;
        }
        String path = musicPaths.get(track);
        if (path == null || path.isBlank() || !Gdx.files.internal(path).exists()) {
            return null;
        }
        Music music = Gdx.audio.newMusic(Gdx.files.internal(path));
        musicCache.put(track, music);
        return music;
    }

    private Sound resolveSound(SoundEffect effect) {
        Sound cached = soundCache.get(effect);
        if (cached != null) {
            return cached;
        }
        String path = soundPaths.get(effect);
        if (path == null || path.isBlank() || !Gdx.files.internal(path).exists()) {
            return null;
        }
        Sound sound = Gdx.audio.newSound(Gdx.files.internal(path));
        soundCache.put(effect, sound);
        return sound;
    }

    private static MusicTrack mapMusicTrackOrNull(TiledMap map) {
        if (map == null || !map.getProperties().containsKey("musicTrack")) {
            return null;
        }

        String value = map.getProperties().get("musicTrack", "", String.class);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return MusicTrack.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    @Override
    public void dispose() {
        musicCache.values().forEach(Music::dispose);
        soundCache.values().forEach(Sound::dispose);
        musicCache.clear();
        soundCache.clear();
        currentMusic = null;
        currentTrack = null;
    }
}
