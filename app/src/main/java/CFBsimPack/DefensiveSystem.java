package CFBsimPack;

/**
 * Team defensive front / package identity.
 */
public enum DefensiveSystem {
    BASE_3_4("3-4", new RoleTag[]{
            RoleTag.NT, RoleTag.DE, RoleTag.DE, RoleTag.ILB, RoleTag.ILB,
            RoleTag.EDGE, RoleTag.EDGE, RoleTag.CB, RoleTag.CB, RoleTag.FS, RoleTag.SS
    }, 1.10, 0.95),
    BASE_4_3("4-3", new RoleTag[]{
            RoleTag.DE, RoleTag.DT, RoleTag.DT, RoleTag.DE, RoleTag.WILL,
            RoleTag.MIKE, RoleTag.SAM, RoleTag.CB, RoleTag.CB, RoleTag.FS, RoleTag.SS
    }, 1.00, 1.00),
    BASE_4_4("4-4", new RoleTag[]{
            RoleTag.DE, RoleTag.DT, RoleTag.DT, RoleTag.DE, RoleTag.LB,
            RoleTag.LB, RoleTag.LB, RoleTag.LB, RoleTag.CB, RoleTag.CB, RoleTag.S
    }, 1.20, 0.85),
    NICKEL("Nickel", new RoleTag[]{
            RoleTag.DE, RoleTag.DT, RoleTag.DT, RoleTag.EDGE, RoleTag.LB,
            RoleTag.LB, RoleTag.NB, RoleTag.CB, RoleTag.CB, RoleTag.FS, RoleTag.SS
    }, 0.90, 1.15),
    FOUR_TWO_FIVE("4-2-5", new RoleTag[]{
            RoleTag.DE, RoleTag.DT, RoleTag.DT, RoleTag.DE, RoleTag.LB,
            RoleTag.LB, RoleTag.NB, RoleTag.CB, RoleTag.CB, RoleTag.FS, RoleTag.SS
    }, 0.95, 1.12),
    DIME("Dime", new RoleTag[]{
            RoleTag.DE, RoleTag.DT, RoleTag.EDGE, RoleTag.EDGE, RoleTag.LB,
            RoleTag.NB, RoleTag.CB, RoleTag.CB, RoleTag.CB, RoleTag.FS, RoleTag.SS
    }, 0.75, 1.25),
    THREE_THREE_FIVE("3-3-5", new RoleTag[]{
            RoleTag.NT, RoleTag.DE, RoleTag.DE, RoleTag.LB, RoleTag.LB,
            RoleTag.LB, RoleTag.NB, RoleTag.CB, RoleTag.CB, RoleTag.FS, RoleTag.SS
    }, 0.92, 1.10),
    FIVE_TWO("5-2", new RoleTag[]{
            RoleTag.DE, RoleTag.DT, RoleTag.NT, RoleTag.DT, RoleTag.DE,
            RoleTag.LB, RoleTag.LB, RoleTag.CB, RoleTag.CB, RoleTag.FS, RoleTag.SS
    }, 1.25, 0.80),
    BEAR_46("Bear / 46", new RoleTag[]{
            RoleTag.DE, RoleTag.DT, RoleTag.NT, RoleTag.DT, RoleTag.DE,
            RoleTag.LB, RoleTag.LB, RoleTag.LB, RoleTag.CB, RoleTag.CB, RoleTag.S
    }, 1.30, 0.75),
    TWO_FOUR_FIVE("2-4-5 / Okie", new RoleTag[]{
            RoleTag.DT, RoleTag.DT, RoleTag.EDGE, RoleTag.EDGE, RoleTag.LB,
            RoleTag.LB, RoleTag.NB, RoleTag.CB, RoleTag.CB, RoleTag.FS, RoleTag.SS
    }, 0.88, 1.18);

    public final String displayName;
    public final RoleTag[] slots;
    public final double runWeight;
    public final double passWeight;

    DefensiveSystem(String displayName, RoleTag[] slots, double runWeight, double passWeight) {
        this.displayName = displayName;
        this.slots = slots;
        this.runWeight = runWeight;
        this.passWeight = passWeight;
    }

    public int dbCount() {
        int n = 0;
        for (RoleTag t : slots) {
            PositionGroup g = t.preferredGroup();
            if (g == PositionGroup.CB || g == PositionGroup.S) n++;
        }
        return n;
    }

    public static DefensiveSystem fromOrdinalSafe(int i) {
        DefensiveSystem[] v = values();
        if (i < 0 || i >= v.length) return BASE_4_3;
        return v[i];
    }

    public static DefensiveSystem assignForProgramStrength(int programStrength, java.util.Random rng) {
        if (programStrength >= 85) {
            DefensiveSystem[] elite = {FOUR_TWO_FIVE, NICKEL, BASE_3_4, THREE_THREE_FIVE, DIME};
            return elite[rng.nextInt(elite.length)];
        }
        if (programStrength >= 70) {
            DefensiveSystem[] mid = {BASE_4_3, FOUR_TWO_FIVE, NICKEL, BASE_3_4, TWO_FOUR_FIVE};
            return mid[rng.nextInt(mid.length)];
        }
        DefensiveSystem[] low = {BASE_4_3, BASE_4_4, FIVE_TWO, BEAR_46, BASE_3_4};
        return low[rng.nextInt(low.length)];
    }
}
