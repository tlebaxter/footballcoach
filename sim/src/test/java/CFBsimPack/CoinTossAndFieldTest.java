package CFBsimPack;

import CFBsimPack.engine.GameSituation;

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

public class CoinTossAndFieldTest {

    private static final String FIRST_NAMES = "A,B,C,D,E,F,G,H,I,J";
    private static final String LAST_NAMES = "K,L,M,N,O,P,Q,R,S,T";

    @Test
    public void receiveSetsOpeningKickerToLoser() throws Exception {
        League league = createLeagueNoUser();
        Game g = new Game(league.teamList.get(0), league.teamList.get(1));
        g.setRandom(new Random(1L));
        g.startGame();
        // League teams have no userControlled — toss auto-resolved. Re-apply known choice.
        g.state.awaitingCoinToss = true;
        g.state.tossResolved = false;
        g.state.pendingKickoff = false;
        g.state.homeWonToss = true;
        assertTrue(g.applyTossChoice(true, true));
        assertFalse(g.state.deferred);
        assertTrue(g.state.homeReceivesFirstHalf);
        assertFalse(g.state.possessionHome); // away kicks
        assertTrue(g.state.pendingKickoff);
        assertTrue(g.state.homeDefendsLeft);
        assertFalse(g.state.awaitingCoinToss);
    }

    @Test
    public void deferGivesOtherTeamFirstHalfReceiveAndWinnerSecond() throws Exception {
        League league = createLeagueNoUser();
        Game g = new Game(league.teamList.get(0), league.teamList.get(1));
        g.setRandom(new Random(2L));
        g.startGame();
        g.state.awaitingCoinToss = true;
        g.state.tossResolved = false;
        g.state.pendingKickoff = false;
        g.state.homeWonToss = true;
        assertTrue(g.applyTossChoice(false, false)); // defer, defend right
        assertTrue(g.state.deferred);
        assertFalse(g.state.homeReceivesFirstHalf); // away receives 1st
        assertTrue(g.state.possessionHome); // home kicks opening
        assertFalse(g.state.homeDefendsLeft); // home won and chose defend right

        // Simulate halftime boundary
        boolean beforeLeft = g.state.homeDefendsLeft;
        g.state.gameTime = 1801;
        // Force second half via reflection-free path: call beginSecondHalf by crossing quarter
        // Set clock so a snap would cross — invoke through package by mirroring state:
        g.state.gameTime = 1790;
        // Directly verify second-half receiver rule using same formula as Game.beginSecondHalf
        boolean homeReceivesSecond = g.state.deferred
                ? g.state.homeWonToss
                : !g.state.homeReceivesFirstHalf;
        assertTrue(homeReceivesSecond);

        // Cross half with a snap that burns clock (use auto after forcing time)
        g.state.pendingKickoff = false;
        g.state.yardLine = 40;
        g.state.down = 1;
        g.state.yardsNeed = 10;
        g.state.gameTime = 1805;
        int qBefore = g.state.quarter();
        assertEquals(2, qBefore);
        g.executeSnap(null);
        assertTrue(g.state.quarter() >= 3);
        assertEquals(!beforeLeft, g.state.homeDefendsLeft);
        assertTrue(g.state.pendingKickoff);
        assertFalse(g.state.possessionHome); // home receives 2nd → away kicks
    }

    @Test
    public void userWonTossLeavesAwaitingForUi() throws Exception {
        League league = createLeagueWithUserHome();
        Game g = new Game(league.teamList.get(0), league.teamList.get(1));
        // Force home win by retrying seeds until home wins, or set after start
        g.setRandom(new Random(10L));
        g.startGame();
        g.state.homeWonToss = true;
        g.state.awaitingCoinToss = true;
        g.state.tossResolved = false;
        g.state.pendingKickoff = false;
        assertTrue(g.userWonCoinToss());
        GameSituation sit = g.getSituation();
        assertTrue(sit.awaitingCoinToss);
        assertTrue(sit.userWonToss);
        assertTrue(g.executeSnap(null).logLine.contains("Coin toss"));
    }

    @Test
    public void aiTossWinnerAutoResolvesOnStart() throws Exception {
        League league = createLeagueWithUserHome();
        Team home = league.teamList.get(0);
        Team away = league.teamList.get(1);
        // User coaches away — when home wins the toss, AI resolves immediately.
        home.userControlled = false;
        away.userControlled = true;
        league.userTeam = away;
        Game g = new Game(home, away);
        g.setRandom(new Random(5L));
        g.startGame();
        g.state.homeWonToss = true; // AI (home) wins
        g.state.awaitingCoinToss = true;
        g.state.tossResolved = false;
        g.state.pendingKickoff = false;
        assertFalse(g.userWonCoinToss());
        g.autoResolveCoinToss();
        assertFalse(g.state.awaitingCoinToss);
        assertTrue(g.state.tossResolved);
        assertTrue(g.state.pendingKickoff);
    }

    @Test
    public void playGameCompletesWithAutoToss() throws Exception {
        League league = createLeagueNoUser();
        Game g = new Game(league.teamList.get(0), league.teamList.get(1));
        g.setRandom(new Random(42L));
        g.playGame();
        assertTrue(g.hasPlayed);
    }

    @Test
    public void oppositeDirectionsAbsoluteMapping() {
        // homeDefendsLeft=true → home attacks right, away attacks left
        assertEquals(0.25f, offenseAbs(25, true, true), 0.001f);
        assertEquals(0.75f, offenseAbs(25, false, true), 0.001f);
        // As yards increase, home X increases and away X decreases
        assertTrue(offenseAbs(50, true, true) > offenseAbs(25, true, true));
        assertTrue(offenseAbs(50, false, true) < offenseAbs(25, false, true));

        // homeDefendsLeft=false → home attacks left
        assertEquals(0.75f, offenseAbs(25, true, false), 0.001f);
        assertEquals(0.25f, offenseAbs(25, false, false), 0.001f);
        assertTrue(offenseAbs(50, true, false) < offenseAbs(25, true, false));
        assertTrue(offenseAbs(50, false, false) > offenseAbs(25, false, false));
    }

    /** Mirrors CoachField.offenseYardToAbsolute for JVM tests without Compose deps. */
    private static float offenseAbs(int offenseYard, boolean possessionHome, boolean homeDefendsLeft) {
        int y = Math.max(0, Math.min(100, offenseYard));
        boolean offenseAttacksRight = possessionHome ? homeDefendsLeft : !homeDefendsLeft;
        return offenseAttacksRight ? y / 100f : (100 - y) / 100f;
    }

    private League createLeagueNoUser() throws IOException {
        League league = loadLeague();
        for (Team t : league.teamList) {
            t.userControlled = false;
        }
        league.userTeam = null;
        return league;
    }

    private League createLeagueWithUserHome() throws IOException {
        League league = loadLeague();
        for (Team t : league.teamList) {
            t.userControlled = false;
        }
        league.userTeam = league.teamList.get(0);
        league.userTeam.userControlled = true;
        return league;
    }

    private League loadLeague() throws IOException {
        String csv = achijones.footballcoach.testing.FbsCsv.read();
        return new League(FIRST_NAMES, LAST_NAMES, csv);
    }
}
