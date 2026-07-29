package CFBsimPack.engine.snap;

import CFBsimPack.OnFieldEleven;
import CFBsimPack.Player;
import CFBsimPack.PositionGroup;
import CFBsimPack.RoleTag;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Maps on-field elevens to {@link OffSlot}/{@link DefSlot} using RoleTag + OL order.
 */
public final class SlotMapper {

    private SlotMapper() {}

    public static final class MappedPlayer {
        public final Player player;
        public final OffSlot offSlot;
        public final DefSlot defSlot;

        MappedPlayer(Player player, OffSlot offSlot, DefSlot defSlot) {
            this.player = player;
            this.offSlot = offSlot;
            this.defSlot = defSlot;
        }
    }

    public static Map<OffSlot, Player> mapOffense(OnFieldEleven off) {
        Map<OffSlot, Player> map = new EnumMap<>(OffSlot.class);
        if (off == null) return map;
        int olIdx = 0;
        int wrIdx = 0;
        int teIdx = 0;
        OffSlot[] olOrder = {OffSlot.LT, OffSlot.LG, OffSlot.C, OffSlot.RG, OffSlot.RT};
        OffSlot[] wrOrder = {OffSlot.WR_X, OffSlot.WR_Z, OffSlot.WR_SLOT, OffSlot.WR_H};
        for (int i = 0; i < off.players.size(); i++) {
            Player p = off.players.get(i);
            RoleTag role = i < off.roles.size() ? off.roles.get(i) : null;
            OffSlot slot = offenseSlot(p, role, olIdx, wrIdx, teIdx);
            if (slot == null) continue;
            if (slot == OffSlot.LT || slot == OffSlot.LG || slot == OffSlot.C
                    || slot == OffSlot.RG || slot == OffSlot.RT) {
                if (olIdx < olOrder.length) {
                    slot = olOrder[olIdx++];
                }
            } else if (slot == OffSlot.WR_X || slot == OffSlot.WR_Z
                    || slot == OffSlot.WR_SLOT || slot == OffSlot.WR_H) {
                if (wrIdx < wrOrder.length) {
                    slot = wrOrder[wrIdx++];
                }
            } else if (slot == OffSlot.TE_L || slot == OffSlot.TE_R) {
                slot = teIdx++ == 0 ? OffSlot.TE_L : OffSlot.TE_R;
            }
            if (!map.containsKey(slot)) {
                map.put(slot, p);
            }
        }
        return map;
    }

    public static Map<DefSlot, Player> mapDefense(OnFieldEleven def) {
        Map<DefSlot, Player> map = new EnumMap<>(DefSlot.class);
        if (def == null) return map;
        int edgeIdx = 0;
        int cbIdx = 0;
        int dtIdx = 0;
        for (int i = 0; i < def.players.size(); i++) {
            Player p = def.players.get(i);
            RoleTag role = i < def.roles.size() ? def.roles.get(i) : null;
            DefSlot slot = defenseSlot(p, role, edgeIdx, cbIdx, dtIdx);
            if (slot == null) continue;
            if (slot == DefSlot.EDGE_L || slot == DefSlot.EDGE_R) {
                slot = edgeIdx++ == 0 ? DefSlot.EDGE_L : DefSlot.EDGE_R;
            } else if (slot == DefSlot.CB_L || slot == DefSlot.CB_R) {
                slot = cbIdx++ == 0 ? DefSlot.CB_L : DefSlot.CB_R;
            } else if (slot == DefSlot.DT || slot == DefSlot.NT) {
                if (role == RoleTag.NT) {
                    slot = DefSlot.NT;
                } else {
                    slot = dtIdx++ == 0 ? DefSlot.DT : DefSlot.NT;
                }
            }
            if (!map.containsKey(slot)) {
                map.put(slot, p);
            }
        }
        return map;
    }

