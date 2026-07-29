package CFBsimPack.engine.playdef;

public enum RouteType {
    SLANT(0.95, 6),
    HITCH(1.05, 5),
    OUT(1.25, 8),
    DIG(1.85, 14),
    CROSS(1.70, 12),
    MESH_CROSS(1.55, 8),
    SEAM(2.20, 18),
    VERT(2.70, 28),
    POST(2.55, 24),
    CORNER(2.60, 22),
    FLAT(0.90, 3),
    ANGLE(1.20, 7),
    SCREEN(0.85, 1),
    GO_STOP(2.40, 16);

    public final double defaultOpenBeatSec;
    public final int defaultDepthYards;

    RouteType(double openBeat, int depth) {
        this.defaultOpenBeatSec = openBeat;
        this.defaultDepthYards = depth;
    }
}
