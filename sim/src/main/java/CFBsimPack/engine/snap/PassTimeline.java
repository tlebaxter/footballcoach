package CFBsimPack.engine.snap;

import CFBsimPack.OnFieldEleven;
import CFBsimPack.Player;
import CFBsimPack.PlayerRatings;
import CFBsimPack.engine.CoverageCall;
import CFBsimPack.engine.FatigueTracker;
import CFBsimPack.engine.playdef.RouteAssignment;
import CFBsimPack.engine.playdef.RouteType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.ToIntFunction;

/**
 * Shared pocket clock + route beats + QB progression.
 */
public final class PassTimeline {

    private final DuelResolver duels;
    private final Random rng;
    private final CoverageAssigner coverageAssigner = new CoverageAssigner();

    public PassTimeline(Random rng) {
        this.rng = rng != null ? rng : new Random();
        this.duels = new DuelResolver(this.rng);
    }

    public static final class TimelineState {
        public final ProtectionResult protection;
        public final List<CoverageAssignment> coverage;
        public final ThrowWindow decision;
        public final List<RouteAssignment> routes;

        TimelineState(ProtectionResult protection, List<CoverageAssignment> coverage,
                      ThrowWindow decision, List<RouteAssignment> routes) {
            this.protection = protection;
            this.coverage = coverage;
            this.decision = decision;
            this.routes = routes;
        }
    }

    public TimelineState run(
            OnFieldEleven off,
            OnFieldEleven def,
            ProtectionResult protection,
            List<RouteAssignment> routes,
            CoverageCall cov,
            FatigueTracker fatigue,
            boolean pressJam
    ) {
        Map<OffSlot, Player> offMap = SlotMapper.mapOffense(off);
        List<RouteAssignment> usable = routes != null ? routes : new ArrayList<>();
        List<CoverageAssignment> coverage = coverageAssigner.assign(def, cov, usable, offMap);

        double pressureAt = protection != null ? protection.earliestPressureSec : 3.2;
        boolean hotForced = protection != null && protection.hotForced;

        // Default drop time preference from deepest read
        double preferThrowBy = 2.2;
        for (RouteAssignment r : usable) {
            if (r != null && r.readPriority <= 2) {
                preferThrowBy = Math.min(preferThrowBy, r.openBeatSec + 0.35);
            }
        }

        ThrowWindow bestHot = null;
        ThrowWindow bestOpen = null;

        for (double t = 0.5; t <= 4.5; t += 0.25) {
            if (t >= pressureAt) {
                // Only keep a pre-pressure throw if it was clearly open before the rush arrived
                if (bestOpen != null
                        && bestOpen.throwTimeSec < pressureAt - 0.2
                        && bestOpen.separation >= 11) {
                    return new TimelineState(protection, coverage, bestOpen, usable);
                }
                if (bestHot != null && bestHot.separation >= 10
                        && bestHot.throwTimeSec < pressureAt - 0.1) {
                    return new TimelineState(protection, coverage, bestHot, usable);
                }
                return new TimelineState(protection, coverage,
                        new ThrowWindow(ThrowWindow.Decision.PRESSURE_OUT, null, null, null, t, 0, hotForced),
                        usable);
            }
            if (hotForced && t >= Math.min(1.15, pressureAt)) {
                ThrowWindow hot = pickBest(usable, offMap, coverage, t, fatigue, cov, pressJam, true);
                // Only force a hot throw if it is actually open; otherwise scramble/sack policy
                if (hot != null && hot.separation >= 8.5) {
                    return new TimelineState(protection, coverage,
                            new ThrowWindow(ThrowWindow.Decision.HOT_FORCE, hot.route, hot.target,
                                    hot.coverage, t, hot.separation, true),
                            usable);
                }
                return new TimelineState(protection, coverage,
                        new ThrowWindow(ThrowWindow.Decision.PRESSURE_OUT, null, null, null, t, 0, true),
                        usable);
            }

            ThrowWindow cand = pickBest(usable, offMap, coverage, t, fatigue, cov, pressJam, false);
            if (cand != null) {
                if (cand.hot && (bestHot == null || cand.separation > bestHot.separation)) {
                    bestHot = cand;
                }
                if (!cand.hot || cand.separation >= 8) {
                    if (bestOpen == null || candScore(cand) > candScore(bestOpen)) {
                        bestOpen = new ThrowWindow(ThrowWindow.Decision.THROW, cand.route, cand.target,
                                cand.coverage, t, cand.separation, cand.hot);
                    }
                }
                // Throw when first read is clearly open and drop time reached
                if (bestOpen != null && t >= preferThrowBy && bestOpen.separation >= 9) {
                    return new TimelineState(protection, coverage, bestOpen, usable);
                }
            }
        }

        if (bestOpen != null) {
            return new TimelineState(protection, coverage, bestOpen, usable);
        }
        if (bestHot != null) {
            return new TimelineState(protection, coverage, bestHot, usable);
        }
        // Force a late throw to first route
        RouteAssignment fallback = usable.isEmpty() ? null : usable.get(0);
        Player tgt = fallback != null ? offMap.get(fallback.slot) : null;
        CoverageAssignment covA = findCoverage(coverage, fallback);
        return new TimelineState(protection, coverage,
                new ThrowWindow(ThrowWindow.Decision.THROW, fallback, tgt, covA, 2.6, 6, false),
                usable);
    }

