package CFBsimPack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Resolves the 11 players on offense or defense from depth + system/philosophy.
 */
public final class OnFieldEleven {

    public final List<Player> players = new ArrayList<>(11);
    public final List<RoleTag> roles = new ArrayList<>(11);

    public static OnFieldEleven forDefense(Team team) {
        OnFieldEleven eleven = new OnFieldEleven();
        DefensiveSystem sys = team.defSystem != null ? team.defSystem : DefensiveSystem.BASE_4_3;
        Set<Player> used = new HashSet<>();
        for (RoleTag slot : sys.slots) {
            Player p = pickUnused(team, slot.preferredGroup(), used);
            if (p == null) {
                // Fallback across front / secondary
                p = pickUnused(team, PositionGroup.LB, used);
                if (p == null) p = pickUnused(team, PositionGroup.DL, used);
                if (p == null) p = pickUnused(team, PositionGroup.EDGE, used);
                if (p == null) p = pickUnused(team, PositionGroup.CB, used);
                if (p == null) p = pickUnused(team, PositionGroup.S, used);
            }
            if (p != null) {
                used.add(p);
                eleven.players.add(p);
                eleven.roles.add(slot);
            }
        }
        return eleven;
    }

    public static OnFieldEleven forOffense(Team team) {
        OnFieldEleven eleven = new OnFieldEleven();
        OffensivePhilosophy phil = team.offPhilosophy != null ? team.offPhilosophy : OffensivePhilosophy.MULTIPLE;
        Set<Player> used = new HashSet<>();

        add(eleven, used, team, PositionGroup.QB, RoleTag.QB, 1);
        add(eleven, used, team, PositionGroup.OL, RoleTag.OL, 5);

        boolean fb = phil.wantsFullback();
        boolean te = phil.wantsTightEnd();
        int wrTarget = 3;
        int rbTarget = 2;
        int teTarget = te ? 1 : 0;
        int fbTarget = fb ? 1 : 0;

        if ("10".equals(phil.defaultPersonnel)) {
            wrTarget = 4;
            rbTarget = 1;
            teTarget = 0;
            fbTarget = 0;
        } else if ("12".equals(phil.defaultPersonnel)) {
            wrTarget = 2;
            teTarget = 2;
            rbTarget = 1;
            fbTarget = 0;
        } else if ("21".equals(phil.defaultPersonnel)) {
            wrTarget = 2;
            rbTarget = 1;
            fbTarget = 1;
            teTarget = 1;
        } else if ("20".equals(phil.defaultPersonnel)) {
            wrTarget = 2;
            rbTarget = 2;
            fbTarget = 1;
            teTarget = 0;
        }

        add(eleven, used, team, PositionGroup.RB, RoleTag.RB, rbTarget);
        add(eleven, used, team, PositionGroup.FB, RoleTag.FB, fbTarget);
        add(eleven, used, team, PositionGroup.TE, RoleTag.TE, teTarget);
        add(eleven, used, team, PositionGroup.WR, RoleTag.WR, wrTarget);

        // Fill to 11 with WRs / TE / RB
        while (eleven.players.size() < 11) {
            Player p = pickUnused(team, PositionGroup.WR, used);
            if (p == null) p = pickUnused(team, PositionGroup.TE, used);
            if (p == null) p = pickUnused(team, PositionGroup.RB, used);
            if (p == null) p = pickUnused(team, PositionGroup.FB, used);
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
            Player p = pickUnused(team, g, used);
            if (p == null) return;
            used.add(p);
            eleven.players.add(p);
            eleven.roles.add(role);
        }
    }

    private static Player pickUnused(Team team, PositionGroup g, Set<Player> used) {
        List<? extends Player> list = team.playersForGroup(g);
        if (list == null) return null;
        for (Player p : list) {
            if (p != null && !p.isInjured && !used.contains(p)) return p;
        }
        return null;
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

    public int passRushComposite() {
        int sum = 0;
        int n = 0;
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            RoleTag r = roles.get(i);
            if (p instanceof PlayerEDGE) {
                sum += (((PlayerEDGE) p).ratPass * 2 + ((PlayerEDGE) p).ratPow) / 3;
                n++;
            } else if (p instanceof PlayerDL) {
                sum += (((PlayerDL) p).ratPass + ((PlayerDL) p).ratPow) / 2;
                n++;
            } else if (p instanceof PlayerLB && (r == RoleTag.EDGE || r == RoleTag.OLB || r == RoleTag.SAM)) {
                sum += (((PlayerLB) p).ratRush + ((PlayerLB) p).ratPow) / 2;
                n++;
            }
        }
        return n == 0 ? 60 : sum / n;
    }

    public int runStopComposite() {
        int sum = 0;
        int n = 0;
        for (Player p : players) {
            if (p instanceof PlayerDL) {
                sum += (((PlayerDL) p).ratRush * 2 + ((PlayerDL) p).ratPow) / 3;
                n++;
            } else if (p instanceof PlayerEDGE) {
                sum += (((PlayerEDGE) p).ratRush + ((PlayerEDGE) p).ratPow) / 2;
                n++;
            } else if (p instanceof PlayerLB) {
                sum += (((PlayerLB) p).ratRush * 2 + ((PlayerLB) p).ratPow) / 3;
                n++;
            }
        }
        return n == 0 ? 60 : sum / n;
    }

    public int coverageComposite() {
        int sum = 0;
        int n = 0;
        for (Player p : players) {
            if (p instanceof PlayerCB) {
                sum += ((PlayerCB) p).ratCBCov;
                n++;
            } else if (p instanceof PlayerS) {
                sum += ((PlayerS) p).ratSCov;
                n++;
            } else if (p instanceof PlayerLB) {
                sum += ((PlayerLB) p).ratCov;
                n++;
            }
        }
        return n == 0 ? 60 : sum / n;
    }

    public int olPassComposite() {
        int sum = 0;
        int n = 0;
        for (Player p : players) {
            if (p instanceof PlayerOL) {
                sum += (((PlayerOL) p).ratOLPow + ((PlayerOL) p).ratOLBkP) / 2;
                n++;
            }
        }
        return n == 0 ? 60 : sum / n;
    }

    public int olRushComposite() {
        int sum = 0;
        int n = 0;
        for (Player p : players) {
            if (p instanceof PlayerOL) {
                sum += (((PlayerOL) p).ratOLPow + ((PlayerOL) p).ratOLBkR) / 2;
                n++;
            } else if (p instanceof PlayerFB) {
                sum += ((PlayerFB) p).ratBlock;
                n++;
            } else if (p instanceof PlayerTE) {
                sum += ((PlayerTE) p).ratBlock;
                n++;
            }
        }
        return n == 0 ? 60 : sum / n;
    }

    public Player firstOf(Class<? extends Player> cls) {
        for (Player p : players) {
            if (cls.isInstance(p)) return p;
        }
        return null;
    }

    public List<PlayerWR> receivers() {
        List<PlayerWR> out = new ArrayList<>();
        for (Player p : players) {
            if (p instanceof PlayerWR) out.add((PlayerWR) p);
        }
        return out;
    }

    public interface AttrGetter {
        int get(Player p);
    }
}
