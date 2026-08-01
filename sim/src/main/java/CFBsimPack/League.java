package CFBsimPack;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/**
 * League class for the 2026 FBS configuration.
 * @author Achi
 */
public class League {
    public static final int FIRST_SEASON_YEAR = 2026;
    /** Calendar weeks in the regular season (12 games + 1 bye). */
    public static final int REGULAR_SEASON_WEEKS = 13;
    /** Games each team plays in the regular season. */
    public static final int REGULAR_SEASON_GAMES = 12;
    /** Conference championship week index (after regular season). */
    public static final int WEEK_CCG = 13;
    /** Schedule CFP field + bowls (same week as CCG play). */
    public static final int WEEK_POSTSEASON_SCHEDULE = 13;
    /** @deprecated use {@link #WEEK_POSTSEASON_SCHEDULE} */
    @Deprecated
    public static final int WEEK_BOWL_SCHEDULE = WEEK_POSTSEASON_SCHEDULE;
    /** CFP first round + non-CFP bowls. */
    public static final int WEEK_CFP_FIRST_ROUND = 14;
    /** CFP quarterfinals. */
    public static final int WEEK_CFP_QUARTERS = 15;
    /** CFP semifinals. */
    public static final int WEEK_CFP_SEMIS = 16;
    /** National championship. */
    public static final int WEEK_NCG = 17;
    /** Season complete; recruiting begins. */
    public static final int WEEK_SEASON_END = 18;
    //Lists of conferences/teams
    public ArrayList<String[]> leagueHistory;
    public ArrayList<String> heismanHistory;
    public ArrayList<Conference> conferences;
    public ArrayList<Team> teamList;
    public ArrayList<String> nameList;
    public ArrayList<String> lastNameList;

    public LeagueRecords leagueRecords;
    public LeagueRecords userTeamRecords;
    public TeamStreak longestWinStreak;
    public TeamStreak yearStartLongestWinStreak;
    public TeamStreak longestActiveWinStreak;


    // Current week: 0 … WEEK_SEASON_END
    public int currentWeek;

    // Postseason
    public boolean hasScheduledBowls;
    /** CFP field in seed order (#1 at index 0). */
    public ArrayList<Team> cfpField;
    /** Teams among {@link #cfpField} that earned automatic conference-champ bids. */
    public HashSet<Team> cfpAutoBids;
    public Game[] cfpFirstRound;
    public Game[] cfpQuarters;
    public Game[] cfpSemis;
    public Game ncg;
    public Game[] bowlGames;

    //User Team
    public Team userTeam;

    /** Retention / portal / shared HS class for the current offseason. */
    public LeagueOffseason offseason;

    /** True when this league was loaded from a mid-offseason save. */
    public boolean loadedInOffseason;
    public OffseasonSession.Phase loadedOffseasonPhase;

    /** Multi-year OOC buy games and home-and-homes. */
    public OocContractBook oocContracts;

    boolean heismanDecided;
    Player heisman;
    ArrayList<Player> heismanCandidates;
    private String heismanWinnerStrFull;

    ArrayList<Player> allAmericans;
    private String allAmericanStr;

    public static final String[] donationNames = {"Mark Eeslee", "Lee Sin", "Brent Uttwipe", "Gabriel Kemble",
            "Jon Stupak", "Kiergan Ren", "Dean Steinkuhler", "Declan Greally", "Parks Wilson", "Darren Ryder"};

    /**
     * Creates League from name pools + FBS team JSON seed, then schedules games.
     */
    public League(List<String> firstNames, List<String> lastNames, String teamsJson) {
        initShell(firstNames, lastNames);
        LeagueDataLoader.load2026Teams(this, teamsJson);
        setUpSeasonSchedule();
    }

    /**
     * Convenience for tests that still pass comma-separated name lists.
     */
    public League(String namesCSV, String lastNamesCSV, String teamsJson) {
        this(NamePools.splitCsv(namesCSV), NamePools.splitCsv(lastNamesCSV), teamsJson);
    }

    /**
     * Empty league for typed JSON hydrate: names loaded, no teams/schedule.
     */
    public static League createEmptyShell(List<String> firstNames, List<String> lastNames) {
        League league = new League();
        league.initShell(firstNames, lastNames);
        league.loadedInOffseason = false;
        league.loadedOffseasonPhase = OffseasonSession.Phase.RETENTION;
        league.teamList = new ArrayList<>();
        league.conferences = new ArrayList<>();
        return league;
    }

    /** @see #createEmptyShell(List, List) */
    public static League createEmptyShell(String namesCSV, String lastNamesCSV) {
        return createEmptyShell(NamePools.splitCsv(namesCSV), NamePools.splitCsv(lastNamesCSV));
    }

    private League() {
        // shell via createEmptyShell / initShell
    }

    private void initShell(List<String> firstNames, List<String> lastNames) {
        heismanDecided = false;
        hasScheduledBowls = false;
        cfpField = new ArrayList<>();
        cfpAutoBids = new HashSet<>();
        bowlGames = new Game[0];
        leagueHistory = new ArrayList<String[]>();
        heismanHistory = new ArrayList<String>();
        currentWeek = 0;
        conferences = new ArrayList<Conference>();
        teamList = new ArrayList<Team>();
        allAmericans = new ArrayList<Player>();

        leagueRecords = new LeagueRecords();
        userTeamRecords = new LeagueRecords();
        longestWinStreak = new TeamStreak(FIRST_SEASON_YEAR, FIRST_SEASON_YEAR, 0, "XXX");
        yearStartLongestWinStreak = new TeamStreak(FIRST_SEASON_YEAR, FIRST_SEASON_YEAR, 0, "XXX");
        longestActiveWinStreak = new TeamStreak(FIRST_SEASON_YEAR, FIRST_SEASON_YEAR, 0, "XXX");
        oocContracts = new OocContractBook(this);

        nameList = new ArrayList<>();
        if (firstNames != null) {
            for (String n : firstNames) {
                if (n != null && !n.trim().isEmpty()) {
                    nameList.add(n.trim());
                }
            }
        }
        lastNameList = new ArrayList<>();
        if (lastNames != null) {
            for (String n : lastNames) {
                if (n != null && !n.trim().isEmpty()) {
                    lastNameList.add(n.trim());
                }
            }
        }
    }

    /**
     * Test/helper constructor that builds conference + byes but leaves OOC open.
     */
    League(String namesCSV, String lastNamesCSV, String teamsJson, boolean fillOoc) {
        this(NamePools.splitCsv(namesCSV), NamePools.splitCsv(lastNamesCSV), teamsJson, fillOoc);
    }

    private League(
            List<String> firstNames,
            List<String> lastNames,
            String teamsJson,
            boolean fillOoc) {
        initShell(firstNames, lastNames);
        LeagueDataLoader.load2026Teams(this, teamsJson);
        if (fillOoc) {
            setUpSeasonSchedule();
        } else {
            prepareConferenceScheduleOnly();
        }
    }

    /** Conference games only — no byes, no OOC. For diagnostics. */
    void prepareConferenceScheduleOnly() {
        for (Team team : teamList) {
            team.gameSchedule.clear();
            team.evenYearHomeOpp = "";
            team.byeWeek = -1;
            for (int week = 0; week < REGULAR_SEASON_WEEKS; week++) {
                team.gameSchedule.add(null);
            }
        }
        for (Conference conference : conferences) {
            conference.resetSeason();
            conference.setUpSchedule();
        }
    }

    private void setUpSeasonSchedule() {
        prepareSeasonSchedule();
        completeOocSchedule();
    }

    /**
     * Builds conference games and assigns immovable bye weeks; leaves OOC slots open.
     */
    public void prepareSeasonSchedule() {
        for (Team team : teamList) {
            team.gameSchedule.clear();
            team.evenYearHomeOpp = "";
            team.byeWeek = -1;
            for (int week = 0; week < REGULAR_SEASON_WEEKS; week++) {
                team.gameSchedule.add(null);
            }
        }
        for (Conference conference : conferences) {
            conference.resetSeason();
            conference.setUpSchedule();
        }
        ByeWeekAssigner.assign(teamList);
        if (oocContracts != null) {
            oocContracts.materializeCurrentYear();
        }
    }

