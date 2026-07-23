package CFBsimPack;

/**
 * NIL / program budget helpers. Amounts are whole dollars.
 */
public final class NilMoney {
    public static final int ROSTER_CAP = 105;
    public static final int SCHOLARSHIP_CAP = 85;
    public static final int ROSTER_TARGET = 90;
    public static final int ROSTER_MIN = 46;

    public static final int SUG_QB = 2;
    public static final int SUG_RB = 4;
    public static final int SUG_FB = 2;
    public static final int SUG_WR = 6;
    public static final int SUG_TE = 3;
    public static final int SUG_OL = 10;
    public static final int SUG_K = 2;
    public static final int SUG_S = 3;
    public static final int SUG_CB = 5;
    public static final int SUG_EDGE = 4;
    public static final int SUG_DL = 5;
    public static final int SUG_LB = 5;

    public static final int INIT_QB = 4;
    public static final int INIT_RB = 7;
    public static final int INIT_FB = 3;
    public static final int INIT_WR = 11;
    public static final int INIT_TE = 5;
    public static final int INIT_OL = 16;
    public static final int INIT_K = 3;
    public static final int INIT_S = 4;
    public static final int INIT_CB = 10;
    public static final int INIT_EDGE = 7;
    public static final int INIT_DL = 9;
    public static final int INIT_LB = 9;

    public static final String[] POSITIONS = {
            "QB", "RB", "FB", "WR", "TE", "OL", "K", "S", "CB", "EDGE", "DL", "LB"
    };

    private NilMoney() {}

    public static String format(int dollars) {
        if (dollars < 0) dollars = 0;
        if (dollars >= 1_000_000) {
            double m = dollars / 1_000_000.0;
            if (m >= 10) {
                return "$" + Math.round(m) + "M";
            }
            return String.format("$%.2fM", m).replace(".00M", "M").replace("0M", "M");
        }
        if (dollars >= 1_000) {
            double k = dollars / 1_000.0;
            if (k >= 100) {
                return "$" + Math.round(k) + "K";
            }
            if (Math.abs(k - Math.round(k)) < 0.05) {
                return "$" + Math.round(k) + "K";
            }
            return String.format("$%.1fK", k);
        }
        return "$" + dollars;
    }

    public static int yearlyBudget(int prestige) {
        if (prestige < 40) prestige = 40;
        if (prestige > 99) prestige = 99;
        double p = prestige;
        double budget = 400_000 + Math.pow(p, 2.15) * 650;
        return (int) Math.round(budget / 1000.0) * 1000;
    }

    public static int scholarshipCoa(int prestige) {
        if (prestige < 40) prestige = 40;
        if (prestige > 99) prestige = 99;
        return 45000 + (prestige - 40) * 750;
    }

    public static double positionPremium(String position) {
        if (position == null) return 1.0;
        switch (position) {
            case "QB": return 1.55;
            case "RB": return 1.20;
            case "FB": return 0.95;
            case "WR": return 1.25;
            case "TE": return 1.15;
            case "OL": return 1.05;
            case "EDGE": return 1.20;
            case "DL": return 1.10;
            case "LB": return 1.10;
            case "CB": return 1.15;
            case "S": return 1.05;
            case "K": return 0.70;
            default: return 1.0;
        }
    }

    public static int sugFor(String pos) {
        if (pos == null) return 4;
        switch (pos) {
            case "QB": return SUG_QB;
            case "RB": return SUG_RB;
            case "FB": return SUG_FB;
            case "WR": return SUG_WR;
            case "TE": return SUG_TE;
            case "OL": return SUG_OL;
            case "K": return SUG_K;
            case "S": return SUG_S;
            case "CB": return SUG_CB;
            case "EDGE": return SUG_EDGE;
            case "DL": return SUG_DL;
            case "LB": return SUG_LB;
            default: return 4;
        }
    }

    public static int initFor(String pos) {
        if (pos == null) return 8;
        switch (pos) {
            case "QB": return INIT_QB;
            case "RB": return INIT_RB;
            case "FB": return INIT_FB;
            case "WR": return INIT_WR;
            case "TE": return INIT_TE;
            case "OL": return INIT_OL;
            case "K": return INIT_K;
            case "S": return INIT_S;
            case "CB": return INIT_CB;
            case "EDGE": return INIT_EDGE;
            case "DL": return INIT_DL;
            case "LB": return INIT_LB;
            default: return 8;
        }
    }

