package CFBsimPack;

import java.util.Random;

/**
 * CFB class-year development: pot converges toward peak by Jr/Sr; no long decline.
 * Offseason gains scale with on-field snaps and position skill usage.
 */
public final class DevelopmentCurve {

    private DevelopmentCurve() {}

    /**
     * Advance one season of development. Updates ratings, ratOvr, ratImprovement, year.
     * Uses {@link Player#seasonSnaps} and {@link Player#seasonStats} for usage-driven gains.
     */
    public static void advance(Player p, int programDevBonus, Random rng) {
        if (p == null) return;
        Random r = rng != null ? rng : new Random();
        p.recordSeasonSnapshot();
        int oldOvr = p.ratOvr;
        p.year++;

        PlayerRatings ratings = p.ratings != null ? p.ratings : new PlayerRatings();
        PositionGroup pos = PositionOvr.primaryGroup(p);
        double yearFactor = yearGrowthFactor(p.year, p.isRedshirt);
        double usageFactor = usageFactor(p, pos);
        double potFactor = (ratings.pot - 40) / 60.0;
        double expectedGain = (1.2 + yearFactor * 2.5 + usageFactor * 1.5 + potFactor * 1.5)
                + programDevBonus * 0.35;
        if (expectedGain < 0.15) expectedGain = 0.15;

        // Soft convergence: as ovr approaches pot, gains shrink
        int primaryOvr = PositionOvr.ovr(ratings, pos);
        int headroom = Math.max(0, ratings.pot - primaryOvr);
        double converge = headroom <= 0 ? 0.15 : Math.min(1.0, headroom / 25.0);
        expectedGain *= (0.35 + 0.65 * converge);

        String[] growKeys = growthKeys(pos);
        double[] weights = growthWeights(pos, p.seasonStats);
        double weightSum = 0;
        for (double w : weights) weightSum += w;
        if (weightSum <= 0) weightSum = growKeys.length;

        for (int i = 0; i < growKeys.length; i++) {
            double share = weights[i] / weightSum;
            double roll = r.nextDouble();
            int delta = (int) Math.round(expectedGain * share * growKeys.length * (0.4 + roll));
            if (delta > 0) ratings.bump(growKeys[i], delta);
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

    /** Blended snaps + skill volume in [0, ~1.25]. */
    static double usageFactor(Player p, PositionGroup pos) {
        double snapFactor = clamp(p.seasonSnaps / (double) expectedSnaps(pos), 0, 1.25);
        int skillVol = skillVolume(p.seasonStats, pos);
        if (skillVol <= 0) {
            return snapFactor;
        }
        double skillFactor = clamp(skillVol / (double) expectedSkillVolume(pos), 0, 1.25);
        // OL / defense have no skill bag — skillVolume returns 0 above
        return 0.7 * snapFactor + 0.3 * skillFactor;
    }

    static int expectedSnaps(PositionGroup pos) {
        if (pos == null) return 650;
        switch (pos) {
            case RB:
            case WR:
            case TE:
            case FB:
                return 350;
            case K:
            case P:
                return 40;
            default:
                return 650;
        }
    }

    static int expectedSkillVolume(PositionGroup pos) {
        if (pos == null) return 1;
        switch (pos) {
            case QB:
                return 400;
            case RB:
            case FB:
                return 220;
            case WR:
            case TE:
                return 80;
            case K:
                return 40;
            case P:
                return 55;
            default:
                return 1;
        }
    }

    static int skillVolume(PlayerSkillStats s, PositionGroup pos) {
        if (s == null || pos == null) return 0;
        switch (pos) {
            case QB:
                return s.passAtt + s.rushAtt;
            case RB:
            case FB:
                return s.rushAtt + s.targets;
            case WR:
            case TE:
                return s.targets;
            case K:
                return s.fgAtt + s.xpAtt;
            case P:
                return s.puntAtt;
            default:
                return 0;
        }
    }

    private static double[] growthWeights(PositionGroup pos, PlayerSkillStats s) {
        String[] keys = growthKeys(pos);
        double[] w = new double[keys.length];
        for (int i = 0; i < w.length; i++) w[i] = 1.0;
        if (s == null || pos == null) return w;

        switch (pos) {
            case QB: {
                int total = Math.max(1, s.passAtt + s.rushAtt);
                double rushShare = s.rushAtt / (double) total;
                for (int i = 0; i < keys.length; i++) {
                    String k = keys[i];
                    if ("elu".equals(k) || "spd".equals(k) || "bsc".equals(k)) {
                        w[i] = 0.6 + rushShare * 1.4;
                    } else if ("tha".equals(k) || "thp".equals(k) || "thv".equals(k)) {
                        w[i] = 0.6 + (1.0 - rushShare) * 1.4;
                    }
                }
                break;
            }
            case RB: {
                int total = Math.max(1, s.rushAtt + s.targets);
                double recvShare = s.targets / (double) total;
                for (int i = 0; i < keys.length; i++) {
                    String k = keys[i];
                    if ("hnd".equals(k) || "bsc".equals(k)) {
                        w[i] = 0.5 + recvShare * 1.5;
                    } else if ("spd".equals(k) || "elu".equals(k) || "stre".equals(k)) {
                        w[i] = 0.5 + (1.0 - recvShare) * 1.5;
                    }
                }
                break;
            }
            case WR:
            case TE: {
                double catchRate = s.targets > 0 ? s.receptions / (double) s.targets : 0.5;
                double yacBias = s.receptions > 0 ? Math.min(1.0, s.recYards / (double) (s.receptions * 12)) : 0.5;
                for (int i = 0; i < keys.length; i++) {
                    String k = keys[i];
                    if ("hnd".equals(k) || "rtr".equals(k)) {
                        w[i] = 0.7 + catchRate;
                    } else if ("spd".equals(k) || "elu".equals(k)) {
                        w[i] = 0.6 + yacBias;
                    }
                }
                break;
            }
            case K:
            case P: {
                int vol = skillVolume(s, pos);
                for (int i = 0; i < w.length; i++) {
                    w[i] = vol > 0 ? 1.0 : 0.15;
                }
                break;
            }
            default:
                break;
        }
        return w;
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

    private static double clamp(double v, double lo, double hi) {
        if (v < lo) return lo;
        if (v > hi) return hi;
        return v;
    }
}
