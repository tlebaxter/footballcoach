package CFBsimPack;

import CFBsimPack.engine.CoverageCall;
import CFBsimPack.engine.GameSituation;
import CFBsimPack.engine.GameState;
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GameSituationLabelsTest {

    private static final String FIRST_NAMES = "A,B,C,D,E,F,G,H,I,J";
    private static final String LAST_NAMES = "K,L,M,N,O,P,Q,R,S,T";

    @Test
    public void ballOnUsesTerritoryAbbrOwnOppAndMidfield() throws Exception {
        League league = createLeague();
        Team home = league.teamList.get(0);
        Team away = league.teamList.get(1);
        Game g = liveScrimmage(home, away);

        g.state.possessionHome = true;
        g.state.yardLine = 25;
        GameSituation sit = g.getSituation();
        assertEquals(home.abbr + " 25", sit.ballOnLabel);
        assertEquals(home.abbr, sit.possessionAbbr);

        g.state.yardLine = 75;
        sit = g.getSituation();
        assertEquals(away.abbr + " 25", sit.ballOnLabel);

        g.state.yardLine = 50;
        sit = g.getSituation();
        assertEquals("50", sit.ballOnLabel);

        g.state.possessionHome = false;
        g.state.yardLine = 30;
        sit = g.getSituation();
        assertEquals(away.abbr + " 30", sit.ballOnLabel);
        assertEquals(away.abbr, sit.possessionAbbr);
    }

    @Test
    public void downLabelUsesGoalWhenLineToGainIsEndZone() throws Exception {
        League league = createLeague();
        Game g = liveScrimmage(league.teamList.get(0), league.teamList.get(1));
        g.state.yardLine = 92;
        g.state.yardsNeed = 10;
        g.state.down = 1;
        GameSituation sit = g.getSituation();
        assertEquals("1st & Goal", sit.downLabel);
        assertEquals(100, sit.firstDownYard);
        assertTrue(sit.downDistanceLabel.contains("1st & Goal"));
        assertTrue(sit.downDistanceLabel.contains(sit.ballOnLabel));

        g.state.yardLine = 40;
        g.state.yardsNeed = 10;
        g.state.down = 3;
        sit = g.getSituation();
        assertEquals("3rd & 10", sit.downLabel);
        assertEquals(50, sit.firstDownYard);
    }

    @Test
    public void specialPhasesDoNotExposeScrimmageFirstDown() throws Exception {
        League league = createLeague();
        Team home = league.teamList.get(0);
        Team away = league.teamList.get(1);
        Game g = new Game(home, away);
        g.setRandom(noFoulRandom());
        g.startGame();
        g.state.homeWonToss = true;
        g.state.awaitingCoinToss = true;
        g.state.tossResolved = false;
        g.state.pendingKickoff = false;

        GameSituation sit = g.getSituation();
        assertEquals("Coin toss", sit.downLabel);
        assertEquals("—", sit.ballOnLabel);
        assertEquals(-1, sit.firstDownYard);
        assertEquals("—", sit.driveSummary);

        g.autoResolveCoinToss();
        sit = g.getSituation();
        assertEquals("Kickoff", sit.downLabel);
        assertEquals(-1, sit.firstDownYard);
        assertTrue(sit.downDistanceLabel.contains("pending"));
        assertEquals("—", sit.driveSummary);

        settleOpeningKickoff(g);
        g.state.pendingTry = true;
        g.state.tryAwaitingChoice = true;
        g.state.tryIsTwoPoint = false;
        g.state.yardLine = 97;
        sit = g.getSituation();
        assertEquals("PAT / 2-Point", sit.downLabel);
        assertEquals(-1, sit.firstDownYard);
    }

    @Test
    public void clockStatusAndTimeoutsMaxReflectState() throws Exception {
        League league = createLeague();
        Game g = liveScrimmage(league.teamList.get(0), league.teamList.get(1));

        g.state.clockRunning = true;
        g.state.pendingTenSecondRunoff = false;
        assertEquals("RUNNING", g.getSituation().clockStatusLabel);

        g.state.clockRunning = false;
        assertEquals("STOPPED", g.getSituation().clockStatusLabel);

        g.state.pendingTenSecondRunoff = true;
        assertEquals("10S RUNOFF", g.getSituation().clockStatusLabel);
        assertEquals(GameState.TIMEOUTS_PER_HALF, g.getSituation().timeoutsMax);

        g.state.playingOT = true;
        g.state.pendingTenSecondRunoff = false;
        assertEquals(GameState.TIMEOUTS_PER_OT, g.getSituation().timeoutsMax);
    }

    @Test
    public void driveCountersResetAndAccumulateAcrossSnaps() throws Exception {
        League league = createLeague();
        Game g = liveScrimmage(league.teamList.get(0), league.teamList.get(1));
        g.state.possessionHome = true;
        g.state.yardLine = 40;
        g.state.down = 1;
        g.state.yardsNeed = 10;
        g.state.clockRunning = false;
        g.state.resetDriveStats();

        GameSituation before = g.getSituation();
        assertEquals(0, before.drivePlayCount);
        assertEquals(0, before.driveNetYards);
        assertEquals(0, before.driveTimeOfPossessionSec);
        assertEquals("0 plays, 0 yds, 0:00", before.driveSummary);

        g.executeSnap(new PlayCall(OffensePlay.SPIKE, Formation.SHOTGUN, CoverageCall.COVER_3, TempoCall.NORMAL));
        GameSituation afterSpike = g.getSituation();
        assertEquals(1, afterSpike.drivePlayCount);
        assertTrue(afterSpike.driveTimeOfPossessionSec >= 3);
        assertTrue(afterSpike.driveSummary.startsWith("1 play,"));

        g.executeSnap(new PlayCall(OffensePlay.KNEEL, Formation.SHOTGUN, CoverageCall.COVER_3, TempoCall.NORMAL));
        GameSituation afterKneel = g.getSituation();
        assertEquals(2, afterKneel.drivePlayCount);
        assertTrue(afterKneel.driveTimeOfPossessionSec > afterSpike.driveTimeOfPossessionSec);

        // Force a new drive — counters clear
        g.state.yardLine = 35;
        // Use package-visible path: flip via turnover-on-downs style reset
        int playsBeforeFlip = g.state.drivePlayCount;
        assertTrue(playsBeforeFlip >= 2);
        // Simulate possession change the same way Game does
        g.state.possessionHome = !g.state.possessionHome;
        g.state.yardLine = 100 - g.state.yardLine;
        g.state.down = 1;
        g.state.yardsNeed = 10;
        // resetDrive is private — exercise via pending kickoff helper path
        g.state.pendingKickoff = true;
        g.state.freeKick = false;
        g.state.yardLine = 35;
        g.state.down = 1;
        g.state.yardsNeed = 10;
        // kickoffAfterScore calls resetDrive; mirror by executing kickoff settle after manual reset
        // Directly clear via public GameState API then assert situation
        g.state.resetDriveStats();
        GameSituation resetSit = g.getSituation();
        assertEquals(0, resetSit.drivePlayCount);
        assertEquals(0, resetSit.driveNetYards);
        assertEquals(0, resetSit.driveTimeOfPossessionSec);
        assertEquals("—", resetSit.driveSummary); // kickoff pending special phase
    }

    @Test
    public void runoffAddsToDriveTimeOfPossession() throws Exception {
        League league = createLeague();
        Game g = liveScrimmage(league.teamList.get(0), league.teamList.get(1));
        g.state.clockRunning = false;
        g.state.resetDriveStats();
        g.executeSnap(new PlayCall(OffensePlay.KNEEL, Formation.SHOTGUN, CoverageCall.COVER_3, TempoCall.NORMAL));
        assertTrue(g.state.clockRunning);
        int topAfterKneel = g.state.driveTimeOfPossessionSec;

        g.executeSnap(new PlayCall(OffensePlay.SPIKE, Formation.SHOTGUN, CoverageCall.COVER_3, TempoCall.NORMAL));
        int expectedMin = topAfterKneel + TempoCall.NORMAL.runoffSeconds() + 3;
        assertEquals(expectedMin, g.state.driveTimeOfPossessionSec);
        assertFalse(g.getSituation().driveSummary.equals("—"));
    }

    private Game liveScrimmage(Team home, Team away) throws Exception {
        Game g = new Game(home, away);
        g.setRandom(noFoulRandom());
        g.startGame();
        settleOpeningKickoff(g);
        g.state.pendingKickoff = false;
        g.state.clearTry();
        g.state.awaitingCoinToss = false;
        return g;
    }

    private static void settleOpeningKickoff(Game g) {
        if (g.state != null && g.state.awaitingCoinToss) {
            g.autoResolveCoinToss();
        }
        if (g.state != null && g.state.pendingKickoff) {
            g.executeSnap(null);
        }
        assertTrue(g.state != null && !g.state.pendingKickoff);
    }

    private static Random noFoulRandom() {
        return new Random() {
            @Override
            public double nextDouble() {
                return 0.99;
            }
        };
    }

    private League createLeague() throws IOException {
        String csv = achijones.footballcoach.testing.FbsCsv.read();
        League league = new League(FIRST_NAMES, LAST_NAMES, csv);
        league.userTeam = league.teamList.get(0);
        league.userTeam.userControlled = true;
        return league;
    }
}
