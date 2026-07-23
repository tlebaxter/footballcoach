package CFBsimPack;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class OocContractBookTest {

    private static final String FIRST_NAMES =
            "Alex,Blake,Casey,Drew,Evan,Frankie,Gray,Hayden";
    private static final String LAST_NAMES =
            "Adams,Baker,Clark,Davis,Evans,Foster,Green,Hill";

    @Test
    public void buyGameMovesRecruitMoneyWhenSettled() throws Exception {
        League league = createOpenOocLeague();
        Team home = league.findTeamAbbr("ALA");
        Team away = league.findTeamAbbr("UMA");
        assertNotNull(home);
        assertNotNull(away);

        int homeBefore = home.recruitMoney;
        int awayBefore = away.recruitMoney;
        OocContract contract = league.oocContracts.signBuyGame(home, away, league.getYear(), 1);
        assertNotNull(contract);
        int guarantee = contract.games.get(0).guarantee;
        assertTrue(guarantee > 0);

        int week = -1;
        for (int w = 0; w < League.REGULAR_SEASON_WEEKS; w++) {
            if (home.isOpenOocWeek(w) && away.isOpenOocWeek(w)) {
                week = w;
                break;
            }
        }
        assertTrue(week >= 0);
        assertTrue(OocScheduleBuilder.placeFixedHomeOocGame(home, away, week, contract.id));

        Game game = home.gameSchedule.get(week);
        assertNotNull(game);
        game.homeScore = 45;
        game.awayScore = 10;
        league.oocContracts.settlePlayedGame(game);
        game.hasPlayed = true;

        assertEquals(homeBefore - guarantee, home.recruitMoney);
        assertEquals(awayBefore + guarantee, away.recruitMoney);
        assertTrue(contract.games.get(0).settled);
    }

    @Test
    public void homeAndHomeSwapsHomeInYearTwo() throws Exception {
        League league = createOpenOocLeague();
        Team a = league.findTeamAbbr("ORE");
        Team b = league.findTeamAbbr("CLE");
        assertNotNull(a);
        assertNotNull(b);

        OocContract contract = league.oocContracts.signHomeAndHome(a, b, league.getYear(), true);
        assertNotNull(contract);
        assertEquals(2, contract.games.size());
        assertEquals(a.abbr, contract.games.get(0).homeAbbr);
        assertEquals(b.abbr, contract.games.get(0).awayAbbr);
        assertEquals(b.abbr, contract.games.get(1).homeAbbr);
        assertEquals(a.abbr, contract.games.get(1).awayAbbr);
        assertEquals(0, contract.games.get(0).guarantee);
    }

    @Test
    public void saveAndLoadRoundTripsContracts() throws Exception {
        League league = createOpenOocLeague();
        Team home = league.findTeamAbbr("TEX");
        Team away = league.findTeamAbbr("RIC");
        assertNotNull(home);
        assertNotNull(away);
        OocContract signed = league.oocContracts.signBuyGame(home, away, league.getYear(), 2);
        assertNotNull(signed);

        StringBuilder sb = new StringBuilder();
        league.oocContracts.appendSave(sb);
        OocContractBook restored = new OocContractBook(league);
        restored.restore(new BufferedReader(new StringReader(
                sb.toString().replace("OOC_CONTRACTS\n", "").replace("END_OOC_CONTRACTS\n", "")
                        .replace("END_OOC_CONTRACTS", ""))));
        // Manual restore path: parse block properly
        OocContractBook book2 = new OocContractBook(league);
        BufferedReader reader = new BufferedReader(new StringReader(sb.toString()));
        assertEquals("OOC_CONTRACTS", reader.readLine());
        book2.restore(reader);
        assertFalse(book2.all().isEmpty());
        OocContract loaded = book2.findById(signed.id);
        assertNotNull(loaded);
        assertEquals(2, loaded.games.size());
        assertEquals(signed.games.get(0).guarantee, loaded.games.get(0).guarantee);
    }

    @Test
    public void materializePlacesCurrentYearContract() throws Exception {
        League league = createOpenOocLeague();
        Team home = league.findTeamAbbr("OSU");
        Team away = league.findTeamAbbr("HAW");
        assertNotNull(home);
        assertNotNull(away);
        assertNotNull(league.oocContracts.signBuyGame(home, away, league.getYear(), 1));

        // Rebuild open schedule then materialize
        league.prepareSeasonSchedule();
        boolean found = false;
        for (Game game : home.gameSchedule) {
            if (game == null) continue;
            Team opp = game.homeTeam == home ? game.awayTeam : game.homeTeam;
            if (opp == away) {
                found = true;
                assertEquals(home, game.homeTeam);
                assertNotNull(game.contractId);
            }
        }
        assertTrue("Expected buy-game contract to materialize on schedule", found);
    }

    @Test
    public void rivalryEncodeRoundTrip() {
        String encoded = "AUB:90;LSU:60;TEN:60";
        assertEquals(encoded, Rivalry.encode(Rivalry.parseEncoded(encoded)));
        assertEquals(90, Rivalry.parseEncoded("AUB").get(0).strength);
        assertEquals(90, Rivalry.parseEncoded("AUB:P").get(0).strength);
        assertEquals(60, Rivalry.parseEncoded("LSU:S").get(0).strength);
        assertEquals(35, Rivalry.parseEncoded("GAT:T").get(0).strength);
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
