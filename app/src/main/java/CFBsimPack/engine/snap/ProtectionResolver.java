package CFBsimPack.engine.snap;

import CFBsimPack.OnFieldEleven;
import CFBsimPack.Player;
import CFBsimPack.PlayerRatings;
import CFBsimPack.engine.FatigueTracker;
import CFBsimPack.engine.playdef.ProtectionScheme;
import CFBsimPack.engine.playdef.ProtectionType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.ToIntFunction;

/**
 * Real OL–rusher pairs with pressure arrival times, doubles, and hot triggers.
 */
public final class ProtectionResolver {

    private final DuelResolver duels;
    private final Random rng;

    public ProtectionResolver(Random rng) {
        this.rng = rng != null ? rng : new Random();
        this.duels = new DuelResolver(this.rng);
    }

    public ProtectionResult resolve(
            OnFieldEleven off,
            OnFieldEleven def,
            ProtectionScheme scheme,
            SituationMods sit,
            FatigueTracker fatigue,
            double sackRiskMod,
            int roadPressureAdd,
            double thvScale
    ) {
        if (scheme == null) {
            scheme = ProtectionScheme.infer(null, false, false);
        }
        if (sit == null) {
            sit = new SituationMods(0, 0, false, false, false, 0, false);
        }

        Map<OffSlot, Player> offMap = SlotMapper.mapOffense(off);
        Map<DefSlot, Player> defMap = SlotMapper.mapDefense(def);
        boolean blitz = sit.extraRusher || scheme.type == ProtectionType.EMPTY_FIVE;
        List<DefSlot> rusherSlots = SlotMapper.rusherSlots(defMap, blitz);
        // Cap rushers: base 4, +1 if blitz/cover0, empty often faces 5
        int maxRush = scheme.type == ProtectionType.EMPTY_FIVE ? 5 : (blitz ? 5 : 4);
        if (rusherSlots.size() > maxRush) {
            rusherSlots = new ArrayList<>(rusherSlots.subList(0, maxRush));
        }

        List<OffSlot> blockers = blockersFor(scheme, offMap);
        if (blockers.isEmpty() || rusherSlots.isEmpty()) {
            return fallback(sackRiskMod, roadPressureAdd, thvScale);
        }

        // Optional double on stud interior
        DefSlot doubledSlot = null;
        OffSlot helperSlot = null;
        double doubleChance = scheme.allowDoubleTeams ? 0.18 + sit.doubleTeamChanceBoost : 0;
        if (scheme.type == ProtectionType.MAX_PROTECT) doubleChance += 0.25;
        if (scheme.type == ProtectionType.PLAY_ACTION) doubleChance += 0.08;
        if (doubleChance > 0 && rng.nextDouble() < doubleChance && blockers.size() >= 2) {
            doubledSlot = pickDoubleTarget(rusherSlots, defMap, fatigue);
            if (doubledSlot != null) {
                helperSlot = pickHelper(blockers, doubledSlot);
            }
        }

        List<PassRushMatchup> matchups = new ArrayList<>();
        Set<OffSlot> usedBlockers = new HashSet<>();
        Player freeRusher = null;
        double earliest = 9.0;
        boolean hotForced = false;
        double hotTime = sit.adjustedHotTime(scheme.hotTimeSec, scheme.type);

        // Pair rushers to blockers
        Map<DefSlot, OffSlot> pairs = new EnumMap<>(DefSlot.class);
        int bIdx = 0;
        for (DefSlot rs : rusherSlots) {
            if (rs == doubledSlot && helperSlot != null) {
                // Primary blocker + helper both mark this rusher
                OffSlot primary = primaryForDouble(blockers, helperSlot);
                pairs.put(rs, primary);
                usedBlockers.add(primary);
                usedBlockers.add(helperSlot);
                continue;
            }
            OffSlot b = null;
            while (bIdx < blockers.size()) {
                OffSlot cand = blockers.get(bIdx++);
                if (!usedBlockers.contains(cand)) {
                    b = cand;
                    break;
                }
            }
            if (b == null) {
                // Unblocked
                Player rusher = defMap.get(rs);
                double arr = arrivalFor(DuelOutcome.Result.LOSS, 1.2, true, sackRiskMod, roadPressureAdd, thvScale);
                if (scheme.type == ProtectionType.PLAY_ACTION) arr += 0.25;
                matchups.add(new PassRushMatchup(null, null, rusher, rs,
                        new DuelOutcome(DuelOutcome.Result.LOSS, -1.2), arr, false, true));
                earliest = Math.min(earliest, arr);
                if (arr < hotTime) {
                    hotForced = true;
                    freeRusher = rusher;
                }
                continue;
            }
            pairs.put(rs, b);
            usedBlockers.add(b);
        }

        for (Map.Entry<DefSlot, OffSlot> e : pairs.entrySet()) {
            DefSlot rs = e.getKey();
            OffSlot bs = e.getValue();
            Player rusher = defMap.get(rs);
            Player blocker = offMap.get(bs);
            boolean doubled = rs == doubledSlot && helperSlot != null;
            int offR = blockerRating(blocker, fatigue, true);
            int defR = rusherRating(rusher, fatigue);
            if (doubled) {
                Player helper = offMap.get(helperSlot);
                offR = (offR + blockerRating(helper, fatigue, true)) / 2 + 12;
            }
            offR += (int) sit.duelOffenseBoost;
            if (scheme.type == ProtectionType.SPRINT_OUT_LEFT || scheme.type == ProtectionType.SPRINT_OUT_RIGHT) {
                if (isEdge(rs)) defR -= 8;
            }
            DuelOutcome duel = duels.contest(offR, defR);
            Double arr = arrivalFor(duel.result, duel.margin, false, sackRiskMod, roadPressureAdd, thvScale);
            if (scheme.type == ProtectionType.PLAY_ACTION && arr != null) arr += 0.30;
            if (scheme.type == ProtectionType.MAX_PROTECT && arr != null) arr += 0.35;
            if (scheme.type == ProtectionType.EMPTY_FIVE && arr != null) arr -= 0.15;
            if (arr != null) {
                earliest = Math.min(earliest, arr);
                if (arr < hotTime && (duel.isLoss() || doubled == false && duel.result == DuelOutcome.Result.HOLD)) {
                    if (duel.isLoss()) {
                        hotForced = true;
                        if (freeRusher == null) freeRusher = rusher;
                    }
                }
            }
            matchups.add(new PassRushMatchup(blocker, bs, rusher, rs, duel, arr, doubled, false));
        }

        if (earliest > 8) earliest = 4.5;
        return new ProtectionResult(matchups, earliest, hotForced, freeRusher, false);
    }

