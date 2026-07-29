package CFBsimPack.engine;

import CFBsimPack.DefensiveSystem;
import CFBsimPack.OnFieldEleven;
import CFBsimPack.Player;
import CFBsimPack.PlayerRatings;
import CFBsimPack.PositionGroup;
import CFBsimPack.PressureResponse;
import CFBsimPack.QbPressurePolicy;
import CFBsimPack.RoleTag;
import CFBsimPack.Team;

import java.util.List;
import java.util.Random;

/**
 * Resolves a single snap given situation + call. Uses on-field elevens and coverage mods.
 */
public final class PlayResolver {

    private final Random rng;
    private PlayerGameStats gameStats;
    private FatigueTracker fatigue;

    public PlayResolver(Random rng) {
        this.rng = rng != null ? rng : new Random();
    }

    public void setGameStats(PlayerGameStats gameStats) {
        this.gameStats = gameStats;
    }

    public void setFatigueTracker(FatigueTracker fatigue) {
        this.fatigue = fatigue;
    }

    public PlayResult resolve(Team home, Team away, GameState state, PlayCall call) {
        Team offense = state.possessionHome ? home : away;
        Team defense = state.possessionHome ? away : home;
        OffenseConcept concept = call.resolvedOffenseConcept();
        String personnel = concept != null ? concept.personnel : null;
        OnFieldEleven offEleven = OnFieldEleven.forOffense(offense, personnel, fatigue);
        OnFieldEleven defEleven = OnFieldEleven.forDefense(defense, fatigue);

        if (call.offensePlay == OffensePlay.SPIKE) {
            return spike(state, offense, call);
        }
        if (call.offensePlay == OffensePlay.KNEEL) {
            return kneel(state, offense, call);
        }
        if (call.offensePlay == OffensePlay.FIELD_GOAL) {
            return fieldGoal(offense, defense, state, call);
        }
        if (call.offensePlay == OffensePlay.PUNT) {
            return punt(offense, defense, state, call);
        }
        if (call.offensePlay == OffensePlay.FAKE_PUNT) {
            return fakePunt(offense, defense, offEleven, defEleven, state, call);
        }
        if (call.offensePlay == OffensePlay.KICKOFF) {
            return kickoff(offense, defense, state, call);
        }
        if (concept != null && concept.family == ConceptFamily.RPO) {
            return rpo(offense, defense, offEleven, defEleven, state, call);
        }
        if (call.offensePlay == OffensePlay.PASS) {
            return pass(offense, defense, offEleven, defEleven, state, call);
        }
        return rush(offense, defense, offEleven, defEleven, state, call, null);
    }

    private PlayResult spike(GameState state, Team offense, PlayCall call) {
        PlayResult r = new PlayResult();
        r.playType = OffensePlay.SPIKE;
        r.clockBurned = 3;
        r.stoppedClock = true;
        r.incomplete = true;
        state.down++;
        r.logLine = offense.abbr + " spikes the ball. Clock stopped. ("
                + call.resolvedOffenseConcept().displayName + ").";
        return r;
    }

    private PlayResult kneel(GameState state, Team offense, PlayCall call) {
        PlayResult r = new PlayResult();
        r.playType = OffensePlay.KNEEL;
        r.yardsGained = -1;
        state.yardLine = Math.max(1, state.yardLine - 1);
        state.down++;
        state.yardsNeed++;
        double tempo = call.tempo.clockMult * (1.0 + call.resolvedOffenseConcept().clockMultExtra);
        r.clockBurned = (int) (35 + 10 * rng.nextDouble() * tempo);
        r.logLine = offense.abbr + " kneels to run clock. ("
                + call.resolvedOffenseConcept().displayName + ").";
        return r;
    }

    private PlayResult rpo(Team offense, Team defense, OnFieldEleven off, OnFieldEleven def,
                           GameState state, PlayCall call) {
        OffenseConcept concept = call.resolvedOffenseConcept();
        Player qb = onFieldQb(off, offense);
        RpoBranch branch = chooseRpoBranch(qb, concept, call.coverage);
        if (branch == RpoBranch.GIVE) {
            Player carrier = pickNonQbCarrier(off, offense, concept);
            if (carrier != null) {
                return rush(offense, defense, off, def, state, call, carrier);
            }
            // No back available — pull and throw
            return pass(offense, defense, off, def, state, call);
        }
        if (branch == RpoBranch.KEEP && qb != null) {
            return rush(offense, defense, off, def, state, call, qb);
        }
        return pass(offense, defense, off, def, state, call);
    }

    /**
     * Weighted give / keep / throw for RPO family. Package-visible for tests.
     * Weights are relative (not forced to sum to 1 before sampling).
     */
    RpoBranch chooseRpoBranch(Player qb, OffenseConcept concept, CoverageCall cov) {
        int spd = rating(qb, x -> x.spd, 55);
        int elu = rating(qb, x -> x.elu, 55);
        double scrambleMod = cov != null ? cov.scrambleMod : 1.0;

        double give = 0.35;
        if (cov == CoverageCall.STACK_BOX) give += 0.18;

        double keep = (0.15 + spd / 400.0 + elu / 500.0) * scrambleMod;
        if (concept != null && "pistol_zone_read".equals(concept.id)) {
            keep += 0.12 * scrambleMod;
        }
        if (keep < 0.02) keep = 0.02;

        // Baseline throw attractiveness; remainder-style floor without collapsing keep
        double throwW = Math.max(0.25, 1.0 - give - keep);

        double total = give + keep + throwW;
        double roll = rng.nextDouble() * total;
        if (roll < give) return RpoBranch.GIVE;
        if (roll < give + keep) return RpoBranch.KEEP;
        return RpoBranch.THROW;
    }

    enum RpoBranch {
        GIVE, KEEP, THROW
    }

