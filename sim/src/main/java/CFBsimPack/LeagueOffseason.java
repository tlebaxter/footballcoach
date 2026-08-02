package CFBsimPack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Retention, transfer portal, HS recruiting, contracts, and CPU AI.
 */
public class LeagueOffseason {
    public final League league;
    public ArrayList<Player> transferPortal = new ArrayList<>();
    public ArrayList<Player> hsClass = new ArrayList<>();

    public LeagueOffseason(League league) {
        this.league = league;
    }

    public void grantAllBudgets() {
        for (Team t : league.teamList) {
            t.updateProgramProfileForOffseason();
            t.grantYearlyBudget();
        }
    }

    public void resolveCoachingChanges() {
        for (Team t : league.teamList) {
            t.hadCoachingChange = false;
            if (t.userControlled) continue;
            if (t.programProfile.diffProgramPower <= -4
                    || (t.wins + t.losses > 0
                    && t.wins <= 3
                    && t.programProfile.expectation >= 70)) {
                if (Math.random() < 0.35) {
                    t.hadCoachingChange = true;
                }
            }
        }
    }

    public void scorePortalRiskForTeam(Team t) {
        for (Player p : t.getAllPlayers()) {
            if (t.playersLeaving.contains(p) || p.draftDeclared) {
                p.portalRiskTier = 0;
                continue;
            }
            if (p.retainedThisOffseason) {
                p.portalRiskTier = 0;
                continue;
            }
            if (p.year >= 5) {
                p.portalRiskTier = 0;
                continue;
            }
            // Fair multi-year deals stay put
            if (p.contractYearsRemaining > 0 && PlayerMarket.isFairlyPaid(p, t)) {
                p.transferReason = TransferReason.NONE;
                p.transferReasonText = "Under contract at " + t.name;
                p.portalRiskTier = 0;
                continue;
            }
            // Underpaid mid-deal: soft risk / renegotiate pressure
            if (p.contractYearsRemaining > 0 && PlayerMarket.isUnderpaid(p, t)) {
                p.transferReason = TransferReason.BETTER_FIT;
                p.transferReasonText = "Wants a raise at " + t.name;
                p.portalRiskTier = 1;
                continue;
            }
            TransferReason reason = evaluateReason(p, t);
            p.transferReason = reason;
            if (reason == TransferReason.NONE && !p.needsDealRenewal()
                    && !PlayerMarket.wantsNilUpgrade(p, t)) {
                p.transferReasonText = "Happy at " + t.name;
                p.portalRiskTier = 0;
            } else {
                if (reason == TransferReason.NONE) {
                    reason = PlayerMarket.wantsNilUpgrade(p, t)
                            ? TransferReason.BETTER_FIT : TransferReason.BETTER_FIT;
                    p.transferReason = reason;
                }
                p.transferReasonText = reason.label + " at " + t.name;
                p.portalRiskTier = Math.max(1, riskTierFor(p, t, reason));
                if (p.needsDealRenewal()) {
                    p.portalRiskTier = Math.max(p.portalRiskTier, 1);
                }
            }
        }
    }

    private int riskTierFor(Player p, Team t, TransferReason reason) {
        if (reason == null || reason == TransferReason.NONE) return 0;
        int depth = t.depthRank(p);
        int score = 0;
        switch (reason) {
            case PLAYING_TIME:
                score = depth >= 3 ? 3 : (depth == 2 ? 2 : 1);
                break;
            case MOVE_UP:
                score = p.ratOvr >= 78 ? 3 : 2;
                break;
            case COACHING_CHANGE:
            case PROGRAM_FREEFALL:
            case WINNING:
            case INJURY_COMEBACK:
            case SCHEME_FIT:
                score = 2;
                break;
            case TITLE_CHASE:
                score = p.year >= 3 ? 2 : 1;
                break;
            default:
                score = 1;
        }
        if (p.ratOvr >= 88) score = Math.min(3, score + 1);
        return score;
    }

