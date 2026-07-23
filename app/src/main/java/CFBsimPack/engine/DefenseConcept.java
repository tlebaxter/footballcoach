package CFBsimPack.engine;

/**
 * Named defensive coverage concept — mods live on {@link CoverageCall}.
 */
public final class DefenseConcept {
    public final String id;
    public final String displayName;
    public final CoverageCall coverage;
    /** Short coach-speak description. */
    public final String concept;

    public DefenseConcept(String id, String displayName, CoverageCall coverage, String concept) {
        this.id = id;
        this.displayName = displayName;
        this.coverage = coverage != null ? coverage : CoverageCall.COVER_3;
        this.concept = concept != null ? concept : "";
    }
}
