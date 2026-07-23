package CFBsimPack.engine;

/**
 * Mutable live-game situation.
 */
public final class GameState {
    public static final int REG_SECONDS = 3600;
    public static final int TIMEOUTS_PER_HALF = 3;

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

    public String lastPlayLog = "";
    public boolean gameOver;

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

    public boolean callTimeout(boolean home) {
        if (home) {
            if (homeTimeouts <= 0) return false;
            homeTimeouts--;
            return true;
        }
        if (awayTimeouts <= 0) return false;
        awayTimeouts--;
        return true;
    }

    public TeamSide offenseSide() {
        return possessionHome ? TeamSide.HOME : TeamSide.AWAY;
    }

    public enum TeamSide { HOME, AWAY }
}
