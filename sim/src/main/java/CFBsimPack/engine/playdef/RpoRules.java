package CFBsimPack.engine.playdef;

/** Relative give / keep / throw weights for RPO (not forced to sum to 1). */
public final class RpoRules {
    public final double giveWeight;
    public final double keepWeightBase;
    public final double throwFloor;
    public final boolean zoneReadBoost;

    public RpoRules(double giveWeight, double keepWeightBase, double throwFloor, boolean zoneReadBoost) {
        this.giveWeight = giveWeight;
        this.keepWeightBase = keepWeightBase;
        this.throwFloor = throwFloor;
        this.zoneReadBoost = zoneReadBoost;
    }

    public static RpoRules defaults() {
        return new RpoRules(0.35, 0.15, 0.25, false);
    }

    public static RpoRules zoneRead() {
        return new RpoRules(0.35, 0.15, 0.25, true);
    }
}
