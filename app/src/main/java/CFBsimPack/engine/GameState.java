package CFBsimPack.engine;

/**
 * Mutable live-game situation.
 */
public final class GameState {
    public static final int REG_SECONDS = 3600;
    public static final int TIMEOUTS_PER_HALF = 3;
    public static final int TIMEOUTS_PER_OT = 1;

    public int homeScore;
    public int awayScore;
    public int[] homeQScore = new int[10];
    public int[] awayQScore = new int[10];
    public int homeYards;
    public int awayYards;
    public int homeTOs;
    public int awayTOs;
    public int numOT;

    /** Seconds remaining in regulation (3600 → 0). OT uses -1. */
    public int gameTime = REG_SECONDS;
    public boolean possessionHome = true;
    public int yardLine = 25;
    public int down = 1;
    public int yardsNeed = 10;
    public GamePhase phase = GamePhase.REGULATION;
    public boolean bottomOT;
    public boolean playingOT;

    public int homeTimeouts = TIMEOUTS_PER_HALF;
    public int awayTimeouts = TIMEOUTS_PER_HALF;
    /**
     * True after the first snap of the current half (or once OT begins).
     * Timeouts cannot be called until the half is underway.
     */
    public boolean halfUnderway;
    /**
     * True after a live-ball play while regulation clock should keep running
     * between snaps. Cleared by timeout, stopped-clock plays, DOG, half/OT reset.
     * Pending between-play runoff is applied at the start of the next snap.
     */
    public boolean clockRunning;
    /**
     * NCAA 10-second runoff pending under 1:00 of a half after injury / certain fouls.
     * Cleared by timeout or applied at the start of the next snap.
     */
    public boolean pendingTenSecondRunoff;

    public String lastPlayLog = "";
    public boolean gameOver;

    /** Next snap is a kickoff (or free kick) by the team with possession. */
    public boolean pendingKickoff;
    /** Free kick after safety (from the 20). */
    public boolean freeKick;

    /** After a TD: waiting for PAT / 2-point decision or a 2-point snap. */
    public boolean pendingTry;
    /** True while coach/AI has not yet chosen Kick XP vs Go for 2. */
    public boolean tryAwaitingChoice;
    /** True when the try is a 2-point conversion snap (not an XP kick). */
    public boolean tryIsTwoPoint;

    /** Waiting for coin-toss election (receive/defer + end). */
    public boolean awaitingCoinToss;
    public boolean homeWonToss;
    /** Toss winner deferred the ball choice to the second half. */
    public boolean deferred;
    public boolean homeReceivesFirstHalf;
    /** Home team's end zone is on the left of the field display. */
    public boolean homeDefendsLeft = true;
    public boolean tossResolved;

    /** Live home-crowd intensity (0–100), fluctuates during the game. */
    public int crowdEnergy = 50;
    /** Seeded atmosphere target for mean-reversion (set at kickoff). */
    public int crowdBaseline = 50;
    /** Rivalry strength used to amplify crowd spikes (0–100). */
    public int crowdRivalry;

    public boolean isSpecialTeamsDown() {
        return pendingKickoff || (down >= 4 && !pendingTry);
    }

    public void clearTry() {
        pendingTry = false;
        tryAwaitingChoice = false;
        tryIsTwoPoint = false;
    }

    public int quarter() {
        if (playingOT || phase == GamePhase.OT) return 5 + Math.max(0, numOT - 1);
        if (gameTime <= 0) return 4;
        return Math.min(4, (REG_SECONDS - gameTime) / 900 + 1);
    }

    public int clockInQuarter() {
        if (playingOT) return 0;
        int q = quarter();
        return gameTime - 900 * (4 - q);
    }

    public String clockDisplay() {
        if (playingOT) return "OT";
        int rem = Math.max(0, clockInQuarter());
        int min = rem / 60;
        int sec = rem % 60;
        return String.format("%d:%02d", min, sec);
    }

    public void resetTimeoutsForHalf() {
        homeTimeouts = TIMEOUTS_PER_HALF;
        awayTimeouts = TIMEOUTS_PER_HALF;
    }

    public void resetTimeoutsForOt() {
        homeTimeouts = TIMEOUTS_PER_OT;
        awayTimeouts = TIMEOUTS_PER_OT;
    }

    /** NCAA-style: dead ball after half/OT has started, with timeouts remaining. */
    public boolean canCallTimeout(boolean home) {
        if (gameOver || awaitingCoinToss || !halfUnderway) return false;
        return home ? homeTimeouts > 0 : awayTimeouts > 0;
    }

    public boolean callTimeout(boolean home) {
        if (!canCallTimeout(home)) return false;
        if (home) {
            homeTimeouts--;
        } else {
            awayTimeouts--;
        }
        clockRunning = false;
        pendingTenSecondRunoff = false;
        return true;
    }

    /** True in Q2/Q4 with ≤60 seconds left in the quarter (not OT). */
    public boolean underOneMinuteInHalf() {
        if (playingOT || phase == GamePhase.OT) return false;
        int q = quarter();
        return (q == 2 || q == 4) && clockInQuarter() <= 60;
    }

    public TeamSide offenseSide() {
        return possessionHome ? TeamSide.HOME : TeamSide.AWAY;
    }

    public enum TeamSide { HOME, AWAY }
}
