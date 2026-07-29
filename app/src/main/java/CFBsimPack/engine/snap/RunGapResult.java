package CFBsimPack.engine.snap;

public final class RunGapResult {
    public final Gap gapUsed;
    public final boolean cutback;
    public final double crease; // roughly -10..+15
    public final int yards;
    public final boolean explosive;

    public RunGapResult(Gap gapUsed, boolean cutback, double crease, int yards, boolean explosive) {
        this.gapUsed = gapUsed;
        this.cutback = cutback;
        this.crease = crease;
        this.yards = yards;
        this.explosive = explosive;
    }
}
