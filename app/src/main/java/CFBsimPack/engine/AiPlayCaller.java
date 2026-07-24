package CFBsimPack.engine;

import CFBsimPack.DefensiveSystem;
import CFBsimPack.OffensivePhilosophy;
import CFBsimPack.Player;
import CFBsimPack.Team;

import java.util.ArrayList;
import java.util.Collections;
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
        DefenseConcept defConcept = chooseDefenseConcept(offense, defense, state, offConcept);
        TempoCall tempo = chooseTempo(offense, state);
        return PlayCall.fromConcepts(offConcept, defConcept, tempo);
    }

    /** Offense-only suggestion (user defense call kept separately). */
    public OffenseConcept suggestOffense(Team offense, Team defense, GameState state) {
        return chooseOffenseConcept(offense, defense, state);
    }

    public DefenseConcept suggestDefense(Team defense, GameState state) {
        return chooseDefenseConcept(null, defense, state, null);
    }

    /** Defense suggestion that can react to a known offense concept (4th down / try). */
    public DefenseConcept suggestDefense(Team defense, GameState state, OffenseConcept offenseConcept) {
        return chooseDefenseConcept(null, defense, state, offenseConcept);
    }

    /** Defense suggestion with offense context (Spy vs mobile QBs). */
    public DefenseConcept suggestDefense(Team offense, Team defense, GameState state,
                                         OffenseConcept offenseConcept) {
        return chooseDefenseConcept(offense, defense, state, offenseConcept);
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
        int deficit = scoreDeficit(offense, state);
        int yardsToGoal = Math.max(1, 100 - yardLine);

        if (state.pendingKickoff) {
            // Rare onside when trailing late
            if (!state.playingOT && trailing && time <= 120 && deficit >= 1 && deficit <= 8
                    && rng.nextDouble() < 0.08) {
                return Playbook.offenseById("onside");
            }
            return Playbook.offenseById("kickoff");
        }

        if (down >= 4) {
            return fourthDownCall(offense, state, yardLine, need, time, trailing, deficit);
        }

        // Victory formation: enough kneels to burn remaining clock
        if (shouldKneel(offense, state, yardLine, down, time, trailing)) {
            return Playbook.offenseById("kneel");
        }

        // Spike to stop clock for FG setup / last-second play
        if (shouldSpike(state, yardLine, down, time, trailing)) {
            return Playbook.offenseById("spike");
        }

        // Late trailing: FG chip shot, hail mary, or force-pass verts
        if (!state.playingOT && trailing) {
            if (time <= 30 && deficit <= 3 && yardLine > 60) {
                return Playbook.offenseById("field_goal");
            }
            if (shouldHailMary(yardLine, need, time, deficit, yardsToGoal)) {
                // Prefer true deep shots; occasional empty verts look
                return rng.nextDouble() < 0.35
                        ? Playbook.offenseById("empty_four_verts")
                        : Playbook.offenseById("gun_four_verts");
            }
            if (time <= 30) {
                return weightedPick(scoreCandidates(offense, state, true),
                        Playbook.offenseById("gun_four_verts"));
            }
        }

        List<Scored> scored = scoreCandidates(offense, state, false);
        return weightedPick(scored, Playbook.defaultOffense());
    }

    private boolean shouldSpike(GameState state, int yardLine, int down, int time, boolean trailing) {
        if (state.playingOT || !trailing || state.pendingTry || state.pendingKickoff) return false;
        if (time > 15 || down > 2 || state.yardsNeed < 1) return false;
        return yardLine >= 55 || time <= 8;
    }

    private boolean shouldHailMary(int yardLine, int need, int time, int deficit, int yardsToGoal) {
        if (time > 25 || yardLine > 70) return false;
        if (deficit >= 4) return true;
        return time <= 12 && need > yardsToGoal;
    }

    private OffenseConcept fourthDownCall(Team offense, GameState state, int yardLine, int need,
                                          int time, boolean trailing, int deficit) {
        double fgMake = fgMakePct(offense, yardLine);
        boolean late = !state.playingOT && time < 300;
        int yardsToGoal = Math.max(1, 100 - yardLine);
        // Goal-to-go / red-zone: conversion distance is capped by the end zone
        int effectiveNeed = Math.min(need, yardsToGoal);

        // Must-score situations: go for it more aggressively
        if (trailing && late && deficit > 3) {
            if (yardLine >= 55 && fgMake >= 0.45 && deficit <= 3) {
                return Playbook.offenseById("field_goal");
            }
            return goForItCall(offense, state, effectiveNeed);
        }

        // Goal-to-go / inside the 10: punch it in on short yardage instead of kicking
        if (yardLine >= 90) {
            if (effectiveNeed <= 3 && rng.nextDouble() < 0.80) {
                return goForItCall(offense, state, effectiveNeed);
            }
            if (fgMake >= 0.45) {
                return Playbook.offenseById("field_goal");
            }
            return goForItCall(offense, state, effectiveNeed);
        }

        // Red zone: prefer go-for-it on short/manageable 4th downs
        if (yardLine >= 80) {
            if (effectiveNeed <= 2 && rng.nextDouble() < 0.75) {
                return goForItCall(offense, state, effectiveNeed);
            }
            if (effectiveNeed <= 4 && rng.nextDouble() < 0.45) {
                return goForItCall(offense, state, effectiveNeed);
            }
            if (fgMake >= 0.52 || yardLine > 68) {
                return Playbook.offenseById("field_goal");
            }
        }

        // Short yardage near midfield: analytics-leaning go
        if (need <= 2 && yardLine >= 45 && yardLine <= 70) {
            if (rng.nextDouble() < 0.55) return Playbook.offenseById("i_dive");
        }

        // FG range (outside red zone / after red-zone go declined)
        if (yardLine > 62 && fgMake >= 0.52) {
            return Playbook.offenseById("field_goal");
        }
        if (yardLine > 68) {
            return Playbook.offenseById("field_goal");
        }

        if (trailing && deficit > 0 && yardLine >= 40 && yardLine <= 55 && rng.nextDouble() < 0.07) {
            return Playbook.offenseById("fake_punt");
        }
        return Playbook.offenseById("punt");
    }

    private OffenseConcept goForItCall(Team offense, GameState state, int need) {
        return weightedPick(scoreCandidates(offense, state, need > 4),
                need <= 3 ? Playbook.offenseById("i_dive") : Playbook.offenseById("gun_slants"));
    }

    private boolean shouldKneel(Team offense, GameState state, int yardLine, int down,
                                int time, boolean trailing) {
        if (state.playingOT || trailing) return false;
        if (yardLine >= 50) return false; // still in scoring territory — don't kneel casually
        // ~40s per kneel including runoff; need enough snaps + clock
        int snapsLeft = Math.max(1, 5 - down);
        int estimatedBurn = snapsLeft * 40;
        if (time > estimatedBurn + 15) return false;
        if (time < 150 && down <= 2) return true;
        return time < 90;
    }

    private double fgMakePct(Team offense, int yardLine) {
        Player k = offense != null ? offense.getK(0) : null;
        if (k == null) return 0.35;
        int distance = 100 - yardLine + 17;
        int kac = k.ratings != null ? k.ratings.kac : 70;
        int kpw = k.ratings != null ? k.ratings.kpw : 70;
        double chance = kac + (kpw - 70) - (distance - 30);
        return Math.max(0.08, Math.min(0.95, chance / 100.0));
    }

    private List<Scored> scoreCandidates(Team offense, GameState state, boolean forcePass) {
        OffensivePhilosophy phil = offense.offPhilosophy != null ? offense.offPhilosophy : OffensivePhilosophy.MULTIPLE;
        List<OffenseConcept> pool = Playbook.situationalOffense(state, false);
        List<Scored> scored = new ArrayList<>();
        boolean trailing = trailing(offense, state);
        for (OffenseConcept c : pool) {
            if (c.family == ConceptFamily.SPECIAL) continue;
            if (forcePass && c.offensePlay != OffensePlay.PASS && c.family != ConceptFamily.RPO) continue;
            double score = 1.0;
            if (c.family == ConceptFamily.PASS || c.family == ConceptFamily.RPO) {
                score += phil.passBias;
                if (state.down == 3 && state.yardsNeed > 4) score += 0.35;
                if (c.depth == DepthBand.DEEP) {
                    score += state.yardsNeed >= 8 ? 0.25 : -0.35;
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
            score += applySituationBias(c, state, trailing);
            if (score < 0.05) score = 0.05;
            scored.add(new Scored(c, score));
        }
        return scored;
    }

    /**
     * Situation tables: red-zone, short-yardage, and 2-minute offense weights.
     */
    private double applySituationBias(OffenseConcept c, GameState state, boolean trailing) {
        if (state.pendingTry) return 0;
        double bias = 0;
        int yardLine = state.yardLine;
        int need = state.yardsNeed;
        int down = state.down;
        int time = state.gameTime;
        boolean redZone = yardLine >= 80;
        boolean shortYardage = need <= 2 && down >= 1 && down <= 3;
        boolean powerId = isPowerShortId(c.id);
        boolean heavyForm = c.formation == CFBsimPack.Formation.I_FORM
                || c.formation == CFBsimPack.Formation.JUMBO;
        boolean heavyPers = "21".equals(c.personnel) || "22".equals(c.personnel);

        if (redZone) {
            if (c.family == ConceptFamily.RUN || heavyForm || heavyPers) {
                bias += 0.55;
                if (yardLine >= 95) bias += 0.25;
            }
            if (c.family == ConceptFamily.PASS || c.family == ConceptFamily.RPO) {
                if (c.depth == DepthBand.SHORT) bias += 0.15;
                if (c.depth == DepthBand.DEEP) bias -= 0.75;
            }
            if (yardLine >= 90 && powerId) bias += 0.35;
        }

        if (shortYardage) {
            if (c.family == ConceptFamily.RUN) bias += 0.45;
            if (powerId) bias += 0.40;
            if (c.depth == DepthBand.DEEP
                    || "gun_four_verts".equals(c.id)
                    || "empty_four_verts".equals(c.id)) {
                bias -= 0.50;
            }
            if (c.depth == DepthBand.MEDIUM && need == 2 && down == 3) {
                bias += 0.10;
            }
        }

        if (!state.playingOT && trailing && time <= 120) {
            if (c.family == ConceptFamily.PASS || c.family == ConceptFamily.RPO) {
                // Final half-minute: prioritize deep shots over the quick game
                if (time <= 30 && c.depth == DepthBand.DEEP) {
                    bias += 0.55;
                } else if (c.depth == DepthBand.SHORT || c.depth == DepthBand.MEDIUM) {
                    bias += 0.35;
                } else if (c.depth == DepthBand.DEEP && time <= 40 && need >= 15) {
                    bias += 0.20;
                }
            } else if (c.family == ConceptFamily.RUN && need > 1) {
                bias -= 0.25;
            }
        }

        return bias;
    }

    private static boolean isPowerShortId(String id) {
        return "jumbo_power".equals(id) || "jumbo_iso".equals(id)
                || "i_dive".equals(id) || "i_power".equals(id) || "i_iso".equals(id);
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
        Collections.sort(scored, (a, b) -> Double.compare(b.score, a.score));
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

    /** Prefer {@link #chooseDefenseConcept(Team, Team, GameState, OffenseConcept)} when offense is known. */
    @Deprecated
    public DefenseConcept chooseDefenseConcept(Team defense, GameState state, OffenseConcept offenseConcept) {
        return chooseDefenseConcept(null, defense, state, offenseConcept);
    }

    public DefenseConcept chooseDefenseConcept(Team offense, Team defense, GameState state,
                                               OffenseConcept offenseConcept) {
        if (state.pendingKickoff) {
            return rng.nextDouble() < 0.2
                    ? Playbook.defenseById("kick_fair_catch")
                    : Playbook.defenseById("kick_return");
        }

        if (state.pendingTry && state.tryIsTwoPoint) {
            return chooseCoverageBySystem(offense, defense, state);
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
            return chooseCoverageBySystem(offense, defense, state);
        }

        return chooseCoverageBySystem(offense, defense, state);
    }

    private static boolean isSpecialTeamsOffense(OffenseConcept c) {
        if (c == null) return false;
        OffensePlay p = c.offensePlay;
        return p == OffensePlay.PUNT || p == OffensePlay.FAKE_PUNT || p == OffensePlay.FIELD_GOAL
                || p == OffensePlay.KICKOFF;
    }

    private DefenseConcept chooseCoverageBySystem(Team offense, Team defense, GameState state) {
        DefensiveSystem sys = defense.defSystem != null ? defense.defSystem : DefensiveSystem.BASE_4_3;
        double roll = rng.nextDouble();

        // Short yardage: pack the box before prevent
        if (state.yardsNeed <= 2 && !state.pendingTry) {
            return Playbook.defenseFor(CoverageCall.STACK_BOX);
        }

        // Late-lead prevent shell (no dedicated PREVENT enum)
        if (!state.playingOT && state.gameTime < 120 && state.yardLine < 75 && !state.pendingTry) {
            boolean defenseLeading = state.possessionHome
                    ? state.awayScore > state.homeScore
                    : state.homeScore > state.awayScore;
            if (defenseLeading && roll < 0.70) {
                if (roll < 0.28) return Playbook.defenseFor(CoverageCall.COVER_2);
                if (roll < 0.50) return Playbook.defenseFor(CoverageCall.COVER_4);
                return Playbook.defenseFor(CoverageCall.OFF_COVERAGE);
            }
        }

        if (state.down >= 3 && state.yardsNeed >= 7) {
            if (sys == DefensiveSystem.DIME || sys == DefensiveSystem.NICKEL || sys == DefensiveSystem.FOUR_TWO_FIVE) {
                return Playbook.defenseFor(rng.nextBoolean() ? CoverageCall.COVER_2 : CoverageCall.COVER_4);
            }
            return Playbook.defenseFor(CoverageCall.COVER_3);
        }

        // Contain dual-threat QBs on early downs
        if (offense != null && state.down <= 2 && !state.pendingTry) {
            Player qb = offense.getQB(0);
            int qbSpd = qb != null && qb.ratings != null ? qb.ratings.spd : 55;
            if (qbSpd >= 78 && rng.nextDouble() < 0.15) {
                return Playbook.defenseFor(CoverageCall.SPY);
            }
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
        if (state.playingOT) return TempoCall.NORMAL;
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
