package CFBsimPack.engine;

import CFBsimPack.Formation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Static catalog of offense/defense concepts for coach UI and AI.
 */
public final class Playbook {

    private static final Map<String, OffenseConcept> OFFENSE_BY_ID = new LinkedHashMap<>();
    private static final Map<CoverageCall, DefenseConcept> DEFENSE_BY_COV = new LinkedHashMap<>();
    private static final List<OffenseConcept> OFFENSE;
    private static final List<DefenseConcept> DEFENSE;

    static {
        addOff(run("inside_zone", "Inside Zone", Formation.SINGLEBACK, "11", 1.08, 0.95,
                d(r("IZ", 0, 0.28f, 0.50f, 0.55f, 0.48f, 0.72f, 0.45f),
                        r("Lead", 2, 0.28f, 0.58f, 0.48f, 0.55f))));
        addOff(run("outside_zone", "Outside Zone", Formation.SHOTGUN, "11", 1.12, 1.02,
                d(r("OZ", 0, 0.28f, 0.52f, 0.50f, 0.30f, 0.78f, 0.22f))));
        addOff(run("power", "Power", Formation.I_FORM, "21", 1.05, 0.92,
                d(r("Power", 0, 0.26f, 0.55f, 0.48f, 0.52f, 0.70f, 0.48f),
                        r("Pull", 2, 0.22f, 0.42f, 0.45f, 0.50f))));
        addOff(run("counter", "Counter", Formation.PISTOL, "11", 1.10, 0.98,
                d(r("Counter", 0, 0.28f, 0.50f, 0.40f, 0.62f, 0.68f, 0.58f))));
        addOff(run("dive", "Dive", Formation.I_FORM, "21", 0.92, 0.88,
                d(r("Dive", 0, 0.28f, 0.50f, 0.58f, 0.50f))));
        addOff(run("sweep", "Sweep", Formation.WISHBONE, "20", 1.15, 1.05,
                d(r("Sweep", 0, 0.28f, 0.55f, 0.55f, 0.25f, 0.82f, 0.18f))));
        addOff(run("qb_draw", "QB Draw", Formation.SHOTGUN, "10", 1.06, 0.90,
                d(r("Draw", 0, 0.28f, 0.50f, 0.62f, 0.50f))));
        addOff(run("iso", "Iso", Formation.I_FORM, "21", 1.00, 0.90,
                d(r("Iso", 0, 0.28f, 0.52f, 0.65f, 0.48f),
                        r("FB", 2, 0.30f, 0.58f, 0.50f, 0.52f))));

        addOff(pass("pa_comebacks", "PA Comebacks", Formation.SHOTGUN, "10", DepthBand.MEDIUM,
                1.02, 1.05, 1.08, TargetBias.WR,
                d(r("Comeback", 0, 0.30f, 0.22f, 0.62f, 0.18f, 0.78f, 0.28f),
                        r("Comeback", 0, 0.30f, 0.78f, 0.62f, 0.82f, 0.78f, 0.72f),
                        r("Cross", 1, 0.30f, 0.40f, 0.70f, 0.55f))));
        addOff(pass("pa_crossers", "PA Crossers", Formation.PISTOL, "11", DepthBand.MEDIUM,
                1.00, 1.08, 1.05, TargetBias.WR,
                d(r("Cross", 0, 0.30f, 0.25f, 0.75f, 0.70f),
                        r("Cross", 0, 0.30f, 0.75f, 0.75f, 0.30f),
                        r("Check", 1, 0.30f, 0.55f, 0.55f, 0.55f))));
        addOff(pass("mesh", "Mesh", Formation.SHOTGUN, "11", DepthBand.SHORT,
                1.10, 0.88, 0.95, TargetBias.WR,
                d(r("Mesh", 0, 0.30f, 0.30f, 0.55f, 0.55f, 0.72f, 0.70f),
                        r("Mesh", 0, 0.30f, 0.70f, 0.55f, 0.45f, 0.72f, 0.30f))));
        addOff(pass("slants", "Slants", Formation.SHOTGUN, "11", DepthBand.SHORT,
                1.12, 0.85, 0.92, TargetBias.WR,
                d(r("Slant", 0, 0.30f, 0.25f, 0.58f, 0.40f),
                        r("Slant", 0, 0.30f, 0.75f, 0.58f, 0.60f))));
        addOff(pass("four_verts", "Four Verts", Formation.EMPTY, "10", DepthBand.DEEP,
                0.88, 1.22, 1.18, TargetBias.WR,
                d(r("Vert", 0, 0.30f, 0.18f, 0.88f, 0.15f),
                        r("Vert", 0, 0.30f, 0.38f, 0.88f, 0.35f),
                        r("Vert", 0, 0.30f, 0.62f, 0.88f, 0.65f),
                        r("Vert", 0, 0.30f, 0.82f, 0.88f, 0.85f))));
        addOff(pass("smash", "Smash", Formation.SLOT, "11", DepthBand.MEDIUM,
                1.04, 1.00, 1.00, TargetBias.WR,
                d(r("Hitch", 1, 0.30f, 0.22f, 0.52f, 0.20f),
                        r("Corner", 0, 0.30f, 0.35f, 0.60f, 0.28f, 0.78f, 0.15f))));
        addOff(pass("levels", "Levels", Formation.SINGLEBACK, "11", DepthBand.MEDIUM,
                1.06, 0.98, 0.98, TargetBias.TE,
                d(r("Dig", 0, 0.30f, 0.45f, 0.70f, 0.45f),
                        r("Sit", 1, 0.30f, 0.55f, 0.52f, 0.55f))));
        addOff(pass("flood", "Flood", Formation.TRIPS, "10", DepthBand.MEDIUM,
                1.00, 1.06, 1.05, TargetBias.WR,
                d(r("Flat", 1, 0.30f, 0.78f, 0.55f, 0.88f),
                        r("Out", 0, 0.30f, 0.65f, 0.68f, 0.78f),
                        r("Corner", 0, 0.30f, 0.50f, 0.80f, 0.70f))));
        addOff(pass("te_seam", "TE Seam", Formation.ACE, "12", DepthBand.MEDIUM,
                1.02, 1.10, 1.02, TargetBias.TE,
                d(r("Seam", 0, 0.30f, 0.48f, 0.82f, 0.45f),
                        r("Clear", 1, 0.30f, 0.25f, 0.75f, 0.20f))));
        addOff(pass("screen", "WR Screen", Formation.SHOTGUN, "10", DepthBand.SHORT,
                1.15, 0.92, 0.70, TargetBias.WR,
                d(r("Screen", 0, 0.30f, 0.25f, 0.42f, 0.20f, 0.55f, 0.28f),
                        r("Block", 2, 0.35f, 0.40f, 0.48f, 0.28f))));
        addOff(pass("rb_angle", "RB Angle", Formation.SHOTGUN, "11", DepthBand.SHORT,
                1.08, 0.90, 0.90, TargetBias.RB,
                d(r("Angle", 0, 0.28f, 0.58f, 0.50f, 0.65f, 0.68f, 0.55f))));
        addOff(pass("post_corner", "Post-Corner", Formation.SHOTGUN, "11", DepthBand.DEEP,
                0.92, 1.18, 1.12, TargetBias.WR,
                d(r("Post", 0, 0.30f, 0.40f, 0.70f, 0.42f, 0.88f, 0.35f),
                        r("Corner", 0, 0.30f, 0.70f, 0.65f, 0.75f, 0.85f, 0.88f))));

        addOff(rpo("rpo_peek", "RPO Peek", Formation.SHOTGUN, "11", DepthBand.SHORT,
                1.04, 0.95, 1.02, 0.98,
                d(r("Zone", 0, 0.28f, 0.50f, 0.60f, 0.48f),
                        r("Peek", 1, 0.30f, 0.28f, 0.58f, 0.22f))));
        addOff(rpo("rpo_slant", "RPO Slant", Formation.SHOTGUN, "11", DepthBand.SHORT,
                1.06, 0.92, 1.00, 0.96,
                d(r("Zone", 0, 0.28f, 0.52f, 0.58f, 0.50f),
                        r("Slant", 1, 0.30f, 0.30f, 0.55f, 0.42f))));

        addOff(special("field_goal", "Field Goal", OffensePlay.FIELD_GOAL, Formation.ACE, "11",
                d(r("Kick", 0, 0.25f, 0.50f, 0.70f, 0.50f))));
        addOff(special("punt", "Punt", OffensePlay.PUNT, Formation.ACE, "11",
                d(r("Punt", 0, 0.22f, 0.50f, 0.75f, 0.45f))));
        addOff(special("spike", "Spike", OffensePlay.SPIKE, Formation.SHOTGUN, "11",
                d(r("Spike", 0, 0.28f, 0.50f, 0.35f, 0.50f))));
        addOff(special("kneel", "Kneel", OffensePlay.KNEEL, Formation.SHOTGUN, "11",
                d(r("Kneel", 0, 0.28f, 0.50f, 0.32f, 0.50f))));

        addDef(CoverageCall.COVER_0, "Cover 0",
                PlayDiagram.defense(z(0.55f, 0.25f, 0.08f, 2), z(0.55f, 0.75f, 0.08f, 2),
                        z(0.48f, 0.50f, 0.10f, 1)));
        addDef(CoverageCall.COVER_1, "Cover 1",
                PlayDiagram.defense(z(0.78f, 0.50f, 0.14f, 0), z(0.52f, 0.25f, 0.07f, 2),
                        z(0.52f, 0.75f, 0.07f, 2)));
        addDef(CoverageCall.COVER_2, "Cover 2",
                PlayDiagram.defense(z(0.80f, 0.28f, 0.16f, 0), z(0.80f, 0.72f, 0.16f, 0),
                        z(0.50f, 0.50f, 0.10f, 1)));
        addDef(CoverageCall.COVER_3, "Cover 3",
                PlayDiagram.defense(z(0.82f, 0.20f, 0.12f, 0), z(0.82f, 0.50f, 0.14f, 0),
                        z(0.82f, 0.80f, 0.12f, 0), z(0.52f, 0.35f, 0.08f, 1), z(0.52f, 0.65f, 0.08f, 1)));
        addDef(CoverageCall.COVER_4, "Cover 4",
                PlayDiagram.defense(z(0.82f, 0.18f, 0.10f, 0), z(0.82f, 0.40f, 0.10f, 0),
                        z(0.82f, 0.60f, 0.10f, 0), z(0.82f, 0.82f, 0.10f, 0)));
        addDef(CoverageCall.MAN, "Man",
                PlayDiagram.defense(z(0.55f, 0.22f, 0.06f, 2), z(0.55f, 0.40f, 0.06f, 2),
                        z(0.55f, 0.60f, 0.06f, 2), z(0.55f, 0.78f, 0.06f, 2)));
        addDef(CoverageCall.ZONE, "Zone",
                PlayDiagram.defense(z(0.70f, 0.30f, 0.12f, 0), z(0.70f, 0.70f, 0.12f, 0),
                        z(0.48f, 0.50f, 0.10f, 1)));
        addDef(CoverageCall.STACK_BOX, "Stack Box",
                PlayDiagram.defense(z(0.45f, 0.35f, 0.09f, 1), z(0.45f, 0.50f, 0.09f, 1),
                        z(0.45f, 0.65f, 0.09f, 1), z(0.72f, 0.50f, 0.10f, 0)));
        addDef(CoverageCall.SPY, "Spy",
                PlayDiagram.defense(z(0.48f, 0.50f, 0.11f, 1), z(0.75f, 0.30f, 0.10f, 0),
                        z(0.75f, 0.70f, 0.10f, 0)));
        addDef(CoverageCall.PRESS, "Press",
                PlayDiagram.defense(z(0.42f, 0.22f, 0.06f, 2), z(0.42f, 0.40f, 0.06f, 2),
                        z(0.42f, 0.60f, 0.06f, 2), z(0.42f, 0.78f, 0.06f, 2)));
        addDef(CoverageCall.OFF_COVERAGE, "Off Coverage",
                PlayDiagram.defense(z(0.62f, 0.25f, 0.08f, 2), z(0.62f, 0.50f, 0.08f, 2),
                        z(0.62f, 0.75f, 0.08f, 2), z(0.82f, 0.50f, 0.12f, 0)));

        OFFENSE = Collections.unmodifiableList(new ArrayList<>(OFFENSE_BY_ID.values()));
        DEFENSE = Collections.unmodifiableList(new ArrayList<>(DEFENSE_BY_COV.values()));
    }

