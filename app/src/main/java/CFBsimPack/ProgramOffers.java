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
        if (amount > 6000000) amount = 6000000;
        return amount;
    }

    public static int yearOneCost(Team team, RosterStatus status, int annualNil, int years) {
        if (team == null) return 0;
        return NilMoney.offerCashCost(status, annualNil, team.teamPrestige);
    }

    public static int nilAmountFor(Player p, Team destination) {
        int base = NilMoney.marketValue(p);
        double fit = 1.0;
        TransferReason reason = p.transferReason != null ? p.transferReason : TransferReason.NONE;
        Team prior = p.priorTeam != null ? p.priorTeam : p.team;

        int destPrestige = destination != null ? destination.teamPrestige : 70;
        int priorPrestige = prior != null ? prior.teamPrestige : destPrestige;
        int depthRank = projectedDepthRank(p, destination);

        switch (reason) {
            case PLAYING_TIME:
                if (depthRank <= 1) fit = 0.75;
                else if (depthRank <= 2) fit = 0.90;
                else fit = 1.25;
                break;
            case MOVE_UP:
                if (destPrestige > priorPrestige + 8) fit = 0.80;
                else if (destPrestige >= priorPrestige) fit = 1.05;
                else fit = 1.35;
                break;
            case COACHING_CHANGE:
                fit = destPrestige >= priorPrestige ? 0.90 : 1.10;
                break;
            case SCHEME_FIT:
                fit = depthRank <= 1 ? 0.85 : 1.15;
                break;
            case WINNING:
                if (destination != null && destination.rankTeamPollScore > 0 && destination.rankTeamPollScore <= 25) {
                    fit = 0.85;
                } else if (destPrestige >= 80) {
                    fit = 0.90;
                } else {
                    fit = 1.20;
                }
                break;
            case PRESTIGE_FREEFALL:
                fit = destination != null && destination.diffPrestige >= 0 ? 0.85 : 1.20;
                break;
            case TITLE_CHASE:
                if (destPrestige >= 88 || (destination != null && destination.totalNCs > 0)) fit = 0.80;
                else fit = 1.30;
                break;
            case INJURY_COMEBACK:
                fit = 0.90;
                base = (int) (base * 0.90);
                break;
            case NONE:
                fit = 0.95;
                break;
            case BETTER_FIT:
            default:
                fit = 1.0;
                break;
        }

        double brand = 1.0 + Math.max(0, destPrestige - 70) * 0.008;
        if (reason == TransferReason.MOVE_UP || reason == TransferReason.TITLE_CHASE) {
            brand = 1.0 + Math.max(0, destPrestige - 70) * 0.004;
        }

        int amount = (int) Math.round(base * fit * brand / 1000.0) * 1000;
        if (amount < 25000) amount = 25000;
        if (amount > 6000000) amount = 6000000;
        return amount;
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

    private static int offerOrdinal(RosterStatus s) {
        if (s == RosterStatus.SCHOLARSHIP_PLUS_NIL) return 2;
        if (s == RosterStatus.SCHOLARSHIP) return 1;
        return 0;
    }

    /**
     * Project NFL draft round 1–7, or 0 for UDFA / not declaring-level.
     */
    public static int projectDraftRound(Player p) {
        if (p == null || p.year < 3 || p.year >= 5) return 0;
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
            mult *= 1.0 + Math.max(0, t.teamPrestige - 70) * 0.004;
        }
        int amount = (int) Math.round(base * mult / 1000.0) * 1000;
        if (amount < 100000) amount = 100000;
        if (amount > 8000000) amount = 8000000;
        return amount;
    }

    public static String draftRoundLabel(int round) {
        if (round >= 1 && round <= 7) return "Rd " + round;
        return "UDFA";
    }
}
