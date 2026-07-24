package CFBsimPack.engine;

import CFBsimPack.OnFieldEleven;
import CFBsimPack.Player;
import CFBsimPack.PlayerRatings;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Per-game energy (100 = fresh). Multiplies ratings for on-field selection softness.
 */
public final class FatigueTracker {

    /** Sit when below this if a fresher backup is available. */
    public static final int SIT_ENERGY = 40;
    /** Backup must be at least this fresh to replace a sitting starter. */
    public static final int FRESH_ENERGY = 55;

    private final Map<Player, Integer> energy = new HashMap<>();

    public int energyOf(Player p) {
        if (p == null) return 100;
        Integer e = energy.get(p);
        return e != null ? e : 100;
    }

    /** Test / setup helper: set absolute energy (clamped 15–100). */
    public void setEnergy(Player p, int value) {
        if (p == null) return;
        energy.put(p, Math.max(15, Math.min(100, value)));
    }

    public double factor(Player p) {
        int e = energyOf(p);
        if (e >= 70) return 1.0;
        if (e >= 40) return 0.92;
        return 0.82;
    }

    public void afterSnap(OnFieldEleven onField, OnFieldEleven offField) {
        Set<Player> played = new HashSet<>();
        drainEleven(onField, played);
        drainEleven(offField, played);
        for (Map.Entry<Player, Integer> e : energy.entrySet()) {
            Player p = e.getKey();
            if (played.contains(p)) continue;
            energy.put(p, Math.min(100, e.getValue() + 3));
        }
    }

    private void drainEleven(OnFieldEleven eleven, Set<Player> played) {
        if (eleven == null) return;
        for (Player p : eleven.players) {
            if (p == null) continue;
            played.add(p);
            drain(p, snapDrain(p));
        }
    }

    private void drain(Player p, int amount) {
        int cur = energyOf(p);
        energy.put(p, Math.max(15, cur - amount));
    }

    private int snapDrain(Player p) {
        int endu = p != null && p.ratings != null ? p.ratings.endu : 60;
        int base = 6;
        if (p != null && ("RB".equals(p.position) || "WR".equals(p.position) || "EDGE".equals(p.position))) {
            base = 8;
        }
        if (p != null && ("QB".equals(p.position) || "K".equals(p.position) || "P".equals(p.position))) {
            base = 3;
        }
        return Math.max(2, base - (endu - 50) / 20);
    }

    /** Apply fatigue to a working copy of ratings for resolve (does not mutate player). */
    public PlayerRatings fatiguedCopy(Player p) {
        if (p == null || p.ratings == null) return new PlayerRatings();
        PlayerRatings c = p.ratings.copy();
        double f = factor(p);
        if (f >= 0.99) return c;
        for (String k : PlayerRatings.KEYS) {
            c.set(k, (int) Math.round(c.get(k) * f));
        }
        return c;
    }
}
