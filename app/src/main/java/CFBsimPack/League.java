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
    /** Bowl scheduling week (after CCG). */
    public static final int WEEK_BOWL_SCHEDULE = 13;
    /** Bowl games played. */
    public static final int WEEK_BOWLS = 14;
    /** National championship. */
    public static final int WEEK_NCG = 15;
    /** Season complete; recruiting begins. */
    public static final int WEEK_SEASON_END = 16;
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


    //Current week, 1-14
    public int currentWeek;

    //Bowl Games
    public boolean hasScheduledBowls;
    public Game semiG14;
    public Game semiG23;
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

    public String[] bowlNames = {"Lilac Bowl", "Apple Bowl", "Salty Bowl", "Salsa Bowl", "Mango Bowl",
            "Patriot Bowl", "Salad Bowl", "Frost Bowl", "Tropical Bowl", "I'd Rather Bowl"};

    public static final String[] donationNames = {"Mark Eeslee", "Lee Sin", "Brent Uttwipe", "Gabriel Kemble",
            "Jon Stupak", "Kiergan Ren", "Dean Steinkuhler", "Declan Greally", "Parks Wilson", "Darren Ryder"};

    /**
     * Creates League, sets up Conferences, reads team names and conferences from file.
     * Also schedules games for every team.
     */
    public League(String namesCSV, String lastNamesCSV, String teamsCSV) {
        heismanDecided = false;
        hasScheduledBowls = false;
        bowlGames = new Game[10];
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
        bowlGames = new Game[10];
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
        prepareSeasonSchedule();
        hasScheduledBowls = false;
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
        bowlGames = new Game[10];
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
            if (saveVersion < 2) {
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

            // Optional schedule + OOC contracts + mid-offseason blocks
            line = bufferedReader.readLine();
            boolean restoredSchedule = false;
            if (line != null && line.equals("SCHEDULE")) {
                restoreScheduleFromSave(bufferedReader);
                restoredSchedule = true;
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
     * Plays week. If normal week, handled by conferences. If bowl week, handled here.
     */
    public void playWeek() {
        if ( currentWeek <= WEEK_CCG ) {
            for (int i = 0; i < conferences.size(); ++i) {
                conferences.get(i).playWeek();
            }
        }

        if ( currentWeek == WEEK_BOWL_SCHEDULE ) {
            // After CCG: schedule bowls
            for (int i = 0; i < teamList.size(); ++i) {
                teamList.get(i).updatePollScore();
            }
            Collections.sort( teamList, new TeamCompPoll() );

            schedBowlGames();
        } else if ( currentWeek == WEEK_BOWLS ) {
            ArrayList<Player> heismans = getHeisman();
            heismanHistory.add(heismans.get(0).position + " " + heismans.get(0).getInitialName() + " [" + heismans.get(0).getYrStr() + "], "
                    + heismans.get(0).team.abbr + " (" + heismans.get(0).team.wins + "-" + heismans.get(0).team.losses + ")");
            playBowlGames();
        } else if ( currentWeek == WEEK_NCG ) {
            ncg.playGame();
            if ( ncg.homeScore > ncg.awayScore ) {
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

        setTeamRanks();
        updateLongestActiveWinStreak();

        currentWeek++;
    }
    
    /**
     * Schedules bowl games based on team rankings.
     */
    public void schedBowlGames() {
        //bowl week
        for (int i = 0; i < teamList.size(); ++i) {
            teamList.get(i).updatePollScore();
        }
        Collections.sort( teamList, new TeamCompPoll() );

        //semifinals
        semiG14 = new Game( teamList.get(0), teamList.get(3), "Semis, 1v4" );
        teamList.get(0).gameSchedule.add(semiG14);
        teamList.get(3).gameSchedule.add(semiG14);

        semiG23 = new Game( teamList.get(1), teamList.get(2), "Semis, 2v3" );
        teamList.get(1).gameSchedule.add(semiG23);
        teamList.get(2).gameSchedule.add(semiG23);

        //other bowl games
        //bowlNames = {"Lilac Bowl", "Apple Bowl", "Salty Bowl", "Salsa Bowl", "Mango Bowl",
        //             "Patriot Bowl", "Salad Bowl", "Frost Bowl", "Tropical Bowl", "Music Bowl"};
        bowlGames[0] = new Game( teamList.get(4), teamList.get(6), bowlNames[0] );
        teamList.get(4).gameSchedule.add(bowlGames[0]);
        teamList.get(6).gameSchedule.add(bowlGames[0]);

        bowlGames[1] = new Game( teamList.get(5), teamList.get(7), bowlNames[1] );
        teamList.get(5).gameSchedule.add(bowlGames[1]);
        teamList.get(7).gameSchedule.add(bowlGames[1]);

        bowlGames[2] = new Game( teamList.get(8), teamList.get(14), bowlNames[2] );
        teamList.get(8).gameSchedule.add(bowlGames[2]);
        teamList.get(14).gameSchedule.add(bowlGames[2]);

        bowlGames[3] = new Game( teamList.get(9), teamList.get(15), bowlNames[3] );
        teamList.get(9).gameSchedule.add(bowlGames[3]);
        teamList.get(15).gameSchedule.add(bowlGames[3]);

        bowlGames[4] = new Game( teamList.get(10), teamList.get(11), bowlNames[4] );
        teamList.get(10).gameSchedule.add(bowlGames[4]);
        teamList.get(11).gameSchedule.add(bowlGames[4]);

        bowlGames[5] = new Game( teamList.get(12), teamList.get(13), bowlNames[5] );
        teamList.get(12).gameSchedule.add(bowlGames[5]);
        teamList.get(13).gameSchedule.add(bowlGames[5]);

        bowlGames[6] = new Game( teamList.get(16), teamList.get(20), bowlNames[6] );
        teamList.get(16).gameSchedule.add(bowlGames[6]);
        teamList.get(20).gameSchedule.add(bowlGames[6]);

        bowlGames[7] = new Game( teamList.get(17), teamList.get(21), bowlNames[7] );
        teamList.get(17).gameSchedule.add(bowlGames[7]);
        teamList.get(21).gameSchedule.add(bowlGames[7]);

        bowlGames[8] = new Game( teamList.get(18), teamList.get(22), bowlNames[8] );
        teamList.get(18).gameSchedule.add(bowlGames[8]);
        teamList.get(22).gameSchedule.add(bowlGames[8]);

        bowlGames[9] = new Game( teamList.get(19), teamList.get(23), bowlNames[9] );
        teamList.get(19).gameSchedule.add(bowlGames[9]);
        teamList.get(23).gameSchedule.add(bowlGames[9]);

        hasScheduledBowls = true;

    }

    /**
     * Actually plays each bowl game.
     */

    public void playBowlGames() {
        for (Game g : bowlGames) {
            playBowl(g);
        }

        semiG14.playGame();
        semiG23.playGame();
        Team semi14winner;
        Team semi23winner;
        if ( semiG14.homeScore > semiG14.awayScore ) {
            semiG14.homeTeam.semiFinalWL = "SFW";
            semiG14.awayTeam.semiFinalWL = "SFL";
            semiG14.awayTeam.totalBowlLosses++;
            semiG14.homeTeam.totalBowls++;
            semi14winner = semiG14.homeTeam;
        } else {
            semiG14.homeTeam.semiFinalWL = "SFL";
            semiG14.awayTeam.semiFinalWL = "SFW";
            semiG14.homeTeam.totalBowlLosses++;
            semiG14.awayTeam.totalBowls++;
            semi14winner = semiG14.awayTeam;
        }
        if ( semiG23.homeScore > semiG23.awayScore ) {
            semiG23.homeTeam.semiFinalWL = "SFW";
            semiG23.awayTeam.semiFinalWL = "SFL";
            semiG23.homeTeam.totalBowls++;
            semiG23.awayTeam.totalBowlLosses++;
            semi23winner = semiG23.homeTeam;
        } else {
            semiG23.homeTeam.semiFinalWL = "SFL";
            semiG23.awayTeam.semiFinalWL = "SFW";
            semiG23.awayTeam.totalBowls++;
            semiG23.homeTeam.totalBowlLosses++;
            semi23winner = semiG23.awayTeam;
        }

        //schedule NCG
        ncg = new Game( semi14winner, semi23winner, "NCG" );
        semi14winner.gameSchedule.add( ncg );
        semi23winner.gameSchedule.add(ncg);

    }

    /**
     * Plays a particular bowl game
     * @param g bowl game to be played
     */
    private void playBowl(Game g) {
        g.playGame();
        if ( g.homeScore > g.awayScore ) {
            g.homeTeam.semiFinalWL = "BW";
            g.awayTeam.semiFinalWL = "BL";
            g.homeTeam.totalBowls++;
            g.awayTeam.totalBowlLosses++;
        } else {
            g.homeTeam.semiFinalWL = "BL";
            g.awayTeam.semiFinalWL = "BW";
            g.homeTeam.totalBowlLosses++;
            g.awayTeam.totalBowls++;
        }
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

        Collections.sort( teamList, new TeamCompPrestige() );
        for (int t = 0; t < teamList.size(); ++t) {
            teamList.get(t).rankTeamPrestige = t+1;
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
                if (p instanceof PlayerQB) {
                    PlayerQB pqb = (PlayerQB) p;
                    heismanTop5 += " QB " + pqb.name + " [" + pqb.getYrStr() +
                            "]\n \t\t(" + pqb.statsTD + " TDs, " + pqb.statsInt + " Int, " + pqb.statsPassYards + " Yds)\n\n";
                } else if (p instanceof PlayerRB) {
                    PlayerRB prb = (PlayerRB) p;
                    heismanTop5 += " RB " + prb.name + " [" + prb.getYrStr() +
                            "]\n \t\t(" + prb.statsTD + " TDs, " + prb.statsFumbles + " Fum, " + prb.statsRushYards + " Yds)\n\n";
                } else if (p instanceof PlayerWR) {
                    PlayerWR pwr = (PlayerWR) p;
                    heismanTop5 += " WR " + pwr.name + " [" + pwr.getYrStr() +
                            "]\n \t\t(" + pwr.statsTD + " TDs, " + pwr.statsFumbles + " Fum, " + pwr.statsRecYards + " Yds)\n\n";
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
                if (p instanceof PlayerQB) {
                    PlayerQB pqb = (PlayerQB) p;
                    heismanTop5 += " QB " + pqb.getInitialName() + ": " + p.getHeismanScore() + " votes\n\t("
                            + pqb.statsTD + " TDs, " + pqb.statsInt + " Int, " + pqb.statsPassYards + " Yds)\n\n";
                } else if (p instanceof PlayerRB) {
                    PlayerRB prb = (PlayerRB) p;
                    heismanTop5 += " RB " + prb.getInitialName() + ": " + p.getHeismanScore() + " votes\n\t("
                            + prb.statsTD + " TDs, " + prb.statsFumbles + " Fum, " + prb.statsRushYards + " Yds)\n\n";
                } else if (p instanceof PlayerWR) {
                    PlayerWR pwr = (PlayerWR) p;
                    heismanTop5 += " WR " + pwr.getInitialName() + ": " + p.getHeismanScore() + " votes\n\t("
                            + pwr.statsTD + " TDs, " + pwr.statsFumbles + " Fum, " + pwr.statsRecYards + " Yds)\n\n";
                }
            }
            String heismanStats = "";
            String heismanWinnerStr = "";
            if (heisman instanceof PlayerQB) {
                //qb heisman
                PlayerQB heisQB = (PlayerQB) heisman;
                heismanWinnerStr = "Congratulations to the Player of the Year, " + heisQB.team.abbr +
                        " QB " + heisQB.name + " [" + heisman.getYrStr() + "], who had " +
                        heisQB.statsTD + " TDs, just " + heisQB.statsInt + " interceptions, and " +
                        heisQB.statsPassYards + " passing yards. He led " + heisQB.team.name +
                        " to a " + heisQB.team.wins + "-" + heisQB.team.losses + " record and a #" + heisQB.team.rankTeamPollScore +
                        " poll ranking.";
                heismanStats = heismanWinnerStr + "\n\nFull Results:" + heismanTop5;
            } else if (heisman instanceof PlayerRB) {
                //rb heisman
                PlayerRB heisRB = (PlayerRB) heisman;
                heismanWinnerStr = "Congratulations to the Player of the Year, " + heisRB.team.abbr +
                        " RB " + heisRB.name + " [" + heisman.getYrStr() + "], who had " +
                        heisRB.statsTD + " TDs, just " + heisRB.statsFumbles + " fumbles, and " +
                        heisRB.statsRushYards + " rushing yards. He led " + heisRB.team.name +
                        " to a " + heisRB.team.wins + "-" + heisRB.team.losses + " record and a #" + heisRB.team.rankTeamPollScore +
                        " poll ranking.";
                heismanStats = heismanWinnerStr + "\n\nFull Results:" + heismanTop5;
            } else if (heisman instanceof PlayerWR) {
                //wr heisman
                PlayerWR heisWR = (PlayerWR) heisman;
                heismanWinnerStr = "Congratulations to the Player of the Year, " + heisWR.team.abbr +
                        " WR " + heisWR.name + " [" + heisman.getYrStr() + "], who had " +
                        heisWR.statsTD + " TDs, just " + heisWR.statsFumbles + " fumbles, and " +
                        heisWR.statsRecYards + " receiving yards. He led " + heisWR.team.name +
                        " to a " + heisWR.team.wins + "-" + heisWR.team.losses + " record and a #" + heisWR.team.rankTeamPollScore +
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
            ArrayList<PlayerQB> qbs = new ArrayList<>();
            ArrayList<PlayerRB> rbs = new ArrayList<>();
            ArrayList<PlayerFB> fbs = new ArrayList<>();
            ArrayList<PlayerWR> wrs = new ArrayList<>();
            ArrayList<PlayerTE> tes = new ArrayList<>();
            ArrayList<PlayerOL> ols = new ArrayList<>();
            ArrayList<PlayerK> ks = new ArrayList<>();
            ArrayList<PlayerS> ss = new ArrayList<>();
            ArrayList<PlayerCB> cbs = new ArrayList<>();
            ArrayList<PlayerEDGE> edges = new ArrayList<>();
            ArrayList<PlayerDL> dls = new ArrayList<>();
            ArrayList<PlayerLB> lbs = new ArrayList<>();

            for (Conference c : conferences) {
                c.getAllConfPlayers();
                int idx = 0;
                qbs.add((PlayerQB) c.allConfPlayers.get(idx++));
                rbs.add((PlayerRB) c.allConfPlayers.get(idx++));
                rbs.add((PlayerRB) c.allConfPlayers.get(idx++));
                fbs.add((PlayerFB) c.allConfPlayers.get(idx++));
                for (int i = 0; i < 3; ++i) wrs.add((PlayerWR) c.allConfPlayers.get(idx++));
                tes.add((PlayerTE) c.allConfPlayers.get(idx++));
                for (int i = 0; i < 5; ++i) ols.add((PlayerOL) c.allConfPlayers.get(idx++));
                ks.add((PlayerK) c.allConfPlayers.get(idx++));
                ss.add((PlayerS) c.allConfPlayers.get(idx++));
                for (int i = 0; i < 3; ++i) cbs.add((PlayerCB) c.allConfPlayers.get(idx++));
                for (int i = 0; i < 2; ++i) edges.add((PlayerEDGE) c.allConfPlayers.get(idx++));
                for (int i = 0; i < 3; ++i) dls.add((PlayerDL) c.allConfPlayers.get(idx++));
                for (int i = 0; i < 3; ++i) lbs.add((PlayerLB) c.allConfPlayers.get(idx++));
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
            if (p instanceof PlayerQB) {
                PlayerQB pqb = (PlayerQB) p;
                allAmerican.append(" QB " + pqb.name + " [" + pqb.getYrStr() + "]\n \t\t" +
                        pqb.statsTD + " TDs, " + pqb.statsInt + " Int, " + pqb.statsPassYards + " Yds\n");
            } else if (p instanceof PlayerRB) {
                PlayerRB prb = (PlayerRB) p;
                allAmerican.append(" RB " + prb.name + " [" + prb.getYrStr() + "]\n \t\t" +
                        prb.statsTD + " TDs, " + prb.statsFumbles + " Fum, " + prb.statsRushYards + " Yds\n");
            } else if (p instanceof PlayerWR) {
                PlayerWR pwr = (PlayerWR) p;
                allAmerican.append(" WR " + pwr.name + " [" + pwr.getYrStr() + "]\n \t\t" +
                        pwr.statsTD + " TDs, " + pwr.statsFumbles + " Fum, " + pwr.statsRecYards + " Yds\n");
            } else if (p instanceof PlayerK) {
                PlayerK pk = (PlayerK) p;
                allAmerican.append(" K " + pk.name + " [" + pk.getYrStr() + "]\n \t\t" +
                        "FGs: " + pk.statsFGMade + "/" + pk.statsFGAtt + ", XPs: " + pk.statsXPMade + "/" + pk.statsXPAtt + "\n");
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
            if (p instanceof PlayerQB) {
                PlayerQB pqb = (PlayerQB) p;
                sb.append(" QB " + pqb.name + " [" + pqb.getYrStr() + "]\n \t\t" +
                        pqb.statsTD + " TDs, " + pqb.statsInt + " Int, " + pqb.statsPassYards + " Yds\n");
            } else if (p instanceof PlayerRB) {
                PlayerRB prb = (PlayerRB) p;
                sb.append(" RB " + prb.name + " [" + prb.getYrStr() + "]\n \t\t" +
                        prb.statsTD + " TDs, " + prb.statsFumbles + " Fum, " + prb.statsRushYards + " Yds\n");
            } else if (p instanceof PlayerWR) {
                PlayerWR pwr = (PlayerWR) p;
                sb.append(" WR " + pwr.name + " [" + pwr.getYrStr() + "]\n \t\t" +
                        pwr.statsTD + " TDs, " + pwr.statsFumbles + " Fum, " + pwr.statsRecYards + " Yds\n");
            } else if (p instanceof PlayerK) {
                PlayerK pk = (PlayerK) p;
                sb.append(" K " + pk.name + " [" + pk.getYrStr() + "]\n \t\t" +
                        "FGs: " + pk.statsFGMade + "/" + pk.statsFGAtt + ", XPs: " + pk.statsXPMade + "/" + pk.statsXPAtt + "\n");
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
        14 = prestige
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
            case 14: Collections.sort( teams, new TeamCompPrestige() );
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    rankings.add(t.getRankStrStarUser(i + 1) + "," + t.strRepWithBowlResults() + "," + t.teamPrestige);
                }
                break;
            case 15: Collections.sort( teams, new TeamCompRecruitClass() );
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    rankings.add(t.getRankStrStarUser(i + 1) + "," + t.strRepWithPrestige() + "," + t.getRecruitingClassRat());
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
     * Get list of teams and their prestige, used for selecting when a new game is started
     * @return array of all the teams
     */
    public String[] getTeamListStr() {
        String[] teams = new String[teamList.size()];
        for (int i = 0; i < teamList.size(); ++i){
            teams[i] = teamList.get(i).conference + ": " +
                    teamList.get(i).name + ", Pres: " + teamList.get(i).teamPrestige;
        }
        return teams;
    }

    /**
     * Get list of all bowl games and their predicted teams
     * @return string of all the bowls and their predictions
     */
    public String getBowlGameWatchStr() {
        //if bowls arent scheduled yet, give predictions
        if (!hasScheduledBowls) {

            for (int i = 0; i < teamList.size(); ++i) {
                teamList.get(i).updatePollScore();
            }
            Collections.sort(teamList, new TeamCompPoll());

            StringBuilder sb = new StringBuilder();
            Team t1;
            Team t2;

            sb.append("Semifinal 1v4:\n\t\t");
            t1 = teamList.get(0);
            t2 = teamList.get(3);
            sb.append(t1.strRep() + " vs " + t2.strRep() + "\n\n");

            sb.append("Semifinal 2v3:\n\t\t");
            t1 = teamList.get(1);
            t2 = teamList.get(2);
            sb.append(t1.strRep() + " vs " + t2.strRep() + "\n\n");

            sb.append(bowlNames[0]+":\n\t\t");
            t1 = teamList.get(4);
            t2 = teamList.get(6);
            sb.append(t1.strRep() + " vs " + t2.strRep() + "\n\n");

            sb.append(bowlNames[1]+":\n\t\t");
            t1 = teamList.get(5);
            t2 = teamList.get(7);
            sb.append(t1.strRep() + " vs " + t2.strRep() + "\n\n");

            sb.append(bowlNames[2]+":\n\t\t");
            t1 = teamList.get(8);
            t2 = teamList.get(14);
            sb.append(t1.strRep() + " vs " + t2.strRep() + "\n\n");

            sb.append(bowlNames[3]+":\n\t\t");
            t1 = teamList.get(9);
            t2 = teamList.get(15);
            sb.append(t1.strRep() + " vs " + t2.strRep() + "\n\n");

            sb.append(bowlNames[4]+":\n\t\t");
            t1 = teamList.get(10);
            t2 = teamList.get(11);
            sb.append(t1.strRep() + " vs " + t2.strRep() + "\n\n");

            sb.append(bowlNames[5]+":\n\t\t");
            t1 = teamList.get(12);
            t2 = teamList.get(13);
            sb.append(t1.strRep() + " vs " + t2.strRep() + "\n\n");

            for (int i = 6; i < 10; ++i) {
                sb.append(bowlNames[i]+":\n\t\t");
                t1 = teamList.get(10 + i);
                t2 = teamList.get(14 + i);
                sb.append(t1.strRep() + " vs " + t2.strRep() + "\n\n");
            }

            return sb.toString();

        } else {
            // Games have already been scheduled, give actual teams
            StringBuilder sb = new StringBuilder();

            sb.append("Semifinal 1v4:\n");
            sb.append(getGameSummaryBowl(semiG14));

            sb.append("\n\nSemifinal 2v3:\n");
            sb.append(getGameSummaryBowl(semiG23));

            for (int i = 0; i < bowlGames.length; ++i) {
                sb.append("\n\n"+bowlNames[i]+":\n");
                sb.append(getGameSummaryBowl(bowlGames[i]));
            }

            return sb.toString();
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
        sb.append("SAVE_VERSION,4\n");
        sb.append("TEAM_COUNT," + teamList.size() + "\n");

        // Save information about each team like W-L records, as well as all the players
        for (Team t : teamList) {
            int offPhil = t.offPhilosophy != null ? t.offPhilosophy.ordinal() : OffensivePhilosophy.MULTIPLE.ordinal();
            int defSys = t.defSystem != null ? t.defSystem.ordinal() : DefensiveSystem.BASE_4_3.ordinal();
            sb.append(t.conference + "," + t.name + "," + t.abbr + "," + t.teamPrestige + "," +
                    (t.totalWins - t.wins) + "," + (t.totalLosses - t.losses) + "," + t.totalCCs + "," + t.totalNCs + "," + t.encodeRivalries() + "," +
                    t.totalNCLosses + "," + t.totalCCLosses + "," + t.totalBowls + "," + t.totalBowlLosses + "," +
                    "1,1," + (t.showPopups ? 1 : 0) + "," +
                    t.yearStartWinStreak.getStreakCSV() + "," +  t.teamTVDeal + "," + t.confTVDeal + "," +
                    offPhil + "," + defSys + "%" + t.evenYearHomeOpp + "%\n");
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
        appendScheduleBlock(sb);
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
                    sb.append("H:").append(game.awayTeam.abbr);
                } else {
                    sb.append("A:").append(game.homeTeam.abbr);
                }
            }
            sb.append("\n");
        }
        sb.append("END_SCHEDULE\n");
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
                boolean home = entry.startsWith("H:");
                String oppAbbr = entry.substring(2);
                Team opponent = findTeamAbbr(oppAbbr);
                if (opponent == null) {
                    continue;
                }
                if (team.gameSchedule.get(week) != null) {
                    continue;
                }
                boolean sameConf = team.conference.equals(opponent.conference);
                String name = sameConf ? "In Conf" : "OOC";
                Team homeTeam = home ? team : opponent;
                Team awayTeam = home ? opponent : team;
                Game game = new Game(homeTeam, awayTeam, name);
                homeTeam.gameSchedule.set(week, game);
                awayTeam.gameSchedule.set(week, game);
            }
        }
        for (Conference conference : conferences) {
            conference.resetSeason();
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
        if (p instanceof PlayerQB) {
            PlayerQB q = (PlayerQB) p;
            return q.statsTD + " TDs · " + q.statsInt + " Int · "
                    + String.format("%,d", q.statsPassYards) + " Yds";
        } else if (p instanceof PlayerRB) {
            PlayerRB r = (PlayerRB) p;
            return r.statsTD + " TDs · " + r.statsFumbles + " Fum · "
                    + String.format("%,d", r.statsRushYards) + " Yds";
        } else if (p instanceof PlayerWR) {
            PlayerWR w = (PlayerWR) p;
            return w.statsTD + " TDs · " + w.statsFumbles + " Fum · "
                    + String.format("%,d", w.statsRecYards) + " Yds";
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
            if (p instanceof PlayerQB) {
                PlayerQB q = (PlayerQB) p;
                stats = q.statsTD + " TDs, " + q.statsInt + " Int, "
                        + String.format("%,d", q.statsPassYards) + " Yds";
            } else if (p instanceof PlayerRB) {
                PlayerRB r = (PlayerRB) p;
                stats = r.statsTD + " TDs, " + r.statsFumbles + " Fum, "
                        + String.format("%,d", r.statsRushYards) + " Yds";
            } else if (p instanceof PlayerWR) {
                PlayerWR w = (PlayerWR) p;
                stats = w.statsTD + " TDs, " + w.statsFumbles + " Fum, "
                        + String.format("%,d", w.statsRecYards) + " Yds";
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

class TeamCompPrestige implements Comparator<Team> {
    @Override
    public int compare( Team a, Team b ) {
        return a.teamPrestige > b.teamPrestige ? -1 : a.teamPrestige == b.teamPrestige ? 0 : 1;
    }
}

class TeamCompRecruitClass implements Comparator<Team> {
    @Override
    public int compare( Team a, Team b ) {
        return a.getRecruitingClassRat() > b.getRecruitingClassRat() ? -1 : a.getRecruitingClassRat() == b.getRecruitingClassRat() ? 0 : 1;
    }
}






