package CFBsimPack;

import CFBsimPack.engine.AiPlayCaller;
import CFBsimPack.engine.AutoSimUntil;
import CFBsimPack.engine.BoxScoreLine;
import CFBsimPack.engine.DefenseConcept;
import CFBsimPack.engine.GamePhase;
import CFBsimPack.engine.GameSituation;
import CFBsimPack.engine.GameState;
import CFBsimPack.engine.OffenseConcept;
import CFBsimPack.engine.OffensePlay;
import CFBsimPack.engine.PlayCall;
import CFBsimPack.engine.PlayLogEntry;
import CFBsimPack.OnFieldEleven;
import CFBsimPack.engine.FatigueTracker;
import CFBsimPack.engine.PenaltyCatalog;
import CFBsimPack.engine.PenaltyResolver;
import CFBsimPack.engine.PendingPlay;
import CFBsimPack.engine.PlayResolver;
import CFBsimPack.engine.PlayState;
import CFBsimPack.engine.TempoCall;
import CFBsimPack.engine.PlayResult;
import CFBsimPack.engine.Playbook;
import CFBsimPack.engine.PlayerGameStats;
import CFBsimPack.engine.TempoCall;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Matchup + stepable game engine facade.
 * League/Conference call {@link #playGame()}; live coach uses start/execute/autoSim/finalize.
 */
public class Game implements Serializable {

    public Team homeTeam;
    public Team awayTeam;
    public boolean hasPlayed;
    public String gameName;
    /** Optional OOC contract id when this game was materialized from a multi-year deal. */
    public String contractId;

    public int homeScore;
    public int[] homeQScore;
    public int awayScore;
    public int[] awayQScore;
    public int homeYards;
    public int awayYards;
    public int numOT;
    public int homeTOs;
    public int awayTOs;

    public transient PlayerGameStats playerGameStats = new PlayerGameStats();
    public transient GameState state;
    public transient String gameEventLog = "";
    public transient List<Integer> drivePath = new ArrayList<>();
    public transient List<PlayLogEntry> playLog = new ArrayList<>();
    public transient String lastOffenseConceptId;
    public transient String lastDefenseConceptId;

    private transient PlayResolver resolver;
    private transient AiPlayCaller aiCaller;
    private transient FatigueTracker fatigueTracker;
    private transient Random rng;
    private transient boolean started;
    private transient int prevQuarter = 1;

    public Game(Team home, Team away, String name) {
        homeTeam = home;
        awayTeam = away;
        gameName = name;
        initScores();
        hasPlayed = false;
        tagRivalry();
    }

    public Game(Team home, Team away) {
        homeTeam = home;
        awayTeam = away;
        gameName = home.conference.equals(away.conference) ? "In Conf" : "OOC";
        initScores();
        hasPlayed = false;
        tagRivalry();
    }

    private void initScores() {
        homeScore = 0;
        awayScore = 0;
        homeQScore = new int[10];
        awayQScore = new int[10];
        numOT = 0;
        homeTOs = 0;
        awayTOs = 0;
        homeYards = 0;
        awayYards = 0;
    }

    private void tagRivalry() {
        if (Team.strongestRivalryBetween(homeTeam, awayTeam) > 0) {
            if (gameName.equals("In Conf")) gameName = "Rivalry Game";
            else if (gameName.equals("OOC")) gameName = "OOC Rivalry";
        }
    }

    /** Strongest rivalry strength between the two teams (0 if none). */
    public int rivalryStrength() {
        return Team.strongestRivalryBetween(homeTeam, awayTeam);
    }

    public void setRandom(Random random) {
        this.rng = random != null ? random : new Random();
        this.resolver = new PlayResolver(this.rng);
        if (playerGameStats != null) this.resolver.setGameStats(playerGameStats);
        this.aiCaller = new AiPlayCaller(this.rng);
        this.fatigueTracker = new FatigueTracker();
    }

    private void ensureEngine() {
        if (rng == null) setRandom(new Random());
        if (playerGameStats == null) playerGameStats = new PlayerGameStats();
        if (resolver != null) resolver.setGameStats(playerGameStats);
        if (fatigueTracker == null) fatigueTracker = new FatigueTracker();
        if (drivePath == null) drivePath = new ArrayList<>();
        if (playLog == null) playLog = new ArrayList<>();
    }

    private static void writePlayState(GameState g, PlayState s) {
        if (g == null || s == null) return;
        g.down = s.down;
        g.yardsNeed = s.yardsNeed;
        g.yardLine = s.yardLine;
        g.gameTime = s.gameTime;
        g.possessionHome = s.possessionHome;
        g.homeScore = s.homeScore;
        g.awayScore = s.awayScore;
        g.phase = s.phase;
    }

    private int betweenPlayRunoff(TempoCall tempo) {
        TempoCall t = tempo != null ? tempo : TempoCall.NORMAL;
        if (t == TempoCall.HURRY_UP) return 12;
        if (t == TempoCall.CHEW_CLOCK) return 38;
        return 28;
    }

    private void afterSnapFatigue(PlayCall call, boolean possessionHome) {
        if (fatigueTracker == null || call == null) return;
        Team offense = possessionHome ? homeTeam : awayTeam;
        Team defense = possessionHome ? awayTeam : homeTeam;
        String pers = call.resolvedOffenseConcept() != null
                ? call.resolvedOffenseConcept().personnel : null;
        OnFieldEleven off = OnFieldEleven.forOffense(offense, pers);
        OnFieldEleven def = OnFieldEleven.forDefense(defense);
        fatigueTracker.afterSnap(off, def);
    }

    public void startGame() {
        if (hasPlayed) return;
        ensureEngine();
        DepthChart.applySystems(homeTeam);
        DepthChart.applySystems(awayTeam);
        homeTeam.ensureSpecialTeamsDepth();
        awayTeam.ensureSpecialTeamsDepth();
        state = new GameState();
        state.gameTime = GameState.REG_SECONDS;
        state.possessionHome = true;
        state.yardLine = 35;
        state.down = 1;
        state.yardsNeed = 10;
        state.pendingKickoff = false;
        state.freeKick = false;
        state.phase = GamePhase.REGULATION;
        state.homeWonToss = rng.nextBoolean();
        state.awaitingCoinToss = true;
        state.tossResolved = false;
        state.homeDefendsLeft = true;
        started = true;
        prevQuarter = 1;
        drivePath = new ArrayList<>();
        playLog = new ArrayList<>();
        lastOffenseConceptId = null;
        lastDefenseConceptId = null;
        playerGameStats = new PlayerGameStats();
        if (resolver != null) resolver.setGameStats(playerGameStats);
        String tossWinner = state.homeWonToss ? homeTeam.abbr : awayTeam.abbr;
        gameEventLog = "LOG: #" + awayTeam.rankTeamPollScore + " " + awayTeam.abbr + " (" + awayTeam.wins + "-" + awayTeam.losses
                + ") @ #" + homeTeam.rankTeamPollScore + " " + homeTeam.abbr + " (" + homeTeam.wins + "-" + homeTeam.losses + ")\n"
                + "---------------------------------------------------------\n\n"
                + awayTeam.abbr + " Offense: " + awayTeam.offPhilosophy.displayName + "\n"
                + awayTeam.abbr + " Defense: " + awayTeam.defSystem.displayName + "\n"
                + homeTeam.abbr + " Offense: " + homeTeam.offPhilosophy.displayName + "\n"
                + homeTeam.abbr + " Defense: " + homeTeam.defSystem.displayName + "\n"
                + tossWinner + " wins the coin toss.\n";

        boolean userOnHome = homeTeam.userControlled;
        boolean userOnAway = awayTeam.userControlled;
        boolean userWonToss = (userOnHome && state.homeWonToss) || (userOnAway && !state.homeWonToss);
        if (!userWonToss) {
            // League sims and AI toss winners resolve immediately.
            autoResolveCoinToss();
        }
    }

    /**
     * Toss winner elects receive or defer, and which end they defend.
     *
     * @param receive    true to receive first half; false to defer
     * @param defendLeft true if the toss winner defends the left end zone
     */
    public boolean applyTossChoice(boolean receive, boolean defendLeft) {
        if (state == null || !state.awaitingCoinToss || state.tossResolved) return false;
        state.deferred = !receive;
        state.homeReceivesFirstHalf = receive ? state.homeWonToss : !state.homeWonToss;
        state.homeDefendsLeft = state.homeWonToss ? defendLeft : !defendLeft;
        state.possessionHome = !state.homeReceivesFirstHalf;
        state.yardLine = 35;
        state.down = 1;
        state.yardsNeed = 10;
        state.pendingKickoff = true;
        state.freeKick = false;
        state.awaitingCoinToss = false;
        state.tossResolved = true;
        resetDrive(35);

        String winner = state.homeWonToss ? homeTeam.abbr : awayTeam.abbr;
        String election = receive ? "elected to receive" : "elected to defer";
        String end = defendLeft ? "defend left" : "defend right";
        String msg = winner + " " + election + " and will " + end + ".";
        state.lastPlayLog = msg;
        gameEventLog += msg + "\nOpening kickoff pending.\n";
        return true;
    }

    /** AI / league toss resolution: ~55% receive, random end. */
    public void autoResolveCoinToss() {
        if (state == null || !state.awaitingCoinToss) return;
        boolean receive = rng.nextDouble() < 0.55;
        boolean defendLeft = rng.nextBoolean();
        applyTossChoice(receive, defendLeft);
    }

    public boolean userWonCoinToss() {
        if (state == null) return false;
        if (homeTeam.userControlled) return state.homeWonToss;
        if (awayTeam.userControlled) return !state.homeWonToss;
        return false;
    }

    public GameSituation getSituation() {
        if (state == null) startGame();
        Team user = homeTeam.userControlled ? homeTeam : (awayTeam.userControlled ? awayTeam : null);
        boolean userOff = user != null && ((state.possessionHome && user == homeTeam) || (!state.possessionHome && user == awayTeam));
        String dd;
        if (state.awaitingCoinToss) {
            dd = "Coin toss";
        } else if (state.pendingTry && state.tryAwaitingChoice) {
            dd = "PAT / 2-Point";
        } else if (state.pendingTry && state.tryIsTwoPoint) {
            dd = "2-Point Try · OPP 3";
        } else if (state.pendingKickoff) {
            dd = (state.freeKick ? "Free kick" : "Kickoff") + " pending";
        } else {
            dd = ordinal(state.down) + " & " + state.yardsNeed + " · "
                    + (state.yardLine <= 50 ? "OWN " + state.yardLine : "OPP " + (100 - state.yardLine));
        }
        String prName = null;
        String krName = null;
        if (user != null) {
            if (user.getPuntReturner() != null) prName = user.getPuntReturner().name;
            if (user.getKickReturner() != null) krName = user.getKickReturner().name;
        }
        boolean userChoosesTry = state.pendingTry && state.tryAwaitingChoice && userOff;
        boolean userDefendsTwoPoint = state.pendingTry && state.tryIsTwoPoint && !userOff;
        boolean userIsHome = user == homeTeam;
        boolean canCallTimeout = user != null && !hasPlayed && state.canCallTimeout(userIsHome);
        return new GameSituation(
                state.homeScore, state.awayScore, homeTeam.abbr, awayTeam.abbr,
                homeTeam.name, awayTeam.name,
                homeTeam.rankTeamPollScore, awayTeam.rankTeamPollScore,
                state.quarter(), state.clockDisplay(), state.down, state.yardsNeed, state.yardLine,
                state.possessionHome, state.homeTimeouts, state.awayTimeouts,
                state.playingOT, state.gameOver || hasPlayed, userOff,
                state.lastPlayLog, dd,
                state.homeYards, state.awayYards, state.homeTOs, state.awayTOs,
                state.homeQScore, state.awayQScore,
                drivePath, playLog, buildBoxScore(),
                lastOffenseConceptId, lastDefenseConceptId,
                state.pendingKickoff, state.freeKick, state.isSpecialTeamsDown(),
                prName, krName,
                state.awaitingCoinToss, state.homeWonToss, state.homeDefendsLeft, userWonCoinToss(),
                state.pendingTry, state.tryAwaitingChoice, state.tryIsTwoPoint,
                userChoosesTry, userDefendsTwoPoint,
                canCallTimeout
        );
    }

    /**
     * Build a full play call, filling any missing side via AI so every snap is a matchup.
     */
    public PlayCall buildMatchedCall(OffenseConcept offenseConcept, DefenseConcept defenseConcept, TempoCall tempo) {
        ensureEngine();
        if (state == null) startGame();
        Team offense = state.possessionHome ? homeTeam : awayTeam;
        Team defense = state.possessionHome ? awayTeam : homeTeam;
        OffenseConcept off = offenseConcept;
        DefenseConcept def = defenseConcept;
        if (off == null) {
            off = aiCaller.suggestOffense(offense, defense, state);
        }
        if (def == null) {
            def = aiCaller.suggestDefense(defense, state, off);
        }
        TempoCall t = tempo != null ? tempo : TempoCall.NORMAL;
        return PlayCall.fromConcepts(off, def, t);
    }

    public AiPlayCaller getAiCaller() {
        ensureEngine();
        return aiCaller;
    }

    public boolean callTimeout(boolean home) {
        if (state == null || hasPlayed) return false;
        boolean ok = state.callTimeout(home);
        if (ok) {
            state.lastPlayLog = (home ? homeTeam.abbr : awayTeam.abbr) + " timeout.";
            gameEventLog += prefix() + state.lastPlayLog + "\n";
        }
        return ok;
    }

    public PlayResult executeSnap(PlayCall call) {
        if (hasPlayed) return PlayResult.logOnly("Game already final.", 0);
        if (state == null) startGame();
        if (state.gameOver) return PlayResult.logOnly("Game over.", 0);
        ensureEngine();
        if (state.awaitingCoinToss) {
            return PlayResult.logOnly("Coin toss pending.", 0);
        }
        if (state.pendingTry && state.tryAwaitingChoice) {
            if (userControlsOffense()) {
                return PlayResult.logOnly("PAT / 2-point choice pending.", 0);
            }
            autoResolveTryIfNeeded();
            if (state.pendingTry && state.tryAwaitingChoice) {
                return PlayResult.logOnly("PAT / 2-point choice pending.", 0);
            }
            if (!state.pendingTry) {
                syncPublicFields();
                return PlayResult.logOnly(state.lastPlayLog != null ? state.lastPlayLog : "Try resolved.", 0);
            }
            // AI elected go-for-2; fall through to snap if a call was provided / AI both sides
        }

        // Turnover on downs before snap
        if (!state.pendingKickoff && !state.pendingTry && state.down > 4) {
            return handleTurnoverOnDowns();
        }

        Team offense = state.possessionHome ? homeTeam : awayTeam;
        Team defense = state.possessionHome ? awayTeam : homeTeam;
        if (call == null) {
            call = aiCaller.choose(offense, defense, state);
        } else if (call.offenseConcept == null || call.defenseConcept == null) {
            // Preserve bare OffensePlay / CoverageCall mappings; AI-fill only a truly missing side
            OffenseConcept off = call.offenseConcept != null
                    ? call.offenseConcept
                    : call.resolvedOffenseConcept();
            DefenseConcept def = call.defenseConcept != null
                    ? call.defenseConcept
                    : aiCaller.suggestDefense(defense, state, off);
            call = PlayCall.fromConcepts(off, def, call.tempo != null ? call.tempo : TempoCall.NORMAL);
        }
        if (state.pendingKickoff) {
            // Force kickoff snap; keep defense package from the call
            call = PlayCall.fromConcepts(
                    Playbook.offenseById("kickoff"),
                    call.resolvedDefenseConcept(),
                    call.tempo != null ? call.tempo : TempoCall.NORMAL
            );
        }

        int yardBefore = state.yardLine;
        int downBefore = state.down;
        int distBefore = state.yardsNeed;
        String clockBefore = state.clockDisplay();
        int qBefore = state.quarter();
        boolean possBefore = state.possessionHome;

        PlayState before = PlayState.from(state);
        PlayResult result = resolver.resolve(homeTeam, awayTeam, state, call);
        // Resolver mutates GameState; snapshot provisional after-play situation
        PlayState after = PlayState.from(state);

        PendingPlay pending = new PendingPlay(result, before, after);
        pending.foul = PenaltyCatalog.roll(rng, call.offensePlay);
        if (pending.foul != null) {
            PenaltyResolver.resolve(pending);
        }

        if (pending.foul != null && pending.foulAccepted) {
            // Accepted foul: discard play outcome; keep penalty spot/down
            writePlayState(state, pending.after);
            state.homeScore = before.homeScore;
            state.awayScore = before.awayScore;
            state.possessionHome = before.possessionHome;
            int runoff = betweenPlayRunoff(call.tempo);
            if (!state.playingOT) {
                state.gameTime = Math.max(0, before.gameTime - Math.max(result.clockBurned, 0) - runoff);
            }
            state.lastPlayLog = result.logLine != null ? result.logLine : "";
            if (result.logLine != null && !result.logLine.isEmpty()) {
                gameEventLog += prefix() + result.logLine + "\n";
            }
            recordPlay(call, result, clockBefore, qBefore, downBefore, distBefore, yardBefore, possBefore);
            afterSnapFatigue(call, possBefore);
            state.halfUnderway = true;
            syncPublicFields();
            return result;
        }

        // No foul or declined: keep resolved play situation, then score/flip via applyResult
        writePlayState(state, pending.after);
        recordPlay(call, result, clockBefore, qBefore, downBefore, distBefore, yardBefore, possBefore);
        applyResult(result, call);
        int runoff = betweenPlayRunoff(call.tempo);
        if (!state.playingOT && runoff > 0) {
            state.gameTime = Math.max(0, state.gameTime - runoff);
        }
        afterSnapFatigue(call, possBefore);
        state.halfUnderway = true;
        syncPublicFields();

        if (!state.playingOT && state.gameTime <= 0) {
            if (state.homeScore == state.awayScore) {
                enterOT();
            } else {
                state.gameOver = true;
                gameEventLog += prefix() + "Time has expired! The game is over.\n";
            }
        }
        if (qBefore == 2 && state.quarter() >= 3) {
            beginSecondHalf();
        }
        return result;
    }

    /** Halftime: swap ends, reset timeouts, schedule second-half kickoff. */
    private void beginSecondHalf() {
        state.resetTimeoutsForHalf();
        state.halfUnderway = false;
        state.homeDefendsLeft = !state.homeDefendsLeft;
        boolean homeReceivesSecond = state.deferred
                ? state.homeWonToss
                : !state.homeReceivesFirstHalf;
        state.possessionHome = !homeReceivesSecond;
        state.yardLine = 35;
        state.down = 1;
        state.yardsNeed = 10;
        state.pendingKickoff = true;
        state.freeKick = false;
        resetDrive(35);
        String receiver = homeReceivesSecond ? homeTeam.abbr : awayTeam.abbr;
        String msg = "Halftime — ends switch; " + receiver + " to receive.";
        state.lastPlayLog = msg;
        gameEventLog += prefix() + msg + "\n";
    }

    private void recordPlay(
            PlayCall call,
            PlayResult result,
            String clockBefore,
            int quarter,
            int down,
            int distance,
            int yardBefore,
            boolean possessionHome
    ) {
        if (call != null) {
            lastOffenseConceptId = call.resolvedOffenseConcept().id;
            lastDefenseConceptId = call.resolvedDefenseConcept().id;
        }
        if (drivePath == null) drivePath = new ArrayList<>();
        if (playLog == null) playLog = new ArrayList<>();
        if (drivePath.isEmpty()) drivePath.add(yardBefore);
        int endYard = state != null ? state.yardLine : yardBefore;
        if (result != null && !result.possessionChanged && !result.touchdown && !result.scoreFg && !result.safety) {
            drivePath.add(Math.max(0, Math.min(100, endYard)));
        }
        playLog.add(new PlayLogEntry(
                clockBefore,
                quarter,
                down,
                distance,
                yardBefore,
                result != null ? result.yardsGained : 0,
                call != null ? call.resolvedOffenseConcept().id : null,
                call != null ? call.resolvedOffenseConcept().displayName : "",
                call != null ? call.resolvedDefenseConcept().id : null,
                call != null ? call.resolvedDefenseConcept().displayName : "",
                result != null ? result.logLine : "",
                possessionHome
        ));
    }

    private void resetDrive(int startYard) {
        drivePath = new ArrayList<>();
        drivePath.add(Math.max(0, Math.min(100, startYard)));
    }

    private List<BoxScoreLine> buildBoxScore() {
        List<BoxScoreLine> lines = new ArrayList<>();
        if (playerGameStats == null) return lines;
        for (PlayerGameStats.Line line : playerGameStats.byKey.values()) {
            if (line.player == null) continue;
            boolean meaningful = line.passAtt > 0 || line.rushAtt > 0 || line.receptions > 0
                    || line.fgAtt > 0 || line.xpAtt > 0
                    || line.prAtt > 0 || line.krAtt > 0 || line.puntAtt > 0;
            if (!meaningful) continue;
            boolean home = line.player.team == homeTeam;
            lines.add(new BoxScoreLine(
                    line.player.name,
                    line.player.position,
                    home,
                    line.passComp,
                    line.passAtt,
                    line.passYards,
                    line.passTd,
                    line.passInt,
                    line.rushAtt,
                    line.rushYards,
                    line.rushTd,
                    line.receptions,
                    line.recYards,
                    line.recTd,
                    line.prAtt,
                    line.prYards,
                    line.prTd,
                    line.krAtt,
                    line.krYards,
                    line.krTd,
                    line.fgMade,
                    line.fgAtt,
                    line.xpMade,
                    line.xpAtt,
                    line.puntAtt,
                    line.puntYards
            ));
        }
        return lines;
    }

    public void autoSimUntil(AutoSimUntil until) {
        if (hasPlayed) return;
        if (state == null) startGame();
        if (state.awaitingCoinToss) {
            autoResolveCoinToss();
        }
        boolean startHomePoss = state.possessionHome;
        int startQuarter = state.quarter();
        boolean startFirstHalf = state.gameTime > 1800;

        int guard = 0;
        while (!state.gameOver && !hasPlayed && guard++ < 800) {
            autoResolveTryIfNeeded(true);
            Team offense = state.possessionHome ? homeTeam : awayTeam;
            Team defense = state.possessionHome ? awayTeam : homeTeam;
            PlayCall call = aiCaller.choose(offense, defense, state);
            PlayResult r = executeSnap(call);
            // User TDs leave a try choice pending; resolve with AI policy so sim can continue.
            autoResolveTryIfNeeded(true);

            if (until == AutoSimUntil.GAME) {
                if (state.gameOver) break;
                continue;
            }
            if (until == AutoSimUntil.QUARTER && state.quarter() != startQuarter) break;
            if (until == AutoSimUntil.HALF) {
                boolean firstHalf = state.gameTime > 1800;
                if (startFirstHalf && !firstHalf) break;
                if (state.gameOver) break;
            }
            if (until == AutoSimUntil.POSSESSION || until == AutoSimUntil.DRIVE) {
                if (r != null && (r.possessionChanged || r.touchdown || r.scoreFg || r.safety || r.turnover)) {
                    break;
                }
                if (state.possessionHome != startHomePoss) break;
            }
        }
        if (state.gameOver && !hasPlayed) {
            finalizeGame();
        }
    }

    public void playGame() {
        if (hasPlayed) return;
        startGame();
        if (state.awaitingCoinToss) {
            autoResolveCoinToss();
        }
        int guard = 0;
        while (!state.gameOver && guard++ < 900) {
            autoResolveTryIfNeeded();
            Team offense = state.possessionHome ? homeTeam : awayTeam;
            Team defense = state.possessionHome ? awayTeam : homeTeam;
            executeSnap(aiCaller.choose(offense, defense, state));
        }
        if (!hasPlayed) finalizeGame();
    }

    public void finalizeGame() {
        if (hasPlayed || state == null) return;
        syncPublicFields();
        numOT = state.numOT;

        if (homeScore > awayScore) {
            homeTeam.wins++;
            homeTeam.totalWins++;
            homeTeam.gameWLSchedule.add("W");
            awayTeam.losses++;
            awayTeam.totalLosses++;
            awayTeam.gameWLSchedule.add("L");
            homeTeam.gameWinsAgainst.add(awayTeam);
            homeTeam.winStreak.addWin(homeTeam.league.getYear());
            homeTeam.league.checkLongestWinStreak(homeTeam.winStreak);
            awayTeam.winStreak.resetStreak(awayTeam.league.getYear());
        } else {
            homeTeam.losses++;
            homeTeam.totalLosses++;
            homeTeam.gameWLSchedule.add("L");
            awayTeam.wins++;
            awayTeam.totalWins++;
            awayTeam.gameWLSchedule.add("W");
            awayTeam.gameWinsAgainst.add(homeTeam);
            awayTeam.winStreak.addWin(awayTeam.league.getYear());
            awayTeam.league.checkLongestWinStreak(awayTeam.winStreak);
            homeTeam.winStreak.resetStreak(homeTeam.league.getYear());
        }

        homeTeam.addGamePlayedPlayers(homeScore > awayScore);
        awayTeam.addGamePlayedPlayers(awayScore > homeScore);

        homeTeam.teamPoints += homeScore;
        awayTeam.teamPoints += awayScore;
        homeTeam.teamOppPoints += awayScore;
        awayTeam.teamOppPoints += homeScore;
        homeTeam.teamYards += homeYards;
        awayTeam.teamYards += awayYards;
        homeTeam.teamOppYards += awayYards;
        awayTeam.teamOppYards += homeYards;
        homeTeam.teamOppPassYards += getPassYards(true);
        awayTeam.teamOppPassYards += getPassYards(false);
        homeTeam.teamOppRushYards += getRushYards(true);
        awayTeam.teamOppRushYards += getRushYards(false);
        homeTeam.teamTODiff += awayTOs - homeTOs;
        awayTeam.teamTODiff += homeTOs - awayTOs;

        boolean homeWon = homeScore > awayScore;
        if (homeTeam.isRival(awayTeam.abbr)) {
            homeTeam.recordRivalryResult(awayTeam.abbr, homeWon);
        }
        if (awayTeam.isRival(homeTeam.abbr)) {
            awayTeam.recordRivalryResult(homeTeam.abbr, !homeWon);
        }

        if (homeTeam.league != null && homeTeam.league.oocContracts != null) {
            homeTeam.league.oocContracts.settlePlayedGame(this);
        }

        homeTeam.checkForInjury();
        awayTeam.checkForInjury();
        hasPlayed = true;
        state.gameOver = true;
    }

    private void applyResult(PlayResult result, PlayCall call) {
        if (result == null) return;
        state.lastPlayLog = result.logLine != null ? result.logLine : "";
        if (result.logLine != null && !result.logLine.isEmpty()) {
            gameEventLog += prefix() + result.logLine + "\n";
        }

        int burn = Math.max(0, result.clockBurned);
        if (!state.playingOT) {
            state.gameTime -= burn;
            if (state.gameTime < 0) state.gameTime = 0;
        }

        if (state.pendingTry && state.tryIsTwoPoint) {
            applyTwoPointResult(result);
            return;
        }
        if (result.touchdown) {
            if (result.returnTd) {
                // Return TD: scoring team is the receiving side
                state.possessionHome = !state.possessionHome;
            }
            addScore(6);
            if (otTouchdownAlreadyWins()) {
                gameEventLog += prefix() + "Game-winning TD — no try needed.\n";
                state.lastPlayLog = "Game-winning touchdown!";
                resetForOT();
                return;
            }
            beginTryAfterTd();
            return;
        }
        if (result.scoreFg) {
            addScore(3);
            if (!state.playingOT) kickoffAfterScore();
            else resetForOT();
            return;
        }
        if (result.safety) {
            // Defense scores 2
            if (state.possessionHome) state.awayScore += 2;
            else state.homeScore += 2;
            addPointsQuarter(2, !state.possessionHome);
            freeKick();
            return;
        }
        if (result.turnover || result.possessionChanged) {
            if (result.turnover) {
                if (state.possessionHome) state.homeTOs++;
                else state.awayTOs++;
            }
            boolean wasKickoff = state.pendingKickoff || result.playType == OffensePlay.KICKOFF;
            flipPossession(result.playType == OffensePlay.FIELD_GOAL && !result.scoreFg);
            if (wasKickoff) {
                state.pendingKickoff = false;
                state.freeKick = false;
            }
            return;
        }
        if (state.pendingKickoff && result.playType == OffensePlay.KICKOFF) {
            state.pendingKickoff = false;
            state.freeKick = false;
        }
    }

    private PlayResult handleTurnoverOnDowns() {
        PlayResult r = new PlayResult();
        Team offense = state.possessionHome ? homeTeam : awayTeam;
        r.possessionChanged = true;
        r.turnover = true;
        r.logLine = "TURNOVER ON DOWNS! " + offense.abbr + " turns it over.";
        gameEventLog += prefix() + r.logLine + "\n";
        if (state.playingOT) {
            resetForOT();
        } else {
            flipPossession(false);
        }
        state.lastPlayLog = r.logLine;
        syncPublicFields();
        return r;
    }

    private void flipPossession(boolean missedFg) {
        state.possessionHome = !state.possessionHome;
        state.down = 1;
        state.yardsNeed = 10;
        if (missedFg) {
            state.yardLine = 100 - state.yardLine;
        } else {
            state.yardLine = 100 - state.yardLine;
        }
        if (state.yardLine < 1) state.yardLine = 20;
        if (state.yardLine > 99) state.yardLine = 80;
        resetDrive(state.yardLine);
    }

    private void kickoffAfterScore() {
        // Scoring team kicks; keep possession with them for the kickoff snap
        state.pendingKickoff = true;
        state.freeKick = false;
        state.yardLine = 35;
        state.down = 1;
        state.yardsNeed = 10;
        resetDrive(35);
        gameEventLog += prefix() + "Kickoff pending.\n";
    }

    private void freeKick() {
        // Team that was safetied kicks from its 20
        state.pendingKickoff = true;
        state.freeKick = true;
        state.yardLine = 20;
        state.down = 1;
        state.yardsNeed = 10;
        resetDrive(20);
        gameEventLog += prefix() + "Free kick pending.\n";
    }

    private void beginTryAfterTd() {
        state.pendingTry = true;
        state.tryAwaitingChoice = true;
        state.tryIsTwoPoint = false;
        state.pendingKickoff = false;
        state.yardLine = 97;
        state.down = 1;
        state.yardsNeed = 3;
        resetDrive(97);
        gameEventLog += prefix() + "PAT / 2-point choice pending.\n";
        state.lastPlayLog = "Touchdown — choose Kick XP or Go for 2.";
        // League / AI offense resolves immediately; coach pauses when user scored.
        if (!userControlsOffense()) {
            autoResolveTryIfNeeded();
        }
    }

    /** Bottom of OT TD that already puts the scoring team ahead — try cannot change the winner. */
    private boolean otTouchdownAlreadyWins() {
        if (!state.playingOT || !state.bottomOT) return false;
        if (state.possessionHome) return state.homeScore > state.awayScore;
        return state.awayScore > state.homeScore;
    }

    private boolean userControlsOffense() {
        Team offense = state.possessionHome ? homeTeam : awayTeam;
        return offense != null && offense.userControlled;
    }

    /** Coach: kick the extra point. */
    public boolean chooseKickXp() {
        if (state == null || !state.pendingTry || !state.tryAwaitingChoice) return false;
        state.tryAwaitingChoice = false;
        state.tryIsTwoPoint = false;
        kickXp();
        finishTrySequence();
        syncPublicFields();
        return true;
    }

    /** Coach: go for two — sets up a short-yardage snap. */
    public boolean chooseGoForTwo() {
        if (state == null || !state.pendingTry || !state.tryAwaitingChoice) return false;
        state.tryAwaitingChoice = false;
        state.tryIsTwoPoint = true;
        state.yardLine = 97;
        state.down = 1;
        state.yardsNeed = 3;
        resetDrive(97);
        state.lastPlayLog = "Going for 2.";
        gameEventLog += prefix() + "Going for 2.\n";
        syncPublicFields();
        return true;
    }

    /**
     * AI / auto-sim: if a try is awaiting choice and offense is AI, pick and resolve.
     * When going for 2 with AI offense, leaves tryIsTwoPoint set for the next snap
     * (or snaps immediately when defense is also AI).
     */
    public void autoResolveTryIfNeeded() {
        autoResolveTryIfNeeded(false);
    }

    /**
     * @param force when true, also resolve for user-controlled offense (used by auto-sim).
     */
    public void autoResolveTryIfNeeded(boolean force) {
        if (state == null || !state.pendingTry || !state.tryAwaitingChoice) return;
        if (!force && userControlsOffense()) return;
        ensureEngine();
        Team offense = state.possessionHome ? homeTeam : awayTeam;
        Team defense = state.possessionHome ? awayTeam : homeTeam;
        boolean goTwo = aiCaller.shouldGoForTwo(offense, state);
        if (goTwo) {
            state.tryAwaitingChoice = false;
            state.tryIsTwoPoint = true;
            state.yardLine = 97;
            state.down = 1;
            state.yardsNeed = 3;
            resetDrive(97);
            gameEventLog += prefix() + offense.abbr + " going for 2.\n";
            state.lastPlayLog = offense.abbr + " going for 2.";
            // League sim / both-AI: snap immediately. If user defends, pause for coverage.
            if (!defense.userControlled) {
                PlayCall call = aiCaller.choose(offense, defense, state);
                executeSnap(call);
            }
        } else {
            state.tryAwaitingChoice = false;
            state.tryIsTwoPoint = false;
            kickXp();
            finishTrySequence();
        }
    }

    private void applyTwoPointResult(PlayResult result) {
        if (result.touchdown && !result.returnTd) {
            addScore(2);
            result.scoreXp = false;
            gameEventLog += prefix() + "2-point conversion good.\n";
            state.lastPlayLog = result.logLine != null ? result.logLine : "2-point conversion good.";
            finishTrySequence();
            return;
        }
        if (result.returnTd) {
            // Defense returns the try for 2 (NCAA)
            state.possessionHome = !state.possessionHome;
            addScore(2);
            gameEventLog += prefix() + "Defense scores 2 on the return!\n";
            state.lastPlayLog = "Defense scores 2 on the return!";
            // Possession is now with the team that just scored on the return — they kick
            finishTrySequence();
            return;
        }
        gameEventLog += prefix() + "2-point conversion failed.\n";
        state.lastPlayLog = "2-point conversion failed.";
        // Restore possession to the team that scored the TD (still has the ball for kickoff)
        finishTrySequence();
    }

    private void finishTrySequence() {
        state.clearTry();
        if (!state.playingOT) kickoffAfterScore();
        else resetForOT();
    }

    private void kickXp() {
        Team offense = state.possessionHome ? homeTeam : awayTeam;
        Player k = offense.getK(0);
        k.seasonStats.xpAtt++;
        if (playerGameStats != null) {
            playerGameStats.line(k).xpAtt++;
        }
        int kac = k.ratings != null ? k.ratings.kac : 70;
        if (rng.nextDouble() * 100 < 92 + (kac - 70) / 5.0) {
            addScore(1);
            k.seasonStats.xpMade++;
            if (playerGameStats != null) {
                playerGameStats.line(k).xpMade++;
            }
            gameEventLog += prefix() + offense.abbr + " XP good.\n";
            state.lastPlayLog = offense.abbr + " XP good.";
        } else {
            gameEventLog += prefix() + offense.abbr + " XP missed.\n";
            state.lastPlayLog = offense.abbr + " XP missed.";
        }
    }

    private void addScore(int pts) {
        if (state.possessionHome) state.homeScore += pts;
        else state.awayScore += pts;
        addPointsQuarter(pts, state.possessionHome);
    }

    private void addPointsQuarter(int points, boolean home) {
        int idx;
        if (state.playingOT) {
            idx = Math.min(9, 3 + state.numOT);
        } else if (state.gameTime > 2700) idx = 0;
        else if (state.gameTime > 1800) idx = 1;
        else if (state.gameTime > 900) idx = 2;
        else idx = 3;
        if (home) state.homeQScore[idx] += points;
        else state.awayQScore[idx] += points;
    }

    private void enterOT() {
        state.playingOT = true;
        state.phase = GamePhase.OT;
        state.numOT = 1;
        state.gameTime = -1;
        state.possessionHome = false;
        state.yardLine = 75;
        state.down = 1;
        state.yardsNeed = 10;
        state.bottomOT = false;
        state.resetTimeoutsForOt();
        state.halfUnderway = true;
        resetDrive(75);
        gameEventLog += prefix() + "OVERTIME!\n";
    }

    private void resetForOT() {
        if (state.bottomOT && state.homeScore == state.awayScore) {
            state.yardLine = 75;
            state.yardsNeed = 10;
            state.down = 1;
            state.numOT++;
            state.possessionHome = (state.numOT % 2) == 0;
            state.bottomOT = false;
            state.resetTimeoutsForOt();
            resetDrive(75);
        } else if (!state.bottomOT) {
            state.possessionHome = !state.possessionHome;
            state.yardLine = 75;
            state.yardsNeed = 10;
            state.down = 1;
            state.bottomOT = true;
            resetDrive(75);
        } else {
            state.playingOT = false;
            state.gameOver = true;
            gameEventLog += prefix() + "OT complete.\n";
        }
    }

    private void syncPublicFields() {
        if (state == null) return;
        homeScore = state.homeScore;
        awayScore = state.awayScore;
        homeQScore = state.homeQScore;
        awayQScore = state.awayQScore;
        homeYards = state.homeYards;
        awayYards = state.awayYards;
        homeTOs = state.homeTOs;
        awayTOs = state.awayTOs;
        numOT = state.numOT;
    }

    private String prefix() {
        syncPublicFields();
        int q = state.quarter();
        String clock = state.clockDisplay();
        return "\n" + q + "Q " + clock + " " + awayTeam.abbr + " " + awayScore + " - "
                + homeTeam.abbr + " " + homeScore + "\n";
    }

    private static String ordinal(int d) {
        if (d == 1) return "1st";
        if (d == 2) return "2nd";
        if (d == 3) return "3rd";
        return d + "th";
    }

    public int getPassYards(boolean home) {
        Team t = home ? awayTeam : homeTeam; // opp pass yards tracked from other team's pass
        // Prefer team season accumulators already updated during plays via offense.teamPassYards —
        // for box score use difference of teamPassYards isn't per-game. Approximate from homeYards split.
        return home ? Math.max(0, homeYards * 55 / 100) : Math.max(0, awayYards * 55 / 100);
    }

    public int getRushYards(boolean home) {
        return home ? Math.max(0, homeYards * 45 / 100) : Math.max(0, awayYards * 45 / 100);
    }

    public String[] getGameSummaryStr() {
        syncPublicFields();
        String left = awayTeam.abbr + "\n" + awayScore + "\nPass Yds ~" + getPassYards(false)
                + "\nRush Yds ~" + getRushYards(false) + "\nTO " + awayTOs;
        String center = gameName + (numOT > 0 ? "\nOT" + numOT : "") + "\n\nFINAL";
        String right = homeTeam.abbr + "\n" + homeScore + "\nPass Yds ~" + getPassYards(true)
                + "\nRush Yds ~" + getRushYards(true) + "\nTO " + homeTOs;
        return new String[]{left, center, right, gameEventLog != null ? gameEventLog : ""};
    }

    public String[] getGameScoutStr() {
        String left = awayTeam.abbr + "\nOff: " + awayTeam.offPhilosophy.displayName
                + "\nDef: " + awayTeam.defSystem.displayName
                + "\nOff Tal " + awayTeam.getOffTalent()
                + "\nDef Tal " + awayTeam.getDefTalent()
                + "\nProgram Power " + awayTeam.programProfile.programPower;
        int rivalry = rivalryStrength();
        String center = gameName + "\n\nSCOUT"
                + (rivalry > 0
                ? "\n" + Rivalry.band(rivalry) + " rivalry (" + rivalry + ")"
                : "");
        String right = homeTeam.abbr + "\nOff: " + homeTeam.offPhilosophy.displayName
                + "\nDef: " + homeTeam.defSystem.displayName
                + "\nOff Tal " + homeTeam.getOffTalent()
                + "\nDef Tal " + homeTeam.getDefTalent()
                + "\nProgram Power " + homeTeam.programProfile.programPower;
        String notes = "Philosophies and fronts shape personnel and playcalling.\n"
                + "Set your Offense Philosophy and Defense System on the Team tab.";
        if (rivalry >= Rivalry.MOMENTUM_THRESHOLD) {
            int swing = rivalry >= Rivalry.HOT_THRESHOLD ? 2 : 1;
            notes += "\nRivalry stakes: about ±" + swing
                    + " momentum when the programs are competitive.";
        }
        if (contractId != null && homeTeam.league != null && homeTeam.league.oocContracts != null) {
            OocContractGame cg = homeTeam.league.oocContracts.findById(contractId) != null
                    ? homeTeam.league.oocContracts.findById(contractId)
                    .gameForYear(homeTeam.league.getYear())
                    : null;
            if (cg != null && cg.guarantee > 0) {
                notes += "\nBuy-game guarantee: " + NilMoney.format(cg.guarantee)
                        + " paid by " + cg.homeAbbr + " to " + cg.awayAbbr + " when played.";
            }
        }
        return new String[]{left, center, right, notes};
    }
}
