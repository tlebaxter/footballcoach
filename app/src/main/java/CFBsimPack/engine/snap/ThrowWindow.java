package CFBsimPack.engine.snap;

import CFBsimPack.Player;
import CFBsimPack.engine.playdef.RouteAssignment;

/** Chosen throw (or pressure-out) from the pass timeline. */
public final class ThrowWindow {
    public enum Decision { THROW, PRESSURE_OUT, HOT_FORCE }

    public final Decision decision;
    public final RouteAssignment route;
    public final Player target;
    public final CoverageAssignment coverage;
    public final double throwTimeSec;
    public final double separation;
    public final boolean hot;

    public ThrowWindow(
            Decision decision,
            RouteAssignment route,
            Player target,
            CoverageAssignment coverage,
            double throwTimeSec,
            double separation,
            boolean hot
    ) {
        this.decision = decision != null ? decision : Decision.THROW;
        this.route = route;
        this.target = target;
        this.coverage = coverage;
        this.throwTimeSec = throwTimeSec;
        this.separation = separation;
        this.hot = hot;
    }
}