    public TransferReason evaluateReason(Player p, Team t) {
        int depth = t.depthRank(p);
        boolean backupQb = "QB".equals(p.position) && depth >= 2 && p.ratOvr >= 75;
        boolean crowded = depth >= 3 && p.ratOvr >= 70;
        if (backupQb || crowded) return TransferReason.PLAYING_TIME;

        if (t.hadCoachingChange && Math.random() < 0.55) return TransferReason.COACHING_CHANGE;

        if (t.programProfile.diffProgramPower <= -5) return TransferReason.PROGRAM_FREEFALL;

        if (p.injury != null || (p.gamesPlayed <= 2 && p.year >= 2 && p.ratOvr >= 70)) {
            if (Math.random() < 0.4) return TransferReason.INJURY_COMEBACK;
        }

        boolean strongYear = p.gamesPlayed >= 8 && p.ratOvr >= 76;
        if (strongYear && t.programProfile.brandAttract < 78) return TransferReason.MOVE_UP;

        if (p.year >= 3 && p.ratOvr >= 80 && t.rankTeamPollScore > 25) return TransferReason.TITLE_CHASE;

        if (depth == 1 && p.gamesPlayed >= 6 && p.getHeismanScore() < p.ratOvr * 4) {
            return TransferReason.SCHEME_FIT;
        }

        if (t.wins + t.losses > 0
                && t.wins <= 4
                && t.programProfile.expectation >= 65) return TransferReason.WINNING;

        // Most players are happy — no issue
        if (Math.random() < 0.72) return TransferReason.NONE;
        return TransferReason.BETTER_FIT;
    }

    public ArrayList<Player> userDraftLocked(Team user) {
        ArrayList<Player> list = new ArrayList<>();
        for (Player p : new ArrayList<>(user.playersLeaving)) {
            if (p.year >= 5 || ProgramOffers.isLockedDraftRound(p.projectedDraftRound)) {
                list.add(p);
            }
        }
        sortByOvr(list);
        return list;
    }

    public ArrayList<Player> userDraftPayable(Team user) {
        ArrayList<Player> list = new ArrayList<>();
        for (Player p : new ArrayList<>(user.playersLeaving)) {
            if (p.year >= 5) continue;
            if (ProgramOffers.canPayToStay(p)) list.add(p);
        }
        sortByOvr(list);
        return list;
    }

    public ArrayList<Player> userAtRiskPlayers(Team user) {
        ArrayList<Player> list = new ArrayList<>();
        for (Player p : user.getAllPlayers()) {
            if (user.playersLeaving.contains(p)) continue;
            if (p.retainedThisOffseason) continue;
            if (p.portalRiskTier >= 1) list.add(p);
        }
        Collections.sort(list, new Comparator<Player>() {
            @Override
            public int compare(Player a, Player b) {
                if (b.portalRiskTier != a.portalRiskTier) return b.portalRiskTier - a.portalRiskTier;
                return b.ratOvr - a.ratOvr;
            }
        });
        return list;
    }

    public ArrayList<Player> userRenewals(Team user) {
        ArrayList<Player> list = new ArrayList<>();
        for (Player p : user.getAllPlayers()) {
            if (user.playersLeaving.contains(p)) continue;
            if (p.portalRiskTier >= 1) continue;
            if (p.needsDealRenewal()) list.add(p);
        }
        sortByOvr(list);
        return list;
    }

    private void sortByOvr(ArrayList<Player> list) {
        Collections.sort(list, new Comparator<Player>() {
            @Override
            public int compare(Player a, Player b) {
                return b.ratOvr - a.ratOvr;
            }
        });
    }

    /**
     * Pending retention suggestion. {@code bucket} is a UI label only:
     * DRAFT, UNDERPAID, EXPIRED, BREAKOUT, RISK — not separate products.
     */
    public static class RetainSuggestion {
        public final Player player;
        public final String bucket;
        public RosterStatus status;
        public int nil;
        public int years;
        /** @deprecated draft-stay product removed; always 0 */
        @Deprecated
        public int stayBonus;
        public boolean selected;

        public RetainSuggestion(Player player, String bucket) {
            this.player = player;
            this.bucket = bucket;
        }

        public int yearOneNilPurse(Team t) {
            return t.nilPurseCost(status, nil);
        }
    }

