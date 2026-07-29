package CFBsimPack.engine.playdef;

import CFBsimPack.engine.snap.Gap;
import CFBsimPack.engine.snap.OffSlot;

public final class RunScheme {
    public final Gap primaryGap;
    public final Gap secondaryGap;
    public final RunTrack track;
    public final OffSlot leadBlocker;
    public final OffSlot doubleTeamA;
    public final OffSlot doubleTeamB;
    public final ExplosiveProfile explosive;

    public RunScheme(
            Gap primaryGap,
            Gap secondaryGap,
            RunTrack track,
            OffSlot leadBlocker,
            OffSlot doubleTeamA,
            OffSlot doubleTeamB,
            ExplosiveProfile explosive
    ) {
        this.primaryGap = primaryGap != null ? primaryGap : Gap.A_R;
        this.secondaryGap = secondaryGap;
        this.track = track != null ? track : RunTrack.INSIDE_ZONE;
        this.leadBlocker = leadBlocker;
        this.doubleTeamA = doubleTeamA;
        this.doubleTeamB = doubleTeamB;
        this.explosive = explosive != null ? explosive : ExplosiveProfile.forTrack(this.track);
    }

    public static RunScheme forTrack(RunTrack track) {
        if (track == null) track = RunTrack.INSIDE_ZONE;
        switch (track) {
            case OUTSIDE_ZONE:
                return new RunScheme(Gap.C_R, Gap.B_R, track, null, OffSlot.LG, OffSlot.C,
                        ExplosiveProfile.forTrack(track));
            case SWEEP:
                return new RunScheme(Gap.D_R, Gap.C_R, track, OffSlot.FB, null, null,
                        ExplosiveProfile.forTrack(track));
            case POWER:
                return new RunScheme(Gap.B_R, Gap.A_R, track, OffSlot.FB, OffSlot.RG, OffSlot.RT,
                        ExplosiveProfile.forTrack(track));
            case COUNTER:
                return new RunScheme(Gap.B_L, Gap.A_L, track, OffSlot.RG, OffSlot.LG, OffSlot.C,
                        ExplosiveProfile.forTrack(track));
            case ISO:
                return new RunScheme(Gap.A_R, null, track, OffSlot.FB, OffSlot.RG, OffSlot.C,
                        ExplosiveProfile.forTrack(track));
            case DIVE:
                return new RunScheme(Gap.A_L, null, track, null, OffSlot.LG, OffSlot.C,
                        ExplosiveProfile.forTrack(track));
            case QB_DRAW:
                return new RunScheme(Gap.B_L, Gap.A_L, track, null, null, null,
                        ExplosiveProfile.forTrack(track));
            case OPTION:
                return new RunScheme(Gap.B_R, Gap.C_R, track, null, null, null,
                        ExplosiveProfile.forTrack(track));
            case INSIDE_ZONE:
            default:
                return new RunScheme(Gap.B_L, Gap.A_R, RunTrack.INSIDE_ZONE, null, OffSlot.LG, OffSlot.C,
                        ExplosiveProfile.forTrack(RunTrack.INSIDE_ZONE));
        }
    }
}
