package CFBsimPack;

/**
 * System / depth-chart role labels (vocab for ESPN-style UI and slot fill).
 */
public enum RoleTag {
    QB,
    RB,
    FB,
    WR,
    SLOT,
    TE,
    OL,
    NT,
    DT,
    DE,
    EDGE,
    MIKE,
    WILL,
    SAM,
    ILB,
    OLB,
    LB,
    CB,
    NB,
    FS,
    SS,
    S,
    K,
    /** Special-teams overlay roles (display / depth UI). */
    PR,
    KR,
    GUNNER,
    LS;

    public PositionGroup preferredGroup() {
        switch (this) {
            case QB: return PositionGroup.QB;
            case RB: return PositionGroup.RB;
            case FB: return PositionGroup.FB;
            case WR:
            case SLOT:
            case PR:
            case KR:
            case GUNNER: return PositionGroup.WR;
            case TE:
            case LS: return PositionGroup.TE;
            case OL: return PositionGroup.OL;
            case NT:
            case DT:
            case DE: return PositionGroup.DL;
            case EDGE: return PositionGroup.EDGE;
            case MIKE:
            case WILL:
            case SAM:
            case ILB:
            case OLB:
            case LB: return PositionGroup.LB;
            case CB:
            case NB: return PositionGroup.CB;
            case FS:
            case SS:
            case S: return PositionGroup.S;
            case K: return PositionGroup.K;
            default: return PositionGroup.LB;
        }
    }
}
