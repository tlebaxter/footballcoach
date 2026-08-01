package CFBsimPack;

/**
 * Named player composites derived from {@link PlayerRatings}.
 * Values are 0–1 (fraction of 100) for engine math convenience.
 */
public final class CompositeWeights {

    private CompositeWeights() {}

    public static double composite(PlayerRatings r, String name) {
        if (r == null) return 0.5;
        switch (name) {
            case "passingAccuracy":
                return w(r, "tha", 1.0, "hgt", 0.2);
            case "passingDeep":
                return w(r, "thp", 1.0, "tha", 0.1, "hgt", 0.2);
            case "passingVision":
                return w(r, "thv", 1.0, "hgt", 0.5, "footIq", 0.3);
            case "athleticism":
                return w(r, "stre", 1.0, "spd", 1.0, "hgt", 0.2);
            case "rushing":
                return w(r, "stre", 0.5, "spd", 1.0, "elu", 1.0);
            case "catching":
                return w(r, "hgt", 0.2, "hnd", 1.0);
            case "gettingOpen":
                return w(r, "hgt", 1.0, "spd", 0.25, "rtr", 2.0, "hnd", 1.0);
            case "speed":
                return w(r, "spd", 1.0);
            case "passBlocking":
                return w(r, "hgt", 0.5, "stre", 1.0, "spd", 0.2, "pbk", 1.0);
            case "runBlocking":
                return w(r, "hgt", 0.5, "stre", 1.0, "spd", 0.4, "rbk", 1.0);
            case "passRushing":
                return w(r, "hgt", 1.0, "stre", 1.0, "spd", 0.5, "prs", 1.0, "tck", 0.25);
            case "runStopping":
                return w(r, "hgt", 0.5, "stre", 1.0, "spd", 0.5, "rns", 1.0, "tck", 0.4);
            case "passCoverage":
                return w(r, "hgt", 0.1, "spd", 1.0, "pcv", 1.0);
            case "tackling":
                return w(r, "spd", 1.0, "stre", 1.0, "tck", 2.5);
            case "avoidingSacks":
                return w(r, "thv", 1.0, "elu", 1.0, "stre", 0.25);
            case "ballSecurity":
                return w(r, "bsc", 1.0, "stre", 0.2);
            case "endurance":
                return w(r, "endu", 1.0);
            case "kickingPower":
                return w(r, "kpw", 1.0);
            case "kickingAccuracy":
                return w(r, "kac", 1.0);
            case "punting":
                return w(r, "ppw", 1.0, "pac", 1.0);
            case "returning":
                return w(r, "spd", 2.0, "elu", 1.0, "bsc", 0.5);
            default:
                return 0.5;
        }
    }

    /** 0–100 scale for UI / OVR blending. */
    public static int composite100(PlayerRatings r, String name) {
        return (int) Math.round(composite(r, name) * 100.0);
    }

    private static double w(PlayerRatings r, Object... keyWeightPairs) {
        double sum = 0;
        double weightSum = 0;
        for (int i = 0; i + 1 < keyWeightPairs.length; i += 2) {
            String key = (String) keyWeightPairs[i];
            double wt = ((Number) keyWeightPairs[i + 1]).doubleValue();
            int val = "footIq".equals(key) ? r.footIq : r.get(key);
            sum += (val / 100.0) * wt;
            weightSum += wt;
        }
        if (weightSum <= 0) return 0.5;
        return sum / weightSum;
    }
}
