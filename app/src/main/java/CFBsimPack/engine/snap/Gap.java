package CFBsimPack.engine.snap;

/** Run / rush gap landmarks (offense perspective). */
public enum Gap {
    A_L, A_R, B_L, B_R, C_L, C_R, D_L, D_R;

    public boolean isInterior() {
        return this == A_L || this == A_R || this == B_L || this == B_R;
    }

    public boolean isEdge() {
        return this == C_L || this == C_R || this == D_L || this == D_R;
    }
}
