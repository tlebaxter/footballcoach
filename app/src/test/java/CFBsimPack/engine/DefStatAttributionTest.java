package CFBsimPack.engine;

import CFBsimPack.Player;
import CFBsimPack.PlayerFactory;
import CFBsimPack.PlayerRatings;
import CFBsimPack.PositionGroup;
import CFBsimPack.engine.snap.DuelOutcome;
import CFBsimPack.engine.snap.PassRushMatchup;
import CFBsimPack.engine.snap.ProtectionResult;
import CFBsimPack.engine.snap.DefSlot;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DefStatAttributionTest {

    @Test
    public void creditSackUpdatesSeasonAndGame() {
        Player edge = defender("Edge One", PositionGroup.EDGE);
        PlayerGameStats game = new PlayerGameStats();
        DefStatAttribution attrs = new DefStatAttribution(game);

        attrs.creditSack(edge);

        assertEquals(1, edge.seasonStats.sacksDef);
        assertEquals(1, edge.seasonStats.tackles);
        assertEquals(1, edge.seasonStats.tfl);
        assertEquals(1, game.line(edge).sacksDef);
        assertEquals(1, game.line(edge).tackles);
        assertEquals(1, game.line(edge).tfl);
    }

    @Test
    public void creditDefIntAndPassDef() {
        Player cb = defender("Corner", PositionGroup.CB);
        DefStatAttribution attrs = new DefStatAttribution(new PlayerGameStats());
        attrs.creditDefInt(cb);
        attrs.creditPassDef(cb);
        assertEquals(1, cb.seasonStats.defInt);
        assertEquals(1, cb.seasonStats.passDef);
    }

    @Test
    public void pickSackRusherPrefersPressedRusher() {
        Player sealed = defender("Sealed", PositionGroup.EDGE);
        Player presser = defender("Presser", PositionGroup.EDGE);
        PassRushMatchup sealedM = new PassRushMatchup(
                null, null, sealed, DefSlot.EDGE_L,
                new DuelOutcome(DuelOutcome.Result.WIN, 1.0), null, false, false);
        PassRushMatchup pressM = new PassRushMatchup(
                null, null, presser, DefSlot.EDGE_R,
                new DuelOutcome(DuelOutcome.Result.LOSS, -1.0), 1.8, false, true);
        ProtectionResult protection = new ProtectionResult(
                Arrays.asList(sealedM, pressM), 1.8, false, presser, false);

        Player picked = DefStatAttribution.pickSackRusher(protection, new Random(1L));
        assertEquals(presser, picked);
    }

    @Test
    public void pickSackRusherNullSafe() {
        assertNull(DefStatAttribution.pickSackRusher(null, new Random(1L)));
        ProtectionResult empty = new ProtectionResult(Collections.emptyList(), 99, false, null, true);
        assertNull(DefStatAttribution.pickSackRusher(empty, new Random(1L)));
    }

    @Test
    public void pickTackleUsesPrimaryContext() {
        Player dl = defender("DL", PositionGroup.DL);
        Player lb = defender("LB", PositionGroup.LB);
        TackleContext ctx = TackleContext.run(dl, lb);
        Player picked = DefStatAttribution.pickTackle(null, ctx, -2, false, new Random(3L));
        assertNotNull(picked);
        assertTrue(picked == dl || picked == lb);
    }

    @Test
    public void contestedPassDefMoreLikelyWhenTight() {
        Random rng = new Random(42L);
        int tightHits = 0;
        int looseHits = 0;
        for (int i = 0; i < 400; i++) {
            if (DefStatAttribution.rollContestedPassDef(2.0, rng)) tightHits++;
            if (DefStatAttribution.rollContestedPassDef(14.0, rng)) looseHits++;
        }
        assertTrue(tightHits > looseHits);
        assertTrue(tightHits > 40);
        assertEquals(0, looseHits);
    }

    private static Player defender(String name, PositionGroup group) {
        PlayerRatings bag = PlayerFactory.rollRatings(group, 3, 4, new Random(name.hashCode()));
        return PlayerFactory.fromRatings(group, name, null, 3, bag, false);
    }
}