    /**
     * Fills remaining open (non-bye) weeks with OOC games, honoring any user-placed matchups.
     */
    public void completeOocSchedule() {
        OocScheduleBuilder.scheduleRemaining(teamList);
        if (oocContracts != null) {
            oocContracts.autoSignFutureDeals(teamList);
        }
    }

    /**
     * Advances the calendar year and builds conference + bye weeks without auto-filling OOC.
     * Used when the user will pick their OOC schedule before the CPU fills the rest.
     */
    public void advanceSeasonForScheduling() {
        currentWeek = 0;
        for (int t = 0; t < teamList.size(); ++t) {
            teamList.get(t).advanceSeason();
        }
        RivalryDynamics.formNewRivalries(this);
        advanceSeasonWinStreaks();
        if (oocContracts != null) {
            oocContracts.enforceBreaches();
        }
        prepareSeasonSchedule();
        hasScheduledBowls = false;
        cfpField = new ArrayList<>();
        cfpAutoBids = new HashSet<>();
        cfpFirstRound = null;
        cfpQuarters = null;
        cfpSemis = null;
        ncg = null;
        bowlGames = new Game[0];
    }

    /**
     * Get conference number from its name.
     * @param conf conference name
     * @return conference index
     */
    public int getConfNumber(String conf) {
        for (int index = 0; index < conferences.size(); index++) {
            if (conferences.get(index).confName.equals(conf)) {
                return index;
            }
        }
        throw new IllegalArgumentException("Unknown conference: " + conf);
    }

    /** Get or create a conference by name (typed hydrate / save load). */
    public Conference getOrCreateConference(String name) {
        for (Conference conference : conferences) {
            if (conference.confName.equals(name)) {
                return conference;
            }
        }
        Conference conference = new Conference(name, this, !"Independents".equals(name));
        conferences.add(conference);
        return conference;
    }

    /** Exact abbr lookup; {@code null} if missing (unlike {@link #findTeamAbbr}). */
    public Team findTeamAbbrOrNull(String abbr) {
        if (abbr == null) {
            return null;
        }
        for (int i = 0; i < teamList.size(); i++) {
            if (teamList.get(i).abbr.equals(abbr)) {
                return teamList.get(i);
            }
        }
        return null;
    }

    /** Exact conference name lookup; {@code null} if missing. */
    public Conference findConferenceOrNull(String name) {
        if (name == null) {
            return null;
        }
        for (int i = 0; i < conferences.size(); i++) {
            if (conferences.get(i).confName.equals(name)) {
                return conferences.get(i);
            }
        }
        return null;
    }

    /** Clamp each conference week to the restored {@link #currentWeek}. */
    public void syncConferenceWeeksFromCurrentWeek() {
        int confWeek = currentWeek;
        if (confWeek > WEEK_CCG) {
            confWeek = WEEK_CCG;
        }
        if (confWeek < 0) {
            confWeek = 0;
        }
        for (Conference conference : conferences) {
            conference.week = confWeek;
        }
    }

    /** Apply a saved box score onto a game (typed hydrate). */
    public static void applyGameResult(
            Game game,
            int homeScore,
            int awayScore,
            int homeYards,
            int awayYards,
            int homeTOs,
            int awayTOs,
            int numOT,
            int[] homeQScore,
            int[] awayQScore) {
        if (game == null) {
            return;
        }
        game.homeScore = homeScore;
        game.awayScore = awayScore;
        game.homeYards = homeYards;
        game.awayYards = awayYards;
        game.homeTOs = homeTOs;
        game.awayTOs = awayTOs;
        game.numOT = numOT;
        copyQuarterScores(game.homeQScore, homeQScore);
        copyQuarterScores(game.awayQScore, awayQScore);
        game.hasPlayed = true;
    }

    private static void copyQuarterScores(int[] dest, int[] src) {
        if (dest == null) {
            return;
        }
        for (int i = 0; i < dest.length; i++) {
            dest[i] = 0;
        }
        if (src == null) {
            return;
        }
        for (int i = 0; i < dest.length && i < src.length; i++) {
            dest[i] = src[i];
        }
    }

     /**
     * Plays week. If normal week, handled by conferences. If postseason, handled here.
     */
    public void playWeek() {
        if ( currentWeek <= WEEK_CCG ) {
            for (int i = 0; i < conferences.size(); ++i) {
                conferences.get(i).playWeek();
            }
        }

        if ( currentWeek == WEEK_POSTSEASON_SCHEDULE ) {
            for (int i = 0; i < teamList.size(); ++i) {
                teamList.get(i).updatePollScore();
            }
            Collections.sort( teamList, new TeamCompPoll() );
            schedBowlGames();
        } else if ( currentWeek == WEEK_CFP_FIRST_ROUND ) {
            ArrayList<Player> heismans = getHeisman();
            heismanHistory.add(heismans.get(0).position + " " + heismans.get(0).getInitialName() + " [" + heismans.get(0).getYrStr() + "], "
                    + heismans.get(0).team.abbr + " (" + heismans.get(0).team.wins + "-" + heismans.get(0).team.losses + ")");
            playCfpFirstRoundAndBowls();
        } else if ( currentWeek == WEEK_CFP_QUARTERS ) {
            playCfpQuarters();
        } else if ( currentWeek == WEEK_CFP_SEMIS ) {
            playCfpSemis();
        } else if ( currentWeek == WEEK_NCG ) {
            playNationalChampionship();
        }

        setTeamRanks();
        updateLongestActiveWinStreak();

        currentWeek++;
    }
    
    /**
     * Selects the 12-team CFP field, schedules first round + conference-tied bowls.
     */
    public void schedBowlGames() {
        for (int i = 0; i < teamList.size(); ++i) {
            teamList.get(i).updatePollScore();
        }
        Collections.sort(teamList, new TeamCompPoll());

        Postseason.CfpSelection selection = Postseason.selectCfpField(teamList);
        cfpField = new ArrayList<>(selection.field);
        cfpAutoBids = new HashSet<>(selection.autoBids);
        cfpFirstRound = Postseason.scheduleFirstRound(cfpField);
        HashSet<Team> cfpSet = new HashSet<>(cfpField);
        bowlGames = Postseason.matchBowls(teamList, cfpSet);
        cfpQuarters = null;
        cfpSemis = null;
        ncg = null;
        hasScheduledBowls = true;
    }

    /** Plays CFP first round + bowls; schedules quarterfinals. */
    public void playCfpFirstRoundAndBowls() {
        if (bowlGames != null) {
            for (Game g : bowlGames) {
                if (g != null) {
                    Postseason.playBowl(g);
                }
            }
        }
        if (cfpFirstRound != null) {
            for (Game g : cfpFirstRound) {
                if (g != null) {
                    Postseason.playCfpGame(g, "FRW", "FRL");
                }
            }
        }
        List<Team> alive = Postseason.byesPlusWinners(cfpField, cfpFirstRound);
        cfpQuarters = Postseason.scheduleReseededRound(
                alive, cfpField, Postseason.CFP_QUARTER_HOSTS, "CFP Quarter");
    }

    /** Plays CFP quarters; schedules semis. */
    public void playCfpQuarters() {
        if (cfpQuarters != null) {
            for (Game g : cfpQuarters) {
                if (g != null) {
                    Postseason.playCfpGame(g, "QFW", "QFL");
                }
            }
        }
        List<Team> alive = Postseason.winnersOf(cfpQuarters);
        cfpSemis = Postseason.scheduleReseededRound(
                alive, cfpField, Postseason.CFP_SEMI_HOSTS, "CFP Semi");
    }

    /** Plays CFP semis; schedules NCG. */
    public void playCfpSemis() {
        if (cfpSemis != null) {
            for (Game g : cfpSemis) {
                if (g != null) {
                    Postseason.playCfpGame(g, "SFW", "SFL");
                }
            }
        }
        List<Team> winners = Postseason.winnersOf(cfpSemis);
        if (winners.size() >= 2) {
            ncg = Postseason.scheduleNcg(winners.get(0), winners.get(1), cfpField);
        }
    }

