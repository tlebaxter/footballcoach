package CFBsimPack.engine;

import java.util.Random;

/**
 * Tunable foul types with approximate per-play rates and enforcement metadata.
 */
public final class PenaltyCatalog {

    public enum Enforcement {
        PREVIOUS_SPOT,
        SPOT
    }

    public enum Foul {
        FALSE_START(5, true, 0.018, Enforcement.PREVIOUS_SPOT, false, true, false, true),
        OFFSIDES(5, false, 0.012, Enforcement.PREVIOUS_SPOT, false, true, false, false),
        HOLDING(10, true, 0.022, Enforcement.PREVIOUS_SPOT, false, true, false, true),
        DPI(15, false, 0.010, Enforcement.SPOT, true, true, false, false),
        ROUGHING_PASSER(15, false, 0.006, Enforcement.PREVIOUS_SPOT, true, true, false, false),
        TARGETING(15, false, 0.004, Enforcement.PREVIOUS_SPOT, true, true, true, true),
        /** Pre-snap only; excluded from {@link #roll}. */
        DELAY_OF_GAME(5, true, 0.0, Enforcement.PREVIOUS_SPOT, false, true, false, true);

        public final int yards;
        public final boolean againstOffense;
        public final double baseRate;
        public final Enforcement enforcement;
        public final boolean autoFirstDown;
        public final boolean halfDistance;
        public final boolean ejects;
        public final boolean triggersTenSecondRunoff;

        Foul(
                int yards,
                boolean againstOffense,
                double baseRate,
                Enforcement enforcement,
                boolean autoFirstDown,
                boolean halfDistance,
                boolean ejects,
                boolean triggersTenSecondRunoff) {
            this.yards = yards;
            this.againstOffense = againstOffense;
            this.baseRate = baseRate;
            this.enforcement = enforcement != null ? enforcement : Enforcement.PREVIOUS_SPOT;
            this.autoFirstDown = autoFirstDown;
            this.halfDistance = halfDistance;
            this.ejects = ejects;
            this.triggersTenSecondRunoff = triggersTenSecondRunoff;
        }
    }

    private PenaltyCatalog() {}

    public static Foul roll(Random rng, OffensePlay play) {
        return roll(rng, play, 1.0);
    }

    /**
     * @param roadNoiseMult 1.0 for home offense; {@link AtmosphereEngine#roadNoiseMult}
     *                      when the visitor snaps (scales false start / light offsides).
     */
    public static Foul roll(Random rng, OffensePlay play, double roadNoiseMult) {
        return roll(rng, play, roadNoiseMult, false);
    }

    /**
     * @param contactPlay true when the live play completed with contact (run gain / catch),
     *                    used to slightly raise targeting chance.
     */
    public static Foul roll(Random rng, OffensePlay play, double roadNoiseMult, boolean contactPlay) {
        if (rng == null) return null;
        double noise = roadNoiseMult > 0 ? roadNoiseMult : 1.0;
        for (Foul f : Foul.values()) {
            if (f == Foul.DELAY_OF_GAME) continue;
            double rate = rateFor(f, play, noise, contactPlay);
            if (rate <= 0) continue;
            if (rng.nextDouble() < rate) return f;
        }
        return null;
    }

    /** Exposed for tests / tuning — effective per-play rate after play-type and noise mods. */
    public static double rateFor(Foul foul, OffensePlay play, double roadNoiseMult) {
        return rateFor(foul, play, roadNoiseMult, false);
    }

    public static double rateFor(Foul foul, OffensePlay play, double roadNoiseMult, boolean contactPlay) {
        if (foul == null || foul == Foul.DELAY_OF_GAME) return 0;
        double rate = foul.baseRate;
        double noise = roadNoiseMult > 0 ? roadNoiseMult : 1.0;
        if (play == OffensePlay.PASS && (foul == Foul.DPI || foul == Foul.ROUGHING_PASSER || foul == Foul.HOLDING)) {
            rate *= 1.15;
        }
        if (play == OffensePlay.RUN && foul == Foul.HOLDING) {
            rate *= 1.1;
        }
        if (foul == Foul.TARGETING) {
            if (play == OffensePlay.PUNT || play == OffensePlay.FIELD_GOAL || play == OffensePlay.KICKOFF) {
                return 0;
            }
            if (play == OffensePlay.PASS || play == OffensePlay.RUN) {
                rate *= contactPlay ? 1.35 : 0.85;
            }
        }
        if (play == OffensePlay.PUNT || play == OffensePlay.FIELD_GOAL || play == OffensePlay.KICKOFF) {
            if (foul == Foul.DPI || foul == Foul.ROUGHING_PASSER) return 0;
            rate *= 0.4;
        }
        if (foul == Foul.FALSE_START) {
            rate *= noise;
        } else if (foul == Foul.OFFSIDES) {
            // Light crowd effect on defensive jump
            rate *= 1.0 + (noise - 1.0) * 0.4;
        }
        return rate;
    }
}
