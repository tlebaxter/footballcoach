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
            TransferReason reason = evaluateReason(p, t);
            p.transferReason = reason;
            if (reason == TransferReason.NONE) {
                p.transferReasonText = "Happy at " + t.name;
                p.portalRiskTier = 0;
            } else {
                p.transferReasonText = reason.label + " at " + t.name;
                p.portalRiskTier = riskTierFor(p, t, reason);
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

    /** Pending retention suggestion for user approve UI. */
    public static class RetainSuggestion {
        public final Player player;
        public final String bucket; // DRAFT_STAY, RISK, RENEWAL
        public RosterStatus status;
        public int nil;
        public int years;
        public int stayBonus;
        public boolean selected;

        public RetainSuggestion(Player player, String bucket) {
            this.player = player;
            this.bucket = bucket;
        }

        public int yearOneCost(Team t) {
            if ("DRAFT_STAY".equals(bucket)) return stayBonus;
            return t.offerTotalCost(status, nil);
        }
    }

    public ArrayList<RetainSuggestion> suggestUserRetains(Team user) {
        ArrayList<RetainSuggestion> out = new ArrayList<>();
        int purse = user.recruitMoney;
        Set<Player> picked = new HashSet<>();

        ArrayList<Player> draftPay = userDraftPayable(user);
        for (Player p : draftPay) {
            RetainSuggestion s = new RetainSuggestion(p, "DRAFT_STAY");
            s.stayBonus = ProgramOffers.draftStayBonus(p, user);
            s.years = 1;
            s.status = p.rosterStatus != null ? p.rosterStatus : RosterStatus.SCHOLARSHIP;
            s.nil = p.nilDealAmount;
            boolean need = user.depthRank(p) <= 2 && p.ratOvr >= 78;
            s.selected = need && s.stayBonus <= purse;
            if (s.selected) {
                purse -= s.stayBonus;
                picked.add(p);
            }
            out.add(s);
        }

        ArrayList<Player> risk = userAtRiskPlayers(user);
        Collections.sort(risk, new Comparator<Player>() {
            @Override
            public int compare(Player a, Player b) {
                return (b.ratOvr * b.portalRiskTier) - (a.ratOvr * a.portalRiskTier);
            }
        });
        for (Player p : risk) {
            if (picked.contains(p)) continue;
            RetainSuggestion s = new RetainSuggestion(p, "RISK");
            s.status = ProgramOffers.minimumAcceptable(p, p.portalRiskTier);
            s.years = ProgramOffers.suggestedContractYears(p);
            s.nil = s.status == RosterStatus.SCHOLARSHIP_PLUS_NIL
                    ? ProgramOffers.annualNilFor(p, user, s.years) : 0;
            int cost = user.offerTotalCost(s.status, s.nil);
            boolean want = p.portalRiskTier >= 2 || (p.portalRiskTier == 1 && p.ratOvr >= 82);
            s.selected = want && user.canAffordContract(s.status, s.nil, s.years) && cost <= purse;
            if (s.selected) {
                purse -= cost;
                picked.add(p);
            }
            out.add(s);
        }

        for (Player p : userRenewals(user)) {
            if (picked.contains(p)) continue;
            RetainSuggestion s = new RetainSuggestion(p, "RENEWAL");
            s.status = p.rosterStatus != null ? p.rosterStatus : RosterStatus.SCHOLARSHIP;
            if (s.status == RosterStatus.PWO) s.status = RosterStatus.SCHOLARSHIP;
            s.years = ProgramOffers.suggestedContractYears(p);
            s.nil = s.status == RosterStatus.SCHOLARSHIP_PLUS_NIL
                    ? ProgramOffers.annualNilFor(p, user, s.years) : 0;
            int cost = user.offerTotalCost(s.status, s.nil);
            boolean want = p.ratOvr >= 70 || user.depthRank(p) <= 2;
            s.selected = want && user.canAffordContract(s.status, s.nil, s.years) && cost <= purse;
            if (s.selected) {
                purse -= cost;
                picked.add(p);
            }
            out.add(s);
        }
        return out;
    }

    public boolean applyUserRetain(Team user, RetainSuggestion s) {
        if (s == null || s.player == null) return false;
        Player p = s.player;
        if ("DRAFT_STAY".equals(s.bucket)) {
            return user.payDraftStay(p, s.stayBonus);
        }
        if (!ProgramOffers.acceptsOffer(
                p, user, s.status, s.nil, Math.max(1, p.portalRiskTier))) {
            if (!"RENEWAL".equals(s.bucket)) return false;
        }
        if (!user.spendRetentionOffer(s.status, s.nil, s.years, p)) return false;
        p.applyOffer(s.status, s.nil, s.years);
        p.portalRiskTier = 0;
        p.retainedThisOffseason = true;
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

            // Draft stay-pay for R4–UDFA when needed
            for (Player p : new ArrayList<>(t.playersLeaving)) {
                if (p.year >= 5 || ProgramOffers.isLockedDraftRound(p.projectedDraftRound)) {
                    t.clearCommitmentsForDraft(p);
                    continue;
                }
                if (!ProgramOffers.canPayToStay(p)) {
                    t.clearCommitmentsForDraft(p);
                    continue;
                }
                int bonus = ProgramOffers.draftStayBonus(p, t);
                boolean critical = t.depthRank(p) <= 2;
                if (critical && bal.canSpend(p.position, bonus, true) && t.payDraftStay(p, bonus)) {
                    bal.recordSpend(p.position, bonus);
                } else {
                    t.clearCommitmentsForDraft(p);
                }
            }

            ArrayList<Player> targets = new ArrayList<>();
            for (Player p : t.getAllPlayers()) {
                if (t.playersLeaving.contains(p)) continue;
                if (p.portalRiskTier >= 2 || p.needsDealRenewal()) targets.add(p);
            }
            Collections.sort(targets, new Comparator<Player>() {
                @Override
                public int compare(Player a, Player b) {
                    int sa = a.ratOvr * Math.max(1, a.portalRiskTier);
                    int sb = b.ratOvr * Math.max(1, b.portalRiskTier);
                    return sb - sa;
                }
            });

            for (Player p : targets) {
                RosterStatus min = p.needsDealRenewal() && p.portalRiskTier == 0
                        ? (p.rosterStatus != null ? p.rosterStatus : RosterStatus.SCHOLARSHIP)
                        : ProgramOffers.minimumAcceptable(p, Math.max(1, p.portalRiskTier));
                if (min == RosterStatus.PWO && p.needsDealRenewal()) min = RosterStatus.SCHOLARSHIP;
                int years = ProgramOffers.suggestedContractYears(p);
                int nil = min == RosterStatus.SCHOLARSHIP_PLUS_NIL
                        ? ProgramOffers.annualNilFor(p, t, years) : 0;
                int cost = t.offerTotalCost(min, nil);
                boolean critical = t.depthRank(p) <= 1;
                if (!bal.canSpend(p.position, cost, critical)) {
                    years = 1;
                    nil = min == RosterStatus.SCHOLARSHIP_PLUS_NIL
                            ? ProgramOffers.annualNilFor(p, t, 1) : 0;
                    cost = t.offerTotalCost(min, nil);
                    if (!bal.canSpend(p.position, cost, critical)) continue;
                }
                if (!t.spendRetentionOffer(min, nil, years, p)) continue;
                p.applyOffer(min, nil, years);
                p.portalRiskTier = 0;
                bal.recordSpend(p.position, cost);
            }
            bal.rebalanceToNeeds();
            aiBuyoutPressure(t, bal);
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
                return b.ratOvr - a.ratOvr;
            }
        });
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
        int cost = user.offerTotalCost(status, nilAmount);
        if (!user.canAffordContract(status, nilAmount, y)) return false;
        if (!ProgramOffers.acceptsOffer(
                p, user, status, nilAmount, Math.max(1, p.portalRiskTier))) return false;
        user.recruitMoney -= cost;
        p.applyOffer(status, nilAmount, y);
        p.team = user;
        p.portalRiskTier = 0;
        user.addPlayerToRoster(p);
        transferPortal.remove(p);
        return true;
    }

    public boolean userSignHs(Team user, Player p, RosterStatus status, int nilAmount, int years) {
        if (!hsClass.contains(p)) return false;
        if (!user.canAddToRoster()) return false;
        if (status.usesScholarship() && !user.canAwardScholarship()) return false;
        int y = Math.min(years, ProgramOffers.maxContractYears(p));
        int cost = user.offerTotalCost(status, nilAmount);
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
                double score = t.programProfile.brandAttract * 0.35
                        + t.programProfile.pipeline * 0.15
                        - depth * 8 + fitBonus(p, t)
                        + bal.needWeight(p.position) * 12;
                RosterStatus offer = ProgramOffers.minimumAcceptable(p, Math.max(1, p.portalRiskTier));
                if (offer.usesScholarship() && !t.canAwardScholarship()) {
                    if (offer == RosterStatus.SCHOLARSHIP_PLUS_NIL) continue;
                    offer = RosterStatus.PWO;
                    if (!ProgramOffers.acceptsOffer(p, t, offer, 0, p.portalRiskTier)) continue;
                }
                int years = Math.min(ProgramOffers.suggestedContractYears(p), ProgramOffers.maxContractYears(p));
                int nil = offer == RosterStatus.SCHOLARSHIP_PLUS_NIL
                        ? ProgramOffers.annualNilFor(p, t, years) : 0;
                int cost = t.offerTotalCost(offer, nil);
                boolean critical = depth <= 1 && bal.needWeight(p.position) >= 1.5;
                if (!t.canAffordContract(offer, nil, years) || !bal.canSpend(p.position, cost, critical)) {
                    years = 1;
                    nil = offer == RosterStatus.SCHOLARSHIP_PLUS_NIL
                            ? ProgramOffers.annualNilFor(p, t, 1) : 0;
                    cost = t.offerTotalCost(offer, nil);
                    if (!t.canAffordContract(offer, nil, years) || !bal.canSpend(p.position, cost, critical)) {
                        if (ProgramOffers.acceptsOffer(
                                p, t, RosterStatus.SCHOLARSHIP, 0, p.portalRiskTier)
                                && t.canAwardScholarship()) {
                            offer = RosterStatus.SCHOLARSHIP;
                            nil = 0;
                            years = 1;
                            cost = t.offerTotalCost(offer, nil);
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
                int cost = best.offerTotalCost(bestOffer, bestNil);
                best.recruitMoney -= cost;
                p.applyOffer(bestOffer, bestNil, bestYears);
                p.portalRiskTier = 0;
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
                int cost = t.offerTotalCost(offer, nil);
                boolean critical = bal.needWeight(pick.position) >= 2.0;
                if (!t.canAffordContract(offer, nil, years) || !bal.canSpend(pick.position, cost, critical)) {
                    years = 1;
                    nil = offer == RosterStatus.SCHOLARSHIP_PLUS_NIL
                            ? ProgramOffers.annualNilFor(pick, t, 1) : 0;
                    cost = t.offerTotalCost(offer, nil);
                    if (!t.canAffordContract(offer, nil, years) || !bal.canSpend(pick.position, cost, critical)) {
                        if (t.canAwardScholarship() && t.offerTotalCost(RosterStatus.SCHOLARSHIP, 0) <= t.recruitMoney) {
                            offer = RosterStatus.SCHOLARSHIP;
                            nil = 0;
                            years = 1;
                            cost = t.offerTotalCost(offer, nil);
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
