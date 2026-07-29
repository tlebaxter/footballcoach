package CFBsimPack.engine.playdef;

public final class ExplosiveProfile {
    public final double baseBurstChance;
    public final double breakawayMult;
    public final boolean needsSoftSafety;

    public ExplosiveProfile(double baseBurstChance, double breakawayMult, boolean needsSoftSafety) {
        this.baseBurstChance = Math.max(0, Math.min(0.55, baseBurstChance));
        this.breakawayMult = Math.max(0.5, Math.min(2.0, breakawayMult));
        this.needsSoftSafety = needsSoftSafety;
    }

    public static ExplosiveProfile forTrack(RunTrack track) {
        if (track == null) return new ExplosiveProfile(0.08, 1.0, false);
        switch (track) {
            case OUTSIDE_ZONE:
            case SWEEP:
                return new ExplosiveProfile(0.18, 1.35, true);
            case OPTION:
                return new ExplosiveProfile(0.16, 1.25, true);
            case POWER:
            case COUNTER:
                return new ExplosiveProfile(0.10, 1.10, false);
            case ISO:
            case DIVE:
                return new ExplosiveProfile(0.05, 0.85, false);
            case QB_DRAW:
                return new ExplosiveProfile(0.12, 1.15, false);
            case INSIDE_ZONE:
            default:
                return new ExplosiveProfile(0.11, 1.05, false);
        }
    }
}
