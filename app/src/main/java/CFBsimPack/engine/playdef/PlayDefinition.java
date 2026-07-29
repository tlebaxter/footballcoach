package CFBsimPack.engine.playdef;

import CFBsimPack.Formation;
import CFBsimPack.engine.ConceptFamily;
import CFBsimPack.engine.DepthBand;
import CFBsimPack.engine.OffensePlay;
import CFBsimPack.engine.TargetBias;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Structured play data for assignment snap resolution.
 * {@link CFBsimPack.engine.OffenseConcept} remains the AI/UI facade.
 */
public final class PlayDefinition {
    public final String id;
    public final String displayName;
    public final String blurb;
    public final ConceptFamily family;
    public final OffensePlay offensePlay;
    public final Formation formation;
    public final String personnel;
    public final DepthBand depth;
    public final TargetBias targetBias;

    public final ProtectionScheme protection;
    public final List<RouteAssignment> routes;
    public final RunScheme run;
    public final RpoRules rpoRules;

    public PlayDefinition(
            String id,
            String displayName,
            String blurb,
            ConceptFamily family,
            OffensePlay offensePlay,
            Formation formation,
            String personnel,
            DepthBand depth,
            TargetBias targetBias,
            ProtectionScheme protection,
            List<RouteAssignment> routes,
            RunScheme run,
            RpoRules rpoRules
    ) {
        this.id = id;
        this.displayName = displayName;
        this.blurb = blurb != null ? blurb : "";
        this.family = family != null ? family : ConceptFamily.PASS;
        this.offensePlay = offensePlay != null ? offensePlay : OffensePlay.PASS;
        this.formation = formation != null ? formation : Formation.SHOTGUN;
        this.personnel = personnel != null ? personnel : "11";
        this.depth = depth != null ? depth : DepthBand.NONE;
        this.targetBias = targetBias != null ? targetBias : TargetBias.ANY;
        this.protection = protection;
        this.routes = routes != null
                ? Collections.unmodifiableList(new ArrayList<>(routes))
                : Collections.emptyList();
        this.run = run;
        this.rpoRules = rpoRules;
    }
}
