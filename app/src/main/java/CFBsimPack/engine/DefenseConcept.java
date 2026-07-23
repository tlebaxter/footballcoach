package CFBsimPack.engine;

/**
 * Named defensive coverage concept — mods live on {@link CoverageCall}.
 */
public final class DefenseConcept {
    public final String id;
    public final String displayName;
    public final CoverageCall coverage;
    public final PlayDiagram diagram;

    public DefenseConcept(String id, String displayName, CoverageCall coverage, PlayDiagram diagram) {
        this.id = id;
        this.displayName = displayName;
        this.coverage = coverage != null ? coverage : CoverageCall.COVER_3;
        this.diagram = diagram != null ? diagram : PlayDiagram.defense();
    }

    public static DefenseConcept of(CoverageCall coverage) {
        return Playbook.defenseFor(coverage);
    }
}
