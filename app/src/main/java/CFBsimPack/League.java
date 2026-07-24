package CFBsimPack;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
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
     * Creates League, sets up Conferences, reads team names and conferences from file.
     * Also schedules games for every team.
     */
    public League(String namesCSV, String lastNamesCSV, String teamsCSV) {
        heismanDecided = false;
        hasScheduledBowls = false;
        cfpField = new ArrayList<>();
        cfpAutoBids = new HashSet<>();
        bowlGames = new Game[0];
        leagueHistory = new ArrayList<String[]>();
        heismanHistory = new ArrayList<String>();
        currentWeek = 0;
        conferences = new ArrayList<Conference>();
        allAmericans = new ArrayList<Player>();

        leagueRecords = new LeagueRecords();
        userTeamRecords = new LeagueRecords();
        longestWinStreak = new TeamStreak(getYear(), getYear(), 0, "XXX");
        yearStartLongestWinStreak = new TeamStreak(getYear(), getYear(), 0, "XXX");
        longestActiveWinStreak = new TeamStreak(getYear(), getYear(), 0, "XXX");
        oocContracts = new OocContractBook(this);

        // Read first names from file
        nameList = new ArrayList<String>();
        String[] namesSplit = namesCSV.split(",");
        for (String n : namesSplit) {
            nameList.add(n.trim());
        }

        // Read last names from file
        lastNameList = new ArrayList<String>();
        namesSplit = lastNamesCSV.split(",");
        for (String n : namesSplit) {
            lastNameList.add(n.trim());
        }

        LeagueDataLoader.load2026Teams(this, teamsCSV);
        setUpSeasonSchedule();

    }

    /**
     * Test/helper constructor that builds conference + byes but leaves OOC open.
     */
    League(String namesCSV, String lastNamesCSV, String teamsCSV, boolean fillOoc) {
        this(namesCSV, lastNamesCSV, teamsCSV, fillOoc, true);
    }

    private League(
            String namesCSV,
            String lastNamesCSV,
            String teamsCSV,
            boolean fillOoc,
            boolean ignored) {
        heismanDecided = false;
        hasScheduledBowls = false;
        cfpField = new ArrayList<>();
        cfpAutoBids = new HashSet<>();
        bowlGames = new Game[0];
        leagueHistory = new ArrayList<String[]>();
        heismanHistory = new ArrayList<String>();
        currentWeek = 0;
        conferences = new ArrayList<Conference>();
        allAmericans = new ArrayList<Player>();

        leagueRecords = new LeagueRecords();
        userTeamRecords = new LeagueRecords();
        longestWinStreak = new TeamStreak(getYear(), getYear(), 0, "XXX");
        yearStartLongestWinStreak = new TeamStreak(getYear(), getYear(), 0, "XXX");
        longestActiveWinStreak = new TeamStreak(getYear(), getYear(), 0, "XXX");
        oocContracts = new OocContractBook(this);

        nameList = new ArrayList<String>();
        for (String n : namesCSV.split(",")) {
            nameList.add(n.trim());
        }
        lastNameList = new ArrayList<String>();
        for (String n : lastNamesCSV.split(",")) {
            lastNameList.add(n.trim());
        }

        LeagueDataLoader.load2026Teams(this, teamsCSV);
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
     * Create League from saved file.
     * @param saveFile file that league is saved in
     */
    public League(File saveFile, String namesCSV, String lastNamesCSV) {
        heismanDecided = false;
        hasScheduledBowls = false;
        loadedInOffseason = false;
        loadedOffseasonPhase = OffseasonSession.Phase.RETENTION;
        cfpField = new ArrayList<>();
        cfpAutoBids = new HashSet<>();
        bowlGames = new Game[0];
        // This will reference one line at a time
        String line = null;
        currentWeek = 0;

        leagueRecords = new LeagueRecords();
        userTeamRecords = new LeagueRecords();
        longestWinStreak = new TeamStreak(FIRST_SEASON_YEAR, FIRST_SEASON_YEAR, 0, "XXX");
        yearStartLongestWinStreak = new TeamStreak(FIRST_SEASON_YEAR, FIRST_SEASON_YEAR, 0, "XXX");
        longestActiveWinStreak = new TeamStreak(FIRST_SEASON_YEAR, FIRST_SEASON_YEAR, 0, "XXX");
        oocContracts = new OocContractBook(this);

        try {
            // Always wrap FileReader in BufferedReader.
            BufferedReader bufferedReader = new BufferedReader( new FileReader(saveFile) );

            //First ignore the save file info (legacy saves may end with [HARD]% or [EASY]%)
            line = bufferedReader.readLine();

            //Next get league history
            leagueHistory = new ArrayList<String[]>();
            while((line = bufferedReader.readLine()) != null && !line.equals("END_LEAGUE_HIST")) {
                leagueHistory.add(line.split("%"));
            }

            //Next get heismans
            heismanHistory = new ArrayList<String>();
            while((line = bufferedReader.readLine()) != null && !line.equals("END_HEISMAN_HIST")) {
                heismanHistory.add(line);
            }

            //Next make all the teams
            conferences = new ArrayList<Conference>();
            teamList = new ArrayList<Team>();
            allAmericans = new ArrayList<Player>();
            line = bufferedReader.readLine();
            if (line == null || !line.startsWith("SAVE_VERSION,")) {
                throw new IOException("Save from older version — start a new career.");
            }
            int saveVersion = Integer.parseInt(line.substring("SAVE_VERSION,".length()).trim());
            if (saveVersion != 6 && saveVersion != 7 && saveVersion != 8) {
                throw new IOException("Save from older version — start a new career.");
            }
            line = bufferedReader.readLine();
            if (line == null || !line.startsWith("TEAM_COUNT,")) {
                throw new IOException("Unsupported save format: missing team count.");
            }
            int teamCount = Integer.parseInt(line.substring("TEAM_COUNT,".length()));
            String pendingLine = null;
            for(int i = 0; i < teamCount; ++i) {
                StringBuilder sbTeam = new StringBuilder();
                if (pendingLine != null) {
                    sbTeam.append(pendingLine);
                    pendingLine = null;
                }
                while((line = bufferedReader.readLine()) != null && !line.equals("END_PLAYERS")) {
                    sbTeam.append(line);
                }
                Team t = new Team(sbTeam.toString(), this);
                line = bufferedReader.readLine();
                if (line != null && line.startsWith("ST_DEPTH,")) {
                    t.loadSpecialTeamsDepth(line);
                } else {
                    t.ensureSpecialTeamsDepth();
                    pendingLine = line;
                }
                getOrCreateConference(t.conference).confTeams.add(t);
                teamList.add(t);
            }

            //Set up user team
            if (pendingLine != null) {
                line = pendingLine;
            } else {
                line = bufferedReader.readLine();
            }
            if (line != null) {
                for (Team t : teamList) {
                    if (t.name.equals(line)) {
                        userTeam = t;
                        userTeam.userControlled = true;
                    }
                }
            }
            while((line = bufferedReader.readLine()) != null && !line.equals("END_USER_TEAM")) {
                userTeam.teamHistory.add(line);
            }

            // Discard legacy bless/curse markers (kept for save-format compatibility)
            while((line = bufferedReader.readLine()) != null && !line.equals("END_BLESS_TEAM")) {
                // ignore
            }
            while((line = bufferedReader.readLine()) != null && !line.equals("END_CURSE_TEAM")) {
                // ignore
            }

            String[] record;
            while((line = bufferedReader.readLine()) != null && !line.equals("END_LEAGUE_RECORDS")) {
                record = line.split(",");
                if (!record[1].equals("-1"))
                    leagueRecords.checkRecord(record[0], Integer.parseInt(record[1]), record[2], Integer.parseInt(record[3]));
            }

            while((line = bufferedReader.readLine()) != null && !line.equals("END_LEAGUE_WIN_STREAK")) {
                record = line.split(",");
                longestWinStreak = new TeamStreak(
                        Integer.parseInt(record[2]), Integer.parseInt(record[3]), Integer.parseInt(record[0]), record[1]);
                yearStartLongestWinStreak = new TeamStreak(
                        Integer.parseInt(record[2]), Integer.parseInt(record[3]), Integer.parseInt(record[0]), record[1]);
            }

            while((line = bufferedReader.readLine()) != null && !line.equals("END_USER_TEAM_RECORDS")) {
                record = line.split(",");
                if (!record[1].equals("-1"))
                    userTeamRecords.checkRecord(record[0], Integer.parseInt(record[1]), record[2], Integer.parseInt(record[3]));
            }

            while((line = bufferedReader.readLine()) != null && !line.equals("END_HALL_OF_FAME")) {
                userTeam.hallOfFame.add(line);
            }

            // Optional season progress + schedule + team season + OOC + mid-offseason
            line = bufferedReader.readLine();
            boolean restoredSchedule = false;
            boolean restoredSeasonProgress = false;
            if (line != null && line.equals("SEASON_PROGRESS")) {
                restoreSeasonProgress(bufferedReader);
                restoredSeasonProgress = true;
                line = bufferedReader.readLine();
            }
            if (line != null && line.equals("SCHEDULE")) {
                restoreScheduleFromSave(bufferedReader);
                restoredSchedule = true;
                line = bufferedReader.readLine();
            }
            if (line != null && line.equals("TEAM_SEASON")) {
                restoreTeamSeasonBlock(bufferedReader);
                line = bufferedReader.readLine();
            }
            if (line != null && line.equals("OOC_CONTRACTS")) {
                oocContracts.restore(bufferedReader);
                line = bufferedReader.readLine();
            }
            if (restoredSchedule && oocContracts != null) {
                oocContracts.relinkScheduleContractIds();
            }
            if (line != null && line.startsWith("OFFSEASON,")) {
                restoreOffseasonFromSave(bufferedReader, line);
            }

            // Always close files.
            bufferedReader.close();


            // Read first names from file
            nameList = new ArrayList<String>();
            String[] namesSplit = namesCSV.split(",");
            for (String n : namesSplit) {
                nameList.add(n.trim());
            }

            // Read last names from file
            lastNameList = new ArrayList<String>();
            namesSplit = lastNamesCSV.split(",");
            for (String n : namesSplit) {
                lastNameList.add(n.trim());
            }

            //Get longest active win streak
            updateLongestActiveWinStreak();

            if (!restoredSchedule) {
                setUpSeasonSchedule();
            }
            if (restoredSeasonProgress) {
                syncConferenceWeeksFromCurrentWeek();
                setTeamRanks();
            }

        }
        catch(FileNotFoundException ex) {
            throw new IllegalStateException("Unable to open save file.", ex);
        }
        catch(IOException ex) {
            throw new IllegalStateException("Unable to read save file.", ex);
        }
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

    private Conference getOrCreateConference(String name) {
        for (Conference conference : conferences) {
            if (conference.confName.equals(name)) {
                return conference;
            }
        }
        Conference conference = new Conference(name, this, !"Independents".equals(name));
        conferences.add(conference);
        return conference;
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
     * Gets All Americans, best of all conference teams
     * @return string list of all americans
     */
    public String getAllAmericanStr() {
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
                c.getAllConfPlayers();
                int idx = 0;
                qbs.add(c.allConfPlayers.get(idx++));
                rbs.add(c.allConfPlayers.get(idx++));
                rbs.add(c.allConfPlayers.get(idx++));
                fbs.add(c.allConfPlayers.get(idx++));
                for (int i = 0; i < 3; ++i) wrs.add(c.allConfPlayers.get(idx++));
                tes.add(c.allConfPlayers.get(idx++));
                for (int i = 0; i < 5; ++i) ols.add(c.allConfPlayers.get(idx++));
                ks.add(c.allConfPlayers.get(idx++));
                ss.add(c.allConfPlayers.get(idx++));
                for (int i = 0; i < 3; ++i) cbs.add(c.allConfPlayers.get(idx++));
                for (int i = 0; i < 2; ++i) edges.add(c.allConfPlayers.get(idx++));
                for (int i = 0; i < 3; ++i) dls.add(c.allConfPlayers.get(idx++));
                for (int i = 0; i < 3; ++i) lbs.add(c.allConfPlayers.get(idx++));
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

            allAmericans.add(qbs.get(0));
            qbs.get(0).wonAllAmerican = true;
            allAmericans.add(rbs.get(0));
            rbs.get(0).wonAllAmerican = true;
            allAmericans.add(rbs.get(1));
            rbs.get(1).wonAllAmerican = true;
            allAmericans.add(fbs.get(0));
            fbs.get(0).wonAllAmerican = true;
            for (int i = 0; i < 3; ++i) {
                allAmericans.add(wrs.get(i));
                wrs.get(i).wonAllAmerican = true;
            }
            allAmericans.add(tes.get(0));
            tes.get(0).wonAllAmerican = true;
            for (int i = 0; i < 5; ++i) {
                allAmericans.add(ols.get(i));
                ols.get(i).wonAllAmerican = true;
            }
            allAmericans.add(ks.get(0));
            ks.get(0).wonAllAmerican = true;
            allAmericans.add(ss.get(0));
            ss.get(0).wonAllAmerican = true;
            for (int i = 0; i < 3; ++i) {
                allAmericans.add(cbs.get(i));
                cbs.get(i).wonAllAmerican = true;
            }
            for (int i = 0; i < 2; ++i) {
                allAmericans.add(edges.get(i));
                edges.get(i).wonAllAmerican = true;
            }
            for (int i = 0; i < 3; ++i) {
                allAmericans.add(dls.get(i));
                dls.get(i).wonAllAmerican = true;
            }
            for (int i = 0; i < 3; ++i) {
                allAmericans.add(lbs.get(i));
                lbs.get(i).wonAllAmerican = true;
            }
        }

        StringBuilder allAmerican = new StringBuilder();
        for (int i = 0; i < allAmericans.size(); ++i) {
            Player p = allAmericans.get(i);
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
            } else {
                sb.append(" " + p.position + " " + p.name + " [" + p.getYrStr() + "]\n");
            }
            sb.append(" \t\tOverall: " + p.ratOvr + ", Potential: " + p.getLetterGrade(p.ratPot) + "\n\n>");
        }

        return sb.toString();
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
        Team winner, loser;
        if (!g.hasPlayed) {
            return g.homeTeam.strRep() + " vs " + g.awayTeam.strRep();
        } else {
            if (g.homeScore > g.awayScore) {
                winner = g.homeTeam;
                loser = g.awayTeam;
                sb.append(winner.strRep() + " W ");
                sb.append(g.homeScore + "-" + g.awayScore + " ");
                sb.append("vs " + loser.strRep());
                return sb.toString();
            } else {
                winner = g.awayTeam;
                loser = g.homeTeam;
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

    /**
     * Save League in a file.
     * @param saveFile file to be overwritten
     * @return true if successful
     */
    public boolean saveLeague(File saveFile) {
        StringBuilder sb = new StringBuilder();

        // Save information about the save file, user team info
        String offTag = "";
        if (OffseasonSession.ready() && OffseasonSession.league == this) {
            offTag = "[OFF:" + OffseasonSession.phase.name() + "]";
        }
        sb.append(getYear() + ": " + userTeam.abbr + " (" + (userTeam.totalWins - userTeam.wins) + "-" + (userTeam.totalLosses - userTeam.losses) + ") " +
                userTeam.totalCCs + " CCs, " + userTeam.totalNCs + " NCs>" + offTag + "%\n");

        // Save league history of who was #1 each year
        for (int i = 0; i < leagueHistory.size(); ++i) {
            for (int j = 0; j < leagueHistory.get(i).length; ++j) {
                sb.append(leagueHistory.get(i)[j] + "%");
            }
            sb.append("\n");
        }
        sb.append("END_LEAGUE_HIST\n");

        // Save POTY history of who won each year
        // Go through leagueHist size in case they save after the Heisman Ceremony
        for (int i = 0; i < leagueHistory.size(); ++i) {
            sb.append(heismanHistory.get(i) + "\n");
        }
            sb.append("END_HEISMAN_HIST\n");
        sb.append("SAVE_VERSION,8\n");
        sb.append("TEAM_COUNT," + teamList.size() + "\n");

        // Save information about each team like W-L records, as well as all the players
        for (Team t : teamList) {
            int offPhil = t.offPhilosophy != null ? t.offPhilosophy.ordinal() : OffensivePhilosophy.MULTIPLE.ordinal();
            int defSys = t.defSystem != null ? t.defSystem.ordinal() : DefensiveSystem.BASE_4_3.ordinal();
            ProgramProfile profile = t.programProfile;
            sb.append(t.conference).append(",").append(t.name).append(",").append(t.abbr).append(",")
                    .append(profile.tradition).append(",").append(profile.fanbase).append(",")
                    .append(profile.donors).append(",").append(profile.footprint).append(",")
                    .append(profile.pipeline).append(",").append(profile.momentum).append(",")
                    .append(t.totalWins - t.wins).append(",").append(t.totalLosses - t.losses).append(",")
                    .append(t.totalCCs).append(",").append(t.totalNCs).append(",")
                    .append(t.encodeRivalries()).append(",")
                    .append(t.totalNCLosses).append(",").append(t.totalCCLosses).append(",")
                    .append(t.totalBowls).append(",").append(t.totalBowlLosses).append(",")
                    .append(t.showPopups ? 1 : 0).append(",")
                    .append(t.yearStartWinStreak.getStreakCSV()).append(",")
                    .append(t.teamTVDeal).append(",").append(t.confTVDeal).append(",")
                    .append(offPhil).append(",").append(defSys).append(",")
                    .append(profile.finishHistoryCsv()).append(",")
                    .append(profile.draftHistoryCsv()).append(",")
                    .append(t.programProfileUpdatedThisOffseason).append(",")
                    .append(profile.annualDeltaCsv()).append("%")
                    .append(t.evenYearHomeOpp).append("%\n");
            sb.append(t.getPlayerInfoSaveFile());
            sb.append("END_PLAYERS\n");
            sb.append(t.specialTeamsDepthSaveLine() + "\n");
        }

        // Save history of the user's team of the W-L and bowl results each year
        sb.append(userTeam.name + "\n");
        for (String s : userTeam.teamHistory) {
            sb.append(s + "\n");
        }
        sb.append("END_USER_TEAM\n");

        // Legacy bless/curse markers (always NULL; kept for save-format compatibility)
        sb.append("NULL\n");
        sb.append("END_BLESS_TEAM\n");
        sb.append("NULL\n");
        sb.append("END_CURSE_TEAM\n");

        // Save league records
        sb.append(leagueRecords.getRecordsStr());
        sb.append("END_LEAGUE_RECORDS\n");

        sb.append(yearStartLongestWinStreak.getStreakCSV());
        sb.append("\nEND_LEAGUE_WIN_STREAK\n");

        // Save user team records
        sb.append(userTeamRecords.getRecordsStr());
        sb.append("END_USER_TEAM_RECORDS\n");

        // Save all the Hall of Famers
        for (String s : userTeam.hallOfFame) {
            sb.append(s + "\n");
        }
        sb.append("END_HALL_OF_FAME\n");
        appendSeasonProgressBlock(sb);
        appendScheduleBlock(sb);
        appendTeamSeasonBlock(sb);
        if (oocContracts != null) {
            oocContracts.appendSave(sb);
        }

        if (OffseasonSession.ready() && OffseasonSession.league == this) {
            appendOffseasonBlock(sb);
        }

        // Actually write to the file
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(saveFile), "utf-8"))) {
            writer.write(sb.toString());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void appendSeasonProgressBlock(StringBuilder sb) {
        sb.append("SEASON_PROGRESS\n");
        sb.append("currentWeek,").append(currentWeek).append("\n");
        sb.append("hasScheduledBowls,").append(hasScheduledBowls ? 1 : 0).append("\n");
        sb.append("END_SEASON_PROGRESS\n");
    }

    private void restoreSeasonProgress(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null && !line.equals("END_SEASON_PROGRESS")) {
            if (line.startsWith("currentWeek,")) {
                currentWeek = Integer.parseInt(line.substring("currentWeek,".length()).trim());
            } else if (line.startsWith("hasScheduledBowls,")) {
                hasScheduledBowls = "1".equals(line.substring("hasScheduledBowls,".length()).trim());
            }
        }
    }

    private void syncConferenceWeeksFromCurrentWeek() {
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

    private void appendScheduleBlock(StringBuilder sb) {
        sb.append("SCHEDULE\n");
        for (Team team : teamList) {
            sb.append(team.abbr).append(",").append(team.byeWeek);
            for (int week = 0; week < REGULAR_SEASON_WEEKS; week++) {
                sb.append(",");
                if (team.byeWeek == week) {
                    sb.append("BYE");
                    continue;
                }
                Game game = week < team.gameSchedule.size() ? team.gameSchedule.get(week) : null;
                if (game == null) {
                    sb.append("-");
                } else if (game.homeTeam == team) {
                    sb.append(encodeScheduleEntry(game, true));
                } else {
                    sb.append(encodeScheduleEntry(game, false));
                }
            }
            sb.append("\n");
        }
        sb.append("END_SCHEDULE\n");
    }

    /** Matchup token; home side carries full result payload when the game is final. */
    private static String encodeScheduleEntry(Game game, boolean homeSide) {
        String opp = homeSide ? game.awayTeam.abbr : game.homeTeam.abbr;
        String base = (homeSide ? "H:" : "A:") + opp;
        if (!game.hasPlayed) {
            return base;
        }
        if (!homeSide) {
            return base + "|1";
        }
        StringBuilder sb = new StringBuilder(base);
        sb.append("|1|").append(game.homeScore).append('|').append(game.awayScore)
                .append('|').append(game.homeYards).append('|').append(game.awayYards)
                .append('|').append(game.homeTOs).append('|').append(game.awayTOs)
                .append('|').append(game.numOT)
                .append('|').append(encodeQuarterScores(game.homeQScore))
                .append('|').append(encodeQuarterScores(game.awayQScore));
        return sb.toString();
    }

    private static String encodeQuarterScores(int[] q) {
        StringBuilder sb = new StringBuilder();
        int n = q == null ? 0 : Math.min(4, q.length);
        for (int i = 0; i < 4; i++) {
            if (i > 0) sb.append('#');
            sb.append(i < n ? q[i] : 0);
        }
        return sb.toString();
    }

    private void restoreScheduleFromSave(BufferedReader reader) throws IOException {
        for (Team team : teamList) {
            team.gameSchedule.clear();
            team.byeWeek = -1;
            for (int week = 0; week < REGULAR_SEASON_WEEKS; week++) {
                team.gameSchedule.add(null);
            }
        }
        String line;
        while ((line = reader.readLine()) != null && !line.equals("END_SCHEDULE")) {
            String[] parts = line.split(",", -1);
            if (parts.length < 2) {
                continue;
            }
            Team team = findTeamAbbr(parts[0]);
            if (team == null) {
                continue;
            }
            team.byeWeek = Integer.parseInt(parts[1]);
            for (int week = 0; week < REGULAR_SEASON_WEEKS && week + 2 < parts.length; week++) {
                String entry = parts[week + 2];
                if (entry.equals("BYE") || entry.equals("-") || entry.isEmpty()) {
                    continue;
                }
                applyScheduleEntry(team, week, entry);
            }
        }
        for (Conference conference : conferences) {
            conference.resetSeason();
        }
    }

    private void applyScheduleEntry(Team team, int week, String entry) {
        String[] pipe = entry.split("\\|", -1);
        String matchup = pipe[0];
        if (!matchup.startsWith("H:") && !matchup.startsWith("A:")) {
            return;
        }
        boolean home = matchup.startsWith("H:");
        String oppAbbr = matchup.substring(2);
        Team opponent = findTeamAbbr(oppAbbr);
        if (opponent == null) {
            return;
        }
        Game game = team.gameSchedule.get(week);
        if (game == null) {
            boolean sameConf = team.conference.equals(opponent.conference);
            String name = sameConf ? "In Conf" : "OOC";
            Team homeTeam = home ? team : opponent;
            Team awayTeam = home ? opponent : team;
            game = new Game(homeTeam, awayTeam, name);
            homeTeam.gameSchedule.set(week, game);
            awayTeam.gameSchedule.set(week, game);
        }
        if (pipe.length >= 10 && "1".equals(pipe[1]) && !game.hasPlayed) {
            applySavedGameResult(game, pipe);
        } else if (pipe.length >= 2 && "1".equals(pipe[1])) {
            // Away-side marker only; result applied from home payload.
            if (!game.hasPlayed && pipe.length >= 10) {
                applySavedGameResult(game, pipe);
            }
        }
    }

    private static void applySavedGameResult(Game game, String[] pipe) {
        try {
            game.homeScore = Integer.parseInt(pipe[2]);
            game.awayScore = Integer.parseInt(pipe[3]);
            game.homeYards = Integer.parseInt(pipe[4]);
            game.awayYards = Integer.parseInt(pipe[5]);
            game.homeTOs = Integer.parseInt(pipe[6]);
            game.awayTOs = Integer.parseInt(pipe[7]);
            game.numOT = Integer.parseInt(pipe[8]);
            decodeQuarterScores(game.homeQScore, pipe[9]);
            decodeQuarterScores(game.awayQScore, pipe[10]);
            game.hasPlayed = true;
        } catch (Exception ignored) {
            // Leave game unplayed if payload is corrupt.
        }
    }

    private static void decodeQuarterScores(int[] q, String encoded) {
        if (q == null || encoded == null) return;
        String[] parts = encoded.split("#", -1);
        for (int i = 0; i < q.length; i++) {
            q[i] = 0;
        }
        for (int i = 0; i < parts.length && i < q.length; i++) {
            try {
                q[i] = Integer.parseInt(parts[i].trim());
            } catch (Exception e) {
                q[i] = 0;
            }
        }
    }

    private void appendTeamSeasonBlock(StringBuilder sb) {
        sb.append("TEAM_SEASON\n");
        for (Team t : teamList) {
            sb.append(t.abbr).append('|')
                    .append(t.wins).append('|').append(t.losses).append('|')
                    .append(t.teamPoints).append('|').append(t.teamOppPoints).append('|')
                    .append(t.teamYards).append('|').append(t.teamOppYards).append('|')
                    .append(t.teamPassYards).append('|').append(t.teamRushYards).append('|')
                    .append(t.teamOppPassYards).append('|').append(t.teamOppRushYards).append('|')
                    .append(t.teamTODiff).append('|')
                    .append(t.winStreak != null ? t.winStreak.getStreakCSV() : "0," + t.abbr + ",0,0")
                    .append('|')
                    .append(encodeGameWl(t)).append('|')
                    .append(encodeWinsAgainst(t)).append('|')
                    .append(encodeRivalryResults(t))
                    .append('\n');
        }
        sb.append("END_TEAM_SEASON\n");
    }

    private static String encodeGameWl(Team t) {
        if (t.gameWLSchedule == null || t.gameWLSchedule.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < t.gameWLSchedule.size(); i++) {
            if (i > 0) sb.append(';');
            sb.append(t.gameWLSchedule.get(i));
        }
        return sb.toString();
    }

    private static String encodeWinsAgainst(Team t) {
        if (t.gameWinsAgainst == null || t.gameWinsAgainst.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < t.gameWinsAgainst.size(); i++) {
            if (i > 0) sb.append(';');
            Team opp = t.gameWinsAgainst.get(i);
            sb.append(opp != null ? opp.abbr : "XXX");
        }
        return sb.toString();
    }

    private static String encodeRivalryResults(Team t) {
        if (t.rivalryResults == null || t.rivalryResults.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (java.util.Map.Entry<String, Boolean> e : t.rivalryResults.entrySet()) {
            if (!first) sb.append(';');
            first = false;
            sb.append(e.getKey()).append(':').append(Boolean.TRUE.equals(e.getValue()) ? 1 : 0);
        }
        return sb.toString();
    }

    private void restoreTeamSeasonBlock(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null && !line.equals("END_TEAM_SEASON")) {
            String[] parts = line.split("\\|", -1);
            if (parts.length < 12) continue;
            Team t = findTeamAbbr(parts[0]);
            if (t == null) continue;
            try {
                int seasonWins = Integer.parseInt(parts[1]);
                int seasonLosses = Integer.parseInt(parts[2]);
                int careerWins = t.totalWins;
                int careerLosses = t.totalLosses;
                t.wins = seasonWins;
                t.losses = seasonLosses;
                t.totalWins = careerWins + seasonWins;
                t.totalLosses = careerLosses + seasonLosses;
                t.teamPoints = Integer.parseInt(parts[3]);
                t.teamOppPoints = Integer.parseInt(parts[4]);
                t.teamYards = Integer.parseInt(parts[5]);
                t.teamOppYards = Integer.parseInt(parts[6]);
                t.teamPassYards = Integer.parseInt(parts[7]);
                t.teamRushYards = Integer.parseInt(parts[8]);
                t.teamOppPassYards = Integer.parseInt(parts[9]);
                t.teamOppRushYards = Integer.parseInt(parts[10]);
                t.teamTODiff = Integer.parseInt(parts[11]);
                if (parts.length > 12 && !parts[12].isEmpty()) {
                    String[] streak = parts[12].split(",");
                    if (streak.length >= 4) {
                        t.winStreak = new TeamStreak(
                                Integer.parseInt(streak[2]),
                                Integer.parseInt(streak[3]),
                                Integer.parseInt(streak[0]),
                                streak[1]);
                    }
                }
                t.gameWLSchedule.clear();
                if (parts.length > 13 && !parts[13].isEmpty()) {
                    for (String wl : parts[13].split(";", -1)) {
                        if (!wl.isEmpty()) t.gameWLSchedule.add(wl);
                    }
                }
                t.gameWinsAgainst.clear();
                if (parts.length > 14 && !parts[14].isEmpty()) {
                    for (String abbr : parts[14].split(";", -1)) {
                        Team opp = findTeamAbbr(abbr);
                        if (opp != null) t.gameWinsAgainst.add(opp);
                    }
                }
                if (t.rivalryResults == null) {
                    t.rivalryResults = new java.util.HashMap<>();
                } else {
                    t.rivalryResults.clear();
                }
                if (parts.length > 15 && !parts[15].isEmpty()) {
                    for (String pair : parts[15].split(";", -1)) {
                        int colon = pair.lastIndexOf(':');
                        if (colon <= 0) continue;
                        String abbr = pair.substring(0, colon);
                        boolean won = "1".equals(pair.substring(colon + 1));
                        t.rivalryResults.put(abbr, won);
                    }
                }
            } catch (Exception ignored) {
                // Skip corrupt team season rows.
            }
        }
    }

    private void appendOffseasonBlock(StringBuilder sb) {
        LeagueOffseason off = OffseasonSession.offseason;
        sb.append("OFFSEASON,").append(OffseasonSession.phase.name()).append("\n");
        sb.append("BUDGETS\n");
        for (Team t : teamList) {
            sb.append(t.abbr).append(",").append(t.recruitMoney).append("\n");
        }
        sb.append("END_BUDGETS\n");
        sb.append("RETAINED\n");
        for (Team t : teamList) {
            for (Player p : t.getAllPlayers()) {
                if (p.retainedThisOffseason) {
                    sb.append(t.abbr).append(",").append(p.position).append(",")
                            .append(p.name).append(",").append(p.year).append("\n");
                }
            }
        }
        sb.append("END_RETAINED\n");
        sb.append("PORTAL\n");
        if (off != null) {
            for (Player p : off.transferPortal) {
                String prior = p.priorTeam != null ? p.priorTeam.abbr
                        : (p.team != null ? p.team.abbr : "XXX");
                Team seed = userTeam != null ? userTeam : teamList.get(0);
                sb.append(prior).append("|").append(seed.playerToSaveLine(p)).append("%\n");
            }
        }
        sb.append("END_PORTAL\n");
        sb.append("HS\n");
        if (off != null) {
            Team seed = userTeam != null ? userTeam : teamList.get(0);
            for (Player p : off.hsClass) {
                sb.append(seed.playerToSaveLine(p)).append("%\n");
            }
        }
        sb.append("END_HS\n");
        sb.append("END_OFFSEASON\n");
    }

    private void restoreOffseasonFromSave(BufferedReader bufferedReader, String headerLine) throws IOException {
        loadedInOffseason = true;
        String phaseStr = headerLine.substring("OFFSEASON,".length()).trim();
        loadedOffseasonPhase = OffseasonSession.phaseFromString(phaseStr);
        LeagueOffseason off = new LeagueOffseason(this);
        offseason = off;

        String line = bufferedReader.readLine();
        if (line != null && line.equals("BUDGETS")) {
            while ((line = bufferedReader.readLine()) != null && !line.equals("END_BUDGETS")) {
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    Team t = findTeamAbbr(parts[0]);
                    if (t != null) {
                        try {
                            t.recruitMoney = Integer.parseInt(parts[1]);
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }

        line = bufferedReader.readLine();
        if (line != null && line.equals("RETAINED")) {
            while ((line = bufferedReader.readLine()) != null && !line.equals("END_RETAINED")) {
                String[] parts = line.split(",");
                if (parts.length >= 4) {
                    Team t = findTeamAbbr(parts[0]);
                    if (t == null) continue;
                    String pos = parts[1];
                    String name = parts[2];
                    int year;
                    try {
                        year = Integer.parseInt(parts[3]);
                    } catch (Exception e) {
                        continue;
                    }
                    for (Player p : t.getAllPlayers()) {
                        if (pos.equals(p.position) && name.equals(p.name) && p.year == year) {
                            p.retainedThisOffseason = true;
                            break;
                        }
                    }
                }
            }
        }

        Team seed = userTeam != null ? userTeam : teamList.get(0);
        line = bufferedReader.readLine();
        if (line != null && line.equals("PORTAL")) {
            while ((line = bufferedReader.readLine()) != null && !line.equals("END_PORTAL")) {
                if (line.isEmpty()) continue;
                String priorAbbr = "XXX";
                String playerLine = line;
                int pipe = line.indexOf('|');
                if (pipe >= 0) {
                    priorAbbr = line.substring(0, pipe);
                    playerLine = line.substring(pipe + 1);
                }
                if (playerLine.endsWith("%")) {
                    playerLine = playerLine.substring(0, playerLine.length() - 1);
                }
                Player p = seed.parsePlayerSaveLine(playerLine, false, false);
                if (p == null) continue;
                Team prior = findTeamAbbr(priorAbbr);
                p.priorTeam = prior;
                p.team = null;
                off.transferPortal.add(p);
            }
        }

        line = bufferedReader.readLine();
        if (line != null && line.equals("HS")) {
            while ((line = bufferedReader.readLine()) != null && !line.equals("END_HS")) {
                if (line.isEmpty()) continue;
                String playerLine = line.endsWith("%") ? line.substring(0, line.length() - 1) : line;
                Player p = seed.parsePlayerSaveLine(playerLine, false, false);
                if (p == null) continue;
                p.team = null;
                p.cost = NilMoney.marketValue(p);
                off.hsClass.add(p);
            }
        }

        // Consume END_OFFSEASON if present
        line = bufferedReader.readLine();
        while (line != null && !line.equals("END_OFFSEASON")) {
            line = bufferedReader.readLine();
        }

        OffseasonSession.begin(this, off, loadedOffseasonPhase);
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






