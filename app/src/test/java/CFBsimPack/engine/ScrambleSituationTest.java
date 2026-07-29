package CFBsimPack.engine;

import CFBsimPack.League;
import CFBsimPack.Player;
import CFBsimPack.PressureResponse;
import CFBsimPack.QbPressurePolicy;
import CFBsimPack.Team;

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
 * Situational scramble / throwaway behavior under forced pocket pressure.
 */
public class ScrambleSituationTest {

    private static final String FIRST_NAMES = "A,B,C,D,E,F,G,H,I,J";
    private static final String LAST_NAMES = "K,L,M,N,O,P,Q,R,S,T";
    private static final int TRIALS = 2200;

    @Test
    public void convertScramblesClusterNearerNeedThanFirstAndTen() throws Exception {
        League league = createLeague();
        Team offense = league.teamList.get(0);
        Team defense = league.teamList.get(1);
        setupPressure(offense, defense);

        double convertAvg = avgScrambleYards(offense, defense, 3, 4, 35, 2000, 44L);
        double firstAvg = avgScrambleYards(offense, defense, 1, 10, 35, 2000, 44L);
        assertTrue("convertAvg=" + convertAvg + " firstAvg=" + firstAvg,
                convertAvg > 0 && firstAvg > 0 && convertAvg < firstAvg + 0.75);
        assertTrue("convertAvg should sit nearer 4 than open-field average: " + convertAvg,
                convertAvg <= 7.5);
    }

    @Test
    public void protectLeadHasFewerLongScramblesThanLateTrailing() throws Exception {
        League league = createLeague();
        Team offense = league.teamList.get(0);
        Team defense = league.teamList.get(1);
        setupPressure(offense, defense);

        int protectLong = countLongScrambles(offense, defense, /*leading*/ true, 90L);
        int trailingLong = countLongScrambles(offense, defense, /*leading*/ false, 90L);
        assertTrue("protectLong=" + protectLong + " trailingLong=" + trailingLong,
                protectLong < trailingLong);
    }

    @Test
    public void backedUpThrowAwayBeatsSacksIntoEndZone() throws Exception {
        League league = createLeague();
        Team offense = league.teamList.get(0);
        Team defense = league.teamList.get(1);
        setupPressure(offense, defense);
        offense.setQbPressurePolicy(QbPressurePolicy.defaults().copyWith(
                QbPressurePolicy.Slot.BACKED_UP, PressureResponse.THROW_IT_AWAY));

        int throwaways = 0;
        int safetiesOrDeepSacks = 0;
        for (int i = 0; i < TRIALS; i++) {
            GameState state = baseState();
            state.yardLine = 5;
            state.down = 2;
            state.yardsNeed = 10;
            state.gameTime = 1800;
            PlayResolver resolver = new PlayResolver(new Random(3030L + i));
            PlayResult r = resolver.resolve(offense, defense, state, passCall(CoverageCall.COVER_3));
            if (r.throwaway || (r.logLine != null && r.logLine.contains("THROW AWAY!"))) {
                throwaways++;
            }
            if (r.safety || (r.sack && r.yardsGained <= -3 && state.yardLine <= 2)) {
                safetiesOrDeepSacks++;
            }
        }
        assertTrue("throwaways=" + throwaways + " deepSacks=" + safetiesOrDeepSacks,
                throwaways > safetiesOrDeepSacks);
        assertTrue("expected throwaways under backed-up pressure, got " + throwaways, throwaways >= 80);
    }

    @Test
    public void policyEncodeRoundTrips() {
        QbPressurePolicy policy = new QbPressurePolicy(
                PressureResponse.FORCE_SIDELINE,
                PressureResponse.SCRAMBLE_FOR_IT,
                PressureResponse.SLIDE_SECURE,
                PressureResponse.THROW_IT_AWAY,
                PressureResponse.TAKE_THE_FIRST_DOWN
        );
        QbPressurePolicy parsed = QbPressurePolicy.parse(policy.encode());
        assertEquals(policy.normal, parsed.normal);
        assertEquals(policy.convert, parsed.convert);
        assertEquals(policy.protectLead, parsed.protectLead);
        assertEquals(policy.lateTrailing, parsed.lateTrailing);
        assertEquals(policy.backedUp, parsed.backedUp);
    }

    private double avgScrambleYards(Team offense, Team defense, int down, int need,
                                    int yardLine, int gameTime, long seed) {
        long sum = 0;
        int n = 0;
        for (int i = 0; i < TRIALS; i++) {
            GameState state = baseState();
            state.down = down;
            state.yardsNeed = need;
            state.yardLine = yardLine;
            state.gameTime = gameTime;
            PlayResolver resolver = new PlayResolver(new Random(seed + i));
            PlayResult r = resolver.resolve(offense, defense, state, passCall(CoverageCall.COVER_3));
            if (isScramble(r)) {
                sum += r.yardsGained;
                n++;
            }
        }
        return n == 0 ? 0.0 : (double) sum / n;
    }

    private int countLongScrambles(Team offense, Team defense, boolean leading, long seed) {
        int longScrambles = 0;
        for (int i = 0; i < TRIALS; i++) {
            GameState state = baseState();
            state.down = 1;
            state.yardsNeed = 10;
            state.yardLine = 40;
            state.gameTime = 90;
            if (leading) {
                state.homeScore = 28;
                state.awayScore = 17;
            } else {
                state.homeScore = 17;
                state.awayScore = 28;
            }
            PlayResolver resolver = new PlayResolver(new Random(seed + i));
            PlayResult r = resolver.resolve(offense, defense, state, passCall(CoverageCall.PRESS));
            if (isScramble(r) && r.yardsGained >= 8) {
                longScrambles++;
            }
        }
        return longScrambles;
    }

    private static boolean isScramble(PlayResult r) {
        return r.logLine != null
                && (r.logLine.contains("SCRAMBLE!") || r.logLine.contains("SCRAMBLE TD!"));
    }

    private static PlayCall passCall(CoverageCall cov) {
        return PlayCall.fromConcepts(
                Playbook.offenseById("gun_slants"),
                Playbook.defenseFor(cov),
                TempoCall.NORMAL
        );
    }

    private static GameState baseState() {
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

    private static void setupPressure(Team offense, Team defense) {
        for (int i = 0; i < 5; i++) {
            Player ol = offense.getOL(i);
            if (ol == null || ol.ratings == null) continue;
            ol.ratings.pbk = 35;
            ol.ratings.stre = 40;
            ol.applyRatings(ol.ratings);
        }
        for (int i = 0; i < 4; i++) {
            Player edge = defense.getEDGE(i);
            if (edge == null || edge.ratings == null) continue;
            edge.ratings.prs = 95;
            edge.ratings.stre = 90;
            edge.ratings.spd = 88;
            edge.applyRatings(edge.ratings);
        }
        for (int i = 0; i < 3; i++) {
            Player dl = defense.getDL(i);
            if (dl == null || dl.ratings == null) continue;
            dl.ratings.prs = 92;
            dl.ratings.stre = 92;
            dl.applyRatings(dl.ratings);
        }
        Player qb = offense.getQB(0);
        qb.ratings.spd = 88;
        qb.ratings.elu = 85;
        qb.ratings.thv = 70;
        qb.applyRatings(qb.ratings);
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
