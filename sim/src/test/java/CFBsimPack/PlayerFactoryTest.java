package CFBsimPack;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertTrue;

public class PlayerFactoryTest {

    @Test
    public void nonQbThrowSkillsStayInSpecialistBand() {
        int edgeMax = 0;
        int edgeSum = 0;
        int n = 200;
        for (int i = 0; i < n; i++) {
            PlayerRatings edge = PlayerFactory.rollRatings(PositionGroup.EDGE, 5, 5, new Random(1000L + i));
            edgeMax = Math.max(edgeMax, edge.tha);
            edgeSum += edge.tha;
        }
        assertTrue("EDGE tha should stay in specialist band, max=" + edgeMax, edgeMax <= 45);
        assertTrue("EDGE mean tha should be low, mean=" + (edgeSum / (double) n),
                edgeSum / (double) n < 40);
    }

    @Test
    public void qbThrowSkillsExceedNonSpecialists() {
        double qbSum = 0;
        double wrSum = 0;
        int n = 150;
        for (int i = 0; i < n; i++) {
            qbSum += PlayerFactory.rollRatings(PositionGroup.QB, 3, 4, new Random(2000L + i)).tha;
            wrSum += PlayerFactory.rollRatings(PositionGroup.WR, 3, 4, new Random(3000L + i)).tha;
        }
        assertTrue("QB mean tha should beat WR mean", qbSum / n > wrSum / n + 15);
    }

    @Test
    public void formerHsQbAthleteCanAppearOnSkillPositions() {
        boolean foundAthlete = false;
        int athletes = 0;
        for (int i = 0; i < 800; i++) {
            PlayerRatings wr = PlayerFactory.rollRatings(PositionGroup.WR, 3, 4, new Random(5000L + i));
            if (wr.tha >= 50) {
                foundAthlete = true;
                athletes++;
            }
        }
        assertTrue("expected at least one former-HS-QB WR with tha>=50", foundAthlete);
        // ~4% rate; allow wide band for RNG
        assertTrue("athlete rate should be rare, got " + athletes, athletes < 120);
        assertTrue("athlete rate should not be near-zero, got " + athletes, athletes >= 5);
    }

    @Test
    public void edgeNeverGetsFormerHsQbBoost() {
        for (int i = 0; i < 400; i++) {
            PlayerRatings edge = PlayerFactory.rollRatings(PositionGroup.EDGE, 4, 5, new Random(9000L + i));
            assertTrue("EDGE tha should never look like an athlete QB, tha=" + edge.tha,
                    edge.tha <= 45);
        }
    }
}
