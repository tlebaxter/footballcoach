package CFBsimPack;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProgramOffersTest {

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
        assertTrue(ProgramOffers.canPayToStay(mid));
        assertTrue(ProgramOffers.draftStayBonus(mid, null) >= 100000);
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
}
