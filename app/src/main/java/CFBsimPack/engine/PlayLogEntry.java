package CFBsimPack.engine;

/**
 * Structured play log row for coach HUD.
 */
public final class PlayLogEntry {
    public final String clockLabel;
    public final int quarter;
    public final int down;
    public final int distance;
    public final int yardLineBefore;
    public final int yardsGained;
    public final String offenseConceptId;
    public final String offenseConceptName;
    public final String defenseConceptId;
    public final String defenseConceptName;
    public final String logLine;
    public final boolean possessionHome;

    public PlayLogEntry(
            String clockLabel,
            int quarter,
            int down,
            int distance,
            int yardLineBefore,
            int yardsGained,
            String offenseConceptId,
            String offenseConceptName,
            String defenseConceptId,
            String defenseConceptName,
            String logLine,
            boolean possessionHome
    ) {
        this.clockLabel = clockLabel != null ? clockLabel : "";
        this.quarter = quarter;
        this.down = down;
        this.distance = distance;
        this.yardLineBefore = yardLineBefore;
        this.yardsGained = yardsGained;
        this.offenseConceptId = offenseConceptId;
        this.offenseConceptName = offenseConceptName != null ? offenseConceptName : "";
        this.defenseConceptId = defenseConceptId;
        this.defenseConceptName = defenseConceptName != null ? defenseConceptName : "";
        this.logLine = logLine != null ? logLine : "";
        this.possessionHome = possessionHome;
    }
}
