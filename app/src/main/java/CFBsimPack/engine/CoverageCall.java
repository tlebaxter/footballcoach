package CFBsimPack.engine;

/**
 * Per-snap defensive coverage call (atop DefensiveSystem).
 * Identity only — outcome behavior lives in CoverageAssigner / PassTimeline / SafetyHelp.
 */
public enum CoverageCall {
    COVER_0,
    COVER_1,
    COVER_2,
    COVER_3,
    COVER_4,
    MAN,
    ZONE,
    STACK_BOX,
    SPY,
    PRESS,
    OFF_COVERAGE
}
