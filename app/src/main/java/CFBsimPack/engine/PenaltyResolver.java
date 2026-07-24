package CFBsimPack.engine;

/**
 * Accept/decline so the fouling team is worse off when possible.
 * Enforces previous-spot or spot fouls with half-the-distance when near the goal.
 */
public final class PenaltyResolver {

    private PenaltyResolver() {}

    public static void resolve(PendingPlay pending) {
        if (pending == null || pending.foul == null || pending.result == null) return;
        PenaltyCatalog.Foul foul = pending.foul;
        PlayState accepted = pending.before.copy();
        int previousSpot = accepted.yardLine;
        int enforceFrom = previousSpot;
        if (foul.enforcement == PenaltyCatalog.Enforcement.SPOT && pending.foulSpotYardLine > 0) {
            enforceFrom = Math.max(1, Math.min(99, pending.foulSpotYardLine));
        }

        int enforcedYards;
        if (foul.againstOffense) {
            enforcedYards = yardsTowardOwnGoal(enforceFrom, foul.yards, foul.halfDistance);
            accepted.yardLine = Math.max(1, enforceFrom - enforcedYards);
            accepted.down = pending.before.down;
            accepted.yardsNeed = Math.min(99, pending.before.yardsNeed + enforcedYards);
        } else {
            enforcedYards = yardsTowardOpponentGoal(enforceFrom, foul.yards, foul.halfDistance);
            accepted.yardLine = Math.min(99, enforceFrom + enforcedYards);
            int gained = accepted.yardLine - previousSpot;
            if (foul.autoFirstDown || gained >= pending.before.yardsNeed) {
                accepted.down = 1;
                accepted.yardsNeed = Math.min(10, 100 - accepted.yardLine);
                if (accepted.yardsNeed < 1) accepted.yardsNeed = 1;
            } else {
                accepted.down = pending.before.down;
                accepted.yardsNeed = pending.before.yardsNeed - gained;
            }
        }

        boolean playWasTd = pending.result.touchdown;
        boolean playWasTurnover = pending.result.turnover;
        int playGain = pending.after.yardLine - pending.before.yardLine;

        boolean accept;
        if (foul.againstOffense) {
            // Defense accepts when the play gained enough / scored / turned over
            accept = playWasTd || playWasTurnover || playGain >= foul.yards;
        } else {
            // Offense accepts unless they already scored a TD
            accept = !playWasTd;
            if (playWasTurnover) accept = true;
        }

        pending.foulAccepted = accept;
        String side = foul.againstOffense ? "offense" : "defense";
        if (accept) {
            pending.after.down = accepted.down;
            pending.after.yardsNeed = accepted.yardsNeed;
            pending.after.yardLine = accepted.yardLine;
            pending.after.possessionHome = pending.before.possessionHome;
            pending.result.touchdown = false;
            pending.result.scoreFg = false;
            pending.result.safety = false;
            pending.result.turnover = false;
            pending.result.possessionChanged = false;
            StringBuilder log = new StringBuilder();
            if (pending.result.logLine != null && !pending.result.logLine.isEmpty()) {
                log.append(pending.result.logLine).append(' ');
            }
            log.append("PENALTY: ").append(foul.name().replace('_', ' ')).append(" on ").append(side);
            if (foul.enforcement == PenaltyCatalog.Enforcement.SPOT) {
                log.append(" at the ").append(enforceFrom);
            }
            log.append(", ").append(enforcedYards).append(" yards");
            if (foul.halfDistance && enforcedYards < foul.yards) {
                log.append(" (half the distance)");
            }
            if (foul.autoFirstDown) {
                log.append(", automatic first down");
            }
            log.append(" (accepted).");
            if (pending.ejectedPlayer != null) {
                log.append(" ").append(pending.ejectedPlayer.name).append(" ejected.");
            }
            pending.result.logLine = log.toString();
        } else {
            pending.result.logLine = (pending.result.logLine != null ? pending.result.logLine + " " : "")
                    + "PENALTY: " + foul.name().replace('_', ' ') + " declined.";
        }
    }

    /** Yards lost by offense (toward own goal). */
    public static int yardsTowardOwnGoal(int spot, int foulYards, boolean halfDistance) {
        int toGoal = Math.max(1, spot);
        int yards = Math.min(foulYards, toGoal - 1);
        if (yards < 1) yards = 1;
        if (halfDistance && foulYards >= toGoal) {
            yards = Math.max(1, toGoal / 2);
        }
        return yards;
    }

    /** Yards gained by offense (toward opponent goal). */
    public static int yardsTowardOpponentGoal(int spot, int foulYards, boolean halfDistance) {
        int toGoal = Math.max(1, 100 - spot);
        int yards = Math.min(foulYards, toGoal - 1);
        if (yards < 1) yards = 1;
        if (halfDistance && foulYards >= toGoal) {
            yards = Math.max(1, toGoal / 2);
        }
        return yards;
    }
}
