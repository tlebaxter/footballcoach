package CFBsimPack.engine;

public enum TempoCall {
    NORMAL(1.0, 28, 0.025),
    HURRY_UP(0.55, 12, 0.005),
    CHEW_CLOCK(1.35, 38, 0.08);

    /** Multiplier on typical in-play clock burn. */
    public final double clockMult;
    /** Between-play runoff applied at the start of the next snap when the clock is running. */
    public final int runoffSeconds;
    /** Pre-snap delay-of-game probability for this tempo. */
    public final double delayOfGameRate;

    TempoCall(double clockMult, int runoffSeconds, double delayOfGameRate) {
        this.clockMult = clockMult;
        this.runoffSeconds = runoffSeconds;
        this.delayOfGameRate = delayOfGameRate;
    }

    public int runoffSeconds() {
        return runoffSeconds;
    }

    public double delayOfGameRate() {
        return delayOfGameRate;
    }
}