    private PlayResult pass(Team offense, Team defense, OnFieldEleven off, OnFieldEleven def,
                            GameState state, PlayCall call) {
        PlayResult r = new PlayResult();
        r.playType = OffensePlay.PASS;
        OffenseConcept concept = call.resolvedOffenseConcept();
        Player qb = onFieldQb(off, offense);
        if (qb == null) {
            return PlayResult.logOnly(offense.abbr + " has no QB on the field.", 5);
        }
        CoverageCall cov = call.coverage;
        DefensiveSystem sys = defense.defSystem != null ? defense.defSystem : DefensiveSystem.BASE_4_3;
        double tempo = call.tempo.clockMult * (1.0 + concept.clockMultExtra);
        boolean rpoThrow = concept != null && concept.family == ConceptFamily.RPO;
        int qbSpd = rating(qb, x -> x.spd, 55);
        int qbElu = rating(qb, x -> x.elu, 55);

        // Block battles: OL pass-block wins vs pass rush feed pressure
        int blockWins = countBlockWins(off, def, true);
        int pressure = (int) ((def.passRushComposite() * 2 - off.olPassComposite() - blockWins * 4)
                * sys.passWeight * concept.sackRiskMod);
        pressure += AtmosphereEngine.roadPressureAdd(state);
        int thv = rating(qb, x -> x.thv, 55);
        double pressureScale = 1.0 - (thv - 50) / 250.0;
        if (rpoThrow) {
            // Mobility softens pocket pressure on RPO peek/alert throws
            pressureScale -= (qbSpd - 50) / 400.0 + (qbElu - 50) / 500.0;
        }
        int effectivePressure = (int) (pressure * pressureScale);
        if (effectivePressure < 0) effectivePressure = 0;
        if (rng.nextDouble() * 100 < effectivePressure / 8.0) {
            return resolvePressure(offense, defense, state, call, r, qb, concept, cov,
                    tempo, rpoThrow, qbSpd, qbElu, thv);
        }

        int yardsToGoal = Math.max(1, 100 - state.yardLine);
        double intChance = (pressure + def.coverageComposite()
                - (rating(qb, x -> x.tha, 55) + qb.ratFootIQ + thv + 100) / 4.0) / 18.0;
        intChance *= cov.intMod;
        if (concept.depth == DepthBand.DEEP) {
            intChance *= 1.12;
            if (state.gameTime <= 40 && yardsToGoal >= 35) {
                intChance *= 1.25;
            }
        }
        if (intChance < 0.015) intChance = 0.015;
        if (100 * rng.nextDouble() < intChance) {
            r.turnover = true;
            r.possessionChanged = true;
            r.stoppedClock = true;
            r.clockBurned = (int) (8 + 10 * rng.nextDouble() * tempo);
            qb.seasonStats.passInt++;
            qb.seasonStats.passAtt++;
            if (gameStats != null) {
                PlayerGameStats.Line line = gameStats.line(qb);
                line.passAtt++;
                line.passInt++;
            }
            r.logLine = "INTERCEPTION! " + offense.abbr + " QB " + qb.name + " intercepted ("
                    + concept.displayName + ").";
            return r;
        }

        Player target = pickReceiver(off, offense, concept.targetBias);
        Player cb = onFieldCb(def, defense);
        int cat = recvCatch(target);
        int spd = recvSpeed(target);
        int rtr = rating(target, x -> x.rtr, 60);
        int cbCov = cb != null ? rating(cb, x -> x.pcv, 70) : 70;
        int cbSpd = cb != null ? rating(cb, x -> x.spd, 70) : 70;

        r.passArriveYardLine = passArriveYardLine(state.yardLine, concept.depth);

        double completion = (normalize(rating(qb, x -> x.tha, 55)) + normalize(cat) - normalize(cbCov)) / 2.0
                + 18.25 - pressure / 16.8 + AtmosphereEngine.offenseBonus(state)
                + normalize(rtr) / 4.0;
        if (rpoThrow && pressure > 0) {
            // Capped mobility bump vs pressure on RPO throws
            double mobBump = Math.min(4.0, (qbElu + thv - 100) / 25.0);
            if (mobBump > 0) completion += mobBump;
        }
        completion *= cov.completionMod * concept.completionMod;
        completion += cov.passFitBonus();
        completion += concept.matchupBonus(cov, state);

        qb.seasonStats.passAtt++;
        if (gameStats != null) gameStats.line(qb).passAtt++;
        if (isPos(target, PositionGroup.WR) || isPos(target, PositionGroup.TE)) {
            target.seasonStats.targets++;
        }

        if (100 * rng.nextDouble() >= completion) {
            r.incomplete = true;
            r.stoppedClock = true;
            r.clockBurned = (int) (6 + 10 * rng.nextDouble() * tempo);
            state.down++;
            r.logLine = offense.abbr + " incomplete pass" + (target != null ? " intended for " + target.name : "")
                    + " (" + concept.displayName + " · " + call.formation.displayName + ").";
            return r;
        }

        if (100 * rng.nextDouble() < (100 - cat) / 3.0) {
            r.incomplete = true;
            r.stoppedClock = true;
            r.clockBurned = (int) (6 + 8 * rng.nextDouble() * tempo);
            state.down++;
            if (isPos(target, PositionGroup.WR) || isPos(target, PositionGroup.TE)) {
                target.seasonStats.drops++;
                if (gameStats != null) gameStats.line(target).drops++;
            }
            r.logLine = "Drop! " + (target != null ? target.name : "Receiver") + " couldn't hang on ("
                    + concept.displayName + ").";
            return r;
        }

        int yards = (int) ((normalize(rating(qb, x -> x.thp, 55)) + normalize(spd) - normalize(cbSpd)) * rng.nextDouble() / 3.7
                * cov.yardsMod * concept.yardsMod);
        if (concept.depth == DepthBand.DEEP) {
            int deepBonus = (int) (4 + 8 * rng.nextDouble());
            deepBonus = Math.min(deepBonus, Math.max(0, yardsToGoal - 1));
            yards += deepBonus;
        } else if (concept.depth == DepthBand.SHORT) {
            yards = (int) (yards * 0.85);
        }
        if (yards < 0) yards = 0;

        Player s = onFieldS(def, defense);
        double escape = (normalize(recvEva(target)) * 3 + normalize(rtr)
                - (cb != null ? rating(cb, x -> x.tck, 70) : 70)
                - (s != null ? s.ratOvr : 70)) * rng.nextDouble();
        if (escape > 92 || rng.nextDouble() > 0.95) {
            yards += (int) (3 + spd * rng.nextDouble() / 3);
        }

        return applyGain(offense, defense, state, call, r, qb, target, yards, true);
    }

