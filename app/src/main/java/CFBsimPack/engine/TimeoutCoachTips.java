package CFBsimPack.engine;

/**
 * Pure coach tips for when to call a timeout / change tempo.
 * First matching tip wins; UI is responsible for dismiss/spam control.
 */
public final class TimeoutCoachTips {

    public enum TipId {
        RUNOFF_EXPIRES,
        END_OF_HALF,
        LATE_GAME_TRAILING,
        CHEW_WHILE_TRAILING,
        DOG_RISK_CHEW,
    }

    public static final class Tip {
        public final TipId id;
        public final String message;

        public Tip(TipId id, String message) {
            this.id = id;
            this.message = message;
        }
    }

    private TimeoutCoachTips() {}

    public static Tip suggest(GameSituation sit, TempoCall tempo) {
        if (sit == null) return null;
        if (sit.gameOver || sit.awaitingCoinToss || sit.playingOT) return null;
        if (!sit.canCallTimeout) return null;
        TempoCall t = tempo != null ? tempo : TempoCall.NORMAL;
        int runoff = t.runoffSeconds();

        if (sit.clockRunning && runoffWouldExpire(sit, runoff)) {
            return new Tip(TipId.RUNOFF_EXPIRES,
                    "Next snap runoff ends the half/game — call timeout or switch to Hurry.");
        }

        if (sit.userOnOffense && sit.clockRunning && sit.quarter == 2 && sit.clockInQuarter <= 45) {
            return new Tip(TipId.END_OF_HALF, "End of half — timeout to stop the clock.");
        }

        int deficit = userDeficit(sit);
        if (sit.userOnOffense && sit.clockRunning && sit.quarter == 4
                && sit.clockInQuarter <= 120 && deficit >= 1 && deficit <= 16) {
            return new Tip(TipId.LATE_GAME_TRAILING, "Late game — timeout to preserve clock.");
        }

        if (sit.userOnOffense && t == TempoCall.CHEW_CLOCK && sit.quarter == 4
                && sit.clockInQuarter <= 180 && deficit >= 1) {
            return new Tip(TipId.CHEW_WHILE_TRAILING,
                    "Trailing — hurry-up (chew risks delay of game).");
        }

        if (sit.userOnOffense && t == TempoCall.CHEW_CLOCK && secondsLeftInHalf(sit) <= 90) {
            return new Tip(TipId.DOG_RISK_CHEW,
                    "Chewing the clock — delay of game risk; timeout resets the play clock.");
        }

        return null;
    }

    static boolean runoffWouldExpire(GameSituation sit, int runoff) {
        if (sit.playingOT || runoff <= 0) return false;
        if (sit.gameTime <= runoff) return true;
        return sit.quarter == 2 && sit.clockInQuarter <= runoff;
    }

    static int secondsLeftInHalf(GameSituation sit) {
        if (sit.playingOT) return 0;
        if (sit.quarter == 1 || sit.quarter == 3) {
            return Math.max(0, sit.clockInQuarter) + 900;
        }
        return Math.max(0, sit.clockInQuarter);
    }

    /** Positive when the user (possession team) is behind. Requires user on offense. */
    static int userDeficit(GameSituation sit) {
        if (!sit.userOnOffense) return 0;
        int user = sit.possessionHome ? sit.homeScore : sit.awayScore;
        int opp = sit.possessionHome ? sit.awayScore : sit.homeScore;
        return opp - user;
    }
}
