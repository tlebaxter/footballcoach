package CFBsimPack.engine;

import CFBsimPack.DefensiveSystem;
import CFBsimPack.OffensivePhilosophy;
import CFBsimPack.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * AI playcaller using offense philosophy, defensive system, and situation.
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
        OffenseConcept offConcept = chooseOffenseConcept(offense, defense, state);
        DefenseConcept defConcept = chooseDefenseConcept(defense, state, offConcept);
        TempoCall tempo = chooseTempo(offense, state);
        return PlayCall.fromConcepts(offConcept, defConcept, tempo);
    }

    /** Offense-only suggestion (user defense call kept separately). */
    public OffenseConcept suggestOffense(Team offense, Team defense, GameState state) {
        return chooseOffenseConcept(offense, defense, state);
    }

    public DefenseConcept suggestDefense(Team defense, GameState state) {
        return chooseDefenseConcept(defense, state, null);
    }

    /** Defense suggestion that can react to a known offense concept (4th down / try). */
    public DefenseConcept suggestDefense(Team defense, GameState state, OffenseConcept offenseConcept) {
        return chooseDefenseConcept(defense, state, offenseConcept);
    }

    /**
     * Whether AI offense should go for 2 after a TD instead of kicking XP.
     */
    public boolean shouldGoForTwo(Team offense, GameState state) {
        if (state == null) return false;
        if (state.playingOT) {
            // In OT, kick unless behind after scoring 6 (need the deuce).
            int deficit = scoreDeficit(offense, state);
            return deficit == 1 || deficit == 2;
        }
        int deficit = scoreDeficit(offense, state);
        // After TD, deficit is relative to pre-try score (already includes the 6).
        if (state.gameTime <= 180 && (deficit == 1 || deficit == 2)) return true;
        if (state.gameTime <= 60 && deficit == 5) return true; // TD made it 5; 2 makes it 3 (FG range)
        if (state.gameTime <= 120 && deficit == -1) return rng.nextDouble() < 0.35; // go up by 2 instead of 1
        return false;
    }

    private OffenseConcept chooseOffenseConcept(Team offense, Team defense, GameState state) {
        if (state.pendingTry && state.tryIsTwoPoint) {
            List<Scored> scored = scoreCandidates(offense, state, false);
            return weightedPick(scored, Playbook.offenseById("i_dive"));
        }
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
            return weightedPick(scoreCandidates(offense, state, true), Playbook.offenseById("gun_four_verts"));
        }

        if (state.pendingKickoff) {
            return Playbook.offenseById("kickoff");
        }

        if (down >= 4) {
            int deficit = scoreDeficit(offense, state);
            if (deficit > 3 && time < 300) {
                return weightedPick(scoreCandidates(offense, state, false),
                        need < 3 ? Playbook.offenseById("i_dive") : Playbook.offenseById("gun_slants"));
            }
            if (need < 3 && yardLine > 55 && yardLine <= 65) {
                return Playbook.offenseById("i_dive");
            }
            if (yardLine > 60) {
                return Playbook.offenseById("field_goal");
            }
            if (trailing && deficit > 0 && yardLine >= 40 && yardLine <= 55 && rng.nextDouble() < 0.07) {
                return Playbook.offenseById("fake_punt");
            }
            return Playbook.offenseById("punt");
        }

        if (!state.playingOT && time < 120 && !trailing && yardLine < 50 && down <= 2) {
            if (rng.nextDouble() < 0.55) {
                return Playbook.offenseById("kneel");
            }
            return Playbook.offenseById("i_dive");
        }

        List<Scored> scored = scoreCandidates(offense, state, false);
        return weightedPick(scored, Playbook.defaultOffense());
    }

    private List<Scored> scoreCandidates(Team offense, GameState state, boolean forcePass) {
        OffensivePhilosophy phil = offense.offPhilosophy != null ? offense.offPhilosophy : OffensivePhilosophy.MULTIPLE;
        List<OffenseConcept> pool = Playbook.situationalOffense(state, false);
        List<Scored> scored = new ArrayList<>();
        for (OffenseConcept c : pool) {
            if (c.family == ConceptFamily.SPECIAL) continue;
            if (forcePass && c.offensePlay != OffensePlay.PASS && c.family != ConceptFamily.RPO) continue;
            double score = 1.0;
            if (c.family == ConceptFamily.PASS || c.family == ConceptFamily.RPO) {
                score += phil.passBias;
                if (state.down == 3 && state.yardsNeed > 4) score += 0.35;
                if (c.depth == DepthBand.DEEP) {
                    score += state.yardsNeed >= 8 ? 0.25 : -0.35;
                    if (state.yardLine > 85) score -= 0.4;
                }
                if (c.depth == DepthBand.SHORT && state.yardsNeed <= 4) score += 0.2;
            } else {
                score += (1.0 - phil.passBias);
                if (state.yardsNeed <= 2) score += 0.35;
                if (state.down == 1) score += 0.1;
            }
            if (formationFitsPhilosophy(c.formation, phil)) score += 0.35;
            if (personnelFitsPhilosophy(c.personnel, phil)) score += 0.25;
            score += philosophyFamilyBonus(phil, c);
            if (score < 0.05) score = 0.05;
            scored.add(new Scored(c, score));
        }
        return scored;
    }

    private static double philosophyFamilyBonus(OffensivePhilosophy phil, OffenseConcept c) {
        switch (phil) {
            case AIR_RAID:
            case RUN_AND_SHOOT:
                if (c.family == ConceptFamily.PASS) return 0.45;
                if (c.family == ConceptFamily.RUN) return -0.15;
                return 0;
            case POWER_RUN:
            case SMASHMOUTH:
                if (c.family == ConceptFamily.RUN) return 0.5;
                if (c.family == ConceptFamily.PASS && c.depth == DepthBand.DEEP) return -0.2;
                return 0;
            case RPO_SPREAD:
                if (c.family == ConceptFamily.RPO) return 0.55;
                return 0;
            case WEST_COAST:
                if (c.family == ConceptFamily.PASS
                        && (c.depth == DepthBand.SHORT || c.depth == DepthBand.MEDIUM)) {
                    return 0.4;
                }
                if (c.depth == DepthBand.DEEP) return -0.15;
                return 0;
            case OPTION:
            case FLEXBONE:
                if (c.family == ConceptFamily.RUN) return 0.45;
                if (c.family == ConceptFamily.PASS) return -0.1;
                return 0;
            case PISTOL:
                if (c.formation == CFBsimPack.Formation.PISTOL) return 0.35;
                if (c.family == ConceptFamily.RUN) return 0.15;
                return 0;
            case SPREAD:
                if (c.family == ConceptFamily.PASS || c.family == ConceptFamily.RPO) return 0.2;
                return 0;
            default:
                return 0;
        }
    }

    private static boolean personnelFitsPhilosophy(String personnel, OffensivePhilosophy phil) {
        if (personnel == null || phil == null || phil.defaultPersonnel == null) return false;
        return phil.defaultPersonnel.equals(personnel);
    }

    private OffenseConcept weightedPick(List<Scored> scored, OffenseConcept fallback) {
        if (scored == null || scored.isEmpty()) return fallback;
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

    public DefenseConcept chooseDefenseConcept(Team defense, GameState state, OffenseConcept offenseConcept) {
        if (state.pendingKickoff) {
            return rng.nextDouble() < 0.2
                    ? Playbook.defenseById("kick_fair_catch")
                    : Playbook.defenseById("kick_return");
        }

        if (state.pendingTry && state.tryIsTwoPoint) {
            return chooseCoverageBySystem(defense, state);
        }

        if (state.down >= 4 || (offenseConcept != null && isSpecialTeamsOffense(offenseConcept))) {
            OffensePlay op = offenseConcept != null ? offenseConcept.offensePlay : null;
            if (op == OffensePlay.FIELD_GOAL) {
                return Playbook.defenseById("defend_scrimmage");
            }
            if (op == OffensePlay.FAKE_PUNT) {
                return Playbook.defenseById("defend_scrimmage");
            }
            if (op == OffensePlay.PUNT || offenseConcept == null) {
                // Unknown offense on 4th: mostly punt packages; leave room for go-for-it
                double roll = rng.nextDouble();
                if (offenseConcept == null && roll < 0.22) {
                    return Playbook.defenseById("defend_scrimmage");
                }
                if (roll < 0.12) return Playbook.defenseById("punt_block");
                if (roll < 0.30) return Playbook.defenseById("fair_catch");
                if (offenseConcept == null && roll < 0.42) {
                    return Playbook.defenseById("defend_scrimmage");
                }
                return Playbook.defenseById("punt_return");
            }
            // Go-for-it / other scrimmage 4th
            return chooseCoverageBySystem(defense, state);
        }

        return chooseCoverageBySystem(defense, state);
    }

    private static boolean isSpecialTeamsOffense(OffenseConcept c) {
        if (c == null) return false;
        OffensePlay p = c.offensePlay;
        return p == OffensePlay.PUNT || p == OffensePlay.FAKE_PUNT || p == OffensePlay.FIELD_GOAL
                || p == OffensePlay.KICKOFF;
    }

    private DefenseConcept chooseCoverageBySystem(Team defense, GameState state) {
        DefensiveSystem sys = defense.defSystem != null ? defense.defSystem : DefensiveSystem.BASE_4_3;
        double roll = rng.nextDouble();

        if (state.yardsNeed <= 2 && !state.pendingTry) {
            return Playbook.defenseFor(CoverageCall.STACK_BOX);
        }
        if (state.down >= 3 && state.yardsNeed >= 7) {
            if (sys == DefensiveSystem.DIME || sys == DefensiveSystem.NICKEL || sys == DefensiveSystem.FOUR_TWO_FIVE) {
                return Playbook.defenseFor(rng.nextBoolean() ? CoverageCall.COVER_2 : CoverageCall.COVER_4);
            }
            return Playbook.defenseFor(CoverageCall.COVER_3);
        }

        // Run-heavy packages sell out vs run more often
        if (sys.runWeight >= 1.15 && roll < 0.38) {
            return Playbook.defenseFor(CoverageCall.STACK_BOX);
        }
        // Pass-heavy packages prefer deeper / tighter coverages
        if (sys.passWeight >= 1.10) {
            if (roll < 0.28) return Playbook.defenseFor(CoverageCall.COVER_4);
            if (roll < 0.48) return Playbook.defenseFor(CoverageCall.COVER_2);
            if (roll < 0.62) return Playbook.defenseFor(CoverageCall.PRESS);
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
            case OPTION:
            case FLEXBONE:
                return form == CFBsimPack.Formation.I_FORM || form == CFBsimPack.Formation.PISTOL
                        || form == CFBsimPack.Formation.SINGLEBACK;
            case WEST_COAST:
                return form == CFBsimPack.Formation.SINGLEBACK || form == CFBsimPack.Formation.SHOTGUN
                        || form == CFBsimPack.Formation.I_FORM;
            default:
                return form == CFBsimPack.Formation.SHOTGUN || form == CFBsimPack.Formation.SINGLEBACK;
        }
    }

    private TempoCall chooseTempo(Team offense, GameState state) {
        if (state.pendingTry) return TempoCall.NORMAL;
        if (state.gameTime < 120 && trailing(offense, state)) return TempoCall.HURRY_UP;
        if (state.gameTime < 180 && !trailing(offense, state)) return TempoCall.CHEW_CLOCK;
        OffensivePhilosophy phil = offense.offPhilosophy;
        if (phil == OffensivePhilosophy.AIR_RAID || phil == OffensivePhilosophy.RPO_SPREAD
                || phil == OffensivePhilosophy.RUN_AND_SHOOT) {
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
