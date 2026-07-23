package CFBsimPack.engine;

import CFBsimPack.Formation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Static catalog: formations hold named plays (popular call names + concept tags).
 * Defense catalog is coverage calls with short descriptions. Text-only — no diagrams.
 */
public final class Playbook {

    private static final Map<String, OffenseConcept> OFFENSE_BY_ID = new LinkedHashMap<>();
    private static final Map<CoverageCall, DefenseConcept> DEFENSE_BY_COV = new LinkedHashMap<>();
    private static final Map<String, DefenseConcept> DEFENSE_BY_ID = new LinkedHashMap<>();
    private static final List<OffenseConcept> OFFENSE;
    private static final List<DefenseConcept> DEFENSE;
    private static final List<DefenseConcept> ST_DEFENSE;
    private static final List<Formation> OFFENSE_FORMATIONS;

    static {
        // —— Shotgun ——
        addOff(run("gun_inside_zone", "Inside Zone", Formation.SHOTGUN, "11",
                "Gap-scheme stretch; cutback lanes", 1.08, 0.95));
        addOff(run("gun_outside_zone", "Outside Zone", Formation.SHOTGUN, "11",
                "Wide stretch; bounce or cut", 1.12, 1.02));
        addOff(run("gun_qb_draw", "QB Draw", Formation.SHOTGUN, "10",
                "Delayed QB keep up A/B gap", 1.06, 0.90));
        addOff(pass("gun_mesh", "Mesh", Formation.SHOTGUN, "11", DepthBand.SHORT, TargetBias.WR,
                "Crossing mesh under; rub vs man", 1.10, 0.88, 0.95));
        addOff(pass("gun_slants", "Slants", Formation.SHOTGUN, "11", DepthBand.SHORT, TargetBias.WR,
                "Quick slants off release", 1.12, 0.85, 0.92));
        addOff(pass("gun_stick", "Stick", Formation.SHOTGUN, "11", DepthBand.SHORT, TargetBias.TE,
                "Stick / glance / flat triangle", 1.10, 0.86, 0.90));
        addOff(pass("gun_levels", "Levels", Formation.SHOTGUN, "11", DepthBand.MEDIUM, TargetBias.TE,
                "High-low dig over sit", 1.06, 0.98, 0.98));
        addOff(pass("gun_smash", "Smash", Formation.SHOTGUN, "11", DepthBand.MEDIUM, TargetBias.WR,
                "Hitch under corner", 1.04, 1.00, 1.00));
        addOff(pass("gun_pa_comebacks", "PA Comebacks", Formation.SHOTGUN, "10", DepthBand.MEDIUM, TargetBias.WR,
                "Play-action comebacks + crosser", 1.02, 1.05, 1.08));
        addOff(pass("gun_four_verts", "Four Verts", Formation.SHOTGUN, "10", DepthBand.DEEP, TargetBias.WR,
                "All verticals; attack deep thirds", 0.88, 1.22, 1.18));
        addOff(pass("gun_post_corner", "Post-Corner", Formation.SHOTGUN, "11", DepthBand.DEEP, TargetBias.WR,
                "Post clear + corner behind", 0.92, 1.18, 1.12));
        addOff(pass("gun_screen", "WR Screen", Formation.SHOTGUN, "10", DepthBand.SHORT, TargetBias.WR,
                "Tunnel/now screen to perimeter", 1.15, 0.92, 0.70));
        addOff(pass("gun_rb_angle", "RB Angle", Formation.SHOTGUN, "11", DepthBand.SHORT, TargetBias.RB,
                "Back angle / option vs LB", 1.08, 0.90, 0.90));
        addOff(rpo("gun_rpo_peek", "RPO Peek", Formation.SHOTGUN, "11", DepthBand.SHORT,
                "Zone run with bubble/peek", 1.04, 0.95, 1.02, 0.98));
        addOff(rpo("gun_rpo_slant", "RPO Alert Slant", Formation.SHOTGUN, "11", DepthBand.SHORT,
                "Zone + alert slant vs light box", 1.06, 0.92, 1.00, 0.96));

        // —— Pistol ——
        addOff(run("pistol_counter", "Counter", Formation.PISTOL, "11",
                "Misdirection pull; cutback", 1.10, 0.98));
        addOff(run("pistol_inside_zone", "Inside Zone", Formation.PISTOL, "11",
                " downhill zone from pistol", 1.07, 0.95));
        addOff(pass("pistol_pa_crossers", "PA Crossers", Formation.PISTOL, "11", DepthBand.MEDIUM, TargetBias.WR,
                "Play-action deep crossers", 1.00, 1.08, 1.05));
        addOff(pass("pistol_boot", "PA Boot", Formation.PISTOL, "11", DepthBand.MEDIUM, TargetBias.TE,
                "Bootleg flood off fake", 0.98, 1.06, 1.10));
        addOff(rpo("pistol_zone_read", "Zone Read", Formation.PISTOL, "11", DepthBand.SHORT,
                "Read end; give or keep", 1.02, 0.90, 0.95, 1.05));

        // —— I-Form ——
        addOff(run("i_power", "Power", Formation.I_FORM, "21",
                "Down blocks + puller; lead FB", 1.05, 0.92));
        addOff(run("i_iso", "Iso", Formation.I_FORM, "21",
                "FB lead on LB; iso crease", 1.00, 0.90));
        addOff(run("i_dive", "Dive", Formation.I_FORM, "21",
                "Straight ahead A-gap dive", 0.92, 0.88));
        addOff(pass("i_pa_boot", "PA Boot", Formation.I_FORM, "21", DepthBand.MEDIUM, TargetBias.TE,
                "Boot off power fake", 0.97, 1.05, 1.08));
        addOff(pass("i_flat_sail", "Flat-Sail", Formation.I_FORM, "12", DepthBand.MEDIUM, TargetBias.WR,
                "Flat under sail / dig", 1.03, 0.98, 1.00));

        // —— Singleback ——
        addOff(run("sb_inside_zone", "Inside Zone", Formation.SINGLEBACK, "11",
                "One-back zone stretch", 1.08, 0.95));
        addOff(run("sb_duo", "Duo", Formation.SINGLEBACK, "11",
                "Double teams; vertical double", 1.04, 0.93));
        addOff(pass("sb_levels", "Levels", Formation.SINGLEBACK, "11", DepthBand.MEDIUM, TargetBias.TE,
                "TE dig over shallow", 1.06, 0.98, 0.98));
        addOff(pass("sb_y_cross", "Y-Cross", Formation.SINGLEBACK, "11", DepthBand.MEDIUM, TargetBias.TE,
                "TE cross + dig hierarchy", 1.02, 1.04, 1.02));
        addOff(pass("sb_curl_flat", "Curl-Flat", Formation.SINGLEBACK, "11", DepthBand.SHORT, TargetBias.WR,
                "Curl / flat hi-lo", 1.08, 0.90, 0.94));

        // —— Empty ——
        addOff(pass("empty_four_verts", "Four Verts", Formation.EMPTY, "10", DepthBand.DEEP, TargetBias.WR,
                "5-wide verticals", 0.88, 1.22, 1.18));
        addOff(pass("empty_mesh", "Mesh", Formation.EMPTY, "10", DepthBand.SHORT, TargetBias.WR,
                "Empty mesh rubs", 1.10, 0.88, 0.95));
        addOff(pass("empty_all_hitch", "All Hitch", Formation.EMPTY, "10", DepthBand.SHORT, TargetBias.WR,
                "Quick game all hitches", 1.14, 0.82, 0.88));
        addOff(pass("empty_smash", "Smash", Formation.EMPTY, "10", DepthBand.MEDIUM, TargetBias.WR,
                "Empty smash corners", 1.04, 1.00, 1.00));

        // —— Trips ——
        addOff(pass("trips_flood", "Flood", Formation.TRIPS, "10", DepthBand.MEDIUM, TargetBias.WR,
                "3-level flood to trips", 1.00, 1.06, 1.05));
        addOff(pass("trips_smash", "Smash", Formation.TRIPS, "10", DepthBand.MEDIUM, TargetBias.WR,
                "Trips smash / hitch-corner", 1.04, 1.00, 1.00));
        addOff(pass("trips_stick_nod", "Stick-Nod", Formation.TRIPS, "11", DepthBand.MEDIUM, TargetBias.TE,
                "Stick then nod vertical", 1.00, 1.08, 1.02));
        addOff(run("trips_outside_zone", "Outside Zone", Formation.TRIPS, "10",
                "Zone bounce to trips edge", 1.10, 1.00));

        // —— Slot ——
        addOff(pass("slot_smash", "Smash", Formation.SLOT, "11", DepthBand.MEDIUM, TargetBias.WR,
                "Slot smash corner", 1.04, 1.00, 1.00));
        addOff(pass("slot_dagger", "Dagger", Formation.SLOT, "11", DepthBand.MEDIUM, TargetBias.WR,
                "Deep dig over seam", 1.00, 1.08, 1.04));
        addOff(pass("slot_drive", "Drive", Formation.SLOT, "11", DepthBand.SHORT, TargetBias.WR,
                "Shallow drive / dig", 1.08, 0.92, 0.96));

        // —— Jumbo ——
        addOff(run("jumbo_power", "Power", Formation.JUMBO, "22",
                "Heavy power; short yardage", 1.02, 0.90));
        addOff(run("jumbo_iso", "Iso", Formation.JUMBO, "22",
                "Heavy iso / sneak look", 0.98, 0.88));
        addOff(pass("jumbo_te_seam", "TE Seam", Formation.JUMBO, "22", DepthBand.MEDIUM, TargetBias.TE,
                "Play-action TE seam", 1.00, 1.08, 1.00));

        // —— Wishbone ——
        addOff(run("bone_sweep", "Sweep", Formation.WISHBONE, "20",
                "Triple-option sweep edge", 1.15, 1.05));
        addOff(run("bone_dive", "Dive", Formation.WISHBONE, "20",
                "Fullback dive", 0.95, 0.90));
        addOff(run("bone_option", "Triple Option", Formation.WISHBONE, "20",
                "Dive / keep / pitch read", 1.08, 1.02));

        // —— Ace ——
        addOff(pass("ace_te_seam", "TE Seam", Formation.ACE, "12", DepthBand.MEDIUM, TargetBias.TE,
                "2-TE seam vertical", 1.02, 1.10, 1.02));
        addOff(pass("ace_levels", "Levels", Formation.ACE, "12", DepthBand.MEDIUM, TargetBias.TE,
                "TE levels high-low", 1.05, 0.98, 0.98));
        addOff(run("ace_duo", "Duo", Formation.ACE, "12",
                "2-TE duo downhill", 1.04, 0.92));

        // —— Specials (Ace / Shotgun shells) ——
        addOff(special("field_goal", "Field Goal", OffensePlay.FIELD_GOAL, Formation.ACE, "11",
                "Placekick attempt"));
        addOff(special("punt", "Punt", OffensePlay.PUNT, Formation.ACE, "11",
                "Punt protection / kick"));
        addOff(special("fake_punt", "Fake Punt", OffensePlay.FAKE_PUNT, Formation.ACE, "11",
                "Punt look; run or pass"));
        addOff(special("kickoff", "Kickoff", OffensePlay.KICKOFF, Formation.ACE, "11",
                "Kickoff / coverage"));
        addOff(special("spike", "Spike", OffensePlay.SPIKE, Formation.SHOTGUN, "11",
                "Kill clock; incomplete"));
        addOff(special("kneel", "Kneel", OffensePlay.KNEEL, Formation.SHOTGUN, "11",
                "Victory formation kneel"));

        addDef(CoverageCall.COVER_0, "Cover 0", "Man across; no deep help — blitz look");
        addDef(CoverageCall.COVER_1, "Cover 1", "Man under with single-high safety");
        addDef(CoverageCall.COVER_2, "Cover 2", "Two-deep safeties; five underneath");
        addDef(CoverageCall.COVER_3, "Cover 3", "Three-deep zone; four underneath");
        addDef(CoverageCall.COVER_4, "Cover 4", "Quarters; defend verticals");
        addDef(CoverageCall.MAN, "Man", "Man-to-man; match releases");
        addDef(CoverageCall.ZONE, "Zone", "Soft zone drops; pattern match lite");
        addDef(CoverageCall.STACK_BOX, "Stack Box", "Extra hat in box vs run");
        addDef(CoverageCall.SPY, "Spy", "Spy on QB; contain scramble");
        addDef(CoverageCall.PRESS, "Press", "Press man at LOS");
        addDef(CoverageCall.OFF_COVERAGE, "Off Coverage", "Soft cushions; give cushion");

        List<DefenseConcept> st = new ArrayList<>();
        st.add(addStDef("punt_return", "Punt Return", CoverageCall.ZONE,
                "Set return; PR fields the kick"));
        st.add(addStDef("fair_catch", "Fair Catch", CoverageCall.OFF_COVERAGE,
                "Fair catch; no return"));
        st.add(addStDef("punt_block", "Punt Block", CoverageCall.COVER_0,
                "Rush the punter; risk fake"));
        st.add(addStDef("defend_scrimmage", "Defend Play", CoverageCall.COVER_3,
                "Expect go-for-it or fake; normal D"));
        st.add(addStDef("kick_return", "Kick Return", CoverageCall.ZONE,
                "KR returns the kickoff"));
        st.add(addStDef("kick_fair_catch", "Kick Fair Catch", CoverageCall.OFF_COVERAGE,
                "Fair catch / settle for touchback"));
        ST_DEFENSE = Collections.unmodifiableList(st);

        OFFENSE = Collections.unmodifiableList(new ArrayList<>(OFFENSE_BY_ID.values()));
        DEFENSE = Collections.unmodifiableList(new ArrayList<>(DEFENSE_BY_COV.values()));

        Set<Formation> forms = new LinkedHashSet<>();
        for (OffenseConcept c : OFFENSE) {
            if (c.family != ConceptFamily.SPECIAL) forms.add(c.formation);
        }
        // Keep specials' formations available too for FG/punt shells when filtering specials
        forms.add(Formation.ACE);
        forms.add(Formation.SHOTGUN);
        OFFENSE_FORMATIONS = Collections.unmodifiableList(new ArrayList<>(forms));
    }