    private PlayResult rush(Team offense, Team defense, OnFieldEleven off, OnFieldEleven def,
                            GameState state, PlayCall call, Player forcedCarrier) {
        PlayResult r = new PlayResult();
        r.playType = OffensePlay.RUN;
        OffenseConcept concept = call.resolvedOffenseConcept();
        Player carrier = forcedCarrier != null ? forcedCarrier : pickCarrier(off, offense, concept);
        if (carrier == null) {
            return pass(offense, defense, off, def, state, call);
        }
        CoverageCall cov = call.coverage;
        DefensiveSystem sys = defense.defSystem != null ? defense.defSystem : DefensiveSystem.BASE_4_3;

        int blockWins = countBlockWins(off, def, false);
        int blockAdv = (int) ((off.olRushComposite() - def.runStopComposite() * sys.runWeight)
                + blockWins * 3 + cov.runFitBonus() + concept.matchupBonus(cov, state));
        int rushSpd = carrierRushSpd(carrier);
        int rushPow = carrierRushPow(carrier);
        int rushEva = carrierRushEva(carrier);
        int yards = (int) ((rushSpd + blockAdv + AtmosphereEngine.offenseBonus(state))
                * rng.nextDouble() / 10.0 * concept.runYardsMod);
        if (yards < 2) {
            yards += rushPow / 20 - 3;
        } else if (rng.nextDouble() < 0.28) {
            yards += (int) (rushEva / 5.0 * rng.nextDouble());
        }
        if (isPos(carrier, PositionGroup.QB)) {
            yards = (int) (yards * cov.scrambleMod);
        }

        return applyGain(offense, defense, state, call, r, null, carrier, yards, false, null);
    }

    private PlayResult applyGain(Team offense, Team defense, GameState state, PlayCall call, PlayResult r,
                                 Player qb, Player ballCarrier, int yards, boolean wasPass) {
        return applyGain(offense, defense, state, call, r, qb, ballCarrier, yards, wasPass, null);
    }

    private PlayResult applyGain(Team offense, Team defense, GameState state, PlayCall call, PlayResult r,
                                 Player qb, Player ballCarrier, int yards, boolean wasPass,
                                 ScrambleIntent scramble) {
        OffenseConcept concept = call.resolvedOffenseConcept();
        double tempo = call.tempo.clockMult * (1.0 + concept.clockMultExtra);
        if (yards < -5) yards = -5;
        state.yardLine += yards;
        r.yardsGained = yards;

        if (state.yardLine >= 100) {
            int actual = yards - (state.yardLine - 100);
            r.yardsGained = actual;
            state.yardLine = 100;
            r.touchdown = true;
            r.stoppedClock = true;
            r.clockBurned = (int) (8 + 12 * rng.nextDouble() * tempo);
            if (state.pendingTry && state.tryIsTwoPoint) {
                r.logLine = "2-POINT CONVERSION GOOD! " + offense.abbr
                        + (ballCarrier != null ? " " + ballCarrier.name : "")
                        + " (" + concept.displayName + " · " + call.resolvedDefenseConcept().displayName + ").";
            } else {
                creditTd(offense, state, qb, ballCarrier, actual, wasPass);
                if (scramble != null) {
                    r.logLine = "SCRAMBLE TD! " + offense.abbr + " "
                            + (ballCarrier != null ? ballCarrier.name : "") + " " + actual + " yards"
                            + " (" + concept.displayName + " · " + call.resolvedDefenseConcept().displayName + ").";
                } else {
                    r.logLine = (wasPass ? "PASS TD! " : "RUSH TD! ") + offense.abbr + " "
                            + (ballCarrier != null ? ballCarrier.name : "") + " " + actual + " yards"
                            + " (" + concept.displayName + " · " + call.resolvedDefenseConcept().displayName + ").";
                }
            }
            return r;
        }

        state.yardsNeed -= yards;
        if (state.yardsNeed <= 0) {
            state.down = 1;
            state.yardsNeed = 10;
            r.firstDown = true;
        } else {
            state.down++;
        }

        creditYards(offense, state, qb, ballCarrier, yards, wasPass);

        // Fumble check — ball security lowers forced-fumble chance
        Player safety = defense.getS(0);
        int sTkl = safety != null ? rating(safety, x -> x.tck, 70) : 70;
        double fum = isPos(ballCarrier, PositionGroup.RB)
                ? (sTkl + defRun(defense)) / 2.0
                : sTkl / 2.0;
        fum *= concept.fumbleMod;
        int bsc = rating(ballCarrier, x -> x.bsc, 70);
        double bscScale = (120.0 - bsc) / 50.0;
        if (bscScale < 0.2) bscScale = 0.2;
        fum *= bscScale;
        if (scramble == ScrambleIntent.PROTECT_BALL) {
            fum *= 0.55;
        }
        if (100 * rng.nextDouble() < fum / 50.0) {
            r.turnover = true;
            r.possessionChanged = true;
            r.clockBurned = (int) (12 + 10 * rng.nextDouble() * tempo);
            if (isPos(ballCarrier, PositionGroup.RB) || isPos(ballCarrier, PositionGroup.QB)
                    || isPos(ballCarrier, PositionGroup.FB)) {
                ballCarrier.seasonStats.fumbles++;
                if (gameStats != null) gameStats.line(ballCarrier).fumbles++;
            }
            if (isPos(ballCarrier, PositionGroup.WR) || isPos(ballCarrier, PositionGroup.TE)) {
                ballCarrier.seasonStats.recFumbles++;
            }
            r.logLine = "FUMBLE! " + offense.abbr + " " + (ballCarrier != null ? ballCarrier.name : "")
                    + " lost it (" + concept.displayName + ").";
            return r;
        }

        if (scramble != null) {
            boolean stopClock = scramble == ScrambleIntent.FORCE_OOB
                    || (scramble == ScrambleIntent.TAKE_WHAT_YOU_NEED && r.firstDown)
                    || (scramble == ScrambleIntent.PROTECT_BALL && rng.nextDouble() < 0.35);
            if (scramble == ScrambleIntent.PROTECT_BALL) {
                r.clockBurned = (int) ((30 + 12 * rng.nextDouble()) * tempo);
            } else if (stopClock) {
                r.clockBurned = (int) ((12 + 10 * rng.nextDouble()) * tempo);
            } else {
                r.clockBurned = (int) ((22 + 14 * rng.nextDouble()) * tempo);
            }
            r.stoppedClock = stopClock;
            r.logLine = "SCRAMBLE! " + offense.abbr + " " + (ballCarrier != null ? ballCarrier.name : "QB")
                    + " for " + yards + " yards (" + concept.displayName + " · "
                    + call.formation.displayName + ").";
            return r;
        }

        r.clockBurned = (int) ((wasPass ? (15 + 15 * rng.nextDouble()) : (25 + 15 * rng.nextDouble())) * tempo);
        r.stoppedClock = false;
        r.logLine = offense.abbr + (wasPass ? " pass " : " rush ") + yards + " yards to "
                + (ballCarrier != null ? ballCarrier.name : "ballcarrier")
                + " (" + concept.displayName + " · " + call.formation.displayName + ").";
        return r;
    }

