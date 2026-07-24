package CFBsimPack.engine;

import CFBsimPack.League;
import CFBsimPack.Player;
import CFBsimPack.Team;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import static org.junit.Assert.assertTrue;

/**
 * Directional Monte Carlo checks that thv / rtr / bsc / scrambleMod affect snap outcomes.
 */
public class PlayResolverRatingsTest {

    private static final String FIRST_NAMES = "A,B,C,D,E,F,G,H,I,J";
    private static final String LAST_NAMES = "K,L,M,N,O,P,Q,R,S,T";
    private static final int TRIALS = 1800;

    @Test
    public void highBscFumblesLessThanLowBsc() throws Exception {
        League league = createLeague();
        Team offense = league.teamList.get(0);
        Team defense = league.teamList.get(1);
        Player rb = offense.getRB(0);
        assertTrue(rb != null && rb.ratings != null);

        int highFumbles = countFumbles(offense, defense, rb, 95, 9001L);
        int lowFumbles = countFumbles(offense, defense, rb, 35, 9001L);
        assertTrue("high bsc fumbles=" + highFumbles + " low=" + lowFumbles,
                highFumbles < lowFumbles);
    }

    @Test
    public void spyScramblesLessThanPress() throws Exception {
        League league = createLeague();
        Team offense = league.teamList.get(0);
        Team defense = league.teamList.get(1);
        weakenPassProtection(offense);
        boostPassRush(defense);
        Player qb = offense.getQB(0);
        qb.ratings.spd = 88;
        qb.ratings.elu = 85;
        qb.ratings.thv = 70;
        qb.applyRatings(qb.ratings);

        int spyEscapes = countScrambleEscapes(offense, defense, CoverageCall.SPY, 4242L);
        int pressEscapes = countScrambleEscapes(offense, defense, CoverageCall.PRESS, 4242L);
        assertTrue("spy escapes=" + spyEscapes + " press=" + pressEscapes,
                spyEscapes < pressEscapes);
    }

    @Test
    public void highRtrCompletesMoreThanLowRtr() throws Exception {
        League league = createLeague();
        Team offense = league.teamList.get(0);
        Team defense = league.teamList.get(1);
        Player wr = offense.getWR(0);
        assertTrue(wr != null && wr.ratings != null);

        int highCompletions = countCompletions(offense, defense, wr, 95, 5150L);
        int lowCompletions = countCompletions(offense, defense, wr, 35, 5150L);
        assertTrue("high rtr completions=" + highCompletions + " low=" + lowCompletions,
                highCompletions > lowCompletions);
    }

    @Test
    public void highThvTakesFewerSacksAndInts() throws Exception {
        League league = createLeague();
        Team offense = league.teamList.get(0);
        Team defense = league.teamList.get(1);
        weakenPassProtection(offense);
        boostPassRush(defense);
        Player qb = offense.getQB(0);

        int highBad = countSacksAndInts(offense, defense, qb, 95, 7777L);
        int lowBad = countSacksAndInts(offense, defense, qb, 35, 7777L);
        assertTrue("high thv sack+int=" + highBad + " low=" + lowBad, highBad < lowBad);
    }

    @Test
    public void aiCallsSpyAgainstMobileQb() throws Exception {
        League league = createLeague();
        Team offense = league.teamList.get(0);
        Team defense = league.teamList.get(1);
        Player qb = offense.getQB(0);
        qb.ratings.spd = 90;
        qb.applyRatings(qb.ratings);

        GameState state = baseState();
        state.down = 1;
        state.yardsNeed = 10;
        AiPlayCaller ai = new AiPlayCaller(new Random(11L));
        int spy = 0;
        for (int i = 0; i < 400; i++) {
            if (ai.chooseDefenseConcept(offense, defense, state, null).coverage == CoverageCall.SPY) {
                spy++;
            }
        }
        assertTrue("expected Spy calls vs mobile QB, got " + spy, spy >= 20);
    }

