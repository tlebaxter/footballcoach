package CFBsimPack.engine;

import CFBsimPack.DefensiveSystem;
import CFBsimPack.OnFieldEleven;
import CFBsimPack.Player;
import CFBsimPack.PlayerCB;
import CFBsimPack.PlayerK;
import CFBsimPack.PlayerQB;
import CFBsimPack.PlayerRB;
import CFBsimPack.PlayerS;
import CFBsimPack.PlayerTE;
import CFBsimPack.PlayerWR;
import CFBsimPack.Team;
import CFBsimPack.TeamStrategy;

import java.util.List;
import java.util.Random;

/**
 * Resolves a single snap given situation + call. Uses on-field elevens and coverage mods.
 */
public final class PlayResolver {

    private final Random rng;
    private PlayerGameStats gameStats;

    public PlayResolver(Random rng) {
        this.rng = rng != null ? rng : new Random();
    }

    public void setGameStats(PlayerGameStats gameStats) {
        this.gameStats = gameStats;
    }

    public PlayResult resolve(Team home, Team away, GameState state, PlayCall call) {
        Team offense = state.possessionHome ? home : away;
        Team defense = state.possessionHome ? away : home;
        OnFieldEleven offEleven = OnFieldEleven.forOffense(offense);
        OnFieldEleven defEleven = OnFieldEleven.forDefense(defense);

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
            return punt(offense, state, call);
        }
        if (call.offensePlay == OffensePlay.PASS) {
            return pass(offense, defense, offEleven, defEleven, state, call);
        }
        return rush(offense, defense, offEleven, defEleven, state, call);
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

    private PlayResult pass(Team offense, Team defense, OnFieldEleven off, OnFieldEleven def,
                            GameState state, PlayCall call) {
        PlayResult r = new PlayResult();
        r.playType = OffensePlay.PASS;
        OffenseConcept concept = call.resolvedOffenseConcept();
        PlayerQB qb = offense.getQB(0);
        CoverageCall cov = call.coverage;
        DefensiveSystem sys = defense.defSystem != null ? defense.defSystem : DefensiveSystem.BASE_4_3;
        double tempo = call.tempo.clockMult * (1.0 + concept.clockMultExtra);

        int pressure = (int) ((def.passRushComposite() * 2 - off.olPassComposite()) * sys.passWeight
                * concept.sackRiskMod);
        if (rng.nextDouble() * 100 < pressure / 8.0) {
            return sack(offense, state, call, qb);
        }

        TeamStrategy offS = offense.teamStratOff;
        TeamStrategy defS = defense.teamStratDef;
        double intChance = (pressure + def.coverageComposite() - (qb.ratPassAcc + qb.ratFootIQ + 100) / 3.0) / 18.0;
        if (offS != null) intChance += offS.getPAB() * 0.01;
        if (defS != null) intChance += defS.getPAB() * 0.01;
        intChance *= cov.intMod;
        if (concept.depth == DepthBand.DEEP) intChance *= 1.12;
        if (intChance < 0.015) intChance = 0.015;
        if (100 * rng.nextDouble() < intChance) {
            r.turnover = true;
            r.possessionChanged = true;
            r.stoppedClock = true;
            r.clockBurned = (int) (8 + 10 * rng.nextDouble() * tempo);
            qb.statsInt++;
            qb.statsPassAtt++;
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
        PlayerCB cb = defense.getCB(0);
        int cat = recvCatch(target);
        int spd = recvSpeed(target);
        int cbCov = cb != null ? cb.ratCBCov : 70;
        int cbSpd = cb != null ? cb.ratCBSpd : 70;

        double completion = (normalize(qb.ratPassAcc) + normalize(cat) - normalize(cbCov)) / 2.0
                + 18.25 - pressure / 16.8 + homeField(offense, state);
        completion *= cov.completionMod * concept.completionMod;
        if (offS != null) completion -= offS.getPAB();
        if (defS != null) completion -= defS.getPAB();
        completion += cov.passFitBonus();
        completion += concept.matchupBonus(cov);

        qb.statsPassAtt++;
        if (gameStats != null) gameStats.line(qb).passAtt++;
        if (target instanceof PlayerWR) ((PlayerWR) target).statsTargets++;

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
            if (target instanceof PlayerWR) {
                ((PlayerWR) target).statsDrops++;
                if (gameStats != null) gameStats.line(target).drops++;
            }
            r.logLine = "Drop! " + (target != null ? target.name : "Receiver") + " couldn't hang on ("
                    + concept.displayName + ").";
            return r;
        }

        int yards = (int) ((normalize(qb.ratPassPow) + normalize(spd) - normalize(cbSpd)) * rng.nextDouble() / 3.7
                * cov.yardsMod * concept.yardsMod);
        if (concept.depth == DepthBand.DEEP) yards += (int) (4 + 8 * rng.nextDouble());
        else if (concept.depth == DepthBand.SHORT) yards = (int) (yards * 0.85);
        if (offS != null) yards += offS.getPYB() / 2;
        if (defS != null) yards -= defS.getPYB();
        if (yards < 0) yards = 0;

        PlayerS s = defense.getS(0);
        double escape = (normalize(recvEva(target)) * 3 - (cb != null ? cb.ratCBTkl : 70)
                - (s != null ? s.ratOvr : 70)) * rng.nextDouble();
        if (escape > 92 || rng.nextDouble() > 0.95) {
            yards += (int) (3 + spd * rng.nextDouble() / 3);
        }

        return applyGain(offense, defense, state, call, r, qb, target, yards, true);
    }

