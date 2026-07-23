package CFBsimPack;

/**
 * Team offensive identity — pass/run bias, personnel, formation weights.
 */
public enum OffensivePhilosophy {
    PRO_STYLE("Pro Style", 0.50, "11"),
    SPREAD("Spread", 0.58, "11"),
    AIR_RAID("Air Raid", 0.72, "10"),
    POWER_RUN("Power Run", 0.32, "21"),
    OPTION("Option", 0.35, "20"),
    WEST_COAST("West Coast", 0.62, "11"),
    SMASHMOUTH("Smashmouth / Gap", 0.28, "21"),
    RPO_SPREAD("RPO Spread", 0.52, "11"),
    PISTOL("Pistol", 0.45, "11"),
    FLEXBONE("Flexbone / Triple Option", 0.25, "20"),
    RUN_AND_SHOOT("Run & Shoot", 0.75, "10"),
    MULTIPLE("Multiple / Pro Spread", 0.52, "11");

    public final String displayName;
    /** Baseline P(pass) before situation/strategy. */
    public final double passBias;
    /** Default personnel package label (10/11/12/20/21…). */
    public final String defaultPersonnel;

    OffensivePhilosophy(String displayName, double passBias, String defaultPersonnel) {
        this.displayName = displayName;
        this.passBias = passBias;
        this.defaultPersonnel = defaultPersonnel;
    }

    public boolean wantsFullback() {
        return "21".equals(defaultPersonnel) || "22".equals(defaultPersonnel) || "20".equals(defaultPersonnel);
    }

    public boolean wantsTightEnd() {
        return !"10".equals(defaultPersonnel);
    }

    public static OffensivePhilosophy fromOrdinalSafe(int i) {
        OffensivePhilosophy[] v = values();
        if (i < 0 || i >= v.length) return MULTIPLE;
        return v[i];
    }

    public static OffensivePhilosophy assignForPrestige(int prestige, java.util.Random rng) {
        if (prestige >= 85) {
            OffensivePhilosophy[] elite = {AIR_RAID, SPREAD, WEST_COAST, MULTIPLE, RPO_SPREAD};
            return elite[rng.nextInt(elite.length)];
        }
        if (prestige >= 70) {
            OffensivePhilosophy[] mid = {PRO_STYLE, SPREAD, MULTIPLE, PISTOL, POWER_RUN, WEST_COAST};
            return mid[rng.nextInt(mid.length)];
        }
        OffensivePhilosophy[] low = {POWER_RUN, SMASHMOUTH, OPTION, FLEXBONE, PRO_STYLE, MULTIPLE};
        return low[rng.nextInt(low.length)];
    }
}
