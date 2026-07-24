package CFBsimPack;

import java.util.Random;

/**
 * CFB class-year development: pot converges toward peak by Jr/Sr; no long decline.
 */
public final class DevelopmentCurve {

    private DevelopmentCurve() {}

    /**
     * Advance one season of development. Updates ratings, ratOvr, ratImprovement, year.
     */
    public static void advance(Player p, int gamesPlayed, int programDevBonus, Random rng) {
        if (p == null) return;
        Random r = rng != null ? rng : new Random();
        p.recordSeasonSnapshot();
        int oldOvr = p.ratOvr;
        p.year++;

        PlayerRatings ratings = p.ratings != null ? p.ratings : new PlayerRatings();
        double yearFactor = yearGrowthFactor(p.year, p.isRedshirt);
        double playFactor = Math.max(0, gamesPlayed - 2) / 12.0;
        double potFactor = (ratings.pot - 40) / 60.0;
        double expectedGain = (1.2 + yearFactor * 2.5 + playFactor * 1.5 + potFactor * 1.5)
                + programDevBonus * 0.35;
        if (expectedGain < 0.15) expectedGain = 0.15;

        // Soft convergence: as ovr approaches pot, gains shrink
        int primaryOvr = PositionOvr.ovr(ratings, PositionOvr.primaryGroup(p));
        int headroom = Math.max(0, ratings.pot - primaryOvr);
        double converge = headroom <= 0 ? 0.15 : Math.min(1.0, headroom / 25.0);
        expectedGain *= (0.35 + 0.65 * converge);

        String[] growKeys = growthKeys(PositionOvr.primaryGroup(p));
        for (String key : growKeys) {
            double roll = r.nextDouble();
            int delta = (int) Math.round(expectedGain * (0.4 + roll));
            if (delta > 0) ratings.bump(key, delta);
        }
        // IQ grows slowly
        if (r.nextDouble() < 0.55) {
            ratings.bump("footIq", Math.max(0, (int) Math.round(expectedGain * 0.5)));
        }
        // Occasional breakthrough for high pot
        if (r.nextDouble() * 100 < ratings.pot * 0.35 && headroom > 5) {
            String k = growKeys[r.nextInt(growKeys.length)];
            ratings.bump(k, 1 + r.nextInt(3));
        }

        // Pot drifts toward ovr ceiling slightly (convergence), never declines ovr via aging
        if (ratings.pot < primaryOvr) {
            ratings.pot = primaryOvr;
        } else if (headroom > 0 && r.nextDouble() < 0.4) {
            // slight pot discovery or trim for seniors
            if (p.year >= 4 && headroom > 12) {
                ratings.pot = Math.max(primaryOvr, ratings.pot - 1);
            }
        }

        p.applyRatings(ratings);
        p.ratImprovement = p.ratOvr - oldOvr;
    }

    public static double yearGrowthFactor(int yearAfterIncrement, boolean redshirt) {
        // year already incremented: 2=So, 3=Jr, 4=Sr, 5=Grad
        int y = yearAfterIncrement;
        if (redshirt && y <= 2) return 1.15; // RS Fr / early still climbing hard
        if (y <= 2) return 1.2;   // Fr→So
        if (y == 3) return 1.0;   // So→Jr peak
        if (y == 4) return 0.55;  // Jr→Sr plateau
        return 0.25;             // Sr→Grad small gains
    }

    private static String[] growthKeys(PositionGroup pos) {
        if (pos == null) {
            return new String[]{"spd", "stre", "endu"};
        }
        switch (pos) {
            case QB:
                return new String[]{"tha", "thp", "thv", "elu", "bsc", "spd"};
            case RB:
                return new String[]{"spd", "elu", "stre", "bsc", "hnd"};
            case FB:
                return new String[]{"rbk", "pbk", "stre", "bsc", "hnd"};
            case WR:
                return new String[]{"hnd", "rtr", "spd", "elu", "hgt"};
            case TE:
                return new String[]{"hnd", "rbk", "pbk", "rtr", "hgt"};
            case OL:
                return new String[]{"pbk", "rbk", "stre", "hgt", "endu"};
            case EDGE:
                return new String[]{"prs", "spd", "stre", "tck", "rns"};
            case DL:
                return new String[]{"rns", "prs", "stre", "tck"};
            case LB:
                return new String[]{"tck", "rns", "pcv", "prs", "spd"};
            case CB:
                return new String[]{"pcv", "spd", "tck", "hgt"};
            case S:
                return new String[]{"pcv", "tck", "spd", "stre"};
            case K:
                return new String[]{"kpw", "kac"};
            case P:
                return new String[]{"ppw", "pac"};
            default:
                return new String[]{"spd", "stre", "endu"};
        }
    }
}
