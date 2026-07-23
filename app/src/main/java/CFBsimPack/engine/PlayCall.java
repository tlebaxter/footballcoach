package CFBsimPack.engine;

import CFBsimPack.Formation;

public final class PlayCall {
    public final OffensePlay offensePlay;
    public final Formation formation;
    public final CoverageCall coverage;
    public final TempoCall tempo;
    public final OffenseConcept offenseConcept;
    public final DefenseConcept defenseConcept;

    public PlayCall(OffensePlay offensePlay, Formation formation, CoverageCall coverage, TempoCall tempo) {
        this(offensePlay, formation, coverage, tempo, null, null);
    }

    public PlayCall(
            OffensePlay offensePlay,
            Formation formation,
            CoverageCall coverage,
            TempoCall tempo,
            OffenseConcept offenseConcept,
            DefenseConcept defenseConcept
    ) {
        this.offenseConcept = offenseConcept;
        this.defenseConcept = defenseConcept != null
                ? defenseConcept
                : (coverage != null ? Playbook.defenseFor(coverage) : Playbook.defaultDefense());
        this.coverage = this.defenseConcept.coverage;
        if (offenseConcept != null) {
            this.offensePlay = offenseConcept.offensePlay;
            this.formation = offenseConcept.formation != null ? offenseConcept.formation : Formation.SHOTGUN;
        } else {
            this.offensePlay = offensePlay != null ? offensePlay : OffensePlay.RUN;
            this.formation = formation != null ? formation : Formation.SHOTGUN;
        }
        this.tempo = tempo != null ? tempo : TempoCall.NORMAL;
    }

    public static PlayCall fromConcepts(OffenseConcept offense, DefenseConcept defense, TempoCall tempo) {
        OffenseConcept off = offense != null ? offense : Playbook.defaultOffense();
        DefenseConcept def = defense != null ? defense : Playbook.defaultDefense();
        return new PlayCall(off.offensePlay, off.formation, def.coverage, tempo, off, def);
    }

    public OffenseConcept resolvedOffenseConcept() {
        if (offenseConcept != null) return offenseConcept;
        // Best-effort map for legacy bare calls
        if (offensePlay == OffensePlay.PASS) return Playbook.offenseById("gun_slants");
        if (offensePlay == OffensePlay.FIELD_GOAL) return Playbook.offenseById("field_goal");
        if (offensePlay == OffensePlay.PUNT) return Playbook.offenseById("punt");
        if (offensePlay == OffensePlay.FAKE_PUNT) return Playbook.offenseById("fake_punt");
        if (offensePlay == OffensePlay.KICKOFF) return Playbook.offenseById("kickoff");
        if (offensePlay == OffensePlay.SPIKE) return Playbook.offenseById("spike");
        if (offensePlay == OffensePlay.KNEEL) return Playbook.offenseById("kneel");
        return Playbook.defaultOffense();
    }

    public DefenseConcept resolvedDefenseConcept() {
        return defenseConcept != null ? defenseConcept : Playbook.defenseFor(coverage);
    }
}