    private PlayResult resolvePressure(Team offense, Team defense, GameState state, PlayCall call,
                                       PlayResult r, Player qb, OffenseConcept concept, CoverageCall cov,
                                       double tempo, boolean rpoThrow, int qbSpd, int qbElu, int thv) {
        QbPressurePolicy.Slot bucket = classifyPressureBucket(offense, state);
        PressureResponse pref = preferenceForBucket(offense, bucket);
        ScrambleIntent intent = intentFor(pref, bucket, qbSpd, state);

        if (intent == ScrambleIntent.THROW_AWAY) {
            return throwAway(offense, state, call, qb, tempo);
        }

        double escapeChance = (0.22 + qbSpd / 500.0 + (thv - 50) / 400.0) * cov.scrambleMod;
        if (rpoThrow) {
            escapeChance += qbElu / 800.0;
        }
        escapeChance *= intentEscapeMult(intent);
        if (escapeChance < 0.05) escapeChance = 0.05;
        if (escapeChance > 0.92) escapeChance = 0.92;

        if (rng.nextDouble() < escapeChance) {
            int scrambleYds = sampleScrambleYards(intent, state, qbSpd, qbElu, cov, rpoThrow);
            return applyGain(offense, defense, state, call, r, null, qb, scrambleYds, false, intent);
        }

        double throwChance = secondaryThrowawayChance(intent, bucket, pref, thv, qb);
        if (rng.nextDouble() < throwChance) {
            return throwAway(offense, state, call, qb, tempo);
        }
        return sack(offense, state, call, qb);
    }

    private PlayResult throwAway(Team offense, GameState state, PlayCall call, Player qb, double tempo) {
        PlayResult r = new PlayResult();
        OffenseConcept concept = call.resolvedOffenseConcept();
        r.playType = OffensePlay.PASS;
        r.incomplete = true;
        r.throwaway = true;
        r.stoppedClock = true;
        r.yardsGained = 0;
        r.clockBurned = (int) (4 + 5 * rng.nextDouble() * tempo);
        state.down++;
        if (qb != null) {
            qb.seasonStats.passAtt++;
            if (gameStats != null) gameStats.line(qb).passAtt++;
        }
        r.logLine = "THROW AWAY! " + offense.abbr + " QB "
                + (qb != null ? qb.name : "") + " gets rid of it ("
                + concept.displayName + ").";
        return r;
    }

    QbPressurePolicy.Slot classifyPressureBucket(Team offense, GameState state) {
        if (state.yardLine <= 10) {
            return QbPressurePolicy.Slot.BACKED_UP;
        }
        if (!state.playingOT && state.gameTime >= 0 && state.gameTime <= 120) {
            if (offenseLeading(offense, state)) {
                return QbPressurePolicy.Slot.PROTECT_LEAD;
            }
            return QbPressurePolicy.Slot.LATE_TRAILING;
        }
        if (state.down >= 3 && state.yardsNeed <= 7) {
            return QbPressurePolicy.Slot.CONVERT;
        }
        return QbPressurePolicy.Slot.NORMAL;
    }

    private PressureResponse preferenceForBucket(Team offense, QbPressurePolicy.Slot bucket) {
        QbPressurePolicy policy = offense != null && offense.qbPressurePolicy != null
                ? offense.qbPressurePolicy
                : QbPressurePolicy.defaults();
        return policy.forSlot(bucket);
    }

    private ScrambleIntent intentFor(PressureResponse pref, QbPressurePolicy.Slot bucket,
                                     int qbSpd, GameState state) {
        PressureResponse resolved = pref;
        if (pref == null || pref == PressureResponse.AUTO) {
            resolved = autoResponseForBucket(bucket, qbSpd, state);
        }
        switch (resolved) {
            case SCRAMBLE_FOR_IT:
                return ScrambleIntent.MAXIMIZE;
            case TAKE_THE_FIRST_DOWN:
                return ScrambleIntent.TAKE_WHAT_YOU_NEED;
            case SLIDE_SECURE:
                return ScrambleIntent.PROTECT_BALL;
            case THROW_IT_AWAY:
                return ScrambleIntent.THROW_AWAY;
            case FORCE_SIDELINE:
                return ScrambleIntent.FORCE_OOB;
            case AUTO:
            default:
                return ScrambleIntent.TAKE_WHAT_YOU_NEED;
        }
    }

    private PressureResponse autoResponseForBucket(QbPressurePolicy.Slot bucket, int qbSpd,
                                                   GameState state) {
        switch (bucket) {
            case BACKED_UP:
                return PressureResponse.THROW_IT_AWAY;
            case PROTECT_LEAD:
                return PressureResponse.SLIDE_SECURE;
            case LATE_TRAILING:
                return PressureResponse.SCRAMBLE_FOR_IT;
            case CONVERT:
                return PressureResponse.TAKE_THE_FIRST_DOWN;
            case NORMAL:
            default:
                if (qbSpd >= 78 && state.yardsNeed > 5) {
                    return PressureResponse.SCRAMBLE_FOR_IT;
                }
                return PressureResponse.TAKE_THE_FIRST_DOWN;
        }
    }

    private double intentEscapeMult(ScrambleIntent intent) {
        switch (intent) {
            case MAXIMIZE:
                return 1.12;
            case FORCE_OOB:
                return 1.08;
            case TAKE_WHAT_YOU_NEED:
                return 1.05;
            case PROTECT_BALL:
                return 0.90;
            case THROW_AWAY:
            default:
                return 1.0;
        }
    }

    private int sampleScrambleYards(ScrambleIntent intent, GameState state, int qbSpd, int qbElu,
                                    CoverageCall cov, boolean rpoThrow) {
        double scrambleScale = cov != null ? cov.scrambleMod : 1.0;
        if (rpoThrow) scrambleScale *= 1.0 + qbElu / 400.0;
        int raw = (int) ((qbSpd + qbElu) / 18.0 * rng.nextDouble() * scrambleScale);
        if (raw < 1) raw = 1;
        int yardsToGoal = Math.max(1, 100 - state.yardLine);
        int need = Math.max(1, state.yardsNeed);

        switch (intent) {
            case PROTECT_BALL: {
                int y = Math.min(raw, 1 + rng.nextInt(3));
                return Math.min(y, yardsToGoal);
            }
            case TAKE_WHAT_YOU_NEED: {
                int target = need + rng.nextInt(3);
                int y = Math.min(raw + need / 3, target);
                if (y < 1) y = 1;
                return Math.min(y, yardsToGoal);
            }
            case FORCE_OOB: {
                int y = Math.max(1, Math.min(raw, 2 + need / 2 + rng.nextInt(4)));
                return Math.min(y, yardsToGoal);
            }
            case MAXIMIZE:
            default: {
                int bonus = (int) (rng.nextDouble() * (qbSpd / 25.0));
                return Math.min(raw + bonus, yardsToGoal);
            }
        }
    }

