package CFBsimPack;

/**
 * Process-scoped holder for the live League used by Compose screens.
 * Offseason state continues to live in {@link OffseasonSession} so League save/load
 * paths stay untouched.
 */
public final class GameSession {

    public enum OffseasonResult {
        NONE,
        DONE_RETENTION,
        DONE_TRANSFER_PORTAL,
        DONE_SCHEDULE,
        DONE_RECRUITING
    }

    /** True when a new career still needs the user to finish OOC scheduling. */
    private static boolean needsOocScheduling = false;

    private static League league;
    private static OffseasonResult pendingOffseasonResult = OffseasonResult.NONE;
    private static int pendingRemainingBudget = -1;
    /** True when a brand-new league still needs the user to pick a team. */
    private static boolean needsTeamPicker = false;
    /** Active live-coach game (optional HC mode). */
    private static Game activeCoachGame;

    private GameSession() {}

    public static void setLeague(League l) {
        league = l;
    }

    public static void setNeedsTeamPicker(boolean needs) {
        needsTeamPicker = needs;
    }

    public static boolean needsTeamPicker() {
        return needsTeamPicker;
    }

    public static League getLeague() {
        return league;
    }

    public static boolean hasLeague() {
        return league != null;
    }

    public static void setNeedsOocScheduling(boolean needs) {
        needsOocScheduling = needs;
    }

    public static boolean needsOocScheduling() {
        return needsOocScheduling;
    }

    public static void clearAll() {
        league = null;
        pendingOffseasonResult = OffseasonResult.NONE;
        pendingRemainingBudget = -1;
        needsTeamPicker = false;
        needsOocScheduling = false;
        activeCoachGame = null;
        OffseasonSession.clear();
    }

    public static void setActiveCoachGame(Game g) {
        activeCoachGame = g;
    }

    public static Game getActiveCoachGame() {
        return activeCoachGame;
    }

    public static void clearActiveCoachGame() {
        activeCoachGame = null;
    }

    public static boolean readyOffseason() {
        return OffseasonSession.ready();
    }

    public static LeagueOffseason getOffseason() {
        return OffseasonSession.offseason;
    }

    public static OffseasonSession.Phase getPhase() {
        return OffseasonSession.phase;
    }

    public static void setPhase(OffseasonSession.Phase phase) {
        OffseasonSession.phase = phase;
    }

    public static void beginOffseason(League l, LeagueOffseason off) {
        setLeague(l);
        OffseasonSession.begin(l, off);
        pendingOffseasonResult = OffseasonResult.NONE;
    }

    public static void beginOffseason(League l, LeagueOffseason off, OffseasonSession.Phase startPhase) {
        setLeague(l);
        OffseasonSession.begin(l, off, startPhase);
        pendingOffseasonResult = OffseasonResult.NONE;
    }

    public static void clearOffseason() {
        OffseasonSession.clear();
        pendingOffseasonResult = OffseasonResult.NONE;
        pendingRemainingBudget = -1;
    }

    public static void setPendingOffseasonResult(OffseasonResult result) {
        pendingOffseasonResult = result != null ? result : OffseasonResult.NONE;
    }

    public static OffseasonResult getPendingOffseasonResult() {
        return pendingOffseasonResult;
    }

    public static void setPendingRemainingBudget(int budget) {
        pendingRemainingBudget = budget;
    }

    public static int getPendingRemainingBudget() {
        return pendingRemainingBudget;
    }

    public static OffseasonResult consumePendingOffseasonResult() {
        OffseasonResult r = pendingOffseasonResult;
        pendingOffseasonResult = OffseasonResult.NONE;
        return r;
    }
}
