package CFBsimPack.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Read-only snapshot for live coach UI.
 */
public final class GameSituation {
    public final int homeScore;
    public final int awayScore;
    public final String homeAbbr;
    public final String awayAbbr;
    public final String homeName;
    public final String awayName;
    public final int homeRank;
    public final int awayRank;
    public final int quarter;
    public final String clock;
    /** Seconds remaining in regulation (OT uses -1). */
    public final int gameTime;
    /** Seconds remaining in the current quarter (0 in OT). */
    public final int clockInQuarter;
    public final int down;
    public final int distance;
    public final int yardLine;
    public final boolean possessionHome;
    public final int homeTimeouts;
    public final int awayTimeouts;
    /** Max timeout pips for this period (3 half / 1 OT). */
    public final int timeoutsMax;
    public final boolean playingOT;
    public final boolean gameOver;
    public final boolean userOnOffense;
    public final String lastPlay;
    /** Composite label for legacy callers (down · ball-on or phase string). */
    public final String downDistanceLabel;
    /** Short down cell: {@code 3rd & 10}, {@code 1st & Goal}, or phase. */
    public final String downLabel;
    /** ESPN-style spot: {@code LAN 13}, {@code 50}, or phase placeholder. */
    public final String ballOnLabel;
    /** Abbr of the team with the ball. */
    public final String possessionAbbr;
    /**
     * Offense-perspective yard of the first-down marker (0–100), or -1 when not a
     * normal scrimmage situation.
     */
    public final int firstDownYard;
    /** {@code RUNNING}, {@code STOPPED}, or {@code 10S RUNOFF}. */
    public final String clockStatusLabel;
    public final int drivePlayCount;
    public final int driveNetYards;
    public final int driveTimeOfPossessionSec;
    /** {@code 4 plays, 32 yds, 1:45} or {@code —}. */
    public final String driveSummary;
    public final int homeYards;
    public final int awayYards;
    public final int homeTOs;
    public final int awayTOs;
    public final int[] homeQScore;
    public final int[] awayQScore;
    /** Current drive yard-line path (offense perspective 0–100). */
    public final List<Integer> drivePath;
    public final List<PlayLogEntry> playLog;
    public final List<BoxScoreLine> boxScore;
    public final String lastOffenseConceptId;
    public final String lastDefenseConceptId;
    public final boolean pendingKickoff;
    public final boolean freeKick;
    public final boolean specialTeamsDown;
    public final String userPuntReturnerName;
    public final String userKickReturnerName;
    public final boolean awaitingCoinToss;
    public final boolean homeWonToss;
    public final boolean homeDefendsLeft;
    public final boolean userWonToss;
    public final boolean pendingTry;
    public final boolean tryAwaitingChoice;
    public final boolean tryIsTwoPoint;
    /** User offense scored the TD and must choose Kick XP vs Go for 2. */
    public final boolean userChoosesTry;
    /** Opponent is going for 2; user picks defense. */
    public final boolean userDefendsTwoPoint;
    /** User team may call a timeout under NCAA-style rules. */
    public final boolean canCallTimeout;
    /** Game clock is running between plays (pending tempo runoff on next snap). */
    public final boolean clockRunning;
    /** NCAA 10-second runoff pending; timeout avoids it. */
    public final boolean pendingTenSecondRunoff;
    /** Live home-crowd intensity (0–100). */
    public final int crowdEnergy;
    /** Quiet / Steady / Loud / Electric / Hostile. */
    public final String crowdBand;

