package CFBsimPack;

import CFBsimPack.engine.AutoSimUntil;
import CFBsimPack.engine.CoverageCall;
import CFBsimPack.engine.OffensePlay;
import CFBsimPack.engine.PlayCall;
import CFBsimPack.engine.AiPlayCaller;
import CFBsimPack.engine.TempoCall;

import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class EngineGreenfieldTest {

    private static final String FIRST_NAMES = "A,B,C,D,E,F,G,H,I,J";
    private static final String LAST_NAMES = "K,L,M,N,O,P,Q,R,S,T";

    @Test
    public void nickelHasFiveDbs() {
        assertEquals(5, DefensiveSystem.NICKEL.dbCount());
        assertEquals(5, DefensiveSystem.FOUR_TWO_FIVE.dbCount());
        assertEquals(6, DefensiveSystem.DIME.dbCount());
        assertEquals(11, DefensiveSystem.BASE_3_4.slots.length);
    }

    @Test
    public void onFieldElevenFillsEleven() throws Exception {
        League league = createLeague();
        Team t = league.teamList.get(0);
        t.setDefSystem(DefensiveSystem.NICKEL);
        OnFieldEleven def = OnFieldEleven.forDefense(t);
        assertEquals(11, def.players.size());
        int dbs = 0;
        for (int i = 0; i < def.roles.size(); i++) {
            PositionGroup g = def.roles.get(i).preferredGroup();
            if (g == PositionGroup.CB || g == PositionGroup.S) dbs++;
        }
        assertEquals(5, dbs);
    }

    @Test
    public void playGameCompletes() throws Exception {
        League league = createLeague();
        Team home = league.teamList.get(0);
        Team away = league.teamList.get(1);
        Game g = new Game(home, away);
        g.setRandom(new Random(42L));
        g.playGame();
        assertTrue(g.hasPlayed);
        assertTrue(home.gameWLSchedule.size() >= 1 || away.gameWLSchedule.size() >= 1);
    }

    @Test
    public void spikeBurnsClockAndAdvancesDown() throws Exception {
        League league = createLeague();
        Game g = new Game(league.teamList.get(0), league.teamList.get(1));
        g.setRandom(new Random(7L));
        g.startGame();
        int timeBefore = g.state.gameTime;
        int downBefore = g.state.down;
        g.executeSnap(new PlayCall(OffensePlay.SPIKE, Formation.SHOTGUN, CoverageCall.COVER_3, TempoCall.NORMAL));
        assertTrue(g.state.gameTime < timeBefore);
        assertEquals(downBefore + 1, g.state.down);
    }

    @Test
    public void timeoutDecrements() throws Exception {
        League league = createLeague();
        Game g = new Game(league.teamList.get(0), league.teamList.get(1));
        g.setRandom(new Random(3L));
        g.startGame();
        assertTrue(g.callTimeout(true));
        assertEquals(2, g.state.homeTimeouts);
    }

    @Test
    public void autoSimPossessionProgresses() throws Exception {
        League league = createLeague();
        Game g = new Game(league.teamList.get(0), league.teamList.get(1));
        g.setRandom(new Random(99L));
        g.startGame();
        boolean start = g.state.possessionHome;
        int timeStart = g.state.gameTime;
        g.autoSimUntil(AutoSimUntil.POSSESSION);
        assertTrue(g.state.possessionHome != start
                || g.state.homeScore + g.state.awayScore > 0
                || g.state.gameTime < timeStart);
    }

    @Test
    public void airRaidPassesMoreThanPowerRun() throws Exception {
        League league = createLeague();
        Team air = league.teamList.get(0);
        Team power = league.teamList.get(1);
        Team defense = league.teamList.get(2);
        air.setOffPhilosophy(OffensivePhilosophy.AIR_RAID);
        power.setOffPhilosophy(OffensivePhilosophy.POWER_RUN);

        int airPass = countPassCalls(air, defense, 200, 11L);
        int powerPass = countPassCalls(power, defense, 200, 11L);
        assertTrue("Air Raid pass calls " + airPass + " should exceed Power Run " + powerPass,
                airPass > powerPass);
    }

    private int countPassCalls(Team offense, Team defense, int snaps, long seed) {
        Game g = new Game(offense, defense);
        g.setRandom(new Random(seed));
        g.startGame();
        g.state.possessionHome = true;
        AiPlayCaller ai = new AiPlayCaller(new Random(seed));
        int passes = 0;
        for (int i = 0; i < snaps; i++) {
            g.state.down = 1;
            g.state.yardsNeed = 10;
            g.state.yardLine = 40;
            g.state.gameTime = 2000;
            PlayCall c = ai.choose(offense, defense, g.state);
            if (c.offensePlay == OffensePlay.PASS) passes++;
        }
        return passes;
    }

    @Test
    public void oldSaveRejectedWithoutVersion() throws Exception {
        File tmp = File.createTempFile("oldsave", ".txt");
        try {
            FileWriter w = new FileWriter(tmp);
            w.write("2026: ABC (0-0) 0 CCs, 0 NCs>[EASY]%\n");
            w.write("END_LEAGUE_HIST\n");
            w.write("END_HEISMAN_HIST\n");
            w.write("TEAM_COUNT,1\n");
            w.close();
            try {
                new League(tmp, FIRST_NAMES, LAST_NAMES);
                fail("Expected exception for old save");
            } catch (AssertionError ae) {
                throw ae;
            } catch (Throwable expected) {
                // League may wrap IOException; any failure loading pre-v2 is success
                assertTrue(expected instanceof Exception || expected instanceof Error);
            }
        } finally {
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
        }
    }

    private League createLeague() throws IOException {
        Path csvPath = Paths.get("src/main/assets/fbs_2026.csv");
        if (!Files.exists(csvPath)) {
            csvPath = Paths.get("app/src/main/assets/fbs_2026.csv");
        }
        String csv = new String(Files.readAllBytes(csvPath), StandardCharsets.UTF_8);
        League league = new League(FIRST_NAMES, LAST_NAMES, csv, false);
        league.userTeam = league.teamList.get(0);
        league.userTeam.userControlled = true;
        return league;
    }
}
