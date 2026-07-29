package CFBsimPack;

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

public class GameFinalizeTest {

    private static final String FIRST_NAMES = "A,B,C,D,E,F,G,H,I,J";
    private static final String LAST_NAMES = "K,L,M,N,O,P,Q,R,S,T";

    @Test
    public void finalizeGameRefusesTiedScores() throws Exception {
        League league = createLeague();
        Team home = league.teamList.get(0);
        Team away = league.teamList.get(1);
        home.ensureRivalry(away.abbr, 50);
        away.ensureRivalry(home.abbr, 50);

        Game g = new Game(home, away);
        g.setRandom(new Random(1L));
        g.startGame();
        g.state.homeScore = 21;
        g.state.awayScore = 21;
        g.homeScore = 21;
        g.awayScore = 21;

        int homeWins = home.wins;
        int awayWins = away.wins;
        int homeLosses = home.losses;
        int awayLosses = away.losses;
        int homeWlSize = home.gameWLSchedule.size();
        int awayWlSize = away.gameWLSchedule.size();

        g.finalizeGame();

        assertFalse(g.hasPlayed);
        assertEquals(homeWins, home.wins);
        assertEquals(awayWins, away.wins);
        assertEquals(homeLosses, home.losses);
        assertEquals(awayLosses, away.losses);
        assertEquals(homeWlSize, home.gameWLSchedule.size());
        assertEquals(awayWlSize, away.gameWLSchedule.size());
        assertFalse(home.rivalryResults.containsKey(away.abbr));
        assertFalse(away.rivalryResults.containsKey(home.abbr));
    }

    @Test
    public void resolveUntilDecidedFromTiedStateDoesNotInventAwayWin() throws Exception {
        League league = createLeague();
        Team home = league.teamList.get(0);
        Team away = league.teamList.get(1);
        Game g = new Game(home, away);
        g.setRandom(new Random(42L));
        g.startGame();
        g.state.homeScore = 14;
        g.state.awayScore = 14;
        g.homeScore = 14;
        g.awayScore = 14;
        g.state.gameOver = false;
        g.state.playingOT = false;
        g.state.gameTime = 0;

        int homeWinsBefore = home.wins;
        int awayWinsBefore = away.wins;

        boolean finished = g.resolveUntilDecided();

        if (finished) {
            assertTrue(g.hasPlayed);
            assertTrue(g.isDecided());
            assertEquals(1, (home.wins - homeWinsBefore) + (away.wins - awayWinsBefore));
            assertTrue(g.homeScore != g.awayScore);
        } else {
            assertFalse(g.hasPlayed);
            assertEquals(homeWinsBefore, home.wins);
            assertEquals(awayWinsBefore, away.wins);
            assertEquals(0, home.losses);
            assertEquals(0, away.losses);
        }
    }

    @Test
    public void playGameProducesExactlyOneWinnerAndMatchingPlayerWins() throws Exception {
        League league = createLeague();
        Team home = league.teamList.get(0);
        Team away = league.teamList.get(1);
        Game g = new Game(home, away);
        g.setRandom(new Random(42L));
        g.playGame();

        assertTrue(g.hasPlayed);
        assertTrue(g.isDecided());
        assertEquals(1, home.wins + away.wins);
        assertEquals(1, home.losses + away.losses);
        assertEquals(1, Math.abs(home.wins - away.wins));

        Team winner = g.winningTeam();
        Team loser = g.losingTeam();
        assertEquals(1, winner.wins);
        assertEquals(1, loser.losses);
        assertEquals(1, winner.teamQBs.get(0).statsWins);
        assertEquals(0, loser.teamQBs.get(0).statsWins);
    }

    private League createLeague() throws IOException {
        Path csvPath = Paths.get("src/main/assets/fbs_2026.csv");
        if (!Files.exists(csvPath)) {
            csvPath = Paths.get("app/src/main/assets/fbs_2026.csv");
        }
        String csv = new String(Files.readAllBytes(csvPath), StandardCharsets.UTF_8);
        return new League(FIRST_NAMES, LAST_NAMES, csv);
    }
}
