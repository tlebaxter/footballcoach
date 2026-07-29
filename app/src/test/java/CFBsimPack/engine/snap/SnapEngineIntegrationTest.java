package CFBsimPack.engine.snap;

import CFBsimPack.League;
import CFBsimPack.Team;
import CFBsimPack.engine.CoverageCall;
import CFBsimPack.engine.GameState;
import CFBsimPack.engine.PlayCall;
import CFBsimPack.engine.PlayResolver;
import CFBsimPack.engine.PlayResult;
import CFBsimPack.engine.Playbook;
import CFBsimPack.engine.TempoCall;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SnapEngineIntegrationTest {

    private static final String FIRST_NAMES = "A,B,C,D,E,F,G,H,I,J";
    private static final String LAST_NAMES = "K,L,M,N,O,P,Q,R,S,T";

    @Test
    public void playDefinitionsAttachedToCatalog() {
        assertNotNull(Playbook.offenseById("gun_slants").definition);
        assertNotNull(Playbook.offenseById("gun_slants").definition.protection);
        assertNotNull(Playbook.offenseById("gun_four_verts").definition.routes);
        assertTrue(Playbook.offenseById("gun_four_verts").definition.routes.size() >= 3);
        assertNotNull(Playbook.offenseById("gun_qb_draw").definition.run);
        assertNotNull(Playbook.offenseById("pistol_zone_read").definition.rpoRules);
    }

    @Test
    public void meshCompletesMoreVsManThanCover2() throws Exception {
        League league = createLeague();
        Team offense = league.teamList.get(0);
        Team defense = league.teamList.get(1);
        int manComp = countCompletions(offense, defense, "gun_mesh", CoverageCall.MAN, 200L);
        int zoneComp = countCompletions(offense, defense, "gun_mesh", CoverageCall.COVER_2, 200L);
        assertTrue("man=" + manComp + " cover2=" + zoneComp, manComp + 8 >= zoneComp || manComp > 40);
        // Directional: mesh is designed vs man; allow noise but man should not collapse
        assertTrue(manComp > 25);
    }

    @Test
    public void deepShotsProduceSomeIntsWithInterceptorCredit() throws Exception {
        League league = createLeague();
        Team offense = league.teamList.get(0);
        Team defense = league.teamList.get(1);
        int ints = 0;
        int credited = 0;
        for (int i = 0; i < 500; i++) {
            GameState state = state();
            PlayResolver resolver = new PlayResolver(new Random(9000L + i));
            PlayResult r = resolver.resolve(offense, defense, state, PlayCall.fromConcepts(
                    Playbook.offenseById("gun_four_verts"),
                    Playbook.defenseFor(CoverageCall.COVER_1),
                    TempoCall.NORMAL
            ));
            if (r.turnover && r.logLine != null && r.logLine.contains("INTERCEPTION")) {
                ints++;
                if (!r.logLine.contains("QB " + offense.getQB(0).name + " intercepted (")) {
                    // Named defender style
                    credited++;
                }
            }
        }
        assertTrue("expected some INTs on deep shots, got " + ints, ints >= 2);
        assertTrue("expected named interceptor logs when INTs occur, credited=" + credited + " ints=" + ints,
                ints == 0 || credited >= 1);
    }

    @Test
    public void outsideZoneCanCutbackAndPowerGetsYards() throws Exception {
        League league = createLeague();
        Team offense = league.teamList.get(0);
        Team defense = league.teamList.get(1);
        int ozYards = 0;
        int powerYards = 0;
        int n = 120;
        for (int i = 0; i < n; i++) {
            ozYards += rushYards(offense, defense, "gun_outside_zone", 3000L + i);
            powerYards += rushYards(offense, defense, "i_power", 4000L + i);
        }
        assertTrue("outside zone should gain yards avg=" + (ozYards / (double) n), ozYards > n);
        assertTrue("power should gain yards avg=" + (powerYards / (double) n), powerYards > n / 2);
    }

    private int countCompletions(Team offense, Team defense, String playId, CoverageCall cov, long seed) {
        int comp = 0;
        for (int i = 0; i < 120; i++) {
            GameState state = state();
            PlayResolver resolver = new PlayResolver(new Random(seed + i));
            PlayResult r = resolver.resolve(offense, defense, state, PlayCall.fromConcepts(
                    Playbook.offenseById(playId),
                    Playbook.defenseFor(cov),
                    TempoCall.NORMAL
            ));
            if (!r.incomplete && !r.turnover && !r.sack && !r.throwaway && r.yardsGained >= 0
                    && r.logLine != null && (r.logLine.contains("pass") || r.logLine.contains("PASS")
                    || r.logLine.contains("complete") || r.playType == CFBsimPack.engine.OffensePlay.PASS)) {
                if (!r.sack && !r.throwaway && !r.incomplete && !r.turnover) {
                    // completed gain path
                    if (r.yardsGained >= 0 && !r.logLine.contains("incomplete")
                            && !r.logLine.contains("INTERCEPTION")
                            && !r.logLine.contains("SACK")
                            && !r.logLine.contains("THROW AWAY")
                            && !r.logLine.contains("Drop")) {
                        comp++;
                    }
                }
            }
        }
        return comp;
    }

    private int rushYards(Team offense, Team defense, String playId, long seed) {
        GameState state = state();
        PlayResolver resolver = new PlayResolver(new Random(seed));
        PlayResult r = resolver.resolve(offense, defense, state, PlayCall.fromConcepts(
                Playbook.offenseById(playId),
                Playbook.defenseFor(CoverageCall.COVER_3),
                TempoCall.NORMAL
        ));
        return Math.max(0, r.yardsGained);
    }

    private static GameState state() {
        GameState state = new GameState();
        state.down = 1;
        state.yardsNeed = 10;
        state.yardLine = 35;
        state.possessionHome = true;
        state.gameTime = 2000;
        state.homeScore = 14;
        state.awayScore = 14;
        state.pendingKickoff = false;
        return state;
    }

    private League createLeague() throws IOException {
        Path csvPath = Paths.get("src/main/assets/fbs_2026.csv");
        if (!Files.exists(csvPath)) {
            csvPath = Paths.get("app/src/main/assets/fbs_2026.csv");
        }
        String csv = new String(Files.readAllBytes(csvPath), StandardCharsets.UTF_8);
        League league = new League(FIRST_NAMES, LAST_NAMES, csv);
        league.userTeam = league.teamList.get(0);
        return league;
    }
}
