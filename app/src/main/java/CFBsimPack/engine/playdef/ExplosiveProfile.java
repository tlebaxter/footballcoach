package CFBsimPack.engine.playdef;

public final class ExplosiveProfile {
    public final double baseBurstChance;
    public final double breakawayMult;
    public final boolean needsSoftSafety;
    /** Scales base run yards (replaces legacy runYardsMod). */
    public final double yardScale;
    /** Scales fumble chance (replaces legacy fumbleMod). */
    public final double fumbleRisk;

    public ExplosiveProfile(
            double baseBurstChance,
            double breakawayMult,
            boolean needsSoftSafety,
            double yardScale,
            double fumbleRisk
    ) {
        this.baseBurstChance = Math.max(0, Math.min(0.55, baseBurstChance));
        this.breakawayMult = Math.max(0.5, Math.min(2.0, breakawayMult));
        this.needsSoftSafety = needsSoftSafety;
        this.yardScale = Math.max(0.75, Math.min(1.35, yardScale));
        this.fumbleRisk = Math.max(0.7, Math.min(1.25, fumbleRisk));
    }

    public ExplosiveProfile(double baseBurstChance, double breakawayMult, boolean needsSoftSafety) {
        this(baseBurstChance, breakawayMult, needsSoftSafety, 1.0, 1.0);
    }

    public static ExplosiveProfile forTrack(RunTrack track) {
        if (track == null) return new ExplosiveProfile(0.08, 1.0, false, 1.05, 0.95);
        switch (track) {
            case OUTSIDE_ZONE:
                return new ExplosiveProfile(0.18, 1.35, true, 1.12, 1.02);
            case SWEEP:
                return new ExplosiveProfile(0.18, 1.35, true, 1.15, 1.05);
            case OPTION:
                return new ExplosiveProfile(0.16, 1.25, true, 1.08, 1.02);
            case POWER:
                return new ExplosiveProfile(0.10, 1.10, false, 1.05, 0.92);
            case COUNTER:
                return new ExplosiveProfile(0.10, 1.10, false, 1.10, 0.98);
            case ISO:
                return new ExplosiveProfile(0.05, 0.85, false, 1.00, 0.90);
            case DIVE:
                return new ExplosiveProfile(0.05, 0.85, false, 0.93, 0.88);
            case QB_DRAW:
                return new ExplosiveProfile(0.12, 1.15, false, 1.06, 0.90);
            case INSIDE_ZONE:
            default:
                return new ExplosiveProfile(0.11, 1.05, false, 1.08, 0.95);
        }
    }
}
