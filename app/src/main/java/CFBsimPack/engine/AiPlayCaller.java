package CFBsimPack.engine;

import CFBsimPack.DefensiveSystem;
import CFBsimPack.OffensivePhilosophy;
import CFBsimPack.Team;
import CFBsimPack.TeamStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * AI playcaller using philosophy, system, weekly strategy, situation, and playbook concepts.
 */
public final class AiPlayCaller {

    private final Random rng;

    public AiPlayCaller(Random rng) {
        this.rng = rng != null ? rng : new Random();
    }

    public PlayCall choose(Team offense, Team defense, GameState state) {
        return suggest(offense, defense, state);
    }

    /** Public suggestion API for coach UI. */
    public PlayCall suggest(Team offense, Team defense, GameState state) {
        DefenseConcept defConcept = chooseDefenseConcept(defense, state);
        TempoCall tempo = chooseTempo(offense, state);
        OffenseConcept offConcept = chooseOffenseConcept(offense, defense, state);
        return PlayCall.fromConcepts(offConcept, defConcept, tempo);
    }

    /** Offense-only suggestion (user defense call kept separately). */
    public OffenseConcept suggestOffense(Team offense, Team defense, GameState state) {
        return chooseOffenseConcept(offense, defense, state);
    }

    public DefenseConcept suggestDefense(Team defense, GameState state) {
        return chooseDefenseConcept(defense, state);
    }

    private OffenseConcept chooseOffenseConcept(Team offense, Team defense, GameState state) {
        if (state.down > 4) {
            return Playbook.offenseById("punt");
        }

        int yardLine = state.yardLine;
        int down = state.down;
        int need = state.yardsNeed;
        int time = state.gameTime;
        boolean trailing = trailing(offense, state);

        if (!state.playingOT && time <= 30 && trailing) {
            int deficit = scoreDeficit(offense, state);
            if (deficit <= 3 && yardLine > 60) {
                return Playbook.offenseById("field_goal");
            }
            return weightedPick(scoreCandidates(offense, state, true), Playbook.offenseById("four_verts"));
        }

        if (down >= 4) {
            int deficit = scoreDeficit(offense, state);
            if (deficit > 3 && time < 300) {
                return weightedPick(scoreCandidates(offense, state, false),
                        need < 3 ? Playbook.offenseById("dive") : Playbook.offenseById("slants"));
            }
            if (need < 3 && yardLine > 55 && yardLine <= 65) {
                return Playbook.offenseById("dive");
            }
            if (yardLine > 60) {
                return Playbook.offenseById("field_goal");
            }
            return Playbook.offenseById("punt");
        }

        if (!state.playingOT && time < 120 && !trailing && yardLine < 50 && down <= 2) {
            if (rng.nextDouble() < 0.55) {
                return Playbook.offenseById("kneel");
            }
            return Playbook.offenseById("dive");
        }

        List<Scored> scored = scoreCandidates(offense, state, false);
        return weightedPick(scored, Playbook.defaultOffense());
    }

    private List<Scored> scoreCandidates(Team offense, GameState state, boolean forcePass) {
        OffensivePhilosophy phil = offense.offPhilosophy != null ? offense.offPhilosophy : OffensivePhilosophy.MULTIPLE;
        TeamStrategy offStrat = offense.teamStratOff;
        List<OffenseConcept> pool = Playbook.situationalOffense(state, false);
        List<Scored> scored = new ArrayList<>();
        for (OffenseConcept c : pool) {
            if (c.family == ConceptFamily.SPECIAL) continue;
            if (forcePass && c.offensePlay != OffensePlay.PASS && c.family != ConceptFamily.RPO) continue;
            double score = 1.0;
            if (c.family == ConceptFamily.PASS || c.family == ConceptFamily.RPO) {
                score += phil.passBias;
                if (offStrat != null) score += offStrat.getPAB() * 0.03;
                if (state.down == 3 && state.yardsNeed > 4) score += 0.35;
                if (c.depth == DepthBand.DEEP) {
                    score += state.yardsNeed >= 8 ? 0.25 : -0.35;
                    if (state.yardLine > 85) score -= 0.4;
                }
                if (c.depth == DepthBand.SHORT && state.yardsNeed <= 4) score += 0.2;
            } else {
                score += (1.0 - phil.passBias);
                if (offStrat != null) score += offStrat.getRYB() * 0.02;
                if (state.yardsNeed <= 2) score += 0.35;
                if (state.down == 1) score += 0.1;
            }
            if (formationFitsPhilosophy(c.formation, phil)) score += 0.15;
            if (phil == OffensivePhilosophy.AIR_RAID || phil == OffensivePhilosophy.RUN_AND_SHOOT) {
                if (c.family == ConceptFamily.PASS) score += 0.25;
            }
            if (phil == OffensivePhilosophy.POWER_RUN || phil == OffensivePhilosophy.SMASHMOUTH) {
                if (c.family == ConceptFamily.RUN) score += 0.3;
            }
            if (phil == OffensivePhilosophy.RPO_SPREAD && c.family == ConceptFamily.RPO) score += 0.35;
            if (score < 0.05) score = 0.05;
            scored.add(new Scored(c, score));
        }
        return scored;
    }

