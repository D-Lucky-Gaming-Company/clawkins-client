package github.dluckycompany.clawkins.encounter;

/**
 * Which early-game (Lv1–9) balance curve applies when rolling wild combat stats.
 */
public enum EncounterBalanceMode {
    /** Grass/random step encounters — Swee'pea-conservative defaults. */
    DEFAULT,
    /** Map wild clawkin encounters — keyed to average party DEF/HP. */
    WILD_PARTY_AVERAGE,
    /** Roaming field trainers — keyed to encounter level sync. */
    ROAMING_TRAINER
}