    private PlayResult rush(Team offense, Team defense, OnFieldEleven off, OnFieldEleven def,
                            GameState state, PlayCall call) {
        PlayResult r = new PlayResult();
        r.playType = OffensePlay.RUN;
        OffenseConcept concept = call.resolvedOffenseConcept();
        PlayerRB rb = pickRb(offense);
        if (rb == null) {
            return pass(offense, defense, off, def, state, call);
        }
        CoverageCall cov = call.coverage;
        DefensiveSystem sys = defense.defSystem != null ? defense.defSystem : DefensiveSystem.BASE_4_3;
        TeamStrategy offS = offense.teamStratOff;
        TeamStrategy defS = defense.teamStratDef;

        int blockAdv = (int) ((off.olRushComposite() - def.runStopComposite() * sys.runWeight)
                + cov.runFitBonus() + concept.matchupBonus(cov));
        int yards = (int) ((rb.ratRushSpd + blockAdv + homeField(offense, state))
                * rng.nextDouble() / 10.0 * concept.runYardsMod);
        if (offS != null) yards += offS.getRYB() / 2;
        if (defS != null) yards -= defS.getRYB() / 2;
        if (yards < 2) {
            yards += rb.ratRushPow / 20 - 3;
        } else if (rng.nextDouble() < 0.28) {
            yards += (int) (rb.ratRushEva / 5.0 * rng.nextDouble());
        }
        if (call.coverage == CoverageCall.SPY) {
            yards = (int) (yards * 0.95);
        }

        return applyGain(offense, defense, state, call, r, null, rb, yards, false);
    }