    private double secondaryThrowawayChance(ScrambleIntent intent, QbPressurePolicy.Slot bucket,
                                            PressureResponse pref, int thv, Player qb) {
        double chance = 0.08 + Math.max(0, thv - 50) / 400.0;
        int iq = qb != null ? qb.ratFootIQ : 70;
        chance += Math.max(0, iq - 70) / 500.0;
        if (bucket == QbPressurePolicy.Slot.BACKED_UP) chance += 0.22;
        if (pref == PressureResponse.THROW_IT_AWAY) chance += 0.18;
        if (intent == ScrambleIntent.PROTECT_BALL) chance += 0.06;
        if (chance > 0.55) chance = 0.55;
        return chance;
    }

    private static boolean offenseLeading(Team offense, GameState state) {
        if (state.possessionHome) {
            return state.homeScore > state.awayScore;
        }
        return state.awayScore > state.homeScore;
    }

    enum ScrambleIntent {
        THROW_AWAY,
        TAKE_WHAT_YOU_NEED,
        MAXIMIZE,
        PROTECT_BALL,
        FORCE_OOB
    }

    private PlayResult sack(Team offense, GameState state, PlayCall call, Player qb) {
        PlayResult r = new PlayResult();
        OffenseConcept concept = call.resolvedOffenseConcept();
        double tempo = call.tempo.clockMult * (1.0 + concept.clockMultExtra);
        r.playType = OffensePlay.PASS;
        r.sack = true;
        r.yardsGained = -3;
        state.yardLine -= 3;
        state.yardsNeed += 3;
        state.down++;
        qb.seasonStats.sacked++;
        if (gameStats != null) gameStats.line(qb).sacks++;
        r.clockBurned = (int) (20 + 10 * rng.nextDouble() * tempo);
        if (state.yardLine < 0) {
            r.safety = true;
            r.possessionChanged = true;
            r.logLine = "SAFETY! " + offense.abbr + " sacked in the end zone ("
                    + concept.displayName + ").";
            state.yardLine = 0;
            return r;
        }
        r.logLine = "SACK! " + offense.abbr + " QB " + qb.name + " taken down ("
                + concept.displayName + ").";
        return r;
    }

    private PlayResult fieldGoal(Team offense, Team defense, GameState state, PlayCall call) {
        PlayResult r = new PlayResult();
        r.playType = OffensePlay.FIELD_GOAL;
        Player k = offense.getK(0);
        int distance = 100 - state.yardLine + 17;
        double chance = rating(k, x -> x.kac, 70) + (rating(k, x -> x.kpw, 70) - 70) - (distance - 30);
        r.clockBurned = (int) (5 + 5 * rng.nextDouble());
        r.stoppedClock = true;
        k.seasonStats.fgAtt++;
        if (gameStats != null) gameStats.line(k).fgAtt++;
        if (100 * rng.nextDouble() < Math.max(8, Math.min(95, chance))) {
            r.scoreFg = true;
            r.possessionChanged = true;
            k.seasonStats.fgMade++;
            if (gameStats != null) gameStats.line(k).fgMade++;
            r.logLine = offense.abbr + " K " + k.name + " hits from " + distance
                    + " (" + call.resolvedOffenseConcept().displayName + ").";
        } else {
            r.possessionChanged = true;
            r.logLine = offense.abbr + " K " + k.name + " misses from " + distance
                    + " (" + call.resolvedOffenseConcept().displayName + ").";
        }
        return r;
    }

    private PlayResult punt(Team offense, Team defense, GameState state, PlayCall call) {
        PlayResult r = new PlayResult();
        r.playType = OffensePlay.PUNT;
        r.clockBurned = (int) (5 + 10 * rng.nextDouble());
        r.stoppedClock = true;
        Player punter = offense.getPunter(0);
        String defId = call.resolvedDefenseConcept().id;

        // Block attempt before the kick
        if ("punt_block".equals(defId)) {
            double blockChance = 0.08 + (defense.getEDGE(0) != null ? defense.getEDGE(0).ratOvr : 70) / 800.0;
            Player ls = offense.getLongSnapper();
            if (ls != null) blockChance -= ls.ratOvr / 1200.0;
            int fum = 100 - rating(punter, x -> x.bsc, 50);
            blockChance -= fum / 2000.0;
            if (rng.nextDouble() < Math.max(0.03, Math.min(0.22, blockChance))) {
                r.puntBlocked = true;
                r.possessionChanged = true;
                r.turnover = true;
                state.yardLine = Math.max(1, state.yardLine - 10);
                if (gameStats != null) {
                    // credit block loosely on EDGE
                    if (defense.getEDGE(0) != null) gameStats.line(defense.getEDGE(0)).puntBlk++;
                }
                r.logLine = "PUNT BLOCKED! " + defense.abbr + " recovers ("
                        + call.resolvedDefenseConcept().displayName + ").";
                return r;
            }
        }

        int power = 70;
        if (isPos(punter, PositionGroup.P)) {
            power = rating(punter, x -> x.ppw, 70);
        } else if (isPos(punter, PositionGroup.K)) {
            power = rating(punter, x -> x.kpw, 70);
        } else if (punter != null) {
            power = rating(punter, x -> x.ppw, 70);
        }
        int puntYards = 32 + (int) ((power - 50) * 0.35) + (int) (18 * rng.nextDouble());
        if (puntYards < 28) puntYards = 28;
        if (puntYards > 62) puntYards = 62;
        if (isPos(punter, PositionGroup.P)) {
            punter.seasonStats.puntAtt++;
            punter.seasonStats.puntYards += puntYards;
            if (gameStats != null) {
                gameStats.line(punter).puntAtt++;
                gameStats.line(punter).puntYards += puntYards;
            }
        } else if (punter != null && gameStats != null) {
            gameStats.line(punter).puntAtt++;
            gameStats.line(punter).puntYards += puntYards;
        }

        int landing = state.yardLine + puntYards;
        if (landing >= 100) {
            // Touchback-ish: receiving team at own 20 after flip → spot at 80 from kicker view
            state.yardLine = 80;
            r.touchback = true;
            r.possessionChanged = true;
            r.logLine = offense.abbr + " punts " + puntYards + " yards into the end zone (touchback).";
            return r;
        }

        state.yardLine = landing;
        Player returner = defense.getPuntReturner();
        String retName = returner != null ? returner.name : "returner";

        if ("fair_catch".equals(defId) || "defend_scrimmage".equals(defId)) {
            r.fairCatch = true;
            r.possessionChanged = true;
            if (returner != null) {
                returner.statsFairCatches++;
                if (gameStats != null) gameStats.line(returner).fairCatches++;
            }
            r.returnerName = retName;
            r.logLine = offense.abbr + " punts " + puntYards + " yards; " + retName + " fair catches.";
            return r;
        }

        // Return (punt_return, punt_block miss, or coverage shell used as soft return)
        int gunnerCov = 0;
        if (offense.getGunner1() != null) gunnerCov += Team.returnSkill(offense.getGunner1()) / 4;
        if (offense.getGunner2() != null) gunnerCov += Team.returnSkill(offense.getGunner2()) / 4;
        int retSkill = Team.returnSkill(returner);
        // Muff recovered by coverage — kicking team keeps the ball at the landing spot
        if (rng.nextDouble() < 0.03) {
            state.down = 1;
            state.yardsNeed = 10;
            r.yardsGained = puntYards;
            r.possessionChanged = false;
            r.turnover = false;
            r.logLine = retName + " muffs the punt! " + offense.abbr + " recovers.";
            return r;
        }

        int returnYards = (int) (8 + (retSkill - gunnerCov) / 25.0 + 18 * rng.nextDouble());
        if ("punt_block".equals(defId)) returnYards = Math.max(0, returnYards / 2); // poor return setup
        if (returnYards < 0) returnYards = 0;
        if (returnYards > 75) returnYards = 75;

        int endSpot = landing - returnYards;
        if (endSpot <= 0) {
            // Return TD — ball crosses kicking team's goal
            state.yardLine = 100; // will score for receiving team after flip logic in applyResult
            r.returnTd = true;
            r.touchdown = true;
            r.possessionChanged = true;
            r.returnYards = returnYards;
            r.returnerName = retName;
            creditReturn(returner, returnYards, true, true);
            r.logLine = offense.abbr + " punts " + puntYards + " yards; " + retName
                    + " returns " + returnYards + " for a TOUCHDOWN!";
            // Possession must be receiving team for TD scoring — flip first in applyResult via touchdown
            // Problem: touchdown scores for current possession (kickers). Need to flip before score.
            // Mark returnTd and possessionChanged; Game.applyResult will handle return TD specially.
            r.touchdown = true;
            return r;
        }
        state.yardLine = Math.max(1, endSpot);
        r.returnYards = returnYards;
        r.returnerName = retName;
        r.possessionChanged = true;
        creditReturn(returner, returnYards, true, false);
        r.logLine = offense.abbr + " punts " + puntYards + " yards; " + retName
                + " returns " + returnYards + " (" + call.resolvedDefenseConcept().displayName + ").";
        return r;
    }

