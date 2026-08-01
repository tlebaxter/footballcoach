package CFBsimPack;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertTrue;

public class PlayerRatingsTest {

    @Test
    public void bagCopyIsIndependent() {
        PlayerRatings a = new PlayerRatings();
        a.thp = 90;
        a.spd = 80;
        PlayerRatings b = a.copy();
        b.thp = 40;
        assertTrue(a.thp == 90);
        assertTrue(b.thp == 40);
    }

    @Test
    public void qbOvrPrefersThrowPowerOverKickerAttrs() {
        PlayerRatings qb = PlayerFactory.rollRatings(PositionGroup.QB, 2, 4, new Random(1L));
        qb.thp = 95;
        qb.tha = 90;
        qb.kpw = 40;
        qb.kac = 40;
        PlayerRatings k = PlayerFactory.rollRatings(PositionGroup.K, 2, 4, new Random(2L));
        k.kpw = 95;
        k.kac = 90;
        k.thp = 40;
        k.tha = 40;
        int qbAsQb = PositionOvr.ovr(qb, PositionGroup.QB);
        int qbAsK = PositionOvr.ovr(qb, PositionGroup.K);
        int kAsK = PositionOvr.ovr(k, PositionGroup.K);
        int kAsQb = PositionOvr.ovr(k, PositionGroup.QB);
        assertTrue("QB bag should rate higher at QB than K", qbAsQb > qbAsK);
        assertTrue("K bag should rate higher at K than QB", kAsK > kAsQb);
    }

    @Test
    public void fbOvrValuesRunBlockMoreThanWr() {
        PlayerRatings fb = new PlayerRatings();
        fb.rbk = 90;
        fb.pbk = 85;
        fb.stre = 85;
        fb.spd = 55;
        fb.hnd = 50;
        PlayerRatings wr = new PlayerRatings();
        wr.hnd = 90;
        wr.rtr = 88;
        wr.spd = 90;
        wr.rbk = 40;
        int fbAtFb = PositionOvr.ovr(fb, PositionGroup.FB);
        int wrAtFb = PositionOvr.ovr(wr, PositionGroup.FB);
        assertTrue(fbAtFb > wrAtFb);
    }

    @Test
    public void compositesAreBounded() {
        PlayerRatings r = PlayerFactory.rollRatings(PositionGroup.EDGE, 3, 5, new Random(9L));
        double pr = CompositeWeights.composite(r, "passRushing");
        assertTrue(pr >= 0 && pr <= 1.0);
    }
}
