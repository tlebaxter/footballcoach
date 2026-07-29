package CFBsimPack.engine;

import CFBsimPack.Formation;
import CFBsimPack.engine.playdef.PlayDefinition;
import CFBsimPack.engine.playdef.PlayDefinitions;

/**
 * Named offensive play concept.
 * Display is text-only: formation + popular call name + concept tagline.
 * Wraps a {@link PlayDefinition} for assignment snap resolution.
 */
public final class OffenseConcept {
    public final String id;
    public final String displayName;
    public final ConceptFamily family;
    public final OffensePlay offensePlay;
    public final Formation formation;
    public final String personnel;
    public final DepthBand depth;
    /** Short coach-speak description of the concept (routes / scheme). */
    public final String concept;
    public final TargetBias targetBias;
    /** Tempo clock multiplier extra (not an outcome scaler). */
    public final double clockMultExtra;
    /** Structured play data for the snap engine. */
    public final PlayDefinition definition;

    public OffenseConcept(
            String id,
            String displayName,
            ConceptFamily family,
            OffensePlay offensePlay,
            Formation formation,
            String personnel,
            DepthBand depth,
            String concept,
            TargetBias targetBias,
            double clockMultExtra
    ) {
        this.id = id;
        this.displayName = displayName;
        this.family = family;
        this.offensePlay = offensePlay;
        this.formation = formation;
        this.personnel = personnel != null ? personnel : "11";
        this.depth = depth != null ? depth : DepthBand.NONE;
        this.concept = concept != null ? concept : "";
        this.targetBias = targetBias != null ? targetBias : TargetBias.ANY;
        this.clockMultExtra = clockMultExtra;
        this.definition = PlayDefinitions.build(
                this.id, this.displayName, this.family, this.offensePlay, this.formation,
                this.personnel, this.depth, this.concept, this.targetBias
        );
    }

    public String depthLabel() {
        switch (depth) {
            case SHORT:
                return "Short";
            case MEDIUM:
                return "Medium";
            case DEEP:
                return "Deep";
            default:
                return family == ConceptFamily.RUN ? "Run" : family.name();
        }
    }

    public String typeLabel() {
        if (family == ConceptFamily.RUN) return "Run";
        if (family == ConceptFamily.RPO) return "RPO";
        if (family == ConceptFamily.SPECIAL) return "Special";
        return depthLabel() + " Pass";
    }

    /** e.g. "Shotgun · Mesh · Short Pass" */
    public String callSheetLine() {
        return formation.displayName + " · " + displayName + " · " + typeLabel();
    }

    public String metaLine() {
        return formation.displayName + " · " + personnel + " pers · " + typeLabel();
    }
}