    private ProtectionResult fallback(double sackRiskMod, int roadPressureAdd, double thvScale) {
        double arr = 2.4 * sackRiskMod - roadPressureAdd / 40.0;
        arr *= thvScale;
        if (arr < 1.2) arr = 1.2;
        List<PassRushMatchup> m = new ArrayList<>();
        m.add(new PassRushMatchup(null, null, null, DefSlot.EDGE_L,
                new DuelOutcome(DuelOutcome.Result.HOLD, 0), arr, false, false));
        return new ProtectionResult(m, arr, false, null, true);
    }

    private List<OffSlot> blockersFor(ProtectionScheme scheme, Map<OffSlot, Player> offMap) {
        List<OffSlot> out = new ArrayList<>();
        for (OffSlot s : new OffSlot[]{OffSlot.LT, OffSlot.LG, OffSlot.C, OffSlot.RG, OffSlot.RT}) {
            if (offMap.containsKey(s)) out.add(s);
        }
        if (scheme.type == ProtectionType.MAX_PROTECT || !scheme.maxProtectSlots.isEmpty()) {
            for (OffSlot s : scheme.maxProtectSlots) {
                if (offMap.containsKey(s) && !out.contains(s)) out.add(s);
            }
        } else if (scheme.type != ProtectionType.EMPTY_FIVE) {
            // Occasional TE chip stays in
            if (offMap.containsKey(OffSlot.TE_L) && rng.nextDouble() < 0.25) {
                out.add(OffSlot.TE_L);
            }
        }
        // Slide order preference
        if (scheme.type == ProtectionType.HALF_SLIDE_LEFT || scheme.type == ProtectionType.FULL_SLIDE) {
            // already LT-first
        } else if (scheme.type == ProtectionType.HALF_SLIDE_RIGHT) {
            java.util.Collections.reverse(out);
        }
        return out;
    }

