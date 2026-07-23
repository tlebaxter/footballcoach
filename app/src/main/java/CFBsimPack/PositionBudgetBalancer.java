package CFBsimPack;

import java.util.HashMap;
import java.util.Map;

/**
 * Splits an AI team's spendable budget across positions by depth need.
 */
public class PositionBudgetBalancer {
    private final Team team;
    private final Map<String, Integer> caps = new HashMap<>();
    private final Map<String, Integer> spent = new HashMap<>();

    public PositionBudgetBalancer(Team team, int spendable) {
        this.team = team;
        double[] weights = new double[NilMoney.POSITIONS.length];
        double sum = 0;
        for (int i = 0; i < NilMoney.POSITIONS.length; i++) {
            String pos = NilMoney.POSITIONS[i];
            int have = team.getPositionList(pos) != null ? team.getPositionList(pos).size() : 0;
            int sug = NilMoney.sugFor(pos);
            int deficit = Math.max(0, sug - have);
            // Floor so in-cap positions can still renew a starter
            double w = 0.35 + deficit * 1.25;
            if (have > sug + 2) w = 0.20;
            weights[i] = w;
            sum += w;
        }
        int assigned = 0;
        for (int i = 0; i < NilMoney.POSITIONS.length; i++) {
            String pos = NilMoney.POSITIONS[i];
            int share;
            if (i == NilMoney.POSITIONS.length - 1) {
                share = Math.max(0, spendable - assigned);
            } else {
                share = (int) Math.round(spendable * (weights[i] / sum));
                assigned += share;
            }
            caps.put(pos, share);
            spent.put(pos, 0);
        }
    }

    public int remaining(String pos) {
        if (pos == null) return 0;
        Integer c = caps.get(pos);
        Integer s = spent.get(pos);
        if (c == null) c = 0;
        if (s == null) s = 0;
        return c - s;
    }

    public boolean canSpend(String pos, int amount, boolean criticalStarter) {
        if (amount <= 0) return true;
        int rem = remaining(pos);
        if (amount <= rem) return true;
        if (criticalStarter && amount <= rem + teamPoolSlack()) return true;
        return false;
    }

    private int teamPoolSlack() {
        int slack = 0;
        for (String pos : NilMoney.POSITIONS) {
            slack += Math.max(0, remaining(pos));
        }
        return slack;
    }

    public void recordSpend(String pos, int amount) {
        if (pos == null || amount <= 0) return;
        Integer s = spent.get(pos);
        if (s == null) s = 0;
        spent.put(pos, s + amount);
        // If over pos cap, steal from positions with surplus remaining
        int over = spent.get(pos) - caps.get(pos);
        if (over > 0) {
            for (String other : NilMoney.POSITIONS) {
                if (other.equals(pos)) continue;
                int rem = remaining(other);
                if (rem <= 0) continue;
                int take = Math.min(rem, over);
                caps.put(other, caps.get(other) - take);
                over -= take;
                if (over <= 0) break;
            }
            caps.put(pos, spent.get(pos));
        }
    }

    /** Move unused cap toward neediest positions. */
    public void rebalanceToNeeds() {
        int unused = 0;
        for (String pos : NilMoney.POSITIONS) {
            int rem = remaining(pos);
            if (rem > 0) {
                int have = team.getPositionList(pos).size();
                int sug = NilMoney.sugFor(pos);
                if (have >= sug) {
                    int release = rem / 2;
                    caps.put(pos, caps.get(pos) - release);
                    unused += release;
                }
            }
        }
        if (unused <= 0) return;
        // Give to biggest deficits
        while (unused > 0) {
            String best = null;
            int bestDef = -1;
            for (String pos : NilMoney.POSITIONS) {
                int have = team.getPositionList(pos).size();
                int sug = NilMoney.sugFor(pos);
                int def = sug - have;
                if (def > bestDef) {
                    bestDef = def;
                    best = pos;
                }
            }
            if (best == null || bestDef <= 0) break;
            int give = Math.min(unused, Math.max(1000, unused / 4));
            caps.put(best, caps.get(best) + give);
            unused -= give;
        }
    }

    public double needWeight(String pos) {
        int have = team.getPositionList(pos) != null ? team.getPositionList(pos).size() : 0;
        int sug = NilMoney.sugFor(pos);
        return Math.max(0.15, (sug - have) + 0.5);
    }
}
