package CFBsimPack.engine.snap;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertTrue;

public class DuelResolverTest {

    private static final int TRIALS = 4000;

    @Test
    public void equalRatingsRoughlySplitWins() {
        DuelResolver duel = new DuelResolver(new Random(42L));
        int wins = 0;
        int losses = 0;
        for (int i = 0; i < TRIALS; i++) {
            DuelOutcome o = duel.contest(70, 70);
            if (o.isWin()) wins++;
            else if (o.isLoss()) losses++;
        }
        double winShare = wins / (double) (wins + losses);
        assertTrue("winShare=" + winShare, winShare > 0.40 && winShare < 0.60);
    }

    @Test
    public void plus25EdgeWinsMoreButNotAlways() {
        DuelResolver duel = new DuelResolver(new Random(7L));
        int wins = 0;
        int losses = 0;
        for (int i = 0; i < TRIALS; i++) {
            DuelOutcome o = duel.contest(80, 55);
            if (o.isWin()) wins++;
            else if (o.isLoss()) losses++;
        }
        assertTrue("wins should exceed losses: wins=" + wins + " losses=" + losses, wins > losses);
        double winShare = wins / (double) Math.max(1, wins + losses);
        assertTrue("winShare=" + winShare, winShare > 0.60 && winShare < 0.95);
        // Favorites must still lose sometimes
        assertTrue("expected some losses, got " + losses, losses > 20);
    }
}