    private Playbook() {
    }

    public static List<OffenseConcept> allOffense() {
        return OFFENSE;
    }

    public static List<DefenseConcept> allDefense() {
        return DEFENSE;
    }

    public static List<DefenseConcept> allSpecialTeamsDefense() {
        return ST_DEFENSE;
    }

    public static boolean isSpecialTeamsDefense(DefenseConcept c) {
        if (c == null || c.id == null) return false;
        for (DefenseConcept st : ST_DEFENSE) {
            if (c.id.equals(st.id)) return true;
        }
        return false;
    }

    /** Formations that have at least one non-special play (plus FG/punt shells). */
    public static List<Formation> offenseFormations() {
        return OFFENSE_FORMATIONS;
    }

    public static List<OffenseConcept> offenseByFormation(Formation formation) {
        List<OffenseConcept> out = new ArrayList<>();
        if (formation == null) return out;
        for (OffenseConcept c : OFFENSE) {
            if (c.formation == formation) out.add(c);
        }
        return out;
    }

    public static OffenseConcept offenseById(String id) {
        return id == null ? null : OFFENSE_BY_ID.get(id);
    }

    public static DefenseConcept defenseFor(CoverageCall coverage) {
        DefenseConcept d = DEFENSE_BY_COV.get(coverage);
        if (d != null) return d;
        return DEFENSE_BY_COV.get(CoverageCall.COVER_3);
    }

