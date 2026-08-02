package CFBsimPack;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProgramOffersTest {
    private static final String FIRST_NAMES =
            "Alex,Blake,Casey,Drew,Evan,Frankie,Gray,Hayden";
    private static final String LAST_NAMES =
            "Adams,Baker,Clark,Davis,Evans,Foster,Green,Hill";

    @Test
    public void maxContractYearsCapsByEligibility() {
        assertEquals(4, ProgramOffers.maxContractYears(playerYear(1)));
        assertEquals(4, ProgramOffers.maxContractYears(playerYear(2)));
        assertEquals(3, ProgramOffers.maxContractYears(playerYear(3)));
        assertEquals(2, ProgramOffers.maxContractYears(playerYear(4)));
        assertEquals(1, ProgramOffers.maxContractYears(playerYear(5)));
        assertEquals(1, ProgramOffers.maxContractYears(null));
    }

    @Test
    public void draftProjectionLocksTopThreeRounds() {
        Player elite = new Player();
        elite.year = 3;
        elite.ratOvr = 95;
        elite.position = "QB";
        elite.wonHeisman = true;
        int round = ProgramOffers.projectDraftRound(elite);
        assertTrue(round >= 1 && round <= 3);
        assertTrue(ProgramOffers.isLockedDraftRound(round));
        assertFalse(ProgramOffers.canPayToStay(elite));
    }

    @Test
    public void midRoundDraftCanPayToStay() {
        Player mid = new Player();
        mid.year = 3;
        mid.ratOvr = 86;
        mid.position = "WR";
        mid.projectedDraftRound = 4;
        assertTrue(ProgramOffers.canRetainDraftEligible(mid));
        assertTrue(ProgramOffers.annualNilFor(mid, null, 1) >= 25000);
    }

    @Test
    public void kickersAreNotDraftProspects() {
        Player k = new Player();
        k.year = 4;
        k.ratOvr = 99;
        k.position = "K";
        assertEquals(0, ProgramOffers.projectDraftRound(k));
    }

    @Test
    public void acceptsOfferRequiresMinimumStatus() {
        Player star = new Player();
        star.ratOvr = 88;
        assertFalse(ProgramOffers.acceptsOffer(star, RosterStatus.PWO, 3));
        assertTrue(ProgramOffers.acceptsOffer(star, RosterStatus.SCHOLARSHIP_PLUS_NIL, 3));
        assertEquals(RosterStatus.SCHOLARSHIP_PLUS_NIL, ProgramOffers.minimumAcceptable(star, 3));
    }

    @Test
    public void lengthAnnualBumpGrowsWithYears() {
        Player young = playerYear(1);
        assertEquals(1.0, ProgramOffers.lengthAnnualBump(young, 1), 0.001);
        assertTrue(ProgramOffers.lengthAnnualBump(young, 3)
                > ProgramOffers.lengthAnnualBump(young, 2));
    }

    @Test
    public void brandAndPipelineLowerRequiredCash() throws Exception {
        League league = createLeague();
        Team destination = league.findTeamAbbr("ALA");
        Player player = playerYear(3);
        player.position = "WR";
        player.ratOvr = 70;
        player.ratPot = 75;
        player.transferReason = TransferReason.MOVE_UP;

        destination.programProfile = new ProgramProfile(45, 45, 45, 70, 45, 50, 50);
        int lowBrandAsk = ProgramOffers.nilAmountFor(player, destination);
        destination.programProfile = new ProgramProfile(95, 95, 95, 90, 95, 95, 95);
        int eliteBrandAsk = ProgramOffers.nilAmountFor(player, destination);

        assertTrue(eliteBrandAsk < lowBrandAsk);
    }

    @Test
    public void singleDealIsLimitedToTwentyPercentOfPurse() throws Exception {
        League league = createLeague();
        Team destination = league.findTeamAbbr("ALA");
        int limit = ProgramOffers.maxSingleDeal(destination);

        assertTrue(limit <= NilMoney.yearlyBudget(destination.programProfile) / 5);
        assertTrue(limit <= 7_000_000);
    }

    @Test
    public void draftRoundLabel() {
        assertEquals("Rd 2", ProgramOffers.draftRoundLabel(2));
        assertEquals("UDFA", ProgramOffers.draftRoundLabel(0));
        assertEquals("UDFA", ProgramOffers.draftRoundLabel(9));
    }

    private static Player playerYear(int year) {
        Player p = new Player();
        p.year = year;
        return p;
    }

    private static League createLeague() throws Exception {
        String teamsCsv = achijones.footballcoach.testing.FbsCsv.read();
        return new League(FIRST_NAMES, LAST_NAMES, teamsCsv, false);
    }
}
