package CFBsimPack;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertTrue;

public class DevelopmentCurveTest {

    @Test
    public void freshmanGrowsFasterThanSeniorPlateau() {
        Random rng = new Random(11L);
        Player fr = PlayerFactory.fromRatings(PositionGroup.QB, "Fr QB", null, 1,
                PlayerFactory.rollRatings(PositionGroup.QB, 1, 3, rng), false);
        Player sr = PlayerFactory.fromRatings(PositionGroup.QB, "Sr QB", null, 3,
                PlayerFactory.rollRatings(PositionGroup.QB, 3, 3, new Random(12L)), false);
        fr.ratings.pot = Math.max(fr.ratOvr + 15, 75);
        sr.ratings.pot = Math.max(sr.ratOvr + 15, 75);
        fr.applyRatings(fr.ratings);
        sr.applyRatings(sr.ratings);
        fr.seasonSnaps = 650;
        sr.seasonSnaps = 650;
        int frBefore = fr.ratOvr;
        int srBefore = sr.ratOvr;

        DevelopmentCurve.advance(fr, 1, new Random(21L));
        DevelopmentCurve.advance(sr, 1, new Random(22L));
        int frGain = fr.ratOvr - frBefore;
        int srGain = sr.ratOvr - srBefore;
        assertTrue("Fr growth should be >= Sr plateau growth", frGain >= srGain);
    }

    @Test
    public void potStaysAtLeastOvrAndNoDeclineFromAdvance() {
        Random rng = new Random(33L);
        Player rb = PlayerFactory.fromRatings(PositionGroup.RB, "RB", null, 2,
                PlayerFactory.rollRatings(PositionGroup.RB, 2, 4, rng), false);
        rb.ratings.pot = rb.ratOvr + 8;
        rb.applyRatings(rb.ratings);
        rb.seasonSnaps = 300;
        int before = rb.ratOvr;
        DevelopmentCurve.advance(rb, 0, new Random(34L));
        assertTrue(rb.ratOvr >= before);
        assertTrue(rb.ratings.pot >= rb.ratOvr);
    }

    @Test
    public void highSnapPeerGrowsMoreThanLowSnapPeer() {
        Player high = cloneLikeQb("High Snap", 41L);
        Player low = cloneLikeQb("Low Snap", 41L);
        high.seasonSnaps = 700;
        low.seasonSnaps = 40;
        int highBefore = high.ratOvr;
        int lowBefore = low.ratOvr;
        DevelopmentCurve.advance(high, 0, new Random(100L));
        DevelopmentCurve.advance(low, 0, new Random(100L));
        assertTrue("High snaps should grow more",
                high.ratOvr - highBefore > low.ratOvr - lowBefore);
    }

    @Test
    public void nearZeroSnapsHasLowUsageVersusHeavyStarter() {
        Player bench = cloneLikeQb("Bench", 55L);
        Player starter = cloneLikeQb("Starter", 55L);
        bench.seasonSnaps = 0;
        starter.seasonSnaps = 800;
        starter.seasonStats.passAtt = 350;
        assertTrue(DevelopmentCurve.usageFactor(bench, PositionGroup.QB) < 0.05);
        assertTrue(DevelopmentCurve.usageFactor(starter, PositionGroup.QB) > 0.9);

        int benchBefore = bench.ratOvr;
        int starterBefore = starter.ratOvr;
        DevelopmentCurve.advance(bench, 0, new Random(200L));
        DevelopmentCurve.advance(starter, 0, new Random(200L));
        assertTrue(bench.ratOvr >= benchBefore);
        assertTrue("Heavy usage should outgrow near-zero snaps",
                starter.ratOvr - starterBefore >= bench.ratOvr - benchBefore);
    }

    @Test
    public void rushHeavyQbBiasesTowardEluAndSpd() {
        Player rusher = cloneLikeQb("Rusher", 77L);
        Player passer = cloneLikeQb("Passer", 77L);
        rusher.seasonSnaps = 600;
        passer.seasonSnaps = 600;
        rusher.seasonStats.passAtt = 80;
        rusher.seasonStats.rushAtt = 180;
        passer.seasonStats.passAtt = 400;
        passer.seasonStats.rushAtt = 20;

        int rusherElu = rusher.ratings.elu;
        int rusherSpd = rusher.ratings.spd;
        int rusherTha = rusher.ratings.tha;
        int passerElu = passer.ratings.elu;
        int passerSpd = passer.ratings.spd;
        int passerTha = passer.ratings.tha;

        // Same RNG stream shape for both; bias should still favor rush keys on rusher
        DevelopmentCurve.advance(rusher, 0, new Random(300L));
        DevelopmentCurve.advance(passer, 0, new Random(300L));

        int rusherMobility = (rusher.ratings.elu - rusherElu) + (rusher.ratings.spd - rusherSpd);
        int passerMobility = (passer.ratings.elu - passerElu) + (passer.ratings.spd - passerSpd);
        int rusherThrow = rusher.ratings.tha - rusherTha;
        int passerThrow = passer.ratings.tha - passerTha;
        assertTrue("Rush-heavy QB should gain more mobility than pass-heavy peer",
                rusherMobility >= passerMobility);
        assertTrue("Pass-heavy QB should gain at least as much throw accuracy",
                passerThrow >= rusherThrow);
    }

    private static Player cloneLikeQb(String name, long seed) {
        Player p = PlayerFactory.fromRatings(PositionGroup.QB, name, null, 2,
                PlayerFactory.rollRatings(PositionGroup.QB, 2, 3, new Random(seed)), false);
        p.ratings.pot = Math.max(p.ratOvr + 18, 80);
        p.applyRatings(p.ratings);
        return p;
    }
}
