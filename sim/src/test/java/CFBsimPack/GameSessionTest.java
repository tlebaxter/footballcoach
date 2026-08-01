package CFBsimPack;

import org.junit.After;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class GameSessionTest {

    private static final String FIRST_NAMES = "A,B,C,D,E,F,G,H,I,J";
    private static final String LAST_NAMES = "K,L,M,N,O,P,Q,R,S,T";

    @After
    public void tearDown() {
        GameSession.clearAll();
        OffseasonSession.clear();
    }

    @Test
    public void finishCoachGamePlayedSetsPendingSaveAndUserPerspectiveScore() throws Exception {
        League league = createLeague();
        Team user = league.userTeam;
        Team opp = league.teamList.get(1);
        GameSession.setLeague(league);

        Game g = new Game(user, opp, "Test");
        g.hasPlayed = true;
        g.homeScore = 31;
        g.awayScore = 17;
        GameSession.setActiveCoachGame(g);

        GameSession.finishCoachGame(g);

        assertNull(GameSession.getActiveCoachGame());
        assertTrue(GameSession.consumePendingCoachResultSave());
        assertEquals("31-17", GameSession.consumePendingCoachResultSummary());
        assertFalse(GameSession.consumePendingCoachResultSave());
        assertNull(GameSession.consumePendingCoachResultSummary());
    }

    @Test
    public void finishCoachGameAwayUserUsesUserPerspectiveScore() throws Exception {
        League league = createLeague();
        Team user = league.userTeam;
        Team opp = league.teamList.get(1);
        GameSession.setLeague(league);

        Game g = new Game(opp, user, "Road");
        g.hasPlayed = true;
        g.homeScore = 10;
        g.awayScore = 24;

        GameSession.finishCoachGame(g);

        assertTrue(GameSession.consumePendingCoachResultSave());
        assertEquals("24-10", GameSession.consumePendingCoachResultSummary());
    }

    @Test
    public void finishCoachGameUnfinishedClearsPending() throws Exception {
        League league = createLeague();
        GameSession.setLeague(league);
        GameSession.finishCoachGame(playedStub(league));
        assertTrue(GameSession.consumePendingCoachResultSave());

        Game unfinished = new Game(league.userTeam, league.teamList.get(1), "Unfinished");
        unfinished.hasPlayed = false;
        GameSession.setActiveCoachGame(unfinished);
        GameSession.finishCoachGame(unfinished);

        assertNull(GameSession.getActiveCoachGame());
        assertFalse(GameSession.consumePendingCoachResultSave());
        assertNull(GameSession.consumePendingCoachResultSummary());
    }

    @Test
    public void beginOffseasonClearsStayingFlag() throws Exception {
        League league = createLeague();
        LeagueOffseason off = new LeagueOffseason(league);
        GameSession.setStayingOnMainDuringOffseason(true);

        GameSession.beginOffseason(league, off, OffseasonSession.Phase.PORTAL);

        assertFalse(GameSession.isStayingOnMainDuringOffseason());
        assertTrue(OffseasonSession.ready());
        assertEquals(OffseasonSession.Phase.PORTAL, OffseasonSession.phase);
    }

    @Test
    public void clearOffseasonResetsStayingFlag() throws Exception {
        League league = createLeague();
        LeagueOffseason off = new LeagueOffseason(league);
        GameSession.beginOffseason(league, off);
        GameSession.setStayingOnMainDuringOffseason(true);

        GameSession.clearOffseason();

        assertFalse(OffseasonSession.ready());
        assertFalse(GameSession.isStayingOnMainDuringOffseason());
    }

    @Test
    public void clearAllResetsSlotsCoachPendingAndLeague() throws Exception {
        League league = createLeague();
        GameSession.setLeague(league);
        GameSession.setActiveSaveSlot(3);
        GameSession.setNeedsOocScheduling(true);
        GameSession.setNeedsTeamPicker(true);
        GameSession.setStayingOnMainDuringOffseason(true);
        GameSession.finishCoachGame(playedStub(league));
        LeagueOffseason off = new LeagueOffseason(league);
        GameSession.beginOffseason(league, off);

        GameSession.clearAll();

        assertFalse(GameSession.hasLeague());
        assertNull(GameSession.getActiveSaveSlot());
        assertFalse(GameSession.needsOocScheduling());
        assertFalse(GameSession.needsTeamPicker());
        assertFalse(GameSession.isStayingOnMainDuringOffseason());
        assertFalse(GameSession.consumePendingCoachResultSave());
        assertFalse(OffseasonSession.ready());
    }

    private static Game playedStub(League league) {
        Game g = new Game(league.userTeam, league.teamList.get(1), "Stub");
        g.hasPlayed = true;
        g.homeScore = 7;
        g.awayScore = 0;
        return g;
    }

    private static League createLeague() throws Exception {
        String csv = achijones.footballcoach.testing.FbsCsv.read();
        League league = new League(FIRST_NAMES, LAST_NAMES, csv);
        league.userTeam = league.teamList.get(0);
        league.userTeam.userControlled = true;
        return league;
    }
}