    public ArrayList<RetainSuggestion> suggestUserRetains(Team user) {
        ArrayList<RetainSuggestion> out = new ArrayList<>();
        int purse = user.recruitMoney;
        Set<Player> picked = new HashSet<>();
        scorePortalRiskForTeam(user);

        // Draft-eligible retainable via normal NIL
        for (Player p : userDraftPayable(user)) {
            RetainSuggestion s = buildNeedsDeal(user, p, "DRAFT");
            boolean want = user.depthRank(p) <= 2 && PlayerMarket.marketTalent(p) >= 78;
            s.selected = want && user.canAffordContract(s.status, s.nil, s.years)
                    && s.yearOneNilPurse(user) <= purse;
            if (s.selected) {
                purse -= s.yearOneNilPurse(user);
                picked.add(p);
            }
            out.add(s);
        }

        ArrayList<Player> needs = new ArrayList<>();
        for (Player p : user.getAllPlayers()) {
            if (picked.contains(p)) continue;
            if (user.playersLeaving.contains(p)) continue;
            if (p.retainedThisOffseason) continue;
            if (p.year >= 5) continue;
            boolean underpaid = PlayerMarket.isUnderpaid(p, user);
            boolean expired = p.needsDealRenewal();
            boolean breakout = PlayerMarket.wantsNilUpgrade(p, user);
            boolean risk = p.portalRiskTier >= 1;
            if (underpaid || expired || breakout || risk) {
                needs.add(p);
            }
        }
        Collections.sort(needs, new Comparator<Player>() {
            @Override
            public int compare(Player a, Player b) {
                int ta = PlayerMarket.marketTalent(a) * Math.max(1, a.portalRiskTier);
                int tb = PlayerMarket.marketTalent(b) * Math.max(1, b.portalRiskTier);
                return tb - ta;
            }
        });
        for (Player p : needs) {
            String label;
            if (PlayerMarket.isUnderpaid(p, user) && p.contractYearsRemaining > 0) {
                label = "UNDERPAID";
            } else if (p.needsDealRenewal()) {
                label = "EXPIRED";
            } else if (PlayerMarket.wantsNilUpgrade(p, user)) {
                label = "BREAKOUT";
            } else {
                label = "RISK";
            }
            RetainSuggestion s = buildNeedsDeal(user, p, label);
            boolean want = p.portalRiskTier >= 2
                    || user.depthRank(p) <= 2
                    || PlayerMarket.marketTalent(p) >= 78;
            s.selected = want && user.canAffordContract(s.status, s.nil, s.years)
                    && s.yearOneNilPurse(user) <= purse;
            if (s.selected) {
                purse -= s.yearOneNilPurse(user);
                picked.add(p);
            }
            out.add(s);
        }
        return out;
    }

    private RetainSuggestion buildNeedsDeal(Team user, Player p, String label) {
        RetainSuggestion s = new RetainSuggestion(p, label);
        s.status = ProgramOffers.suggestedStatus(p, user);
        if ("EXPIRED".equals(label) && p.rosterStatus != null
                && offerAtLeast(p.rosterStatus, s.status)) {
            s.status = p.rosterStatus == RosterStatus.PWO
                    ? RosterStatus.SCHOLARSHIP : p.rosterStatus;
            if (PlayerMarket.qualifiesForNil(p, user)) {
                s.status = RosterStatus.SCHOLARSHIP_PLUS_NIL;
            }
        }
        s.years = ProgramOffers.suggestedContractYears(p);
        s.nil = s.status == RosterStatus.SCHOLARSHIP_PLUS_NIL
                ? ProgramOffers.annualNilFor(p, user, s.years) : 0;
        s.stayBonus = 0;
        return s;
    }

    private static boolean offerAtLeast(RosterStatus have, RosterStatus need) {
        int h = have == RosterStatus.SCHOLARSHIP_PLUS_NIL ? 2
                : have == RosterStatus.SCHOLARSHIP ? 1 : 0;
        int n = need == RosterStatus.SCHOLARSHIP_PLUS_NIL ? 2
                : need == RosterStatus.SCHOLARSHIP ? 1 : 0;
        return h >= n;
    }

    public boolean applyUserRetain(Team user, RetainSuggestion s) {
        if (s == null || s.player == null) return false;
        Player p = s.player;
        int risk = Math.max(1, p.portalRiskTier);
        if ("UNDERPAID".equals(s.bucket) || "EXPIRED".equals(s.bucket)) {
            risk = Math.max(risk, 1);
        }
        if (!ProgramOffers.acceptsOffer(p, user, s.status, s.nil, risk)) {
            if (!"EXPIRED".equals(s.bucket) && !"UNDERPAID".equals(s.bucket)) return false;
        }
        // Renegotiate mid-deal: only pay the raise delta when years remain
        if (p.contractYearsRemaining > 0
                && p.rosterStatus == RosterStatus.SCHOLARSHIP_PLUS_NIL
                && s.status == RosterStatus.SCHOLARSHIP_PLUS_NIL
                && s.nil > p.nilDealAmount) {
            int delta = s.nil - p.nilDealAmount;
            if (delta > user.recruitMoney) return false;
            if (!user.canAffordContract(delta, s.nil, s.years)) return false;
            user.recruitMoney -= delta;
            p.applyOffer(s.status, s.nil, s.years);
            p.portalRiskTier = 0;
            p.retainedThisOffseason = true;
            return true;
        }
        if (!user.spendRetentionOffer(s.status, s.nil, s.years, p)) return false;
        p.applyOffer(s.status, s.nil, s.years);
        p.portalRiskTier = 0;
        p.retainedThisOffseason = true;
        if (user.playersLeaving.contains(p)) {
            user.playersLeaving.remove(p);
            p.draftDeclared = false;
            p.projectedDraftRound = 0;
        }
        return true;
    }

