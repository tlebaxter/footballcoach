package CFBsimPack;

import CFBsimPack.engine.AiPlayCaller;
import CFBsimPack.engine.ConceptFamily;
import CFBsimPack.engine.CoverageCall;
import CFBsimPack.engine.DefenseConcept;
import CFBsimPack.engine.DepthBand;
import CFBsimPack.engine.GameState;
import CFBsimPack.engine.OffenseConcept;
import CFBsimPack.engine.OffensePlay;
import CFBsimPack.engine.PlayCall;
import CFBsimPack.engine.TempoCall;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Red-zone, short-yardage, and 2-minute AI play-calling should look more like CFB.
 */
public class AiPlayCallerSituationTest {

    private static final String FIRST_NAMES = "A,B,C,D,E,F,G,H,I,J";
    private static final String LAST_NAMES = "K,L,M,N,O,P,Q,R,S,T";

    @Test
    public void goalLineEarlyDownOftenRuns() throws Exception {
        League league = createLeague();
        Team offense = league.teamList.get(0);
        Team defense = league.teamList.get(1);

        int trials = 100;
        int runs = 0;
        for (int i = 0; i < trials; i++) {
            AiPlayCaller ai = new AiPlayCaller(new Random(4000L + i));
            GameState state = new GameState();
            state.gameTime = 2400;
            state.yardLine = 98;
            state.down = 1;
            state.yardsNeed = 2;
            state.possessionHome = true;
            state.homeScore = 14;
            state.awayScore = 10;
            state.pendingKickoff = false;
            OffenseConcept call = ai.suggestOffense(offense, defense, state);
            if (call.family == ConceptFamily.RUN) {
                runs++;
            }
        }
        assertTrue("expected mostly goal-line runs, got " + runs + "/" + trials, runs >= 55);
    }

    @Test
    public void shortYardageOftenRuns() throws Exception {
        League league = createLeague();
        Team offense = league.teamList.get(0);
        Team defense = league.teamList.get(1);

        int trials = 100;
        int runs = 0;
        for (int i = 0; i < trials; i++) {
            AiPlayCaller ai = new AiPlayCaller(new Random(4100L + i));
            GameState state = new GameState();
            state.gameTime = 2400;
            state.yardLine = 55;
            state.down = 3;
            state.yardsNeed = 1;
            state.possessionHome = true;
            state.homeScore = 14;
            state.awayScore = 10;
            state.pendingKickoff = false;
            OffenseConcept call = ai.suggestOffense(offense, defense, state);
            if (call.family == ConceptFamily.RUN) {
                runs++;
            }
        }
        assertTrue("expected short-yardage runs, got " + runs + "/" + trials, runs >= 50);
    }

    @Test
    public void lateTrailingOftenSpikes() throws Exception {
        League league = createLeague();
        Team offense = league.teamList.get(0);
        Team defense = league.teamList.get(1);

        int trials = 100;
        int spikes = 0;
        for (int i = 0; i < trials; i++) {
            AiPlayCaller ai = new AiPlayCaller(new Random(4200L + i));
            GameState state = new GameState();
            state.gameTime = 12;
            state.yardLine = 70;
            state.down = 1;
            state.yardsNeed = 10;
            state.possessionHome = true;
            state.homeScore = 20;
            state.awayScore = 27;
            state.pendingKickoff = false;
            OffenseConcept call = ai.suggestOffense(offense, defense, state);
            if (call.offensePlay == OffensePlay.SPIKE) {
                spikes++;
            }
        }
        assertTrue("expected frequent spikes late, got " + spikes + "/" + trials, spikes >= 40);
    }

    @Test
    public void lateTrailingHailMaryOftenDeep() throws Exception {
        League league = createLeague();
        Team offense = league.teamList.get(0);
        Team defense = league.teamList.get(1);

        int trials = 100;
        int deep = 0;
        for (int i = 0; i < trials; i++) {
            AiPlayCaller ai = new AiPlayCaller(new Random(4300L + i));
            GameState state = new GameState();
            state.gameTime = 18;
            state.yardLine = 40;
            state.down = 1;
            state.yardsNeed = 10;
            state.possessionHome = true;
            state.homeScore = 14;
            state.awayScore = 21;
            state.pendingKickoff = false;
            OffenseConcept call = ai.suggestOffense(offense, defense, state);
            if (call.depth == DepthBand.DEEP
                    || "gun_four_verts".equals(call.id)
                    || "empty_four_verts".equals(call.id)) {
                deep++;
            }
        }
        assertTrue("expected frequent hail-mary deep shots, got " + deep + "/" + trials,
                deep >= 50);
    }

    @Test
    public void overtimeSuggestUsesNormalTempo() throws Exception {
        League league = createLeague();
        Team offense = league.teamList.get(0);
        Team defense = league.teamList.get(1);

        for (int i = 0; i < 20; i++) {
            AiPlayCaller ai = new AiPlayCaller(new Random(4500L + i));
            GameState state = new GameState();
            state.playingOT = true;
            state.gameTime = -1;
            state.yardLine = 75;
            state.down = 1;
            state.yardsNeed = 10;
            state.possessionHome = true;
            state.homeScore = 24;
            state.awayScore = 24;
            state.pendingKickoff = false;
            PlayCall call = ai.suggest(offense, defense, state);
            assertEquals("OT must not force hurry/chew from gameTime=-1",
                    TempoCall.NORMAL, call.tempo);
        }
    }

    @Test
    public void lateLeadDefenseOftenPreventShell() throws Exception {
        League league = createLeague();
        Team offense = league.teamList.get(0);
        Team defense = league.teamList.get(1);

        int trials = 100;
        int prevent = 0;
        for (int i = 0; i < trials; i++) {
            AiPlayCaller ai = new AiPlayCaller(new Random(4400L + i));
            GameState state = new GameState();
            state.gameTime = 90;
            state.yardLine = 40;
            state.down = 1;
            state.yardsNeed = 10;
            state.possessionHome = true;
            state.homeScore = 14;
            state.awayScore = 21; // away (defense) leading
            state.pendingKickoff = false;
            DefenseConcept call = ai.suggestDefense(offense, defense, state, null);
            CoverageCall cov = call.coverage;
            if (cov == CoverageCall.COVER_2 || cov == CoverageCall.COVER_4
                    || cov == CoverageCall.OFF_COVERAGE) {
                prevent++;
            }
        }
        assertTrue("expected frequent prevent shell, got " + prevent + "/" + trials,
                prevent >= 50);
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
