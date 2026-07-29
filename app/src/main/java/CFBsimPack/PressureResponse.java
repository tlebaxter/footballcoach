package CFBsimPack;

/**
 * Coach preference for how the QB should respond when pass pressure collapses the pocket.
 */
public enum PressureResponse {
    AUTO("Auto"),
    SCRAMBLE_FOR_IT("Scramble for it"),
    TAKE_THE_FIRST_DOWN("Take the first down"),
    SLIDE_SECURE("Slide / secure"),
    THROW_IT_AWAY("Throw it away"),
    FORCE_SIDELINE("Force sideline");

    public final String displayName;

    PressureResponse(String displayName) {
        this.displayName = displayName;
    }

    public static PressureResponse fromOrdinalSafe(int i) {
        PressureResponse[] v = values();
        if (i < 0 || i >= v.length) return AUTO;
        return v[i];
    }
}
