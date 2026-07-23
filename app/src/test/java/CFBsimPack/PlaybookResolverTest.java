package CFBsimPack;

import CFBsimPack.engine.AiPlayCaller;
import CFBsimPack.engine.ConceptFamily;
import CFBsimPack.engine.CoverageCall;
import CFBsimPack.engine.GameState;
import CFBsimPack.engine.OffenseConcept;
import CFBsimPack.engine.OffensePlay;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PlaybookResolverTest {

    private static final String FIRST_NAMES = "A,B,C,D,E,F,G,H,I,J";
    private static final String LAST_NAMES = "K,L,M,N,O,P,Q,R,S,T";

    @Test
    public void playbookHasNamedConceptsByFormation() {
        assertTrue(Playbook.allOffense().size() >= 20);
        assertTrue(Playbook.allDefense().size() >= 10);
        assertFalse(Playbook.offenseFormations().isEmpty());
        assertTrue(Playbook.offenseByFormation(Formation.SHOTGUN).size() >= 5);
        assertNotNull(Playbook.offenseById("gun_pa_comebacks"));
        assertEquals(OffensePlay.PASS, Playbook.offenseById("gun_pa_comebacks").offensePlay);
        assertNotNull(Playbook.defenseFor(CoverageCall.COVER_3).concept);
        assertTrue(Playbook.offenseById("gun_mesh").concept.length() > 0);
    }

    @Test
    public void deepConceptAveragesMoreYardsThanShort() throws Exception {
        League league = createLeague();
        Team home = league.teamList.get(0);
        Team away = league.teamList.get(1);
        OffenseConcept deep = Playbook.offenseById("gun_four_verts");
        OffenseConcept shortPlay = Playbook.offenseById("gun_slants");
        double deepAvg = avgYards(home, away, deep, 80);
        double shortAvg = avgYards(home, away, shortPlay, 80);
        assertTrue("deep=" + deepAvg + " short=" + shortAvg, deepAvg > shortAvg);
    }

    @Test
    public void aiSuggestReturnsConceptId() throws Exception {
        League league = createLeague();
        Team offense = league.teamList.get(0);
        Team defense = league.teamList.get(1);
        GameState state = new GameState();
        state.gameTime = 2400;
        state.yardLine = 35;
        state.down = 1;
        state.yardsNeed = 10;
        state.possessionHome = true;
        AiPlayCaller ai = new AiPlayCaller(new Random(99L));
        PlayCall call = ai.suggest(offense, defense, state);
        assertNotNull(call.offenseConcept);
        assertNotNull(call.offenseConcept.id);
        assertNotNull(call.defenseConcept);
        assertTrue(call.offenseConcept.family != ConceptFamily.SPECIAL
                || call.offensePlay == OffensePlay.RUN
                || call.offensePlay == OffensePlay.PASS);
    }

    private static double avgYards(Team home, Team away, OffenseConcept concept, int trials) {
        double sum = 0;
        int n = 0;
        for (int i = 0; i < trials; i++) {
            GameState state = new GameState();
            state.gameTime = 2400;
            state.yardLine = 40;
            state.down = 1;
            state.yardsNeed = 10;
            state.possessionHome = true;
            PlayResolver resolver = new PlayResolver(new Random(1000L + i));
            PlayCall call = PlayCall.fromConcepts(
                    concept,
                    Playbook.defenseFor(CoverageCall.COVER_3),
                    TempoCall.NORMAL
            );
            PlayResult r = resolver.resolve(home, away, state, call);
            if (r.playType == OffensePlay.PASS || r.playType == OffensePlay.RUN) {
                sum += r.yardsGained;
                n++;
            }
        }
        return n == 0 ? 0 : sum / n;
    }

    private League createLeague() throws IOException {
        Path csvPath = Paths.get("src/main/assets/fbs_2026.csv");
        if (!Files.exists(csvPath)) {
            csvPath = Paths.get("app/src/main/assets/fbs_2026.csv");
        }
        String csv = new String(Files.readAllBytes(csvPath), StandardCharsets.UTF_8);
        League league = new League(FIRST_NAMES, LAST_NAMES, csv);
        league.userTeam = league.teamList.get(0);
        league.userTeam.userControlled = true;
        return league;
    }
}