    public void finalizeDraftDeclares(Team user) {
        for (Player p : new ArrayList<>(user.playersLeaving)) {
            if (p.year >= 5 || ProgramOffers.isLockedDraftRound(p.projectedDraftRound)
                    || !p.retainedThisOffseason) {
                user.clearCommitmentsForDraft(p);
                p.draftDeclared = true;
            }
        }
    }

    /** AI retention for non-user teams. */
    public void aiRetainAll() {
        for (Team t : league.teamList) {
            if (t.userControlled) continue;
            scorePortalRiskForTeam(t);
            PositionBudgetBalancer bal = new PositionBudgetBalancer(t, t.recruitMoney);

            // Draft-eligible: NIL retain or clear
            for (Player p : new ArrayList<>(t.playersLeaving)) {
                if (p.year >= 5 || ProgramOffers.isLockedDraftRound(p.projectedDraftRound)) {
                    t.clearCommitmentsForDraft(p);
                    continue;
                }
                if (!ProgramOffers.canRetainDraftEligible(p)) {
                    t.clearCommitmentsForDraft(p);
                    continue;
                }
                boolean critical = t.depthRank(p) <= 2 && bal.needWeight(p.position) >= 1.0;
                if (!critical) {
                    t.clearCommitmentsForDraft(p);
                    continue;
                }
                RosterStatus st = ProgramOffers.suggestedStatus(p, t);
                int years = 1;
                int nil = st == RosterStatus.SCHOLARSHIP_PLUS_NIL
                        ? ProgramOffers.annualNilFor(p, t, years) : 0;
                int cost = t.nilPurseCost(st, nil);
                if (bal.canSpend(p.position, cost, true)
                        && t.spendRetentionOffer(st, nil, years, p)) {
                    p.applyOffer(st, nil, years);
                    p.portalRiskTier = 0;
                    t.playersLeaving.remove(p);
                    p.draftDeclared = false;
                    p.projectedDraftRound = 0;
                    bal.recordSpend(p.position, cost);
                } else {
                    t.clearCommitmentsForDraft(p);
                }
            }

            ArrayList<Player> targets = new ArrayList<>();
            for (Player p : t.getAllPlayers()) {
                if (t.playersLeaving.contains(p)) continue;
                if (p.retainedThisOffseason) continue;
                if (PlayerMarket.isFairlyPaid(p, t) && p.contractYearsRemaining > 0) continue;
                if (p.portalRiskTier >= 1 || p.needsDealRenewal()
                        || PlayerMarket.isUnderpaid(p, t)
                        || PlayerMarket.wantsNilUpgrade(p, t)) {
                    targets.add(p);
                }
            }
            Collections.sort(targets, new Comparator<Player>() {
                @Override
                public int compare(Player a, Player b) {
                    int sa = PlayerMarket.marketTalent(a) * Math.max(1, a.portalRiskTier);
                    int sb = PlayerMarket.marketTalent(b) * Math.max(1, b.portalRiskTier);
                    return sb - sa;
                }
            });

            for (Player p : targets) {
                // AI skips overstocked rooms unless starter-critical
                int have = t.getPositionList(p.position) != null
                        ? t.getPositionList(p.position).size() : 0;
                int sug = NilMoney.sugFor(p.position);
                boolean critical = t.depthRank(p) <= 1;
                if (!critical && have > sug + 1) continue;

                RosterStatus min = ProgramOffers.suggestedStatus(p, t);
                if (min == RosterStatus.PWO && p.needsDealRenewal()) min = RosterStatus.SCHOLARSHIP;
                int years = ProgramOffers.suggestedContractYears(p);
                int nil = min == RosterStatus.SCHOLARSHIP_PLUS_NIL
                        ? ProgramOffers.annualNilFor(p, t, years) : 0;
                int cost = t.nilPurseCost(min, nil);
                if (!bal.canSpend(p.position, cost, critical)) {
                    years = 1;
                    nil = min == RosterStatus.SCHOLARSHIP_PLUS_NIL
                            ? ProgramOffers.annualNilFor(p, t, 1) : 0;
                    cost = t.nilPurseCost(min, nil);
                    if (!bal.canSpend(p.position, cost, critical)) continue;
                }
                if (!t.spendRetentionOffer(min, nil, years, p)) continue;
                p.applyOffer(min, nil, years);
                p.portalRiskTier = 0;
                bal.recordSpend(p.position, cost);
            }
            bal.rebalanceToNeeds();
            aiBuyoutPressure(t, bal);
            t.fillRosterToCap();
        }
    }

