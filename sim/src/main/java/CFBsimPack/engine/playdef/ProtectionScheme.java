package CFBsimPack.engine.playdef;

import CFBsimPack.engine.DepthBand;
import CFBsimPack.engine.snap.OffSlot;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class ProtectionScheme {
    public final ProtectionType type;
    public final double hotTimeSec;
    public final Set<OffSlot> maxProtectSlots;
    public final boolean allowDoubleTeams;

    public ProtectionScheme(
            ProtectionType type,
            double hotTimeSec,
            Set<OffSlot> maxProtectSlots,
            boolean allowDoubleTeams
    ) {
        this.type = type != null ? type : ProtectionType.BIG_ON_BIG;
        this.hotTimeSec = hotTimeSec > 0 ? hotTimeSec : 1.1;
        if (maxProtectSlots == null || maxProtectSlots.isEmpty()) {
            this.maxProtectSlots = Collections.emptySet();
        } else {
            this.maxProtectSlots = Collections.unmodifiableSet(EnumSet.copyOf(maxProtectSlots));
        }
        this.allowDoubleTeams = allowDoubleTeams;
    }

    public static ProtectionScheme infer(DepthBand depth, boolean emptyPersonnel, boolean playAction) {
        if (emptyPersonnel) {
            return new ProtectionScheme(ProtectionType.EMPTY_FIVE, 1.0, Collections.emptySet(), false);
        }
        if (playAction) {
            return new ProtectionScheme(ProtectionType.PLAY_ACTION, 1.35, Collections.emptySet(), true);
        }
        if (depth == DepthBand.DEEP) {
            Set<OffSlot> max = EnumSet.of(OffSlot.RB, OffSlot.TE_L);
            return new ProtectionScheme(ProtectionType.HALF_SLIDE_RIGHT, 1.4, max, true);
        }
        if (depth == DepthBand.SHORT) {
            return new ProtectionScheme(ProtectionType.BIG_ON_BIG, 1.0, Collections.emptySet(), true);
        }
        return new ProtectionScheme(ProtectionType.HALF_SLIDE_LEFT, 1.15, Collections.emptySet(), true);
    }

    public static ProtectionScheme maxProtect() {
        return new ProtectionScheme(
                ProtectionType.MAX_PROTECT,
                1.5,
                EnumSet.of(OffSlot.RB, OffSlot.FB, OffSlot.TE_L, OffSlot.TE_R),
                true
        );
    }
}
