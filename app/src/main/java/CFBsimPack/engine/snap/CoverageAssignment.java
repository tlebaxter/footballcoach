package CFBsimPack.engine.snap;

import CFBsimPack.Player;

public final class CoverageAssignment {
    public final Player defender;
    public final DefSlot defenderSlot;
    public final CoverageMode mode;
    public final ZoneLandmark zoneLandmark;
    public final OffSlot manTarget;
    public final boolean isSpy;
    public final boolean deepHelp;

    public CoverageAssignment(
            Player defender,
            DefSlot defenderSlot,
            CoverageMode mode,
            ZoneLandmark zoneLandmark,
            OffSlot manTarget,
            boolean isSpy,
            boolean deepHelp
    ) {
        this.defender = defender;
        this.defenderSlot = defenderSlot;
        this.mode = mode != null ? mode : CoverageMode.ZONE;
        this.zoneLandmark = zoneLandmark;
        this.manTarget = manTarget;
        this.isSpy = isSpy;
        this.deepHelp = deepHelp;
    }
}