    /** Plays the national championship. */
    public void playNationalChampionship() {
        if (ncg == null) {
            return;
        }
        ncg.playGame();
        if (ncg.homeScore > ncg.awayScore) {
            ncg.homeTeam.semiFinalWL = "";
            ncg.awayTeam.semiFinalWL = "";
            ncg.homeTeam.natChampWL = "NCW";
            ncg.awayTeam.natChampWL = "NCL";
            ncg.homeTeam.totalNCs++;
            ncg.awayTeam.totalNCLosses++;
        } else {
            ncg.homeTeam.semiFinalWL = "";
            ncg.awayTeam.semiFinalWL = "";
            ncg.awayTeam.natChampWL = "NCW";
            ncg.homeTeam.natChampWL = "NCL";
            ncg.awayTeam.totalNCs++;
            ncg.homeTeam.totalNCLosses++;
        }
    }

    /** @deprecated use {@link #playCfpFirstRoundAndBowls()} */
    @Deprecated
    public void playBowlGames() {
        playCfpFirstRoundAndBowls();
    }

    /**
     * At the end of the year, record the top 10 teams for the League's History.
     */
    public void updateLeagueHistory() {
        //update league history
        Collections.sort( teamList, new TeamCompPoll() );
        String[] yearTop10 = new String[10];
        Team tt;
        for (int i = 0; i < 10; ++i) {
            tt = teamList.get(i);
            yearTop10[i] = tt.abbr + " (" + tt.wins + "-" + tt.losses + ")";
        }
        leagueHistory.add(yearTop10);
    }

    /**
     * Advances season for each team and sets up schedules for the new year.
     */
    public void advanceSeason() {
        currentWeek = 0;
        //updateTeamHistories();
        for (int t = 0; t < teamList.size(); ++t) {
            teamList.get(t).advanceSeason();
        }
        RivalryDynamics.formNewRivalries(this);

        // Advance win streaks
        advanceSeasonWinStreaks();

        setUpSeasonSchedule();

        hasScheduledBowls = false;
        cfpField = new ArrayList<>();
        cfpAutoBids = new HashSet<>();
        cfpFirstRound = null;
        cfpQuarters = null;
        cfpSemis = null;
        ncg = null;
        bowlGames = new Game[0];
    }

    /**
     * Check the longest win streak. If the given streak is longer, replace.
     * @param streak streak to check
     */
    public void checkLongestWinStreak(TeamStreak streak) {
        if (streak.getStreakLength() > longestWinStreak.getStreakLength()) {
            longestWinStreak = new TeamStreak(streak.getStartYear(), streak.getEndYear(), streak.getStreakLength(), streak.getTeam());
        }
    }

    /**
     * Gets the longest active win streak.
     */
    public void updateLongestActiveWinStreak() {
        for (Team t : teamList) {
            if (t.winStreak.getStreakLength() > longestActiveWinStreak.getStreakLength()) {
                longestActiveWinStreak = t.winStreak;
            }
        }
    }

    /**
     * Advance season for win streaks, so no save-load whackiness.
     */
    public void advanceSeasonWinStreaks() {
        yearStartLongestWinStreak = longestWinStreak;
        for (Team t : teamList) {
            t.yearStartWinStreak = t.winStreak;
        }
    }

    /**
     * Change the team abbr of the lognest win streak if the user changed it
     * @param oldAbbr old abbreviation
     * @param newAbbr new abbreviation
     */
    public void changeAbbrWinStreaks(String oldAbbr, String newAbbr) {
        if (longestWinStreak.getTeam().equals(oldAbbr)) {
            longestWinStreak.changeAbbr(newAbbr);
        }
        if (yearStartLongestWinStreak.getTeam().equals(oldAbbr)) {
            yearStartLongestWinStreak.changeAbbr(newAbbr);
        }
    }

    /**
     * Changes all the abbrs to new abbr, in records and histories.
     * @param oldAbbr
     * @param newAbbr
     */
    public void changeAbbrHistoryRecords(String oldAbbr, String newAbbr) {
        // check records and win streaks
        leagueRecords.changeAbbrRecords(userTeam.abbr, newAbbr);
        userTeamRecords.changeAbbrRecords(userTeam.abbr, newAbbr);
        changeAbbrWinStreaks(userTeam.abbr, newAbbr);
        userTeam.winStreak.changeAbbr(newAbbr);
        userTeam.yearStartWinStreak.changeAbbr(newAbbr);

        // check league and POTY history
        for (String[] yr : leagueHistory) {
            for (int i = 0; i < yr.length; ++i) {
                if (yr[i].split(" ")[0].equals(oldAbbr)) {
                    yr[i] = newAbbr + " " + yr[i].split(" ")[1];
                }
            }
        }

        for (int i = 0; i < heismanHistory.size(); ++i) {
            String p = heismanHistory.get(i);
            if (p.split(" ")[4].equals(oldAbbr)) {
                heismanHistory.set(i,
                        p.split(" ")[0] + " " +
                                p.split(" ")[1] + " " +
                                p.split(" ")[2] + " " +
                                p.split(" ")[3] + " " +
                                newAbbr + " " +
                                p.split(" ")[5]);
            }
        }

    }

    /**
     * Checks if any of the league records were broken by teams.
     */
    public void checkLeagueRecords() {
        for (Team t : teamList) {
            t.checkLeagueRecords(leagueRecords);
        }
        userTeam.checkLeagueRecords(userTeamRecords);
    }
    /**
     * Gets all the league records, including the longest win streak		
     * @return string of all the records, csv		
     */
    public String getLeagueRecordsStr() {
        String winStreakStr = "Longest Win Streak," + longestWinStreak.getStreakLength() + "," +
                longestWinStreak.getTeam() + "," + longestWinStreak.getStartYear() + "-" + longestWinStreak.getEndYear() + "\n";
        String activeWinStreakStr = "Active Win Streak," + longestActiveWinStreak.getStreakLength() + "," +
        longestActiveWinStreak.getTeam() + "," + longestActiveWinStreak.getStartYear() + "-" + longestActiveWinStreak.getEndYear() + "\n";
        return winStreakStr + activeWinStreakStr + leagueRecords.getRecordsStr();
    }

    /**
     * Gets the current season year.
     * @return the current year
     */
    public int getYear() {
        return FIRST_SEASON_YEAR + leagueHistory.size();
    }

    /**
     * Gets rid of all injuries
     */
    public void curePlayers() {
        for (Team t : teamList) {
            t.curePlayers();
        }
    }

    /**
     * Updates team history for each team.
     */
    public void updateTeamHistories() {
        for ( int i = 0; i < teamList.size(); ++i ) {
            teamList.get(i).updateTeamHistory();
        }
    }

    /**
     * Update all teams off talent, def talent, etc
     */
    public void updateTeamTalentRatings() {
        for (Team t : teamList) {
            t.updateTalentRatings();
        }
    }

    /**
     * Gets a random player name.
     * @return random name
     */
    public String getRandName() {
        if (Math.random() > 0.0025) {
            int fn = (int) (Math.random() * nameList.size());
            int ln = (int) (Math.random() * lastNameList.size());
            return nameList.get(fn) + " " + lastNameList.get(ln);
        } else {
            return donationNames[ (int)(Math.random()*donationNames.length) ];
        }
    }

