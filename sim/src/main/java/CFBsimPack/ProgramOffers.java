package CFBsimPack;

import java.util.ArrayList;

/**
 * Destination-fit NIL pricing, contracts, draft projection, and offer acceptance.
 */
public final class ProgramOffers {
    private ProgramOffers() {}

    public static int maxContractYears(Player p) {
        if (p == null) return 1;
        int rem = 6 - p.year;
        if (rem < 1) rem = 1;
        if (rem > 4) rem = 4;
        return rem;
    }

    public static int suggestedContractYears(Player p) {
        int max = maxContractYears(p);
        if (p == null) return 1;
        int draft = p.projectedDraftRound > 0 ? p.projectedDraftRound : projectDraftRound(p);
        if (draft >= 1 && draft <= 7) return 1;
        if (p.year >= 4) return 1;
        if (p.year <= 1 && p.ratPot >= 80) return Math.min(max, 4);
        if (p.year <= 2 && p.ratOvr >= 75) return Math.min(max, 3);
        return Math.min(max, 2);
    }

    public static double lengthAnnualBump(Player p, int years) {
        if (years <= 1) return 1.0;
        double bump = 1.0 + (years - 1) * 0.12;
        if (p != null && p.year <= 2) bump += (years - 1) * 0.08;
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
        if (p == null) return 25000;
        int talent = PlayerMarket.marketTalent(p);
        double premium = NilMoney.positionPremium(p.position);
        if (destination != null && destination.league != null) {
            premium *= destination.league.positionMarketFactor(p.position);
        }

        double base;
        if (talent < 60) {
            base = 25_000 + Math.max(0, talent - 50) * 4_000;
        } else if (talent < 70) {
            base = 70_000 + (talent - 60) * 18_000;
        } else if (talent < 80) {
            base = 280_000 + (talent - 70) * 50_000;
        } else if (talent < 90) {
            base = 850_000 + (talent - 80) * 180_000;
        } else {
            base = 2_800_000 + (talent - 90) * 400_000;
        }

        base *= premium;
        base *= NilMoney.youthPremium(p);

        int depth = projectedDepthRank(p, destination);
        base *= PlayerMarket.roleMultiplier(depth);

        TransferReason reason = p.transferReason != null ? p.transferReason : TransferReason.NONE;
        ProgramProfile profile = destination != null ? destination.programProfile : null;
        int brand = profile != null ? profile.brandAttract : 60;
        int pipeline = profile != null ? profile.pipeline : 60;
        int momentum = profile != null ? profile.momentum : 60;

        double multiplier = 1.0;
        switch (reason) {
            case PLAYING_TIME:
                if (depth <= 1) multiplier *= 0.45;
                else if (depth <= 2) multiplier *= 0.65;
                else multiplier *= 1.55;
                break;
            case MOVE_UP:
                multiplier *= brand >= 82 ? 0.78 : brand >= 70 ? 1.0 : 1.35;
                break;
            case COACHING_CHANGE:
                multiplier *= momentum >= 70 ? 0.88 : 1.25;
                break;
            case SCHEME_FIT:
                multiplier *= depth <= 1 ? 0.70 : 1.25;
                break;
            case WINNING:
                if (destination != null && destination.rankTeamPollScore > 0 && destination.rankTeamPollScore <= 25) {
                    multiplier *= 0.80;
                } else if (momentum >= 80) {
                    multiplier *= 0.88;
                } else {
                    multiplier *= 1.30;
                }
                break;
            case PROGRAM_FREEFALL:
                multiplier *= profile != null && profile.diffProgramPower >= 0 ? 0.82 : 1.40;
                break;
            case TITLE_CHASE:
                multiplier *= brand >= 88 || (destination != null && destination.totalNCs > 0)
                        ? 0.72 : 1.40;
                break;
            case INJURY_COMEBACK:
                multiplier *= 0.88;
                base *= 0.90;
                break;
            case NONE:
                // Homegrown / happy: mild discount; still apply depth
                if (depth <= 1) multiplier *= 0.70;
                else if (depth <= 2) multiplier *= 0.85;
                else multiplier *= 1.20;
                break;
            case BETTER_FIT:
            default:
                if (depth <= 1) multiplier *= 0.75;
                else if (depth >= 3) multiplier *= 1.35;
                break;
        }

        // All schools: playing-time path is the biggest lever
        if (reason != TransferReason.PLAYING_TIME) {
            if (depth <= 1) multiplier *= 0.85;
            else if (depth >= 3) multiplier *= 1.25;
        }

        multiplier *= 1.0 - Math.max(0, brand - 60) * 0.004;
        if (brand < 55) multiplier *= 1.0 + (55 - brand) * 0.008;
        multiplier *= 1.0 - Math.max(0, pipeline - 60) * 0.003;
        multiplier *= PlayerMarket.facilitiesMultiplier(profile);
        multiplier *= PlayerMarket.loyaltyMultiplier(p);
        multiplier *= PlayerMarket.draftLeverageMultiplier(p);

        if (destination != null) {
            double miles = GeoCatalog.get().miles(p, destination);
            multiplier *= GeoCatalog.distanceMultiplier(miles);
        }

        // 1-year rental premium for portal entries
        if (p.priorTeam != null && p.year >= 3) {
            multiplier *= 1.12;
        }

        multiplier *= deterministicMarketPreference(p, destination);

        int amount = (int) Math.round(base * multiplier / 1000.0) * 1000;
        if (amount < 25000) amount = 25000;
        if (amount > maxSingleDeal(destination)) amount = maxSingleDeal(destination);
        // Depth 3+ with tiny role share may fall to scholly floor — keep at least 25k if NIL status
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

    /** Preferred roster status by depth / talent (two-deep NIL band). */
    public static RosterStatus suggestedStatus(Player p, Team team) {
        if (p == null) return RosterStatus.PWO;
        if (PlayerMarket.qualifiesForNil(p, team)) return RosterStatus.SCHOLARSHIP_PLUS_NIL;
        int talent = PlayerMarket.marketTalent(p);
        if (talent >= 62) return RosterStatus.SCHOLARSHIP;
        return RosterStatus.PWO;
    }

    public static RosterStatus minimumAcceptable(Player p, int riskTier) {
        int talent = p != null ? PlayerMarket.marketTalent(p) : 60;
        if (riskTier >= 3 || talent >= 85) return RosterStatus.SCHOLARSHIP_PLUS_NIL;
        if (riskTier >= 2 || talent >= 72) return RosterStatus.SCHOLARSHIP;
        if (talent >= 62) return RosterStatus.SCHOLARSHIP;
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

    public static int projectDraftRound(Player p) {
        if (p == null || p.year < 3) return 0;
        if ("K".equals(p.position) || "P".equals(p.position)) return 0;

        int score = PlayerMarket.marketTalent(p);
        if (p.wonHeisman || p.careerHeismans > 0) score += 8;
        else if (p.wonAllAmerican || p.careerAllAmerican > 0) score += 5;
        else if (p.wonAllConference || p.careerAllConference > 0) score += 2;
        if (p.year == 3) score -= 2;
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

    /** Draft-eligible players who can still be retained via NIL (not locked R1–3). */
    public static boolean canRetainDraftEligible(Player p) {
        if (p == null || p.year >= 5) return false;
        int r = p.projectedDraftRound > 0 ? p.projectedDraftRound : projectDraftRound(p);
        if (isLockedDraftRound(r)) return false;
        return r >= 4 || (r == 0 && p.year >= 3 && PlayerMarket.marketTalent(p) >= 78);
    }

    /** @deprecated use canRetainDraftEligible — draft stay is no longer a separate product */
    @Deprecated
    public static boolean canPayToStay(Player p) {
        return canRetainDraftEligible(p);
    }

    /** @deprecated draft stay bonus removed; use annualNilFor with draft leverage */
    @Deprecated
    public static int draftStayBonus(Player p, Team t) {
        return annualNilFor(p, t, 1);
    }

    public static String draftRoundLabel(int round) {
        if (round >= 1 && round <= 7) return "Rd " + round;
        return "UDFA";
    }
}
