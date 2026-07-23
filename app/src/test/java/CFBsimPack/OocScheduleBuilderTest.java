package CFBsimPack;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class OocScheduleBuilderTest {

    private static final String FIRST_NAMES =
            "Alex,Blake,Casey,Drew,Evan,Frankie,Gray,Hayden";
    private static final String LAST_NAMES =
            "Adams,Baker,Clark,Davis,Evans,Foster,Green,Hill";

    @Test
    public void suggestFillsAllOpenOocWeeks() throws Exception {
        League league = createOpenOocLeague();
        Team user = league.findTeamAbbr("ALA");
        assertNotNull(user);

        int openBefore = countOpenOoc(user);
        assertTrue(openBefore > 0);

        int placed = OocScheduleBuilder.suggestUserOocSchedule(user, league.teamList);
        assertEquals(openBefore, placed);
        assertEquals(0, countOpenOoc(user));
        assertEquals(openBefore, countFilledOoc(user));

        Set<String> opponents = new HashSet<>();
        for (int week = 0; week < League.REGULAR_SEASON_WEEKS; week++) {
            Game game = user.gameSchedule.get(week);
            if (game == null || user.isByeWeek(week)) {
                continue;
            }
            if (!isOoc(game)) {
                continue;
            }
            Team opp = game.homeTeam == user ? game.awayTeam : game.homeTeam;
            assertFalse(opp.conference.equals(user.conference));
            assertTrue(opponents.add(opp.abbr));
        }
        assertEquals(openBefore, opponents.size());
    }

    @Test
    public void suggestMayIncludeCrossConferenceRivalButDoesNotRequireIt() throws Exception {
        League league = createOpenOocLeague();
        Team user = league.findTeamAbbr("NDE");
        assertNotNull(user);
        String primary = user.highestRivalAbbr();
        Team rival = league.findTeamAbbr(primary);
        assertNotNull(rival);
        assertFalse(user.conference.equals(rival.conference));

        OocScheduleBuilder.suggestUserOocSchedule(user, league.teamList);
        assertEquals(0, countOpenOoc(user));

        // Soft preference only — verify schedule is complete; rival is optional.
        for (Game game : user.gameSchedule) {
            if (game == null) {
                continue;
            }
            Team opp = game.homeTeam == user ? game.awayTeam : game.homeTeam;
            if (opp == rival) {
                assertEquals("OOC Rivalry", game.gameName);
            }
        }
    }

    @Test
    public void clearAllAndResuggestRebuildsFullSlate() throws Exception {
        League league = createOpenOocLeague();
        Team user = league.findTeamAbbr("OSU");
        assertNotNull(user);

        OocScheduleBuilder.suggestUserOocSchedule(user, league.teamList);
        int filled = countFilledOoc(user);
        assertTrue(filled > 0);

        int firstOpenWeek = -1;
        for (int week = 0; week < League.REGULAR_SEASON_WEEKS; week++) {
            Game game = user.gameSchedule.get(week);
            if (game != null && isOoc(game)) {
                firstOpenWeek = week;
                break;
            }
        }
        assertTrue(firstOpenWeek >= 0);
        assertTrue(OocScheduleBuilder.clearUserOocGame(user, firstOpenWeek));
        assertEquals(1, countOpenOoc(user));
        assertEquals(filled - 1, countFilledOoc(user));

        OocScheduleBuilder.clearAllUserOocGames(user);
        assertEquals(0, countFilledOoc(user));
        assertEquals(filled, countOpenOoc(user));

        int placed = OocScheduleBuilder.suggestUserOocSchedule(user, league.teamList);
        assertEquals(filled, placed);
        assertEquals(0, countOpenOoc(user));
    }

    @Test
    public void leagueCompletesAfterUserSuggestions() throws Exception {
        League league = createOpenOocLeague();
        Team user = league.findTeamAbbr("MIA");
        assertNotNull(user);

        OocScheduleBuilder.suggestUserOocSchedule(user, league.teamList);
        assertEquals(0, countOpenOoc(user));

        league.completeOocSchedule();

        for (Team team : league.teamList) {
            for (int week = 0; week < League.REGULAR_SEASON_WEEKS; week++) {
                if (team.isByeWeek(week)) {
                    assertTrue(team.gameSchedule.get(week) == null);
                } else {
                    assertNotNull(
                            "Missing game for " + team.abbr + " week " + week,
                            team.gameSchedule.get(week));
                }
            }
        }
    }

    private static League createOpenOocLeague() throws IOException {
        Path asset = Paths.get("src/main/assets/fbs_2026.csv");
        if (!Files.exists(asset)) {
            asset = Paths.get("app/src/main/assets/fbs_2026.csv");
        }
        String teamsCsv = new String(Files.readAllBytes(asset), StandardCharsets.UTF_8);
        League league = new League(FIRST_NAMES, LAST_NAMES, teamsCsv, false);
        league.prepareSeasonSchedule();
        return league;
    }

    private static int countOpenOoc(Team team) {
        int count = 0;
        for (int week = 0; week < League.REGULAR_SEASON_WEEKS; week++) {
            if (team.isOpenOocWeek(week)) {
                count++;
            }
        }
        return count;
    }

    private static int countFilledOoc(Team team) {
        int count = 0;
        for (int week = 0; week < League.REGULAR_SEASON_WEEKS; week++) {
            Game game = team.gameSchedule.get(week);
            if (game != null && isOoc(game)) {
                count++;
            }
        }
        return count;
    }

    private static boolean isOoc(Game game) {
        return game.gameName.equals("OOC")
                || game.gameName.equals("OOC Rivalry")
                || game.gameName.equals("Rivalry Game OOC");
    }
}
