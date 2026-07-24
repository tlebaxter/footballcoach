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
    public static final int SUG_K = 1;
    public static final int SUG_P = 1;
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
    public static final int INIT_K = 2;
    public static final int INIT_P = 2;
    public static final int INIT_S = 4;
    public static final int INIT_CB = 10;
    public static final int INIT_EDGE = 7;
    public static final int INIT_DL = 9;
    public static final int INIT_LB = 9;

    public static final String[] POSITIONS = {
            "QB", "RB", "FB", "WR", "TE", "OL", "K", "P", "S", "CB", "EDGE", "DL", "LB"
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

    public static int yearlyBudget(ProgramProfile profile) {
        if (profile == null) return yearlyBudget(50);
        return yearlyRevShare(profile) + yearlyCollective(profile);
    }

    /** Compatibility helper for isolated callers/tests using a capital score. */
    public static int yearlyBudget(int capitalScore) {
        int score = clampScore(capitalScore);
        double normalized = (score - 25) / 74.0;
        double total = 3_000_000 + Math.pow(normalized, 1.55) * 42_000_000;
        return roundToThousand(total);
    }

    public static int yearlyRevShare(ProgramProfile profile) {
        int score = profile != null ? profile.revSharePool : 50;
        double normalized = (clampScore(score) - 25) / 74.0;
        return roundToThousand(2_000_000 + Math.pow(normalized, 1.50) * 20_000_000);
    }

    public static int yearlyCollective(ProgramProfile profile) {
        int score = profile != null ? profile.collectivePool : 50;
        double normalized = (clampScore(score) - 25) / 74.0;
        return roundToThousand(500_000 + Math.pow(normalized, 2.40) * 24_500_000);
    }

    public static double positionPremium(String position) {
        if (position == null) return 1.0;
        switch (position) {
            case "QB": return 1.50;
            case "RB": return 0.92;
            case "FB": return 0.65;
            case "WR": return 1.18;
            case "TE": return 1.02;
            case "OL": return 1.08;
            case "EDGE": return 1.20;
            case "DL": return 1.10;
            case "LB": return 1.02;
            case "CB": return 1.16;
            case "S": return 0.98;
            case "K": return 0.50;
            case "P": return 0.48;
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
            case "P": return SUG_P;
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
            case "P": return INIT_P;
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
            base = 25_000 + Math.max(0, ovr - 50) * 5_000;
        } else if (ovr < 70) {
            base = 75_000 + (ovr - 60) * 20_000;
        } else if (ovr < 80) {
            base = 300_000 + (ovr - 70) * 45_000;
        } else if (ovr < 90) {
            base = 800_000 + (ovr - 80) * 150_000;
        } else {
            base = 2_400_000 + (ovr - 90) * 350_000;
        }

        base *= premium;
        base *= youthPremium(p);

        if (p.careerHeismans > 0 || p.wonHeisman) base *= 1.25;
        else if (p.careerAllAmerican > 0 || p.wonAllAmerican) base *= 1.15;
        else if (p.careerAllConference > 0 || p.wonAllConference) base *= 1.06;

        if (p.transferReason != null && p.transferReason != TransferReason.NONE) {
            double portalPremium = p.ratOvr >= 85 ? 1.40 : p.ratOvr >= 75 ? 1.25 : 1.15;
            base *= portalPremium;
        }

        int value = roundToThousand(base);
        if (value < 25000) value = 25000;
        if (value > 7000000) value = 7000000;
        return value;
    }

    public static double youthPremium(Player p) {
        if (p == null) return 1.0;
        int year = p.year;
        int gap = Math.max(0, p.ratPot - p.ratOvr);
        double potBump = 1.0 + Math.min(0.45, gap / 80.0);

        double age;
        if (year <= 1) age = 1.25;
        else if (year == 2) age = 1.18;
        else if (year == 3) age = 1.05;
        else if (year == 4) age = 0.82;
        else age = 0.70;

        if (year >= 3 && p.ratOvr >= 80) age *= 0.92;
        if (year <= 2 && p.ratOvr < 70 && gap >= 12) age *= potBump;
        else if (year <= 2) age *= Math.min(1.25, potBump);

        return age;
    }

    /** Purse cost of an offer: NIL only. Scholarships and PWOs are free. */
    public static int offerCashCost(RosterStatus status, int nilAmount, ProgramProfile profile) {
        return offerCashCost(status, nilAmount);
    }

    public static int offerCashCost(RosterStatus status, int nilAmount, int ignoredScore) {
        return offerCashCost(status, nilAmount);
    }

    public static int offerCashCost(RosterStatus status, int nilAmount) {
        return status == RosterStatus.SCHOLARSHIP_PLUS_NIL ? Math.max(0, nilAmount) : 0;
    }

    public static int buyoutCost(Player p, ProgramProfile profile) {
        if (p == null) return 0;
        int remainingYears = Math.max(0, p.contractYearsRemaining);
        if (remainingYears <= 0 && (p.rosterStatus == null || p.rosterStatus == RosterStatus.PWO)) {
            return 0;
        }
        int years = Math.max(1, remainingYears);
        if (remainingYears <= 0 && p.rosterStatus != null && p.rosterStatus.usesScholarship()) {
            years = 1;
        }
        int annual = offerCashCost(p.rosterStatus, p.nilDealAmount, profile);
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

    public static int buyoutCost(Player p, int revShareScore) {
        if (p == null) return 0;
        int remainingYears = Math.max(0, p.contractYearsRemaining);
        if (remainingYears <= 0 && (p.rosterStatus == null || p.rosterStatus == RosterStatus.PWO)) {
            return 0;
        }
        int years = Math.max(1, remainingYears);
        int annual = offerCashCost(p.rosterStatus, p.nilDealAmount, revShareScore);
        double rate = p.year <= 2 ? 0.60 : p.year == 3 ? 0.45 : 0.30;
        if (remainingYears >= 2) rate += 0.12;
        if (p.ratOvr >= 85 || p.ratPot - p.ratOvr >= 15) rate += 0.10;
        return roundToThousand(annual * years * Math.max(0.25, Math.min(0.85, rate)));
    }

    /** Max schedule-tier gap treated as a peer series (free home-and-home). */
    public static final int PEER_SERIES_TIER_GAP = 8;

    /**
     * Guarantee paid by home to away for a buy game.
     * Higher when home is much stronger than the visitor.
     */
    public static int buyGameGuarantee(ProgramProfile home, ProgramProfile away) {
        int homeTier = scheduleComposite(home);
        int awayTier = scheduleComposite(away);
        int gap = Math.max(0, homeTier - awayTier);
        double base = 600_000 + gap * 30_000 + (100 - awayTier) * 8_000;
        return roundToThousand(base);
    }

    public static int buyGameGuarantee(int homeScheduleTier, int awayScheduleTier) {
        int home = clampScore(homeScheduleTier);
        int away = clampScore(awayScheduleTier);
        int gap = Math.max(0, home - away);
        double base = 150_000 + gap * 18_000 + (100 - away) * 2_500;
        return (int) Math.round(base / 1000.0) * 1000;
    }

    /**
     * Appearance fee paid by a weaker home to bring a stronger visitor.
     * Grows exponentially with the composite gap so large mismatches are
     * effectively unaffordable.
     */
    public static int appearanceFee(ProgramProfile home, ProgramProfile away) {
        int homeTier = scheduleComposite(home);
        int awayTier = scheduleComposite(away);
        int gap = Math.max(0, awayTier - homeTier);
        if (gap <= 0) {
            return 0;
        }
        double fee = 250_000 * Math.pow(1.18, gap / 2.0) + gap * 50_000;
        return roundToThousand(fee);
    }

    /**
     * Nominal guarantee a near-peer visitor still collects. Real programs rarely
     * travel for free, so this never returns zero.
     */
    public static int visitorFloorGuarantee(ProgramProfile away) {
        return roundToThousand(150_000 + scheduleComposite(away) * 4_000.0);
    }

    /**
     * Single-game fee paid by home to away. Near-peer matchups still pay a
     * visitor floor; stronger home pays a buy-game guarantee; weaker home pays
     * an appearance fee to bring the bigger visitor in.
     */
    public static int singleGameGuarantee(ProgramProfile home, ProgramProfile away) {
        if (home == null || away == null) {
            return 0;
        }
        int tierGap = home.scheduleTier - away.scheduleTier;
        if (Math.abs(tierGap) <= PEER_SERIES_TIER_GAP) {
            return visitorFloorGuarantee(away);
        }
        if (tierGap > 0) {
            return buyGameGuarantee(home, away);
        }
        return appearanceFee(home, away);
    }

    /**
     * Fee owed on one home-and-home leg. Peer series trade home dates for free;
     * beyond the peer band only the weaker host owes an appearance fee.
     */
    public static int homeAndHomeLegFee(ProgramProfile home, ProgramProfile away) {
        if (home == null || away == null) {
            return 0;
        }
        int tierGap = home.scheduleTier - away.scheduleTier;
        if (tierGap >= -PEER_SERIES_TIER_GAP) {
            return 0;
        }
        return appearanceFee(home, away);
    }

    /** Optional win bonus (~15% of guarantee) paid by home if away wins. */
    public static int buyGameWinBonus(int guarantee) {
        if (guarantee <= 0) {
            return 0;
        }
        return (int) Math.round(guarantee * 0.15 / 1000.0) * 1000;
    }

    /**
     * Cancel fee for an OOC contract: a share of the money still owed on
     * unplayed dates, floored by deal type so free peer series still sting.
     */
    public static int oocCancelBuyout(
            OocContract.Type type,
            int remainingGuarantees,
            int lengthYears,
            int unsettledGameCount) {
        int years = Math.max(1, lengthYears);
        int games = Math.max(1, unsettledGameCount);
        int fromGuarantees = roundToThousand(Math.max(0, remainingGuarantees) * 0.55);
        int floor;
        if (type == OocContract.Type.HOME_AND_HOME) {
            floor = 200_000 * games;
        } else if (type == OocContract.Type.TWO_FOR_ONE) {
            floor = 150_000 * games;
        } else {
            floor = 100_000 * years;
        }
        return Math.max(floor, fromGuarantees);
    }

    private static int scheduleComposite(ProgramProfile profile) {
        if (profile == null) {
            return 50;
        }
        return (int) Math.round(profile.scheduleTier * 0.55 + profile.capitalPool * 0.45);
    }

    /** Breach fine when a deal passes its fulfill-by year unsettled (~1.25× cancel buyout). */
    public static int oocBreachFine(int cancelBuyout) {
        int base = Math.max(75_000, cancelBuyout);
        return (int) Math.round(base * 1.25 / 1000.0) * 1000;
    }

    private static int clampScore(int score) {
        if (score < 25) return 25;
        if (score > 99) return 99;
        return score;
    }

    private static int roundToThousand(double dollars) {
        return (int) Math.round(dollars / 1000.0) * 1000;
    }
}
