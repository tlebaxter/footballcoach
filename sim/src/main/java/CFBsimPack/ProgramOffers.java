package CFBsimPack;

import java.util.ArrayList;

/**
 * Destination-fit NIL pricing, contracts, draft projection, and offer acceptance.
 */
public final class ProgramOffers {
    private ProgramOffers() {}

    public static int maxContractYears(Player p) {
        if (p == null) return 1;
        // year 1=FR … 5=grad; remaining eligibility seasons
        int rem = 6 - p.year;
        if (rem < 1) rem = 1;
        if (rem > 4) rem = 4;
        return rem;
    }

    public static int suggestedContractYears(Player p) {
        int max = maxContractYears(p);
        if (p == null) return 1;
        if (p.year <= 1 && p.ratPot >= 80) return Math.min(max, 4);
        if (p.year <= 2 && p.ratOvr >= 75) return Math.min(max, 3);
        if (p.year >= 4) return 1;
        return Math.min(max, 2);
    }

    public static double lengthAnnualBump(Player p, int years) {
        if (years <= 1) return 1.0;
        double bump = 1.0 + (years - 1) * 0.08;
        if (p != null && p.year <= 2) bump += (years - 1) * 0.06;
        return bump;
    }

    public static int annualNilFor(Player p, Team destination, int years) {
        int base = nilAmountFor(p, destination);
        double bump = lengthAnnualBump(p, years);
        int amount = (int) Math.round(base * bump / 1000.0) * 1000;
        if (amount < 25000) amount = 25000;
        int ceiling = maxSingleDeal(destination);
        if (amount > ceiling) amount = ceiling;
        return amount;
    }

    public static int nilAmountFor(Player p, Team destination) {
        int base = NilMoney.marketValue(p);
        double multiplier = 1.0;
        TransferReason reason = p.transferReason != null ? p.transferReason : TransferReason.NONE;
        ProgramProfile profile = destination != null ? destination.programProfile : null;
        int brand = profile != null ? profile.brandAttract : 60;
        int pipeline = profile != null ? profile.pipeline : 60;
        int momentum = profile != null ? profile.momentum : 60;
        int depthRank = projectedDepthRank(p, destination);

        switch (reason) {
            case PLAYING_TIME:
                if (depthRank <= 1) multiplier *= 0.72;
                else if (depthRank <= 2) multiplier *= 0.86;
                else multiplier *= 1.22;
                break;
            case MOVE_UP:
                multiplier *= brand >= 82 ? 0.82 : brand >= 70 ? 1.0 : 1.25;
                break;
            case COACHING_CHANGE:
                multiplier *= momentum >= 70 ? 0.90 : 1.10;
                break;
            case SCHEME_FIT:
                multiplier *= depthRank <= 1 ? 0.82 : 1.12;
                break;
            case WINNING:
                if (destination != null && destination.rankTeamPollScore > 0 && destination.rankTeamPollScore <= 25) {
                    multiplier *= 0.84;
                } else if (momentum >= 80) {
                    multiplier *= 0.90;
                } else {
                    multiplier *= 1.18;
                }
                break;
            case PROGRAM_FREEFALL:
                multiplier *= profile != null && profile.diffProgramPower >= 0 ? 0.86 : 1.20;
                break;
            case TITLE_CHASE:
                multiplier *= brand >= 88 || (destination != null && destination.totalNCs > 0)
                        ? 0.78 : 1.28;
                break;
            case INJURY_COMEBACK:
                multiplier *= 0.90;
                base = (int) (base * 0.90);
                break;
            case NONE:
                multiplier *= 0.95;
                break;
            case BETTER_FIT:
            default:
                break;
        }

        multiplier *= 1.0 - Math.max(0, brand - 60) * 0.004;
        if (brand < 55) multiplier *= 1.0 + (55 - brand) * 0.006;
        if (p.year >= 3 || reason == TransferReason.TITLE_CHASE || reason == TransferReason.MOVE_UP) {
            multiplier *= 1.0 - Math.max(0, pipeline - 60) * 0.002;
        }
        multiplier *= deterministicMarketPreference(p, destination);

        int amount = (int) Math.round(base * multiplier / 1000.0) * 1000;
        if (amount < 25000) amount = 25000;
        if (amount > maxSingleDeal(destination)) amount = maxSingleDeal(destination);
        return amount;
    }

    private static double deterministicMarketPreference(Player player, Team destination) {
        String playerKey = player != null && player.name != null ? player.name : "PLAYER";
        String teamKey = destination != null && destination.abbr != null ? destination.abbr : "TEAM";
        int hash = (playerKey + ":" + teamKey).hashCode();
        int basisPoints = Math.floorMod(hash, 601) - 300;
        return 1.0 + basisPoints / 10_000.0;
    }

