package CFBsimPack;

import CFBsimPack.engine.GamePhase;
import CFBsimPack.engine.OffensePlay;
import CFBsimPack.engine.PenaltyCatalog;
import CFBsimPack.engine.PenaltyResolver;
import CFBsimPack.engine.PendingPlay;
import CFBsimPack.engine.PlayResult;
import CFBsimPack.engine.PlayState;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PenaltyResolverTest {

    @Test
    public void holdingAcceptedWhenPlayGainedYards() {
        PlayState before = baseState();
        PlayState after = before.copy();
        after.yardLine = before.yardLine + 12;
        after.yardsNeed = before.yardsNeed - 12;
        PlayResult result = new PlayResult();
        result.yardsGained = 12;
        result.logLine = "rush 12";
        PendingPlay pending = new PendingPlay(result, before, after);
        pending.foul = PenaltyCatalog.Foul.HOLDING;
        int playSpot = after.yardLine;
        PenaltyResolver.resolve(pending);
        assertTrue(pending.foulAccepted);
        assertTrue(pending.after.yardLine < playSpot);
        assertTrue(pending.after.yardLine <= before.yardLine);
        assertTrue(pending.result.logLine.contains("accepted"));
    }

    @Test
    public void dpiDeclinedWhenTdAlreadyScored() {
        PlayState before = baseState();
        PlayState after = before.copy();
        after.yardLine = 100;
        PlayResult result = new PlayResult();
        result.touchdown = true;
        result.logLine = "TD";
        PendingPlay pending = new PendingPlay(result, before, after);
        pending.foul = PenaltyCatalog.Foul.DPI;
        PenaltyResolver.resolve(pending);
        assertFalse(pending.foulAccepted);
        assertTrue(pending.result.logLine.contains("declined"));
    }

    @Test
    public void dpiSpotFoulEnforcesFromCatchSpot() {
        PlayState before = baseState();
        before.yardLine = 40;
        before.yardsNeed = 10;
        PlayState after = before.copy();
        after.down = 2;
        PlayResult result = new PlayResult();
        result.incomplete = true;
        result.logLine = "incomplete";
        PendingPlay pending = new PendingPlay(result, before, after);
        pending.foul = PenaltyCatalog.Foul.DPI;
        pending.foulSpotYardLine = 55;
        PenaltyResolver.resolve(pending);
        assertTrue(pending.foulAccepted);
        assertEquals(70, pending.after.yardLine);
        assertEquals(1, pending.after.down);
        assertTrue(pending.result.logLine.contains("at the 55"));
        assertTrue(pending.result.logLine.contains("automatic first down"));
    }

    @Test
    public void dpiHalfDistanceNearGoal() {
        PlayState before = baseState();
        before.yardLine = 90;
        before.yardsNeed = 10;
        PlayState after = before.copy();
        PlayResult result = new PlayResult();
        result.incomplete = true;
        result.logLine = "incomplete";
        PendingPlay pending = new PendingPlay(result, before, after);
        pending.foul = PenaltyCatalog.Foul.DPI;
        pending.foulSpotYardLine = 95;
        PenaltyResolver.resolve(pending);
        assertTrue(pending.foulAccepted);
        // toGoal from 95 is 5; half distance = 2 → ball at 97
        assertEquals(97, pending.after.yardLine);
        assertEquals(1, pending.after.down);
        assertTrue(pending.result.logLine.contains("half the distance"));
    }

    @Test
    public void targetingAcceptedEjectsInLog() {
        PlayState before = baseState();
        PlayState after = before.copy();
        after.yardLine = before.yardLine + 3;
        PlayResult result = new PlayResult();
        result.yardsGained = 3;
        result.logLine = "rush";
        PendingPlay pending = new PendingPlay(result, before, after);
        pending.foul = PenaltyCatalog.Foul.TARGETING;
        Player ejected = new Player();
        ejected.name = "Bad Hit";
        pending.ejectedPlayer = ejected;
        PenaltyResolver.resolve(pending);
        assertTrue(pending.foulAccepted);
        assertEquals(1, pending.after.down);
        assertTrue(pending.result.logLine.contains("TARGETING"));
        assertTrue(pending.result.logLine.contains("Bad Hit ejected"));
    }

    @Test
    public void catalogRollUsesInjectedRng() {
        Random a = new Random(5L);
        Random b = new Random(5L);
        for (int i = 0; i < 50; i++) {
            assertEquals(
                    PenaltyCatalog.roll(a, OffensePlay.PASS),
                    PenaltyCatalog.roll(b, OffensePlay.PASS));
        }
        assertNotNull(PenaltyCatalog.Foul.HOLDING);
        assertTrue(PenaltyCatalog.Foul.DPI.enforcement == PenaltyCatalog.Enforcement.SPOT);
        assertTrue(PenaltyCatalog.Foul.TARGETING.ejects);
        assertTrue(PenaltyCatalog.Foul.HOLDING.triggersTenSecondRunoff);
    }

    @Test
    public void halfDistanceHelpers() {
        assertEquals(15, PenaltyResolver.yardsTowardOpponentGoal(40, 15, true));
        assertEquals(5, PenaltyResolver.yardsTowardOpponentGoal(90, 15, true));
        assertEquals(5, PenaltyResolver.yardsTowardOwnGoal(10, 15, true));
    }

    private static PlayState baseState() {
        PlayState s = new PlayState();
        s.down = 1;
        s.yardsNeed = 10;
        s.yardLine = 40;
        s.gameTime = 600;
        s.possessionHome = true;
        s.homeScore = 7;
        s.awayScore = 3;
        s.phase = GamePhase.REGULATION;
        return s;
    }
}