    private PlayResult kickoff(Team offense, Team defense, GameState state, PlayCall call) {
        PlayResult r = new PlayResult();
        r.playType = OffensePlay.KICKOFF;
        r.clockBurned = (int) (4 + 6 * rng.nextDouble());
        r.stoppedClock = true;
        Player k = offense.getK(0);
        int power = k != null ? rating(k, x -> x.kpw, 70) : 70;
        boolean onside = call.resolvedOffenseConcept() != null
                && "onside".equals(call.resolvedOffenseConcept().id);
        if (onside) {
            // Short kick; rare recovery (~12–18%)
            boolean recovered = rng.nextDouble() < 0.14 + (power - 70) / 400.0;
            state.yardLine = Math.min(55, state.yardLine + 10 + (int) (8 * rng.nextDouble()));
            state.down = 1;
            state.yardsNeed = 10;
            r.possessionChanged = !recovered;
            r.logLine = recovered
                    ? offense.abbr + " recovers the onside kick!"
                    : offense.abbr + " onside kick fails; " + defense.abbr + " takes over.";
            return r;
        }
        // Kick from ~35; distance 50–75 into field
        int kickYards = 55 + (int) ((power - 60) * 0.25) + (int) (15 * rng.nextDouble());
        int landing = state.yardLine + kickYards;
        String defId = call.resolvedDefenseConcept().id;
        Player returner = defense.getKickReturner();
        String retName = returner != null ? returner.name : "returner";

        if (landing >= 100 || "kick_fair_catch".equals(defId) && landing >= 90) {
            state.yardLine = 75; // flip → receiving at 25
            r.touchback = true;
            r.possessionChanged = true;
            r.logLine = offense.abbr + " kickoff — touchback.";
            return r;
        }
        if (landing > 99) landing = 99;
        state.yardLine = landing;

        if ("kick_fair_catch".equals(defId)) {
            r.fairCatch = true;
            r.possessionChanged = true;
            if (returner != null) {
                returner.statsFairCatches++;
                if (gameStats != null) gameStats.line(returner).fairCatches++;
            }
            r.returnerName = retName;
            r.logLine = offense.abbr + " kickoff; " + retName + " fair catches.";
            return r;
        }

        int retSkill = Team.returnSkill(returner);
        int returnYards = (int) (15 + retSkill / 30.0 + 20 * rng.nextDouble());
        if (returnYards > 95) returnYards = 95;
        int endSpot = landing - returnYards;
        if (endSpot <= 0) {
            state.yardLine = 100;
            r.returnTd = true;
            r.touchdown = true;
            r.possessionChanged = true;
            r.returnYards = returnYards;
            r.returnerName = retName;
            creditReturn(returner, returnYards, false, true);
            r.logLine = offense.abbr + " kickoff; " + retName + " returns " + returnYards
                    + " for a TOUCHDOWN!";
            return r;
        }
        state.yardLine = Math.max(1, endSpot);
        r.returnYards = returnYards;
        r.returnerName = retName;
        r.possessionChanged = true;
        creditReturn(returner, returnYards, false, false);
        r.logLine = offense.abbr + " kickoff; " + retName + " returns " + returnYards + ".";
        return r;
    }

