package CFBsimPack.engine.playdef;

import CFBsimPack.Formation;
import CFBsimPack.engine.ConceptFamily;
import CFBsimPack.engine.DepthBand;
import CFBsimPack.engine.OffensePlay;
import CFBsimPack.engine.TargetBias;
import CFBsimPack.engine.snap.OffSlot;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds {@link PlayDefinition}s for the catalog (heuristic + signature overrides).
 */
public final class PlayDefinitions {

    private PlayDefinitions() {}

    public static PlayDefinition build(
            String id,
            String displayName,
            ConceptFamily family,
            OffensePlay offensePlay,
            Formation formation,
            String personnel,
            DepthBand depth,
            String blurb,
            TargetBias bias
    ) {
        boolean empty = "10".equals(personnel) || (formation == Formation.EMPTY);
        boolean pa = id != null && (id.contains("_pa_") || id.contains("boot"));
        ProtectionScheme protection = null;
        List<RouteAssignment> routes = new ArrayList<>();
        RunScheme run = null;
        RpoRules rpo = null;

        if (family == ConceptFamily.RUN || offensePlay == OffensePlay.RUN) {
            run = inferRun(id);
            protection = null;
        } else if (family == ConceptFamily.RPO) {
            protection = ProtectionScheme.infer(depth, empty, false);
            routes = inferRoutes(id, depth, bias, true);
            run = RunScheme.forTrack(RunTrack.INSIDE_ZONE);
            rpo = id != null && id.contains("zone_read") ? RpoRules.zoneRead() : RpoRules.defaults();
        } else if (offensePlay == OffensePlay.PASS || family == ConceptFamily.PASS) {
            if (id != null && id.contains("screen")) {
                protection = new ProtectionScheme(ProtectionType.BIG_ON_BIG, 0.85,
                        java.util.Collections.emptySet(), false);
                routes = screenRoutes();
            } else {
                ProtectionScheme inferred = ProtectionScheme.infer(depth, empty, pa);
                if (depth == DepthBand.DEEP && (id != null && id.contains("four_verts"))) {
                    protection = ProtectionScheme.maxProtect();
                } else if (depth == DepthBand.DEEP) {
                    // Deep shots develop longer — buy pocket via hotTime (old sackRiskMod ~1.1+)
                    protection = new ProtectionScheme(
                            inferred.type,
                            Math.max(inferred.hotTimeSec, 1.35),
                            inferred.maxProtectSlots,
                            inferred.allowDoubleTeams
                    );
                } else if (depth == DepthBand.SHORT) {
                    // Quick game: earlier hot (old sackRisk ~0.9)
                    protection = new ProtectionScheme(
                            inferred.type,
                            Math.min(inferred.hotTimeSec, 1.0),
                            inferred.maxProtectSlots,
                            inferred.allowDoubleTeams
                    );
                } else {
                    protection = inferred;
                }
                routes = inferRoutes(id, depth, bias, false);
            }
        }

        // Signature overrides
        if ("gun_qb_draw".equals(id)) {
            run = RunScheme.forTrack(RunTrack.QB_DRAW);
        } else if ("bone_option".equals(id)) {
            run = RunScheme.forTrack(RunTrack.OPTION);
        } else if (id != null && id.contains("outside_zone")) {
            run = RunScheme.forTrack(RunTrack.OUTSIDE_ZONE);
        } else if (id != null && (id.contains("jumbo_power") || id.contains("i_power"))) {
            run = RunScheme.forTrack(RunTrack.POWER);
        } else if (id != null && (id.contains("jumbo_iso") || id.contains("i_iso"))) {
            run = RunScheme.forTrack(RunTrack.ISO);
        } else if (id != null && id.contains("i_dive")) {
            run = RunScheme.forTrack(RunTrack.DIVE);
        } else if (id != null && id.contains("sweep")) {
            run = RunScheme.forTrack(RunTrack.SWEEP);
        } else if (id != null && id.contains("counter")) {
            run = RunScheme.forTrack(RunTrack.COUNTER);
        }

        if ("gun_mesh".equals(id) || "empty_mesh".equals(id)) {
            routes = meshRoutes();
        } else if ("gun_four_verts".equals(id) || "empty_four_verts".equals(id)) {
            routes = vertsRoutes();
        } else if ("gun_slants".equals(id) || "empty_all_hitch".equals(id)) {
            routes = slantRoutes();
        }

        return new PlayDefinition(
                id, displayName, blurb, family, offensePlay, formation, personnel, depth, bias,
                protection, routes, run, rpo
        );
    }