    private void aiBuyoutPressure(Team t, PositionBudgetBalancer bal) {
        while (t.getScholarshipCount() > NilMoney.SCHOLARSHIP_CAP
                || t.availableForOffset(1) < 0
                || t.getRosterCount() > NilMoney.ROSTER_CAP) {
            Player worst = null;
            int worstScore = Integer.MAX_VALUE;
            for (Player p : t.getAllPlayers()) {
                if (t.playersLeaving.contains(p)) continue;
                int have = t.getPositionList(p.position).size();
                int sug = NilMoney.sugFor(p.position);
                if (have <= sug) continue;
                int score = p.ratOvr - (have - sug) * 5;
                if (score < worstScore) {
                    worstScore = score;
                    worst = p;
                }
            }
            if (worst == null) break;
            if (!t.cutOrBuyout(worst, true)) break;
        }
    }

    /**
     * Build portal from unretained at-risk players. Does not re-score retained players.
     */
    public void buildTransferPortal() {
        transferPortal.clear();
        for (Team t : league.teamList) {
            // Score only players not already retained
            for (Player p : t.getAllPlayers()) {
                if (p.retainedThisOffseason) {
                    p.portalRiskTier = 0;
                    continue;
                }
                if (t.playersLeaving.contains(p) || p.year >= 5) {
                    p.portalRiskTier = 0;
                    continue;
                }
                if (p.portalRiskTier == 0 && p.transferReason == TransferReason.NONE) {
                    continue;
                }
                if (p.transferReason == null || (p.portalRiskTier == 0 && !p.needsDealRenewal())) {
                    TransferReason reason = evaluateReason(p, t);
                    p.transferReason = reason;
                    if (reason == TransferReason.NONE) {
                        p.portalRiskTier = 0;
                        p.transferReasonText = "Happy at " + t.name;
                    } else if (!p.retainedThisOffseason) {
                        p.transferReasonText = reason.label + " at " + t.name;
                        p.portalRiskTier = riskTierFor(p, t, reason);
                    }
                }
            }

            ArrayList<Player> leaving = new ArrayList<>();
            for (Player p : t.getAllPlayers()) {
                if (t.playersLeaving.contains(p)) continue;
                if (p.year >= 5) continue;
                if (p.retainedThisOffseason) continue;
                if (p.contractYearsRemaining > 0 && p.portalRiskTier == 0) continue;
                boolean enters = false;
                if (p.portalRiskTier >= 3 && Math.random() < 0.85) enters = true;
                else if (p.portalRiskTier == 2 && Math.random() < 0.55) enters = true;
                else if (p.portalRiskTier == 1 && Math.random() < 0.12) enters = true;
                if (enters) leaving.add(p);
            }
            for (Player p : leaving) {
                p.priorTeam = t;
                t.removePlayerFromRoster(p);
                t.playersTransferring.add(p);
                transferPortal.add(p);
            }
        }
        Collections.sort(transferPortal, new Comparator<Player>() {
            @Override
            public int compare(Player a, Player b) {
                int pa = PlayerMarket.productionScore(a) * 10 + PlayerMarket.marketTalent(a);
                int pb = PlayerMarket.productionScore(b) * 10 + PlayerMarket.marketTalent(b);
                return pb - pa;
            }
        });
        league.refreshPositionMarketPremiums(transferPortal, hsClass);
    }

    public void removeFromPortal(Player p) {
        transferPortal.remove(p);
    }

    public boolean userSignTransfer(Team user, Player p, RosterStatus status, int nilAmount) {
        return userSignTransfer(user, p, status, nilAmount, 1);
    }

    public boolean userSignTransfer(Team user, Player p, RosterStatus status, int nilAmount, int years) {
        if (!transferPortal.contains(p)) return false;
        if (!user.canAddToRoster()) return false;
        if (status.usesScholarship() && !user.canAwardScholarship()
                && (p.rosterStatus == null || !p.rosterStatus.usesScholarship())) {
            return false;
        }
        int y = Math.min(years, ProgramOffers.maxContractYears(p));
        int nilCost = user.nilPurseCost(status, nilAmount);
        int buyout = 0;
        Team prior = p.priorTeam;
        if (prior != null && p.contractYearsRemaining > 0
                && p.rosterStatus == RosterStatus.SCHOLARSHIP_PLUS_NIL) {
            buyout = NilMoney.buyoutCost(p, prior.programProfile);
        }
        int total = nilCost + buyout;
        if (total > user.recruitMoney) return false;
        if (!user.canAffordContract(nilCost, status == RosterStatus.SCHOLARSHIP_PLUS_NIL
                ? Math.max(0, nilAmount) : 0, y)) return false;
        if (!ProgramOffers.acceptsOffer(
                p, user, status, nilAmount, Math.max(1, p.portalRiskTier))) return false;
        user.recruitMoney -= total;
        if (buyout > 0 && prior != null) {
            prior.recruitMoney += buyout;
        }
        // Clear prior guaranteed years — poacher paid the buyout
        p.contractYearsRemaining = 0;
        p.applyOffer(status, nilAmount, y);
        p.team = user;
        p.portalRiskTier = 0;
        p.yearsAtProgram = 0;
        user.addPlayerToRoster(p);
        transferPortal.remove(p);
        return true;
    }