    /**
     * Updates poll scores for each team and updates their ranking.
     */
    public void setTeamRanks() {
        //get team ranks for PPG, YPG, etc
        for (int i = 0; i < teamList.size(); ++i) {
            teamList.get(i).updatePollScore();
        }

        Collections.sort( teamList, new TeamCompPoll() );
        for (int t = 0; t < teamList.size(); ++t) {
            teamList.get(t).rankTeamPollScore = t+1;
        }

        Collections.sort( teamList, new TeamCompSoW() );
        for (int t = 0; t < teamList.size(); ++t) {
            teamList.get(t).rankTeamStrengthOfWins = t+1;
        }

        Collections.sort( teamList, new TeamCompPPG() );
        for (int t = 0; t < teamList.size(); ++t) {
            teamList.get(t).rankTeamPoints = t+1;
        }

        Collections.sort( teamList, new TeamCompOPPG() );
        for (int t = 0; t < teamList.size(); ++t) {
            teamList.get(t).rankTeamOppPoints = t+1;
        }

        Collections.sort( teamList, new TeamCompYPG() );
        for (int t = 0; t < teamList.size(); ++t) {
            teamList.get(t).rankTeamYards = t+1;
        }

        Collections.sort( teamList, new TeamCompOYPG() );
        for (int t = 0; t < teamList.size(); ++t) {
            teamList.get(t).rankTeamOppYards = t+1;
        }

        Collections.sort( teamList, new TeamCompPYPG() );
        for (int t = 0; t < teamList.size(); ++t) {
            teamList.get(t).rankTeamPassYards = t+1;
        }

        Collections.sort( teamList, new TeamCompRYPG() );
        for (int t = 0; t < teamList.size(); ++t) {
            teamList.get(t).rankTeamRushYards = t+1;
        }

        Collections.sort( teamList, new TeamCompOPYPG() );
        for (int t = 0; t < teamList.size(); ++t) {
            teamList.get(t).rankTeamOppPassYards = t+1;
        }

        Collections.sort( teamList, new TeamCompORYPG() );
        for (int t = 0; t < teamList.size(); ++t) {
            teamList.get(t).rankTeamOppRushYards = t+1;
        }

        Collections.sort( teamList, new TeamCompTODiff() );
        for (int t = 0; t < teamList.size(); ++t) {
            teamList.get(t).rankTeamTODiff= t+1;
        }

        Collections.sort( teamList, new TeamCompOffTalent() );
        for (int t = 0; t < teamList.size(); ++t) {
            teamList.get(t).rankTeamOffTalent = t+1;
        }

        Collections.sort( teamList, new TeamCompDefTalent() );
        for (int t = 0; t < teamList.size(); ++t) {
            teamList.get(t).rankTeamDefTalent = t+1;
        }

        Collections.sort( teamList, new TeamCompProgramPower() );
        for (int t = 0; t < teamList.size(); ++t) {
            teamList.get(t).rankTeamProgramPower = t+1;
        }

        if (currentWeek == 0) {
            Collections.sort(teamList, new TeamCompRecruitClass());
            for (int t = 0; t < teamList.size(); ++t) {
                teamList.get(t).rankTeamRecruitClass = t + 1;
            }
        }

    }

    /**
     * Calculates who wins the Heisman.
     * @return Heisman Winner
     */
    public ArrayList<Player> getHeisman() {
        heisman = null;
        int heismanScore = 0;
        int tempScore = 0;
        ArrayList<Player> heismanCandidates = new ArrayList<Player>();
        for ( int i = 0; i < teamList.size(); ++i ) {
            //qb
            for (int qb = 0; qb < teamList.get(i).teamQBs.size(); ++qb) {
                heismanCandidates.add(teamList.get(i).teamQBs.get(qb));
                tempScore = teamList.get(i).teamQBs.get(qb).getHeismanScore() + teamList.get(i).wins * 100;
                if (tempScore > heismanScore) {
                    heisman = teamList.get(i).teamQBs.get(qb);
                    heismanScore = tempScore;
                }
            }

            //rb
            for (int rb = 0; rb < teamList.get(i).teamRBs.size(); ++rb) {
                heismanCandidates.add( teamList.get(i).teamRBs.get(rb) );
                tempScore = teamList.get(i).teamRBs.get(rb).getHeismanScore() + teamList.get(i).wins*100;
                if ( tempScore > heismanScore ) {
                    heisman = teamList.get(i).teamRBs.get(rb);
                    heismanScore = tempScore;
                }
            }

            //wr
            for (int wr = 0; wr < teamList.get(i).teamWRs.size(); ++wr) {
                heismanCandidates.add( teamList.get(i).teamWRs.get(wr) );
                tempScore = teamList.get(i).teamWRs.get(wr).getHeismanScore() + teamList.get(i).wins*100;
                if ( tempScore > heismanScore ) {
                    heisman = teamList.get(i).teamWRs.get(wr);
                    heismanScore = tempScore;
                }
            }
        }
        Collections.sort( heismanCandidates, new PlayerHeismanComp() );

        return heismanCandidates;
    }

    /**
     * Get string of the top 5 heisman candidates. If the heisman is already decided, get the ceremony str.
     * @return string of top 5 players and their stats
     */
    public String getTop5HeismanStr() {
        if (heismanDecided) {
            return getHeismanCeremonyStr();
        } else {
            ArrayList<Player> heismanCandidates = getHeisman();
            //full results string
            String heismanTop5 = "";
            for (int i = 0; i < 5; ++i) {
                Player p = heismanCandidates.get(i);
                heismanTop5 += (i + 1) + ". " + p.team.abbr + "(" + p.team.wins + "-" + p.team.losses + ")" + " - ";
                if ("QB".equals(p.position)) {
                    heismanTop5 += " QB " + p.name + " [" + p.getYrStr() +
                            "]\n \t\t(" + p.seasonStats.passTd + " TDs, " + p.seasonStats.passInt + " Int, " + p.seasonStats.passYards + " Yds)\n\n";
                } else if ("RB".equals(p.position)) {
                    heismanTop5 += " RB " + p.name + " [" + p.getYrStr() +
                            "]\n \t\t(" + p.seasonStats.rushTd + " TDs, " + p.seasonStats.fumbles + " Fum, " + p.seasonStats.rushYards + " Yds)\n\n";
                } else if ("WR".equals(p.position)) {
                    heismanTop5 += " WR " + p.name + " [" + p.getYrStr() +
                            "]\n \t\t(" + p.seasonStats.recTd + " TDs, " + p.seasonStats.recFumbles + " Fum, " + p.seasonStats.recYards + " Yds)\n\n";
                }
            }
            return heismanTop5;
        }
    }

    /**
     * Perform the heisman ceremony. Congratulate winner and give top 5 vote getters.
     * @return string of the heisman ceremony.
     */
    public String getHeismanCeremonyStr() {
        if (!heismanDecided) {
            heismanDecided = true;
            heismanCandidates = getHeisman();
            heisman = heismanCandidates.get(0);
            heisman.wonHeisman = true;
            //full results string
            String heismanTop5 = "\n";
            for (int i = 0; i < 5; ++i) {
                Player p = heismanCandidates.get(i);
                heismanTop5 += (i + 1) + ". " + p.team.abbr + "(" + p.team.wins + "-" + p.team.losses + ")" + " - ";
                if ("QB".equals(p.position)) {
                    heismanTop5 += " QB " + p.getInitialName() + ": " + p.getHeismanScore() + " votes\n\t("
                            + p.seasonStats.passTd + " TDs, " + p.seasonStats.passInt + " Int, " + p.seasonStats.passYards + " Yds)\n\n";
                } else if ("RB".equals(p.position)) {
                    heismanTop5 += " RB " + p.getInitialName() + ": " + p.getHeismanScore() + " votes\n\t("
                            + p.seasonStats.rushTd + " TDs, " + p.seasonStats.fumbles + " Fum, " + p.seasonStats.rushYards + " Yds)\n\n";
                } else if ("WR".equals(p.position)) {
                    heismanTop5 += " WR " + p.getInitialName() + ": " + p.getHeismanScore() + " votes\n\t("
                            + p.seasonStats.recTd + " TDs, " + p.seasonStats.recFumbles + " Fum, " + p.seasonStats.recYards + " Yds)\n\n";
                }
            }
            String heismanStats = "";
            String heismanWinnerStr = "";
            if ("QB".equals(heisman.position)) {
                //qb heisman
                heismanWinnerStr = "Congratulations to the Player of the Year, " + heisman.team.abbr +
                        " QB " + heisman.name + " [" + heisman.getYrStr() + "], who had " +
                        heisman.seasonStats.passTd + " TDs, just " + heisman.seasonStats.passInt + " interceptions, and " +
                        heisman.seasonStats.passYards + " passing yards. He led " + heisman.team.name +
                        " to a " + heisman.team.wins + "-" + heisman.team.losses + " record and a #" + heisman.team.rankTeamPollScore +
                        " poll ranking.";
                heismanStats = heismanWinnerStr + "\n\nFull Results:" + heismanTop5;
            } else if ("RB".equals(heisman.position)) {
                //rb heisman
                heismanWinnerStr = "Congratulations to the Player of the Year, " + heisman.team.abbr +
                        " RB " + heisman.name + " [" + heisman.getYrStr() + "], who had " +
                        heisman.seasonStats.rushTd + " TDs, just " + heisman.seasonStats.fumbles + " fumbles, and " +
                        heisman.seasonStats.rushYards + " rushing yards. He led " + heisman.team.name +
                        " to a " + heisman.team.wins + "-" + heisman.team.losses + " record and a #" + heisman.team.rankTeamPollScore +
                        " poll ranking.";
                heismanStats = heismanWinnerStr + "\n\nFull Results:" + heismanTop5;
            } else if ("WR".equals(heisman.position)) {
                //wr heisman
                heismanWinnerStr = "Congratulations to the Player of the Year, " + heisman.team.abbr +
                        " WR " + heisman.name + " [" + heisman.getYrStr() + "], who had " +
                        heisman.seasonStats.recTd + " TDs, just " + heisman.seasonStats.recFumbles + " fumbles, and " +
                        heisman.seasonStats.recYards + " receiving yards. He led " + heisman.team.name +
                        " to a " + heisman.team.wins + "-" + heisman.team.losses + " record and a #" + heisman.team.rankTeamPollScore +
                        " poll ranking.";
                heismanStats = heismanWinnerStr + "\n\nFull Results:" + heismanTop5;
            }

            heismanWinnerStrFull = heismanStats;

            return heismanStats;

        } else {
            return heismanWinnerStrFull;
        }
    }

