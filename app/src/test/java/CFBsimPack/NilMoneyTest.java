package CFBsimPack;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NilMoneyTest {

    @Test
    public void formatUsesDollarsThousandsAndMillions() {
        assertEquals("$0", NilMoney.format(0));
        assertEquals("$500", NilMoney.format(500));
        assertEquals("$25K", NilMoney.format(25000));
        assertEquals("$1.5K", NilMoney.format(1500));
        assertEquals("$2M", NilMoney.format(2_000_000));
        assertEquals("$0", NilMoney.format(-100));
    }

    @Test
    public void yearlyBudgetClampsPrestigeAndScalesUp() {
        int low = NilMoney.yearlyBudget(10);
        int mid = NilMoney.yearlyBudget(75);
        int high = NilMoney.yearlyBudget(99);
        int capped = NilMoney.yearlyBudget(200);
        assertTrue(low > 0);
        assertTrue(mid > low);
        assertTrue(high > mid);
        assertEquals(high, capped);
        assertEquals(0, low % 1000);
    }

    @Test
    public void positionPremiumsMatchKnownRoles() {
        assertEquals(1.55, NilMoney.positionPremium("QB"), 0.001);
        assertEquals(0.70, NilMoney.positionPremium("K"), 0.001);
        assertEquals(1.0, NilMoney.positionPremium(null), 0.001);
        assertEquals(1.0, NilMoney.positionPremium("XX"), 0.001);
    }

    @Test
    public void youthPremiumMakesYoungerMoreExpensive() {
        Player young = player(1, 78, 92, "WR");
        Player senior = player(4, 78, 80, "WR");
        assertTrue(NilMoney.youthPremium(young) > NilMoney.youthPremium(senior));
        assertTrue(NilMoney.marketValue(young) > NilMoney.marketValue(senior));
    }

    @Test
    public void marketValueRespectsFloorAndAwards() {
        Player base = player(3, 75, 78, "RB");
        Player heisman = player(3, 75, 78, "RB");
        heisman.wonHeisman = true;
        assertTrue(NilMoney.marketValue(base) >= 25000);
        assertTrue(NilMoney.marketValue(heisman) > NilMoney.marketValue(base));
        assertEquals(25000, NilMoney.marketValue(null));
    }

    @Test
    public void buyoutScalesWithRemainingYears() {
        Player p = player(2, 80, 88, "WR");
        p.rosterStatus = RosterStatus.SCHOLARSHIP_PLUS_NIL;
        p.nilDealAmount = 500000;
        p.contractYearsRemaining = 3;
        p.contractLength = 4;
        int longBuy = NilMoney.buyoutCost(p, 80);
        p.contractYearsRemaining = 1;
        int shortBuy = NilMoney.buyoutCost(p, 80);
        assertTrue(longBuy > shortBuy);
        assertTrue(longBuy % 1000 == 0);
    }

    @Test
    public void offerCashCostIncludesCoaAndNil() {
        int scholly = NilMoney.offerCashCost(RosterStatus.SCHOLARSHIP, 100000, 80);
        int plusNil = NilMoney.offerCashCost(RosterStatus.SCHOLARSHIP_PLUS_NIL, 100000, 80);
        int pwo = NilMoney.offerCashCost(RosterStatus.PWO, 100000, 80);
        assertTrue(scholly > 0);
        assertEquals(scholly + 100000, plusNil);
        assertEquals(0, pwo);
    }

    private static Player player(int year, int ovr, int pot, String pos) {
        Player p = new Player();
        p.year = year;
        p.ratOvr = ovr;
        p.ratPot = pot;
        p.position = pos;
        return p;
    }
}
