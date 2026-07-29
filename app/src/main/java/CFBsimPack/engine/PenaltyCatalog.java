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

    /** When a foul may be rolled relative to the snap. */
    public enum Phase {
        PRE_SNAP,
        LIVE
    }

    public enum Foul {
        FALSE_START(5, true, 0.016, Enforcement.PREVIOUS_SPOT, false, true, false, true, Phase.PRE_SNAP),
        OFFSIDES(5, false, 0.010, Enforcement.PREVIOUS_SPOT, false, true, false, false, Phase.PRE_SNAP),
        HOLDING(10, true, 0.010, Enforcement.PREVIOUS_SPOT, false, true, false, true, Phase.LIVE),
        DPI(15, false, 0.008, Enforcement.SPOT, true, true, false, false, Phase.LIVE),
        ROUGHING_PASSER(15, false, 0.003, Enforcement.PREVIOUS_SPOT, true, true, false, false, Phase.LIVE),
        TARGETING(15, false, 0.0015, Enforcement.PREVIOUS_SPOT, true, true, true, true, Phase.LIVE),
        /** Pre-snap only; excluded from catalog rolls (handled in Game). */
        DELAY_OF_GAME(5, true, 0.0, Enforcement.PREVIOUS_SPOT, false, true, false, true, Phase.PRE_SNAP);

        public final int yards;
        public final boolean againstOffense;
        public final double baseRate;
        public final Enforcement enforcement;
        public final boolean autoFirstDown;
        public final boolean halfDistance;
        public final boolean ejects;
        public final boolean triggersTenSecondRunoff;
        public final Phase phase;

        Foul(
                int yards,
                boolean againstOffense,
                double baseRate,
                Enforcement enforcement,
                boolean autoFirstDown,
                boolean halfDistance,
                boolean ejects,
                boolean triggersTenSecondRunoff,
                Phase phase) {
            this.yards = yards;
            this.againstOffense = againstOffense;
            this.baseRate = baseRate;
            this.enforcement = enforcement != null ? enforcement : Enforcement.PREVIOUS_SPOT;
            this.autoFirstDown = autoFirstDown;
            this.halfDistance = halfDistance;
            this.ejects = ejects;
            this.triggersTenSecondRunoff = triggersTenSecondRunoff;
            this.phase = phase != null ? phase : Phase.LIVE;
        }
    }

    private PenaltyCatalog() {}

    /** Pre-snap fouls only (false start / offsides). */
    public static Foul rollPreSnap(Random rng, OffensePlay play, double roadNoiseMult) {
        return rollPhase(rng, play, null, roadNoiseMult, false, Phase.PRE_SNAP);
    }

    /** Live-ball fouls after the play resolves. */
    public static Foul rollLive(
            Random rng, OffensePlay play, PlayResult result, double roadNoiseMult, boolean contactPlay) {
        return rollPhase(rng, play, result, roadNoiseMult, contactPlay, Phase.LIVE);
    }

    /** Compatibility: rolls live fouls only. Prefer {@link #rollPreSnap} / {@link #rollLive}. */
    public static Foul roll(Random rng, OffensePlay play) {
        return rollLive(rng, play, null, 1.0, false);
    }

    /**
     * Compatibility: rolls live fouls only.
     *
     * @param roadNoiseMult 1.0 for home offense; {@link AtmosphereEngine#roadNoiseMult}
     *                      when the visitor snaps (scales false start / light offsides).
     */
    public static Foul roll(Random rng, OffensePlay play, double roadNoiseMult) {
        return rollLive(rng, play, null, roadNoiseMult, false);
    }

    /**
     * Compatibility: rolls live fouls only.
     *
     * @param contactPlay true when the live play completed with contact (run gain / catch),
     *                    used to slightly raise targeting chance.
     */
    public static Foul roll(Random rng, OffensePlay play, double roadNoiseMult, boolean contactPlay) {
        return rollLive(rng, play, null, roadNoiseMult, contactPlay);
    }

    private static Foul rollPhase(
            Random rng,
            OffensePlay play,
            PlayResult result,
            double roadNoiseMult,
            boolean contactPlay,
            Phase phase) {
        if (rng == null || phase == null) return null;
        double noise = roadNoiseMult > 0 ? roadNoiseMult : 1.0;
        for (Foul f : Foul.values()) {
            if (f == Foul.DELAY_OF_GAME) continue;
            if (f.phase != phase) continue;
            double rate = rateFor(f, play, result, noise, contactPlay);
            if (rate <= 0) continue;
            if (rng.nextDouble() < rate) return f;
        }
        return null;
    }

    /** Exposed for tests / tuning — effective per-play rate after play-type and noise mods. */
    public static double rateFor(Foul foul, OffensePlay play, double roadNoiseMult) {
        return rateFor(foul, play, null, roadNoiseMult, false);
    }

    public static double rateFor(Foul foul, OffensePlay play, double roadNoiseMult, boolean contactPlay) {
        return rateFor(foul, play, null, roadNoiseMult, contactPlay);
    }

    public static double rateFor(
            Foul foul, OffensePlay play, PlayResult result, double roadNoiseMult, boolean contactPlay) {
        if (foul == null || foul == Foul.DELAY_OF_GAME) return 0;
        if (!eligible(foul, play, result, contactPlay)) return 0;

        double rate = foul.baseRate;
        double noise = roadNoiseMult > 0 ? roadNoiseMult : 1.0;

        if (foul == Foul.TARGETING) {
            rate *= contactPlay ? 1.35 : 0.85;
        }
        if (foul == Foul.FALSE_START) {
            rate *= noise;
        } else if (foul == Foul.OFFSIDES) {
            // Light crowd effect on defensive jump
            rate *= 1.0 + (noise - 1.0) * 0.4;
        }
        return rate;
    }

    static boolean eligible(Foul foul, OffensePlay play, PlayResult result, boolean contactPlay) {
        if (foul == null || play == null) return false;
        switch (foul) {
            case FALSE_START:
            case OFFSIDES:
                return isScrimmage(play);
            case HOLDING:
                return play == OffensePlay.RUN
                        || play == OffensePlay.PASS
                        || play == OffensePlay.FAKE_PUNT;
            case DPI:
                return play == OffensePlay.PASS && passThrown(result);
            case ROUGHING_PASSER:
                return play == OffensePlay.PASS;
            case TARGETING:
                if (play == OffensePlay.SPIKE || play == OffensePlay.KNEEL) return false;
                if (play == OffensePlay.PUNT || play == OffensePlay.FIELD_GOAL || play == OffensePlay.KICKOFF) {
                    return false;
                }
                return (play == OffensePlay.RUN || play == OffensePlay.PASS) && contactPlay;
            case DELAY_OF_GAME:
            default:
                return false;
        }
    }

    private static boolean isScrimmage(OffensePlay play) {
        return play == OffensePlay.RUN
                || play == OffensePlay.PASS
                || play == OffensePlay.KNEEL
                || play == OffensePlay.SPIKE
                || play == OffensePlay.FAKE_PUNT;
    }

    /**
     * DPI requires a throw. With no {@link PlayResult} (unit tests), assume a pass play throws.
     * Sacks are ineligible; otherwise a non-null result counts as a throw when
     * {@code passArriveYardLine > 0} or the play was not a sack.
     */
    private static boolean passThrown(PlayResult result) {
        if (result == null) return true;
        if (result.passArriveYardLine > 0) return true;
        return !result.sack;
    }
}
