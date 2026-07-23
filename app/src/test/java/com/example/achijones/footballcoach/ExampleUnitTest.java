package com.example.achijones.footballcoach;

import CFBsimPack.Game;
import CFBsimPack.League;
import CFBsimPack.LeagueOffseason;
import CFBsimPack.Team;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class ExampleUnitTest {

    private static final String FIRST_NAMES =
            "Alex,Blake,Casey,Drew,Evan,Frankie,Gray,Hayden";
    private static final String LAST_NAMES =
            "Adams,Baker,Clark,Davis,Evans,Foster,Green,Hill";

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void createsComplete2026FbsSchedule() throws Exception {
        League league = createLeague();

        assertEquals(138, league.teamList.size());
        assertEquals(11, league.conferences.size());
        assertEquals(2026, league.getYear());
        assertEquals(18, league.findConference("Big Ten").confTeams.size());
        assertEquals(8, league.findConference("Pac-12").confTeams.size());
        assertEquals(2, league.findConference("Independents").confTeams.size());
        assertFalse(league.findConference("Independents").hasChampionship);

        for (Team team : league.teamList) {
            assertEquals(League.REGULAR_SEASON_WEEKS, team.gameSchedule.size());
            assertTrue("Missing bye for " + team.abbr, team.byeWeek >= 0);
            assertTrue(team.byeWeek < League.REGULAR_SEASON_WEEKS);
            assertNull("Bye week should be empty for " + team.abbr, team.gameSchedule.get(team.byeWeek));

            Set<Team> opponents = new HashSet<>();
            boolean playsRival = false;
            int games = 0;
            for (int week = 0; week < League.REGULAR_SEASON_WEEKS; week++) {
                Game game = team.gameSchedule.get(week);
                if (week == team.byeWeek) {
                    assertNull(game);
                    continue;
                }
                assertNotNull("Missing game week " + week + " for " + team.abbr, game);
                games++;
                Team opponent = game.homeTeam == team ? game.awayTeam : game.homeTeam;
                assertTrue(game.homeTeam == team || game.awayTeam == team);
                assertTrue("Duplicate opponent for " + team.abbr, opponents.add(opponent));
                playsRival |= opponent.abbr.equals(team.rivalTeam);
            }
            assertEquals(League.REGULAR_SEASON_GAMES, games);
            assertTrue("Missing rivalry game for " + team.abbr, playsRival);
        }

        for (int week = 0; week < League.REGULAR_SEASON_WEEKS; week++) {
            Set<Game> games = Collections.newSetFromMap(new IdentityHashMap<Game, Boolean>());
            int playing = 0;
            int byes = 0;
            for (Team team : league.teamList) {
                if (team.byeWeek == week) {
                    byes++;
                    assertNull(team.gameSchedule.get(week));
                    continue;
                }
                Game game = team.gameSchedule.get(week);
                assertNotNull(game);
                games.add(game);
                playing++;
                assertSame(game, game.homeTeam.gameSchedule.get(week));
                assertSame(game, game.awayTeam.gameSchedule.get(week));
            }
            assertEquals(0, playing % 2);
            assertEquals(playing / 2, games.size());
            assertEquals(138, playing + byes);
        }
    }

    @Test
    public void staggersByesWithinConference() throws Exception {
        League league = createLeague();
        for (CFBsimPack.Conference conference : league.conferences) {
            if (!conference.hasChampionship || conference.confTeams.size() < 4) {
                continue;
            }
            Map<Integer, Integer> byesByWeek = new HashMap<>();
            for (Team team : conference.confTeams) {
                Integer count = byesByWeek.get(team.byeWeek);
                byesByWeek.put(team.byeWeek, count == null ? 1 : count + 1);
            }
            int maxSameWeek = 0;
            for (int count : byesByWeek.values()) {
                maxSameWeek = Math.max(maxSameWeek, count);
            }
            // Prefer flex-week stagger (~size/4). Sparse open weeks (e.g. Pac-12
            // extras) may need a denser pile so OOC matching stays possible.
            int maxAllowed = Math.max(5, (conference.confTeams.size() + 2) / 3);
            assertTrue(
                    conference.confName + " piled byes onto one week: " + byesByWeek,
                    maxSameWeek <= maxAllowed && maxSameWeek < conference.confTeams.size());
        }
    }

    @Test
    public void usesVariableConferenceAndOocCounts() throws Exception {
        League league = createLeague();

        for (Team team : league.findConference("Pac-12").confTeams) {
            assertEquals(7, conferenceGameCount(team));
            assertEquals(5, oocGameCount(team));
        }
        for (Team team : league.findConference("Big Ten").confTeams) {
            assertEquals(9, conferenceGameCount(team));
            assertEquals(3, oocGameCount(team));
        }
        for (Team team : league.findConference("ACC").confTeams) {
            assertEquals(8, conferenceGameCount(team));
            assertEquals(4, oocGameCount(team));
        }
        for (Team team : league.findConference("MAC").confTeams) {
            assertEquals(8, conferenceGameCount(team));
            assertEquals(4, oocGameCount(team));
        }
        for (Team team : league.findConference("Independents").confTeams) {
            assertEquals(0, conferenceGameCount(team));
            assertEquals(12, oocGameCount(team));
        }
    }

    @Test
    public void savesAndLoadsVariableLeagueSize() throws Exception {
        League league = createLeague();
        league.userTeam = league.teamList.get(0);
        league.userTeam.userControlled = true;
        File saveFile = temporaryFolder.newFile("fbs-2026.cfb");

        assertTrue(league.saveLeague(saveFile));

        League loaded = new League(saveFile, FIRST_NAMES, LAST_NAMES);
        assertEquals(138, loaded.teamList.size());
        assertEquals(11, loaded.conferences.size());
        assertEquals(League.FIRST_SEASON_YEAR, loaded.getYear());
        assertEquals(league.userTeam.name, loaded.userTeam.name);
        assertEquals(League.REGULAR_SEASON_WEEKS, loaded.userTeam.gameSchedule.size());
    }

    @Test
    public void recruitingEntryPathAfterFullSeason() throws Exception {
        League league = createLeague();
        league.userTeam = league.findTeamAbbr("ALA");
        league.userTeam.userControlled = true;

        while (league.currentWeek < League.WEEK_SEASON_END) {
            league.playWeek();
        }

        league.getPlayersLeaving();
        String[] mock = league.getMockDraftPlayersList();
        assertEquals(64, mock.length);

        league.updateTeamHistories();
        league.updateLeagueHistory();
        league.userTeam.resetStats();
        league.advanceSeason();

        File saveFile = temporaryFolder.newFile("recruiting.cfb");
        assertTrue(league.saveLeague(saveFile));

        String recruits = league.userTeam.getRecruitsInfoSaveFile();
        assertTrue(recruits.length() > 100);
        assertTrue(league.userTeam.getPlayerInfoSaveFile().length() > 100);
    }

    @Test
    public void advancesMultipleSeasonsWithOddConferences() throws Exception {
        League league = createLeague();
        league.userTeam = league.findTeamAbbr("SYR");
        league.userTeam.userControlled = true;

        for (int season = 0; season < 8; season++) {
            while (league.currentWeek < League.WEEK_SEASON_END) {
                league.playWeek();
            }
            league.getPlayersLeaving();
            LeagueOffseason off = new LeagueOffseason(league);
            league.offseason = off;
            off.advanceYearAutomated();

            for (Team team : league.teamList) {
                assertTrue(
                        "Season " + season + " left " + team.abbr + " without a QB",
                        team.teamQBs.size() > 0);
                assertEquals(
                        "Season " + season + " incomplete schedule for " + team.abbr,
                        League.REGULAR_SEASON_WEEKS,
                        team.gameSchedule.size());
                assertTrue(team.byeWeek >= 0);
                int games = 0;
                for (int week = 0; week < League.REGULAR_SEASON_WEEKS; week++) {
                    Game game = team.gameSchedule.get(week);
                    if (week == team.byeWeek) {
                        assertNull("Season " + season + " bye not empty for " + team.abbr, game);
                    } else {
                        assertNotNull("Season " + season + " null week for " + team.abbr, game);
                        games++;
                    }
                }
                assertEquals(League.REGULAR_SEASON_GAMES, games);
            }
        }
    }

    private static League createLeague() throws IOException {
        Path asset = Paths.get("src/main/assets/fbs_2026.csv");
        if (!Files.exists(asset)) {
            asset = Paths.get("app/src/main/assets/fbs_2026.csv");
        }
        String teamsCsv = new String(Files.readAllBytes(asset), StandardCharsets.UTF_8);
        return new League(
                FIRST_NAMES,
                LAST_NAMES,
                teamsCsv,
                false);
    }

    private static int conferenceGameCount(Team team) {
        int count = 0;
        for (Game game : team.gameSchedule) {
            if (game == null) {
                continue;
            }
            Team opponent = game.homeTeam == team ? game.awayTeam : game.homeTeam;
            if (opponent.conference.equals(team.conference)) {
                count++;
            }
        }
        return count;
    }

    private static int oocGameCount(Team team) {
        int count = 0;
        for (Game game : team.gameSchedule) {
            if (game == null) {
                continue;
            }
            Team opponent = game.homeTeam == team ? game.awayTeam : game.homeTeam;
            if (!opponent.conference.equals(team.conference)) {
                count++;
            }
        }
        return count;
    }
}
