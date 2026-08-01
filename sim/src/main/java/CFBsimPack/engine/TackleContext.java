package CFBsimPack.engine;

import CFBsimPack.Player;

/**
 * Preferred tacklers already identified for a snap (gap defenders or coverage).
 */
public final class TackleContext {
    public final Player primary;
    public final Player secondary;
    public final boolean scramble;

    public TackleContext(Player primary, Player secondary, boolean scramble) {
        this.primary = primary;
        this.secondary = secondary;
        this.scramble = scramble;
    }

    public static TackleContext run(Player firstLevel, Player secondLevel) {
        return new TackleContext(firstLevel, secondLevel, false);
    }

    public static TackleContext pass(Player coverage, Player help) {
        return new TackleContext(coverage, help, false);
    }

    public static TackleContext scramble(Player edgeBias, Player lbBias) {
        return new TackleContext(edgeBias, lbBias, true);
    }
}
