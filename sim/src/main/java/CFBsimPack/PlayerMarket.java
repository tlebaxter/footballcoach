package CFBsimPack;

/**
 * Production score, role bands, league position premiums, and facilities helpers.
 */
public final class PlayerMarket {
    private PlayerMarket() {}

    /** Composite production used for portal/renew pricing (not just OVR). */
    public static int productionScore(Player p) {
        if (p == null) return 0;
        int snaps = Math.max(0, p.seasonSnaps);
        int games = Math.max(0, p.gamesPlayed);
        int base = snaps / 3 + games * 8 + p.getHeismanScore() / 4;
        if (p.wonHeisman || p.careerHeismans > 0) base += 80;
        else if (p.wonAllAmerican || p.careerAllAmerican > 0) base += 45;
        else if (p.wonAllConference || p.careerAllConference > 0) base += 20;
        // Young unused talent still has value via potential
        if (games < 3 && p.year <= 2) {
            base += Math.max(0, p.ratPot - 60) * 2;
        }
        return Math.max(0, base);
    }

    /**
     * Effective talent for market: blends OVR with production so a productive 82
     * outranks an unused 86.
     */
    public static int marketTalent(Player p) {
        if (p == null) return 50;
        int ovr = p.ratOvr;
        int prod = productionScore(p);
        // Map production into roughly 0–25 OVR-equivalent bump
        int prodBump = Math.min(25, prod / 12);
        int unusedPenalty = 0;
        if (p.gamesPlayed <= 2 && p.year >= 3 && p.ratOvr >= 75) {
            unusedPenalty = 8;
        }
        return Math.max(40, Math.min(99, ovr + prodBump - unusedPenalty));
    }

    /** Depth-based pay share of starter market (steep). */
    public static double roleMultiplier(int depthRank) {
        if (depthRank <= 1) return 1.0;
        if (depthRank == 2) return 0.35;
        if (depthRank == 3) return 0.12;
        return 0.05;
    }

    public static boolean qualifiesForNil(Player p, Team team) {
        if (p == null) return false;
        int depth = team != null ? team.depthRank(p) : ProgramOffers.projectedDepthRank(p, team);
        if (depth <= 2) return true;
        return marketTalent(p) >= 88 || p.ratPot >= 92;
    }

    public static double facilitiesScore(ProgramProfile profile) {
        if (profile == null) return 50;
        return profile.donors * 0.50 + profile.tradition * 0.30 + profile.footprint * 0.20;
    }

    public static double facilitiesMultiplier(ProgramProfile profile) {
        double f = facilitiesScore(profile);
        // Strong facilities discount ask slightly
        return 1.0 - Math.max(0, f - 60) * 0.003 + Math.max(0, 50 - f) * 0.004;
    }

    public static double loyaltyMultiplier(Player p) {
        if (p == null) return 1.0;
        int years = Math.max(0, p.yearsAtProgram);
        if (years >= 3) return 0.82;
        if (years == 2) return 0.90;
        if (p.priorTeam != null || (p.transferReason != null && p.transferReason != TransferReason.NONE)) {
            return 1.18; // portal mercenary
        }
        return 1.0;
    }

    public static double draftLeverageMultiplier(Player p) {
        if (p == null) return 1.0;
        int round = p.projectedDraftRound > 0 ? p.projectedDraftRound : ProgramOffers.projectDraftRound(p);
        if (round >= 1 && round <= 3) return 2.10;
        if (round == 4) return 1.45;
        if (round == 5) return 1.25;
        if (round == 6) return 1.10;
        if (round == 7) return 1.00;
        if (p.year >= 3 && p.ratOvr >= 78) return 1.05;
        return 1.0;
    }

    /** Underpaid if current NIL is below 90% of market ask (for Sch+NIL). */
    public static boolean isUnderpaid(Player p, Team team) {
        if (p == null || team == null) return false;
        if (p.rosterStatus != RosterStatus.SCHOLARSHIP_PLUS_NIL) {
            // Scholly breakout wants NIL
            return wantsNilUpgrade(p, team);
        }
        int market = ProgramOffers.nilAmountFor(p, team);
        return p.nilDealAmount < (int) (market * 0.90);
    }

    public static boolean wantsNilUpgrade(Player p, Team team) {
        if (p == null) return false;
        if (p.rosterStatus == RosterStatus.SCHOLARSHIP_PLUS_NIL) return false;
        if (p.rosterStatus == RosterStatus.PWO) return marketTalent(p) >= 70;
        // Scholarship: massive production/OVR jump
        return qualifiesForNil(p, team) && (marketTalent(p) >= 82 || productionScore(p) >= 120);
    }

    public static boolean isFairlyPaid(Player p, Team team) {
        if (p == null || team == null) return true;
        if (p.contractYearsRemaining <= 0) return false;
        if (p.rosterStatus == RosterStatus.PWO) return true;
        if (p.rosterStatus == RosterStatus.SCHOLARSHIP) {
            return !wantsNilUpgrade(p, team);
        }
        int market = ProgramOffers.nilAmountFor(p, team);
        return p.nilDealAmount >= (int) (market * 0.90);
    }
}
