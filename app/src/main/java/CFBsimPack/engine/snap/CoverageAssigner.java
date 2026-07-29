package CFBsimPack.engine.snap;

import CFBsimPack.OnFieldEleven;
import CFBsimPack.Player;
import CFBsimPack.engine.CoverageCall;
import CFBsimPack.engine.playdef.RouteAssignment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds man/zone coverage assignments from {@link CoverageCall}.
 */
public final class CoverageAssigner {

    public List<CoverageAssignment> assign(
            OnFieldEleven def,
            CoverageCall cov,
            List<RouteAssignment> routes,
            Map<OffSlot, Player> offMap
    ) {
        Map<DefSlot, Player> defMap = SlotMapper.mapDefense(def);
        List<CoverageAssignment> out = new ArrayList<>();
        if (cov == null) cov = CoverageCall.COVER_3;

        boolean manHeavy = cov == CoverageCall.MAN || cov == CoverageCall.COVER_0
                || cov == CoverageCall.COVER_1 || cov == CoverageCall.PRESS;
        boolean twoHigh = cov == CoverageCall.COVER_2 || cov == CoverageCall.COVER_4;
        boolean spy = cov == CoverageCall.SPY;

        List<OffSlot> routeSlots = new ArrayList<>();
        if (routes != null) {
            for (RouteAssignment r : routes) {
                if (r != null && r.slot != null && offMap != null && offMap.containsKey(r.slot)) {
                    routeSlots.add(r.slot);
                }
            }
        }
        if (routeSlots.isEmpty() && offMap != null) {
            for (OffSlot s : new OffSlot[]{OffSlot.WR_X, OffSlot.WR_Z, OffSlot.WR_SLOT, OffSlot.TE_L, OffSlot.RB}) {
                if (offMap.containsKey(s)) routeSlots.add(s);
            }
        }

        if (manHeavy) {
            List<DefSlot> cbs = new ArrayList<>();
            for (DefSlot s : new DefSlot[]{DefSlot.CB_L, DefSlot.CB_R, DefSlot.NB, DefSlot.SS, DefSlot.FS, DefSlot.LB}) {
                if (defMap.containsKey(s)) cbs.add(s);
            }
            int i = 0;
            for (OffSlot tgt : routeSlots) {
                DefSlot ds = i < cbs.size() ? cbs.get(i++) : DefSlot.LB;
                Player d = defMap.get(ds);
                boolean deep = ds == DefSlot.FS || ds == DefSlot.SS;
                // Cover 1: FS is deep help, not man
                if (cov == CoverageCall.COVER_1 && ds == DefSlot.FS) {
                    out.add(new CoverageAssignment(d, ds, CoverageMode.ZONE, ZoneLandmark.DEEP_THIRD,
                            null, false, true));
                    continue;
                }
                out.add(new CoverageAssignment(d, ds, CoverageMode.MAN, null, tgt, false, deep));
            }
            if (cov == CoverageCall.COVER_1 && defMap.containsKey(DefSlot.FS)) {
                boolean hasFs = false;
                for (CoverageAssignment a : out) {
                    if (a.defenderSlot == DefSlot.FS) {
                        hasFs = true;
                        break;
                    }
                }
                if (!hasFs) {
                    out.add(new CoverageAssignment(defMap.get(DefSlot.FS), DefSlot.FS, CoverageMode.ZONE,
                            ZoneLandmark.DEEP_THIRD, null, false, true));
                }
            }
        } else {
            // Zone landmarks
            addZone(out, defMap, DefSlot.CB_L, ZoneLandmark.CURL, false);
            addZone(out, defMap, DefSlot.CB_R, ZoneLandmark.CURL, false);
            addZone(out, defMap, DefSlot.NB, ZoneLandmark.HOOK, false);
            addZone(out, defMap, DefSlot.WILL, ZoneLandmark.FLAT, false);
            addZone(out, defMap, DefSlot.SAM, ZoneLandmark.FLAT, false);
            addZone(out, defMap, DefSlot.MIKE, ZoneLandmark.HOOK, false);
            addZone(out, defMap, DefSlot.LB, ZoneLandmark.HOOK, false);
            if (twoHigh) {
                addZone(out, defMap, DefSlot.FS, ZoneLandmark.DEEP_HALF, true);
                addZone(out, defMap, DefSlot.SS, ZoneLandmark.DEEP_HALF, true);
            } else if (cov == CoverageCall.COVER_3) {
                addZone(out, defMap, DefSlot.FS, ZoneLandmark.DEEP_THIRD, true);
                addZone(out, defMap, DefSlot.SS, ZoneLandmark.CURL, false);
                addZone(out, defMap, DefSlot.CB_L, ZoneLandmark.DEEP_THIRD, true);
            } else if (cov == CoverageCall.OFF_COVERAGE) {
                addZone(out, defMap, DefSlot.FS, ZoneLandmark.DEEP_THIRD, true);
                addZone(out, defMap, DefSlot.SS, ZoneLandmark.DEEP_THIRD, true);
            } else {
                addZone(out, defMap, DefSlot.FS, ZoneLandmark.DEEP_THIRD, true);
                addZone(out, defMap, DefSlot.SS, ZoneLandmark.HOOK, false);
            }
            if (cov == CoverageCall.STACK_BOX) {
                addZone(out, defMap, DefSlot.SS, ZoneLandmark.HOLE, false);
            }
        }

        if (spy) {
            Player spyP = defMap.get(DefSlot.MIKE);
            if (spyP == null) spyP = defMap.get(DefSlot.LB);
            if (spyP == null) spyP = defMap.get(DefSlot.SS);
            DefSlot spySlot = defMap.containsKey(DefSlot.MIKE) ? DefSlot.MIKE
                    : (defMap.containsKey(DefSlot.LB) ? DefSlot.LB : DefSlot.SS);
            out.add(new CoverageAssignment(spyP, spySlot, CoverageMode.ZONE, ZoneLandmark.HOLE,
                    null, true, false));
        }
        return out;
    }

    private void addZone(List<CoverageAssignment> out, Map<DefSlot, Player> defMap,
                         DefSlot slot, ZoneLandmark landmark, boolean deep) {
        Player p = defMap.get(slot);
        if (p == null) return;
        for (CoverageAssignment a : out) {
            if (a.defenderSlot == slot) return;
        }
        out.add(new CoverageAssignment(p, slot, CoverageMode.ZONE, landmark, null, false, deep));
    }
}
