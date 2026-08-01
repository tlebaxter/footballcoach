package CFBsimPack;

import CFBsimPack.engine.AutoSimUntil;
import CFBsimPack.engine.CoverageCall;
import CFBsimPack.engine.GamePhase;
import CFBsimPack.engine.GameSituation;
import CFBsimPack.engine.OffenseConcept;
import CFBsimPack.engine.OffensePlay;
import CFBsimPack.engine.PlayCall;
import CFBsimPack.engine.AiPlayCaller;
import CFBsimPack.engine.Playbook;
import CFBsimPack.engine.TempoCall;
import CFBsimPack.engine.TimeoutCoachTips;

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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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
        assertTrue(g.isDecided());
        assertTrue(home.gameWLSchedule.size() >= 1 || away.gameWLSchedule.size() >= 1);
    }

    @Test
    public void spikeBurnsClockAndAdvancesDown() throws Exception {
        League league = createLeague();
        Game g = new Game(league.teamList.get(0), league.teamList.get(1));
        g.setRandom(noFoulRandom());
        g.startGame();
        settleOpeningKickoff(g);
        int timeBefore = g.state.gameTime;
        int downBefore = g.state.down;
        g.executeSnap(new PlayCall(OffensePlay.SPIKE, Formation.SHOTGUN, CoverageCall.COVER_3, TempoCall.NORMAL));
        assertTrue(g.state.gameTime < timeBefore);
        assertEquals(downBefore + 1, g.state.down);
        assertFalse(g.state.clockRunning);
    }

    @Test
    public void liveBallDefersRunoffUntilNextSnap() throws Exception {
        League league = createLeague();
        Game g = new Game(league.teamList.get(0), league.teamList.get(1));
        g.setRandom(noFoulRandom());
        g.startGame();
        settleOpeningKickoff(g);
        g.executeSnap(new PlayCall(OffensePlay.KNEEL, Formation.SHOTGUN, CoverageCall.COVER_3, TempoCall.NORMAL));
        assertTrue(g.state.clockRunning);
        int afterKneel = g.state.gameTime;

        g.executeSnap(new PlayCall(OffensePlay.SPIKE, Formation.SHOTGUN, CoverageCall.COVER_3, TempoCall.NORMAL));
        assertEquals(afterKneel - TempoCall.NORMAL.runoffSeconds() - 3, g.state.gameTime);
        assertFalse(g.state.clockRunning);
    }

    @Test
    public void timeoutCancelsPendingRunoff() throws Exception {
        League league = createLeague();
        Game g = new Game(league.teamList.get(0), league.teamList.get(1));
        g.setRandom(noFoulRandom());
        g.startGame();
        settleOpeningKickoff(g);
        g.executeSnap(new PlayCall(OffensePlay.KNEEL, Formation.SHOTGUN, CoverageCall.COVER_3, TempoCall.NORMAL));
        assertTrue(g.state.clockRunning);
        assertTrue(g.callTimeout(true));
        assertFalse(g.state.clockRunning);
        int afterTimeout = g.state.gameTime;

        g.executeSnap(new PlayCall(OffensePlay.SPIKE, Formation.SHOTGUN, CoverageCall.COVER_3, TempoCall.NORMAL));
        assertEquals(afterTimeout - 3, g.state.gameTime);
    }

    @Test
    public void stoppedClockSkipsRunoffOnNextSnap() throws Exception {
        League league = createLeague();
        Game g = new Game(league.teamList.get(0), league.teamList.get(1));
        g.setRandom(noFoulRandom());
        g.startGame();
        settleOpeningKickoff(g);
        g.executeSnap(new PlayCall(OffensePlay.SPIKE, Formation.SHOTGUN, CoverageCall.COVER_3, TempoCall.NORMAL));
        assertFalse(g.state.clockRunning);
        int afterSpike = g.state.gameTime;

        g.executeSnap(new PlayCall(OffensePlay.SPIKE, Formation.SHOTGUN, CoverageCall.COVER_3, TempoCall.NORMAL));
        assertEquals(afterSpike - 3, g.state.gameTime);
    }

    @Test
    public void hurryUpUsesShorterDeferredRunoff() throws Exception {
        League league = createLeague();
        Game g = new Game(league.teamList.get(0), league.teamList.get(1));
        g.setRandom(noFoulRandom());
        g.startGame();
        settleOpeningKickoff(g);
        g.executeSnap(new PlayCall(OffensePlay.KNEEL, Formation.SHOTGUN, CoverageCall.COVER_3, TempoCall.NORMAL));
        int afterKneel = g.state.gameTime;

        g.executeSnap(new PlayCall(OffensePlay.SPIKE, Formation.SHOTGUN, CoverageCall.COVER_3, TempoCall.HURRY_UP));
        assertEquals(afterKneel - TempoCall.HURRY_UP.runoffSeconds() - 3, g.state.gameTime);
    }

    @Test
    public void delayOfGameIsPreSnapDeadBall() throws Exception {
        League league = createLeague();
        Game g = new Game(league.teamList.get(0), league.teamList.get(1));
        g.setRandom(noFoulRandom());
        g.startGame();
        settleOpeningKickoff(g);
        g.setRandom(alwaysFoulRandom());
        g.state.clockRunning = false; // no runoff before DOG
        g.state.yardLine = 40;
        g.state.yardsNeed = 10;
        g.state.down = 2;
        int yardBefore = g.state.yardLine;
        int downBefore = g.state.down;

        g.executeSnap(new PlayCall(OffensePlay.KNEEL, Formation.SHOTGUN, CoverageCall.COVER_3, TempoCall.CHEW_CLOCK));
        assertEquals(yardBefore - 5, g.state.yardLine);
        assertEquals(downBefore, g.state.down);
        assertFalse(g.state.clockRunning);
        assertTrue(g.state.lastPlayLog.toUpperCase().contains("DELAY OF GAME"));
    }

    @Test
    public void acceptedFoulBurnsNoPlayClockAndStopsRunoff() throws Exception {
        League league = createLeague();
        Game g = new Game(league.teamList.get(0), league.teamList.get(1));
        g.setRandom(noFoulRandom());
        g.startGame();
        settleOpeningKickoff(g);
        g.setRandom(forceFalseStartOnSpikeRandom());
        g.state.clockRunning = false;
        g.state.yardLine = 40;
        g.state.yardsNeed = 10;
        g.state.down = 2;
        int timeBefore = g.state.gameTime;

        g.executeSnap(new PlayCall(OffensePlay.SPIKE, Formation.SHOTGUN, CoverageCall.COVER_3, TempoCall.NORMAL));
        assertTrue(g.state.lastPlayLog.contains("PENALTY"));
        assertTrue(g.state.lastPlayLog.contains("FALSE START"));
        assertFalse(g.state.lastPlayLog.contains("DPI"));
        // Dead-ball foul: no spike 3s burn
        assertEquals(timeBefore, g.state.gameTime);
        assertFalse(g.state.clockRunning);
    }

    @Test
    public void runPlaysNeverLogDpi() throws Exception {
        League league = createLeague();
        Game g = new Game(league.teamList.get(0), league.teamList.get(1));
        g.setRandom(new Random(99L));
        g.startGame();
        settleOpeningKickoff(g);
        for (int i = 0; i < 60; i++) {
            if (g.state.gameOver) break;
            if (g.state.pendingKickoff) {
                g.executeSnap(null);
                continue;
            }
            if (g.state.pendingTry) {
                g.autoResolveTryIfNeeded(true);
                continue;
            }
            g.state.down = 1;
            g.state.yardsNeed = 10;
            if (g.state.yardLine < 10 || g.state.yardLine > 90) g.state.yardLine = 40;
            g.state.clockRunning = false;
            g.executeSnap(new PlayCall(OffensePlay.RUN, Formation.I_FORM, CoverageCall.COVER_3, TempoCall.NORMAL));
            String log = g.state.lastPlayLog != null ? g.state.lastPlayLog : "";
            assertFalse("DPI must not appear on RUN: " + log, log.contains("DPI"));
            assertFalse("Roughing must not appear on RUN: " + log, log.contains("ROUGHING"));
        }
    }

    @Test
    public void tenSecondRunoffAppliesUnlessTimeout() throws Exception {
        League league = createLeague();
        Game g = new Game(league.teamList.get(0), league.teamList.get(1));
        g.setRandom(noFoulRandom());
        g.startGame();
        settleOpeningKickoff(g);
        g.state.gameTime = 45; // Q4 under 1:00
        g.state.halfUnderway = true;
        g.state.pendingTenSecondRunoff = true;
        g.state.clockRunning = false;
        int before = g.state.gameTime;

        g.executeSnap(new PlayCall(OffensePlay.SPIKE, Formation.SHOTGUN, CoverageCall.COVER_3, TempoCall.NORMAL));
        assertEquals(before - 10 - 3, g.state.gameTime);
        assertFalse(g.state.pendingTenSecondRunoff);

        g.state.gameTime = 50;
        g.state.pendingTenSecondRunoff = true;
        g.state.homeTimeouts = 2;
        assertTrue(g.callTimeout(true));
        assertFalse(g.state.pendingTenSecondRunoff);
        int afterTo = g.state.gameTime;
        g.executeSnap(new PlayCall(OffensePlay.SPIKE, Formation.SHOTGUN, CoverageCall.COVER_3, TempoCall.NORMAL));
        assertEquals(afterTo - 3, g.state.gameTime);
    }

    @Test
    public void tenSecondRunoffTipIsHighestPriority() throws Exception {
        League league = createLeague();
        Game g = new Game(league.teamList.get(0), league.teamList.get(1));
        g.setRandom(noFoulRandom());
        g.startGame();
        settleOpeningKickoff(g);
        g.state.halfUnderway = true;
        g.state.possessionHome = true;
        g.state.homeTimeouts = 3;
        g.state.gameTime = 20;
        g.state.clockRunning = true;
        g.state.pendingTenSecondRunoff = true;
        TimeoutCoachTips.Tip tip = TimeoutCoachTips.suggest(g.getSituation(), TempoCall.NORMAL);
        assertNotNull(tip);
        assertEquals(TimeoutCoachTips.TipId.TEN_SECOND_RUNOFF, tip.id);
        assertTrue(tip.message.contains("10-second"));
    }

    @Test
    public void ejectedPlayerSkippedOnField() throws Exception {
        League league = createLeague();
        Team t = league.teamList.get(0);
        OnFieldEleven before = OnFieldEleven.forDefense(t);
        Player starter = before.players.get(0);
        starter.isEjected = true;
        OnFieldEleven after = OnFieldEleven.forDefense(t);
        assertFalse(after.players.contains(starter));
        starter.isEjected = false;
    }

    @Test
    public void timeoutTipsCoverLateGameAndEndOfHalf() throws Exception {
        League league = createLeague();
        Game g = new Game(league.teamList.get(0), league.teamList.get(1));
        g.setRandom(noFoulRandom());
        g.startGame();
        settleOpeningKickoff(g);
        g.state.halfUnderway = true;
        g.state.possessionHome = true;
        g.state.homeTimeouts = 3;
        g.state.homeScore = 10;
        g.state.awayScore = 17;

        g.state.gameTime = 20;
        g.state.clockRunning = true;
        GameSituation sit = g.getSituation();
        TimeoutCoachTips.Tip tip = TimeoutCoachTips.suggest(sit, TempoCall.NORMAL);
        assertNotNull(tip);
        assertEquals(TimeoutCoachTips.TipId.RUNOFF_EXPIRES, tip.id);

        g.state.gameTime = 1830; // Q2 ~0:30
        g.state.clockRunning = true;
        tip = TimeoutCoachTips.suggest(g.getSituation(), TempoCall.NORMAL);
        assertNotNull(tip);
        assertEquals(TimeoutCoachTips.TipId.END_OF_HALF, tip.id);

        g.state.gameTime = 90;
        g.state.clockRunning = true;
        tip = TimeoutCoachTips.suggest(g.getSituation(), TempoCall.NORMAL);
        assertNotNull(tip);
        assertEquals(TimeoutCoachTips.TipId.LATE_GAME_TRAILING, tip.id);

        g.state.clockRunning = false;
        g.state.homeTimeouts = 0;
        assertFalse(g.getSituation().canCallTimeout);
        assertEquals(null, TimeoutCoachTips.suggest(g.getSituation(), TempoCall.CHEW_CLOCK));
    }

    @Test
    public void timeoutFollowsCollegeRules() throws Exception {
        League league = createLeague();
        Game g = new Game(league.teamList.get(0), league.teamList.get(1));
        g.setRandom(new Random(3L));
        g.startGame();

        // Coin toss / before opening kickoff: cannot burn timeouts
        g.state.homeWonToss = true;
        g.state.awaitingCoinToss = true;
        g.state.tossResolved = false;
        g.state.pendingKickoff = false;
        g.state.halfUnderway = false;
        assertFalse(g.callTimeout(true));
        assertEquals(3, g.state.homeTimeouts);

        g.autoResolveCoinToss();
        assertTrue(g.state.pendingKickoff);
        assertFalse(g.state.halfUnderway);
        assertFalse(g.callTimeout(true));
        assertEquals(3, g.state.homeTimeouts);

        // After opening kickoff: timeouts available
        settleOpeningKickoff(g);
        assertTrue(g.state.halfUnderway);
        assertTrue(g.callTimeout(true));
        assertEquals(2, g.state.homeTimeouts);
        assertTrue(g.getSituation().canCallTimeout);

        // Exhaust remaining timeouts
        assertTrue(g.callTimeout(true));
        assertTrue(g.callTimeout(true));
        assertEquals(0, g.state.homeTimeouts);
        assertFalse(g.callTimeout(true));
        assertFalse(g.getSituation().canCallTimeout);

        // Halftime resets to 3 and blocks until second-half kickoff
        g.state.homeTimeouts = 0;
        g.state.awayTimeouts = 1;
        g.state.pendingKickoff = false;
        g.state.pendingTry = false;
        g.state.yardLine = 40;
        g.state.down = 1;
        g.state.yardsNeed = 10;
        g.state.gameTime = 1805;
        assertEquals(2, g.state.quarter());
        g.state.clockRunning = true; // deferred runoff crosses into Q3
        g.setRandom(noFoulRandom());
        g.executeSnap(new PlayCall(OffensePlay.SPIKE, Formation.SHOTGUN, CoverageCall.COVER_3, TempoCall.NORMAL));
        assertTrue(g.state.quarter() >= 3);
        assertFalse(g.state.halfUnderway);
        assertEquals(3, g.state.homeTimeouts);
        assertEquals(3, g.state.awayTimeouts);
        assertTrue(g.state.pendingKickoff);
        assertFalse(g.callTimeout(true));

        g.executeSnap(null); // second-half kickoff
        assertTrue(g.state.halfUnderway);
        assertTrue(g.callTimeout(true));
        assertEquals(2, g.state.homeTimeouts);

        // OT: 1 timeout each; call once then blocked; new OT period restores 1
        g.state.homeScore = 14;
        g.state.awayScore = 14;
        g.state.homeTimeouts = 2;
        g.state.awayTimeouts = 2;
        g.state.pendingKickoff = false;
        g.state.pendingTry = false;
        g.state.yardLine = 40;
        g.state.down = 1;
        g.state.yardsNeed = 10;
        g.state.gameTime = 0;
        g.executeSnap(null);
        assertTrue(g.state.playingOT);
        assertEquals(1, g.state.homeTimeouts);
        assertEquals(1, g.state.awayTimeouts);
        assertTrue(g.state.halfUnderway);
        assertTrue(g.callTimeout(true));
        assertEquals(0, g.state.homeTimeouts);
        assertFalse(g.callTimeout(true));

        g.state.bottomOT = true;
        g.state.homeScore = 14;
        g.state.awayScore = 14;
        g.state.down = 5;
        g.state.pendingKickoff = false;
        g.state.pendingTry = false;
        g.executeSnap(null);
        assertTrue(g.state.playingOT);
        assertTrue(g.state.numOT >= 2);
        assertEquals(1, g.state.homeTimeouts);
        assertEquals(1, g.state.awayTimeouts);
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

    @Test
    public void bearStacksBoxMoreThanDime() throws Exception {
        League league = createLeague();
        Team bear = league.teamList.get(0);
        Team dime = league.teamList.get(1);
        bear.setDefSystem(DefensiveSystem.BEAR_46);
        dime.setDefSystem(DefensiveSystem.DIME);
        int bearStack = countStackBox(bear, 250, 21L);
        int dimeStack = countStackBox(dime, 250, 21L);
        assertTrue("Bear STACK_BOX " + bearStack + " should exceed Dime " + dimeStack,
                bearStack > dimeStack);
    }

    @Test
    public void buildMatchedCallFillsMissingDefense() throws Exception {
        League league = createLeague();
        Game g = new Game(league.teamList.get(0), league.teamList.get(1));
        g.setRandom(new Random(5L));
        g.startGame();
        settleOpeningKickoff(g);
        OffenseConcept off = Playbook.offenseById("gun_slants");
        PlayCall call = g.buildMatchedCall(off, null, TempoCall.NORMAL);
        assertNotNull(call.offenseConcept);
        assertNotNull(call.defenseConcept);
        assertEquals("gun_slants", call.offenseConcept.id);
    }

    @Test
    public void touchdownPresentsTryThenKickXp() throws Exception {
        League league = createLeague();
        Team home = league.teamList.get(0);
        Team away = league.teamList.get(1);
        home.userControlled = true;
        away.userControlled = false;
        Game g = new Game(home, away);
        g.setRandom(new Random(8L));
        g.startGame();
        settleOpeningKickoff(g);
        g.state.possessionHome = true;
        g.state.yardLine = 99;
        g.state.down = 1;
        g.state.yardsNeed = 1;
        g.state.pendingKickoff = false;
        // Force a rush that should score from the 1
        int guard = 0;
        while (!g.state.pendingTry && guard++ < 40) {
            g.state.yardLine = 99;
            g.state.down = 1;
            g.state.yardsNeed = 1;
            g.state.pendingKickoff = false;
            g.state.clearTry();
            g.executeSnap(PlayCall.fromConcepts(
                    Playbook.offenseById("i_dive"),
                    Playbook.defenseFor(CoverageCall.COVER_3),
                    TempoCall.NORMAL));
        }
        assertTrue("expected a TD to open a try", g.state.pendingTry);
        assertTrue(g.state.tryAwaitingChoice);
        int scoreBefore = g.state.homeScore;
        assertTrue(g.chooseKickXp());
        assertFalse(g.state.pendingTry);
        assertTrue(g.state.pendingKickoff || g.state.playingOT || g.state.gameOver);
        assertTrue(g.state.homeScore >= scoreBefore); // XP may miss
    }

    @Test
    public void otGameWinningTdSkipsTry() throws Exception {
        League league = createLeague();
        Team home = league.teamList.get(0);
        Team away = league.teamList.get(1);
        home.userControlled = true;
        away.userControlled = false;
        Game g = new Game(home, away);
        g.setRandom(new Random(8L));
        g.startGame();
        settleOpeningKickoff(g);

        g.state.playingOT = true;
        g.state.phase = GamePhase.OT;
        g.state.bottomOT = true;
        g.state.numOT = 1;
        g.state.gameTime = -1;
        g.state.homeScore = 17;
        g.state.awayScore = 17;
        g.state.possessionHome = true;
        g.state.pendingKickoff = false;
        g.state.clearTry();

        int guard = 0;
        while (!g.state.gameOver && !g.state.pendingTry && guard++ < 40) {
            g.state.playingOT = true;
            g.state.bottomOT = true;
            g.state.homeScore = 17;
            g.state.awayScore = 17;
            g.state.possessionHome = true;
            g.state.yardLine = 99;
            g.state.down = 1;
            g.state.yardsNeed = 1;
            g.state.pendingKickoff = false;
            g.state.clearTry();
            g.executeSnap(PlayCall.fromConcepts(
                    Playbook.offenseById("i_dive"),
                    Playbook.defenseFor(CoverageCall.COVER_3),
                    TempoCall.NORMAL));
        }
        assertTrue("expected game-winning OT TD", g.state.gameOver);
        assertFalse(g.state.pendingTry);
        assertFalse(g.state.tryAwaitingChoice);
        assertEquals(23, g.state.homeScore);
        assertEquals(17, g.state.awayScore);
    }

    @Test
    public void otTdThatOnlyTiesStillPresentsTry() throws Exception {
        League league = createLeague();
        Team home = league.teamList.get(0);
        Team away = league.teamList.get(1);
        home.userControlled = true;
        away.userControlled = false;
        Game g = new Game(home, away);
        g.setRandom(new Random(8L));
        g.startGame();
        settleOpeningKickoff(g);

        // Away led by 7; home TD ties — XP/2 still needed
        g.state.playingOT = true;
        g.state.phase = GamePhase.OT;
        g.state.bottomOT = true;
        g.state.numOT = 1;
        g.state.gameTime = -1;
        g.state.homeScore = 10;
        g.state.awayScore = 17;
        g.state.possessionHome = true;
        g.state.pendingKickoff = false;
        g.state.clearTry();

        int guard = 0;
        while (!g.state.pendingTry && guard++ < 40) {
            g.state.playingOT = true;
            g.state.bottomOT = true;
            g.state.gameOver = false;
            g.state.homeScore = 10;
            g.state.awayScore = 17;
            g.state.possessionHome = true;
            g.state.yardLine = 99;
            g.state.down = 1;
            g.state.yardsNeed = 1;
            g.state.pendingKickoff = false;
            g.state.clearTry();
            g.executeSnap(PlayCall.fromConcepts(
                    Playbook.offenseById("i_dive"),
                    Playbook.defenseFor(CoverageCall.COVER_3),
                    TempoCall.NORMAL));
        }
        assertTrue("expected a try after OT TD that only ties", g.state.pendingTry);
        assertTrue(g.state.tryAwaitingChoice);
        assertFalse(g.state.gameOver);
        assertEquals(16, g.state.homeScore);
        assertEquals(17, g.state.awayScore);
    }

    @Test
    public void autoSimDoesNotStallOnTry() throws Exception {
        League league = createLeague();
        Team home = league.teamList.get(0);
        Team away = league.teamList.get(1);
        home.userControlled = false;
        away.userControlled = false;
        Game g = new Game(home, away);
        g.setRandom(new Random(44L));
        g.playGame();
        assertTrue(g.hasPlayed);
    }

    @Test
    public void autoSimResolvesUserTryChoice() throws Exception {
        League league = createLeague();
        Team home = league.teamList.get(0);
        Team away = league.teamList.get(1);
        home.userControlled = true;
        away.userControlled = false;
        Game g = new Game(home, away);
        g.setRandom(new Random(8L));
        g.startGame();
        settleOpeningKickoff(g);
        g.state.possessionHome = true;
        g.state.yardLine = 99;
        g.state.down = 1;
        g.state.yardsNeed = 1;
        g.state.pendingKickoff = false;
        int guard = 0;
        while (!g.state.pendingTry && guard++ < 40) {
            g.state.yardLine = 99;
            g.state.down = 1;
            g.state.yardsNeed = 1;
            g.state.pendingKickoff = false;
            g.state.clearTry();
            g.executeSnap(PlayCall.fromConcepts(
                    Playbook.offenseById("i_dive"),
                    Playbook.defenseFor(CoverageCall.COVER_3),
                    TempoCall.NORMAL));
        }
        assertTrue("expected a TD to open a try", g.state.pendingTry);
        assertTrue(g.state.tryAwaitingChoice);
        assertTrue(g.getSituation().userChoosesTry);

        g.autoSimUntil(AutoSimUntil.POSSESSION);

        assertFalse(g.state.tryAwaitingChoice);
        assertFalse(g.getSituation().userChoosesTry);
    }

    @Test
    public void playGameResolvesUserTryChoice() throws Exception {
        League league = createLeague();
        Team home = league.teamList.get(0);
        Team away = league.teamList.get(1);
        home.userControlled = true;
        away.userControlled = false;

        int totalPoints = 0;
        for (int seed = 0; seed < 5; seed++) {
            Game g = new Game(home, away);
            g.setRandom(new Random(700L + seed));
            g.playGame();

            assertTrue(g.hasPlayed);
            assertTrue(g.state.gameOver);
            assertFalse("user try left pending", g.state.pendingTry);
            assertFalse("user try left awaiting choice", g.state.tryAwaitingChoice);
            totalPoints += g.homeScore + g.awayScore;
        }

        // A stalled PAT freezes games on the first user TD (6-0 / 6-7 style finals).
        assertTrue("expected football-like scoring, got " + totalPoints + " points over 5 games",
                totalPoints > 5 * 20);
    }

    private int countPassCalls(Team offense, Team defense, int snaps, long seed) {
        Game g = new Game(offense, defense);
        g.setRandom(new Random(seed));
        g.startGame();
        g.state.pendingKickoff = false;
        g.state.possessionHome = true;
        AiPlayCaller ai = new AiPlayCaller(new Random(seed));
        int passes = 0;
        for (int i = 0; i < snaps; i++) {
            g.state.down = 1;
            g.state.yardsNeed = 10;
            g.state.yardLine = 40;
            g.state.gameTime = 2000;
            g.state.pendingKickoff = false;
            PlayCall c = ai.choose(offense, defense, g.state);
            if (c.offensePlay == OffensePlay.PASS) passes++;
        }
        return passes;
    }

    private int countStackBox(Team defense, int snaps, long seed) {
        AiPlayCaller ai = new AiPlayCaller(new Random(seed));
        CFBsimPack.engine.GameState state = new CFBsimPack.engine.GameState();
        state.gameTime = 2000;
        state.yardLine = 40;
        state.down = 1;
        state.yardsNeed = 10;
        state.pendingKickoff = false;
        int stack = 0;
        for (int i = 0; i < snaps; i++) {
            state.yardsNeed = 10; // avoid automatic short-yardage stack
            if (ai.chooseDefenseConcept(defense, state, null).coverage == CoverageCall.STACK_BOX) {
                stack++;
            }
        }
        return stack;
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

    /** Avoids DOG and random play fouls so clock tests stay deterministic. */
    private static Random noFoulRandom() {
        return new Random() {
            @Override
            public double nextDouble() {
                return 0.99;
            }
        };
    }

    /** Forces the first nextDouble check (DOG) to fire. */
    private static Random alwaysFoulRandom() {
        return new Random() {
            @Override
            public double nextDouble() {
                return 0.0;
            }
        };
    }

    /**
     * Spike uses no play RNG. Sequence: skip DOG, hit false start on pre-snap roll.
     */
    private static Random forceFalseStartOnSpikeRandom() {
        final double[] seq = {0.99, 0.0};
        return new Random() {
            int i;

            @Override
            public double nextDouble() {
                if (i < seq.length) return seq[i++];
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
