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
        int frBefore = fr.ratOvr;
        int srBefore = sr.ratOvr;
        // Force similar headroom
        fr.ratings.pot = Math.max(fr.ratOvr + 15, 75);
        sr.ratings.pot = Math.max(sr.ratOvr + 15, 75);
        fr.applyRatings(fr.ratings);
        sr.applyRatings(sr.ratings);
        frBefore = fr.ratOvr;
        srBefore = sr.ratOvr;

        DevelopmentCurve.advance(fr, 10, 1, new Random(21L));
        DevelopmentCurve.advance(sr, 10, 1, new Random(22L));
        int frGain = fr.ratOvr - frBefore;
        int srGain = sr.ratOvr - srBefore;
        assertTrue("Fr growth should be >= Sr plateau growth", frGain >= srGain);
    }

    @Test
    public void potStaysAtLeastOvrAndNoDeclineFromAdvance() {
        Random rng = new Random(33L);
        Player rb = PlayerFactory.fromRatings(PositionGroup.RB, "RB", null, 2,
                PlayerFactory.rollRatings(PositionGroup.RB, 2, 4, rng), false);
        int before = rb.ratOvr;
        rb.ratings.pot = before + 8;
        rb.applyRatings(rb.ratings);
        before = rb.ratOvr;
        DevelopmentCurve.advance(rb, 8, 0, new Random(34L));
        assertTrue(rb.ratOvr >= before);
        assertTrue(rb.ratings.pot >= rb.ratOvr);
    }
}
