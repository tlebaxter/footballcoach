package CFBsimPack.engine;

public enum TempoCall {
    NORMAL(1.0),
    HURRY_UP(0.55),
    CHEW_CLOCK(1.35);

    /** Multiplier on typical play clock burn. */
    public final double clockMult;

    TempoCall(double clockMult) {
        this.clockMult = clockMult;
    }
}