    public static int maxSingleDeal(Team destination) {
        if (destination == null || destination.programProfile == null) return 7_000_000;
        int concentrationLimit = (int) Math.round(
                NilMoney.yearlyBudget(destination.programProfile) * 0.20 / 1000.0) * 1000;
        return Math.max(500_000, Math.min(7_000_000, concentrationLimit));
    }

    public static int projectedDepthRank(Player p, Team destination) {
        if (p == null || destination == null) return 99;
        ArrayList<? extends Player> list = destination.getPositionList(p.position);
        if (list == null) return 99;
        int better = 0;
        for (Player other : list) {
            if (other != p && other.ratOvr >= p.ratOvr) better++;
        }
        return better + 1;
    }

    public static RosterStatus minimumAcceptable(Player p, int riskTier) {
        int ovr = p != null ? p.ratOvr : 60;
        if (riskTier >= 3 || ovr >= 85) return RosterStatus.SCHOLARSHIP_PLUS_NIL;
        if (riskTier >= 2 || ovr >= 72) return RosterStatus.SCHOLARSHIP;
        if (ovr >= 62) return RosterStatus.SCHOLARSHIP;
        return RosterStatus.PWO;
    }

    public static boolean acceptsOffer(Player p, RosterStatus offer, int riskTier) {
        RosterStatus min = minimumAcceptable(p, riskTier);
        return offerOrdinal(offer) >= offerOrdinal(min);
    }

    public static boolean acceptsOffer(
            Player p,
            Team destination,
            RosterStatus offer,
            int annualNil,
            int riskTier) {
        if (!acceptsOffer(p, offer, riskTier)) return false;
        if (offer != RosterStatus.SCHOLARSHIP_PLUS_NIL) {
            return minimumAcceptable(p, riskTier) != RosterStatus.SCHOLARSHIP_PLUS_NIL;
        }
        return annualNil >= nilAmountFor(p, destination);
    }

    private static int offerOrdinal(RosterStatus s) {
        if (s == RosterStatus.SCHOLARSHIP_PLUS_NIL) return 2;
        if (s == RosterStatus.SCHOLARSHIP) return 1;
        return 0;
    }

    /**
     * Project NFL draft round 1–7, or 0 for UDFA / not declaring-level.
     */
    public static int projectDraftRound(Player p) {
        if (p == null || p.year < 3) return 0;
        if ("K".equals(p.position)) return 0;

        int score = p.ratOvr;
        if (p.wonHeisman || p.careerHeismans > 0) score += 8;
        else if (p.wonAllAmerican || p.careerAllAmerican > 0) score += 5;
        else if (p.wonAllConference || p.careerAllConference > 0) score += 2;
        if (p.year == 3) score -= 2; // early declare slightly harder
        if ("QB".equals(p.position) || "WR".equals(p.position) || "CB".equals(p.position)) score += 1;

        if (score >= 94) return 1;
        if (score >= 91) return 2;
        if (score >= 88) return 3;
        if (score >= 85) return 4;
        if (score >= 82) return 5;
        if (score >= 79) return 6;
        if (score >= 76) return 7;
        return 0;
    }

    public static boolean isLockedDraftRound(int round) {
        return round >= 1 && round <= 3;
    }

    public static boolean canPayToStay(Player p) {
        if (p == null || p.year >= 5) return false;
        int r = p.projectedDraftRound > 0 ? p.projectedDraftRound : projectDraftRound(p);
        if (isLockedDraftRound(r)) return false;
        return r >= 4 || (r == 0 && p.year >= 3 && p.ratOvr > 90);
    }

    /**
     * One-time stay bonus from current budget (not multi-year encumbrance).
     */
    public static int draftStayBonus(Player p, Team t) {
        if (p == null) return 0;
        int round = p.projectedDraftRound > 0 ? p.projectedDraftRound : projectDraftRound(p);
        double base = NilMoney.marketValue(p);
        double mult;
        if (round == 4) mult = 1.35;
        else if (round == 5) mult = 1.10;
        else if (round == 6) mult = 0.85;
        else if (round == 7) mult = 0.65;
        else mult = 0.45; // UDFA-leaning early declare
        if (t != null) {
            int brand = t.programProfile != null ? t.programProfile.brandAttract : 60;
            int collective = t.programProfile != null ? t.programProfile.collectivePool : 60;
            mult *= 1.0 - Math.max(0, brand - 65) * 0.002;
            mult *= 1.0 + Math.max(0, collective - 75) * 0.0015;
        }
        int amount = (int) Math.round(base * mult / 1000.0) * 1000;
        if (amount < 100000) amount = 100000;
        if (amount > maxSingleDeal(t)) amount = maxSingleDeal(t);
        return amount;
    }

    public static String draftRoundLabel(int round) {
        if (round >= 1 && round <= 7) return "Rd " + round;
        return "UDFA";
    }
}
