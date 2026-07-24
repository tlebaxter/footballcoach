package CFBsimPack.engine;

import CFBsimPack.OnFieldEleven;
import CFBsimPack.Player;
import CFBsimPack.PlayerRatings;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-game energy (100 = fresh). Multiplies ratings for on-field selection softness.
 */
public final class FatigueTracker {

    private final Map<Player, Integer> energy = new HashMap<>();

    public int energyOf(Player p) {
        if (p == null) return 100;
        Integer e = energy.get(p);
        return e != null ? e : 100;
    }

    public double factor(Player p) {
        int e = energyOf(p);
        if (e >= 70) return 1.0;
        if (e >= 40) return 0.92;
        return 0.82;
    }

    public void afterSnap(OnFieldEleven onField, OnFieldEleven offField) {
        if (onField != null) {
            for (Player p : onField.players) {
                drain(p, snapDrain(p));
            }
        }
        // Mild recovery for everyone else tracked
        for (Map.Entry<Player, Integer> e : energy.entrySet()) {
            Player p = e.getKey();
            if (onField != null && onField.players.contains(p)) continue;
            energy.put(p, Math.min(100, e.getValue() + 3));
        }
    }

    private void drain(Player p, int amount) {
        int cur = energyOf(p);
        energy.put(p, Math.max(15, cur - amount));
    }

    private int snapDrain(Player p) {
        int endu = p != null && p.ratings != null ? p.ratings.endu : 60;
        int base = 6;
        if ("RB".equals(p.position) || "WR".equals(p.position) || "EDGE".equals(p.position)) base = 8;
        if ("QB".equals(p.position) || "K".equals(p.position) || "P".equals(p.position)) base = 3;
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