    private PlayResult applyGain(Team offense, Team defense, GameState state, PlayCall call, PlayResult r,
                                 PlayerQB qb, Player ballCarrier, int yards, boolean wasPass) {
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
            creditTd(offense, state, qb, ballCarrier, actual, wasPass);
            r.logLine = (wasPass ? "PASS TD! " : "RUSH TD! ") + offense.abbr + " "
                    + (ballCarrier != null ? ballCarrier.name : "") + " " + actual + " yards"
                    + " (" + concept.displayName + " · " + call.resolvedDefenseConcept().displayName + ").";
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

        // Fumble check
        double fum = 0;
        if (ballCarrier instanceof PlayerRB) {
            fum = (defense.getS(0).ratSTkl + defRun(defense)) / 2.0;
        } else {
            fum = defense.getS(0).ratSTkl / 2.0;
        }
        fum *= concept.fumbleMod;
        if (100 * rng.nextDouble() < fum / 50.0) {
            r.turnover = true;
            r.possessionChanged = true;
            r.clockBurned = (int) (12 + 10 * rng.nextDouble() * tempo);
            if (ballCarrier instanceof PlayerRB) ((PlayerRB) ballCarrier).statsFumbles++;
            if (ballCarrier instanceof PlayerWR) ((PlayerWR) ballCarrier).statsFumbles++;
            r.logLine = "FUMBLE! " + offense.abbr + " " + (ballCarrier != null ? ballCarrier.name : "")
                    + " lost it (" + concept.displayName + ").";
            return r;
        }

        r.clockBurned = (int) ((wasPass ? (15 + 15 * rng.nextDouble()) : (25 + 15 * rng.nextDouble())) * tempo);
        r.stoppedClock = false;
        r.logLine = offense.abbr + (wasPass ? " pass " : " rush ") + yards + " yards to "
                + (ballCarrier != null ? ballCarrier.name : "ballcarrier")
                + " (" + concept.displayName + " · " + call.formation.displayName + ").";
        return r;
    }

