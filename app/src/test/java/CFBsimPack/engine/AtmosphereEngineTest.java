package CFBsimPack.engine;

import CFBsimPack.ProgramProfile;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AtmosphereEngineTest {

    @Test
    public void midMajorHomeBaselineIsQuietToSteady() {
        GameState state = new GameState();
        AtmosphereEngine.seed(state, midMajor(), midMajor(), 0, false, false);

        assertTrue("baseline=" + state.crowdBaseline,
                state.crowdBaseline >= 40 && state.crowdBaseline <= 55);
        assertEquals(state.crowdBaseline, state.crowdEnergy);
    }

    @Test
    public void blueBloodPeerBaselineIsLoudToElectric() {
        GameState state = new GameState();
        AtmosphereEngine.seed(state, blueBlood(), blueBloodPeer(), 0, false, false);

        assertTrue("baseline=" + state.crowdBaseline,
                state.crowdBaseline >= 70 && state.crowdBaseline <= 82);
    }

    @Test
    public void blueBloodBuyGameRaisesBaselineAbovePeer() {
        GameState peer = new GameState();
        AtmosphereEngine.seed(peer, blueBlood(), blueBloodPeer(), 0, false, false);

        GameState buy = new GameState();
        AtmosphereEngine.seed(buy, blueBlood(), softVisitor(), 0, true, false);

        assertTrue(buy.crowdBaseline > peer.crowdBaseline);
        assertTrue("buy baseline=" + buy.crowdBaseline,
                buy.crowdBaseline >= 78 && buy.crowdBaseline <= 92);
    }

    @Test
    public void hotRivalryPushesBlueBloodTowardHostile() {
        GameState state = new GameState();
        AtmosphereEngine.seed(state, blueBlood(), blueBloodPeer(), 90, false, false);

        assertTrue("baseline=" + state.crowdBaseline,
                state.crowdBaseline >= 85 && state.crowdBaseline <= 100);
        assertEquals(90, state.crowdRivalry);
    }

    @Test
    public void postseasonDampsBaseline() {
        GameState home = new GameState();
        AtmosphereEngine.seed(home, blueBlood(), blueBloodPeer(), 0, false, false);

        GameState bowl = new GameState();
        AtmosphereEngine.seed(bowl, blueBlood(), blueBloodPeer(), 0, false, true);

        assertTrue(bowl.crowdBaseline < home.crowdBaseline);
        assertEquals(Math.round(home.crowdBaseline * 0.55), bowl.crowdBaseline);
    }

    @Test
    public void isPostseasonDetectsBowlAndPlayoffNames() {
        assertTrue(AtmosphereEngine.isPostseason("Rose Bowl"));
        assertTrue(AtmosphereEngine.isPostseason("CFP Semi"));
        assertTrue(AtmosphereEngine.isPostseason("NCG"));
        assertTrue(AtmosphereEngine.isPostseason("Playoff Round 1"));
        assertTrue(!AtmosphereEngine.isPostseason("Rivalry Game"));
        assertTrue(!AtmosphereEngine.isPostseason("OOC"));
    }

    @Test
    public void offenseBonusIsAsymmetricAtHighEnergy() {
        GameState state = new GameState();
        state.crowdEnergy = 90;
        state.possessionHome = true;
        int homeBonus = AtmosphereEngine.offenseBonus(state);
        assertTrue("homeBonus=" + homeBonus, homeBonus >= 4 && homeBonus <= 6);

        state.possessionHome = false;
        int awayBonus = AtmosphereEngine.offenseBonus(state);
        assertTrue("awayBonus=" + awayBonus, awayBonus <= -2 && awayBonus >= -3);
    }

    @Test
    public void quietCrowdNearZeroOffenseBonus() {
        GameState state = new GameState();
        state.crowdEnergy = 20;
        state.possessionHome = true;
        assertTrue(AtmosphereEngine.offenseBonus(state) <= 2);

        state.possessionHome = false;
        assertTrue(AtmosphereEngine.offenseBonus(state) >= -1);
    }

    @Test
    public void roadNoiseAndPressureScaleWithEnergyForAwayOnly() {
        GameState state = new GameState();
        state.crowdEnergy = 100;
        state.possessionHome = true;
        assertEquals(1.0, AtmosphereEngine.roadNoiseMult(state), 0.001);
        assertEquals(0, AtmosphereEngine.roadPressureAdd(state));

        state.possessionHome = false;
        assertEquals(1.9, AtmosphereEngine.roadNoiseMult(state), 0.001);
        assertEquals(8, AtmosphereEngine.roadPressureAdd(state));
    }

    @Test
    public void afterSnapHomeTdRaisesEnergyThenMeanRevertsTowardBaseline() {
        GameState state = new GameState();
        state.crowdBaseline = 70;
        state.crowdEnergy = 70;
        state.crowdRivalry = 0;
        state.homeScore = 7;
        state.awayScore = 0;
        state.gameTime = 3000;

        PlayResult td = new PlayResult();
        td.touchdown = true;
        AtmosphereEngine.afterSnap(state, td, true, 1, 10);

        assertTrue("energy after TD=" + state.crowdEnergy, state.crowdEnergy > 70);
        assertTrue(state.crowdEnergy <= 100);
    }

    @Test
    public void afterSnapBlowoutPullsEnergyDown() {
        GameState state = new GameState();
        state.crowdBaseline = 80;
        state.crowdEnergy = 95;
        state.crowdRivalry = 0;
        state.homeScore = 35;
        state.awayScore = 7;
        state.gameTime = 900;

        PlayResult uneventful = new PlayResult();
        uneventful.yardsGained = 3;
        AtmosphereEngine.afterSnap(state, uneventful, true, 1, 10);

        assertTrue("blowout energy=" + state.crowdEnergy, state.crowdEnergy < 95);
    }

    @Test
    public void bandLabelsMatchEnergyThresholds() {
        assertEquals("Quiet", AtmosphereEngine.band(20));
        assertEquals("Steady", AtmosphereEngine.band(50));
        assertEquals("Loud", AtmosphereEngine.band(65));
        assertEquals("Electric", AtmosphereEngine.band(80));
        assertEquals("Hostile", AtmosphereEngine.band(90));
    }

    @Test
    public void falseStartRateHigherUnderHostileRoadNoise() {
        double quiet = PenaltyCatalog.rateFor(
                PenaltyCatalog.Foul.FALSE_START, OffensePlay.PASS, 1.0);
        double hostile = PenaltyCatalog.rateFor(
                PenaltyCatalog.Foul.FALSE_START, OffensePlay.PASS, 1.9);
        assertTrue(hostile > quiet);
        assertEquals(quiet * 1.9, hostile, 0.0001);
    }

    private static ProgramProfile blueBlood() {
        return new ProgramProfile(96, 96, 92, 90, 90, 85, 95);
    }

    private static ProgramProfile blueBloodPeer() {
        return new ProgramProfile(90, 88, 88, 85, 85, 80, 90);
    }

    private static ProgramProfile midMajor() {
        return new ProgramProfile(48, 48, 45, 45, 45, 50, 40);
    }

    private static ProgramProfile softVisitor() {
        return new ProgramProfile(40, 42, 38, 40, 40, 45, 35);
    }
}