    /**
     * Ensures All Americans are selected (best of all conference teams) and returns them.
     * Partitions each conference's all-conf list by position so optional FB/TE slots
     * (omitted when empty) do not shift fixed indexes.
     */
    public ArrayList<Player> getAllAmericans() {
        if (allAmericans.isEmpty()) {
            ArrayList<Player> qbs = new ArrayList<>();
            ArrayList<Player> rbs = new ArrayList<>();
            ArrayList<Player> fbs = new ArrayList<>();
            ArrayList<Player> wrs = new ArrayList<>();
            ArrayList<Player> tes = new ArrayList<>();
            ArrayList<Player> ols = new ArrayList<>();
            ArrayList<Player> ks = new ArrayList<>();
            ArrayList<Player> ss = new ArrayList<>();
            ArrayList<Player> cbs = new ArrayList<>();
            ArrayList<Player> edges = new ArrayList<>();
            ArrayList<Player> dls = new ArrayList<>();
            ArrayList<Player> lbs = new ArrayList<>();

            for (Conference c : conferences) {
                for (Player p : c.getAllConfPlayers()) {
                    if (p == null || p.position == null) continue;
                    switch (p.position) {
                        case "QB": qbs.add(p); break;
                        case "RB": rbs.add(p); break;
                        case "FB": fbs.add(p); break;
                        case "WR": wrs.add(p); break;
                        case "TE": tes.add(p); break;
                        case "OL": ols.add(p); break;
                        case "K": ks.add(p); break;
                        case "S": ss.add(p); break;
                        case "CB": cbs.add(p); break;
                        case "EDGE": edges.add(p); break;
                        case "DL": dls.add(p); break;
                        case "LB": lbs.add(p); break;
                        default: break;
                    }
                }
            }

            Collections.sort(qbs, new PlayerHeismanComp());
            Collections.sort(rbs, new PlayerHeismanComp());
            Collections.sort(fbs, new PlayerHeismanComp());
            Collections.sort(wrs, new PlayerHeismanComp());
            Collections.sort(tes, new PlayerHeismanComp());
            Collections.sort(ols, new PlayerHeismanComp());
            Collections.sort(ks, new PlayerHeismanComp());
            Collections.sort(ss, new PlayerHeismanComp());
            Collections.sort(cbs, new PlayerHeismanComp());
            Collections.sort(edges, new PlayerHeismanComp());
            Collections.sort(dls, new PlayerHeismanComp());
            Collections.sort(lbs, new PlayerHeismanComp());

            addTopAllAmerican(qbs, 1);
            addTopAllAmerican(rbs, 2);
            addTopAllAmerican(fbs, 1); // optional when pool empty
            addTopAllAmerican(wrs, 3);
            addTopAllAmerican(tes, 1); // optional when pool empty
            addTopAllAmerican(ols, 5);
            addTopAllAmerican(ks, 1);
            addTopAllAmerican(ss, 1);
            addTopAllAmerican(cbs, 3);
            addTopAllAmerican(edges, 2);
            addTopAllAmerican(dls, 3);
            addTopAllAmerican(lbs, 3);
        }
        return allAmericans;
    }

    /** Adds up to {@code count} players from {@code pool}; no-op when the pool is short. */
    private void addTopAllAmerican(ArrayList<Player> pool, int count) {
        int n = Math.min(count, pool.size());
        for (int i = 0; i < n; i++) {
            Player p = pool.get(i);
            allAmericans.add(p);
            p.wonAllAmerican = true;
        }
    }

    /**
     * Gets All Americans, best of all conference teams
     * @return string list of all americans
     */
    public String getAllAmericanStr() {
        ArrayList<Player> americans = getAllAmericans();
        StringBuilder allAmerican = new StringBuilder();
        for (int i = 0; i < americans.size(); ++i) {
            Player p = americans.get(i);
            allAmerican.append(p.team.abbr + "(" + p.team.wins + "-" + p.team.losses + ")" + " - ");
            if ("QB".equals(p.position)) {
                allAmerican.append(" QB " + p.name + " [" + p.getYrStr() + "]\n \t\t" +
                        p.seasonStats.passTd + " TDs, " + p.seasonStats.passInt + " Int, " + p.seasonStats.passYards + " Yds\n");
            } else if ("RB".equals(p.position)) {
                allAmerican.append(" RB " + p.name + " [" + p.getYrStr() + "]\n \t\t" +
                        p.seasonStats.rushTd + " TDs, " + p.seasonStats.fumbles + " Fum, " + p.seasonStats.rushYards + " Yds\n");
            } else if ("WR".equals(p.position)) {
                allAmerican.append(" WR " + p.name + " [" + p.getYrStr() + "]\n \t\t" +
                        p.seasonStats.recTd + " TDs, " + p.seasonStats.recFumbles + " Fum, " + p.seasonStats.recYards + " Yds\n");
            } else if ("K".equals(p.position)) {
                allAmerican.append(" K " + p.name + " [" + p.getYrStr() + "]\n \t\t" +
                        "FGs: " + p.seasonStats.fgMade + "/" + p.seasonStats.fgAtt + ", XPs: " + p.seasonStats.xpMade + "/" + p.seasonStats.xpAtt + "\n");
            } else if (isDefenseAwardPos(p.position)) {
                allAmerican.append(" " + p.position + " " + p.name + " [" + p.getYrStr() + "]\n \t\t"
                        + defenseAwardLine(p.seasonStats) + "\n");
            } else {
                allAmerican.append(" " + p.position + " " + p.name + " [" + p.getYrStr() + "]\n");
            }
            allAmerican.append(" \t\tOverall: " + p.ratOvr + ", Potential: " + p.getLetterGrade(p.ratPot) + "\n\n>");
        }

        // Go through all the all conf players to get the all americans
        return allAmerican.toString();
    }