    public static DefenseConcept defenseById(String id) {
        if (id == null) return defaultDefense();
        DefenseConcept d = DEFENSE_BY_ID.get(id);
        return d != null ? d : defaultDefense();
    }

    public static OffenseConcept defaultOffense() {
        OffenseConcept c = OFFENSE_BY_ID.get("gun_inside_zone");
        return c != null ? c : OFFENSE.get(0);
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
            if (state.pendingTry && state.tryIsTwoPoint) {
                if (c.family == ConceptFamily.SPECIAL) continue;
                if (c.depth == DepthBand.DEEP) continue;
            }
            if (c.offensePlay == OffensePlay.SPIKE && (state.gameTime > 40 || state.down > 3)) continue;
            if (c.offensePlay == OffensePlay.KNEEL && state.gameTime > 180) continue;
            if (c.offensePlay == OffensePlay.FIELD_GOAL && state.yardLine < 55) continue;
            if (c.offensePlay == OffensePlay.PUNT && state.down < 4) continue;
            if (c.offensePlay == OffensePlay.FAKE_PUNT && state.down < 4) continue;
            if (c.offensePlay == OffensePlay.KICKOFF && !state.pendingKickoff) continue;
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

    public static List<OffenseConcept> situationalOffenseInFormation(
            GameState state,
            Formation formation,
            boolean includeSpecials
    ) {
        List<OffenseConcept> out = new ArrayList<>();
        for (OffenseConcept c : situationalOffense(state, includeSpecials)) {
            if (formation == null || c.formation == formation) out.add(c);
        }
        return out;
    }

    private static void addOff(OffenseConcept c) {
        OFFENSE_BY_ID.put(c.id, c);
    }

    /** Defense packages for 4th-down / kickoff situations. */
    public static List<DefenseConcept> situationalDefense(GameState state) {
        List<DefenseConcept> out = new ArrayList<>();
        if (state == null) {
            out.addAll(DEFENSE);
            return out;
        }
        if (state.pendingKickoff) {
            out.add(defenseById("kick_return"));
            out.add(defenseById("kick_fair_catch"));
            return out;
        }
        if (state.down >= 4) {
            out.add(defenseById("punt_return"));
            out.add(defenseById("fair_catch"));
            out.add(defenseById("punt_block"));
            out.add(defenseById("defend_scrimmage"));
            out.addAll(DEFENSE);
            return out;
        }
        out.addAll(DEFENSE);
        return out;
    }

    private static void addDef(CoverageCall cov, String name, String concept) {
        DefenseConcept d = new DefenseConcept(cov.name().toLowerCase(), name, cov, concept);
        DEFENSE_BY_COV.put(cov, d);
        DEFENSE_BY_ID.put(d.id, d);
    }

    private static DefenseConcept addStDef(String id, String name, CoverageCall cov, String concept) {
        DefenseConcept d = new DefenseConcept(id, name, cov, concept);
        DEFENSE_BY_ID.put(d.id, d);
        return d;
    }

    private static OffenseConcept run(
            String id, String name, Formation form, String pers, String concept,
            double runYards, double fumble
    ) {
        return new OffenseConcept(id, name, ConceptFamily.RUN, OffensePlay.RUN, form, pers,
                DepthBand.NONE, concept, 1.0, 1.0, 1.0, runYards, fumble, TargetBias.RB, 0.0);
    }

    private static OffenseConcept pass(
            String id, String name, Formation form, String pers, DepthBand depth, TargetBias bias,
            String concept, double completion, double yards, double sack
    ) {
        return new OffenseConcept(id, name, ConceptFamily.PASS, OffensePlay.PASS, form, pers,
                depth, concept, completion, yards, sack, 1.0, 1.0, bias, 0.0);
    }

    private static OffenseConcept rpo(
            String id, String name, Formation form, String pers, DepthBand depth, String concept,
            double completion, double passYards, double sack, double runYards
    ) {
        return new OffenseConcept(id, name, ConceptFamily.RPO, OffensePlay.PASS, form, pers,
                depth, concept, completion, passYards, sack, runYards, 1.0, TargetBias.WR, -0.05);
    }

    private static OffenseConcept special(
            String id, String name, OffensePlay play, Formation form, String pers, String concept
    ) {
        return new OffenseConcept(id, name, ConceptFamily.SPECIAL, play, form, pers,
                DepthBand.NONE, concept, 1.0, 1.0, 1.0, 1.0, 1.0, TargetBias.ANY, 0.0);
    }
}
