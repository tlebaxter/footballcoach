package CFBsimPack.engine.snap;

import CFBsimPack.Player;

public final class PassRushMatchup {
    public final Player blocker;
    public final OffSlot blockerSlot;
    public final Player rusher;
    public final DefSlot rusherSlot;
    public final DuelOutcome duel;
    /** Seconds until pressure from this gap; null if sealed. */
    public final Double pressureAtSec;
    public final boolean doubled;
    public final boolean unblocked;

    public PassRushMatchup(
            Player blocker,
            OffSlot blockerSlot,
            Player rusher,
            DefSlot rusherSlot,
            DuelOutcome duel,
            Double pressureAtSec,
            boolean doubled,
            boolean unblocked
    ) {
        this.blocker = blocker;
        this.blockerSlot = blockerSlot;
        this.rusher = rusher;
        this.rusherSlot = rusherSlot;
        this.duel = duel;
        this.pressureAtSec = pressureAtSec;
        this.doubled = doubled;
        this.unblocked = unblocked;
    }
}