    private PlayResult sack(Team offense, GameState state, PlayCall call, PlayerQB qb) {
        PlayResult r = new PlayResult();
        OffenseConcept concept = call.resolvedOffenseConcept();
        double tempo = call.tempo.clockMult * (1.0 + concept.clockMultExtra);
        r.playType = OffensePlay.PASS;
        r.yardsGained = -3;
        state.yardLine -= 3;
        state.yardsNeed += 3;
        state.down++;
        qb.statsSacked++;
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
        PlayerK k = offense.getK(0);
        int distance = 100 - state.yardLine + 17;
        double chance = k.ratKickAcc + (k.ratKickPow - 70) - (distance - 30);
        r.clockBurned = (int) (5 + 5 * rng.nextDouble());
        r.stoppedClock = true;
        k.statsFGAtt++;
        if (gameStats != null) gameStats.line(k).fgAtt++;
        if (100 * rng.nextDouble() < Math.max(8, Math.min(95, chance))) {
            r.scoreFg = true;
            r.possessionChanged = true;
            k.statsFGMade++;
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

    private PlayResult punt(Team offense, GameState state, PlayCall call) {
        PlayResult r = new PlayResult();
        r.playType = OffensePlay.PUNT;
        int puntYards = 35 + (int) (25 * rng.nextDouble());
        state.yardLine += puntYards;
        if (state.yardLine >= 100) state.yardLine = 80 + (int) (15 * rng.nextDouble());
        r.possessionChanged = true;
        r.clockBurned = (int) (5 + 10 * rng.nextDouble());
        r.stoppedClock = true;
        r.logLine = offense.abbr + " punts " + puntYards + " yards ("
                + call.resolvedOffenseConcept().displayName + ").";
        return r;
    }

    private void creditTd(Team offense, GameState state, PlayerQB qb, Player ballCarrier, int yards, boolean wasPass) {
        if (wasPass && qb != null) {
            qb.statsTD++;
            qb.statsPassComp++;
            qb.statsPassYards += yards;
            offense.teamPassYards += yards;
        }
        if (ballCarrier instanceof PlayerWR) {
            PlayerWR wr = (PlayerWR) ballCarrier;
            wr.statsTD++;
            wr.statsReceptions++;
            wr.statsRecYards += yards;
        } else if (ballCarrier instanceof PlayerTE) {
            PlayerTE te = (PlayerTE) ballCarrier;
            te.statsTD++;
            te.statsReceptions++;
            te.statsRecYards += yards;
        } else if (ballCarrier instanceof PlayerRB) {
            PlayerRB rb = (PlayerRB) ballCarrier;
            rb.statsTD++;
            rb.statsRushAtt++;
            rb.statsRushYards += yards;
            offense.teamRushYards += yards;
        }
        if (state.possessionHome) state.homeYards += yards;
        else state.awayYards += yards;
    }

    private void creditYards(Team offense, GameState state, PlayerQB qb, Player ballCarrier, int yards, boolean wasPass) {
        if (wasPass && qb != null) {
            qb.statsPassComp++;
            qb.statsPassYards += yards;
            offense.teamPassYards += yards;
        }
        if (ballCarrier instanceof PlayerWR) {
            ((PlayerWR) ballCarrier).statsReceptions++;
            ((PlayerWR) ballCarrier).statsRecYards += yards;
        } else if (ballCarrier instanceof PlayerTE) {
            ((PlayerTE) ballCarrier).statsReceptions++;
            ((PlayerTE) ballCarrier).statsRecYards += yards;
        } else if (ballCarrier instanceof PlayerRB) {
            ((PlayerRB) ballCarrier).statsRushAtt++;
            ((PlayerRB) ballCarrier).statsRushYards += yards;
            offense.teamRushYards += yards;
        }
        if (state.possessionHome) state.homeYards += yards;
        else state.awayYards += yards;
    }

    private Player pickReceiver(OnFieldEleven off, Team offense, TargetBias bias) {
        if (bias == TargetBias.RB) {
            PlayerRB rb = pickRb(offense);
            if (rb != null) return rb;
        }
        if (bias == TargetBias.TE) {
            for (Player p : off.players) {
                if (p instanceof PlayerTE) return p;
            }
        }
        List<PlayerWR> wrs = off.receivers();
        if (!wrs.isEmpty() && bias != TargetBias.TE) {
            double best = -1;
            PlayerWR sel = wrs.get(0);
            for (PlayerWR wr : wrs) {
                double pref = Math.pow(wr.ratOvr, 1) * rng.nextDouble();
                if (pref > best) {
                    best = pref;
                    sel = wr;
                }
            }
            return sel;
        }
        for (Player p : off.players) {
            if (p instanceof PlayerTE) return p;
        }
        if (!wrs.isEmpty()) return wrs.get(0);
        return offense.getWR(0);
    }

    private PlayerRB pickRb(Team offense) {
        PlayerRB a = offense.getRB(0);
        PlayerRB b = offense.getRB(1);
        if (a == null) return b;
        if (b == null) return a;
        double aPref = Math.pow(a.ratOvr, 1.5) * rng.nextDouble();
        double bPref = Math.pow(b.ratOvr, 1.5) * rng.nextDouble();
        return aPref > bPref ? a : b;
    }

    private int recvCatch(Player p) {
        if (p instanceof PlayerWR) return ((PlayerWR) p).ratRecCat;
        if (p instanceof PlayerTE) return ((PlayerTE) p).ratRecCat;
        return 60;
    }

    private int recvSpeed(Player p) {
        if (p instanceof PlayerWR) return ((PlayerWR) p).ratRecSpd;
        if (p instanceof PlayerTE) return ((PlayerTE) p).ratRecSpd;
        return 55;
    }

    private int recvEva(Player p) {
        if (p instanceof PlayerWR) return ((PlayerWR) p).ratRecEva;
        if (p instanceof PlayerTE) return ((PlayerTE) p).ratRecCat;
        return 55;
    }

    private int defRun(Team defense) {
        return defense.getCompositeFrontRush();
    }

    private int homeField(Team offense, GameState state) {
        // Rough HF: home offense gets +2
        boolean homeOff = state.possessionHome;
        return homeOff ? 2 : 0;
    }

    private int normalize(int rating) {
        return (100 + rating) / 2;
    }
}
