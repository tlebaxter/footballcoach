package CFBsimPack.engine.snap;

import CFBsimPack.Player;
import CFBsimPack.PlayerRatings;
import CFBsimPack.engine.CoverageCall;
import CFBsimPack.engine.FatigueTracker;
import CFBsimPack.engine.playdef.RouteAssignment;

import java.util.List;
import java.util.Random;
import java.util.function.ToIntFunction;

/**
 * Throw-time interceptions: primary coverage, safety help, rare DL tip.
 */
public final class IntResolver {

    public enum Source { NONE, PRIMARY, SAFETY, DL_TIP }

    public static final class IntResult {
        public final Source source;
        public final Player interceptor;
        public final Player tipper;
        public final boolean incompleteBatDown;

        IntResult(Source source, Player interceptor, Player tipper, boolean incompleteBatDown) {
            this.source = source != null ? source : Source.NONE;
            this.interceptor = interceptor;
            this.tipper = tipper;
            this.incompleteBatDown = incompleteBatDown;
        }

        public boolean isInt() {
            return source != Source.NONE && !incompleteBatDown && interceptor != null;
        }
    }

    private final Random rng;

    public IntResolver(Random rng) {
        this.rng = rng != null ? rng : new Random();
    }

    public IntResult resolve(
            Player qb,
            RouteAssignment route,
            Player target,
            CoverageAssignment primary,
            List<CoverageAssignment> coverage,
            CoverageCall cov,
            ProtectionResult protection,
            double throwTimeSec,
            double separation,
            FatigueTracker fatigue
    ) {
        if (qb == null || route == null) {
            return new IntResult(Source.NONE, null, null, false);
        }
        int tha = rate(qb, fatigue, x -> x.tha, 55);
        int thv = rate(qb, fatigue, x -> x.thv, 55);
        int foot = qb.ratFootIQ;
        int depthYd = route.depthYards;
        boolean hurried = protection != null && throwTimeSec >= protection.earliestPressureSec - 0.05;
        double intMod = cov != null ? cov.intMod : 1.0;

        double base = 2.6
                + (hurried ? 1.6 : 0)
                + Math.max(0, 12 - separation) * 0.22
                + (depthYd >= 18 ? 1.35 : 0)
                + (depthYd >= 12 ? 0.55 : 0)
                - (tha - 50) / 28.0
                - (thv - 50) / 35.0
                - (foot - 50) / 40.0;
        base *= intMod;
        if (base < 0.4) base = 0.4;
        if (base > 14) base = 14;

        // Rare DL tip on hurried throws
        if (hurried && protection != null && rng.nextDouble() < 0.045 * (hurried ? 1.0 : 0.2)) {
            Player tipper = firstRusher(protection);
            if (rng.nextDouble() < 0.28) {
                // Tip-INT to nearby defender
                Player interceptor = primary != null && primary.defender != null
                        ? primary.defender
                        : SafetyHelp.deepHelper(coverage);
                if (interceptor == null) interceptor = tipper;
                return new IntResult(Source.DL_TIP, interceptor, tipper, false);
            }
            // Bat down incomplete
            return new IntResult(Source.DL_TIP, null, tipper, true);
        }

        double primaryP = base * primaryContestFactor(primary, separation, fatigue);
        if (rng.nextDouble() * 100 < primaryP) {
            Player p = primary != null ? primary.defender : null;
            return new IntResult(Source.PRIMARY, p, null, false);
        }

        SafetyHelp.Shell shell = SafetyHelp.shell(cov);
        double safetyP = base * 0.55 * SafetyHelp.helpIntFactor(shell, depthYd);
        if (depthYd < 8) safetyP *= 0.25;
        if (rng.nextDouble() * 100 < safetyP) {
            Player s = SafetyHelp.deepHelper(coverage);
            if (s == null && primary != null) s = primary.defender;
            return new IntResult(Source.SAFETY, s, null, false);
        }

        return new IntResult(Source.NONE, null, null, false);
    }

    private double primaryContestFactor(CoverageAssignment primary, double separation, FatigueTracker fatigue) {
        if (primary == null || primary.defender == null) return 0.55;
        if (primary.isSpy) return 0.15;
        int pcv = rate(primary.defender, fatigue, x -> x.pcv, 65);
        double f = 0.65 + (pcv - 60) / 50.0;
        if (primary.mode == CoverageMode.MAN) f += 0.15;
        if (separation > 14) f *= 0.45;
        if (separation < 6) f *= 1.25;
        return Math.max(0.2, Math.min(1.6, f));
    }

    private Player firstRusher(ProtectionResult protection) {
        if (protection == null) return null;
        for (PassRushMatchup m : protection.matchups) {
            if (m != null && m.rusher != null && m.pressureAtSec != null) return m.rusher;
        }
        for (PassRushMatchup m : protection.matchups) {
            if (m != null && m.rusher != null) return m.rusher;
        }
        return null;
    }

    private int rate(Player p, FatigueTracker fatigue, ToIntFunction<PlayerRatings> attr, int fallback) {
        if (p == null || p.ratings == null) return fallback;
        int raw = attr.applyAsInt(p.ratings);
        if (fatigue != null) return (int) Math.round(raw * fatigue.factor(p));
        return raw;
    }
}