    private PlayResult fakePunt(Team offense, Team defense, OnFieldEleven off, OnFieldEleven def,
                                GameState state, PlayCall call) {
        PlayResult r = new PlayResult();
        r.playType = OffensePlay.FAKE_PUNT;
        String defId = call.resolvedDefenseConcept().id;
        boolean blockLook = "punt_block".equals(defId);
        boolean defend = "defend_scrimmage".equals(defId) || !Playbook.isSpecialTeamsDefense(call.resolvedDefenseConcept());
        // Fake as a short run with elevated risk
        double stopBias = blockLook ? 0.35 : (defend ? 0.25 : 0.05);
        int yards = (int) (2 + 12 * rng.nextDouble() - stopBias * 10);
        if (rng.nextDouble() < 0.12 + stopBias) {
            yards = -2 - (int) (6 * rng.nextDouble());
        }
        r.yardsGained = yards;
        r.clockBurned = (int) (6 + 8 * rng.nextDouble());
        state.yardLine += yards;
        if (state.yardLine >= 100) {
            r.touchdown = true;
            state.yardLine = 100;
            r.logLine = offense.abbr + " fake punt — TOUCHDOWN!";
            return r;
        }
        if (state.yardLine <= 0) {
            r.safety = true;
            state.yardLine = 1;
            r.logLine = offense.abbr + " fake punt stuffed for a SAFETY!";
            return r;
        }
        if (yards >= state.yardsNeed) {
            r.firstDown = true;
            state.down = 1;
            state.yardsNeed = 10;
            r.logLine = offense.abbr + " fake punt works for " + yards + " and a FIRST DOWN!";
        } else {
            state.down++;
            state.yardsNeed -= yards;
            if (state.yardsNeed < 1) state.yardsNeed = 1;
            r.logLine = offense.abbr + " fake punt gains " + yards
                    + (blockLook || defend ? " (defense was ready)." : ".");
        }
        return r;
    }

    private void creditReturn(Player returner, int yards, boolean puntReturn, boolean td) {
        if (returner == null) return;
        if (puntReturn) {
            returner.statsPrAtt++;
            returner.statsPrYards += yards;
            if (td) returner.statsPrTd++;
            if (gameStats != null) {
                PlayerGameStats.Line line = gameStats.line(returner);
                line.prAtt++;
                line.prYards += yards;
                if (td) line.prTd++;
            }
        } else {
            returner.statsKrAtt++;
            returner.statsKrYards += yards;
            if (td) returner.statsKrTd++;
            if (gameStats != null) {
                PlayerGameStats.Line line = gameStats.line(returner);
                line.krAtt++;
                line.krYards += yards;
                if (td) line.krTd++;
            }
        }
    }

    private void creditTd(Team offense, GameState state, Player qb, Player ballCarrier, int yards, boolean wasPass) {
        if (wasPass && qb != null) {
            qb.seasonStats.passTd++;
            qb.seasonStats.passComp++;
            qb.seasonStats.passYards += yards;
            offense.teamPassYards += yards;
            if (gameStats != null) {
                PlayerGameStats.Line line = gameStats.line(qb);
                line.passComp++;
                line.passYards += yards;
                line.passTd++;
            }
        }
        if (isPos(ballCarrier, PositionGroup.WR) || isPos(ballCarrier, PositionGroup.TE)) {
            ballCarrier.seasonStats.recTd++;
            ballCarrier.seasonStats.receptions++;
            ballCarrier.seasonStats.recYards += yards;
            if (gameStats != null) {
                PlayerGameStats.Line line = gameStats.line(ballCarrier);
                line.receptions++;
                line.recYards += yards;
                line.recTd++;
            }
        } else if (isPos(ballCarrier, PositionGroup.RB)) {
            ballCarrier.seasonStats.rushTd++;
            if (wasPass) {
                if (gameStats != null) {
                    PlayerGameStats.Line line = gameStats.line(ballCarrier);
                    line.receptions++;
                    line.recYards += yards;
                    line.recTd++;
                }
            } else {
                ballCarrier.seasonStats.rushAtt++;
                ballCarrier.seasonStats.rushYards += yards;
                offense.teamRushYards += yards;
                if (gameStats != null) {
                    PlayerGameStats.Line line = gameStats.line(ballCarrier);
                    line.rushAtt++;
                    line.rushYards += yards;
                    line.rushTd++;
                }
            }
        } else if (isPos(ballCarrier, PositionGroup.QB) && !wasPass) {
            ballCarrier.seasonStats.rushAtt++;
            ballCarrier.seasonStats.rushYards += yards;
            ballCarrier.seasonStats.rushTd++;
            offense.teamRushYards += yards;
            if (gameStats != null) {
                PlayerGameStats.Line line = gameStats.line(ballCarrier);
                line.rushAtt++;
                line.rushYards += yards;
                line.rushTd++;
            }
        } else if (isPos(ballCarrier, PositionGroup.FB) && !wasPass) {
            ballCarrier.seasonStats.rushAtt++;
            ballCarrier.seasonStats.rushYards += yards;
            ballCarrier.seasonStats.rushTd++;
            offense.teamRushYards += yards;
            if (gameStats != null) {
                PlayerGameStats.Line line = gameStats.line(ballCarrier);
                line.rushAtt++;
                line.rushYards += yards;
                line.rushTd++;
            }
        }
        if (state.possessionHome) state.homeYards += yards;
        else state.awayYards += yards;
    }

    private void creditYards(Team offense, GameState state, Player qb, Player ballCarrier, int yards, boolean wasPass) {
        if (wasPass && qb != null) {
            qb.seasonStats.passComp++;
            qb.seasonStats.passYards += yards;
            offense.teamPassYards += yards;
            if (gameStats != null) {
                PlayerGameStats.Line line = gameStats.line(qb);
                line.passComp++;
                line.passYards += yards;
            }
        }
        if (isPos(ballCarrier, PositionGroup.WR) || isPos(ballCarrier, PositionGroup.TE)) {
            ballCarrier.seasonStats.receptions++;
            ballCarrier.seasonStats.recYards += yards;
            if (gameStats != null) {
                PlayerGameStats.Line line = gameStats.line(ballCarrier);
                line.receptions++;
                line.recYards += yards;
            }
        } else if (isPos(ballCarrier, PositionGroup.RB)) {
            if (wasPass) {
                if (gameStats != null) {
                    PlayerGameStats.Line line = gameStats.line(ballCarrier);
                    line.receptions++;
                    line.recYards += yards;
                }
            } else {
                ballCarrier.seasonStats.rushAtt++;
                ballCarrier.seasonStats.rushYards += yards;
                offense.teamRushYards += yards;
                if (gameStats != null) {
                    PlayerGameStats.Line line = gameStats.line(ballCarrier);
                    line.rushAtt++;
                    line.rushYards += yards;
                }
            }
        } else if (isPos(ballCarrier, PositionGroup.QB) && !wasPass) {
            ballCarrier.seasonStats.rushAtt++;
            ballCarrier.seasonStats.rushYards += yards;
            offense.teamRushYards += yards;
            if (gameStats != null) {
                PlayerGameStats.Line line = gameStats.line(ballCarrier);
                line.rushAtt++;
                line.rushYards += yards;
            }
        } else if (isPos(ballCarrier, PositionGroup.FB) && !wasPass) {
            ballCarrier.seasonStats.rushAtt++;
            ballCarrier.seasonStats.rushYards += yards;
            offense.teamRushYards += yards;
            if (gameStats != null) {
                PlayerGameStats.Line line = gameStats.line(ballCarrier);
                line.rushAtt++;
                line.rushYards += yards;
            }
        }
        if (state.possessionHome) state.homeYards += yards;
        else state.awayYards += yards;
    }

