package CFBsimPack.engine.snap;

import CFBsimPack.engine.CoverageCall;
import CFBsimPack.engine.GameState;
import CFBsimPack.engine.playdef.ProtectionType;

/**
 * Situational deltas for duels / timing / protection (not play-id branches).
 */
public final class SituationMods {
    public final double duelOffenseBoost;
    public final double hotTimeDelta;
    public final boolean forcePrimaryGap;
    public final boolean suppressCutback;
    public final boolean extraRusher;
    public final double doubleTeamChanceBoost;
    public final boolean preferMaxProtectFeel;

    public SituationMods(
            double duelOffenseBoost,
            double hotTimeDelta,
            boolean forcePrimaryGap,
            boolean suppressCutback,
            boolean extraRusher,
            double doubleTeamChanceBoost,
            boolean preferMaxProtectFeel
    ) {
        this.duelOffenseBoost = duelOffenseBoost;
        this.hotTimeDelta = hotTimeDelta;
        this.forcePrimaryGap = forcePrimaryGap;
        this.suppressCutback = suppressCutback;
        this.extraRusher = extraRusher;
        this.doubleTeamChanceBoost = doubleTeamChanceBoost;
        this.preferMaxProtectFeel = preferMaxProtectFeel;
    }

    public static SituationMods from(GameState state, CoverageCall cov) {
        if (state == null) {
            return new SituationMods(0, 0, false, false, cov == CoverageCall.COVER_0, 0, false);
        }
        boolean shortYard = state.yardsNeed <= 2;
        boolean goal = state.yardLine >= 95;
        boolean twoMin = !state.playingOT && state.gameTime >= 0 && state.gameTime <= 120;
        boolean backedUp = state.yardLine <= 10;
        boolean cover0 = cov == CoverageCall.COVER_0;

        double duelBoost = 0;
        double hotDelta = 0;
        double doubleBoost = 0;
        if (shortYard || goal) {
            duelBoost += 2;
            doubleBoost += 0.20;
        }
        if (twoMin) {
            hotDelta -= 0.20;
        }
        if (backedUp) {
            hotDelta -= 0.10;
        }
        return new SituationMods(
                duelBoost,
                hotDelta,
                goal || shortYard,
                goal || shortYard,
                cover0,
                doubleBoost,
                shortYard || goal
        );
    }

    public double adjustedHotTime(double baseHot, ProtectionType type) {
        double t = baseHot + hotTimeDelta;
        if (type == ProtectionType.EMPTY_FIVE) t -= 0.05;
        if (t < 0.75) t = 0.75;
        return t;
    }
}