    private static RunScheme inferRun(String id) {
        if (id == null) return RunScheme.forTrack(RunTrack.INSIDE_ZONE);
        if (id.contains("qb_draw")) return RunScheme.forTrack(RunTrack.QB_DRAW);
        if (id.contains("option")) return RunScheme.forTrack(RunTrack.OPTION);
        if (id.contains("outside_zone") || id.contains("sweep")) {
            return id.contains("sweep") ? RunScheme.forTrack(RunTrack.SWEEP)
                    : RunScheme.forTrack(RunTrack.OUTSIDE_ZONE);
        }
        if (id.contains("power")) return RunScheme.forTrack(RunTrack.POWER);
        if (id.contains("counter")) return RunScheme.forTrack(RunTrack.COUNTER);
        if (id.contains("iso")) return RunScheme.forTrack(RunTrack.ISO);
        if (id.contains("dive")) return RunScheme.forTrack(RunTrack.DIVE);
        return RunScheme.forTrack(RunTrack.INSIDE_ZONE);
    }

    private static List<RouteAssignment> inferRoutes(String id, DepthBand depth, TargetBias bias, boolean rpo) {
        if (depth == DepthBand.DEEP) return vertsRoutes();
        if (depth == DepthBand.SHORT) {
            if (bias == TargetBias.RB) {
                List<RouteAssignment> r = new ArrayList<>();
                r.add(RouteAssignment.of(OffSlot.RB, RouteType.ANGLE, 1, true));
                r.add(RouteAssignment.of(OffSlot.WR_X, RouteType.HITCH, 2, false));
                r.add(RouteAssignment.of(OffSlot.TE_L, RouteType.FLAT, 3, true));
                return r;
            }
            return slantRoutes();
        }
        if (depth == DepthBand.MEDIUM) {
            List<RouteAssignment> r = new ArrayList<>();
            r.add(RouteAssignment.of(OffSlot.TE_L, RouteType.DIG, 1, false));
            r.add(RouteAssignment.of(OffSlot.WR_X, RouteType.CROSS, 2, false));
            r.add(RouteAssignment.of(OffSlot.WR_Z, RouteType.HITCH, 3, true));
            r.add(RouteAssignment.of(OffSlot.RB, RouteType.FLAT, 4, true));
            return r;
        }
        return slantRoutes();
    }

    private static List<RouteAssignment> slantRoutes() {
        List<RouteAssignment> r = new ArrayList<>();
        // Quick open beats encode old high completionMod on slants/hitches
        r.add(new RouteAssignment(OffSlot.WR_X, RouteType.SLANT, 0.85, 6, true, 1));
        r.add(new RouteAssignment(OffSlot.WR_SLOT, RouteType.SLANT, 0.90, 6, true, 2));
        r.add(RouteAssignment.of(OffSlot.TE_L, RouteType.FLAT, 3, true));
        r.add(RouteAssignment.of(OffSlot.RB, RouteType.FLAT, 4, true));
        return r;
    }

    private static List<RouteAssignment> meshRoutes() {
        List<RouteAssignment> r = new ArrayList<>();
        // Mesh rubs open earlier vs man (old completionMod 1.10)
        r.add(new RouteAssignment(OffSlot.WR_X, RouteType.MESH_CROSS, 1.05, 6, false, 1));
        r.add(new RouteAssignment(OffSlot.WR_Z, RouteType.MESH_CROSS, 1.10, 6, false, 2));
        r.add(RouteAssignment.of(OffSlot.TE_L, RouteType.HITCH, 3, true));
        r.add(RouteAssignment.of(OffSlot.RB, RouteType.FLAT, 4, true));
        return r;
    }

    private static List<RouteAssignment> vertsRoutes() {
        List<RouteAssignment> r = new ArrayList<>();
        // Deep verts: later openBeat + depth (old lower completion / higher yards)
        r.add(new RouteAssignment(OffSlot.WR_X, RouteType.VERT, 1.85, 22, false, 1));
        r.add(new RouteAssignment(OffSlot.WR_Z, RouteType.VERT, 1.90, 22, false, 2));
        r.add(new RouteAssignment(OffSlot.WR_SLOT, RouteType.SEAM, 1.70, 18, false, 3));
        r.add(new RouteAssignment(OffSlot.TE_L, RouteType.SEAM, 1.75, 16, true, 4));
        return r;
    }

    private static List<RouteAssignment> screenRoutes() {
        List<RouteAssignment> r = new ArrayList<>();
        r.add(new RouteAssignment(OffSlot.WR_X, RouteType.SCREEN, 0.70, 1, true, 1));
        r.add(RouteAssignment.of(OffSlot.WR_Z, RouteType.GO_STOP, 2, false));
        r.add(RouteAssignment.of(OffSlot.RB, RouteType.FLAT, 3, true));
        return r;
    }
}