    private Player pickReceiver(OnFieldEleven off, Team offense, TargetBias bias) {
        if (bias == TargetBias.RB) {
            Player rb = off.firstWithRole(RoleTag.RB);
            if (rb == null) rb = pickRb(off, offense);
            if (rb != null) return rb;
        }
        if (bias == TargetBias.TE) {
            for (Player p : off.players) {
                if (isPos(p, PositionGroup.TE)) return p;
            }
        }
        List<Player> wrs = off.receivers();
        if (!wrs.isEmpty() && bias != TargetBias.TE) {
            double best = -1;
            Player sel = wrs.get(0);
            for (Player wr : wrs) {
                double pref = Math.pow(wr.ratOvr, 1) * rng.nextDouble();
                if (pref > best) {
                    best = pref;
                    sel = wr;
                }
            }
            return sel;
        }
        for (Player p : off.players) {
            if (isPos(p, PositionGroup.TE)) return p;
        }
        if (!wrs.isEmpty()) return wrs.get(0);
        return offense.getWR(0);
    }

    private Player onFieldQb(OnFieldEleven off, Team offense) {
        Player p = off != null ? off.firstWithRole(RoleTag.QB) : null;
        if (p != null) return p;
        p = off != null ? off.firstOf(PositionGroup.QB) : null;
        if (p != null) return p;
        return offense.getQB(0);
    }

    /** Intended catch spot for DPI: SHORT≈+6, MEDIUM≈+12, DEEP≈+22, ±2 jitter. */
    int passArriveYardLine(int los, DepthBand depth) {
        int base;
        if (depth == DepthBand.DEEP) {
            base = 22;
        } else if (depth == DepthBand.MEDIUM) {
            base = 12;
        } else {
            base = 6;
        }
        int jitter = rng != null ? rng.nextInt(5) - 2 : 0;
        int spot = los + base + jitter;
        if (spot < 1) spot = 1;
        if (spot > 99) spot = 99;
        return spot;
    }

    private Player onFieldCb(OnFieldEleven def, Team defense) {
        Player p = def != null ? def.firstWithRole(RoleTag.CB) : null;
        if (p != null) return p;
        p = def != null ? def.firstOf(PositionGroup.CB) : null;
        if (p != null) return p;
        return defense.getCB(0);
    }

    private Player onFieldS(OnFieldEleven def, Team defense) {
        Player p = def != null ? def.firstWithRole(RoleTag.S) : null;
        if (p != null) return p;
        p = def != null ? def.firstOf(PositionGroup.S) : null;
        if (p != null) return p;
        return defense.getS(0);
    }

    /** Package-visible for carrier-selection tests. */
    Player pickCarrier(OnFieldEleven off, Team offense, OffenseConcept concept) {
        Player qb = onFieldQb(off, offense);
        // Designed QB draw: always the QB when available
        if (concept != null && "gun_qb_draw".equals(concept.id) && qb != null) {
            return qb;
        }
        // Designed QB keep / option: athletic QBs occasionally keep
        int qbSpd = rating(qb, x -> x.spd, 55);
        if (qb != null && concept != null && concept.family == ConceptFamily.RUN
                && rng.nextDouble() < 0.08 + qbSpd / 900.0) {
            return qb;
        }
        Player nonQb = pickNonQbCarrier(off, offense, concept);
        return nonQb != null ? nonQb : qb;
    }

    private Player pickNonQbCarrier(OnFieldEleven off, Team offense, OffenseConcept concept) {
        Player fb = off != null ? off.firstWithRole(RoleTag.FB) : null;
        if (fb != null && concept != null && "21".equals(concept.personnel)
                && rng.nextDouble() < 0.18) {
            return fb;
        }
        Player rbRole = off != null ? off.firstWithRole(RoleTag.RB) : null;
        if (rbRole != null) return rbRole;
        Player rb = pickRb(off, offense);
        if (rb != null) return rb;
        Player fbAny = off != null ? off.firstWithRole(RoleTag.FB) : null;
        if (fbAny != null) return fbAny;
        return null;
    }

    private Player pickRb(OnFieldEleven off, Team offense) {
        if (off != null) {
            for (Player p : off.players) {
                if (isPos(p, PositionGroup.RB)) return p;
            }
        }
        Player a = offense.getRB(0);
        Player b = offense.getRB(1);
        if (a == null) return b;
        if (b == null) return a;
        double aPref = Math.pow(a.ratOvr, 1.5) * rng.nextDouble();
        double bPref = Math.pow(b.ratOvr, 1.5) * rng.nextDouble();
        return aPref > bPref ? a : b;
    }

    private int countBlockWins(OnFieldEleven off, OnFieldEleven def, boolean passProtect) {
        int ol = passProtect ? off.olPassComposite() : off.olRushComposite();
        int rush = passProtect ? def.passRushComposite() : def.runStopComposite();
        int wins = 0;
        for (int i = 0; i < 5; i++) {
            double edge = (ol - rush) / 40.0 + rng.nextGaussian() * 0.35;
            if (edge > 0) wins++;
        }
        return wins;
    }

    private int carrierRushSpd(Player p) {
        return rating(p, x -> x.spd, 55);
    }

    private int carrierRushPow(Player p) {
        return rating(p, x -> x.stre, 55);
    }

    private int carrierRushEva(Player p) {
        return rating(p, x -> x.elu, 50);
    }

    private int recvCatch(Player p) {
        return rating(p, x -> x.hnd, 60);
    }

    private int recvSpeed(Player p) {
        return rating(p, x -> x.spd, 55);
    }

    private int recvEva(Player p) {
        return rating(p, x -> x.elu, 55);
    }

    private int defRun(Team defense) {
        return defense.getCompositeFrontRush();
    }

    private int normalize(int rating) {
        return (100 + rating) / 2;
    }

    private static boolean isPos(Player p, PositionGroup g) {
        return p != null && PositionGroup.fromToken(p.position) == g;
    }

    private interface RatingGetter {
        int get(PlayerRatings r);
    }

    private int rating(Player p, RatingGetter getter, int fallback) {
        if (p == null || p.ratings == null) return fallback;
        int raw = getter.get(p.ratings);
        if (fatigue == null) return raw;
        return (int) Math.round(raw * fatigue.factor(p));
    }
}
