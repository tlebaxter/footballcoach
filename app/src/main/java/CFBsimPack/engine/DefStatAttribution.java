package CFBsimPack.engine;

import CFBsimPack.OnFieldEleven;
import CFBsimPack.Player;
import CFBsimPack.PositionGroup;
import CFBsimPack.engine.snap.DuelOutcome;
import CFBsimPack.engine.snap.PassRushMatchup;
import CFBsimPack.engine.snap.ProtectionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Credits defensive counting stats from snap participants already known at resolve time.
 */
public final class DefStatAttribution {

    private final PlayerGameStats gameStats;

    public DefStatAttribution(PlayerGameStats gameStats) {
        this.gameStats = gameStats;
    }

    public void creditDefInt(Player p) {
        if (p == null) return;
        p.seasonStats.defInt++;
        if (gameStats != null) gameStats.line(p).defInt++;
    }

    public void creditPassDef(Player p) {
        if (p == null) return;
        p.seasonStats.passDef++;
        if (gameStats != null) gameStats.line(p).passDef++;
    }

    public void creditSack(Player p) {
        if (p == null) return;
        p.seasonStats.sacksDef++;
        p.seasonStats.tackles++;
        p.seasonStats.tfl++;
        if (gameStats != null) {
            PlayerGameStats.Line line = gameStats.line(p);
            line.sacksDef++;
            line.tackles++;
            line.tfl++;
        }
    }

    public void creditTackle(Player p, boolean forLoss) {
        if (p == null) return;
        p.seasonStats.tackles++;
        if (forLoss) p.seasonStats.tfl++;
        if (gameStats != null) {
            PlayerGameStats.Line line = gameStats.line(p);
            line.tackles++;
            if (forLoss) line.tfl++;
        }
    }

    public void creditForcedFumble(Player p) {
        if (p == null) return;
        p.seasonStats.forcedFumbles++;
        if (gameStats != null) gameStats.line(p).forcedFumbles++;
    }

    public void creditFumbleRec(Player p) {
        if (p == null) return;
        p.seasonStats.fumbleRec++;
        if (gameStats != null) gameStats.line(p).fumbleRec++;
    }

    /**
     * Weight rushers by earliest pressure; bump free/unblocked/LOSS. One full sack credit.
     */
    public static Player pickSackRusher(ProtectionResult protection, Random rng) {
        if (protection == null || protection.matchups == null || protection.matchups.isEmpty()) {
            return null;
        }
        if (protection.freeRusher != null && rng.nextDouble() < 0.55) {
            return protection.freeRusher;
        }
        List<PassRushMatchup> pressed = new ArrayList<>();
        for (PassRushMatchup m : protection.matchups) {
            if (m != null && m.rusher != null && m.pressureAtSec != null) {
                pressed.add(m);
            }
        }
        if (!pressed.isEmpty()) {
            double earliest = protection.earliestPressureSec;
            if (earliest <= 0) earliest = pressed.get(0).pressureAtSec;
            double total = 0;
            double[] weights = new double[pressed.size()];
            for (int i = 0; i < pressed.size(); i++) {
                PassRushMatchup m = pressed.get(i);
                double w = 1.0 / Math.max(0.25, m.pressureAtSec - earliest + 0.25);
                if (m.unblocked) w *= 1.6;
                if (m.duel != null && m.duel.result == DuelOutcome.Result.LOSS) w *= 1.35;
                if (protection.freeRusher != null && m.rusher == protection.freeRusher) w *= 1.4;
                weights[i] = w;
                total += w;
            }
            double roll = rng.nextDouble() * total;
            double acc = 0;
            for (int i = 0; i < pressed.size(); i++) {
                acc += weights[i];
                if (roll <= acc) return pressed.get(i).rusher;
            }
            return pressed.get(pressed.size() - 1).rusher;
        }
        if (protection.freeRusher != null) return protection.freeRusher;
        for (PassRushMatchup m : protection.matchups) {
            if (m != null && m.rusher != null) return m.rusher;
        }
        return null;
    }

    public static Player pickTackle(
            OnFieldEleven def,
            TackleContext ctx,
            int yards,
            boolean wasPass,
            Random rng
    ) {
        List<Weighted> pool = new ArrayList<>();
        if (ctx != null) {
            if (ctx.primary != null) {
                double w = yards <= 2 ? 3.5 : (yards < 0 ? 4.0 : 2.2);
                if (ctx.scramble) w *= 1.3;
                pool.add(new Weighted(ctx.primary, w + tckWeight(ctx.primary)));
            }
            if (ctx.secondary != null) {
                double w = yards <= 2 ? 1.2 : (yards >= 12 ? 2.8 : 2.0);
                pool.add(new Weighted(ctx.secondary, w + tckWeight(ctx.secondary)));
            }
        }
        if (def != null) {
            for (Player p : def.players) {
                if (p == null) continue;
                PositionGroup g = PositionGroup.fromToken(p.position);
                if (g == null) continue;
                double w = 0.35 + tckWeight(p) * 0.04;
                if (wasPass) {
                    if (g == PositionGroup.CB || g == PositionGroup.S) w *= 1.4;
                    if (g == PositionGroup.LB) w *= 1.15;
                } else {
                    if (g == PositionGroup.DL || g == PositionGroup.EDGE) w *= yards <= 3 ? 1.5 : 1.1;
                    if (g == PositionGroup.LB) w *= 1.35;
                    if (g == PositionGroup.S && yards >= 8) w *= 1.4;
                }
                if (ctx != null && (p == ctx.primary || p == ctx.secondary)) continue;
                pool.add(new Weighted(p, w));
            }
        }
        return pickWeighted(pool, rng);
    }

    public static Player pickFumbleRecoverer(OnFieldEleven def, Player exclude, Random rng) {
        if (def == null) return null;
        List<Weighted> pool = new ArrayList<>();
        for (Player p : def.players) {
            if (p == null || p == exclude) continue;
            PositionGroup g = PositionGroup.fromToken(p.position);
            double w = 1.0 + tckWeight(p) * 0.03;
            if (g == PositionGroup.LB || g == PositionGroup.EDGE || g == PositionGroup.DL) w *= 1.35;
            if (g == PositionGroup.S || g == PositionGroup.CB) w *= 1.1;
            pool.add(new Weighted(p, w));
        }
        return pickWeighted(pool, rng);
    }

    /** Contested incomplete → PD chance; separation roughly 0–20+. */
    public static boolean rollContestedPassDef(double separation, Random rng) {
        double contest = Math.max(0, 12.0 - separation) / 12.0;
        double p = 0.35 * contest;
        return rng.nextDouble() < p;
    }

    private static double tckWeight(Player p) {
        if (p == null || p.ratings == null) return 0;
        return Math.max(40, Math.min(99, p.ratings.tck));
    }

    private static Player pickWeighted(List<Weighted> pool, Random rng) {
        if (pool == null || pool.isEmpty()) return null;
        double total = 0;
        for (Weighted w : pool) total += w.weight;
        if (total <= 0) return pool.get(0).player;
        double roll = rng.nextDouble() * total;
        double acc = 0;
        for (Weighted w : pool) {
            acc += w.weight;
            if (roll <= acc) return w.player;
        }
        return pool.get(pool.size() - 1).player;
    }

    private static final class Weighted {
        final Player player;
        final double weight;

        Weighted(Player player, double weight) {
            this.player = player;
            this.weight = weight;
        }
    }
}
