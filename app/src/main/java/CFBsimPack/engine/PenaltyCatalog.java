package CFBsimPack.engine;

import java.util.Random;

/**
 * Tunable foul types with approximate per-play rates.
 */
public final class PenaltyCatalog {

    public enum Foul {
        FALSE_START(5, true, 0.018),
        OFFSIDES(5, false, 0.012),
        HOLDING(10, true, 0.022),
        DPI(15, false, 0.010),
        ROUGHING_PASSER(15, false, 0.006),
        /** Pre-snap only; excluded from {@link #roll}. */
        DELAY_OF_GAME(5, true, 0.0);

        public final int yards;
        public final boolean againstOffense;
        public final double baseRate;

        Foul(int yards, boolean againstOffense, double baseRate) {
            this.yards = yards;
            this.againstOffense = againstOffense;
            this.baseRate = baseRate;
        }
    }

    private PenaltyCatalog() {}

    public static Foul roll(Random rng, OffensePlay play) {
        if (rng == null) return null;
        for (Foul f : Foul.values()) {
            if (f == Foul.DELAY_OF_GAME) continue;
            double rate = f.baseRate;
            if (play == OffensePlay.PASS && (f == Foul.DPI || f == Foul.ROUGHING_PASSER || f == Foul.HOLDING)) {
                rate *= 1.15;
            }
            if (play == OffensePlay.RUN && f == Foul.HOLDING) {
                rate *= 1.1;
            }
            if (play == OffensePlay.PUNT || play == OffensePlay.FIELD_GOAL || play == OffensePlay.KICKOFF) {
                if (f == Foul.DPI || f == Foul.ROUGHING_PASSER) continue;
                rate *= 0.4;
            }
            if (rng.nextDouble() < rate) return f;
        }
        return null;
    }
}
