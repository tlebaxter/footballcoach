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
    public void yearlyBudgetClampsCapitalScoreAndScalesUp() {
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
        assertEquals(1.50, NilMoney.positionPremium("QB"), 0.001);
        assertEquals(0.50, NilMoney.positionPremium("K"), 0.001);
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
    public void purseSeparatesRevenueShareAndCollectiveUpside() {
        ProgramProfile floor = new ProgramProfile(40, 40, 40, 50, 45, 45, 47);
        ProgramProfile elite = new ProgramProfile(95, 95, 95, 90, 95, 95, 95);

        assertTrue(NilMoney.yearlyRevShare(elite) > NilMoney.yearlyRevShare(floor));
        assertTrue(NilMoney.yearlyCollective(elite) > NilMoney.yearlyCollective(floor));
        assertTrue(NilMoney.yearlyBudget(floor) >= 3_000_000);
        assertTrue(NilMoney.yearlyBudget(elite) >= 30_000_000);
        assertTrue(NilMoney.yearlyBudget(elite) <= 47_000_000);
    }

    @Test
    public void portalStarHasPremiumOverEquivalentRosterPlayer() {
        Player rosterPlayer = player(3, 84, 88, "EDGE");
        Player portalPlayer = player(3, 84, 88, "EDGE");
        portalPlayer.transferReason = TransferReason.MOVE_UP;

        assertTrue(NilMoney.marketValue(portalPlayer) > NilMoney.marketValue(rosterPlayer));
    }

    @Test
    public void eliteQuarterbackMarketIsTopHeavy() {
        Player starter = player(3, 80, 84, "QB");
        Player star = player(3, 95, 97, "QB");

        assertTrue(NilMoney.marketValue(star) > NilMoney.marketValue(starter) * 2);
        assertTrue(NilMoney.marketValue(star) <= 7_000_000);
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
    public void scholarshipOnlyBuyoutIsFree() {
        Player p = player(2, 80, 88, "WR");
        p.rosterStatus = RosterStatus.SCHOLARSHIP;
        p.nilDealAmount = 0;
        p.contractYearsRemaining = 3;
        p.contractLength = 4;
        assertEquals(0, NilMoney.buyoutCost(p, 80));
        assertEquals(0, NilMoney.buyoutCost(p, (ProgramProfile) null));
    }

    @Test
    public void offerCashCostIsNilOnly() {
        int scholly = NilMoney.offerCashCost(RosterStatus.SCHOLARSHIP, 100000, 80);
        int plusNil = NilMoney.offerCashCost(RosterStatus.SCHOLARSHIP_PLUS_NIL, 100000, 80);
        int pwo = NilMoney.offerCashCost(RosterStatus.PWO, 100000, 80);
        assertEquals(0, scholly);
        assertEquals(100000, plusNil);
        assertEquals(0, pwo);
    }

    @Test
    public void appearanceFeeGrowsExponentiallyWithGap() {
        ProgramProfile soft = new ProgramProfile(40, 40, 40, 40, 40, 40, 40);
        ProgramProfile mid = new ProgramProfile(55, 55, 55, 55, 55, 55, 55);
        ProgramProfile power = new ProgramProfile(90, 90, 90, 90, 90, 90, 90);

        int smallGap = NilMoney.appearanceFee(soft, mid);
        int largeGap = NilMoney.appearanceFee(soft, power);
        assertTrue(smallGap > 0);
        assertTrue(largeGap > smallGap * 2);
        assertEquals(0, largeGap % 1000);
        assertEquals(0, NilMoney.appearanceFee(power, soft));
        assertEquals(0, NilMoney.appearanceFee(soft, soft));
    }

    @Test
    public void singleGameGuaranteePicksBuyAppearanceOrVisitorFloor() {
        ProgramProfile soft = new ProgramProfile(40, 40, 40, 40, 40, 40, 40);
        ProgramProfile power = new ProgramProfile(90, 90, 90, 90, 90, 90, 90);

        assertEquals(
                NilMoney.buyGameGuarantee(power, soft),
                NilMoney.singleGameGuarantee(power, soft));
        assertEquals(
                NilMoney.appearanceFee(soft, power),
                NilMoney.singleGameGuarantee(soft, power));
        assertEquals(
                NilMoney.visitorFloorGuarantee(soft),
                NilMoney.singleGameGuarantee(soft, soft));
        assertTrue("Peers never travel for free", NilMoney.singleGameGuarantee(soft, soft) > 0);
    }

    @Test
    public void homeAndHomeLegFeeOnlyChargesTheWeakerHost() {
        ProgramProfile soft = new ProgramProfile(40, 40, 40, 40, 40, 40, 40);
        ProgramProfile power = new ProgramProfile(90, 90, 90, 90, 90, 90, 90);

        assertEquals(0, NilMoney.homeAndHomeLegFee(power, soft));
        assertEquals(0, NilMoney.homeAndHomeLegFee(soft, soft));
        assertEquals(
                NilMoney.appearanceFee(soft, power),
                NilMoney.homeAndHomeLegFee(soft, power));
    }

    @Test
    public void cancelBuyoutTracksRemainingMoneyAndDealShape() {
        int peerSeries = NilMoney.oocCancelBuyout(OocContract.Type.HOME_AND_HOME, 0, 2, 2);
        int paidSeries = NilMoney.oocCancelBuyout(
                OocContract.Type.HOME_AND_HOME, 4_000_000, 2, 2);
        int twoForOne = NilMoney.oocCancelBuyout(OocContract.Type.TWO_FOR_ONE, 0, 3, 3);

        assertEquals(400_000, peerSeries);
        assertEquals(2_200_000, paidSeries);
        assertEquals(450_000, twoForOne);
        assertTrue(paidSeries > peerSeries);
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
