package CFBsimPack;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class PlayerMarketTest {

    @Test
    public void productivePlayerOutranksUnusedHigherOvr() {
        Player productive = new Player();
        productive.ratOvr = 82;
        productive.ratPot = 84;
        productive.gamesPlayed = 12;
        productive.seasonSnaps = 700;
        productive.wonAllConference = true;

        Player unused = new Player();
        unused.ratOvr = 86;
        unused.ratPot = 88;
        unused.gamesPlayed = 0;
        unused.seasonSnaps = 0;
        unused.year = 3;

        assertTrue(PlayerMarket.marketTalent(productive) > PlayerMarket.marketTalent(unused));
    }

    @Test
    public void roleMultiplierSteep() {
        assertTrue(PlayerMarket.roleMultiplier(1) > PlayerMarket.roleMultiplier(2) * 2);
        assertTrue(PlayerMarket.roleMultiplier(2) > PlayerMarket.roleMultiplier(3));
    }
}
