package CFBsimPack.engine.snap;

import CFBsimPack.OnFieldEleven;
import CFBsimPack.Player;
import CFBsimPack.PlayerRatings;
import CFBsimPack.engine.CoverageCall;
import CFBsimPack.engine.FatigueTracker;
import CFBsimPack.engine.playdef.ExplosiveProfile;
import CFBsimPack.engine.playdef.RunScheme;
import CFBsimPack.engine.playdef.RunTrack;

import java.util.Map;
import java.util.Random;
import java.util.function.ToIntFunction;

/**
 * Gap-based run resolution with doubles, cutbacks, explosives, and safety brakes.
 */
public final class RunGapResolver {

    private final DuelResolver duels;
    private final Random rng;

    public RunGapResolver(Random rng) {
        this.rng = rng != null ? rng : new Random();
        this.duels = new DuelResolver(this.rng);
    }

    public RunGapResult resolve(
            OnFieldEleven off,
            OnFieldEleven def,
            Player carrier,
            RunScheme scheme,
            CoverageCall cov,
            SituationMods sit,
            FatigueTracker fatigue,
            double runYardsMod,
            int matchupBonus,
            int atmosphereBonus,
            double sysRunWeight
    ) {
        if (scheme == null) scheme = RunScheme.forTrack(RunTrack.INSIDE_ZONE);
        if (sit == null) sit = new SituationMods(0, 0, false, false, false, 0, false);

        Map<OffSlot, Player> offMap = SlotMapper.mapOffense(off);
        Map<DefSlot, Player> defMap = SlotMapper.mapDefense(def);

        Gap primary = scheme.primaryGap;
        Gap secondary = scheme.secondaryGap;
        if (sit.forcePrimaryGap) {
            secondary = null;
        }

        double creasePrimary = resolveGapCrease(primary, offMap, defMap, scheme, fatigue, sit, true);
        Gap used = primary;
        boolean cutback = false;
        double crease = creasePrimary;

        boolean allowCut = secondary != null && !sit.suppressCutback
                && (scheme.track == RunTrack.OUTSIDE_ZONE || scheme.track == RunTrack.SWEEP
                || scheme.track == RunTrack.OPTION || scheme.track == RunTrack.INSIDE_ZONE);
        if (allowCut && creasePrimary < -1.5) {
            double creaseSec = resolveGapCrease(secondary, offMap, defMap, scheme, fatigue, sit, false);
            if (creaseSec > creasePrimary + 1.0) {
                used = secondary;
                crease = creaseSec;
                cutback = true;
            }
        }

        crease += matchupBonus / 3.0;
        crease -= (sysRunWeight - 1.0) * 3.0;
        if (cov == CoverageCall.STACK_BOX) crease -= 2.5;

        int spd = rate(carrier, fatigue, x -> x.spd, 55);
        int stre = rate(carrier, fatigue, x -> x.stre, 55);
        int elu = rate(carrier, fatigue, x -> x.elu, 50);
        double attr;
        switch (scheme.track) {
            case DIVE:
            case ISO:
            case POWER:
                attr = stre * 0.55 + spd * 0.25 + elu * 0.20;
                break;
            case OUTSIDE_ZONE:
            case SWEEP:
            case OPTION:
                attr = spd * 0.50 + elu * 0.35 + stre * 0.15;
                break;
            case QB_DRAW:
                attr = spd * 0.45 + elu * 0.30 + stre * 0.25;
                break;
            default:
                attr = spd * 0.40 + stre * 0.30 + elu * 0.30;
                break;
        }

        int yards = (int) ((attr * 0.35 + crease * 2.2 + atmosphereBonus)
                * (0.35 + rng.nextDouble() * 0.85) / 8.5 * runYardsMod);
        if (yards < 2 && (scheme.track == RunTrack.POWER || scheme.track == RunTrack.DIVE
                || scheme.track == RunTrack.ISO)) {
            yards += stre / 22 - 2;
        }

        ExplosiveProfile exp = scheme.explosive;
        SafetyHelp.Shell shell = SafetyHelp.shell(cov);
        boolean inside = used.isInterior();
        double burstP = exp.baseBurstChance * Math.max(0.2, (crease + 6) / 12.0);
        burstP *= SafetyHelp.explosiveBrake(shell, exp, inside);
        boolean explosive = false;
        if (rng.nextDouble() < burstP) {
            explosive = true;
            int chunk = (int) (8 + spd / 12.0 * rng.nextDouble() * exp.breakawayMult
                    * SafetyHelp.explosiveBrake(shell, exp, inside));
            if (shell == SafetyHelp.Shell.TWO_HIGH && chunk > 12) {
                chunk = 12 + (chunk - 12) / 2;
            }
            if (shell == SafetyHelp.Shell.BOX_SS && inside) {
                chunk = Math.min(chunk, 8);
            }
            yards += chunk;
        }

        if (cov != null && isQb(carrier)) {
            yards = (int) (yards * cov.scrambleMod);
        }
        if (yards < -4) yards = -4;
        return new RunGapResult(used, cutback, crease, yards, explosive);
    }

