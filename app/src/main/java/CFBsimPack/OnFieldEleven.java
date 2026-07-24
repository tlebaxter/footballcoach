package CFBsimPack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Resolves the 11 players on offense or defense from depth + system/philosophy/personnel.
 * Slot fill ranks by {@link PositionOvr#ovr} across the roster (cross-position eligible).
 */
public final class OnFieldEleven {

    public final List<Player> players = new ArrayList<>(11);
    public final List<RoleTag> roles = new ArrayList<>(11);

    public static OnFieldEleven forDefense(Team team) {
        OnFieldEleven eleven = new OnFieldEleven();
        DefensiveSystem sys = team.defSystem != null ? team.defSystem : DefensiveSystem.BASE_4_3;
        Set<Player> used = new HashSet<>();
        for (RoleTag slot : sys.slots) {
            Player p = pickBestForRole(team, slot.preferredGroup(), used, 35);
            if (p == null) {
                p = pickBestForRole(team, PositionGroup.LB, used, 30);
                if (p == null) p = pickBestForRole(team, PositionGroup.DL, used, 30);
                if (p == null) p = pickBestForRole(team, PositionGroup.EDGE, used, 30);
                if (p == null) p = pickBestForRole(team, PositionGroup.CB, used, 30);
                if (p == null) p = pickBestForRole(team, PositionGroup.S, used, 30);
            }
            if (p != null) {
                used.add(p);
                eleven.players.add(p);
                eleven.roles.add(slot);
            }
        }
        return eleven;
    }

    /** Philosophy default package (AI / legacy). */
    public static OnFieldEleven forOffense(Team team) {
        OffensivePhilosophy phil = team.offPhilosophy != null ? team.offPhilosophy : OffensivePhilosophy.MULTIPLE;
        return forOffense(team, phil.defaultPersonnel);
    }

    /** Concept personnel string ("11", "21", …) drives package. */
    public static OnFieldEleven forOffense(Team team, String personnel) {
        OnFieldEleven eleven = new OnFieldEleven();
        Set<Player> used = new HashSet<>();
        String pers = personnel != null ? personnel : "11";

        add(eleven, used, team, PositionGroup.QB, RoleTag.QB, 1);
        add(eleven, used, team, PositionGroup.OL, RoleTag.OL, 5);

        int wrTarget = 3;
        int rbTarget = 1;
        int teTarget = 1;
        int fbTarget = 0;

        if ("10".equals(pers)) {
            wrTarget = 4;
            rbTarget = 1;
            teTarget = 0;
            fbTarget = 0;
        } else if ("12".equals(pers)) {
            wrTarget = 2;
            teTarget = 2;
            rbTarget = 1;
            fbTarget = 0;
        } else if ("21".equals(pers)) {
            wrTarget = 2;
            rbTarget = 1;
            fbTarget = 1;
            teTarget = 1;
        } else if ("20".equals(pers)) {
            wrTarget = 2;
            rbTarget = 2;
            fbTarget = 1;
            teTarget = 0;
        } else if ("11".equals(pers)) {
            wrTarget = 3;
            rbTarget = 1;
            teTarget = 1;
            fbTarget = 0;
        }

        add(eleven, used, team, PositionGroup.RB, RoleTag.RB, rbTarget);
        add(eleven, used, team, PositionGroup.FB, RoleTag.FB, fbTarget);
        add(eleven, used, team, PositionGroup.TE, RoleTag.TE, teTarget);
        add(eleven, used, team, PositionGroup.WR, RoleTag.WR, wrTarget);

        while (eleven.players.size() < 11) {
            Player p = pickBestForRole(team, PositionGroup.WR, used, 35);
            if (p == null) p = pickBestForRole(team, PositionGroup.TE, used, 35);
            if (p == null) p = pickBestForRole(team, PositionGroup.RB, used, 35);
            if (p == null) p = pickBestForRole(team, PositionGroup.FB, used, 35);
            if (p == null) break;
            used.add(p);
            eleven.players.add(p);
            eleven.roles.add(RoleTag.WR);
        }
        return eleven;
    }

    private static void add(OnFieldEleven eleven, Set<Player> used, Team team,
                            PositionGroup g, RoleTag role, int count) {
        for (int i = 0; i < count; i++) {
            Player p = pickBestForRole(team, g, used, 35);
            if (p == null) return;
            used.add(p);
            eleven.players.add(p);
            eleven.roles.add(role);
        }
    }

    /**
     * Prefer depth-chart order for primary group; allow cross-pos if ovr(pos) clears cutoff.
     */
    static Player pickBestForRole(Team team, PositionGroup g, Set<Player> used, int minOvr) {
        if (team == null || g == null) return null;
        // 1) Primary depth chart order
        List<? extends Player> primary = team.playersForGroup(g);
        if (primary != null) {
            for (Player p : primary) {
                if (p != null && !p.isInjured && !used.contains(p)) return p;
            }
        }
        // 2) Cross-position: best ovr(g) on roster above cutoff
        Player best = null;
        int bestOvr = minOvr - 1;
        for (Player p : team.getAllPlayers()) {
            if (p == null || p.isInjured || used.contains(p)) continue;
            int o = PositionOvr.ovr(p, g);
            if (o > bestOvr) {
                bestOvr = o;
                best = p;
            }
        }
        return best;
    }

    public int avgAttr(AttrGetter getter) {
        if (players.isEmpty()) return 60;
        int sum = 0;
        int n = 0;
        for (Player p : players) {
            sum += getter.get(p);
            n++;
        }
        return n == 0 ? 60 : sum / n;
    }

    /** Weighted best-on-field composite (0–100). */
    public int weightedComposite(String compositeName, PositionGroup[] preferGroups, double[] mainWeights) {
        List<PlayerScore> scores = new ArrayList<>();
        for (Player p : players) {
            if (p == null || p.ratings == null) continue;
            double c = CompositeWeights.composite(p.ratings, compositeName);
            int ovrBoost = 0;
            if (preferGroups != null) {
                for (PositionGroup g : preferGroups) {
                    ovrBoost = Math.max(ovrBoost, PositionOvr.ovr(p, g));
                }
            }
            double val = (ovrBoost / 100.0 + c) / 2.0;
            scores.add(new PlayerScore(p, val));
        }
        scores.sort(Comparator.comparingDouble((PlayerScore s) -> s.val).reversed());
        double sum = 0;
        double wSum = 0;
        for (int i = 0; i < scores.size(); i++) {
            double w = (mainWeights != null && i < mainWeights.length) ? mainWeights[i] : 1.0;
            sum += scores.get(i).val * w;
            wSum += w;
        }
        if (wSum <= 0) return 60;
        return (int) Math.round(100.0 * sum / wSum);
    }

    public int passRushComposite() {
        return weightedComposite("passRushing",
                new PositionGroup[]{PositionGroup.EDGE, PositionGroup.DL, PositionGroup.LB},
                new double[]{5, 4, 3, 2, 2, 1});
    }

    public int runStopComposite() {
        return weightedComposite("runStopping",
                new PositionGroup[]{PositionGroup.DL, PositionGroup.EDGE, PositionGroup.LB},
                new double[]{5, 4, 3, 2, 2, 1});
    }

    public int coverageComposite() {
        return weightedComposite("passCoverage",
                new PositionGroup[]{PositionGroup.CB, PositionGroup.S, PositionGroup.LB},
                new double[]{5, 4, 3, 2, 1, 1});
    }

    public int olPassComposite() {
        return weightedComposite("passBlocking",
                new PositionGroup[]{PositionGroup.OL, PositionGroup.TE, PositionGroup.FB},
                new double[]{5, 4, 3, 2, 1, 0.5});
    }

    public int olRushComposite() {
        return weightedComposite("runBlocking",
                new PositionGroup[]{PositionGroup.OL, PositionGroup.FB, PositionGroup.TE},
                new double[]{5, 4, 3, 2, 1, 0.5});
    }

    public Player firstOf(PositionGroup group) {
        if (group == null) return null;
        for (Player p : players) {
            if (p != null && PositionGroup.fromToken(p.position) == group) return p;
        }
        return null;
    }

    public Player firstWithRole(RoleTag role) {
        for (int i = 0; i < roles.size(); i++) {
            if (roles.get(i) == role) return players.get(i);
        }
        return null;
    }

    public List<Player> receivers() {
        List<Player> out = new ArrayList<>();
        for (Player p : players) {
            if (p != null && PositionGroup.fromToken(p.position) == PositionGroup.WR) {
                out.add(p);
            }
        }
        return out;
    }

    public interface AttrGetter {
        int get(Player p);
    }

    private static final class PlayerScore {
        final Player p;
        final double val;
        PlayerScore(Player p, double val) {
            this.p = p;
            this.val = val;
        }
    }
}