    private int countFumbles(Team offense, Team defense, Player rb, int bsc, long seed) {
        rb.ratings.bsc = bsc;
        rb.applyRatings(rb.ratings);
        int before = rb.seasonStats.fumbles;
        for (int i = 0; i < TRIALS; i++) {
            GameState state = baseState();
            PlayResolver resolver = new PlayResolver(new Random(seed + i));
            PlayCall call = PlayCall.fromConcepts(
                    Playbook.offenseById("i_dive"),
                    Playbook.defenseFor(CoverageCall.COVER_3),
                    TempoCall.NORMAL
            );
            // Force RB as carrier by temporarily removing QB keep odds via slow QB
            Player qb = offense.getQB(0);
            int oldSpd = qb.ratings.spd;
            qb.ratings.spd = 40;
            resolver.resolve(offense, defense, state, call);
            qb.ratings.spd = oldSpd;
        }
        return rb.seasonStats.fumbles - before;
    }

    private int countScrambleEscapes(Team offense, Team defense, CoverageCall cov, long seed) {
        int escapes = 0;
        Player qb = offense.getQB(0);
        for (int i = 0; i < TRIALS; i++) {
            GameState state = baseState();
            PlayResolver resolver = new PlayResolver(new Random(seed + i));
            PlayCall call = PlayCall.fromConcepts(
                    Playbook.offenseById("gun_slants"),
                    Playbook.defenseFor(cov),
                    TempoCall.NORMAL
            );
            PlayResult r = resolver.resolve(offense, defense, state, call);
            if (r.logLine != null && r.logLine.contains(" rush ") && r.logLine.contains(qb.name)) {
                escapes++;
            }
        }
        return escapes;
    }

    private int countCompletions(Team offense, Team defense, Player wr, int rtr, long seed) {
        // Pin other WRs low so this WR is often targeted
        for (int i = 0; i < 5; i++) {
            Player w = offense.getWR(i);
            if (w == null) continue;
            w.ratings.rtr = (w == wr) ? rtr : 40;
            w.ratings.hnd = (w == wr) ? 85 : 55;
            w.ratings.spd = (w == wr) ? 88 : 70;
            w.applyRatings(w.ratings);
        }
        int completions = 0;
        for (int i = 0; i < TRIALS; i++) {
            GameState state = baseState();
            PlayResolver resolver = new PlayResolver(new Random(seed + i));
            PlayCall call = PlayCall.fromConcepts(
                    Playbook.offenseById("gun_slants"),
                    Playbook.defenseFor(CoverageCall.COVER_3),
                    TempoCall.NORMAL
            );
            PlayResult r = resolver.resolve(offense, defense, state, call);
            if (r.logLine != null
                    && (r.logLine.contains(" pass ") || r.logLine.contains("PASS TD"))
                    && r.logLine.contains(wr.name)) {
                completions++;
            }
        }
        return completions;
    }

    private int countSacksAndInts(Team offense, Team defense, Player qb, int thv, long seed) {
        qb.ratings.thv = thv;
        qb.ratings.spd = 60;
        qb.ratings.elu = 55;
        qb.applyRatings(qb.ratings);
        int bad = 0;
        for (int i = 0; i < TRIALS; i++) {
            GameState state = baseState();
            PlayResolver resolver = new PlayResolver(new Random(seed + i));
            PlayCall call = PlayCall.fromConcepts(
                    Playbook.offenseById("gun_slants"),
                    Playbook.defenseFor(CoverageCall.COVER_3),
                    TempoCall.NORMAL
            );
            PlayResult r = resolver.resolve(offense, defense, state, call);
            if (r.logLine != null
                    && (r.logLine.contains("SACK!") || r.logLine.contains("INTERCEPTION!"))) {
                bad++;
            }
        }
        return bad;
    }

    private static GameState baseState() {
        GameState state = new GameState();
        state.down = 1;
        state.yardsNeed = 10;
        state.yardLine = 35;
        state.possessionHome = true;
        state.gameTime = 2000;
        state.pendingKickoff = false;
        return state;
    }

    private static void weakenPassProtection(Team offense) {
        for (int i = 0; i < 5; i++) {
            Player ol = offense.getOL(i);
            if (ol == null || ol.ratings == null) continue;
            ol.ratings.pbk = 35;
            ol.ratings.stre = 40;
            ol.applyRatings(ol.ratings);
        }
    }

    private static void boostPassRush(Team defense) {
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
