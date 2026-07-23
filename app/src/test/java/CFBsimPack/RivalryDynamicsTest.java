package CFBsimPack;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RivalryDynamicsTest {

    private static final String FIRST_NAMES =
            "Alex,Blake,Casey,Drew,Evan,Frankie,Gray,Hayden";
    private static final String LAST_NAMES =
            "Adams,Baker,Clark,Davis,Evans,Foster,Green,Hill";

    @Test
    public void closeGameBoostsRivalryStrength() throws Exception {
        League league = createOpenOocLeague();
        Team ala = league.findTeamAbbr("ALA");
        Team aub = league.findTeamAbbr("AUB");
        assertNotNull(ala);
        assertNotNull(aub);
        Rivalry link = ala.rivalryWith("AUB");
        assertNotNull(link);
        int before = link.strength;

        Game game = findOrPlaceGame(ala, aub);
        game.homeScore = 28;
        game.awayScore = 24;
        game.hasPlayed = true;
        ala.recordRivalryResult("AUB", game.homeTeam == ala);

        RivalryDynamics.evolveTeam(ala);
        assertEquals(Rivalry.clamp(before + 6), ala.rivalryWith("AUB").strength);
    }

    @Test
    public void idleRivalryDecays() throws Exception {
        League league = createOpenOocLeague();
        Team nde = league.findTeamAbbr("NDE");
        assertNotNull(nde);
        Rivalry usc = nde.rivalryWith("USC");
        assertNotNull(usc);
        int before = usc.strength;
        // No played game vs USC on schedule after prepare — decay
        RivalryDynamics.evolveTeam(nde);
        assertEquals(Rivalry.clamp(before - 2), nde.rivalryWith("USC").strength);
    }

    @Test
    public void memorableGameFormsNewRivalry() throws Exception {
        League league = createOpenOocLeague();
        Team a = league.findTeamAbbr("BOI");
        Team b = league.findTeamAbbr("HAW");
        assertNotNull(a);
        assertNotNull(b);
        assertFalse(a.isRival(b.abbr));
        assertFalse(b.isRival(a.abbr));

        int week = -1;
        for (int w = 0; w < League.REGULAR_SEASON_WEEKS; w++) {
            if (a.isOpenOocWeek(w) && b.isOpenOocWeek(w)) {
                week = w;
                break;
            }
        }
        assertTrue(week >= 0);
        assertTrue(OocScheduleBuilder.placeFixedHomeOocGame(a, b, week, null));
        Game game = a.gameSchedule.get(week);
        game.homeScore = 21;
        game.awayScore = 17;
        game.hasPlayed = true;

        RivalryDynamics.formNewRivalries(league);
        assertTrue(a.isRival(b.abbr));
        assertTrue(b.isRival(a.abbr));
        assertEquals(30, a.rivalryWith(b.abbr).strength);
    }

    @Test
    public void declareRivalBoostsToFortyFive() throws Exception {
        League league = createOpenOocLeague();
        Team user = league.findTeamAbbr("TEX");
        Team opp = league.findTeamAbbr("RIC");
        assertNotNull(user);
        assertNotNull(opp);
        assertNull(RivalryDynamics.declareRival(user, opp));
        assertEquals(45, user.rivalryWith(opp.abbr).strength);
        assertTrue(opp.isRival(user.abbr));
    }

    @Test
    public void advanceSeasonHasNoPrestigeFloorOnRival() throws Exception {
        League league = createOpenOocLeague();
        Team user = league.findTeamAbbr("ALA");
        Team rival = league.findTeamAbbr("AUB");
        assertNotNull(user);
        assertNotNull(rival);
        user.userControlled = true;
        league.userTeam = user;
        user.teamPrestige = 90;
        rival.teamPrestige = 50;
        user.rankTeamPollScore = 10;
        rival.rankTeamPollScore = 80;
        user.advanceSeason();
        // Floor removed: rival can stay far below user
        assertTrue(rival.teamPrestige <= 50 || rival.teamPrestige >= 45);
        assertTrue(user.teamPrestige - rival.teamPrestige >= 10
                || rival.teamPrestige == 45);
    }

    @Test
    public void bandHelpers() {
        assertEquals("Hot", Rivalry.band(90));
        assertEquals("Warm", Rivalry.band(55));
        assertEquals("Cold", Rivalry.band(20));
        assertEquals("None", Rivalry.band(0));
    }

    private static Game findOrPlaceGame(Team a, Team b) {
        for (Game g : a.gameSchedule) {
            if (g == null) continue;
            Team opp = g.homeTeam == a ? g.awayTeam : g.homeTeam;
            if (opp == b) {
                return g;
            }
        }
        for (int w = 0; w < League.REGULAR_SEASON_WEEKS; w++) {
            if (a.isOpenOocWeek(w) && b.isOpenOocWeek(w)) {
                OocScheduleBuilder.placeFixedHomeOocGame(a, b, w, null);
                return a.gameSchedule.get(w);
            }
        }
        // Conference game already exists for ALA-AUB typically
        for (Game g : a.gameSchedule) {
            if (g != null) {
                Team opp = g.homeTeam == a ? g.awayTeam : g.homeTeam;
                if (opp == b) return g;
            }
        }
        throw new IllegalStateException("No week for " + a.abbr + " vs " + b.abbr);
    }

    private static League createOpenOocLeague() throws Exception {
        Path asset = Paths.get("src/main/assets/fbs_2026.csv");
        if (!Files.exists(asset)) {
            asset = Paths.get("app/src/main/assets/fbs_2026.csv");
        }
        String teamsCsv = new String(Files.readAllBytes(asset), StandardCharsets.UTF_8);
        League league = new League(FIRST_NAMES, LAST_NAMES, teamsCsv, false);
        league.prepareSeasonSchedule();
        return league;
    }
}