    public boolean userSignHs(Team user, Player p, RosterStatus status, int nilAmount, int years) {
        if (!hsClass.contains(p)) return false;
        if (!user.canAddToRoster()) return false;
        if (status.usesScholarship() && !user.canAwardScholarship()) return false;
        int y = Math.min(years, ProgramOffers.maxContractYears(p));
        int cost = user.nilPurseCost(status, nilAmount);
        if (!user.canAffordContract(status, nilAmount, y)) return false;
        if (!ProgramOffers.acceptsOffer(p, user, status, nilAmount, 0)) return false;
        user.recruitMoney -= cost;
        p.applyOffer(status, nilAmount, y);
        user.addPlayerToRoster(p);
        hsClass.remove(p);
        return true;
    }

    public void aiClaimRemainingPortal() {
        Collections.sort(transferPortal, new Comparator<Player>() {
            @Override
            public int compare(Player a, Player b) {
                return b.ratOvr - a.ratOvr;
            }
        });
        Map<Team, PositionBudgetBalancer> bals = new HashMap<>();
        for (Team t : league.teamList) {
            if (!t.userControlled) bals.put(t, new PositionBudgetBalancer(t, t.recruitMoney));
        }

        ArrayList<Player> remaining = new ArrayList<>(transferPortal);
        for (Player p : remaining) {
            Team best = null;
            double bestScore = -1e9;
            RosterStatus bestOffer = RosterStatus.SCHOLARSHIP;
            int bestNil = 0;
            int bestYears = 1;
            for (Team t : league.teamList) {
                if (t.userControlled) continue;
                if (!t.canAddToRoster()) continue;
                PositionBudgetBalancer bal = bals.get(t);
                int depth = ProgramOffers.projectedDepthRank(p, t);
                int have = t.getPositionList(p.position) != null
                        ? t.getPositionList(p.position).size() : 0;
                int sug = NilMoney.sugFor(p.position);
                // AI: skip overstocked rooms unless they'd start
                if (depth > 2 && have >= sug) continue;
                double miles = GeoCatalog.get().miles(p, t);
                double score = t.programProfile.brandAttract * 0.35
                        + t.programProfile.pipeline * 0.15
                        - depth * 8 + fitBonus(p, t)
                        + bal.needWeight(p.position) * 12
                        - miles * 0.02;
                RosterStatus offer = ProgramOffers.suggestedStatus(p, t);
                if (offer.usesScholarship() && !t.canAwardScholarship()) {
                    if (offer == RosterStatus.SCHOLARSHIP_PLUS_NIL) continue;
                    offer = RosterStatus.PWO;
                    if (!ProgramOffers.acceptsOffer(p, t, offer, 0, p.portalRiskTier)) continue;
                }
                int years = Math.min(ProgramOffers.suggestedContractYears(p), ProgramOffers.maxContractYears(p));
                int nil = offer == RosterStatus.SCHOLARSHIP_PLUS_NIL
                        ? ProgramOffers.annualNilFor(p, t, years) : 0;
                int buyout = 0;
                if (p.priorTeam != null && p.contractYearsRemaining > 0
                        && p.rosterStatus == RosterStatus.SCHOLARSHIP_PLUS_NIL) {
                    buyout = NilMoney.buyoutCost(p, p.priorTeam.programProfile);
                }
                int cost = t.nilPurseCost(offer, nil) + buyout;
                boolean critical = depth <= 1 && bal.needWeight(p.position) >= 1.5;
                if (!t.canAffordContract(offer, nil, years) || !bal.canSpend(p.position, cost, critical)
                        || cost > t.recruitMoney) {
                    years = 1;
                    nil = offer == RosterStatus.SCHOLARSHIP_PLUS_NIL
                            ? ProgramOffers.annualNilFor(p, t, 1) : 0;
                    cost = t.nilPurseCost(offer, nil);
                    if (!t.canAffordContract(offer, nil, years) || !bal.canSpend(p.position, cost, critical)) {
                        if (ProgramOffers.acceptsOffer(
                                p, t, RosterStatus.SCHOLARSHIP, 0, p.portalRiskTier)
                                && t.canAwardScholarship()) {
                            offer = RosterStatus.SCHOLARSHIP;
                            nil = 0;
                            years = 1;
                            cost = t.nilPurseCost(offer, nil);
                            if (!t.canAffordContract(offer, nil, years) || !bal.canSpend(p.position, cost, critical)) {
                                continue;
                            }
                        } else {
                            continue;
                        }
                    }
                }
                if (score > bestScore) {
                    bestScore = score;
                    best = t;
                    bestOffer = offer;
                    bestNil = nil;
                    bestYears = years;
                }
            }
            if (best != null) {
                int buyout = 0;
                if (p.priorTeam != null && p.contractYearsRemaining > 0
                        && p.rosterStatus == RosterStatus.SCHOLARSHIP_PLUS_NIL) {
                    buyout = NilMoney.buyoutCost(p, p.priorTeam.programProfile);
                }
                int nilCost = best.nilPurseCost(bestOffer, bestNil);
                int cost = nilCost + buyout;
                best.recruitMoney -= cost;
                if (buyout > 0 && p.priorTeam != null) {
                    p.priorTeam.recruitMoney += buyout;
                }
                p.contractYearsRemaining = 0;
                p.applyOffer(bestOffer, bestNil, bestYears);
                p.portalRiskTier = 0;
                p.yearsAtProgram = 0;
                best.addPlayerToRoster(p);
                transferPortal.remove(p);
                bals.get(best).recordSpend(p.position, cost);
                bals.get(best).rebalanceToNeeds();
            }
        }
        ArrayList<Player> left = new ArrayList<>(transferPortal);
        for (Player p : left) {
            if (p.priorTeam != null && p.priorTeam.canAddToRoster()) {
                p.applyOffer(RosterStatus.PWO, 0, 1);
                p.priorTeam.addPlayerToRoster(p);
            }
            transferPortal.remove(p);
        }
        for (Team t : league.teamList) {
            if (!t.userControlled) t.fillRosterToCap();
        }
    }

