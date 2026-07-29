package CFBsimPack.engine.playdef;

import CFBsimPack.engine.snap.OffSlot;

public final class RouteAssignment {
    public final OffSlot slot;
    public final RouteType route;
    public final double openBeatSec;
    public final int depthYards;
    public final boolean hotEligible;
    public final int readPriority;

    public RouteAssignment(
            OffSlot slot,
            RouteType route,
            double openBeatSec,
            int depthYards,
            boolean hotEligible,
            int readPriority
    ) {
        this.slot = slot;
        this.route = route != null ? route : RouteType.HITCH;
        this.openBeatSec = openBeatSec > 0 ? openBeatSec : this.route.defaultOpenBeatSec;
        this.depthYards = depthYards > 0 ? depthYards : this.route.defaultDepthYards;
        this.hotEligible = hotEligible;
        this.readPriority = Math.max(1, readPriority);
    }

    public static RouteAssignment of(OffSlot slot, RouteType route, int priority, boolean hot) {
        return new RouteAssignment(slot, route, route.defaultOpenBeatSec, route.defaultDepthYards, hot, priority);
    }
}