    private Playbook() {
    }

    public static List<OffenseConcept> allOffense() {
        return OFFENSE;
    }

    public static List<DefenseConcept> allDefense() {
        return DEFENSE;
    }

    public static OffenseConcept offenseById(String id) {
        return id == null ? null : OFFENSE_BY_ID.get(id);
    }

    public static DefenseConcept defenseFor(CoverageCall coverage) {
        DefenseConcept d = DEFENSE_BY_COV.get(coverage);
        if (d != null) return d;
        return DEFENSE_BY_COV.get(CoverageCall.COVER_3);
    }

    public static OffenseConcept defaultOffense() {
        return OFFENSE_BY_ID.get("inside_zone");
    }

    public static DefenseConcept defaultDefense() {
        return DEFENSE_BY_COV.get(CoverageCall.COVER_3);
    }

    public static List<OffenseConcept> offenseByFamily(ConceptFamily family) {
        List<OffenseConcept> out = new ArrayList<>();
        for (OffenseConcept c : OFFENSE) {
            if (c.family == family) out.add(c);
        }
        return out;
    }

    /** Situational filter for the play picker / AI. */
    public static List<OffenseConcept> situationalOffense(GameState state, boolean includeSpecials) {
        List<OffenseConcept> out = new ArrayList<>();
        if (state == null) {
            out.addAll(OFFENSE);
            return out;
        }
        for (OffenseConcept c : OFFENSE) {
            if (!includeSpecials && c.family == ConceptFamily.SPECIAL) continue;
            if (c.offensePlay == OffensePlay.SPIKE && (state.gameTime > 40 || state.down > 3)) continue;
            if (c.offensePlay == OffensePlay.KNEEL && state.gameTime > 180) continue;
            if (c.offensePlay == OffensePlay.FIELD_GOAL && state.yardLine < 55) continue;
            if (c.offensePlay == OffensePlay.PUNT && state.down < 4) continue;
            if (c.depth == DepthBand.DEEP && state.down == 4 && state.yardsNeed <= 2) continue;
            out.add(c);
        }
        if (out.isEmpty()) {
            for (OffenseConcept c : OFFENSE) {
                if (c.family != ConceptFamily.SPECIAL) out.add(c);
            }
        }
        return out;
    }

