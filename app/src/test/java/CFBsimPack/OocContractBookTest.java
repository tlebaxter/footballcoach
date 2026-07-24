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
import static org.junit.Assert.assertNull;
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
        OocContractBook book2 = new OocContractBook(league);
        BufferedReader reader = new BufferedReader(new StringReader(sb.toString()));
        assertEquals("OOC_CONTRACTS", reader.readLine());
        book2.restore(reader);
        assertFalse(book2.all().isEmpty());
        OocContract loaded = book2.findById(signed.id);
        assertNotNull(loaded);
        assertEquals(2, loaded.games.size());
        assertEquals(signed.games.get(0).guarantee, loaded.games.get(0).guarantee);
        assertEquals(OocContract.Type.BUY, loaded.type);
        assertEquals(signed.mustFulfillByYear, loaded.mustFulfillByYear);
        assertEquals(signed.buyout, loaded.buyout);
    }

    @Test
    public void signSingleGameCreatesOneYearContract() throws Exception {
        League league = createOpenOocLeague();
        Team home = league.findTeamAbbr("MIA");
        Team away = league.findTeamAbbr("TOL");
        assertNotNull(home);
        assertNotNull(away);
        OocContract peer = league.oocContracts.signSingleGame(home, away, league.getYear(), false);
        assertNotNull(peer);
        assertEquals(OocContract.Type.SINGLE, peer.type);
        assertEquals(1, peer.games.size());
        assertEquals(0, peer.games.get(0).guarantee);
        assertEquals(league.getYear(), peer.mustFulfillByYear);

        Team power = league.findTeamAbbr("ALA");
        Team soft = league.findTeamAbbr("AKR");
        assertNotNull(power);
        assertNotNull(soft);
        OocContract buy = league.oocContracts.signSingleGame(power, soft, league.getYear() + 1, true);
        assertNotNull(buy);
        assertEquals(OocContract.Type.BUY, buy.type);
        assertTrue(buy.games.get(0).guarantee > 0);
    }

    @Test
    public void deferredHomeAndHomeSetsFulfillByReturnYear() throws Exception {
        League league = createOpenOocLeague();
        Team a = league.findTeamAbbr("USC");
        Team b = league.findTeamAbbr("NDE");
        assertNotNull(a);
        assertNotNull(b);
        int start = league.getYear();
        int ret = start + 4;
        OocContract contract = league.oocContracts.signHomeAndHome(a, b, start, ret, true);
        assertNotNull(contract);
        assertEquals(OocContract.Type.HOME_AND_HOME, contract.type);
        assertEquals(2, contract.games.size());
        assertEquals(start, contract.games.get(0).year);
        assertEquals(ret, contract.games.get(1).year);
        assertEquals(ret, contract.mustFulfillByYear);
        assertEquals(b.abbr, contract.games.get(1).homeAbbr);
    }

    @Test
    public void cancelChargesBuyout() throws Exception {
        League league = createOpenOocLeague();
        Team a = league.findTeamAbbr("UGA");
        Team b = league.findTeamAbbr("CLE");
        assertNotNull(a);
        assertNotNull(b);
        OocContract contract = league.oocContracts.signHomeAndHome(a, b, league.getYear(), true);
        assertNotNull(contract);
        int before = a.recruitMoney;
        assertTrue(league.oocContracts.cancel(contract.id, a));
        assertTrue(a.recruitMoney < before);
        assertTrue(a.recruitMoney <= before - Math.min(before, contract.buyout));
        assertTrue(league.oocContracts.findById(contract.id) == null);
    }

    @Test
    public void enforceBreachesRemovesPastDueDeals() throws Exception {
        League league = createOpenOocLeague();
        Team a = league.findTeamAbbr("USC");
        Team b = league.findTeamAbbr("NDE");
        assertNotNull(a);
        assertNotNull(b);
        int start = league.getYear();
        String pastLine = "CX,USC,NDE," + (start - 3) + ",2,H," + (start - 1) + ",250000,"
                + (start - 2) + ":USC:NDE:0:0:0|" + (start - 1) + ":NDE:USC:0:0:0";
        OocContract pastDue = OocContract.parse(pastLine);
        StringBuilder sb = new StringBuilder();
        sb.append("OOC_CONTRACTS\nNEXT_ID,99\n").append(pastDue.encode()).append("\nEND_OOC_CONTRACTS\n");
        BufferedReader reader = new BufferedReader(new StringReader(sb.toString()));
        assertEquals("OOC_CONTRACTS", reader.readLine());
        league.oocContracts.restore(reader);
        assertNotNull(league.oocContracts.findById("CX"));
        int moneyBefore = a.recruitMoney + b.recruitMoney;
        int count = league.oocContracts.enforceBreaches();
        assertTrue(count >= 1);
        assertTrue(league.oocContracts.findById("CX") == null);
        assertTrue(a.recruitMoney + b.recruitMoney <= moneyBefore);
        assertFalse(league.oocContracts.consumeBreachNotices().isEmpty());
    }

    @Test
    public void legacySaveLineStillParses() {
        String legacy = "C7,ALA,UMA,2026,2,2026:ALA:UMA:200000:30000:0|2027:ALA:UMA:200000:30000:0";
        OocContract c = OocContract.parse(legacy);
        assertEquals(OocContract.Type.BUY, c.type);
        assertEquals(2027, c.mustFulfillByYear);
        assertEquals(2, c.games.size());
        assertTrue(c.buyout > 0);
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

    @Test
    public void preferredWeekEncodesAndParses() {
        OocContractGame g = new OocContractGame(2026, "ALA", "UMA", 1000, 100, 3);
        String encoded = g.encode();
        assertTrue(encoded.endsWith(":3"));
        OocContractGame loaded = OocContractGame.parse(encoded);
        assertEquals(3, loaded.preferredWeek);
        assertEquals(-1, OocContractGame.parse("2026:ALA:UMA:1000:100:0").preferredWeek);
    }

    @Test
    public void rescheduleYearWithinFulfillBy() throws Exception {
        League league = createOpenOocLeague();
        Team home = league.findTeamAbbr("ALA");
        Team away = league.findTeamAbbr("UMA");
        assertNotNull(home);
        assertNotNull(away);
        int year = league.getYear();
        OocContract contract = league.oocContracts.signHomeAndHome(home, away, year, year + 3, true);
        assertNotNull(contract);
        assertTrue(league.oocContracts.canReschedule(contract.id, year));
        assertTrue(league.oocContracts.rescheduleYear(contract.id, year, year + 1));
        assertNull(contract.gameForYear(year));
        assertNotNull(contract.gameForYear(year + 1));
        assertEquals(home.abbr, contract.gameForYear(year + 1).homeAbbr);
        assertFalse(league.oocContracts.rescheduleYear(contract.id, year + 1, year + 4));
        assertFalse(league.oocContracts.rescheduleYear(contract.id, year + 1, year + 3));
    }

    @Test
    public void rescheduleWeekMovesCurrentYearPlacement() throws Exception {
        League league = createOpenOocLeague();
        Team home = league.findTeamAbbr("OSU");
        Team away = league.findTeamAbbr("HAW");
        assertNotNull(home);
        assertNotNull(away);
        OocContract contract = league.oocContracts.signBuyGame(home, away, league.getYear(), 1);
        assertNotNull(contract);
        league.prepareSeasonSchedule();

        int placedWeek = -1;
        for (int w = 0; w < League.REGULAR_SEASON_WEEKS; w++) {
            Game g = home.gameSchedule.get(w);
            if (g != null && contract.id.equals(g.contractId)) {
                placedWeek = w;
                break;
            }
        }
        assertTrue(placedWeek >= 0);

        int targetWeek = -1;
        for (int w = 0; w < League.REGULAR_SEASON_WEEKS; w++) {
            if (w == placedWeek) {
                continue;
            }
            if (home.isOpenOocWeek(w) && away.isOpenOocWeek(w)) {
                targetWeek = w;
                break;
            }
        }
        assertTrue("Need a second shared open week", targetWeek >= 0);
        assertTrue(league.oocContracts.rescheduleWeek(contract.id, league.getYear(), targetWeek));
        assertEquals(targetWeek, contract.games.get(0).preferredWeek);
        Game moved = home.gameSchedule.get(targetWeek);
        assertNotNull(moved);
        assertEquals(contract.id, moved.contractId);
        assertNull(home.gameSchedule.get(placedWeek));
    }

    @Test
    public void cannotRescheduleSettledOrPastFulfillBy() throws Exception {
        League league = createOpenOocLeague();
        Team home = league.findTeamAbbr("TEX");
        Team away = league.findTeamAbbr("RIC");
        assertNotNull(home);
        assertNotNull(away);
        OocContract contract = league.oocContracts.signSingleGame(home, away, league.getYear(), false);
        assertNotNull(contract);
        contract.games.get(0).settled = true;
        assertFalse(league.oocContracts.canReschedule(contract.id, league.getYear()));

        String pastLine = "CY,TEX,RIC," + (league.getYear() - 2) + ",1,S,"
                + (league.getYear() - 1) + ",0,"
                + league.getYear() + ":TEX:RIC:0:0:0";
        OocContract past = OocContract.parse(pastLine);
        StringBuilder sb = new StringBuilder();
        sb.append("OOC_CONTRACTS\nNEXT_ID,99\n").append(past.encode()).append("\nEND_OOC_CONTRACTS\n");
        BufferedReader reader = new BufferedReader(new StringReader(sb.toString()));
        assertEquals("OOC_CONTRACTS", reader.readLine());
        league.oocContracts.restore(reader);
        assertFalse(league.oocContracts.canReschedule("CY", league.getYear()));
    }

    @Test
    public void autoSignFutureDealsNoopsWhenUserTeamUnset() throws Exception {
        League league = createOpenOocLeague();
        assertNull(league.userTeam);
        int before = league.oocContracts.all().size();
        league.oocContracts.autoSignFutureDeals(league.teamList);
        assertEquals(before, league.oocContracts.all().size());
    }

    @Test
    public void autoSignFutureDealsSkipsUserTeam() throws Exception {
        League league = createOpenOocLeague();
        Team user = league.findTeamAbbr("ALA");
        assertNotNull(user);
        league.userTeam = user;
        user.userControlled = true;

        int before = league.oocContracts.all().size();
        league.oocContracts.autoSignFutureDeals(league.teamList);
        assertTrue(league.oocContracts.all().size() > before);
        for (OocContract c : league.oocContracts.all()) {
            assertFalse("AI must not sign deals involving user", c.involves(user.abbr));
        }
    }

    @Test
    public void suggestAndRevertUserFutureDeals() throws Exception {
        League league = createOpenOocLeague();
        Team user = league.findTeamAbbr("ALA");
        assertNotNull(user);
        league.userTeam = user;
        user.userControlled = true;
        int moneyBefore = user.recruitMoney;

        int signed = league.oocContracts.suggestUserFutureDeals(user, league.teamList);
        assertTrue(signed > 0);
        assertTrue(league.oocContracts.hasSuggestedUserDeals());
        assertEquals(signed, league.oocContracts.forTeam(user.abbr).size());

        int removed = league.oocContracts.revertSuggestedUserDeals();
        assertEquals(signed, removed);
        assertFalse(league.oocContracts.hasSuggestedUserDeals());
        assertEquals(0, league.oocContracts.forTeam(user.abbr).size());
        assertEquals(moneyBefore, user.recruitMoney);
    }

    @Test
    public void suggestUserFutureDealsReplacesPriorBatch() throws Exception {
        League league = createOpenOocLeague();
        Team user = league.findTeamAbbr("OSU");
        assertNotNull(user);
        league.userTeam = user;
        user.userControlled = true;

        int first = league.oocContracts.suggestUserFutureDeals(user, league.teamList);
        assertTrue(first > 0);
        java.util.ArrayList<String> firstIds = new java.util.ArrayList<>();
        for (OocContract c : league.oocContracts.forTeam(user.abbr)) {
            firstIds.add(c.id);
        }

        int second = league.oocContracts.suggestUserFutureDeals(user, league.teamList);
        assertTrue(second > 0);
        for (String id : firstIds) {
            assertNull(league.oocContracts.findById(id));
        }
        assertTrue(league.oocContracts.hasSuggestedUserDeals());
        assertEquals(second, league.oocContracts.forTeam(user.abbr).size());
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