    private double fitBonus(Player p, Team t) {
        TransferReason r = p.transferReason != null ? p.transferReason : TransferReason.BETTER_FIT;
        int prior = p.priorTeam != null
                ? p.priorTeam.programProfile.brandAttract
                : 70;
        switch (r) {
            case PLAYING_TIME:
                return (3 - Math.min(3, ProgramOffers.projectedDepthRank(p, t))) * 15;
            case MOVE_UP:
                return t.programProfile.brandAttract - prior;
            case TITLE_CHASE:
                return t.programProfile.brandAttract >= 88 ? 20 : -10;
            case WINNING:
                return t.wins * 2;
            case PROGRAM_FREEFALL:
                return t.programProfile.diffProgramPower >= 0 ? 10 : -10;
            default:
                return 0;
        }
    }

    public void generateHsClass() {
        hsClass.clear();
        int total = 3500;
        int sumWeights = 0;
        for (String pos : NilMoney.POSITIONS) {
            sumWeights += NilMoney.initFor(pos);
        }
        int[] counts = new int[NilMoney.POSITIONS.length];
        for (int i = 0; i < NilMoney.POSITIONS.length; i++) {
            counts[i] = total * NilMoney.initFor(NilMoney.POSITIONS[i]) / sumWeights;
        }
        Team seed = league.teamList.get(0);
        for (int i = 0; i < NilMoney.POSITIONS.length; i++) {
            for (int j = 0; j < counts[i]; j++) {
                hsClass.add(newFreshman(NilMoney.POSITIONS[i], seed));
            }
        }
        Collections.sort(hsClass, new Comparator<Player>() {
            @Override
            public int compare(Player a, Player b) {
                return b.ratOvr - a.ratOvr;
            }
        });
    }

    private Player newFreshman(String pos, Team seedTeam) {
        int stars = 1 + (int) (Math.random() * 5);
        if (Math.random() < 0.15) stars = Math.min(5, stars + 1);
        String name = league.getRandName();
        PositionGroup g = PositionGroup.fromToken(pos);
        if (g == null) return null;
        Player p = PlayerFactory.fromStars(g, name, 1, stars, seedTeam, new Random());
        p.team = null;
        p.cost = NilMoney.marketValue(p);
        return p;
    }

    public ArrayList<Player> hsByPosition(String pos) {
        ArrayList<Player> list = new ArrayList<>();
        for (Player p : hsClass) {
            if (pos == null || "ALL".equals(pos) || pos.equals(p.position)) list.add(p);
        }
        return list;
    }