    private DefSlot pickDoubleTarget(List<DefSlot> rushers, Map<DefSlot, Player> defMap, FatigueTracker fatigue) {
        DefSlot best = null;
        int bestR = -1;
        for (DefSlot s : rushers) {
            if (s != DefSlot.DT && s != DefSlot.NT && s != DefSlot.MIKE) continue;
            int r = rusherRating(defMap.get(s), fatigue);
            if (r > bestR) {
                bestR = r;
                best = s;
            }
        }
        return best;
    }

    private OffSlot pickHelper(List<OffSlot> blockers, DefSlot doubled) {
        if (blockers.size() < 2) return null;
        // Prefer center/guard as helper
        for (OffSlot s : new OffSlot[]{OffSlot.C, OffSlot.LG, OffSlot.RG}) {
            if (blockers.contains(s)) return s;
        }
        return blockers.get(blockers.size() - 1);
    }

    private OffSlot primaryForDouble(List<OffSlot> blockers, OffSlot helper) {
        for (OffSlot s : blockers) {
            if (s != helper) return s;
        }
        return helper;
    }

    private boolean isEdge(DefSlot s) {
        return s == DefSlot.EDGE_L || s == DefSlot.EDGE_R || s == DefSlot.LE || s == DefSlot.RE;
    }

    private Double arrivalFor(
            DuelOutcome.Result result,
            double margin,
            boolean unblocked,
            double sackRiskMod,
            int roadPressureAdd,
            double thvScale
    ) {
        double base;
        if (unblocked) {
            base = 1.0 + rng.nextDouble() * 0.6;
        } else if (result == DuelOutcome.Result.WIN) {
            return null; // sealed
        } else if (result == DuelOutcome.Result.HOLD) {
            base = 2.8 + rng.nextDouble() * 0.7 - Math.min(0.4, Math.abs(margin) * 0.15);
        } else {
            base = 1.4 + rng.nextDouble() * 1.2 - Math.min(0.5, Math.abs(margin) * 0.25);
        }
        base *= Math.max(0.75, sackRiskMod);
        base -= roadPressureAdd / 50.0;
        // thvScale < 1 for high-thv QBs (same meaning as old pressureScale): buy time
        base /= Math.max(0.7, Math.min(1.25, thvScale));
        if (base < 0.9) base = 0.9;
        if (base > 4.5) base = 4.5;
        return base;
    }

    private int blockerRating(Player p, FatigueTracker fatigue, boolean pass) {
        if (p == null) return 50;
        int pbk = rate(p, fatigue, x -> x.pbk, 55);
        int stre = rate(p, fatigue, x -> x.stre, 55);
        int spd = rate(p, fatigue, x -> x.spd, 50);
        return (int) (pbk * 0.55 + stre * 0.30 + spd * 0.15);
    }

    private int rusherRating(Player p, FatigueTracker fatigue) {
        if (p == null) return 55;
        int prs = rate(p, fatigue, x -> x.prs, 60);
        int stre = rate(p, fatigue, x -> x.stre, 55);
        int spd = rate(p, fatigue, x -> x.spd, 55);
        return (int) (prs * 0.55 + stre * 0.25 + spd * 0.20);
    }

    private int rate(Player p, FatigueTracker fatigue, ToIntFunction<PlayerRatings> attr, int fallback) {
        if (p == null || p.ratings == null) return fallback;
        int raw = attr.applyAsInt(p.ratings);
        if (fatigue != null) {
            return (int) Math.round(raw * fatigue.factor(p));
        }
        return raw;
    }
}
