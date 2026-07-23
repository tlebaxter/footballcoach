package CFBsimPack;

import CFBsimPack.engine.AiPlayCaller;
import CFBsimPack.engine.AutoSimUntil;
import CFBsimPack.engine.CoverageCall;
import CFBsimPack.engine.GamePhase;
import CFBsimPack.engine.GameSituation;
import CFBsimPack.engine.GameState;
import CFBsimPack.engine.OffensePlay;
import CFBsimPack.engine.PlayCall;
import CFBsimPack.engine.PlayResolver;
import CFBsimPack.engine.PlayResult;
import CFBsimPack.engine.PlayerGameStats;
import CFBsimPack.engine.TempoCall;

import java.io.Serializable;
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

    public int homeScore;
    public int[] homeQScore;
    public int awayScore;
    public int[] awayQScore;
    public int homeYards;
    public int awayYards;
    public int numOT;
    public int homeTOs;
    public int awayTOs;

    // Legacy slot arrays kept empty-compatible for any leftover UI; prefer playerGameStats
    public int[] HomeQBStats = new int[6];
    public int[] AwayQBStats = new int[6];
    public int[] HomeRB1Stats = new int[4];
    public int[] HomeRB2Stats = new int[4];
    public int[] AwayRB1Stats = new int[4];
    public int[] AwayRB2Stats = new int[4];
    public int[] HomeWR1Stats = new int[6];
    public int[] HomeWR2Stats = new int[6];
    public int[] HomeWR3Stats = new int[6];
    public int[] AwayWR1Stats = new int[6];
    public int[] AwayWR2Stats = new int[6];
    public int[] AwayWR3Stats = new int[6];
    public int[] HomeKStats = new int[6];
    public int[] AwayKStats = new int[6];

    public transient PlayerGameStats playerGameStats = new PlayerGameStats();
    public transient GameState state;
    public transient String gameEventLog = "";

    private transient PlayResolver resolver;
    private transient AiPlayCaller aiCaller;
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
        if (homeTeam.rivalTeam.equals(awayTeam.abbr) || awayTeam.rivalTeam.equals(homeTeam.abbr)) {
            if (gameName.equals("In Conf")) gameName = "Rivalry Game";
            else if (gameName.equals("OOC")) gameName = "Rivalry Game OOC";
        }
    }

    public void setRandom(Random random) {
        this.rng = random != null ? random : new Random();
        this.resolver = new PlayResolver(this.rng);
        this.aiCaller = new AiPlayCaller(this.rng);
    }

    private void ensureEngine() {
        if (rng == null) setRandom(new Random());
        if (playerGameStats == null) playerGameStats = new PlayerGameStats();
    }

    public void startGame() {
        if (hasPlayed) return;
        ensureEngine();
        DepthChart.applySystems(homeTeam);
        DepthChart.applySystems(awayTeam);
        state = new GameState();
        state.gameTime = GameState.REG_SECONDS;
        state.possessionHome = true;
        state.yardLine = 25;
        state.down = 1;
        state.yardsNeed = 10;
        state.phase = GamePhase.REGULATION;
        started = true;
        prevQuarter = 1;
        gameEventLog = "LOG: #" + awayTeam.rankTeamPollScore + " " + awayTeam.abbr + " (" + awayTeam.wins + "-" + awayTeam.losses
                + ") @ #" + homeTeam.rankTeamPollScore + " " + homeTeam.abbr + " (" + homeTeam.wins + "-" + homeTeam.losses + ")\n"
                + "---------------------------------------------------------\n\n"
                + awayTeam.abbr + " Offense: " + awayTeam.offPhilosophy.displayName + " / " + awayTeam.teamStratOff.getStratName() + "\n"
                + awayTeam.abbr + " Defense: " + awayTeam.defSystem.displayName + " / " + awayTeam.teamStratDef.getStratName() + "\n"
                + homeTeam.abbr + " Offense: " + homeTeam.offPhilosophy.displayName + " / " + homeTeam.teamStratOff.getStratName() + "\n"
                + homeTeam.abbr + " Defense: " + homeTeam.defSystem.displayName + " / " + homeTeam.teamStratDef.getStratName() + "\n";
    }

    public GameSituation getSituation() {
        if (state == null) startGame();
        Team user = homeTeam.userControlled ? homeTeam : (awayTeam.userControlled ? awayTeam : null);
        boolean userOff = user != null && ((state.possessionHome && user == homeTeam) || (!state.possessionHome && user == awayTeam));
        String dd = ordinal(state.down) + " & " + state.yardsNeed + " · "
                + (state.yardLine <= 50 ? "OWN " + state.yardLine : "OPP " + (100 - state.yardLine));
        return new GameSituation(
                state.homeScore, state.awayScore, homeTeam.abbr, awayTeam.abbr,
                state.quarter(), state.clockDisplay(), state.down, state.yardsNeed, state.yardLine,
                state.possessionHome, state.homeTimeouts, state.awayTimeouts,
                state.playingOT, state.gameOver || hasPlayed, userOff,
                state.lastPlayLog, dd
        );
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

        // Turnover on downs before snap
        if (state.down > 4) {
            return handleTurnoverOnDowns();
        }

        Team offense = state.possessionHome ? homeTeam : awayTeam;
        Team defense = state.possessionHome ? awayTeam : homeTeam;
        if (call == null) {
            call = aiCaller.choose(offense, defense, state);
        }

        int qBefore = state.quarter();
        PlayResult result = resolver.resolve(homeTeam, awayTeam, state, call);
        applyResult(result, call);
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
            state.resetTimeoutsForHalf();
        }
        return result;
    }

    public void autoSimUntil(AutoSimUntil until) {
        if (hasPlayed) return;
        if (state == null) startGame();
        int startPossFlip = 0;
        boolean startHomePoss = state.possessionHome;
        int startQuarter = state.quarter();
        boolean startFirstHalf = state.gameTime > 1800;

        int guard = 0;
        while (!state.gameOver && !hasPlayed && guard++ < 800) {
            Team offense = state.possessionHome ? homeTeam : awayTeam;
            Team defense = state.possessionHome ? awayTeam : homeTeam;
            PlayCall call = aiCaller.choose(offense, defense, state);
            PlayResult r = executeSnap(call);

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
        int guard = 0;
        while (!state.gameOver && guard++ < 900) {
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

        if (homeTeam.rivalTeam.equals(awayTeam.abbr) || awayTeam.rivalTeam.equals(homeTeam.abbr)) {
            if (homeScore > awayScore) homeTeam.wonRivalryGame = true;
            else awayTeam.wonRivalryGame = true;
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

        if (result.touchdown) {
            addScore(6);
            kickXp();
            if (!state.playingOT) kickoffAfterScore();
            else resetForOT();
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
            flipPossession(result.playType == OffensePlay.FIELD_GOAL && !result.scoreFg);
            return;
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
    }

    private void kickoffAfterScore() {
        state.possessionHome = !state.possessionHome;
        state.yardLine = 25;
        state.down = 1;
        state.yardsNeed = 10;
    }

    private void freeKick() {
        // Receiving team after safety
        state.possessionHome = !state.possessionHome;
        state.yardLine = 20;
        state.down = 1;
        state.yardsNeed = 10;
    }

    private void kickXp() {
        Team offense = state.possessionHome ? homeTeam : awayTeam;
        PlayerK k = offense.getK(0);
        k.statsXPAtt++;
        if (rng.nextDouble() * 100 < 92 + (k.ratKickAcc - 70) / 5.0) {
            addScore(1);
            k.statsXPMade++;
            gameEventLog += prefix() + offense.abbr + " XP good.\n";
        } else {
            gameEventLog += prefix() + offense.abbr + " XP missed.\n";
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
        } else if (!state.bottomOT) {
            state.possessionHome = !state.possessionHome;
            state.yardLine = 75;
            state.yardsNeed = 10;
            state.down = 1;
            state.bottomOT = true;
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
                + "\nPrestige " + awayTeam.teamPrestige;
        String center = gameName + "\n\nSCOUT";
        String right = homeTeam.abbr + "\nOff: " + homeTeam.offPhilosophy.displayName
                + "\nDef: " + homeTeam.defSystem.displayName
                + "\nOff Tal " + homeTeam.getOffTalent()
                + "\nDef Tal " + homeTeam.getDefTalent()
                + "\nPrestige " + homeTeam.teamPrestige;
        String notes = "Philosophies and fronts shape personnel and playcalling.\n"
                + "Set your system on the Team tab. Weekly game plan still applies.";
        return new String[]{left, center, right, notes};
    }
}