    private OffenseConcept weightedPick(List<Scored> scored, OffenseConcept fallback) {
        if (scored == null || scored.isEmpty()) return fallback;
        // Keep top candidates for variety
        scored.sort((a, b) -> Double.compare(b.score, a.score));
        int n = Math.min(6, scored.size());
        double sum = 0;
        for (int i = 0; i < n; i++) sum += scored.get(i).score;
        double roll = rng.nextDouble() * sum;
        double acc = 0;
        for (int i = 0; i < n; i++) {
            acc += scored.get(i).score;
            if (roll <= acc) return scored.get(i).concept;
        }
        return scored.get(0).concept;
    }

    public CoverageCall chooseCoverage(Team defense, GameState state) {
        return chooseDefenseConcept(defense, state).coverage;
    }

    public DefenseConcept chooseDefenseConcept(Team defense, GameState state) {
        DefensiveSystem sys = defense.defSystem != null ? defense.defSystem : DefensiveSystem.BASE_4_3;
        TeamStrategy defStrat = defense.teamStratDef;
        double roll = rng.nextDouble();

        if (defStrat != null && defStrat.getRYB() > 0 && roll < 0.35) {
            return Playbook.defenseFor(CoverageCall.STACK_BOX);
        }
        if (defStrat != null && defStrat.getPYB() > 0 && roll < 0.4) {
            return Playbook.defenseFor(CoverageCall.COVER_4);
        }
        if (state.down >= 3 && state.yardsNeed >= 7) {
            if (sys == DefensiveSystem.DIME || sys == DefensiveSystem.NICKEL) {
                return Playbook.defenseFor(rng.nextBoolean() ? CoverageCall.COVER_2 : CoverageCall.COVER_4);
            }
            return Playbook.defenseFor(CoverageCall.COVER_3);
        }
        if (state.yardsNeed <= 2) {
            return Playbook.defenseFor(CoverageCall.STACK_BOX);
        }
        CoverageCall[] base = {
                CoverageCall.COVER_3, CoverageCall.COVER_1, CoverageCall.COVER_2,
                CoverageCall.ZONE, CoverageCall.MAN, CoverageCall.PRESS
        };
        return Playbook.defenseFor(base[rng.nextInt(base.length)]);
    }

    private static boolean formationFitsPhilosophy(CFBsimPack.Formation form, OffensivePhilosophy phil) {
        if (form == null || phil == null) return false;
        switch (phil) {
            case AIR_RAID:
            case RUN_AND_SHOOT:
                return form == CFBsimPack.Formation.EMPTY || form == CFBsimPack.Formation.TRIPS
                        || form == CFBsimPack.Formation.SHOTGUN;
            case POWER_RUN:
            case SMASHMOUTH:
                return form == CFBsimPack.Formation.I_FORM || form == CFBsimPack.Formation.JUMBO;
            case PISTOL:
                return form == CFBsimPack.Formation.PISTOL;
            case SPREAD:
            case RPO_SPREAD:
                return form == CFBsimPack.Formation.SHOTGUN || form == CFBsimPack.Formation.TRIPS;
            default:
                return form == CFBsimPack.Formation.SHOTGUN || form == CFBsimPack.Formation.SINGLEBACK;
        }
    }

    private TempoCall chooseTempo(Team offense, GameState state) {
        if (state.gameTime < 120 && trailing(offense, state)) return TempoCall.HURRY_UP;
        if (state.gameTime < 180 && !trailing(offense, state)) return TempoCall.CHEW_CLOCK;
        OffensivePhilosophy phil = offense.offPhilosophy;
        if (phil == OffensivePhilosophy.AIR_RAID || phil == OffensivePhilosophy.RPO_SPREAD) {
            return rng.nextDouble() < 0.25 ? TempoCall.HURRY_UP : TempoCall.NORMAL;
        }
        return TempoCall.NORMAL;
    }

    private boolean trailing(Team offense, GameState state) {
        if (state.possessionHome) {
            return state.homeScore <= state.awayScore;
        }
        return state.awayScore <= state.homeScore;
    }

    private int scoreDeficit(Team offense, GameState state) {
        if (state.possessionHome) return state.awayScore - state.homeScore;
        return state.homeScore - state.awayScore;
    }

    private static final class Scored {
        final OffenseConcept concept;
        final double score;

        Scored(OffenseConcept concept, double score) {
            this.concept = concept;
            this.score = score;
        }
    }
}
