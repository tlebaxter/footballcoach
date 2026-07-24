package CFBsimPack.engine;

import CFBsimPack.ProgramProfile;

/**
 * Live home-crowd atmosphere: seeded from program identity / matchup context,
 * then updated snap-by-snap. Transient game state only — does not mutate
 * {@link ProgramProfile} season factors.
 */
public final class AtmosphereEngine {

    /** Same gap {@code OocContractBook} uses for buy-game booking. */
    public static final int BUY_GAME_TIER_GAP = 12;

    private AtmosphereEngine() {}

    /**
     * Seed {@link GameState#crowdBaseline} / {@link GameState#crowdEnergy} from
     * home fanbase, tradition, momentum, rivalry, and buy-game / road-dog tier gap.
     */
    public static void seed(
            GameState state,
            ProgramProfile home,
            ProgramProfile away,
            int rivalryStrength,
            boolean buyGame,
            boolean postseason
    ) {
        if (state == null) return;
        int homeFan = factor(home != null ? home.fanbase : 50);
        int homeTrad = factor(home != null ? home.tradition : 50);
        int homeMom = factor(home != null ? home.momentum : 50);
        int homeTier = home != null ? home.scheduleTier : 50;
        int awayTier = away != null ? away.scheduleTier : 50;
        int rivalry = clamp(rivalryStrength, 0, 100);

        // Fanbase 25→42, 99→72 (primary driver; leaves headroom for rivalry / buy-game)
        int baseline = 42 + (homeFan - 25) * 30 / 74;
        // Tradition +0…8
        baseline += (homeTrad - 25) * 8 / 74;
        // Momentum hot ≥70 → +0…5
        if (homeMom >= 70) {
            baseline += (homeMom - 70) * 5 / 29;
        }
        // Rivalry continuous +0…18
        baseline += rivalry * 18 / 100;

        int tierGap = homeTier - awayTier;
        if (buyGame || tierGap >= BUY_GAME_TIER_GAP) {
            int gap = Math.max(tierGap, buyGame ? BUY_GAME_TIER_GAP : tierGap);
            // +6…12 for big house vs soft visitor
            baseline += 6 + clamp((gap - BUY_GAME_TIER_GAP) * 6 / 20, 0, 6);
        } else if (tierGap <= -BUY_GAME_TIER_GAP) {
            // Road underdog home hosting a much bigger visitor → damp
            baseline -= 4 + clamp((-tierGap - BUY_GAME_TIER_GAP) * 4 / 20, 0, 4);
        }

        if (postseason) {
            baseline = (int) Math.round(baseline * 0.55);
        }

        baseline = clamp(baseline, 0, 100);
        state.crowdBaseline = baseline;
        state.crowdEnergy = baseline;
        state.crowdRivalry = rivalry;
    }

    /**
     * Adjust crowd energy from the last snap: event spikes, score context,
     * then mean-revert toward baseline.
     *
     * @param possessionHomeBefore true if home had the ball when the snap started
     * @param downBefore           down at snap start (1–4)
     * @param yardsNeedBefore      distance at snap start
     */
    public static void afterSnap(
            GameState state,
            PlayResult result,
            boolean possessionHomeBefore,
            int downBefore,
            int yardsNeedBefore
    ) {
        if (state == null || result == null) return;

        int delta = 0;
        double rivalryAmp = 1.0 + state.crowdRivalry / 200.0;

        if (result.touchdown) {
            delta += possessionHomeBefore ? 8 : -6;
        } else if (result.scoreFg || result.scoreXp) {
            delta += possessionHomeBefore ? 3 : -2;
        }

        if (result.turnover) {
            // Defense of home gets the ball → home crowd spikes
            delta += possessionHomeBefore ? -6 : 8;
        }

        if (result.sack && !possessionHomeBefore) {
            delta += 3;
        } else if (result.sack && possessionHomeBefore) {
            delta -= 2;
        }

        // 3rd-down stop (no conversion, no score)
        if (downBefore == 3
                && !result.firstDown
                && !result.touchdown
                && !result.turnover
                && result.yardsGained < yardsNeedBefore) {
            delta += possessionHomeBefore ? -2 : 3;
        }

        if (delta != 0) {
            state.crowdEnergy += (int) Math.round(delta * rivalryAmp);
        }

        // Score / clock context
        int margin = state.homeScore - state.awayScore;
        int q = state.quarter();
        boolean late = q >= 4 || state.playingOT;
        if (late && Math.abs(margin) <= 8) {
            state.crowdEnergy += 2;
        }
        if (margin >= 21) {
            // Blowout: flatten toward baseline − 10
            int flatTarget = Math.max(0, state.crowdBaseline - 10);
            state.crowdEnergy += (int) Math.round((flatTarget - state.crowdEnergy) * 0.15);
        } else if (late && margin <= -8) {
            // Home trailing late — upset / desperation spike
            state.crowdEnergy += 3;
        }

        // Mean reversion (~8% toward baseline)
        state.crowdEnergy += (int) Math.round((state.crowdBaseline - state.crowdEnergy) * 0.08);
        state.crowdEnergy = clamp(state.crowdEnergy, 0, 100);
    }

    /**
     * Offense yardage / completion modifier: roughly −2…+6.
     * Home gets help from energy; away gets asymmetric noise (road dogs feel
     * big buildings more than home offenses are helped).
     */
    public static int offenseBonus(GameState state) {
        if (state == null) return 0;
        double e = state.crowdEnergy / 100.0;
        if (state.possessionHome) {
            return (int) Math.round(e * 6.0);
        }
        // Away: up to about −2.5 at peak Hostile
        return (int) Math.round(-e * 2.5);
    }

    /**
     * Multiplier for road false-start / DOG rates. Home offense always 1.0;
     * away scales 1.0…~1.9 with crowd energy.
     */
    public static double roadNoiseMult(GameState state) {
        if (state == null || state.possessionHome) return 1.0;
        return 1.0 + (state.crowdEnergy / 100.0) * 0.9;
    }

    /**
     * Extra pressure / INT points when the road offense is in a loud building.
     */
    public static int roadPressureAdd(GameState state) {
        if (state == null || state.possessionHome) return 0;
        return (int) Math.round((state.crowdEnergy / 100.0) * 8.0);
    }

    public static String band(int energy) {
        int e = clamp(energy, 0, 100);
        if (e < 35) return "Quiet";
        if (e < 55) return "Steady";
        if (e < 70) return "Loud";
        if (e < 85) return "Electric";
        return "Hostile";
    }

    public static String band(GameState state) {
        return band(state != null ? state.crowdEnergy : 50);
    }

    public static boolean isPostseason(String gameName) {
        if (gameName == null || gameName.isEmpty()) return false;
        String n = gameName.toLowerCase();
        return n.contains("bowl")
                || n.contains("semi")
                || n.contains("ncg")
                || n.contains("playoff")
                || n.contains("national championship");
    }

    private static int factor(int value) {
        return clamp(value, 25, 99);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
