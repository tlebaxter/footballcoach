package CFBsimPack;

import CFBsimPack.engine.AiPlayCaller;
import CFBsimPack.engine.GameState;
import CFBsimPack.engine.OffenseConcept;
import CFBsimPack.engine.OffensePlay;
import CFBsimPack.engine.PlayerGameStats;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import static org.junit.Assert.assertTrue;

/**
 * Fourth-down AI should punch it in near the goal line instead of always kicking FGs.
 */
public class AiPlayCallerFourthDownTest {

    private static final String FIRST_NAMES = "A,B,C,D,E,F,G,H,I,J";
    private static final String LAST_NAMES = "K,L,M,N,O,P,Q,R,S,T";

    @Test
    public void fourthAndShortAtGoalLineUsuallyGoesForIt() throws Exception {
        League league = createLeague();
        Team offense = league.teamList.get(0);
        Team defense = league.teamList.get(1);

        int trials = 100;
        int goForIt = 0;
        for (int i = 0; i < trials; i++) {
            AiPlayCaller ai = new AiPlayCaller(new Random(1000L + i));
            GameState state = goalLineFourthAndOne();
            OffenseConcept call = ai.suggestOffense(offense, defense, state);
            if (call.offensePlay != OffensePlay.FIELD_GOAL && call.offensePlay != OffensePlay.PUNT) {
                goForIt++;
            }
        }
        // Policy targets ~80% go; allow statistical slack
        assertTrue("expected mostly go-for-it near goal, got " + goForIt + "/" + trials,
                goForIt >= 60);
    }

    @Test
    public void fourthAndShortInRedZoneOftenGoesForIt() throws Exception {
        League league = createLeague();
        Team offense = league.teamList.get(0);
        Team defense = league.teamList.get(1);

        int trials = 100;
        int goForIt = 0;
        for (int i = 0; i < trials; i++) {
            AiPlayCaller ai = new AiPlayCaller(new Random(2000L + i));
            GameState state = new GameState();
            state.gameTime = 2400;
            state.yardLine = 85;
            state.down = 4;
            state.yardsNeed = 1;
            state.possessionHome = true;
            state.homeScore = 14;
            state.awayScore = 10;
            state.pendingKickoff = false;
            OffenseConcept call = ai.suggestOffense(offense, defense, state);
            if (call.offensePlay != OffensePlay.FIELD_GOAL && call.offensePlay != OffensePlay.PUNT) {
                goForIt++;
            }
        }
        // Policy targets ~75% go on 4th & 1 in red zone
        assertTrue("expected frequent go-for-it in red zone, got " + goForIt + "/" + trials,
                goForIt >= 50);
    }

    @Test
    public void simBatchScoresTouchdownsNotJustFieldGoals() throws Exception {
        League league = createLeague();
        int games = 40;
        int totalTd = 0;
        int totalFg = 0;
        int totalPts = 0;
        int pureFgStyle = 0; // both teams under 10 and neither score looks like a TD+XP game
        StringBuilder samples = new StringBuilder();

        for (int i = 0; i < games; i++) {
            Team home = league.teamList.get(i % league.teamList.size());
            Team away = league.teamList.get((i + 11) % league.teamList.size());
            if (home == away) {
                away = league.teamList.get((i + 1) % league.teamList.size());
            }
            Game g = new Game(home, away);
            g.setRandom(new Random(5000L + i));
            g.playGame();

            int td = 0;
            int fg = 0;
            for (PlayerGameStats.Line line : g.playerGameStats.byKey.values()) {
                td += line.passTd + line.rushTd + line.prTd + line.krTd;
                fg += line.fgMade;
            }
            totalTd += td;
            totalFg += fg;
            totalPts += g.homeScore + g.awayScore;
            if (g.homeScore < 10 && g.awayScore < 10 && td == 0) {
                pureFgStyle++;
            }
            if (i < 10) {
                samples.append(g.homeScore).append('-').append(g.awayScore).append(' ');
            }
        }

        double avgPtsPerTeam = totalPts / (2.0 * games);
        double avgTdPerGame = totalTd / (double) games;
        System.out.println("Sim batch samples: " + samples);
        System.out.println("avgPts/team=" + avgPtsPerTeam
                + " avgTd/game=" + avgTdPerGame
                + " totalFg=" + totalFg
                + " pureFgStyle=" + pureFgStyle);

        assertTrue("expected TDs in batch sims, got " + totalTd + " across " + games
                        + " (avgPts/team=" + avgPtsPerTeam + ")",
                totalTd >= games); // at least ~1 TD/game combined
        assertTrue("expected TDs to outpace FGs after red-zone go-for-it, td="
                        + totalTd + " fg=" + totalFg,
                totalTd >= totalFg);
        assertTrue("expected fewer pure FG-only low-scoring games, got " + pureFgStyle,
                pureFgStyle <= games / 4);
    }

    @Test
    public void fourthAndLongFromOpponent35StillKicks() throws Exception {
        League league = createLeague();
        Team offense = league.teamList.get(0);
        Team defense = league.teamList.get(1);

        int trials = 40;
        int fieldGoals = 0;
        for (int i = 0; i < trials; i++) {
            AiPlayCaller ai = new AiPlayCaller(new Random(3000L + i));
            GameState state = new GameState();
            state.gameTime = 2400;
            state.yardLine = 65;
            state.down = 4;
            state.yardsNeed = 8;
            state.possessionHome = true;
            state.homeScore = 14;
            state.awayScore = 10;
            state.pendingKickoff = false;
            OffenseConcept call = ai.suggestOffense(offense, defense, state);
            if (call.offensePlay == OffensePlay.FIELD_GOAL) {
                fieldGoals++;
            }
        }
        assertTrue("expected FG attempts from ~52 yd range, got " + fieldGoals + "/" + trials,
                fieldGoals >= 30);
    }

    private static GameState goalLineFourthAndOne() {
        GameState state = new GameState();
        state.gameTime = 2400;
        state.yardLine = 98;
        state.down = 4;
        state.yardsNeed = 1;
        state.possessionHome = true;
        state.homeScore = 14;
        state.awayScore = 10;
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
        league.userTeam.userControlled = true;
        return league;
    }
}