    /**
     * Get a string list of all conference team of choice
     * @param confNum which conference
     * @return string of the conference team
     */
    public String getAllConfStr(int confNum) {
        ArrayList<Player> allConfPlayers = conferences.get(confNum).getAllConfPlayers();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < allConfPlayers.size(); ++i) {
            Player p = allConfPlayers.get(i);
            sb.append(p.team.abbr + "(" + p.team.wins + "-" + p.team.losses + ")" + " - ");
            if ("QB".equals(p.position)) {
                sb.append(" QB " + p.name + " [" + p.getYrStr() + "]\n \t\t" +
                        p.seasonStats.passTd + " TDs, " + p.seasonStats.passInt + " Int, " + p.seasonStats.passYards + " Yds\n");
            } else if ("RB".equals(p.position)) {
                sb.append(" RB " + p.name + " [" + p.getYrStr() + "]\n \t\t" +
                        p.seasonStats.rushTd + " TDs, " + p.seasonStats.fumbles + " Fum, " + p.seasonStats.rushYards + " Yds\n");
            } else if ("WR".equals(p.position)) {
                sb.append(" WR " + p.name + " [" + p.getYrStr() + "]\n \t\t" +
                        p.seasonStats.recTd + " TDs, " + p.seasonStats.recFumbles + " Fum, " + p.seasonStats.recYards + " Yds\n");
            } else if ("K".equals(p.position)) {
                sb.append(" K " + p.name + " [" + p.getYrStr() + "]\n \t\t" +
                        "FGs: " + p.seasonStats.fgMade + "/" + p.seasonStats.fgAtt + ", XPs: " + p.seasonStats.xpMade + "/" + p.seasonStats.xpAtt + "\n");
            } else if (isDefenseAwardPos(p.position)) {
                sb.append(" " + p.position + " " + p.name + " [" + p.getYrStr() + "]\n \t\t"
                        + defenseAwardLine(p.seasonStats) + "\n");
            } else {
                sb.append(" " + p.position + " " + p.name + " [" + p.getYrStr() + "]\n");
            }
            sb.append(" \t\tOverall: " + p.ratOvr + ", Potential: " + p.getLetterGrade(p.ratPot) + "\n\n>");
        }

