package CFBsimPack;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MidSeasonSaveTest {

    private static final String FIRST_NAMES =
            "Alex,Blake,Casey,Drew,Evan,Frankie,Gray,Hayden";
    private static final String LAST_NAMES =
            "Adams,Baker,Clark,Davis,Evans,Foster,Green,Hill";

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void midSeasonCoachedGameRoundTrips() throws Exception {
        League league = createLeague();
        Team user = league.teamList.get(0);
        league.userTeam = user;
        user.userControlled = true;

        Game game = firstPlayableGame(user);
        assertNotNull(game);
        game.startGame();
        game.resolveUntilDecided();
        assertTrue(game.hasPlayed);
        assertTrue(game.isDecided());
        assertEquals(0, league.currentWeek);

        int homeScore = game.homeScore;
        int awayScore = game.awayScore;
        int userWins = user.wins;
        int userLosses = user.losses;
        assertFalse(user.gameWLSchedule.isEmpty());

        Player qb = user.teamQBs.get(0);
        qb.gamesPlayed = Math.max(1, qb.gamesPlayed);
        qb.statsWins = userWins > 0 ? 1 : 0;
        qb.seasonSnaps = Math.max(25, qb.seasonSnaps);
        int expectedSnaps = qb.seasonSnaps;
        qb.seasonStats.passYards = 212;
        qb.injury = new Injury(3, "Knee", qb);

        File saveFile = temporaryFolder.newFile("midseason.cfb");
        assertTrue(league.saveLeague(saveFile));
        String raw = new String(Files.readAllBytes(saveFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(raw.contains("SAVE_VERSION,9"));
        assertTrue(raw.contains("SEASON_PROGRESS"));
        assertTrue(raw.contains("TEAM_SEASON"));
        assertTrue(raw.contains("|SEASON,"));

        League loaded = new League(saveFile, FIRST_NAMES, LAST_NAMES);
        assertEquals(0, loaded.currentWeek);
        Team loadedUser = loaded.findTeamAbbr(user.abbr);
        assertNotNull(loadedUser);
        assertEquals(userWins, loadedUser.wins);
        assertEquals(userLosses, loadedUser.losses);
        assertFalse(loadedUser.gameWLSchedule.isEmpty());
        assertEquals(user.gameWLSchedule.get(0), loadedUser.gameWLSchedule.get(0));

        Game loadedGame = firstPlayableGame(loadedUser);
        assertNotNull(loadedGame);
        assertTrue(loadedGame.hasPlayed);
        assertEquals(homeScore, loadedGame.homeScore);
        assertEquals(awayScore, loadedGame.awayScore);

        Player loadedQb = findPlayer(loadedUser, qb.position, qb.name);
        assertNotNull(loadedQb);
        assertEquals(qb.gamesPlayed, loadedQb.gamesPlayed);
        assertEquals(expectedSnaps, loadedQb.seasonSnaps);
        assertEquals(212, loadedQb.seasonStats.passYards);
        assertTrue(loadedQb.isInjured);
        assertNotNull(loadedQb.injury);
        assertEquals(3, loadedQb.injury.getDuration());
        assertEquals("Knee", loadedQb.injury.getDescription());
    }

    @Test
    public void postseasonBracketRoundTripsThroughCfb() throws Exception {
        League league = createLeague();
        league.userTeam = league.teamList.get(0);
        league.userTeam.userControlled = true;
        while (league.currentWeek < League.WEEK_CFP_FIRST_ROUND) {
            league.playWeek();
        }
        assertTrue(league.hasScheduledBowls);
        assertEquals(Postseason.CFP_FIELD_SIZE, league.cfpField.size());
        assertNotNull(league.cfpFirstRound);
        assertEquals(4, league.cfpFirstRound.length);

        String seed0 = league.cfpField.get(0).abbr;
        String fr0 = league.cfpFirstRound[0].gameName
                + "|" + league.cfpFirstRound[0].homeTeam.abbr
                + "|" + league.cfpFirstRound[0].awayTeam.abbr;
        int bowlCount = league.bowlGames.length;

        File saveFile = temporaryFolder.newFile("postseason.cfb");
        assertTrue(league.saveLeague(saveFile));
        String raw = new String(Files.readAllBytes(saveFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(raw.contains("POSTSEASON"));
        assertTrue(raw.contains("\nFR\n"));
        assertTrue(raw.contains("END_POSTSEASON"));

        League loaded = new League(saveFile, FIRST_NAMES, LAST_NAMES);
        assertEquals(League.WEEK_CFP_FIRST_ROUND, loaded.currentWeek);
        assertTrue(loaded.hasScheduledBowls);
        assertEquals(Postseason.CFP_FIELD_SIZE, loaded.cfpField.size());
        assertEquals(seed0, loaded.cfpField.get(0).abbr);
        assertNotNull(loaded.cfpFirstRound);
        assertEquals(4, loaded.cfpFirstRound.length);
        assertEquals(
                fr0,
                loaded.cfpFirstRound[0].gameName
                        + "|" + loaded.cfpFirstRound[0].homeTeam.abbr
                        + "|" + loaded.cfpFirstRound[0].awayTeam.abbr);
        assertEquals(bowlCount, loaded.bowlGames.length);
        assertTrue(loaded.cfpQuarters == null);
        boolean anyCcg = false;
        for (Conference c : loaded.conferences) {
            if (c.getCcg() != null) {
                anyCcg = true;
                break;
            }
        }
        assertTrue(anyCcg);
    }

    @Test
    public void playWeekProgressRoundTripsCurrentWeek() throws Exception {
        League league = createLeague();
        league.userTeam = league.teamList.get(0);
        league.userTeam.userControlled = true;
        league.playWeek();
        assertEquals(1, league.currentWeek);

        File saveFile = temporaryFolder.newFile("week1.cfb");
        assertTrue(league.saveLeague(saveFile));

        League loaded = new League(saveFile, FIRST_NAMES, LAST_NAMES);
        assertEquals(1, loaded.currentWeek);
        for (Conference conf : loaded.conferences) {
            assertEquals(1, conf.week);
        }
        Team user = loaded.userTeam;
        assertNotNull(user);
        assertEquals(user.wins + user.losses + byeCount(user), user.gameWLSchedule.size());
    }

    @Test
    public void version6LoadsWithoutSeasonProgress() throws Exception {
        League league = createLeague();
        league.userTeam = league.teamList.get(0);
        league.userTeam.userControlled = true;
        league.playWeek();
        File v8 = temporaryFolder.newFile("as-v8.cfb");
        assertTrue(league.saveLeague(v8));

        String raw = new String(Files.readAllBytes(v8.toPath()), StandardCharsets.UTF_8);
        raw = raw.replace("SAVE_VERSION,9", "SAVE_VERSION,6");
        raw = stripBlock(raw, "SEASON_PROGRESS", "END_SEASON_PROGRESS");
        raw = stripBlock(raw, "TEAM_SEASON", "END_TEAM_SEASON");
        // Drop |SEASON suffixes so player lines look like v6
        raw = raw.replaceAll("\\|SEASON,[^|%\n]*", "");
        // Drop mid-season result payloads from schedule matchup tokens
        raw = raw.replaceAll("\\|1(?:\\|[^,]*)?", "");

        File v6 = temporaryFolder.newFile("as-v6.cfb");
        Files.write(v6.toPath(), raw.getBytes(StandardCharsets.UTF_8));

        League loaded = new League(v6, FIRST_NAMES, LAST_NAMES);
        assertEquals(0, loaded.currentWeek);
        assertEquals(0, loaded.userTeam.wins);
        Game g = firstPlayableGame(loaded.userTeam);
        assertNotNull(g);
        assertFalse(g.hasPlayed);
    }

    @Test
    public void unknownSaveVersionRejected() throws Exception {
        File tmp = temporaryFolder.newFile("badver.cfb");
        Files.write(tmp.toPath(), (
                "2026: ABC (0-0) 0 CCs, 0 NCs>%\n"
                        + "END_LEAGUE_HIST\n"
                        + "END_HEISMAN_HIST\n"
                        + "SAVE_VERSION,5\n"
                        + "TEAM_COUNT,1\n"
        ).getBytes(StandardCharsets.UTF_8));
        try {
            new League(tmp, FIRST_NAMES, LAST_NAMES);
            fail("Expected exception for unsupported save version");
        } catch (AssertionError ae) {
            throw ae;
        } catch (Throwable expected) {
            assertTrue(expected instanceof Exception || expected instanceof Error);
        }
    }

    private static int byeCount(Team team) {
        int n = 0;
        for (String wl : team.gameWLSchedule) {
            if ("BYE".equals(wl)) n++;
        }
        return n;
    }

    private static String stripBlock(String raw, String start, String end) {
        int a = raw.indexOf(start + "\n");
        if (a < 0) return raw;
        int b = raw.indexOf(end + "\n", a);
        if (b < 0) return raw;
        return raw.substring(0, a) + raw.substring(b + end.length() + 1);
    }

    private static Game firstPlayableGame(Team team) {
        for (int week = 0; week < team.gameSchedule.size(); week++) {
            if (team.isByeWeek(week)) continue;
            Game g = team.gameSchedule.get(week);
            if (g != null) return g;
        }
        return null;
    }

    private static Player findPlayer(Team team, String position, String name) {
        for (Player p : team.getAllPlayers()) {
            if (position.equals(p.position) && name.equals(p.name)) {
                return p;
            }
        }
        return null;
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