    public GameSituation(
            int homeScore, int awayScore, String homeAbbr, String awayAbbr,
            String homeName, String awayName,
            int homeRank, int awayRank,
            int quarter, String clock, int gameTime, int clockInQuarter,
            int down, int distance, int yardLine,
            boolean possessionHome, int homeTimeouts, int awayTimeouts, int timeoutsMax,
            boolean playingOT, boolean gameOver, boolean userOnOffense,
            String lastPlay, String downDistanceLabel,
            String downLabel, String ballOnLabel, String possessionAbbr,
            int firstDownYard, String clockStatusLabel,
            int drivePlayCount, int driveNetYards, int driveTimeOfPossessionSec, String driveSummary,
            int homeYards, int awayYards, int homeTOs, int awayTOs,
            int[] homeQScore, int[] awayQScore,
            List<Integer> drivePath, List<PlayLogEntry> playLog, List<BoxScoreLine> boxScore,
            String lastOffenseConceptId, String lastDefenseConceptId,
            boolean pendingKickoff, boolean freeKick, boolean specialTeamsDown,
            String userPuntReturnerName, String userKickReturnerName,
            boolean awaitingCoinToss, boolean homeWonToss, boolean homeDefendsLeft, boolean userWonToss,
            boolean pendingTry, boolean tryAwaitingChoice, boolean tryIsTwoPoint,
            boolean userChoosesTry, boolean userDefendsTwoPoint,
            boolean canCallTimeout, boolean clockRunning, boolean pendingTenSecondRunoff,
            int crowdEnergy, String crowdBand
    ) {
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.homeAbbr = homeAbbr;
        this.awayAbbr = awayAbbr;
        this.homeName = homeName;
        this.awayName = awayName;
        this.homeRank = homeRank;
        this.awayRank = awayRank;
        this.quarter = quarter;
        this.clock = clock;
        this.gameTime = gameTime;
        this.clockInQuarter = clockInQuarter;
        this.down = down;
        this.distance = distance;
        this.yardLine = yardLine;
        this.possessionHome = possessionHome;
        this.homeTimeouts = homeTimeouts;
        this.awayTimeouts = awayTimeouts;
        this.timeoutsMax = timeoutsMax;
        this.playingOT = playingOT;
        this.gameOver = gameOver;
        this.userOnOffense = userOnOffense;
        this.lastPlay = lastPlay;
        this.downDistanceLabel = downDistanceLabel;
        this.downLabel = downLabel != null ? downLabel : "";
        this.ballOnLabel = ballOnLabel != null ? ballOnLabel : "";
        this.possessionAbbr = possessionAbbr != null ? possessionAbbr : "";
        this.firstDownYard = firstDownYard;
        this.clockStatusLabel = clockStatusLabel != null ? clockStatusLabel : "STOPPED";
        this.drivePlayCount = drivePlayCount;
        this.driveNetYards = driveNetYards;
        this.driveTimeOfPossessionSec = driveTimeOfPossessionSec;
        this.driveSummary = driveSummary != null ? driveSummary : "—";
        this.homeYards = homeYards;
        this.awayYards = awayYards;
        this.homeTOs = homeTOs;
        this.awayTOs = awayTOs;
        this.homeQScore = homeQScore != null ? homeQScore.clone() : new int[10];
        this.awayQScore = awayQScore != null ? awayQScore.clone() : new int[10];
        this.drivePath = drivePath != null
                ? Collections.unmodifiableList(new ArrayList<>(drivePath))
                : Collections.emptyList();
        this.playLog = playLog != null
                ? Collections.unmodifiableList(new ArrayList<>(playLog))
                : Collections.emptyList();
        this.boxScore = boxScore != null
                ? Collections.unmodifiableList(new ArrayList<>(boxScore))
                : Collections.emptyList();
        this.lastOffenseConceptId = lastOffenseConceptId;
        this.lastDefenseConceptId = lastDefenseConceptId;
        this.pendingKickoff = pendingKickoff;
        this.freeKick = freeKick;
        this.specialTeamsDown = specialTeamsDown;
        this.userPuntReturnerName = userPuntReturnerName;
        this.userKickReturnerName = userKickReturnerName;
        this.awaitingCoinToss = awaitingCoinToss;
        this.homeWonToss = homeWonToss;
        this.homeDefendsLeft = homeDefendsLeft;
        this.userWonToss = userWonToss;
        this.pendingTry = pendingTry;
        this.tryAwaitingChoice = tryAwaitingChoice;
        this.tryIsTwoPoint = tryIsTwoPoint;
        this.userChoosesTry = userChoosesTry;
        this.userDefendsTwoPoint = userDefendsTwoPoint;
        this.canCallTimeout = canCallTimeout;
        this.clockRunning = clockRunning;
        this.pendingTenSecondRunoff = pendingTenSecondRunoff;
        this.crowdEnergy = crowdEnergy;
        this.crowdBand = crowdBand != null ? crowdBand : AtmosphereEngine.band(crowdEnergy);
    }
}
