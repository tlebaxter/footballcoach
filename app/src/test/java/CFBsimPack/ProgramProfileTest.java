package CFBsimPack;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProgramProfileTest {

    @Test
    public void derivedScoresUseDifferentPartsOfProgramIdentity() {
        ProgramProfile profile = new ProgramProfile(90, 80, 70, 60, 50, 40, 95);

        assertTrue(profile.brandAttract > profile.momentum);
        assertTrue(profile.revSharePool > profile.collectivePool);
        assertTrue(profile.talentGravity >= 25 && profile.talentGravity <= 99);
        assertTrue(profile.programPower >= 25 && profile.programPower <= 99);
    }

    @Test
    public void annualMovementIsClampedByFactorSpeed() {
        ProgramProfile profile = new ProgramProfile(60, 60, 60, 60, 60, 60, 50);

        profile.updateForSeason(1, 140, true, 3, 40, 50);

        assertTrue(profile.diffMomentum <= 10);
        assertTrue(profile.diffDonors <= 6);
        assertTrue(profile.diffFanbase <= 3);
        assertTrue(profile.diffTradition <= 1);
        assertTrue(profile.diffPipeline <= 5);
    }

    @Test
    public void traditionRequiresSustainedEliteFinishes() {
        ProgramProfile profile = new ProgramProfile(60, 70, 70, 70, 70, 80, 55);

        profile.updateForSeason(10, 140, false, 0, 20, 55);
        profile.updateForSeason(12, 140, false, 0, 20, 55);
        assertEquals(60, profile.tradition);

        profile.updateForSeason(8, 140, false, 0, 20, 55);
        assertEquals(61, profile.tradition);
    }

    @Test
    public void draftProductionBuildsPipeline() {
        ProgramProfile profile = new ProgramProfile(60, 60, 60, 60, 50, 60, 55);

        profile.updateForSeason(40, 140, false, 0, 40, 55);

        assertTrue(profile.pipeline > 50);
        assertTrue(profile.diffPipeline > 0);
    }

    @Test
    public void sustainedG5DynastyCanReachNationalMarketTier() {
        ProgramProfile profile = new ProgramProfile(55, 58, 55, 60, 58, 60, 55);

        for (int year = 0; year < 14; year++) {
            profile.updateForSeason(1, 140, true, 2, 40, 55);
        }

        assertTrue(profile.brandAttract >= 75);
        assertTrue(profile.collectivePool >= 90);
        assertTrue(profile.capitalPool >= 75);
        assertTrue(profile.programPower >= 75);
    }
}
