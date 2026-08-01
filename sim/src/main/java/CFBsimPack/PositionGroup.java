package CFBsimPack;

/**
 * Recruitable / roster position groups (replaces F7 blob).
 */
public enum PositionGroup {
    QB("QB"),
    RB("RB"),
    FB("FB"),
    WR("WR"),
    TE("TE"),
    OL("OL"),
    EDGE("EDGE"),
    DL("DL"),
    LB("LB"),
    CB("CB"),
    S("S"),
    K("K"),
    P("P");

    public final String token;

    PositionGroup(String token) {
        this.token = token;
    }

    public static PositionGroup fromToken(String token) {
        if (token == null) return null;
        for (PositionGroup g : values()) {
            if (g.token.equalsIgnoreCase(token)) return g;
        }
        return null;
    }

    public boolean isDefensiveFront() {
        return this == EDGE || this == DL || this == LB;
    }

    public boolean isSecondary() {
        return this == CB || this == S;
    }
}
