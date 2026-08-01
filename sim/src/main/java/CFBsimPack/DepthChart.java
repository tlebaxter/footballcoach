package CFBsimPack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Applies offensive philosophy / defensive system to depth order and role tags.
 */
public final class DepthChart {

    private DepthChart() {}

    public static void applySystems(Team team) {
        if (team == null) return;
        applyOffense(team);
        applyDefense(team);
    }

    private static void applyOffense(Team team) {
        OffensivePhilosophy phil = team.offPhilosophy != null ? team.offPhilosophy : OffensivePhilosophy.MULTIPLE;
        sortUnlocked(team.teamQBs, byOvr());
        sortUnlocked(team.teamRBs, byOvr());
        sortUnlocked(team.teamWRs, byOvr());
        sortUnlocked(team.teamOLs, byOvr());
        if (phil.wantsFullback()) {
            sortUnlocked(team.teamFBs, byFbFit());
        } else {
            sortUnlocked(team.teamFBs, byOvr());
        }
        if (phil.wantsTightEnd()) {
            sortUnlocked(team.teamTEs, byTeFit(phil));
        } else {
            sortUnlocked(team.teamTEs, byOvr());
        }
        if (phil == OffensivePhilosophy.AIR_RAID || phil == OffensivePhilosophy.RUN_AND_SHOOT) {
            sortUnlocked(team.teamWRs, byWrPassFit());
        }
        if (phil == OffensivePhilosophy.POWER_RUN || phil == OffensivePhilosophy.SMASHMOUTH) {
            sortUnlocked(team.teamOLs, byOlRunFit());
            sortUnlocked(team.teamRBs, byRbPowerFit());
        }
    }

    private static void applyDefense(Team team) {
        DefensiveSystem sys = team.defSystem != null ? team.defSystem : DefensiveSystem.BASE_4_3;
        sortUnlocked(team.teamEDGEs, byEdgeFit());
        sortUnlocked(team.teamDLs, byDlFit(sys));
        sortUnlocked(team.teamLBs, byLbFit(sys));
        sortUnlocked(team.teamCBs, byOvr());
        sortUnlocked(team.teamSs, byOvr());

        // Assign role tags to top depth by system slots
        Map<PositionGroup, Integer> idx = new HashMap<>();
        for (RoleTag slot : sys.slots) {
            PositionGroup g = slot.preferredGroup();
            int i = idx.containsKey(g) ? idx.get(g) : 0;
            Player p = pickAt(team, g, i);
            if (p != null) {
                setRole(p, slot);
            }
            idx.put(g, i + 1);
        }
    }

    private static Player pickAt(Team team, PositionGroup g, int i) {
        List<? extends Player> list = team.playersForGroup(g);
        if (list == null || i < 0 || i >= list.size()) return null;
        return list.get(i);
    }

    private static void setRole(Player p, RoleTag tag) {
        if (p == null) return;
        PositionGroup g = PositionGroup.fromToken(p.position);
        if (g == PositionGroup.EDGE) p.roleTag = tag == RoleTag.EDGE ? RoleTag.EDGE : tag;
        else if (g == PositionGroup.DL || g == PositionGroup.LB) p.roleTag = tag;
    }

    private static void sortUnlocked(ArrayList<Player> list, Comparator<? super Player> fit) {
        if (list == null || list.size() < 2) return;
        ArrayList<Player> unlocked = new ArrayList<>();
        boolean[] wasLocked = new boolean[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Player p = list.get(i);
            wasLocked[i] = p.depthLocked;
            if (!p.depthLocked) unlocked.add(p);
        }
        Collections.sort(unlocked, fit);
        // Rebuild preserving locked slots
        ArrayList<Player> result = new ArrayList<>(list.size());
        int u = 0;
        for (int i = 0; i < list.size(); i++) {
            if (wasLocked[i]) {
                result.add(list.get(i));
            } else if (u < unlocked.size()) {
                result.add(unlocked.get(u++));
            }
        }
        while (u < unlocked.size()) result.add(unlocked.get(u++));
        list.clear();
        list.addAll(result);
    }

    private static Comparator<Player> byOvr() {
        return (a, b) -> Integer.compare(b.ratOvr, a.ratOvr);
    }

    private static Comparator<Player> byFbFit() {
        return (a, b) -> Integer.compare(
                b.ratings.rbk * 2 + b.ratings.stre,
                a.ratings.rbk * 2 + a.ratings.stre);
    }

    private static Comparator<Player> byTeFit(OffensivePhilosophy phil) {
        if (phil == OffensivePhilosophy.AIR_RAID || phil == OffensivePhilosophy.WEST_COAST) {
            return (a, b) -> Integer.compare(
                    b.ratings.hnd * 2 + b.ratings.spd,
                    a.ratings.hnd * 2 + a.ratings.spd);
        }
        return (a, b) -> Integer.compare(
                b.ratings.rbk * 2 + b.ratings.hnd,
                a.ratings.rbk * 2 + a.ratings.hnd);
    }

    private static Comparator<Player> byWrPassFit() {
        return (a, b) -> Integer.compare(
                b.ratings.hnd + b.ratings.spd + b.ratings.elu,
                a.ratings.hnd + a.ratings.spd + a.ratings.elu);
    }

    private static Comparator<Player> byOlRunFit() {
        return (a, b) -> Integer.compare(
                b.ratings.stre + b.ratings.rbk * 2,
                a.ratings.stre + a.ratings.rbk * 2);
    }

    private static Comparator<Player> byRbPowerFit() {
        return (a, b) -> Integer.compare(
                b.ratings.stre * 2 + b.ratings.spd,
                a.ratings.stre * 2 + a.ratings.spd);
    }

    private static Comparator<Player> byEdgeFit() {
        return (a, b) -> Integer.compare(
                b.ratings.prs * 2 + b.ratings.stre,
                a.ratings.prs * 2 + a.ratings.stre);
    }

    private static Comparator<Player> byDlFit(DefensiveSystem sys) {
        if (sys == DefensiveSystem.BASE_3_4 || sys == DefensiveSystem.FIVE_TWO || sys == DefensiveSystem.BEAR_46) {
            return (a, b) -> Integer.compare(
                    b.ratings.stre * 3 + b.ratings.rns,
                    a.ratings.stre * 3 + a.ratings.rns);
        }
        return (a, b) -> Integer.compare(b.ratOvr, a.ratOvr);
    }

    private static Comparator<Player> byLbFit(DefensiveSystem sys) {
        if (sys == DefensiveSystem.NICKEL || sys == DefensiveSystem.DIME || sys == DefensiveSystem.FOUR_TWO_FIVE) {
            return (a, b) -> Integer.compare(
                    b.ratings.pcv * 2 + b.ratings.rns,
                    a.ratings.pcv * 2 + a.ratings.rns);
        }
        return (a, b) -> Integer.compare(
                b.ratings.rns * 2 + b.ratings.stre,
                a.ratings.rns * 2 + a.ratings.stre);
    }
}