    private double resolveGapCrease(
            Gap gap,
            Map<OffSlot, Player> offMap,
            Map<DefSlot, Player> defMap,
            RunScheme scheme,
            FatigueTracker fatigue,
            SituationMods sit,
            boolean allowDouble
    ) {
        DefSlot first = firstLevelFor(gap);
        DefSlot second = secondLevelFor(gap);
        Player dl = defMap.get(first);
        if (dl == null) dl = defMap.get(DefSlot.DT);
        if (dl == null) dl = defMap.get(DefSlot.NT);

        OffSlot olA = olForGap(gap, true);
        OffSlot olB = olForGap(gap, false);
        Player blocker = offMap.get(olA);
        if (blocker == null) blocker = offMap.get(OffSlot.C);

        int offR = runBlockRating(blocker, fatigue) + (int) sit.duelOffenseBoost;
        int defR = runStopRating(dl, fatigue);

        boolean doubled = allowDouble && scheme.doubleTeamA != null && scheme.doubleTeamB != null
                && (rng.nextDouble() < 0.35 + sit.doubleTeamChanceBoost);
        if (doubled) {
            Player helper = offMap.get(scheme.doubleTeamB);
            offR = (offR + runBlockRating(helper, fatigue)) / 2 + 10;
        }

        DuelOutcome firstDuel = duels.contest(offR, defR);
        double crease = firstDuel.margin * 4.5;

        Player lb = defMap.get(second);
        if (lb == null) lb = defMap.get(DefSlot.MIKE);
        if (lb == null) lb = defMap.get(DefSlot.LB);
        if (lb != null) {
            int leadBonus = 0;
            if (scheme.leadBlocker != null && offMap.get(scheme.leadBlocker) != null) {
                leadBonus = runBlockRating(offMap.get(scheme.leadBlocker), fatigue) / 8;
            }
            // Doubles free the LB climb
            int lbR = runStopRating(lb, fatigue) + (doubled ? 6 : 0);
            int secondOff = 55 + leadBonus + (int) (firstDuel.isWin() ? 8 : 0);
            DuelOutcome secondDuel = duels.contest(secondOff, lbR);
            crease += secondDuel.margin * 3.0;
        }

        if (scheme.track == RunTrack.COUNTER) crease -= 0.8; // slower develop
        if (scheme.track == RunTrack.QB_DRAW) crease += 1.0; // invite upfield
        return crease;
    }

    private DefSlot firstLevelFor(Gap gap) {
        switch (gap) {
            case A_L:
            case B_L:
                return DefSlot.DT;
            case A_R:
            case B_R:
                return DefSlot.NT;
            case C_L:
            case D_L:
                return DefSlot.EDGE_L;
            case C_R:
            case D_R:
            default:
                return DefSlot.EDGE_R;
        }
    }

    private DefSlot secondLevelFor(Gap gap) {
        switch (gap) {
            case A_L:
            case B_L:
            case C_L:
                return DefSlot.WILL;
            case A_R:
            case B_R:
                return DefSlot.MIKE;
            case C_R:
            case D_R:
            default:
                return DefSlot.SAM;
        }
    }

    private OffSlot olForGap(Gap gap, boolean primary) {
        switch (gap) {
            case A_L:
            case B_L:
                return primary ? OffSlot.LG : OffSlot.C;
            case A_R:
            case B_R:
                return primary ? OffSlot.RG : OffSlot.C;
            case C_L:
            case D_L:
                return primary ? OffSlot.LT : OffSlot.LG;
            case C_R:
            case D_R:
            default:
                return primary ? OffSlot.RT : OffSlot.RG;
        }
    }

    private int runBlockRating(Player p, FatigueTracker fatigue) {
        if (p == null) return 50;
        int rbk = rate(p, fatigue, x -> x.rbk, 55);
        int stre = rate(p, fatigue, x -> x.stre, 55);
        int spd = rate(p, fatigue, x -> x.spd, 50);
        return (int) (rbk * 0.55 + stre * 0.35 + spd * 0.10);
    }

    private int runStopRating(Player p, FatigueTracker fatigue) {
        if (p == null) return 55;
        int rns = rate(p, fatigue, x -> x.rns, 60);
        int stre = rate(p, fatigue, x -> x.stre, 55);
        int tck = rate(p, fatigue, x -> x.tck, 55);
        return (int) (rns * 0.50 + stre * 0.30 + tck * 0.20);
    }

    private boolean isQb(Player p) {
        return p != null && p.position != null && p.position.toUpperCase().contains("QB");
    }

    private int rate(Player p, FatigueTracker fatigue, ToIntFunction<PlayerRatings> attr, int fallback) {
        if (p == null || p.ratings == null) return fallback;
        int raw = attr.applyAsInt(p.ratings);
        if (fatigue != null) return (int) Math.round(raw * fatigue.factor(p));
        return raw;
    }
}