    public static List<Player> rushers(Map<DefSlot, Player> defMap, boolean includeBlitzLb) {
        List<Player> out = new ArrayList<>();
        addIfPresent(out, defMap, DefSlot.EDGE_L);
        addIfPresent(out, defMap, DefSlot.EDGE_R);
        addIfPresent(out, defMap, DefSlot.LE);
        addIfPresent(out, defMap, DefSlot.RE);
        addIfPresent(out, defMap, DefSlot.DT);
        addIfPresent(out, defMap, DefSlot.NT);
        if (includeBlitzLb) {
            addIfPresent(out, defMap, DefSlot.SAM);
            addIfPresent(out, defMap, DefSlot.WILL);
            addIfPresent(out, defMap, DefSlot.MIKE);
            addIfPresent(out, defMap, DefSlot.LB);
        }
        return out;
    }

    public static List<DefSlot> rusherSlots(Map<DefSlot, Player> defMap, boolean includeBlitzLb) {
        List<DefSlot> out = new ArrayList<>();
        for (DefSlot s : new DefSlot[]{
                DefSlot.EDGE_L, DefSlot.EDGE_R, DefSlot.LE, DefSlot.RE,
                DefSlot.DT, DefSlot.NT
        }) {
            if (defMap.containsKey(s)) out.add(s);
        }
        if (includeBlitzLb) {
            for (DefSlot s : new DefSlot[]{DefSlot.SAM, DefSlot.WILL, DefSlot.MIKE, DefSlot.LB}) {
                if (defMap.containsKey(s)) out.add(s);
            }
        }
        return out;
    }

    private static void addIfPresent(List<Player> out, Map<DefSlot, Player> map, DefSlot slot) {
        Player p = map.get(slot);
        if (p != null) out.add(p);
    }

    private static OffSlot offenseSlot(Player p, RoleTag role, int olIdx, int wrIdx, int teIdx) {
        if (role == RoleTag.QB) return OffSlot.QB;
        if (role == RoleTag.RB) return OffSlot.RB;
        if (role == RoleTag.FB) return OffSlot.FB;
        if (role == RoleTag.TE || role == RoleTag.LS) return OffSlot.TE_L;
        if (role == RoleTag.OL) return OffSlot.LT;
        if (role == RoleTag.SLOT) return OffSlot.WR_SLOT;
        if (role == RoleTag.WR || role == RoleTag.PR || role == RoleTag.KR || role == RoleTag.GUNNER) {
            return OffSlot.WR_X;
        }
        if (p == null) return null;
        PositionGroup g = PositionGroup.fromToken(p.position);
        if (g == PositionGroup.QB) return OffSlot.QB;
        if (g == PositionGroup.RB) return OffSlot.RB;
        if (g == PositionGroup.FB) return OffSlot.FB;
        if (g == PositionGroup.TE) return OffSlot.TE_L;
        if (g == PositionGroup.OL) return OffSlot.LT;
        if (g == PositionGroup.WR) return OffSlot.WR_X;
        return null;
    }

    private static DefSlot defenseSlot(Player p, RoleTag role, int edgeIdx, int cbIdx, int dtIdx) {
        if (role == RoleTag.EDGE) return DefSlot.EDGE_L;
        if (role == RoleTag.DE) return edgeIdx == 0 ? DefSlot.LE : DefSlot.RE;
        if (role == RoleTag.NT) return DefSlot.NT;
        if (role == RoleTag.DT) return DefSlot.DT;
        if (role == RoleTag.WILL) return DefSlot.WILL;
        if (role == RoleTag.MIKE) return DefSlot.MIKE;
        if (role == RoleTag.SAM) return DefSlot.SAM;
        if (role == RoleTag.ILB || role == RoleTag.OLB || role == RoleTag.LB) return DefSlot.LB;
        if (role == RoleTag.NB) return DefSlot.NB;
        if (role == RoleTag.CB) return DefSlot.CB_L;
        if (role == RoleTag.FS) return DefSlot.FS;
        if (role == RoleTag.SS) return DefSlot.SS;
        if (role == RoleTag.S) return DefSlot.S;
        if (p == null) return null;
        PositionGroup g = PositionGroup.fromToken(p.position);
        if (g == PositionGroup.EDGE) return DefSlot.EDGE_L;
        if (g == PositionGroup.DL) return DefSlot.DT;
        if (g == PositionGroup.LB) return DefSlot.LB;
        if (g == PositionGroup.CB) return DefSlot.CB_L;
        if (g == PositionGroup.S) return DefSlot.FS;
        return null;
    }
}
