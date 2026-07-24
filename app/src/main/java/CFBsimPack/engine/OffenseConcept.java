package CFBsimPack.engine;

import CFBsimPack.Formation;

/**
 * Named offensive play concept with engine modifiers.
 * Display is text-only: formation + popular call name + concept tagline.
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
    public final double completionMod;
    public final double yardsMod;
    public final double sackRiskMod;
    public final double runYardsMod;
    public final double fumbleMod;
    public final TargetBias targetBias;
    public final double clockMultExtra;

    public OffenseConcept(
            String id,
            String displayName,
            ConceptFamily family,
            OffensePlay offensePlay,
            Formation formation,
            String personnel,
            DepthBand depth,
            String concept,
            double completionMod,
            double yardsMod,
            double sackRiskMod,
            double runYardsMod,
            double fumbleMod,
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
        this.completionMod = completionMod;
        this.yardsMod = yardsMod;
        this.sackRiskMod = sackRiskMod;
        this.runYardsMod = runYardsMod;
        this.fumbleMod = fumbleMod;
        this.targetBias = targetBias != null ? targetBias : TargetBias.ANY;
        this.clockMultExtra = clockMultExtra;
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

    /**
     * Soft capped matchup adjustment added to completion (pass) or block advantage (run).
     */
    public double matchupBonus(CoverageCall cov) {
        return matchupBonus(cov, null);
    }

    /**
     * Situation-aware matchup adjustment (red zone / short yardage / late deep).
     */
    public double matchupBonus(CoverageCall cov, GameState state) {
        if (cov == null) return 0;
        boolean shortOrGoal = state != null
                && (state.yardsNeed <= 2 || state.yardLine >= 95);
        boolean redZoneDeep = state != null && state.yardLine >= 85;
        boolean lateDeep = state != null && state.gameTime <= 40;

        double bonus = 0;
        if (family == ConceptFamily.RUN || offensePlay == OffensePlay.RUN) {
            if (cov == CoverageCall.STACK_BOX) bonus -= shortOrGoal ? 2 : 5;
            else if (cov == CoverageCall.COVER_4 || cov == CoverageCall.OFF_COVERAGE) bonus += 3.5;
            else if (cov == CoverageCall.COVER_0 || cov == CoverageCall.PRESS) bonus += 2;
        } else if (offensePlay == OffensePlay.PASS || family == ConceptFamily.RPO) {
            if (depth == DepthBand.DEEP) {
                if (cov == CoverageCall.COVER_0 || cov == CoverageCall.MAN || cov == CoverageCall.PRESS) {
                    if (redZoneDeep && (cov == CoverageCall.MAN || cov == CoverageCall.PRESS)) {
                        bonus += 1.5;
                    } else {
                        bonus += lateDeep && (cov == CoverageCall.COVER_0 || cov == CoverageCall.MAN)
                                ? 5 : 4;
                    }
                }
                if (cov == CoverageCall.COVER_4 || cov == CoverageCall.COVER_2) {
                    bonus -= lateDeep ? 5 : 3.5;
                }
                if (cov == CoverageCall.SPY) bonus -= 1.5;
            } else if (depth == DepthBand.MEDIUM) {
                if (cov == CoverageCall.COVER_3 || cov == CoverageCall.ZONE) bonus += 1.5;
                if (cov == CoverageCall.COVER_0) bonus -= 1;
            } else if (depth == DepthBand.SHORT) {
                if (cov == CoverageCall.COVER_4 || cov == CoverageCall.OFF_COVERAGE) bonus += 2.5;
                if (cov == CoverageCall.PRESS || cov == CoverageCall.MAN) bonus -= 1.5;
                if (cov == CoverageCall.STACK_BOX && targetBias == TargetBias.RB) bonus -= 2;
            }
        }
        if (bonus > 6) bonus = 6;
        if (bonus < -6) bonus = -6;
        return bonus;
    }
}
