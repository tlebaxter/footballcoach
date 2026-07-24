package CFBsimPack.engine;

/**
 * Accept/decline so the fouling team is worse off when possible.
 */
public final class PenaltyResolver {

    private PenaltyResolver() {}

    public static void resolve(PendingPlay pending) {
        if (pending == null || pending.foul == null || pending.result == null) return;
        PenaltyCatalog.Foul foul = pending.foul;
        PlayState accepted = pending.before.copy();
        int spot = accepted.yardLine;

        if (foul.againstOffense) {
            accepted.yardLine = Math.max(1, spot - foul.yards);
            accepted.down = pending.before.down;
            accepted.yardsNeed = Math.min(99, pending.before.yardsNeed + foul.yards);
        } else {
            accepted.yardLine = Math.min(99, spot + foul.yards);
            int gained = accepted.yardLine - spot;
            if (foul == PenaltyCatalog.Foul.DPI || foul == PenaltyCatalog.Foul.ROUGHING_PASSER
                    || gained >= pending.before.yardsNeed) {
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
            pending.result.logLine = (pending.result.logLine != null ? pending.result.logLine + " " : "")
                    + "PENALTY: " + foul.name().replace('_', ' ') + " on " + side
                    + ", " + foul.yards + " yards (accepted).";
        } else {
            pending.result.logLine = (pending.result.logLine != null ? pending.result.logLine + " " : "")
                    + "PENALTY: " + foul.name().replace('_', ' ') + " declined.";
        }
    }
}
