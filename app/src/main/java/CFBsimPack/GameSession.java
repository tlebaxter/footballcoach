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
    /** True while the user has stepped out of Talent Hub to browse Main mid-offseason. */
    private static boolean stayingOnMainDuringOffseason = false;
    /** Save slot last loaded or manually saved; null until the user picks one. */
    private static Integer activeSaveSlot = null;
    /** Set when a coached game finishes so Main can autosave + confirm. */
    private static boolean pendingCoachResultSave = false;
    private static String pendingCoachResultSummary = null;

    private GameSession() {}

    public static void setLeague(League l) {
        league = l;
    }

    public static void setActiveSaveSlot(Integer slot) {
        activeSaveSlot = slot;
    }

    public static Integer getActiveSaveSlot() {
        return activeSaveSlot;
    }

    public static boolean hasActiveSaveSlot() {
        return activeSaveSlot != null;
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
        stayingOnMainDuringOffseason = false;
        activeSaveSlot = null;
        pendingCoachResultSave = false;
        pendingCoachResultSummary = null;
        OffseasonSession.clear();
    }

    public static void setStayingOnMainDuringOffseason(boolean staying) {
        stayingOnMainDuringOffseason = staying;
    }

    public static boolean isStayingOnMainDuringOffseason() {
        return stayingOnMainDuringOffseason;
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

    /**
     * Clears the active coach game and, if it finished, marks Main to autosave / confirm.
     */
    public static void finishCoachGame(Game g) {
        if (g != null && g.hasPlayed) {
            pendingCoachResultSave = true;
            Team user = league != null ? league.userTeam : null;
            if (user != null && (g.homeTeam == user || g.awayTeam == user)) {
                int userScore = g.homeTeam == user ? g.homeScore : g.awayScore;
                int oppScore = g.homeTeam == user ? g.awayScore : g.homeScore;
                pendingCoachResultSummary = userScore + "-" + oppScore;
            } else {
                pendingCoachResultSummary = g.homeScore + "-" + g.awayScore;
            }
        } else {
            pendingCoachResultSave = false;
            pendingCoachResultSummary = null;
        }
        activeCoachGame = null;
    }

    public static boolean consumePendingCoachResultSave() {
        boolean pending = pendingCoachResultSave;
        pendingCoachResultSave = false;
        return pending;
    }

    public static String consumePendingCoachResultSummary() {
        String s = pendingCoachResultSummary;
        pendingCoachResultSummary = null;
        return s;
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
        stayingOnMainDuringOffseason = false;
    }

    public static void beginOffseason(League l, LeagueOffseason off, OffseasonSession.Phase startPhase) {
        setLeague(l);
        OffseasonSession.begin(l, off, startPhase);
        pendingOffseasonResult = OffseasonResult.NONE;
        stayingOnMainDuringOffseason = false;
    }

    public static void clearOffseason() {
        OffseasonSession.clear();
        pendingOffseasonResult = OffseasonResult.NONE;
        pendingRemainingBudget = -1;
        stayingOnMainDuringOffseason = false;
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