    public static int marketValue(Player p) {
        if (p == null) return 25000;
        int ovr = p.ratOvr;
        double premium = positionPremium(p.position);

        double base;
        if (ovr < 60) {
            base = 25000 + (ovr - 50) * 5000;
        } else if (ovr < 70) {
            base = 75000 + (ovr - 60) * 25000;
        } else if (ovr < 80) {
            base = 350000 + (ovr - 70) * 40000;
        } else if (ovr < 90) {
            base = 750000 + (ovr - 80) * 150000;
        } else {
            base = 2250000 + (ovr - 90) * 400000;
        }

        base *= premium;
        base *= youthPremium(p);

        if (p.careerHeismans > 0 || p.wonHeisman) base *= 1.25;
        else if (p.careerAllAmerican > 0 || p.wonAllAmerican) base *= 1.15;
        else if (p.careerAllConference > 0 || p.wonAllConference) base *= 1.06;

        int value = (int) Math.round(base / 1000.0) * 1000;
        if (value < 25000) value = 25000;
        if (value > 6000000) value = 6000000;
        return value;
    }

    public static double youthPremium(Player p) {
        if (p == null) return 1.0;
        int year = p.year;
        int gap = Math.max(0, p.ratPot - p.ratOvr);
        double potBump = 1.0 + Math.min(0.45, gap / 80.0);

        double age;
        if (year <= 1) age = 1.55;
        else if (year == 2) age = 1.30;
        else if (year == 3) age = 1.05;
        else if (year == 4) age = 0.82;
        else age = 0.70;

        if (year >= 3 && p.ratOvr >= 80) age *= 0.92;
        if (year <= 2 && p.ratOvr < 70 && gap >= 12) age *= potBump;
        else if (year <= 2) age *= Math.min(1.25, potBump);

        return age;
    }

    public static int offerCashCost(RosterStatus status, int nilAmount, int prestige) {
        int coa = status != null && status.usesScholarship() ? scholarshipCoa(prestige) : 0;
        int nil = (status == RosterStatus.SCHOLARSHIP_PLUS_NIL) ? Math.max(0, nilAmount) : 0;
        return coa + nil;
    }

    public static int buyoutCost(Player p, int prestige) {
        if (p == null) return 0;
        int remainingYears = Math.max(0, p.contractYearsRemaining);
        if (remainingYears <= 0 && (p.rosterStatus == null || p.rosterStatus == RosterStatus.PWO)) {
            return 0;
        }
        int years = Math.max(1, remainingYears);
        if (remainingYears <= 0 && p.rosterStatus != null && p.rosterStatus.usesScholarship()) {
            years = 1;
        }
        int annual = offerCashCost(p.rosterStatus, p.nilDealAmount, prestige);
        double remaining = annual * (double) years;
        double rate = 0.35;
        if (p.year <= 2) rate += 0.25;
        else if (p.year == 3) rate += 0.10;
        else rate -= 0.05;
        if (p.contractYearsRemaining >= 2) rate += 0.12;
        if (p.ratOvr >= 85 || p.ratPot - p.ratOvr >= 15) rate += 0.10;
        if (p.year >= 4) rate = Math.min(rate, 0.45);
        if (rate < 0.25) rate = 0.25;
        if (rate > 0.85) rate = 0.85;
        int cost = (int) Math.round(remaining * rate / 1000.0) * 1000;
        return Math.max(0, cost);
    }

    /**
     * Guarantee paid by home to away for a buy game.
     * Higher when home is much stronger than the visitor.
     */
    public static int buyGameGuarantee(int homePrestige, int awayPrestige) {
        int home = clampPrestige(homePrestige);
        int away = clampPrestige(awayPrestige);
        int gap = Math.max(0, home - away);
        double base = 150_000 + gap * 18_000 + (100 - away) * 2_500;
        return (int) Math.round(base / 1000.0) * 1000;
    }

    /** Optional win bonus (~15% of guarantee) paid by home if away wins. */
    public static int buyGameWinBonus(int guarantee) {
        if (guarantee <= 0) {
            return 0;
        }
        return (int) Math.round(guarantee * 0.15 / 1000.0) * 1000;
    }

    /**
     * Cancel fee for an OOC contract. Buy deals scale with remaining guarantees;
     * H&amp;H / single use a flat prestige-style floor.
     */
    public static int oocCancelBuyout(OocContract.Type type, int remainingGuarantees, int lengthYears) {
        int years = Math.max(1, lengthYears);
        if (type == OocContract.Type.BUY || remainingGuarantees > 0) {
            int fromGuarantees = (int) Math.round(remainingGuarantees * 0.50 / 1000.0) * 1000;
            int floor = 100_000 * years;
            return Math.max(floor, fromGuarantees);
        }
        if (type == OocContract.Type.HOME_AND_HOME) {
            return 250_000 * years;
        }
        return 75_000;
    }

    /** Breach fine when a deal passes its fulfill-by year unsettled (~1.25× cancel buyout). */
    public static int oocBreachFine(int cancelBuyout) {
        int base = Math.max(75_000, cancelBuyout);
        return (int) Math.round(base * 1.25 / 1000.0) * 1000;
    }

    private static int clampPrestige(int prestige) {
        if (prestige < 40) return 40;
        if (prestige > 99) return 99;
        return prestige;
    }
}