        return sb.toString();
    }

    private static boolean isDefenseAwardPos(String position) {
        return "CB".equals(position) || "S".equals(position) || "EDGE".equals(position)
                || "DL".equals(position) || "LB".equals(position);
    }

    private static String defenseAwardLine(PlayerSkillStats s) {
        return s.tackles + " Tck, " + s.tfl + " TFL, " + s.sacksDef + " Sk, "
                + s.defInt + " INT, " + s.passDef + " PD";
    }

    /**
     * Set the players leaving for each team.
     */
    public void getPlayersLeaving() {
        for (Team t : teamList) {
            t.getPlayersLeaving();
        }
    }

    /**
     * Get a mock draft of all players who are leaving, sorted by overall.
     * @return array of string reps of the players
     */
    public String[] getMockDraftPlayersList() {
        ArrayList<Player> allPlayersLeaving = new ArrayList<>();
        for (Team t : teamList) {
            for (Player p : t.playersLeaving) {
                if (p.ratOvr > 85 && !p.position.equals("K")) allPlayersLeaving.add(p);
            }
        }

        Collections.sort(allPlayersLeaving, new PlayerComparator());

        // Get 64 players (first 2 rounds)
        ArrayList<Player> top64Players = new ArrayList<>(64);
        for (int i = 0; i < 64; ++i) {
            top64Players.add(allPlayersLeaving.get(i));
        }

        String[] nflPlayers = new String[ top64Players.size() ];
        for (int i = 0; i < nflPlayers.length; ++i) {
            nflPlayers[i] = top64Players.get(i).getMockDraftStr();
        }

        return nflPlayers;
    }


    /**
     * Get list of all the teams and their rankings based on selection
     * @param selection stat to sort by, 0-13
     * @return list of the teams: ranking,str rep,stat
     */
    public ArrayList<String> getTeamRankingsStr(int selection) {
        /*
        0 = poll score
        1 = conf standings
        2 = sos
        3 = points
        4 = opp points
        5 = yards
        6 = opp yards
        7 = pass yards
        8 = rush yards
        9 = opp pass yards
        10 = opp rush yards
        11 = TO diff
        12 = off talent
        13 = def talent
        14 = program power
         */
        ArrayList<Team> teams = teamList; //(ArrayList<Team>) teamList.clone();
        ArrayList<String> rankings = new ArrayList<String>();
        Team t;
        switch (selection) {
            case 0: Collections.sort( teams, new TeamCompPoll() );
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    rankings.add(t.getRankStrStarUser(i + 1) + "," + t.strRepWithBowlResults() + "," + t.teamPollScore);
                }
                break;
            case 1: return getConfStandings();
            case 2: Collections.sort( teams, new TeamCompSoW() );
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    rankings.add(t.getRankStrStarUser(i+1) + "," + t.strRepWithBowlResults() + "," + t.teamStrengthOfWins);
                }
                break;
            case 3: Collections.sort( teams, new TeamCompPPG() );
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    rankings.add(t.getRankStrStarUser(i+1) + "," + t.strRepWithBowlResults() + "," + (t.teamPoints/t.numGames()));
                }
                break;
            case 4: Collections.sort( teams, new TeamCompOPPG() );
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    rankings.add(t.getRankStrStarUser(i+1) + "," + t.strRepWithBowlResults() + "," + (t.teamOppPoints/t.numGames()));
                }
                break;
            case 5: Collections.sort( teams, new TeamCompYPG() );
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    rankings.add(t.getRankStrStarUser(i+1) + "," + t.strRepWithBowlResults() + "," + (t.teamYards/t.numGames()));
                }
                break;
            case 6: Collections.sort( teams, new TeamCompOYPG() );
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    rankings.add(t.getRankStrStarUser(i+1) + "," + t.strRepWithBowlResults() + "," + (t.teamOppYards/t.numGames()));
                }
                break;
            case 7: Collections.sort( teams, new TeamCompPYPG() );
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    rankings.add(t.getRankStrStarUser(i+1) + "," + t.strRepWithBowlResults() + "," + (t.teamPassYards/t.numGames()));
                }
                break;
            case 8: Collections.sort( teams, new TeamCompRYPG() );
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    rankings.add(t.getRankStrStarUser(i+1) + "," + t.strRepWithBowlResults() + "," + (t.teamRushYards/t.numGames()));
                }
                break;
            case 9: Collections.sort( teams, new TeamCompOPYPG() );
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    rankings.add(t.getRankStrStarUser(i+1) + "," + t.strRepWithBowlResults() + "," + (t.teamOppPassYards/t.numGames()));
                }
                break;
            case 10: Collections.sort( teams, new TeamCompORYPG() );
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    rankings.add(t.getRankStrStarUser(i+1) + "," + t.strRepWithBowlResults() + "," + (t.teamOppRushYards/t.numGames()));
                }
                break;
            case 11: Collections.sort( teams, new TeamCompTODiff() );
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    if (t.teamTODiff > 0) rankings.add(t.getRankStrStarUser(i+1) + "," + t.strRepWithBowlResults() + ",+" + t.teamTODiff);
                    else rankings.add(t.getRankStrStarUser(i+1) + "," + t.strRepWithBowlResults() + "," + t.teamTODiff);
                }
                break;
            case 12: Collections.sort( teams, new TeamCompOffTalent() );
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    rankings.add(t.getRankStrStarUser(i+1) + "," + t.strRepWithBowlResults() + "," + t.teamOffTalent);
                }
                break;
            case 13: Collections.sort( teams, new TeamCompDefTalent() );
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    rankings.add(t.getRankStrStarUser(i+1) + "," + t.strRepWithBowlResults() + "," + t.teamDefTalent);
                }
                break;
            case 14: Collections.sort( teams, new TeamCompProgramPower() );
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    rankings.add(t.getRankStrStarUser(i + 1) + ","
                            + t.strRepWithBowlResults() + "," + t.programProfile.programPower);
                }
                break;
            case 15: Collections.sort( teams, new TeamCompRecruitClass() );
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    rankings.add(t.getRankStrStarUser(i + 1) + ","
                            + t.strRepWithProgramPower() + "," + t.getRecruitingClassRat());
                }
                break;
            default: Collections.sort( teams, new TeamCompPoll() );
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    rankings.add(t.getRankStrStarUser(i+1) + "," + t.strRepWithBowlResults() + "," + t.teamPollScore);
                }
                break;
        }

        return rankings;
    }

    /**
     * Get conference standings in an list of Strings.
     * Must be CSV form: Rank,Team,Num
     */
    public ArrayList<String> getConfStandings() {
        ArrayList<String> confStandings = new ArrayList<>();
        ArrayList<Team> confTeams = new ArrayList<>();
        for (Conference c : conferences) {
            confTeams.addAll(c.confTeams);
            Collections.sort(confTeams, new TeamCompConfWins());
            confStandings.add(" ,"+c.confName+" Conference, ");
            Team t;
            for (int i = 0; i < confTeams.size(); ++i) {
                t = confTeams.get(i);
                confStandings.add(t.getRankStrStarUser(i+1) + "," + t.strRepWithBowlResults() + "," + t.getConfWins()+"-"+t.getConfLosses());
            }
            confTeams.clear();
        }
        return confStandings;
    }

    /**
     * Get String of the league's history, year by year.
     * Saves the NCG winner and the POTY.
     * @return list of the league's history.
     */
    public String getLeagueHistoryStr() {
        String hist = "";
        for (int i = 0; i < leagueHistory.size(); ++i) {
            hist += (FIRST_SEASON_YEAR+i) + ":\n";
            hist += "\tChampions: " + leagueHistory.get(i)[0] + "\n";
            hist += "\tPOTY: " + heismanHistory.get(i) + "\n%";
        }
        return hist;
    }

    /**
     * Get list of teams and their program power, used for selecting a new career.
     * @return array of all the teams
     */
    public String[] getTeamListStr() {
        String[] teams = new String[teamList.size()];
        for (int i = 0; i < teamList.size(); ++i){
            teams[i] = teamList.get(i).conference + ": " +
                    teamList.get(i).name + ", Power: "
                    + teamList.get(i).programProfile.programPower;
        }
        return teams;
    }

    /**
     * Get list of all bowl games and their predicted teams
     * @return string of all the bowls and their predictions
     */
    public String getBowlGameWatchStr() {
        StringBuilder sb = new StringBuilder();
        if (!hasScheduledBowls) {
            for (int i = 0; i < teamList.size(); ++i) {
                teamList.get(i).updatePollScore();
            }
            Collections.sort(teamList, new TeamCompPoll());
            Postseason.CfpSelection projected = Postseason.selectCfpField(teamList);
            sb.append("Projected 12-Team CFP:\n");
            for (int i = 0; i < projected.field.size(); i++) {
                Team t = projected.field.get(i);
                String bid = projected.autoBids.contains(t) ? " (auto)" : " (at-large)";
                sb.append("\t").append(i + 1).append(". ").append(t.strRep()).append(bid).append("\n");
            }
            if (projected.field.size() >= Postseason.CFP_FIELD_SIZE) {
                List<Team> seeds = projected.field;
                sb.append("\nFirst Round:\n");
                sb.append("\t5v12: ").append(seeds.get(4).strRep())
                        .append(" vs ").append(seeds.get(11).strRep()).append("\n");
                sb.append("\t6v11: ").append(seeds.get(5).strRep())
                        .append(" vs ").append(seeds.get(10).strRep()).append("\n");
                sb.append("\t7v10: ").append(seeds.get(6).strRep())
                        .append(" vs ").append(seeds.get(9).strRep()).append("\n");
                sb.append("\t8v9: ").append(seeds.get(7).strRep())
                        .append(" vs ").append(seeds.get(8).strRep()).append("\n");
            }
            return sb.toString();
        }

        sb.append("12-Team CFP Field:\n");
        if (cfpField != null) {
            for (int i = 0; i < cfpField.size(); i++) {
                Team t = cfpField.get(i);
                boolean auto = cfpAutoBids != null && cfpAutoBids.contains(t);
                String bid = auto ? " (auto)" : " (at-large)";
                sb.append("\t").append(i + 1).append(". ").append(t.strRep()).append(bid).append("\n");
            }
        }
        appendGameRound(sb, "CFP First Round", cfpFirstRound);
        appendGameRound(sb, "CFP Quarters", cfpQuarters);
        appendGameRound(sb, "CFP Semis", cfpSemis);
        if (ncg != null) {
            sb.append("\nNational Championship:\n");
            sb.append(getGameSummaryBowl(ncg)).append("\n");
        }
        if (bowlGames != null && bowlGames.length > 0) {
            sb.append("\nBowl Games:\n");
            for (Game g : bowlGames) {
                if (g == null) {
                    continue;
                }
                sb.append("\n").append(g.gameName).append(":\n");
                sb.append(getGameSummaryBowl(g));
            }
        }
        return sb.toString();
    }

    private void appendGameRound(StringBuilder sb, String title, Game[] games) {
        if (games == null || games.length == 0) {
            return;
        }
        sb.append("\n").append(title).append(":\n");
        for (Game g : games) {
            if (g == null) {
                continue;
            }
            sb.append(g.gameName).append("\n");
            sb.append(getGameSummaryBowl(g)).append("\n");
        }
    }

    /**
     * Get string of what happened in a particular bowl
     * @param g Bowl game to be examined
     * @return string of its summary, ALA W 24 - 40 @ GEO, etc
     */
    public String getGameSummaryBowl(Game g) {
        StringBuilder sb = new StringBuilder();
        if (!g.hasPlayed || !g.isDecided()) {
            return g.homeTeam.strRep() + " vs " + g.awayTeam.strRep();
        } else {
            Team winner = g.winningTeam();
            Team loser = g.losingTeam();
            if (g.homeWon()) {
                sb.append(winner.strRep() + " W ");
                sb.append(g.homeScore + "-" + g.awayScore + " ");
                sb.append("vs " + loser.strRep());
                return sb.toString();
            } else {
                sb.append(winner.strRep() + " W ");
                sb.append(g.awayScore + "-" + g.homeScore + " ");
                sb.append("@ " + loser.strRep());
                return sb.toString();
            }
        }
    }

    /**
     * Get a list of all the CCGs and their teams
     * @return
     */
    public String getCCGsStr() {
        StringBuilder sb = new StringBuilder();
        for (Conference c : conferences) {
            if (c.hasChampionship) {
                sb.append(c.getCCGStr()+"\n\n");
            }
        }
        return sb.toString();
    }

    /**
     * Find team based on a name
     * @param name team name
     * @return reference to the Team object
     */
    public Team findTeam(String name) {
        for (int i = 0; i < teamList.size(); i++){
            if (teamList.get(i).strRep().equals(name)) {
                return teamList.get(i);
            }
        }
        return teamList.get(0);
    }

    /**
     * Find team based on a abbr
     * @param abbr team abbr
     * @return reference to the Team object
     */
    public Team findTeamAbbr(String abbr) {
        for (int i = 0; i < teamList.size(); i++){
            if (teamList.get(i).abbr.equals(abbr)) {
                return teamList.get(i);
            }
        }
        return teamList.get(0);
    }

    /**
     * Find conference based on a name
     * @param name conf name
     * @return reference to the Conference object
     */
    public Conference findConference(String name) {
        for (int i = 0; i < teamList.size(); i++){
            if (conferences.get(i).confName.equals(name)) {
                return conferences.get(i);
            }
        }
        return conferences.get(0);
    }

    /**
     * See if team name is in use, or has illegal characters.
     * @param name team name
     * @return true if valid, false if not
     */
    public boolean isNameValid(String name) {
        if (name.length() == 0) {
            return false;
        }

        if (name.contains(",") || name.contains(">") || name.contains("%") || name.contains("\\")) {
            // Illegal character!
            return false;
        }

        for (int i = 0; i < teamList.size(); i++) {
            // compare using all lower case so no dumb duplicates
            if (teamList.get(i).name.toLowerCase().equals(name.toLowerCase()) &&
                    !teamList.get(i).userControlled) {
                return false;
            }
        }

        return true;
    }

    /**
     * See if team abbr is in use, or has illegal characters, or is not 3 characters
     * @param abbr new abbr
     * @return true if valid, false if not
     */
    public boolean isAbbrValid(String abbr) {
        if (abbr.length() > 3 || abbr.length() == 0) {
            // Only 3 letter abbr allowed
            return false;
        }

        if (abbr.contains(",") || abbr.contains(">") || abbr.contains("%") || abbr.contains("\\") || abbr.contains(" ")) {
            // Illegal character!
            return false;
        }

        for (int i = 0; i < teamList.size(); i++) {
            if (teamList.get(i).abbr.equals(abbr) &&
                    !teamList.get(i).userControlled) {
                return false;
            }
        }

        return true;
    }

    /**
     * Get summary of what happened in the NCG
     * @return string of summary
     */
    public String ncgSummaryStr() {
        // Give summary of what happened in the NCG
        if (ncg.homeScore > ncg.awayScore) {
            return ncg.homeTeam.name + " (" + ncg.homeTeam.wins + "-" + ncg.homeTeam.losses + ") won the National Championship, " +
                    "winning against " + ncg.awayTeam.name + " (" + ncg.awayTeam.wins + "-" + ncg.awayTeam.losses + ") in the NCG " +
                    ncg.homeScore + "-" + ncg.awayScore + ".";
        } else {
            return ncg.awayTeam.name + " (" + ncg.awayTeam.wins + "-" + ncg.awayTeam.losses + ") won the National Championship, " +
                    "winning against " + ncg.homeTeam.name + " (" + ncg.homeTeam.wins + "-" + ncg.homeTeam.losses + ") in the NCG " +
                    ncg.awayScore + "-" + ncg.homeScore + ".";
        }
    }

    /**
     * Get summary of season.
     * @return ncgSummary, userTeam's summary
     */
    public String seasonSummaryStr() {
        setTeamRanks();
        StringBuilder sb = new StringBuilder();
        sb.append(ncgSummaryStr());
        sb.append("\n\n" + userTeam.seasonSummaryStr());
        sb.append("\n\n" + leagueRecords.brokenRecordsStr(getYear(), userTeam.abbr));
        return sb.toString();
    }

    /** Short stats line for POTY winner header (typed stats, not ceremony prose). */
    public String heismanWinnerStatsLine(Player p) {
        if ("QB".equals(p.position)) {
            return p.seasonStats.passTd + " TDs · " + p.seasonStats.passInt + " Int · "
                    + String.format("%,d", p.seasonStats.passYards) + " Yds";
        } else if ("RB".equals(p.position)) {
            return p.seasonStats.rushTd + " TDs · " + p.seasonStats.fumbles + " Fum · "
                    + String.format("%,d", p.seasonStats.rushYards) + " Yds";
        } else if ("WR".equals(p.position)) {
            return p.seasonStats.recTd + " TDs · " + p.seasonStats.recFumbles + " Fum · "
                    + String.format("%,d", p.seasonStats.recYards) + " Yds";
        }
        return "Ovr " + p.ratOvr;
    }

    /**
     * Rows for SeasonAwardsListArrayAdapter: top N Heisman candidates.
     * Format: "rank. ABBR POS Name\nvotes · W-L\nkey stats"
     */
    public String[] heismanVotingResultRows(int topN) {
        ArrayList<Player> cands = getHeisman();
        int n = Math.min(topN, cands.size());
        String[] rows = new String[n];
        for (int i = 0; i < n; i++) {
            Player p = cands.get(i);
            String stats;
            if ("QB".equals(p.position)) {
                stats = p.seasonStats.passTd + " TDs, " + p.seasonStats.passInt + " Int, "
                        + String.format("%,d", p.seasonStats.passYards) + " Yds";
            } else if ("RB".equals(p.position)) {
                stats = p.seasonStats.rushTd + " TDs, " + p.seasonStats.fumbles + " Fum, "
                        + String.format("%,d", p.seasonStats.rushYards) + " Yds";
            } else if ("WR".equals(p.position)) {
                stats = p.seasonStats.recTd + " TDs, " + p.seasonStats.recFumbles + " Fum, "
                        + String.format("%,d", p.seasonStats.recYards) + " Yds";
            } else {
                stats = "Ovr " + p.ratOvr;
            }
            rows[i] = (i + 1) + ". " + p.team.abbr + " " + p.position + " " + p.name
                    + "\n" + p.getHeismanScore() + " votes · " + p.team.wins + "-" + p.team.losses
                    + "\n" + stats;
        }
        return rows;
    }
}

