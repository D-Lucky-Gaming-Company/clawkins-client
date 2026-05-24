package github.dluckycompany.clawkins.encounter;

/**
 * Inputs for {@link WildEnemyBalance} when rolling Lv1–9 encounter stats.
 */
public record WildEncounterBalanceContext(
        EncounterBalanceMode mode,
        int playerLevel,
        int enemyLevel,
        int partyAverageDefense,
        int partyAverageHp,
        int enemyDefense,
        int enemySpeed
) {
    public static WildEncounterBalanceContext forDefault(
            int playerLevel,
            int enemyLevel,
            int enemyDefense,
            int enemySpeed
    ) {
        return new WildEncounterBalanceContext(
                EncounterBalanceMode.DEFAULT,
                playerLevel,
                enemyLevel,
                0,
                0,
                enemyDefense,
                enemySpeed
        );
    }

    public static WildEncounterBalanceContext forWildPartyAverage(
            int playerLevel,
            int enemyLevel,
            int partyAverageDefense,
            int partyAverageHp,
            int enemyDefense,
            int enemySpeed
    ) {
        return new WildEncounterBalanceContext(
                EncounterBalanceMode.WILD_PARTY_AVERAGE,
                playerLevel,
                enemyLevel,
                partyAverageDefense,
                partyAverageHp,
                enemyDefense,
                enemySpeed
        );
    }

    public static WildEncounterBalanceContext forRoamingTrainer(
            int playerLevel,
            int enemyLevel,
            int enemyDefense,
            int enemySpeed
    ) {
        return new WildEncounterBalanceContext(
                EncounterBalanceMode.ROAMING_TRAINER,
                playerLevel,
                enemyLevel,
                0,
                0,
                enemyDefense,
                enemySpeed
        );
    }
}
