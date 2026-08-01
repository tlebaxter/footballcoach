package CFBsimPack;

/**
 * Per-position overall / potential from {@link PlayerRatings} composites.
 */
public final class PositionOvr {

    private PositionOvr() {}

    public static int ovr(PlayerRatings r, PositionGroup pos) {
        if (r == null || pos == null) return 50;
        double raw;
        switch (pos) {
            case QB:
                raw = blend(r,
                        "passingAccuracy", 3.0,
                        "passingDeep", 3.0,
                        "passingVision", 3.0,
                        "athleticism", 1.0,
                        "rushing", 1.0,
                        "avoidingSacks", 1.0,
                        "ballSecurity", 1.0);
                break;
            case RB:
                raw = blend(r,
                        "rushing", 10.0,
                        "catching", 2.0,
                        "gettingOpen", 1.0,
                        "passBlocking", 1.0,
                        "runBlocking", 1.0,
                        "ballSecurity", 1.0);
                break;
            case FB:
                raw = blend(r,
                        "runBlocking", 6.0,
                        "passBlocking", 2.0,
                        "rushing", 2.0,
                        "catching", 1.0,
                        "ballSecurity", 1.0);
                break;
            case WR:
                raw = blend(r,
                        "catching", 5.0,
                        "gettingOpen", 5.0,
                        "rushing", 1.0,
                        "ballSecurity", 1.0);
                break;
            case TE:
                raw = blend(r,
                        "catching", 2.0,
                        "gettingOpen", 2.0,
                        "passBlocking", 2.0,
                        "runBlocking", 2.0);
                break;
            case OL:
                raw = blend(r,
                        "passBlocking", 3.0,
                        "runBlocking", 3.0);
                break;
            case EDGE:
                raw = blend(r,
                        "passRushing", 6.0,
                        "runStopping", 3.0,
                        "tackling", 1.0);
                break;
            case DL:
                raw = blend(r,
                        "passRushing", 4.0,
                        "runStopping", 5.0,
                        "tackling", 1.0);
                break;
            case LB:
                raw = blend(r,
                        "passRushing", 2.0,
                        "runStopping", 2.0,
                        "passCoverage", 1.0,
                        "tackling", 4.0);
                break;
            case CB:
                raw = blend(r, "passCoverage", 4.2);
                break;
            case S:
                raw = blend(r,
                        "passCoverage", 2.0,
                        "tackling", 1.0);
                break;
            case K:
                raw = blend(r,
                        "kickingPower", 1.0,
                        "kickingAccuracy", 1.0);
                break;
            case P:
                raw = blend(r, "punting", 1.0);
                break;
            default:
                raw = 0.5;
        }
        // Soft position bias on the 0–100 scale (not on the 0–1 blend)
        double scaled = raw * 100.0;
        switch (pos) {
            case QB:
            case RB:
                scaled -= 2;
                break;
            case FB:
            case TE:
                scaled -= 1;
                break;
            case WR:
            case OL:
                scaled += 1;
                break;
            case K:
            case P:
                scaled *= 0.92;
                break;
            default:
                break;
        }
        int ovr = (int) Math.round(scaled + fudge(scaled));
        return PlayerRatings.clamp(ovr);
    }

    public static int pot(PlayerRatings r, PositionGroup pos) {
        if (r == null) return 50;
        int base = ovr(r, pos);
        int gap = Math.max(0, r.pot - base);
        // Potential OVR: current ovr plus unused pot headroom (scaled).
        return PlayerRatings.clamp(base + gap / 2);
    }

    public static int ovr(Player p, PositionGroup pos) {
        if (p == null) return 50;
        return ovr(p.ratings, pos);
    }

    public static int pot(Player p, PositionGroup pos) {
        if (p == null) return 50;
        return pot(p.ratings, pos);
    }

    public static PositionGroup primaryGroup(Player p) {
        if (p == null) return PositionGroup.LB;
        PositionGroup g = PositionGroup.fromToken(p.position);
        return g != null ? g : PositionGroup.LB;
    }

    private static double blend(PlayerRatings r, Object... nameCoeffPairs) {
        double sum = 0;
        double coeffSum = 0;
        for (int i = 0; i + 1 < nameCoeffPairs.length; i += 2) {
            String name = (String) nameCoeffPairs[i];
            double coeff = ((Number) nameCoeffPairs[i + 1]).doubleValue();
            sum += coeff * CompositeWeights.composite(r, name);
            coeffSum += coeff;
        }
        if (coeffSum <= 0) return 0.5;
        return sum / coeffSum;
    }

    /** Soft fudge so mid ratings feel familiar (~50–99 band). */
    private static double fudge(double r) {
        if (r >= 68) return 8;
        if (r >= 62) return 3 + (r - 62) * (5.0 / 6.0);
        if (r >= 55) return (r - 55) * (3.0 / 7.0);
        if (r >= 45) return -3 + (r - 45) * (3.0 / 10.0);
        return -5;
    }
}
