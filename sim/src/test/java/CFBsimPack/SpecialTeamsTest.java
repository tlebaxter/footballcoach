package CFBsimPack;

import CFBsimPack.engine.CoverageCall;
import CFBsimPack.engine.GameState;
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

public class SpecialTeamsTest {

    private static final String FIRST_NAMES = "A,B,C,D,E,F,G,H,I,J";
    private static final String LAST_NAMES = "K,L,M,N,O,P,Q,R,S,T";

    @Test
    public void ensureSpecialTeamsDepthFillsSlots() throws Exception {
        League league = createLeague();
        Team t = league.teamList.get(0);
        t.puntReturner = null;
        t.kickReturner = null;
        t.gunner1 = null;
        t.gunner2 = null;
        t.longSnapper = null;
        t.ensureSpecialTeamsDepth();
        assertNotNull(t.getPuntReturner());
        assertNotNull(t.getKickReturner());
        assertNotNull(t.getGunner1());
        assertNotNull(t.getLongSnapper());
    }

    @Test
    public void stDepthSaveRoundTrip() throws Exception {
        League league = createLeague();
        Team t = league.teamList.get(0);
        t.ensureSpecialTeamsDepth();
        Player pr = t.teamWRs.get(0);
        Player kr = t.teamRBs.get(0);
        t.puntReturner = pr;
        t.kickReturner = kr;
        String line = t.specialTeamsDepthSaveLine();
        assertTrue(line.startsWith("ST_DEPTH,"));
        t.puntReturner = null;
        t.kickReturner = null;
        t.loadSpecialTeamsDepth(line);
        assertEquals(pr, t.puntReturner);
        assertEquals(kr, t.kickReturner);
    }

    @Test
    public void puntReturnCreditsReturner() throws Exception {
        League league = createLeague();
        Team home = league.teamList.get(0);
        Team away = league.teamList.get(1);
        home.ensureSpecialTeamsDepth();
        away.ensureSpecialTeamsDepth();
        Player pr = away.getPuntReturner();
        assertNotNull(pr);
        int before = pr.statsPrAtt;

        GameState state = new GameState();
        state.down = 4;
        state.yardsNeed = 8;
        state.yardLine = 35;
        state.possessionHome = true;
        PlayResolver resolver = new PlayResolver(new Random(1234L));
        PlayCall call = PlayCall.fromConcepts(
                Playbook.offenseById("punt"),
                Playbook.defenseById("punt_return"),
                TempoCall.NORMAL
        );
        PlayResult r = resolver.resolve(home, away, state, call);
        assertEquals(OffensePlay.PUNT, r.playType);
        assertTrue(r.possessionChanged || r.touchback || r.fairCatch || r.puntBlocked || r.turnover);
        if (!r.puntBlocked && !r.touchback && !r.fairCatch && !r.turnover) {
            assertTrue(pr.statsPrAtt >= before);
            assertTrue(r.returnYards >= 0);
            assertNotNull(r.returnerName);
        }
    }

    @Test
    public void fairCatchSpotsBall() throws Exception {
        League league = createLeague();
        Team home = league.teamList.get(0);
        Team away = league.teamList.get(1);
        GameState state = new GameState();
        state.down = 4;
        state.yardsNeed = 10;
        state.yardLine = 40;
        state.possessionHome = true;
        PlayResolver resolver = new PlayResolver(new Random(55L));
        PlayResult r = resolver.resolve(
                home, away, state,
                PlayCall.fromConcepts(
                        Playbook.offenseById("punt"),
                        Playbook.defenseById("fair_catch"),
                        TempoCall.NORMAL
                )
        );
        assertTrue(r.fairCatch || r.touchback || r.puntBlocked);
        assertTrue(r.possessionChanged || r.puntBlocked);
    }

    @Test
    public void kickoffTouchbackOrReturn() throws Exception {
        League league = createLeague();
        Team home = league.teamList.get(0);
        Team away = league.teamList.get(1);
        GameState state = new GameState();
        state.pendingKickoff = true;
        state.yardLine = 35;
        state.possessionHome = true;
        PlayResolver resolver = new PlayResolver(new Random(9L));
        PlayResult r = resolver.resolve(
                home, away, state,
                PlayCall.fromConcepts(
                        Playbook.offenseById("kickoff"),
                        Playbook.defenseById("kick_return"),
                        TempoCall.NORMAL
                )
        );
        assertEquals(OffensePlay.KICKOFF, r.playType);
        assertTrue(r.possessionChanged || r.touchback || r.returnTd);
    }

    @Test
    public void situationalDefenseOnFourthIncludesReturn() {
        GameState state = new GameState();
        state.down = 4;
        state.yardsNeed = 7;
        state.yardLine = 40;
        assertTrue(Playbook.situationalDefense(state).stream()
                .anyMatch(c -> "punt_return".equals(c.id)));
        assertTrue(Playbook.situationalDefense(state).stream()
                .anyMatch(c -> c.coverage == CoverageCall.COVER_3));
    }

    @Test
    public void gameOpeningKickoffClearsPending() throws Exception {
        League league = createLeague();
        Game g = new Game(league.teamList.get(0), league.teamList.get(1));
        g.setRandom(new Random(42L));
        g.startGame();
        if (g.state.awaitingCoinToss) {
            g.autoResolveCoinToss();
        }
        assertTrue(g.state.pendingKickoff);
        g.executeSnap(null);
        assertFalse(g.state.pendingKickoff);
        assertEquals(1, g.state.down);
    }

    @Test
    public void playbookHasFakePuntAndKickoff() {
        assertNotNull(Playbook.offenseById("fake_punt"));
        assertNotNull(Playbook.offenseById("kickoff"));
        assertEquals(OffensePlay.FAKE_PUNT, Playbook.offenseById("fake_punt").offensePlay);
        assertNotNull(Playbook.defenseById("punt_block"));
        assertTrue(Playbook.isSpecialTeamsDefense(Playbook.defenseById("punt_return")));
        assertFalse(Playbook.isSpecialTeamsDefense(Playbook.defenseFor(CoverageCall.COVER_3)));
    }

    @Test
    public void kickerAndPunterAreDistinctPositions() throws Exception {
        League league = createLeague();
        Team t = league.teamList.get(0);
        assertNotNull(t.getK(0));
        assertNotNull(t.getPunter(0));
        String punterPos = t.getPunter(0).position;
        assertTrue("P".equals(punterPos) || "K".equals(punterPos));
        int kOvr = PositionOvr.ovr(t.getK(0), PositionGroup.K);
        int pAsK = PositionOvr.ovr(t.getPunter(0), PositionGroup.K);
        int pOvr = PositionOvr.ovr(t.getPunter(0), PositionGroup.P);
        assertTrue(kOvr > 0 && pOvr > 0);
        // Dedicated P should generally rate higher at P than as K (unless emergency K fallback)
        if ("P".equals(punterPos)) {
            assertTrue(pOvr >= pAsK - 5);
        }
    }

    private League createLeague() throws IOException {
        String csv = achijones.footballcoach.testing.FbsCsv.read();
        League league = new League(FIRST_NAMES, LAST_NAMES, csv);
        league.userTeam = league.teamList.get(0);
        league.userTeam.userControlled = true;
        return league;
    }
}