    public void aiSignHsClass() {
        ArrayList<Team> teams = new ArrayList<>(league.teamList);
        Collections.sort(teams, new Comparator<Team>() {
            @Override
            public int compare(Team a, Team b) {
                return b.programProfile.talentGravity - a.programProfile.talentGravity;
            }
        });
        for (Team t : teams) {
            if (t.userControlled) continue;
            PositionBudgetBalancer bal = new PositionBudgetBalancer(t, t.recruitMoney);
            while (t.getRosterCount() < NilMoney.ROSTER_TARGET && t.canAddToRoster() && !hsClass.isEmpty()) {
                Player pick = bestHsForTeam(t, bal);
                if (pick == null) break;
                RosterStatus offer = pick.ratOvr >= 82 ? RosterStatus.SCHOLARSHIP_PLUS_NIL
                        : (pick.ratOvr >= 60 ? RosterStatus.SCHOLARSHIP : RosterStatus.PWO);
                if (offer.usesScholarship() && !t.canAwardScholarship()) offer = RosterStatus.PWO;
                int years = offer == RosterStatus.PWO ? 1 : ProgramOffers.suggestedContractYears(pick);
                int nil = offer == RosterStatus.SCHOLARSHIP_PLUS_NIL
                        ? ProgramOffers.annualNilFor(pick, t, years) : 0;
                int cost = t.nilPurseCost(offer, nil);
                boolean critical = bal.needWeight(pick.position) >= 2.0;
                if (!t.canAffordContract(offer, nil, years) || !bal.canSpend(pick.position, cost, critical)) {
                    years = 1;
                    nil = offer == RosterStatus.SCHOLARSHIP_PLUS_NIL
                            ? ProgramOffers.annualNilFor(pick, t, 1) : 0;
                    cost = t.nilPurseCost(offer, nil);
                    if (!t.canAffordContract(offer, nil, years) || !bal.canSpend(pick.position, cost, critical)) {
                        if (t.canAwardScholarship() && t.nilPurseCost(RosterStatus.SCHOLARSHIP, 0) <= t.recruitMoney) {
                            offer = RosterStatus.SCHOLARSHIP;
                            nil = 0;
                            years = 1;
                            cost = t.nilPurseCost(offer, nil);
                        } else {
                            offer = RosterStatus.PWO;
                            nil = 0;
                            years = 1;
                            cost = 0;
                        }
                    }
                }
                if (cost > t.recruitMoney && cost > 0) break;
                t.recruitMoney -= cost;
                pick.applyOffer(offer, nil, years);
                t.addPlayerToRoster(pick);
                hsClass.remove(pick);
                bal.recordSpend(pick.position, cost);
                bal.rebalanceToNeeds();
            }
            t.recruitWalkOns();
        }
        // User team skips AI HS signing; still pad minimum depth with walk-ons
        if (league.userTeam != null && league.userTeam.userControlled) {
            league.userTeam.recruitWalkOns();
        }
    }

    /**
     * Full AI offseason for sims/tests: retention → portal → season advance → HS class.
     * Call after {@link League#getPlayersLeaving()}. Treats the user team as CPU for
     * retention/HS so multi-year automated seasons stay playable.
     */
    public void advanceYearAutomated() {
        grantAllBudgets();
        resolveCoachingChanges();
        Team user = league.userTeam;
        boolean wasUser = user != null && user.userControlled;
        if (wasUser) {
            user.userControlled = false;
        }
        try {
            aiRetainAll();
            buildTransferPortal();
            aiClaimRemainingPortal();
            league.updateTeamHistories();
            league.updateLeagueHistory();
            // Rivalry dynamics need intact schedules (resetStats clears the user slate).
            RivalryDynamics.applyEndOfSeason(league);
            if (user != null) {
                user.resetStats();
            }
            league.advanceSeason();
            generateHsClass();
            aiSignHsClass();
            league.updateTeamTalentRatings();
        } finally {
            if (wasUser) {
                user.userControlled = true;
            }
        }
    }

    private Player bestHsForTeam(Team t, PositionBudgetBalancer bal) {
        String bestPos = null;
        double bestNeed = -1;
        for (String pos : NilMoney.POSITIONS) {
            double n = bal.needWeight(pos);
            if (n > bestNeed) {
                bestNeed = n;
                bestPos = pos;
            }
        }
        if (bestPos != null) {
            for (Player p : hsClass) {
                if (bestPos.equals(p.position)) return p;
            }
        }
        return hsClass.isEmpty() ? null : hsClass.get(0);
    }
}
