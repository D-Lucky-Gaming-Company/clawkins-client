package github.dluckycompany.clawkins.audio;

public enum MusicTrack {
    FLINTLOCK,
    EXPLORATION_2,
    EXPLORATION,
    BATTLE,
    MENU,
    /** Post-credits leaderboard screen. Maps to audio/music/menu.mp3. */
    POST_CREDITS,
    VICTORY,
    DEFEAT,
    COTTAGE,
    FIELDS,
    MOUNTAIN_PATH,
    NURSERY,
    BACKALLEY_SHOP,
    TAMERGROUNDS,
    BOSS_CERBERUS,
    /** Cerberus bridge / atmosphere dialogue bed (cave_3 {@code cerberus_enc_atmos0}). */
    BOSS_CERBERUS_DIA,
    BOSS_SPARTACUS,
    BOSS_SANTIRAL,
    BOSS_BERTJR_DIA_FIRST_ENCOUNTER,
    /** Duke Khai / Spartacus first encounter dialogue (before battle). */
    BOSS_DUKE_DIA_FIRST_ENCOUNTER,
    /** Ending credits music. Maps to audio/music/ending.mp3 if present. */
    CREDITS
}
