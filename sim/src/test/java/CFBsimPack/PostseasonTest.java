package CFBsimPack;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PostseasonTest {

    private static final String FIRST_NAMES =
            "Alex,Blake,Casey,Drew,Evan,Frankie,Gray,Hayden";
    private static final String LAST_NAMES =
            "Adams,Baker,Clark,Davis,Evans,Foster,Green,Hill";

    @Test
    public void autoBidsIncludeFifthChampOverHigherAtLarge() throws Exception {
        League league = createLeague();
        for (Team t : league.teamList) {
            t.confChampion = "";
            t.wins = 9;
            t.losses = 3;
            t.teamPollScore = 0;
        }

        List<Team> champs = new ArrayList<>();
        for (Conference c : league.conferences) {
            if (!c.hasChampionship || c.confTeams.isEmpty()) {
                continue;
            }
            Team champ = c.confTeams.get(0);
            champ.confChampion = "CC";
            champs.add(champ);
        }
        assertTrue(champs.size() >= 5);

        // Poll: 4 elite champs, a pile of strong at-larges, then a weaker 5th champ.
        int poll = 5000;
        for (int i = 0; i < 4; i++) {
            champs.get(i).teamPollScore = poll--;
        }
        List<Team> atLargePool = new ArrayList<>();
        for (Team t : league.teamList) {
            if ("CC".equals(t.confChampion)) {
                continue;
            }
            atLargePool.add(t);
            if (atLargePool.size() >= 10) {
                break;
            }
        }
        for (Team t : atLargePool) {
            t.teamPollScore = poll--;
        }
        Team fifthChamp = champs.get(4);
        fifthChamp.teamPollScore = poll - 50; // worse than all 10 at-larges

        Postseason.CfpSelection selection = Postseason.selectCfpField(league.teamList);
        assertEquals(Postseason.CFP_FIELD_SIZE, selection.field.size());
        assertTrue("5th conference champ must receive an auto-bid", selection.field.contains(fifthChamp));
        assertTrue(selection.autoBids.contains(fifthChamp));
        assertEquals(5, selection.autoBids.size());
    }

    @Test
    public void firstRoundPairsFiveThroughTwelve() throws Exception {
        League league = createLeague();
        List<Team> seeds = new ArrayList<>(league.teamList.subList(0, 12));
        for (Team t : seeds) {
            t.gameSchedule.clear();
        }
        Game[] games = Postseason.scheduleFirstRound(seeds);
        assertEquals(4, games.length);
        assertEquals(seeds.get(4), games[0].homeTeam);
        assertEquals(seeds.get(11), games[0].awayTeam);
        assertEquals("CFP First Round, 5v12", games[0].gameName);
        assertEquals(seeds.get(5), games[1].homeTeam);
        assertEquals(seeds.get(10), games[1].awayTeam);
        assertEquals(seeds.get(6), games[2].homeTeam);
        assertEquals(seeds.get(9), games[2].awayTeam);
        assertEquals(seeds.get(7), games[3].homeTeam);
        assertEquals(seeds.get(8), games[3].awayTeam);
    }

    @Test
    public void reseedPairsHighestVsLowest() throws Exception {
        League league = createLeague();
        List<Team> seeds = new ArrayList<>(league.teamList.subList(0, 12));
        for (Team t : seeds) {
            t.gameSchedule.clear();
        }
        // Simulate byes 1-4 + winners of 5,6,7,8
        List<Team> alive = new ArrayList<>();
        alive.add(seeds.get(0));
        alive.add(seeds.get(1));
        alive.add(seeds.get(2));
        alive.add(seeds.get(3));
        alive.add(seeds.get(4));
        alive.add(seeds.get(5));
        alive.add(seeds.get(6));
        alive.add(seeds.get(7));

        Game[] quarters = Postseason.scheduleReseededRound(
                alive, seeds, Postseason.CFP_QUARTER_HOSTS, "CFP Quarter");
        assertEquals(4, quarters.length);
        assertEquals(seeds.get(0), quarters[0].homeTeam);
        assertEquals(seeds.get(7), quarters[0].awayTeam);
        assertTrue(quarters[0].gameName.contains("Rose Bowl"));
        assertEquals(seeds.get(1), quarters[1].homeTeam);
        assertEquals(seeds.get(6), quarters[1].awayTeam);
        assertEquals(seeds.get(2), quarters[2].homeTeam);
        assertEquals(seeds.get(5), quarters[2].awayTeam);
        assertEquals(seeds.get(3), quarters[3].homeTeam);
        assertEquals(seeds.get(4), quarters[3].awayTeam);
    }

    @Test
    public void bowlMatcherPrefersConferenceTieIn() throws Exception {
        League league = createLeague();
        for (Team t : league.teamList) {
            t.wins = 0;
            t.losses = 12;
            t.teamPollScore = 0;
            t.gameSchedule.clear();
        }

        Team big12A = firstInConference(league, "Big 12");
        Team pac12A = firstInConference(league, "Pac-12");
        Team secBest = firstInConference(league, "SEC");
        Team bigTenBest = firstInConference(league, "Big Ten");

        // Higher poll SEC/B1G sit in the CFP field; Alamo should still prefer Big 12 vs Pac-12.
        secBest.wins = 10;
        secBest.teamPollScore = 900;
        bigTenBest.wins = 10;
        bigTenBest.teamPollScore = 890;
        big12A.wins = 8;
        big12A.teamPollScore = 500;
        pac12A.wins = 8;
        pac12A.teamPollScore = 480;

        Set<Team> cfp = new HashSet<>();
        cfp.add(secBest);
        cfp.add(bigTenBest);
        Game[] bowls = Postseason.matchBowls(league.teamList, cfp);
        assertTrue(bowls.length >= 1);
        Game alamo = bowls[0];
        assertEquals("Alamo Bowl", alamo.gameName);
        assertEquals(big12A, alamo.homeTeam);
        assertEquals(pac12A, alamo.awayTeam);
    }

    @Test
    public void fullSeasonReachesNewSeasonEndWithCfpField() throws Exception {
        League league = createLeague();
        league.userTeam = league.findTeamAbbr("ALA");
        league.userTeam.userControlled = true;

        while (league.currentWeek < League.WEEK_SEASON_END) {
            league.playWeek();
        }

        assertEquals(League.WEEK_SEASON_END, league.currentWeek);
        assertTrue(league.hasScheduledBowls);
        assertNotNull(league.cfpField);
        assertEquals(Postseason.CFP_FIELD_SIZE, league.cfpField.size());
        assertNotNull(league.cfpFirstRound);
        assertEquals(4, league.cfpFirstRound.length);
        assertNotNull(league.ncg);
        assertTrue(league.ncg.hasPlayed);
        assertFalse(
                "NCW".equals(league.ncg.homeTeam.natChampWL)
                        && "NCW".equals(league.ncg.awayTeam.natChampWL));
        assertTrue(
                "NCW".equals(league.ncg.homeTeam.natChampWL)
                        || "NCW".equals(league.ncg.awayTeam.natChampWL));
        assertNotNull(league.bowlGames);
        assertTrue(league.bowlGames.length > 0);
    }

    private static Team firstInConference(League league, String conf) {
        for (Team t : league.teamList) {
            if (conf.equals(t.conference)) {
                return t;
            }
        }
        throw new IllegalStateException("No team in " + conf);
    }

    private static League createLeague() throws Exception {
        String teamsCsv = achijones.footballcoach.testing.FbsCsv.read();
        return new League(FIRST_NAMES, LAST_NAMES, teamsCsv);
    }
}
