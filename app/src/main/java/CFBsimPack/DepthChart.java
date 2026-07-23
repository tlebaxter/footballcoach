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
        if (p instanceof PlayerEDGE) ((PlayerEDGE) p).roleTag = tag == RoleTag.EDGE ? RoleTag.EDGE : tag;
        else if (p instanceof PlayerDL) ((PlayerDL) p).roleTag = tag;
        else if (p instanceof PlayerLB) ((PlayerLB) p).roleTag = tag;
        else if (p instanceof PlayerCB) { /* CB/NB via position string ok */ }
    }

    private static <T extends Player> void sortUnlocked(ArrayList<T> list, Comparator<? super T> fit) {
        if (list == null || list.size() < 2) return;
        ArrayList<T> unlocked = new ArrayList<>();
        boolean[] wasLocked = new boolean[list.size()];
        for (int i = 0; i < list.size(); i++) {
            T p = list.get(i);
            wasLocked[i] = p.depthLocked;
            if (!p.depthLocked) unlocked.add(p);
        }
        Collections.sort(unlocked, fit);
        // Rebuild preserving locked slots
        ArrayList<T> result = new ArrayList<>(list.size());
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

    private static Comparator<PlayerFB> byFbFit() {
        return (a, b) -> Integer.compare(b.ratBlock * 2 + b.ratRushPow, a.ratBlock * 2 + a.ratRushPow);
    }

    private static Comparator<PlayerTE> byTeFit(OffensivePhilosophy phil) {
        if (phil == OffensivePhilosophy.AIR_RAID || phil == OffensivePhilosophy.WEST_COAST) {
            return (a, b) -> Integer.compare(b.ratRecCat * 2 + b.ratRecSpd, a.ratRecCat * 2 + a.ratRecSpd);
        }
        return (a, b) -> Integer.compare(b.ratBlock * 2 + b.ratRecCat, a.ratBlock * 2 + a.ratRecCat);
    }

    private static Comparator<PlayerWR> byWrPassFit() {
        return (a, b) -> Integer.compare(
                b.ratRecCat + b.ratRecSpd + b.ratRecEva,
                a.ratRecCat + a.ratRecSpd + a.ratRecEva);
    }

    private static Comparator<PlayerOL> byOlRunFit() {
        return (a, b) -> Integer.compare(b.ratOLPow + b.ratOLBkR * 2, a.ratOLPow + a.ratOLBkR * 2);
    }

    private static Comparator<PlayerRB> byRbPowerFit() {
        return (a, b) -> Integer.compare(b.ratRushPow * 2 + b.ratRushSpd, a.ratRushPow * 2 + a.ratRushSpd);
    }

    private static Comparator<PlayerEDGE> byEdgeFit() {
        return (a, b) -> Integer.compare(b.ratPass * 2 + b.ratPow, a.ratPass * 2 + a.ratPow);
    }

    private static Comparator<PlayerDL> byDlFit(DefensiveSystem sys) {
        if (sys == DefensiveSystem.BASE_3_4 || sys == DefensiveSystem.FIVE_TWO || sys == DefensiveSystem.BEAR_46) {
            return (a, b) -> Integer.compare(b.ratPow * 3 + b.ratRush, a.ratPow * 3 + a.ratRush);
        }
        return (a, b) -> Integer.compare(b.ratOvr, a.ratOvr);
    }

    private static Comparator<PlayerLB> byLbFit(DefensiveSystem sys) {
        if (sys == DefensiveSystem.NICKEL || sys == DefensiveSystem.DIME || sys == DefensiveSystem.FOUR_TWO_FIVE) {
            return (a, b) -> Integer.compare(b.ratCov * 2 + b.ratRush, a.ratCov * 2 + a.ratRush);
        }
        return (a, b) -> Integer.compare(b.ratRush * 2 + b.ratPow, a.ratRush * 2 + a.ratPow);
    }
}
