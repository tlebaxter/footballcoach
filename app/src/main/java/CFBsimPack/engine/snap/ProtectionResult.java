package CFBsimPack.engine.snap;

import CFBsimPack.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ProtectionResult {
    public final List<PassRushMatchup> matchups;
    public final double earliestPressureSec;
    public final boolean hotForced;
    public final Player freeRusher;
    public final boolean usedFallback;

    public ProtectionResult(
            List<PassRushMatchup> matchups,
            double earliestPressureSec,
            boolean hotForced,
            Player freeRusher,
            boolean usedFallback
    ) {
        this.matchups = matchups != null
                ? Collections.unmodifiableList(new ArrayList<>(matchups))
                : Collections.emptyList();
        this.earliestPressureSec = earliestPressureSec;
        this.hotForced = hotForced;
        this.freeRusher = freeRusher;
        this.usedFallback = usedFallback;
    }
}
