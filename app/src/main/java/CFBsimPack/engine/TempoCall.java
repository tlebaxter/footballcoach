package CFBsimPack.engine;

public enum TempoCall {
    NORMAL(1.0, 28, 0.025, 1.0, 3),
    HURRY_UP(0.55, 12, 0.005, 1.5, 0),
    CHEW_CLOCK(1.35, 38, 0.08, 1.0, 4);

    /** Multiplier on typical in-play clock burn. */
    public final double clockMult;
    /** Between-play runoff applied at the start of the next snap when the clock is running. */
    public final int runoffSeconds;
    /** Pre-snap delay-of-game probability for this tempo. */
    public final double delayOfGameRate;
    /** Multiplier on per-snap fatigue drain for both elevens. */
    public final double fatigueDrainMult;
    /** Sideline energy recovery after a snap for players not on the field. */
    public final int benchRecover;

    TempoCall(
            double clockMult,
            int runoffSeconds,
            double delayOfGameRate,
            double fatigueDrainMult,
            int benchRecover) {
        this.clockMult = clockMult;
        this.runoffSeconds = runoffSeconds;
        this.delayOfGameRate = delayOfGameRate;
        this.fatigueDrainMult = fatigueDrainMult;
        this.benchRecover = benchRecover;
    }

    public int runoffSeconds() {
        return runoffSeconds;
    }

    public double delayOfGameRate() {
        return delayOfGameRate;
    }
}