class PlayerHeismanComp implements Comparator<Player> {
    @Override
    public int compare( Player a, Player b ) {
        return a.getHeismanScore() > b.getHeismanScore() ? -1 : a.getHeismanScore() == b.getHeismanScore() ? 0 : 1;
    }
}

class TeamCompPoll implements Comparator<Team> {
    @Override
    public int compare( Team a, Team b ) {
        return a.teamPollScore > b.teamPollScore ? -1 : a.teamPollScore == b.teamPollScore ? 0 : 1;
    }
}

class TeamCompSoW implements Comparator<Team> {
    @Override
    public int compare( Team a, Team b ) {
        return a.teamStrengthOfWins > b.teamStrengthOfWins ? -1 : a.teamStrengthOfWins == b.teamStrengthOfWins ? 0 : 1;
    }
}

class TeamCompPPG implements Comparator<Team> {
    @Override
    public int compare( Team a, Team b ) {
        return a.teamPoints/a.numGames() > b.teamPoints/b.numGames() ? -1 : a.teamPoints/a.numGames() == b.teamPoints/b.numGames() ? 0 : 1;
    }
}

class TeamCompOPPG implements Comparator<Team> {
    @Override
    public int compare( Team a, Team b ) {
        return a.teamOppPoints/a.numGames() < b.teamOppPoints/b.numGames() ? -1 : a.teamOppPoints/a.numGames() == b.teamOppPoints/b.numGames() ? 0 : 1;
    }
}

class TeamCompYPG implements Comparator<Team> {
    @Override
    public int compare( Team a, Team b ) {
        return a.teamYards/a.numGames() > b.teamYards/b.numGames() ? -1 : a.teamYards/a.numGames() == b.teamYards/b.numGames() ? 0 : 1;
    }
}

class TeamCompOYPG implements Comparator<Team> {
    @Override
    public int compare( Team a, Team b ) {
        return a.teamOppYards/a.numGames() < b.teamOppYards/b.numGames() ? -1 : a.teamOppYards/a.numGames() == b.teamOppYards/b.numGames() ? 0 : 1;
    }
}

class TeamCompOPYPG implements Comparator<Team> {
    @Override
    public int compare( Team a, Team b ) {
        return a.teamOppPassYards/a.numGames() < b.teamOppPassYards/b.numGames() ? -1 : a.teamOppPassYards/a.numGames() == b.teamOppPassYards/b.numGames() ? 0 : 1;
    }
}

class TeamCompORYPG implements Comparator<Team> {
    @Override
    public int compare( Team a, Team b ) {
        return a.teamOppRushYards/a.numGames() < b.teamOppRushYards/b.numGames() ? -1 : a.teamOppRushYards/a.numGames() == b.teamOppRushYards/b.numGames() ? 0 : 1;
    }
}

class TeamCompPYPG implements Comparator<Team> {
    @Override
    public int compare( Team a, Team b ) {
        return a.teamPassYards/a.numGames() > b.teamPassYards/b.numGames() ? -1 : a.teamPassYards/a.numGames() == b.teamPassYards/b.numGames() ? 0 : 1;
    }
}

class TeamCompRYPG implements Comparator<Team> {
    @Override
    public int compare( Team a, Team b ) {
        return a.teamRushYards/a.numGames() > b.teamRushYards/b.numGames() ? -1 : a.teamRushYards/a.numGames() == b.teamRushYards/b.numGames() ? 0 : 1;
    }
}

class TeamCompTODiff implements Comparator<Team> {
    @Override
    public int compare( Team a, Team b ) {
        return a.teamTODiff > b.teamTODiff ? -1 : a.teamTODiff == b.teamTODiff ? 0 : 1;
    }
}

class TeamCompOffTalent implements Comparator<Team> {
    @Override
    public int compare( Team a, Team b ) {
        return a.teamOffTalent > b.teamOffTalent ? -1 : a.teamOffTalent == b.teamOffTalent ? 0 : 1;
    }
}

class TeamCompDefTalent implements Comparator<Team> {
    @Override
    public int compare( Team a, Team b ) {
        return a.teamDefTalent > b.teamDefTalent ? -1 : a.teamDefTalent == b.teamDefTalent ? 0 : 1;
    }
}

class TeamCompProgramPower implements Comparator<Team> {
    @Override
    public int compare( Team a, Team b ) {
        return Integer.compare(b.programProfile.programPower, a.programProfile.programPower);
    }
}

class TeamCompRecruitClass implements Comparator<Team> {
    @Override
    public int compare( Team a, Team b ) {
        return a.getRecruitingClassRat() > b.getRecruitingClassRat() ? -1 : a.getRecruitingClassRat() == b.getRecruitingClassRat() ? 0 : 1;
    }
}






