package CFBsimPack.engine.snap;

import CFBsimPack.Player;
import CFBsimPack.engine.CoverageCall;
import CFBsimPack.engine.playdef.ExplosiveProfile;

import java.util.List;

/**
 * Deep safety brake on explosives, help-INT prior, and scramble contain.
 */
public final class SafetyHelp {

    public enum Shell {
        SINGLE_HIGH,
        TWO_HIGH,
        BOX_SS,
        SPY_NO_DEEP,
        SOFT
    }

    private SafetyHelp() {}

    public static Shell shell(CoverageCall cov) {
        if (cov == null) return Shell.SOFT;
        if (cov == CoverageCall.SPY) return Shell.SPY_NO_DEEP;
        if (cov == CoverageCall.STACK_BOX) return Shell.BOX_SS;
        if (cov == CoverageCall.COVER_2 || cov == CoverageCall.COVER_4) return Shell.TWO_HIGH;
        if (cov == CoverageCall.COVER_1 || cov == CoverageCall.COVER_3 || cov == CoverageCall.COVER_0) {
            return Shell.SINGLE_HIGH;
        }
        if (cov == CoverageCall.OFF_COVERAGE) return Shell.SOFT;
        return Shell.SOFT;
    }

    /** Multiplier on explosive yards / burst chance (&lt;1 brakes). */
    public static double explosiveBrake(Shell shell, ExplosiveProfile profile, boolean insideRun) {
        if (profile == null) return 1.0;
        double m = 1.0;
        switch (shell) {
            case TWO_HIGH:
                m = profile.needsSoftSafety ? 0.55 : 0.75;
                break;
            case SINGLE_HIGH:
                m = insideRun ? 0.70 : 0.90;
                break;
            case BOX_SS:
                m = insideRun ? 0.45 : 0.85;
                break;
            case SPY_NO_DEEP:
                m = 1.05;
                break;
            case SOFT:
                m = profile.needsSoftSafety ? 1.15 : 1.0;
                break;
            default:
                break;
        }
        return m * profile.breakawayMult;
    }

    public static double helpIntFactor(Shell shell, int depthYards) {
        if (depthYards < 10) return 0.15;
        switch (shell) {
            case TWO_HIGH:
                return depthYards >= 18 ? 1.25 : 0.85;
            case SINGLE_HIGH:
                return depthYards >= 16 ? 1.15 : 0.70;
            case BOX_SS:
                return 0.45;
            case SPY_NO_DEEP:
                return 0.35;
            case SOFT:
                return 0.55;
            default:
                return 0.6;
        }
    }

    /**
     * Extra INT prior from coverage identity (replaces legacy intMod).
     * Applied on top of shell help / primary contest.
     */
    public static double coverageIntPrior(CoverageCall cov) {
        if (cov == null) return 1.0;
        switch (cov) {
            case COVER_0:
                return 1.20;
            case PRESS:
                return 1.15;
            case MAN:
                return 1.12;
            case COVER_1:
                return 1.10;
            case COVER_2:
            case OFF_COVERAGE:
                return 0.85;
            case COVER_4:
                return 0.80;
            case STACK_BOX:
                return 0.90;
            case ZONE:
                return 0.95;
            case SPY:
            case COVER_3:
            default:
                return 1.0;
        }
    }

    /**
     * Scramble / QB-keep contain (replaces legacy scrambleMod).
     * Lower = spy/contain works.
     */
    public static double containFactor(CoverageCall cov) {
        if (cov == null) return 1.0;
        switch (cov) {
            case SPY:
                return 0.70;
            case STACK_BOX:
                return 0.85;
            case COVER_2:
                return 0.95;
            case ZONE:
                return 0.98;
            case OFF_COVERAGE:
                return 0.90;
            case COVER_0:
                return 1.10;
            case PRESS:
                return 1.20;
            case COVER_1:
            case MAN:
                return 1.05;
            case COVER_4:
                return 1.05;
            case COVER_3:
            default:
                return 1.0;
        }
    }

    /** Soft crease / sep additives that used to live in passFitBonus / runFitBonus. */
    public static double passSepAdjust(CoverageCall cov) {
        if (cov == CoverageCall.COVER_4 || cov == CoverageCall.COVER_2) return 0.8;
        if (cov == CoverageCall.STACK_BOX) return -1.2;
        if (cov == CoverageCall.OFF_COVERAGE) return 1.4;
        if (cov == CoverageCall.PRESS) return -0.9;
        return 0;
    }

    public static double runCreaseAdjust(CoverageCall cov) {
        if (cov == CoverageCall.STACK_BOX) return -2.5;
        if (cov == CoverageCall.COVER_4 || cov == CoverageCall.OFF_COVERAGE) return 1.2;
        if (cov == CoverageCall.COVER_0 || cov == CoverageCall.PRESS) return 0.7;
        return 0;
    }

    public static Player deepHelper(List<CoverageAssignment> coverage) {
        if (coverage == null) return null;
        for (CoverageAssignment a : coverage) {
            if (a != null && a.deepHelp && a.defender != null) return a.defender;
        }
        for (CoverageAssignment a : coverage) {
            if (a != null && (a.defenderSlot == DefSlot.FS || a.defenderSlot == DefSlot.SS)
                    && a.defender != null) {
                return a.defender;
            }
        }
        return null;
    }
}
