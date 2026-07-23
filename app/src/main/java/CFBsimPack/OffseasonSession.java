package CFBsimPack;

/**
 * Holds live offseason state across Talent Hub phases.
 */
public final class OffseasonSession {
    public enum Phase {
        RETENTION,
        PORTAL,
        SCHEDULE,
        HS
    }

    public static League league;
    public static LeagueOffseason offseason;
    public static Phase phase = Phase.RETENTION;

    private OffseasonSession() {}

    public static void clear() {
        league = null;
        offseason = null;
        phase = Phase.RETENTION;
    }

    public static boolean ready() {
        return league != null && offseason != null;
    }

    public static void begin(League l, LeagueOffseason off) {
        league = l;
        offseason = off;
        phase = Phase.RETENTION;
    }

    public static void begin(League l, LeagueOffseason off, Phase startPhase) {
        league = l;
        offseason = off;
        phase = startPhase != null ? startPhase : Phase.RETENTION;
    }

    public static Phase phaseFromString(String s) {
        if (s == null) return Phase.RETENTION;
        try {
            return Phase.valueOf(s.trim().toUpperCase());
        } catch (Exception e) {
            return Phase.RETENTION;
        }
    }

    public static String phaseLabel(Phase p) {
        if (p == Phase.PORTAL) return "Portal";
        if (p == Phase.SCHEDULE) return "Schedule";
        if (p == Phase.HS) return "HS";
        return "Retention";
    }
}