    private static void addOff(OffenseConcept c) {
        OFFENSE_BY_ID.put(c.id, c);
    }

    private static void addDef(CoverageCall cov, String name, PlayDiagram diagram) {
        DEFENSE_BY_COV.put(cov, new DefenseConcept(cov.name().toLowerCase(), name, cov, diagram));
    }

    private static OffenseConcept run(String id, String name, Formation form, String pers,
                                     double runYards, double fumble, PlayDiagram diagram) {
        return new OffenseConcept(id, name, ConceptFamily.RUN, OffensePlay.RUN, form, pers,
                DepthBand.NONE, 1.0, 1.0, 1.0, runYards, fumble, TargetBias.RB, 0.0, diagram);
    }

    private static OffenseConcept pass(String id, String name, Formation form, String pers, DepthBand depth,
                                      double completion, double yards, double sack, TargetBias bias,
                                      PlayDiagram diagram) {
        return new OffenseConcept(id, name, ConceptFamily.PASS, OffensePlay.PASS, form, pers,
                depth, completion, yards, sack, 1.0, 1.0, bias, 0.0, diagram);
    }

    private static OffenseConcept rpo(String id, String name, Formation form, String pers, DepthBand depth,
                                     double completion, double passYards, double sack, double runYards,
                                     PlayDiagram diagram) {
        // RPOs resolve as PASS with run-friendly matchup; slight dual mods
        return new OffenseConcept(id, name, ConceptFamily.RPO, OffensePlay.PASS, form, pers,
                depth, completion, passYards, sack, runYards, 1.0, TargetBias.WR, -0.05, diagram);
    }

    private static OffenseConcept special(String id, String name, OffensePlay play, Formation form,
                                         String pers, PlayDiagram diagram) {
        return new OffenseConcept(id, name, ConceptFamily.SPECIAL, play, form, pers,
                DepthBand.NONE, 1.0, 1.0, 1.0, 1.0, 1.0, TargetBias.ANY, 0.0, diagram);
    }

    private static PlayDiagram d(PlayDiagram.Route... routes) {
        return PlayDiagram.offense(routes);
    }

    private static PlayDiagram.Route r(String label, int style, float... xy) {
        return PlayDiagram.Route.of(label, style, xy);
    }

    private static PlayDiagram.Zone z(float cx, float cy, float rad, int style) {
        return new PlayDiagram.Zone(cx, cy, rad, style);
    }
}
