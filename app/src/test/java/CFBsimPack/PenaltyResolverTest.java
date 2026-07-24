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
    public void catalogRollUsesInjectedRng() {
        Random a = new Random(5L);
        Random b = new Random(5L);
        for (int i = 0; i < 50; i++) {
            assertEquals(
                    PenaltyCatalog.roll(a, OffensePlay.PASS),
                    PenaltyCatalog.roll(b, OffensePlay.PASS));
        }
        assertNotNull(PenaltyCatalog.Foul.HOLDING);
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