    private double candScore(ThrowWindow w) {
        if (w == null) return -1;
        return w.separation * 2 - w.route.readPriority + (w.hot ? -1 : 0);
    }

    private ThrowWindow pickBest(
            List<RouteAssignment> routes,
            Map<OffSlot, Player> offMap,
            List<CoverageAssignment> coverage,
            double t,
            FatigueTracker fatigue,
            CoverageCall call,
            boolean pressJam,
            boolean hotOnly
    ) {
        List<RouteAssignment> ordered = new ArrayList<>(routes);
        ordered.sort(Comparator.comparingInt(r -> r.readPriority));
        ThrowWindow best = null;
        for (RouteAssignment route : ordered) {
            if (route == null) continue;
            if (hotOnly && !route.hotEligible) continue;
            if (t + 0.01 < route.openBeatSec) continue;
            Player target = offMap.get(route.slot);
            if (target == null) continue;
            CoverageAssignment cov = findCoverage(coverage, route);
            double sep = separation(target, route, cov, t, fatigue, pressJam, call);
            ThrowWindow w = new ThrowWindow(ThrowWindow.Decision.THROW, route, target, cov, t, sep,
                    route.hotEligible);
            if (best == null || candScore(w) > candScore(best)) best = w;
        }
        return best;
    }

    private CoverageAssignment findCoverage(List<CoverageAssignment> coverage, RouteAssignment route) {
        if (coverage == null || route == null) return null;
        for (CoverageAssignment a : coverage) {
            if (a.mode == CoverageMode.MAN && a.manTarget == route.slot) return a;
        }
        // Zone: pick landmark matching route depth
        ZoneLandmark want = landmarkFor(route.route, route.depthYards);
        CoverageAssignment best = null;
        for (CoverageAssignment a : coverage) {
            if (a.mode != CoverageMode.ZONE || a.isSpy) continue;
            if (a.zoneLandmark == want) return a;
            if (best == null) best = a;
        }
        return best;
    }

    private ZoneLandmark landmarkFor(RouteType route, int depth) {
        if (route == RouteType.FLAT || route == RouteType.SCREEN || route == RouteType.ANGLE) {
            return ZoneLandmark.FLAT;
        }
        if (route == RouteType.SLANT || route == RouteType.HITCH || route == RouteType.MESH_CROSS) {
            return ZoneLandmark.HOOK;
        }
        if (depth >= 20 || route == RouteType.VERT || route == RouteType.POST || route == RouteType.CORNER) {
            return ZoneLandmark.DEEP_THIRD;
        }
        if (route == RouteType.DIG || route == RouteType.CROSS || route == RouteType.SEAM) {
            return ZoneLandmark.CURL;
        }
        return ZoneLandmark.HOOK;
    }

    private double separation(
            Player target,
            RouteAssignment route,
            CoverageAssignment cov,
            double t,
            FatigueTracker fatigue,
            boolean pressJam,
            CoverageCall call
    ) {
        int rtr = rate(target, fatigue, x -> x.rtr, 60);
        int spd = rate(target, fatigue, x -> x.spd, 55);
        double openBonus = Math.max(0, t - route.openBeatSec) * 3.5;
        double callAdj = SafetyHelp.passSepAdjust(call);
        if (cov == null) {
            return Math.max(1, Math.min(22, 10 + (rtr - 50) / 8.0 + openBonus + callAdj));
        }
        if (cov.mode == CoverageMode.MAN) {
            int pcv = rate(cov.defender, fatigue, x -> x.pcv, 65);
            int cSpd = rate(cov.defender, fatigue, x -> x.spd, 65);
            int offR = (int) (rtr * 0.6 + spd * 0.4);
            int defR = (int) (pcv * 0.6 + cSpd * 0.4);
            if (pressJam) {
                // Jam delays early separation
                if (t < route.openBeatSec + 0.35) offR -= 10;
            }
            DuelOutcome duel = duels.contest(offR, defR);
            double sep = 8 + duel.margin * 6 + openBonus + callAdj;
            if (route.route == RouteType.MESH_CROSS || route.route == RouteType.CROSS) sep += 2.5;
            if (pressJam) sep -= 0.8;
            return Math.max(1, Math.min(22, sep));
        }
        // Zone vacancy
        double vac = 9 + openBonus + (rtr - 55) / 10.0 + callAdj;
        if (cov.zoneLandmark == ZoneLandmark.DEEP_HALF || cov.zoneLandmark == ZoneLandmark.DEEP_QUARTER) {
            if (route.depthYards >= 18) vac -= 3;
            else vac += 2;
        }
        if (cov.zoneLandmark == ZoneLandmark.FLAT && route.depthYards <= 5) vac -= 2;
        if (route.route == RouteType.MESH_CROSS || route.route == RouteType.CROSS) vac += 1.5;
        int pcv = rate(cov.defender, fatigue, x -> x.pcv, 60);
        vac -= (pcv - 60) / 12.0;
        return Math.max(1, Math.min(22, vac));
    }

    private int rate(Player p, FatigueTracker fatigue, ToIntFunction<PlayerRatings> attr, int fallback) {
        if (p == null || p.ratings == null) return fallback;
        int raw = attr.applyAsInt(p.ratings);
        if (fatigue != null) return (int) Math.round(raw * fatigue.factor(p));
        return raw;
    }
}
