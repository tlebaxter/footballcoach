package CFBsimPack.engine;

/**
 * Per-snap defensive coverage call (atop DefensiveSystem).
 */
public enum CoverageCall {
    COVER_0(1.15, 0.85, 1.20, 1.10),
    COVER_1(1.05, 0.95, 1.10, 1.05),
    COVER_2(0.90, 1.10, 0.85, 0.95),
    COVER_3(1.00, 1.00, 1.00, 1.00),
    COVER_4(0.85, 1.15, 0.80, 1.05),
    MAN(1.08, 0.92, 1.12, 1.05),
    ZONE(0.95, 1.05, 0.95, 0.98),
    STACK_BOX(1.25, 0.80, 0.90, 0.85),
    SPY(1.00, 1.00, 1.00, 0.70),
    PRESS(0.88, 1.05, 1.15, 1.20),
    OFF_COVERAGE(1.12, 0.90, 0.85, 0.90);

    /** Multiplier on completion chance (higher = easier catch). */
    public final double completionMod;
    /** Multiplier on deep yards / YAC. */
    public final double yardsMod;
    /** Multiplier on INT chance. */
    public final double intMod;
    /** Multiplier affecting scramble / designed QB keep success (lower = spy works). */
    public final double scrambleMod;

    CoverageCall(double completionMod, double yardsMod, double intMod, double scrambleMod) {
        this.completionMod = completionMod;
        this.yardsMod = yardsMod;
        this.intMod = intMod;
        this.scrambleMod = scrambleMod;
    }

    public double runFitBonus() {
        return this == STACK_BOX ? 8.0 : 0.0;
    }

    public double passFitBonus() {
        if (this == COVER_4 || this == COVER_2) return 4.0;
        if (this == STACK_BOX) return -6.0;
        return 0.0;
    }
}
