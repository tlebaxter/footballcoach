package CFBsimPack;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Team class.
 * @author Achi
 */
public class Team {

    public League league;

    public String name;
    public String abbr;
    public String conference;
    /** Named rivals with 0–100 strength. */
    public ArrayList<Rivalry> rivalries;
    /** Opponent abbr → won this season's rivalry game (only opponents listed as rivals). */
    public HashMap<String, Boolean> rivalryResults;
    /** Notes from {@link RivalryDynamics} for UI / season summary. */
    public ArrayList<String> rivalryDynamicsNotes;
    /** Set when dynamics already ran this offseason (avoids double decay). */
    public boolean rivalryDynamicsAppliedThisOffseason;
    public ArrayList<String> teamHistory;
    public ArrayList<String> hallOfFame;
    public boolean userControlled;
    public boolean showPopups;
    public int recruitMoney;
    public int numRecruits;

    public int wins;
    public int losses;
    public int totalWins;
    public int totalLosses;
    public int totalCCs;
    public int totalNCs;
    public int totalCCLosses;
    public int totalNCLosses;
    public int totalBowls;
    public int totalBowlLosses;
    public String evenYearHomeOpp;

    public TeamStreak winStreak;
    public TeamStreak yearStartWinStreak;

    //Game Log variables
    public ArrayList<Game> gameSchedule;
    public ArrayList<String> gameWLSchedule;
    public ArrayList<Team> gameWinsAgainst;
    /** Immovable regular-season bye week index, or -1 if not yet assigned. */
    public int byeWeek = -1;
    public String confChampion;
    public String semiFinalWL;
    public String natChampWL;

    //Team stats
    public int teamPoints;
    public int teamOppPoints;
    public int teamYards;
    public int teamOppYards;
    public int teamPassYards;
    public int teamRushYards;
    public int teamOppPassYards;
    public int teamOppRushYards;
    public int teamTODiff;
    public int teamOffTalent;
    public int teamDefTalent;
    public ProgramProfile programProfile;
    public int teamPollScore;
    public int teamStrengthOfWins;

    public int rankTeamPoints;
    public int rankTeamOppPoints;
    public int rankTeamYards;
    public int rankTeamOppYards;
    public int rankTeamPassYards;
    public int rankTeamRushYards;
    public int rankTeamOppPassYards;
    public int rankTeamOppRushYards;
    public int rankTeamTODiff;
    public int rankTeamOffTalent;
    public int rankTeamDefTalent;
    public int rankTeamProgramPower;
    public int rankTeamRecruitClass;
    public int rankTeamPollScore;
    public int rankTeamStrengthOfWins;

    //program/talent improvements
    public int diffOffTalent;
    public int diffDefTalent;

    //players on team
    //offense
    public ArrayList<Player> teamQBs;
    public ArrayList<Player> teamRBs;
    public ArrayList<Player> teamWRs;
    public ArrayList<Player> teamKs;
    public ArrayList<Player> teamPs;
    public ArrayList<Player> teamFBs;
    public ArrayList<Player> teamTEs;
    public ArrayList<Player> teamOLs;
    //defense
    public ArrayList<Player> teamEDGEs;
    public ArrayList<Player> teamDLs;
    public ArrayList<Player> teamLBs;
    public ArrayList<Player> teamSs;
    public ArrayList<Player> teamCBs;
    //By year
    public ArrayList<Player> teamFRs;
    public ArrayList<Player> teamSOs;
    public ArrayList<Player> teamJRs;
    public ArrayList<Player> teamSRs;
    public ArrayList<Player> teamGrads;

    public ArrayList<Player> playersLeaving;
    public ArrayList<Player> playersInjured;
    public ArrayList<Player> playersRecovered;
    public ArrayList<Player> playersInjuredAll;
    public ArrayList<Player> playersTransferring;

    /** Legacy save columns 13–14; always persisted as 1,1 (weekly game plan removed). */
    public int teamStratOffNum = 1;
    public int teamStratDefNum = 1;

    public OffensivePhilosophy offPhilosophy = OffensivePhilosophy.MULTIPLE;
    public DefensiveSystem defSystem = DefensiveSystem.BASE_4_3;
    public QbPressurePolicy qbPressurePolicy = QbPressurePolicy.defaults();

    /** Special-teams depth overlays (point at existing roster players). */
    public Player puntReturner;
    public Player kickReturner;
    public Player gunner1;
    public Player gunner2;
    public Player longSnapper;

    public boolean confTVDeal;
    public boolean teamTVDeal;
    public boolean hadCoachingChange;
    public boolean programProfileUpdatedThisOffseason;

    private static final int NFL_OVR = 90;
    private static final double NFL_CHANCE = 0.7;

    /**
     * Creates new team, recruiting needed players and setting team stats to 0.
     * @param name name of the team
     * @param abbr abbreviation of the team, 3 letters
     * @param conference conference the team is in
     * @param league reference to the league object all must obey
     */
    public Team(
            String name,
            String abbr,
            String conference,
            League league,
            int tradition,
            int fanbase,
            int donors,
            int footprint,
            int pipeline,
            int momentum,
            String rivalsEncoded) {
        this.league = league;
        this.name = name;
        this.abbr = abbr;
        this.conference = conference;
        userControlled = false;
        showPopups = true;
        teamHistory = new ArrayList<String>();
        hallOfFame = new ArrayList<>();
        playersInjuredAll = new ArrayList<>();
        rivalries = new ArrayList<>(Rivalry.parseEncoded(rivalsEncoded));
        rivalryResults = new HashMap<>();
        rivalryDynamicsNotes = new ArrayList<>();

        teamQBs = new ArrayList<Player>();
        teamRBs = new ArrayList<Player>();
        teamWRs = new ArrayList<Player>();
        teamKs = new ArrayList<Player>();
        teamPs = new ArrayList<Player>();
        teamFBs = new ArrayList<Player>();
        teamTEs = new ArrayList<Player>();
        teamOLs = new ArrayList<Player>();
        teamEDGEs = new ArrayList<Player>();
        teamDLs = new ArrayList<Player>();
        teamLBs = new ArrayList<Player>();
        teamSs = new ArrayList<Player>();
        teamCBs = new ArrayList<Player>();
        
        teamFRs = new ArrayList<Player>();
        teamSOs = new ArrayList<Player>();
        teamJRs = new ArrayList<Player>();
        teamSRs = new ArrayList<Player>();
        teamGrads = new ArrayList<Player>();

        gameSchedule = new ArrayList<Game>();
        gameWinsAgainst = new ArrayList<Team>();
        gameWLSchedule = new ArrayList<String>();
        byeWeek = -1;
        confChampion = "";
        semiFinalWL = "";
        natChampWL = "";

        programProfile = new ProgramProfile(
                tradition,
                fanbase,
                donors,
                footprint,
                pipeline,
                momentum,
                Conference.mediaShareFor(conference));
        playersTransferring = new ArrayList<>();
        hadCoachingChange = false;
        programProfileUpdatedThisOffseason = false;
        assignSystemsForProgram();
        recruitPlayers(NilMoney.INIT_QB, NilMoney.INIT_RB, NilMoney.INIT_FB, NilMoney.INIT_WR, NilMoney.INIT_TE,
                NilMoney.INIT_OL, NilMoney.INIT_K, NilMoney.INIT_P, NilMoney.INIT_S, NilMoney.INIT_CB,
                NilMoney.INIT_EDGE, NilMoney.INIT_DL, NilMoney.INIT_LB);
        assignInitialRosterStatuses();
        ensureSpecialTeamsDepth();
        recruitMoney = NilMoney.yearlyBudget(programProfile);

        //set stats
        totalWins = 0;
        totalLosses = 0;
        winStreak = new TeamStreak(league.getYear(), league.getYear(), 0, abbr);
        yearStartWinStreak = new TeamStreak(league.getYear(), league.getYear(), 0, abbr);
        totalCCs = 0;
        totalNCs = 0;
        totalCCLosses = 0;
        totalNCLosses = 0;
        totalBowls = 0;
        totalBowlLosses = 0;
        teamPoints = 0;
        teamOppPoints = 0;
        teamYards = 0;
        teamOppYards = 0;
        teamPassYards = 0;
        teamRushYards = 0;
        teamOppPassYards = 0;
        teamOppRushYards = 0;
        teamTODiff = 0;
        teamOffTalent = getOffTalent();
        teamDefTalent = getDefTalent();

        teamPollScore = programProfile.programPower + getOffTalent() + getDefTalent();

        teamStratOffNum = 1;
        teamStratDefNum = 1;
        numRecruits = 30;
        playersLeaving = new ArrayList<>();
    }

    /**
     * Constructor for team that is being loaded from file.
     * @param loadStr String containing the team info that can be loaded
     */
    public Team( String loadStr, League league ) {
        this.league = league;
        userControlled = false;
        showPopups = true;
        teamHistory = new ArrayList<String>();
        hallOfFame = new ArrayList<>();
        playersInjuredAll = new ArrayList<>();
        rivalries = new ArrayList<>();
        rivalryResults = new HashMap<>();
        rivalryDynamicsNotes = new ArrayList<>();

        teamQBs = new ArrayList<Player>();
        teamRBs = new ArrayList<Player>();
        teamWRs = new ArrayList<Player>();
        teamKs = new ArrayList<Player>();
        teamPs = new ArrayList<Player>();
        teamFBs = new ArrayList<Player>();
        teamTEs = new ArrayList<Player>();
        teamOLs = new ArrayList<Player>();
        teamEDGEs = new ArrayList<Player>();
        teamDLs = new ArrayList<Player>();
        teamLBs = new ArrayList<Player>();
        teamSs = new ArrayList<Player>();
        teamCBs = new ArrayList<Player>();
        
        teamFRs = new ArrayList<Player>();
        teamSOs = new ArrayList<Player>();
        teamJRs = new ArrayList<Player>();
        teamSRs = new ArrayList<Player>();
        teamGrads = new ArrayList<Player>();

        gameSchedule = new ArrayList<Game>();
        gameWinsAgainst = new ArrayList<Team>();
        gameWLSchedule = new ArrayList<String>();
        byeWeek = -1;
        confChampion = "";
        semiFinalWL = "";
        natChampWL = "";

        //set stats
        teamPoints = 0;
        teamOppPoints = 0;
        teamYards = 0;
        teamOppYards = 0;
        teamPassYards = 0;
        teamRushYards = 0;
        teamOppPassYards = 0;
        teamOppRushYards = 0;
        teamTODiff = 0;
        teamOffTalent = 0;
        teamDefTalent = 0;
        teamPollScore = 0;
        teamStratOffNum = 1; // 1 is the default strats
        teamStratDefNum = 1;
        teamTVDeal = false;
        confTVDeal = false;

        // Actually load the team from the string
        String[] lines = loadStr.split("%");

        // Lines 0 is team info
        String[] teamInfo = lines[0].split(",");
        if (teamInfo.length >= 31) {
            conference = teamInfo[0];
            name = teamInfo[1];
            abbr = teamInfo[2];
            programProfile = new ProgramProfile(
                    Integer.parseInt(teamInfo[3]),
                    Integer.parseInt(teamInfo[4]),
                    Integer.parseInt(teamInfo[5]),
                    Integer.parseInt(teamInfo[6]),
                    Integer.parseInt(teamInfo[7]),
                    Integer.parseInt(teamInfo[8]),
                    Conference.mediaShareFor(conference));
            totalWins = Integer.parseInt(teamInfo[9]);
            totalLosses = Integer.parseInt(teamInfo[10]);
            totalCCs = Integer.parseInt(teamInfo[11]);
            totalNCs = Integer.parseInt(teamInfo[12]);
            String rivalsField = teamInfo[13];
            if (rivalsField.contains(":") || rivalsField.contains(";")) {
                rivalries = new ArrayList<>(Rivalry.parseEncoded(rivalsField));
            } else {
                rivalries = new ArrayList<>(Rivalry.singlePrimary(rivalsField));
            }
            totalNCLosses = Integer.parseInt(teamInfo[14]);
            totalCCLosses = Integer.parseInt(teamInfo[15]);
            totalBowls = Integer.parseInt(teamInfo[16]);
            totalBowlLosses = Integer.parseInt(teamInfo[17]);
            showPopups = Integer.parseInt(teamInfo[18]) == 1;
            winStreak = new TeamStreak(
                    Integer.parseInt(teamInfo[21]),
                    Integer.parseInt(teamInfo[22]),
                    Integer.parseInt(teamInfo[19]),
                    teamInfo[20]);
            yearStartWinStreak = new TeamStreak(
                    Integer.parseInt(teamInfo[21]),
                    Integer.parseInt(teamInfo[22]),
                    Integer.parseInt(teamInfo[19]),
                    teamInfo[20]);
            teamTVDeal = Boolean.parseBoolean(teamInfo[23]);
            confTVDeal = Boolean.parseBoolean(teamInfo[24]);
            offPhilosophy = OffensivePhilosophy.fromOrdinalSafe(Integer.parseInt(teamInfo[25]));
            defSystem = DefensiveSystem.fromOrdinalSafe(Integer.parseInt(teamInfo[26]));
            programProfile.restoreHistory(teamInfo[27], teamInfo[28]);
            programProfile.refreshDerived(Conference.mediaShareFor(conference));
            programProfileUpdatedThisOffseason = Boolean.parseBoolean(teamInfo[29]);
            programProfile.restoreAnnualDeltas(teamInfo[30]);
            if (teamInfo.length >= 32) {
                qbPressurePolicy = QbPressurePolicy.parse(teamInfo[31]);
            } else {
                qbPressurePolicy = QbPressurePolicy.defaults();
            }
        } else {
            throw new IllegalArgumentException("Unsupported team save profile.");
        }

        // Lines 1 is Team Home/Away Rotation
        int startOfPlayers = 2;
        if (!lines[1].split(",")[0].equals("QB")) evenYearHomeOpp = lines[1];
        else {
            startOfPlayers = 1;
        }

        // Rest of lines are player info
        for (int i = startOfPlayers; i < lines.length; ++i) {
            recruitPlayerCSV(lines[i], false);
        }

        // Group players by class standing (FRs, SOs, etc)
        groupPlayerStandingCSV();
        
        
        if (rivalries == null) {
            rivalries = new ArrayList<>();
        }
        if (rivalryResults == null) {
            rivalryResults = new HashMap<>();
        }
        if (rivalryDynamicsNotes == null) {
            rivalryDynamicsNotes = new ArrayList<>();
        }
        rivalryResults.clear();
        teamStratOffNum = 1;
        teamStratDefNum = 1;
        numRecruits = 30;
        playersLeaving = new ArrayList<>();
        playersTransferring = new ArrayList<>();
        hadCoachingChange = false;
        if (recruitMoney <= 0) {
            recruitMoney = NilMoney.yearlyBudget(programProfile);
        }
        if (offPhilosophy == null) offPhilosophy = OffensivePhilosophy.MULTIPLE;
        if (defSystem == null) defSystem = DefensiveSystem.BASE_4_3;
        // Only randomize systems when not present in save (pre-v2 fields already rejected at League load)
        DepthChart.applySystems(this);
        ensureSpecialTeamsDepth();
    }

    /**
     * Gets the OffTalent, DefTalent, poll score
     */
    public void updateTalentRatings() {
        teamOffTalent = getOffTalent();
        teamDefTalent = getDefTalent();
        teamPollScore = programProfile.programPower + getOffTalent() + getDefTalent();
    }

    public String encodeRivalries() {
        return Rivalry.encode(rivalries);
    }

    /** Opponent abbr of the highest-strength rivalry, or null. */
    public String highestRivalAbbr() {
        if (rivalries == null || rivalries.isEmpty()) {
            return null;
        }
        Rivalry best = null;
        for (Rivalry r : rivalries) {
            if (best == null || r.strength > best.strength) {
                best = r;
            }
        }
        return best != null ? best.opponentAbbr : null;
    }

    /** @deprecated use {@link #highestRivalAbbr()} */
    @Deprecated
    public String primaryRivalAbbr() {
        return highestRivalAbbr();
    }

    public Rivalry rivalryWith(String opponentAbbr) {
        if (opponentAbbr == null || rivalries == null) {
            return null;
        }
        for (Rivalry r : rivalries) {
            if (r.opponentAbbr.equalsIgnoreCase(opponentAbbr)) {
                return r;
            }
        }
        return null;
    }

    public boolean isRival(String opponentAbbr) {
        return rivalryWith(opponentAbbr) != null;
    }

    /**
     * Strongest rivalry strength either side has toward the other (0 if none).
     */
    public static int strongestRivalryBetween(Team a, Team b) {
        if (a == null || b == null) {
            return 0;
        }
        int best = 0;
        Rivalry fromA = a.rivalryWith(b.abbr);
        Rivalry fromB = b.rivalryWith(a.abbr);
        if (fromA != null) {
            best = fromA.strength;
        }
        if (fromB != null && fromB.strength > best) {
            best = fromB.strength;
        }
        return best;
    }

    public Rivalry ensureRivalry(String opponentAbbr, int minStrength) {
        Rivalry existing = rivalryWith(opponentAbbr);
        if (existing != null) {
            if (existing.strength < minStrength) {
                existing.strength = Rivalry.clamp(minStrength);
            }
            return existing;
        }
        if (rivalries == null) {
            rivalries = new ArrayList<>();
        }
        if (rivalries.size() >= Rivalry.MAX_RIVALRIES) {
            Rivalry weakest = null;
            for (Rivalry r : rivalries) {
                if (r.strength < Rivalry.WARM_THRESHOLD
                        && (weakest == null || r.strength < weakest.strength)) {
                    weakest = r;
                }
            }
            if (weakest == null || minStrength < 30) {
                return null;
            }
            rivalries.remove(weakest);
        }
        Rivalry created = new Rivalry(opponentAbbr, minStrength);
        rivalries.add(created);
        return created;
    }

    public void recordRivalryResult(String opponentAbbr, boolean won) {
        if (opponentAbbr == null) {
            return;
        }
        if (rivalryResults == null) {
            rivalryResults = new HashMap<>();
        }
        rivalryResults.put(opponentAbbr, won);
    }

    public void clearRivalryResults() {
        if (rivalryResults == null) {
            rivalryResults = new HashMap<>();
        } else {
            rivalryResults.clear();
        }
    }

    /**
     * Momentum delta from rivalry games this season.
     */
    public int computeRivalryMomentumDelta() {
        int delta = 0;
        if (rivalries == null || rivalryResults == null || league == null) {
            return 0;
        }
        for (Rivalry rivalry : rivalries) {
            if (!rivalryResults.containsKey(rivalry.opponentAbbr)) {
                continue;
            }
            boolean won = Boolean.TRUE.equals(rivalryResults.get(rivalry.opponentAbbr));
            Team rival = league.findTeamAbbr(rivalry.opponentAbbr);
            if (rival == null) {
                continue;
            }
            delta += rivalryMomentumSwing(
                    rivalry.strength,
                    won,
                    programProfile.programPower,
                    rival.programProfile.programPower);
        }
        return delta;
    }

    static int rivalryMomentumSwing(
            int strength,
            boolean won,
            int ownPower,
            int rivalPower) {
        if (strength < Rivalry.MOMENTUM_THRESHOLD) {
            return 0;
        }
        int competitiveGap = 15 + strength / 10;
        int momentumSwing = strength >= Rivalry.HOT_THRESHOLD ? 2 : 1;
        int gap = ownPower - rivalPower;
        if (won) {
            if (gap >= competitiveGap) {
                return 0;
            }
            int swing = momentumSwing;
            if (rivalPower - ownPower >= 10) {
                swing += 1;
            }
            return swing;
        }
        if (rivalPower - ownPower >= competitiveGap) {
            return 0;
        }
        return -momentumSwing;
    }

    public String rivalryMomentumSummaryLines() {
        StringBuilder sb = new StringBuilder();
        if (rivalries == null || rivalryResults == null || league == null) {
            return "";
        }
        for (Rivalry rivalry : rivalries) {
            if (!rivalryResults.containsKey(rivalry.opponentAbbr)) {
                continue;
            }
            boolean won = Boolean.TRUE.equals(rivalryResults.get(rivalry.opponentAbbr));
            Team rival = league.findTeamAbbr(rivalry.opponentAbbr);
            if (rival == null) {
                continue;
            }
            int swing = rivalryMomentumSwing(
                    rivalry.strength,
                    won,
                    programProfile.programPower,
                    rival.programProfile.programPower);
            String label = rivalry.displayLabel();
            if (won) {
                if (swing > 0) {
                    sb.append("\n\nWon your rivalry vs ").append(label)
                            .append(". Recruits noticed (+")
                            .append(swing).append(" momentum).");
                } else {
                    sb.append("\n\nWon your rivalry vs ").append(label)
                            .append(", but it was expected given the program gap (no change).");
                }
            } else {
                if (swing < 0) {
                    sb.append("\n\nLost your rivalry vs ").append(label)
                            .append(". Recruits soured (")
                            .append(swing).append(" momentum).");
                } else {
                    sb.append("\n\nLost your rivalry vs ").append(label)
                            .append(", but it was expected in a rebuild (no change).");
                }
            }
        }
        return sb.toString();
    }

    public void retargetRivalAbbr(String oldAbbr, String newAbbr) {
        if (oldAbbr == null || newAbbr == null || rivalries == null) {
            return;
        }
        for (int i = 0; i < rivalries.size(); i++) {
            Rivalry r = rivalries.get(i);
            if (r.opponentAbbr.equalsIgnoreCase(oldAbbr)) {
                rivalries.set(i, new Rivalry(newAbbr, r.strength));
            }
        }
    }

    public int getRosterCount() {
        return teamQBs.size() + teamRBs.size() + teamFBs.size() + teamWRs.size() + teamTEs.size()
                + teamOLs.size() + teamKs.size() + teamPs.size() + teamSs.size() + teamCBs.size()
                + teamEDGEs.size() + teamDLs.size() + teamLBs.size();
    }

    public int getScholarshipCount() {
        int n = 0;
        for (Player p : getAllPlayers()) {
            if (p.rosterStatus != null && p.rosterStatus.usesScholarship()) n++;
        }
        return n;
    }

    public ArrayList<Player> getAllPlayers() {
        ArrayList<Player> all = new ArrayList<>();
        all.addAll(teamQBs);
        all.addAll(teamRBs);
        all.addAll(teamFBs);
        all.addAll(teamWRs);
        all.addAll(teamTEs);
        all.addAll(teamOLs);
        all.addAll(teamKs);
        all.addAll(teamPs);
        all.addAll(teamSs);
        all.addAll(teamCBs);
        all.addAll(teamEDGEs);
        all.addAll(teamDLs);
        all.addAll(teamLBs);
        return all;
    }

    /** Player row for flat roster UI: ordered starters then bench. */
    public static class RosterDisplayPlayer {
        public final Player player;
        public final boolean starter;

        public RosterDisplayPlayer(Player player, boolean starter) {
            this.player = player;
            this.starter = starter;
        }
    }

    /**
     * Starters first (depth-chart order), then bench, for mobile roster cards.
     */
    public ArrayList<RosterDisplayPlayer> getRosterDisplayPlayers() {
        ArrayList<RosterDisplayPlayer> rows = new ArrayList<>();
        appendRosterRows(rows, teamQBs, 1);
        appendRosterRows(rows, teamRBs, 2);
        appendRosterRows(rows, teamFBs, 1);
        appendRosterRows(rows, teamWRs, 3);
        appendRosterRows(rows, teamTEs, 1);
        appendRosterRows(rows, teamOLs, 5);
        appendRosterRows(rows, teamKs, 1);
        appendRosterRows(rows, teamPs, 1);
        appendRosterRows(rows, teamSs, 1);
        appendRosterRows(rows, teamCBs, 3);
        appendRosterRows(rows, teamEDGEs, 2);
        appendRosterRows(rows, teamDLs, 3);
        appendRosterRows(rows, teamLBs, 3);
        return rows;
    }

    private void appendRosterRows(ArrayList<RosterDisplayPlayer> rows,
                                  ArrayList<? extends Player> players, int starterCount) {
        for (int i = 0; i < players.size(); i++) {
            rows.add(new RosterDisplayPlayer(players.get(i), i < starterCount));
        }
    }

    public ArrayList<? extends Player> playersForPosition(String pos) {
        if (pos == null) return null;
        switch (pos) {
            case "QB": return teamQBs;
            case "RB": return teamRBs;
            case "FB": return teamFBs;
            case "WR": return teamWRs;
            case "TE": return teamTEs;
            case "OL": return teamOLs;
            case "K": return teamKs;
            case "P": return teamPs;
            case "S": return teamSs;
            case "CB": return teamCBs;
            case "EDGE": return teamEDGEs;
            case "DL": return teamDLs;
            case "LB": return teamLBs;
            default: return null;
        }
    }

    public ArrayList<? extends Player> getPositionList(String pos) {
        return playersForPosition(pos);
    }

    public List<? extends Player> playersForGroup(PositionGroup g) {
        if (g == null) return null;
        switch (g) {
            case QB: return teamQBs;
            case RB: return teamRBs;
            case FB: return teamFBs;
            case WR: return teamWRs;
            case TE: return teamTEs;
            case OL: return teamOLs;
            case K: return teamKs;
            case P: return teamPs;
            case S: return teamSs;
            case CB: return teamCBs;
            case EDGE: return teamEDGEs;
            case DL: return teamDLs;
            case LB: return teamLBs;
            default: return null;
        }
    }

    private void assignSystemsForProgram() {
        String seedKey = name != null ? name : (abbr != null ? abbr : "TEAM");
        int gravity = programProfile != null ? programProfile.talentGravity : 50;
        java.util.Random rng = new java.util.Random(seedKey.hashCode() ^ gravity * 31L);
        offPhilosophy = OffensivePhilosophy.assignForProgramStrength(gravity, rng);
        defSystem = DefensiveSystem.assignForProgramStrength(gravity, rng);
    }

    public void setOffPhilosophy(OffensivePhilosophy phil) {
        offPhilosophy = phil != null ? phil : OffensivePhilosophy.MULTIPLE;
        DepthChart.applySystems(this);
    }

    public void setDefSystem(DefensiveSystem sys) {
        defSystem = sys != null ? sys : DefensiveSystem.BASE_4_3;
        DepthChart.applySystems(this);
    }

    public void setQbPressurePolicy(QbPressurePolicy policy) {
        qbPressurePolicy = policy != null ? policy : QbPressurePolicy.defaults();
    }

    public void setPressureResponse(QbPressurePolicy.Slot slot, PressureResponse response) {
        QbPressurePolicy base = qbPressurePolicy != null ? qbPressurePolicy : QbPressurePolicy.defaults();
        qbPressurePolicy = base.copyWith(slot, response);
    }

    public void grantYearlyBudget() {
        recruitMoney = NilMoney.yearlyBudget(programProfile);
        // Pay remaining multi-year NIL commitments.
        for (Player p : getAllPlayers()) {
            if (p.contractYearsRemaining <= 0) continue;
            if (playersLeaving != null && playersLeaving.contains(p)) continue;
            recruitMoney -= p.futureNilCommitment();
            p.contractYearsRemaining--;
            p.retainedThisOffseason = false;
        }
        if (recruitMoney < 0) recruitMoney = 0;
    }

    public int projectedBudget(int yearOffset) {
        int base = NilMoney.yearlyBudget(programProfile);
        if (yearOffset <= 0) return base;
        return base;
    }

    /** Remaining guaranteed NIL for future season offset (1 = next year). */
    public int committedForOffset(int yearOffset) {
        if (yearOffset < 1) return 0;
        int sum = 0;
        for (Player p : getAllPlayers()) {
            if (playersLeaving != null && playersLeaving.contains(p)) continue;
            if (p.contractYearsRemaining >= yearOffset) {
                sum += p.futureNilCommitment();
            }
        }
        return sum;
    }

    public int availableForOffset(int yearOffset) {
        return projectedBudget(yearOffset) - committedForOffset(yearOffset);
    }

    /**
     * Year-1 NIL cost must fit the current purse; future years encumber NIL only.
     * Scholarships have no purse cost.
     */
    public boolean canAffordContract(int yearOneCost, int futureAnnualNil, int years) {
        if (yearOneCost > recruitMoney) return false;
        int y = Math.max(1, years);
        int future = Math.max(0, futureAnnualNil);
        for (int offset = 1; offset < y; offset++) {
            if (availableForOffset(offset) < future) return false;
        }
        return true;
    }

    public boolean canAffordContract(RosterStatus status, int nilAmount, int years) {
        int yearOne = offerTotalCost(status, nilAmount);
        int futureNil = status == RosterStatus.SCHOLARSHIP_PLUS_NIL ? Math.max(0, nilAmount) : 0;
        return canAffordContract(yearOne, futureNil, years);
    }

    public boolean canAffordContract(int annualCost, int years) {
        return canAffordContract(annualCost, annualCost, years);
    }

    public boolean canAddToRoster() {
        return getRosterCount() < NilMoney.ROSTER_CAP;
    }

    public boolean canAwardScholarship() {
        return getScholarshipCount() < NilMoney.SCHOLARSHIP_CAP;
    }

    public int offerTotalCost(RosterStatus status, int nilAmount) {
        return NilMoney.offerCashCost(status, nilAmount, programProfile);
    }

    public boolean spendOffer(RosterStatus status, int nilAmount) {
        return spendOffer(status, nilAmount, 1);
    }

    public boolean spendOffer(RosterStatus status, int nilAmount, int years) {
        int cost = offerTotalCost(status, nilAmount);
        if (!canAffordContract(status, nilAmount, years)) return false;
        if (status.usesScholarship() && !canAwardScholarship()) return false;
        if (!canAddToRoster()) return false;
        recruitMoney -= cost;
        return true;
    }

    /** Spend for retaining a player already on roster (no roster-slot check). */
    public boolean spendRetentionOffer(RosterStatus status, int nilAmount) {
        return spendRetentionOffer(status, nilAmount, 1, null);
    }

    public boolean spendRetentionOffer(RosterStatus status, int nilAmount, int years, Player existing) {
        int cost = offerTotalCost(status, nilAmount);
        if (!canAffordContract(status, nilAmount, years)) return false;
        boolean alreadyScholly = existing != null && existing.rosterStatus != null && existing.rosterStatus.usesScholarship();
        if (status.usesScholarship() && !alreadyScholly && !canAwardScholarship()) return false;
        recruitMoney -= cost;
        return true;
    }

    public int buyoutCost(Player p) {
        return NilMoney.buyoutCost(p, programProfile);
    }

    /**
     * Cut or buy out a player. Returns false if cannot afford buyout.
     * Clears contract and removes from roster.
     */
    public boolean cutOrBuyout(Player p, boolean forceCut) {
        if (p == null) return false;
        int cost = buyoutCost(p);
        if (cost > recruitMoney) return false;
        recruitMoney -= cost;
        p.clearContract();
        removePlayerFromRoster(p);
        if (playersLeaving != null) playersLeaving.remove(p);
        if (playersTransferring != null) playersTransferring.remove(p);
        return true;
    }

    /** Clear contract commitments when player declares for draft (no buyout). */
    public void clearCommitmentsForDraft(Player p) {
        if (p == null) return;
        p.nilDealAmount = 0;
        p.contractYearsRemaining = 0;
        p.contractLength = 0;
        p.draftDeclared = true;
        p.retainedThisOffseason = false;
    }

    public boolean payDraftStay(Player p, int bonus) {
        if (p == null || !ProgramOffers.canPayToStay(p)) return false;
        if (bonus > recruitMoney) return false;
        recruitMoney -= bonus;
        playersLeaving.remove(p);
        p.draftDeclared = false;
        p.retainedThisOffseason = true;
        p.projectedDraftRound = 0;
        return true;
    }

    public void assignInitialRosterStatuses() {
        ArrayList<Player> all = getAllPlayers();
        Collections.sort(all, new Comparator<Player>() {
            @Override
            public int compare(Player a, Player b) {
                return b.ratOvr - a.ratOvr;
            }
        });
        int scholly = 0;
        for (Player p : all) {
            if (scholly < NilMoney.SCHOLARSHIP_CAP) {
                // 1-year opening deals (prepaid with roster construction) so the first
                // offseason grant does not drain the purse before retention.
                if (p.ratOvr >= 82) {
                    int nil = ProgramOffers.annualNilFor(p, this, 1);
                    p.applyOffer(RosterStatus.SCHOLARSHIP_PLUS_NIL, nil, 1);
                } else {
                    p.applyOffer(RosterStatus.SCHOLARSHIP, 0, 1);
                }
                scholly++;
            } else {
                p.applyOffer(RosterStatus.PWO, 0, 1);
            }
            p.contractYearsRemaining = 0;
            p.retainedThisOffseason = false;
        }
    }

    public String budgetHeader() {
        return "Budget: " + NilMoney.format(recruitMoney)
                + " · Y+1 free: " + NilMoney.format(Math.max(0, availableForOffset(1)))
                + " · Scholly: " + getScholarshipCount() + "/" + NilMoney.SCHOLARSHIP_CAP
                + " · Roster: " + getRosterCount() + "/" + NilMoney.ROSTER_CAP;
    }

    public String budgetCashLabel() {
        return "Available " + NilMoney.format(recruitMoney);
    }

    public String budgetPurseLabel() {
        return "Purse " + NilMoney.format(NilMoney.yearlyBudget(programProfile));
    }

    public String budgetY1FreeLabel() {
        return NilMoney.format(Math.max(0, availableForOffset(1)));
    }

    public String budgetSchollyLabel() {
        return getScholarshipCount() + "/" + NilMoney.SCHOLARSHIP_CAP;
    }

    public String budgetRosterLabel() {
        return getRosterCount() + "/" + NilMoney.ROSTER_CAP;
    }

    /** Structured ledger lines for Money tab (no prose dump). */
    public ArrayList<String> budgetLedgerRows() {
        ArrayList<String> rows = new ArrayList<>();
        rows.add("This year\nAvailable " + NilMoney.format(recruitMoney)
                + "\nTotal purse " + NilMoney.format(projectedBudget(0)));
        for (int y = 1; y <= 3; y++) {
            rows.add("Y+" + y
                    + "\nFree " + NilMoney.format(Math.max(0, availableForOffset(y)))
                    + "\nPurse " + NilMoney.format(projectedBudget(y))
                    + " · committed " + NilMoney.format(committedForOffset(y)));
        }
        return rows;
    }

    public String futureBudgetLedger() {
        StringBuilder sb = new StringBuilder();
        sb.append("This year available: ").append(NilMoney.format(recruitMoney)).append("\n");
        for (int y = 1; y <= 3; y++) {
            sb.append("Y+").append(y)
                    .append(": purse ").append(NilMoney.format(projectedBudget(y)))
                    .append(" · committed ").append(NilMoney.format(committedForOffset(y)))
                    .append(" · free ").append(NilMoney.format(Math.max(0, availableForOffset(y))))
                    .append("\n");
        }
        return sb.toString();
    }

    public void removePlayerFromRoster(Player p) {
        if (p == null) return;
        teamQBs.remove(p);
        teamRBs.remove(p);
        teamWRs.remove(p);
        teamOLs.remove(p);
        teamKs.remove(p);
        teamPs.remove(p);
        teamSs.remove(p);
        teamCBs.remove(p);
        teamFBs.remove(p);
        teamTEs.remove(p);
        teamEDGEs.remove(p);
        teamDLs.remove(p);
        teamLBs.remove(p);
    }

    public void addPlayerToRoster(Player p) {
        if (p == null) return;
        p.team = this;
        PositionGroup g = PositionGroup.fromToken(p.position);
        if (g == PositionGroup.QB) teamQBs.add(p);
        else if (g == PositionGroup.RB) teamRBs.add(p);
        else if (g == PositionGroup.WR) teamWRs.add(p);
        else if (g == PositionGroup.OL) teamOLs.add(p);
        else if (g == PositionGroup.K) teamKs.add(p);
        else if (g == PositionGroup.P) teamPs.add(p);
        else if (g == PositionGroup.S) teamSs.add(p);
        else if (g == PositionGroup.CB) teamCBs.add(p);
        else if (g == PositionGroup.FB) teamFBs.add(p);
        else if (g == PositionGroup.TE) teamTEs.add(p);
        else if (g == PositionGroup.EDGE) teamEDGEs.add(p);
        else if (g == PositionGroup.DL) teamDLs.add(p);
        else if (g == PositionGroup.LB) teamLBs.add(p);
        sortPlayers();
    }

    public int depthRank(Player p) {
        ArrayList<? extends Player> list = getPositionList(p.position);
        if (list == null) return 99;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == p) return i + 1;
        }
        return 99;
    }

    /** Advance season and update the program's market identity. */
    public void advanceSeason() {
        updateProgramProfileForOffseason();
        if (!rivalryDynamicsAppliedThisOffseason) {
            RivalryDynamics.evolveTeam(this);
        }
        rivalryDynamicsAppliedThisOffseason = false;
        clearRivalryResults();

        if (userControlled) checkHallofFame();

        checkCareerRecords(league.leagueRecords);
        if (league.userTeam == this) checkCareerRecords(league.userTeamRecords);

        advanceSeasonPlayers();
        applyProgramDevelopment();
        programProfileUpdatedThisOffseason = false;

    }

    public void updateProgramProfileForOffseason() {
        if (programProfileUpdatedThisOffseason) return;
        programProfile.updateForSeason(
                rankTeamPollScore,
                league.teamList.size(),
                rankTeamPollScore == 1,
                computeRivalryMomentumDelta(),
                draftClassScore(),
                Conference.mediaShareFor(conference));
        programProfileUpdatedThisOffseason = true;
    }

    private int draftClassScore() {
        int score = 0;
        if (playersLeaving == null) return score;
        for (Player player : playersLeaving) {
            int round = player.projectedDraftRound > 0
                    ? player.projectedDraftRound
                    : ProgramOffers.projectDraftRound(player);
            if (round == 1) score += 10;
            else if (round == 2) score += 8;
            else if (round == 3) score += 6;
            else if (round >= 4 && round <= 5) score += 4;
            else if (round >= 6 && round <= 7) score += 2;
        }
        return score;
    }

    private void applyProgramDevelopment() {
        int bonus = programProfile.developmentBonus();
        if (bonus == 0) return;
        for (Player player : getAllPlayers()) {
            int pot = Math.max(player.ratOvr, Math.min(99, player.ratPot + bonus));
            player.ratPot = pot;
            if (player.ratings != null) player.ratings.pot = pot;
        }
    }

    /**
     * Checks all the players leaving to see if they should be inducted to the hall of fame.
     */
    public void checkHallofFame() {
        // hofScore = gamesPlayed + 5*allConf + 15*allAmer + 50*POTY
        // Need 50 to get in
        for (Player p : playersLeaving) {
            int gms = p.gamesPlayed + p.careerGamesPlayed;
            int allConf = p.careerAllConference + (p.wonAllConference ? 1 : 0);
            int allAmer = p.careerAllAmerican + (p.wonAllAmerican ? 1 : 0);
            int poty = p.careerHeismans + (p.wonHeisman ? 1 : 0);
            if (gms/2 + 5*allConf + 15*allAmer + 50*poty > 50) {
                // HOFer
                ArrayList<String> careerStats = p.getCareerStatsList();
                StringBuilder sb = new StringBuilder();
                sb.append(p.getPosNameYrOvr_Str() + "&");
                for (String s : careerStats) {
                    sb.append(s + "&");
                }
                hallOfFame.add(sb.toString());
            }
        }
    }

    /**
     * Checks if any of the league records were broken by this team.
     */
    public void checkLeagueRecords(LeagueRecords records) {
        records.checkRecord("Team PPG", teamPoints/numGames(), abbr, league.getYear());
        records.checkRecord("Team Opp PPG", teamOppPoints/numGames(), abbr, league.getYear());
        records.checkRecord("Team YPG", teamYards/numGames(), abbr, league.getYear());
        records.checkRecord("Team Opp YPG", teamOppYards/numGames(), abbr, league.getYear());
        records.checkRecord("Team PPG", teamPoints/numGames(), abbr, league.getYear());
        records.checkRecord("Team TO Diff", teamTODiff, abbr, league.getYear());

        for (int i = 0; i < teamQBs.size(); ++i) {
            if (getQB(i).gamesPlayed > 6) {
                records.checkRecord("Pass Yards", getQB(i).seasonStats.passYards, abbr + " " + getQB(i).getInitialName(), league.getYear());
                records.checkRecord("Pass TDs", getQB(i).seasonStats.passTd, abbr + " " + getQB(i).getInitialName(), league.getYear());
                records.checkRecord("Interceptions", getQB(i).seasonStats.passInt, abbr + " " + getQB(i).getInitialName(), league.getYear());
                records.checkRecord("Comp Percent", (100 * getQB(i).seasonStats.passComp) / (getQB(i).seasonStats.passAtt + 1), abbr + " " + getQB(i).getInitialName(), league.getYear());
            }
        }


        for (int i = 0; i < teamRBs.size(); ++i) {
            if (getRB(i).gamesPlayed > 6) {
                records.checkRecord("Rush Yards", getRB(i).seasonStats.rushYards, abbr + " " + getRB(i).getInitialName(), league.getYear());
                records.checkRecord("Rush TDs", getRB(i).seasonStats.rushTd, abbr + " " + getRB(i).getInitialName(), league.getYear());
                records.checkRecord("Rush Fumbles", getRB(i).seasonStats.fumbles, abbr + " " + getRB(i).getInitialName(), league.getYear());
            }
        }

        for (int i = 0; i < teamWRs.size(); ++i) {
            if (getWR(i).gamesPlayed > 6) {
                records.checkRecord("Rec Yards", getWR(i).seasonStats.recYards, abbr + " " + getWR(i).getInitialName(), league.getYear());
                records.checkRecord("Rec TDs", getWR(i).seasonStats.recTd, abbr + " " + getWR(i).getInitialName(), league.getYear());
                records.checkRecord("Catch Percent", (100 * getWR(i).seasonStats.receptions) / (getWR(i).seasonStats.targets + 1), abbr + " " + getWR(i).getInitialName(), league.getYear());
            }
        }

    }

    /**
     * Checks the career records for all the leaving players. Must be done after playersLeaving is populated.
     */
    public void checkCareerRecords(LeagueRecords records) {
        for (Player p : playersLeaving) {
            PositionGroup g = PositionGroup.fromToken(p.position);
            if (g == PositionGroup.QB) {
                records.checkRecord("Career Pass Yards", p.seasonStats.passYards+p.careerStats.passYards, abbr + " " + p.getInitialName(), league.getYear()-1);
                records.checkRecord("Career Pass TDs", p.seasonStats.passTd+p.careerStats.passTd, abbr + " " + p.getInitialName(), league.getYear()-1);
                records.checkRecord("Career Interceptions", p.seasonStats.passInt+p.careerStats.passInt, abbr + " " + p.getInitialName(), league.getYear()-1);
            }
            else if (g == PositionGroup.RB) {
                records.checkRecord("Career Rush Yards", p.seasonStats.rushYards+p.careerStats.rushYards, abbr + " " + p.getInitialName(), league.getYear()-1);
                records.checkRecord("Career Rush TDs", p.seasonStats.rushTd+p.careerStats.rushTd, abbr + " " + p.getInitialName(), league.getYear()-1);
                records.checkRecord("Career Rush Fumbles", p.seasonStats.fumbles+p.careerStats.fumbles, abbr + " " + p.getInitialName(), league.getYear()-1);
            }
            else if (g == PositionGroup.WR) {
                records.checkRecord("Career Rec Yards", p.seasonStats.recYards+p.careerStats.recYards, abbr + " " + p.getInitialName(), league.getYear()-1);
                records.checkRecord("Career Rec TDs", p.seasonStats.recTd+p.careerStats.recTd, abbr + " " + p.getInitialName(), league.getYear()-1);
            }
        }
    }

    public void getPlayersLeaving() {
        if (playersLeaving.isEmpty()) {
            double leaveBonus = 0;
            if (natChampWL != null && natChampWL.equals("NCW")) {
                leaveBonus += 0.2;
            }
            for (Player p : getAllPlayers()) {
                considerDraftOrGraduation(p, leaveBonus);
            }
        }
    }

    private void considerDraftOrGraduation(Player p, double leaveBonus) {
        if (p == null) return;
        p.projectedDraftRound = 0;
        p.draftDeclared = false;
        if (p.year >= 5) {
            playersLeaving.add(p);
            clearCommitmentsForDraft(p);
            return;
        }
        if (p.year < 3) return;

        int round = ProgramOffers.projectDraftRound(p);
        p.projectedDraftRound = round;

        if (ProgramOffers.isLockedDraftRound(round)) {
            playersLeaving.add(p);
            clearCommitmentsForDraft(p);
            return;
        }

        // R4–7 always declare (payable stay); fringe elite UDFA may declare on chance
        if (round >= 4 && round <= 7) {
            playersLeaving.add(p);
            // Keep contract until stay/declare resolved in retention
            return;
        }

        // Legacy high-ovr early declare chance for near-UDFA
        if (p.ratOvr > NFL_OVR && Math.random() < NFL_CHANCE + leaveBonus) {
            p.projectedDraftRound = 0;
            playersLeaving.add(p);
        }
    }

    /**
     * Advance season for players. Removes seniors and develops underclassmen.
     */
    public void advanceSeasonPlayers() {
        int qbNeeds=0, rbNeeds=0, fbNeeds=0, wrNeeds=0, teNeeds=0, kNeeds=0, olNeeds=0, sNeeds=0, cbNeeds=0, edgeNeeds=0, dlNeeds=0, lbNeeds=0;
        if (playersLeaving.isEmpty()) {
            int i = 0;
            while (i < teamQBs.size()) {
                if (teamQBs.get(i).year == 5 || ((teamQBs.get(i).year == 3 || teamQBs.get(i).year == 4) && teamQBs.get(i).ratOvr > NFL_OVR && Math.random() < NFL_CHANCE)) {
                    teamQBs.remove(i);
                    qbNeeds++;
                } else {
                    teamQBs.get(i).advanceSeason();
                    i++;
                }
            }

            i = 0;
            while (i < teamRBs.size()) {
                if (teamRBs.get(i).year == 5 || ((teamRBs.get(i).year == 3 || teamRBs.get(i).year == 4) && teamRBs.get(i).ratOvr > NFL_OVR && Math.random() < NFL_CHANCE)) {
                    teamRBs.remove(i);
                    rbNeeds++;
                } else {
                    teamRBs.get(i).advanceSeason();
                    i++;
                }
            }

            i = 0;
            while (i < teamWRs.size()) {
                if (teamWRs.get(i).year == 5 || ((teamWRs.get(i).year == 3 || teamWRs.get(i).year == 4) && teamWRs.get(i).ratOvr > NFL_OVR && Math.random() < NFL_CHANCE)) {
                    teamWRs.remove(i);
                    wrNeeds++;
                } else {
                    teamWRs.get(i).advanceSeason();
                    i++;
                }
            }

            i = 0;
            while (i < teamKs.size()) {
                if (teamKs.get(i).year == 5) {
                    teamKs.remove(i);
                    kNeeds++;
                } else {
                    teamKs.get(i).advanceSeason();
                    i++;
                }
            }

            i = 0;
            while (i < teamPs.size()) {
                if (teamPs.get(i).year == 5) {
                    teamPs.remove(i);
                } else {
                    teamPs.get(i).advanceSeason();
                    i++;
                }
            }

            i = 0;
            while (i < teamOLs.size()) {
                if (teamOLs.get(i).year == 5 || ((teamOLs.get(i).year == 3 || teamOLs.get(i).year == 4) && teamOLs.get(i).ratOvr > NFL_OVR && Math.random() < NFL_CHANCE)) {
                    teamOLs.remove(i);
                    olNeeds++;
                } else {
                    teamOLs.get(i).advanceSeason();
                    i++;
                }
            }

            i = 0;
            while (i < teamSs.size()) {
                if (teamSs.get(i).year == 5 || ((teamSs.get(i).year == 3 || teamSs.get(i).year == 4) && teamSs.get(i).ratOvr > NFL_OVR && Math.random() < NFL_CHANCE)) {
                    teamSs.remove(i);
                    sNeeds++;
                } else {
                    teamSs.get(i).advanceSeason();
                    i++;
                }
            }

            i = 0;
            while (i < teamCBs.size()) {
                if (teamCBs.get(i).year == 5 || ((teamCBs.get(i).year == 3 || teamCBs.get(i).year == 4) && teamCBs.get(i).ratOvr > NFL_OVR && Math.random() < NFL_CHANCE)) {
                    teamCBs.remove(i);
                    cbNeeds++;
                } else {
                    teamCBs.get(i).advanceSeason();
                    i++;
                }
            }

            i = 0;
            while (i < teamFBs.size()) {
                if (teamFBs.get(i).year == 5 || ((teamFBs.get(i).year == 3 || teamFBs.get(i).year == 4) && teamFBs.get(i).ratOvr > NFL_OVR && Math.random() < NFL_CHANCE)) {
                    teamFBs.remove(i);
                    fbNeeds++;
                } else {
                    teamFBs.get(i).advanceSeason();
                    i++;
                }
            }

            i = 0;
            while (i < teamTEs.size()) {
                if (teamTEs.get(i).year == 5 || ((teamTEs.get(i).year == 3 || teamTEs.get(i).year == 4) && teamTEs.get(i).ratOvr > NFL_OVR && Math.random() < NFL_CHANCE)) {
                    teamTEs.remove(i);
                    teNeeds++;
                } else {
                    teamTEs.get(i).advanceSeason();
                    i++;
                }
            }

            i = 0;
            while (i < teamEDGEs.size()) {
                if (teamEDGEs.get(i).year == 5 || ((teamEDGEs.get(i).year == 3 || teamEDGEs.get(i).year == 4) && teamEDGEs.get(i).ratOvr > NFL_OVR && Math.random() < NFL_CHANCE)) {
                    teamEDGEs.remove(i);
                    edgeNeeds++;
                } else {
                    teamEDGEs.get(i).advanceSeason();
                    i++;
                }
            }

            i = 0;
            while (i < teamDLs.size()) {
                if (teamDLs.get(i).year == 5 || ((teamDLs.get(i).year == 3 || teamDLs.get(i).year == 4) && teamDLs.get(i).ratOvr > NFL_OVR && Math.random() < NFL_CHANCE)) {
                    teamDLs.remove(i);
                    dlNeeds++;
                } else {
                    teamDLs.get(i).advanceSeason();
                    i++;
                }
            }

            i = 0;
            while (i < teamLBs.size()) {
                if (teamLBs.get(i).year == 5 || ((teamLBs.get(i).year == 3 || teamLBs.get(i).year == 4) && teamLBs.get(i).ratOvr > NFL_OVR && Math.random() < NFL_CHANCE)) {
                    teamLBs.remove(i);
                    lbNeeds++;
                } else {
                    teamLBs.get(i).advanceSeason();
                    i++;
                }
            }

        } else {
            // Just remove the players that are in playersLeaving
            int i = 0;
            while (i < teamQBs.size()) {
                if (playersLeaving.contains(teamQBs.get(i))) {
                    teamQBs.remove(i);
                    qbNeeds++;
                } else {
                    teamQBs.get(i).advanceSeason();
                    i++;
                }
            }

            i = 0;
            while (i < teamRBs.size()) {
                if (playersLeaving.contains(teamRBs.get(i))) {
                    teamRBs.remove(i);
                    rbNeeds++;
                } else {
                    teamRBs.get(i).advanceSeason();
                    i++;
                }
            }

            i = 0;
            while (i < teamWRs.size()) {
                if (playersLeaving.contains(teamWRs.get(i))) {
                    teamWRs.remove(i);
                    wrNeeds++;
                } else {
                    teamWRs.get(i).advanceSeason();
                    i++;
                }
            }

            i = 0;
            while (i < teamKs.size()) {
                if (playersLeaving.contains(teamKs.get(i))) {
                    teamKs.remove(i);
                    kNeeds++;
                } else {
                    teamKs.get(i).advanceSeason();
                    i++;
                }
            }

            i = 0;
            while (i < teamOLs.size()) {
                if (playersLeaving.contains(teamOLs.get(i))) {
                    teamOLs.remove(i);
                    olNeeds++;
                } else {
                    teamOLs.get(i).advanceSeason();
                    i++;
                }
            }

            i = 0;
            while (i < teamSs.size()) {
                if (playersLeaving.contains(teamSs.get(i))) {
                    teamSs.remove(i);
                    sNeeds++;
                } else {
                    teamSs.get(i).advanceSeason();
                    i++;
                }
            }

            i = 0;
            while (i < teamCBs.size()) {
                if (playersLeaving.contains(teamCBs.get(i))) {
                    teamCBs.remove(i);
                    cbNeeds++;
                } else {
                    teamCBs.get(i).advanceSeason();
                    i++;
                }
            }

            i = 0;
            while (i < teamFBs.size()) {
                if (playersLeaving.contains(teamFBs.get(i))) {
                    teamFBs.remove(i);
                    fbNeeds++;
                } else {
                    teamFBs.get(i).advanceSeason();
                    i++;
                }
            }

            i = 0;
            while (i < teamTEs.size()) {
                if (playersLeaving.contains(teamTEs.get(i))) {
                    teamTEs.remove(i);
                    teNeeds++;
                } else {
                    teamTEs.get(i).advanceSeason();
                    i++;
                }
            }

            i = 0;
            while (i < teamEDGEs.size()) {
                if (playersLeaving.contains(teamEDGEs.get(i))) {
                    teamEDGEs.remove(i);
                    edgeNeeds++;
                } else {
                    teamEDGEs.get(i).advanceSeason();
                    i++;
                }
            }

            i = 0;
            while (i < teamDLs.size()) {
                if (playersLeaving.contains(teamDLs.get(i))) {
                    teamDLs.remove(i);
                    dlNeeds++;
                } else {
                    teamDLs.get(i).advanceSeason();
                    i++;
                }
            }

            i = 0;
            while (i < teamLBs.size()) {
                if (playersLeaving.contains(teamLBs.get(i))) {
                    teamLBs.remove(i);
                    lbNeeds++;
                } else {
                    teamLBs.get(i).advanceSeason();
                    i++;
                }
            }
        }

        // Leavers are off the roster; clear so next year's getPlayersLeaving() can rebuild
        playersLeaving.clear();
        if (playersTransferring != null) {
            playersTransferring.clear();
        }

        // HS recruiting handled by LeagueOffseason shared class after portal
        if (!userControlled) {
            resetStats();
        }
    }

    /**
     * Recruits the needed amount of players at each position.
     * Rating of each player based on the program's talent gravity.
     * This is used when first creating a team.
     * @param qbNeeds
     * @param rbNeeds
     * @param wrNeeds
     * @param kNeeds
     * @param olNeeds
     * @param sNeeds
     * @param cbNeeds
     * @param lbNeeds
     */
    public void recruitPlayers( int qbNeeds, int rbNeeds, int fbNeeds, int wrNeeds, int teNeeds,
                                int olNeeds, int kNeeds, int pNeeds, int sNeeds, int cbNeeds,
                                int edgeNeeds, int dlNeeds, int lbNeeds ) {
        //make team
        int talentGravity = programProfile.talentGravity;
        int stars = talentGravity/20 + 1;
        int chance = 20 - (talentGravity - 20*( talentGravity/20 )); //between 0 and 20

        for( int i = 0; i < qbNeeds; ++i ) {
            //make QBs
            if ( 100*Math.random() < 5*chance ) {
                teamQBs.add( PlayerFactory.fromStars(PositionGroup.QB, league.getRandName(), (int)(5*Math.random() + 1), stars-1, this, new java.util.Random()) );
            } else {
                teamQBs.add( PlayerFactory.fromStars(PositionGroup.QB, league.getRandName(), (int)(5*Math.random() + 1), stars, this, new java.util.Random()) );
            }
        }

        for( int i = 0; i < kNeeds; ++i ) {
            //make Ks
            if ( 100*Math.random() < 5*chance ) {
                teamKs.add( PlayerFactory.fromStars(PositionGroup.K, league.getRandName(), (int)(5*Math.random() + 1), stars-1, this, new java.util.Random()) );
            } else {
                teamKs.add( PlayerFactory.fromStars(PositionGroup.K, league.getRandName(), (int)(5*Math.random() + 1), stars, this, new java.util.Random()) );
            }
        }

        for( int i = 0; i < pNeeds; ++i ) {
            if ( 100*Math.random() < 5*chance ) {
                teamPs.add( PlayerFactory.fromStars(PositionGroup.P, league.getRandName(), (int)(5*Math.random() + 1), stars-1, this, new java.util.Random()) );
            } else {
                teamPs.add( PlayerFactory.fromStars(PositionGroup.P, league.getRandName(), (int)(5*Math.random() + 1), stars, this, new java.util.Random()) );
            }
        }

        for( int i = 0; i < rbNeeds; ++i ) {
            //make RBs
            if ( 100*Math.random() < 5*chance ) {
                teamRBs.add( PlayerFactory.fromStars(PositionGroup.RB, league.getRandName(), (int)(5*Math.random() + 1), stars-1, this, new java.util.Random()) );
            } else {
                teamRBs.add( PlayerFactory.fromStars(PositionGroup.RB, league.getRandName(), (int)(5*Math.random() + 1), stars, this, new java.util.Random()) );
            }
        }

        for( int i = 0; i < wrNeeds; ++i ) {
            //make WRs
            if ( 100*Math.random() < 5*chance ) {
                teamWRs.add( PlayerFactory.fromStars(PositionGroup.WR, league.getRandName(), (int)(5*Math.random() + 1), stars-1, this, new java.util.Random()) );
            } else {
                teamWRs.add( PlayerFactory.fromStars(PositionGroup.WR, league.getRandName(), (int)(5*Math.random() + 1), stars, this, new java.util.Random()) );
            }
        }

        for( int i = 0; i < olNeeds; ++i ) {
            //make OLs
            if ( 100*Math.random() < 5*chance ) {
                teamOLs.add( PlayerFactory.fromStars(PositionGroup.OL, league.getRandName(), (int)(5*Math.random() + 1), stars-1, this, new java.util.Random()) );
            } else {
                teamOLs.add( PlayerFactory.fromStars(PositionGroup.OL, league.getRandName(), (int)(5*Math.random() + 1), stars, this, new java.util.Random()) );
            }
        }

        for( int i = 0; i < cbNeeds; ++i ) {
            //make CBs
            if ( 100*Math.random() < 5*chance ) {
                teamCBs.add( PlayerFactory.fromStars(PositionGroup.CB, league.getRandName(), (int)(5*Math.random() + 1), stars-1, this, new java.util.Random()) );
            } else {
                teamCBs.add( PlayerFactory.fromStars(PositionGroup.CB, league.getRandName(), (int)(5*Math.random() + 1), stars, this, new java.util.Random()) );
            }
        }

        for( int i = 0; i < fbNeeds; ++i ) {
            if ( 100*Math.random() < 5*chance ) {
                teamFBs.add( PlayerFactory.fromStars(PositionGroup.FB, league.getRandName(), (int)(5*Math.random() + 1), stars-1, this, new java.util.Random()) );
            } else {
                teamFBs.add( PlayerFactory.fromStars(PositionGroup.FB, league.getRandName(), (int)(5*Math.random() + 1), stars, this, new java.util.Random()) );
            }
        }

        for( int i = 0; i < teNeeds; ++i ) {
            if ( 100*Math.random() < 5*chance ) {
                teamTEs.add( PlayerFactory.fromStars(PositionGroup.TE, league.getRandName(), (int)(5*Math.random() + 1), stars-1, this, new java.util.Random()) );
            } else {
                teamTEs.add( PlayerFactory.fromStars(PositionGroup.TE, league.getRandName(), (int)(5*Math.random() + 1), stars, this, new java.util.Random()) );
            }
        }

        for( int i = 0; i < edgeNeeds; ++i ) {
            if ( 100*Math.random() < 5*chance ) {
                teamEDGEs.add( PlayerFactory.fromStars(PositionGroup.EDGE, league.getRandName(), (int)(5*Math.random() + 1), stars-1, this, new java.util.Random()) );
            } else {
                teamEDGEs.add( PlayerFactory.fromStars(PositionGroup.EDGE, league.getRandName(), (int)(5*Math.random() + 1), stars, this, new java.util.Random()) );
            }
        }

        for( int i = 0; i < dlNeeds; ++i ) {
            if ( 100*Math.random() < 5*chance ) {
                teamDLs.add( PlayerFactory.fromStars(PositionGroup.DL, league.getRandName(), (int)(5*Math.random() + 1), stars-1, this, new java.util.Random()) );
            } else {
                teamDLs.add( PlayerFactory.fromStars(PositionGroup.DL, league.getRandName(), (int)(5*Math.random() + 1), stars, this, new java.util.Random()) );
            }
        }

        for( int i = 0; i < lbNeeds; ++i ) {
            if ( 100*Math.random() < 5*chance ) {
                teamLBs.add( PlayerFactory.fromStars(PositionGroup.LB, league.getRandName(), (int)(5*Math.random() + 1), stars-1, this, new java.util.Random()) );
            } else {
                teamLBs.add( PlayerFactory.fromStars(PositionGroup.LB, league.getRandName(), (int)(5*Math.random() + 1), stars, this, new java.util.Random()) );
            }
        }

        for( int i = 0; i < sNeeds; ++i ) {
            //make Ss
            if ( 100*Math.random() < 5*chance ) {
                teamSs.add( PlayerFactory.fromStars(PositionGroup.S, league.getRandName(), (int)(5*Math.random() + 1), stars-1, this, new java.util.Random()) );
            } else {
                teamSs.add( PlayerFactory.fromStars(PositionGroup.S, league.getRandName(), (int)(5*Math.random() + 1), stars, this, new java.util.Random()) );
            }
        }

        //done making players, sort them
        sortPlayers();
        DepthChart.applySystems(this);
    }

    /**
     * Recruit freshman at each position.
     * This is used after each season.
     * @param qbNeeds
     * @param rbNeeds
     * @param wrNeeds
     * @param kNeeds
     * @param olNeeds
     * @param sNeeds
     * @param cbNeeds
     * @param lbNeeds
     */
    public void recruitPlayersFreshman( int qbNeeds, int rbNeeds, int fbNeeds, int wrNeeds, int teNeeds,
                                        int olNeeds, int kNeeds, int pNeeds, int sNeeds, int cbNeeds,
                                        int edgeNeeds, int dlNeeds, int lbNeeds ) {
        //make team
        int talentGravity = programProfile.talentGravity;
        int stars = talentGravity/20 + 1;
        int chance = 20 - (talentGravity - 20*( talentGravity/20 )); //between 0 and 20

        double starsBonusChance = 0.15;
        double starsBonusDoubleChance = 0.05;

        for( int i = 0; i < qbNeeds; ++i ) {
            // Add some randomness so that players with higher stars can be recruited
            stars = talentGravity/20 + 1;
            if ( 100*Math.random() < 5*chance ) stars = stars - 1;
            if (Math.random() < starsBonusChance) stars = stars + 1;
            else if (Math.random() < starsBonusDoubleChance) stars = stars + 2;
            if (stars > 5) stars = 5;

            //make QBs
            teamQBs.add(PlayerFactory.fromStars(PositionGroup.QB, league.getRandName(), 1, stars, this, new java.util.Random()));
        }

        for( int i = 0; i < kNeeds; ++i ) {
            // Add some randomness so that players with higher stars can be recruited
            stars = talentGravity/20 + 1;
            if ( 100*Math.random() < 5*chance ) stars = stars - 1;
            if (Math.random() < starsBonusChance) stars = stars + 1;
            else if (Math.random() < starsBonusDoubleChance) stars = stars + 2;
            if (stars > 5) stars = 5;

            //make Ks
            teamKs.add( PlayerFactory.fromStars(PositionGroup.K, league.getRandName(), 1, stars, this, new java.util.Random()) );
        }

        for( int i = 0; i < pNeeds; ++i ) {
            stars = talentGravity/20 + 1;
            if ( 100*Math.random() < 5*chance ) stars = stars - 1;
            if (Math.random() < starsBonusChance) stars = stars + 1;
            else if (Math.random() < starsBonusDoubleChance) stars = stars + 2;
            if (stars > 5) stars = 5;
            teamPs.add( PlayerFactory.fromStars(PositionGroup.P, league.getRandName(), 1, stars, this, new java.util.Random()) );
        }

        for( int i = 0; i < rbNeeds; ++i ) {
            // Add some randomness so that players with higher stars can be recruited
            stars = talentGravity/20 + 1;
            if ( 100*Math.random() < 5*chance ) stars = stars - 1;
            if (Math.random() < starsBonusChance) stars = stars + 1;
            else if (Math.random() < starsBonusDoubleChance) stars = stars + 2;
            if (stars > 5) stars = 5;

            //make RBs
            teamRBs.add( PlayerFactory.fromStars(PositionGroup.RB, league.getRandName(), 1, stars, this, new java.util.Random()) );
        }

        for( int i = 0; i < wrNeeds; ++i ) {
            // Add some randomness so that players with higher stars can be recruited
            stars = talentGravity/20 + 1;
            if ( 100*Math.random() < 5*chance ) stars = stars - 1;
            if (Math.random() < starsBonusChance) stars = stars + 1;
            else if (Math.random() < starsBonusDoubleChance) stars = stars + 2;
            if (stars > 5) stars = 5;

            //make WRs
            teamWRs.add( PlayerFactory.fromStars(PositionGroup.WR, league.getRandName(), 1, stars, this, new java.util.Random()) );
        }

        for( int i = 0; i < olNeeds; ++i ) {
            // Add some randomness so that players with higher stars can be recruited
            stars = talentGravity/20 + 1;
            if ( 100*Math.random() < 5*chance ) stars = stars - 1;
            if (Math.random() < starsBonusChance) stars = stars + 1;
            else if (Math.random() < starsBonusDoubleChance) stars = stars + 2;
            if (stars > 5) stars = 5;

            //make OLs
            teamOLs.add( PlayerFactory.fromStars(PositionGroup.OL, league.getRandName(), 1, stars, this, new java.util.Random()) );
        }

        for( int i = 0; i < cbNeeds; ++i ) {
            // Add some randomness so that players with higher stars can be recruited
            stars = talentGravity/20 + 1;
            if ( 100*Math.random() < 5*chance ) stars = stars - 1;
            if (Math.random() < starsBonusChance) stars = stars + 1;
            else if (Math.random() < starsBonusDoubleChance) stars = stars + 2;
            if (stars > 5) stars = 5;

            //make CBs
            teamCBs.add( PlayerFactory.fromStars(PositionGroup.CB, league.getRandName(), 1, stars, this, new java.util.Random()) );
        }

        for( int i = 0; i < fbNeeds; ++i ) {
            stars = talentGravity/20 + 1;
            if ( 100*Math.random() < 5*chance ) stars = stars - 1;
            if (Math.random() < starsBonusChance) stars = stars + 1;
            else if (Math.random() < starsBonusDoubleChance) stars = stars + 2;
            if (stars > 5) stars = 5;
            teamFBs.add( PlayerFactory.fromStars(PositionGroup.FB, league.getRandName(), 1, stars, this, new java.util.Random()) );
        }

        for( int i = 0; i < teNeeds; ++i ) {
            stars = talentGravity/20 + 1;
            if ( 100*Math.random() < 5*chance ) stars = stars - 1;
            if (Math.random() < starsBonusChance) stars = stars + 1;
            else if (Math.random() < starsBonusDoubleChance) stars = stars + 2;
            if (stars > 5) stars = 5;
            teamTEs.add( PlayerFactory.fromStars(PositionGroup.TE, league.getRandName(), 1, stars, this, new java.util.Random()) );
        }

        for( int i = 0; i < edgeNeeds; ++i ) {
            stars = talentGravity/20 + 1;
            if ( 100*Math.random() < 5*chance ) stars = stars - 1;
            if (Math.random() < starsBonusChance) stars = stars + 1;
            else if (Math.random() < starsBonusDoubleChance) stars = stars + 2;
            if (stars > 5) stars = 5;
            teamEDGEs.add( PlayerFactory.fromStars(PositionGroup.EDGE, league.getRandName(), 1, stars, this, new java.util.Random()) );
        }

        for( int i = 0; i < dlNeeds; ++i ) {
            stars = talentGravity/20 + 1;
            if ( 100*Math.random() < 5*chance ) stars = stars - 1;
            if (Math.random() < starsBonusChance) stars = stars + 1;
            else if (Math.random() < starsBonusDoubleChance) stars = stars + 2;
            if (stars > 5) stars = 5;
            teamDLs.add( PlayerFactory.fromStars(PositionGroup.DL, league.getRandName(), 1, stars, this, new java.util.Random()) );
        }

        for( int i = 0; i < lbNeeds; ++i ) {
            stars = talentGravity/20 + 1;
            if ( 100*Math.random() < 5*chance ) stars = stars - 1;
            if (Math.random() < starsBonusChance) stars = stars + 1;
            else if (Math.random() < starsBonusDoubleChance) stars = stars + 2;
            if (stars > 5) stars = 5;
            teamLBs.add( PlayerFactory.fromStars(PositionGroup.LB, league.getRandName(), 1, stars, this, new java.util.Random()) );
        }

        for( int i = 0; i < sNeeds; ++i ) {
            // Add some randomness so that players with higher stars can be recruited
            stars = talentGravity/20 + 1;
            if ( 100*Math.random() < 5*chance ) stars = stars - 1;
            if (Math.random() < starsBonusChance) stars = stars + 1;
            else if (Math.random() < starsBonusDoubleChance) stars = stars + 2;
            if (stars > 5) stars = 5;

            //make Ss
            teamSs.add( PlayerFactory.fromStars(PositionGroup.S, league.getRandName(), 1, stars, this, new java.util.Random()) );
        }

        //done making players, sort them
        sortPlayers();
    }

    /**
     * Recruits walk ons at each needed position.
     * This is used by user teams if there is a dearth at any position.
     */
    public void recruitWalkOns() {
        // Pad to minimum depth suggestions with PWOs; never exceed roster cap
        addWalkOns("QB", NilMoney.SUG_QB - teamQBs.size());
        addWalkOns("RB", NilMoney.SUG_RB - teamRBs.size());
        addWalkOns("FB", NilMoney.SUG_FB - teamFBs.size());
        addWalkOns("WR", NilMoney.SUG_WR - teamWRs.size());
        addWalkOns("TE", NilMoney.SUG_TE - teamTEs.size());
        addWalkOns("OL", NilMoney.SUG_OL - teamOLs.size());
        addWalkOns("K", NilMoney.SUG_K - teamKs.size());
        addWalkOns("P", NilMoney.SUG_P - teamPs.size());
        addWalkOns("S", NilMoney.SUG_S - teamSs.size());
        addWalkOns("CB", NilMoney.SUG_CB - teamCBs.size());
        addWalkOns("EDGE", NilMoney.SUG_EDGE - teamEDGEs.size());
        addWalkOns("DL", NilMoney.SUG_DL - teamDLs.size());
        addWalkOns("LB", NilMoney.SUG_LB - teamLBs.size());
        sortPlayers();
    }

    private void addWalkOns(String pos, int needs) {
        for (int i = 0; i < needs; ++i) {
            if (!canAddToRoster()) return;
            Player p = null;
            PositionGroup g = PositionGroup.fromToken(pos);
            if (g == PositionGroup.QB) {
                p = PlayerFactory.fromStars(PositionGroup.QB, league.getRandName(), 1, 2, this, new java.util.Random());
                teamQBs.add(p);
            } else if (g == PositionGroup.RB) {
                p = PlayerFactory.fromStars(PositionGroup.RB, league.getRandName(), 1, 2, this, new java.util.Random());
                teamRBs.add(p);
            } else if (g == PositionGroup.WR) {
                p = PlayerFactory.fromStars(PositionGroup.WR, league.getRandName(), 1, 2, this, new java.util.Random());
                teamWRs.add(p);
            } else if (g == PositionGroup.OL) {
                p = PlayerFactory.fromStars(PositionGroup.OL, league.getRandName(), 1, 2, this, new java.util.Random());
                teamOLs.add(p);
            } else if (g == PositionGroup.K) {
                p = PlayerFactory.fromStars(PositionGroup.K, league.getRandName(), 1, 2, this, new java.util.Random());
                teamKs.add(p);
            } else if (g == PositionGroup.P) {
                p = PlayerFactory.fromStars(PositionGroup.P, league.getRandName(), 1, 2, this, new java.util.Random());
                teamPs.add(p);
            } else if (g == PositionGroup.S) {
                p = PlayerFactory.fromStars(PositionGroup.S, league.getRandName(), 1, 2, this, new java.util.Random());
                teamSs.add(p);
            } else if (g == PositionGroup.CB) {
                p = PlayerFactory.fromStars(PositionGroup.CB, league.getRandName(), 1, 2, this, new java.util.Random());
                teamCBs.add(p);
            } else if (g == PositionGroup.FB) {
                p = PlayerFactory.fromStars(PositionGroup.FB, league.getRandName(), 1, 2, this, new java.util.Random());
                teamFBs.add(p);
            } else if (g == PositionGroup.TE) {
                p = PlayerFactory.fromStars(PositionGroup.TE, league.getRandName(), 1, 2, this, new java.util.Random());
                teamTEs.add(p);
            } else if (g == PositionGroup.EDGE) {
                p = PlayerFactory.fromStars(PositionGroup.EDGE, league.getRandName(), 1, 2, this, new java.util.Random());
                teamEDGEs.add(p);
            } else if (g == PositionGroup.DL) {
                p = PlayerFactory.fromStars(PositionGroup.DL, league.getRandName(), 1, 2, this, new java.util.Random());
                teamDLs.add(p);
            } else if (g == PositionGroup.LB) {
                p = PlayerFactory.fromStars(PositionGroup.LB, league.getRandName(), 1, 2, this, new java.util.Random());
                teamLBs.add(p);
            }
            if (p != null) p.applyOffer(RosterStatus.PWO, 0);
        }
    }

    /**
     * After attrition, recruit toward roster target (~90), not only 1:1 replacements.
     */
    public void recruitTowardTarget() {
        int qbNeed = Math.max(0, NilMoney.SUG_QB - teamQBs.size());
        int rbNeed = Math.max(0, NilMoney.SUG_RB - teamRBs.size());
        int fbNeed = Math.max(0, NilMoney.SUG_FB - teamFBs.size());
        int wrNeed = Math.max(0, NilMoney.SUG_WR - teamWRs.size());
        int teNeed = Math.max(0, NilMoney.SUG_TE - teamTEs.size());
        int olNeed = Math.max(0, NilMoney.SUG_OL - teamOLs.size());
        int kNeed = Math.max(0, NilMoney.SUG_K - teamKs.size());
        int pNeed = Math.max(0, NilMoney.SUG_P - teamPs.size());
        int sNeed = Math.max(0, NilMoney.SUG_S - teamSs.size());
        int cbNeed = Math.max(0, NilMoney.SUG_CB - teamCBs.size());
        int edgeNeed = Math.max(0, NilMoney.SUG_EDGE - teamEDGEs.size());
        int dlNeed = Math.max(0, NilMoney.SUG_DL - teamDLs.size());
        int lbNeed = Math.max(0, NilMoney.SUG_LB - teamLBs.size());

        int baseNeed = qbNeed + rbNeed + fbNeed + wrNeed + teNeed + olNeed + kNeed + pNeed + sNeed + cbNeed + edgeNeed + dlNeed + lbNeed;
        int extras = Math.max(0, NilMoney.ROSTER_TARGET - getRosterCount() - baseNeed);
        while (extras > 0 && getRosterCount() < NilMoney.ROSTER_TARGET) {
            double bestGap = -1;
            int best = -1;
            for (int i = 0; i < NilMoney.POSITIONS.length; i++) {
                String pos = NilMoney.POSITIONS[i];
                ArrayList<? extends Player> list = getPositionList(pos);
                int have = list != null ? list.size() : 0;
                double gap = (NilMoney.initFor(pos) - have) / (double) NilMoney.initFor(pos);
                if (gap > bestGap) {
                    bestGap = gap;
                    best = i;
                }
            }
            if (bestGap <= 0) break;
            switch (NilMoney.POSITIONS[best]) {
                case "QB": qbNeed++; break;
                case "RB": rbNeed++; break;
                case "FB": fbNeed++; break;
                case "WR": wrNeed++; break;
                case "TE": teNeed++; break;
                case "OL": olNeed++; break;
                case "K": kNeed++; break;
                case "P": pNeed++; break;
                case "S": sNeed++; break;
                case "CB": cbNeed++; break;
                case "EDGE": edgeNeed++; break;
                case "DL": dlNeed++; break;
                case "LB": lbNeed++; break;
            }
            extras--;
        }

        int totalNeed = qbNeed + rbNeed + fbNeed + wrNeed + teNeed + olNeed + kNeed + pNeed + sNeed + cbNeed + edgeNeed + dlNeed + lbNeed;
        int room = NilMoney.ROSTER_CAP - getRosterCount();
        if (totalNeed > room && room > 0) {
            double scale = room / (double) totalNeed;
            qbNeed = (int) Math.floor(qbNeed * scale);
            rbNeed = (int) Math.floor(rbNeed * scale);
            fbNeed = (int) Math.floor(fbNeed * scale);
            wrNeed = (int) Math.floor(wrNeed * scale);
            teNeed = (int) Math.floor(teNeed * scale);
            olNeed = (int) Math.floor(olNeed * scale);
            kNeed = (int) Math.floor(kNeed * scale);
            pNeed = (int) Math.floor(pNeed * scale);
            sNeed = (int) Math.floor(sNeed * scale);
            cbNeed = (int) Math.floor(cbNeed * scale);
            edgeNeed = (int) Math.floor(edgeNeed * scale);
            dlNeed = (int) Math.floor(dlNeed * scale);
            lbNeed = (int) Math.floor(lbNeed * scale);
        } else if (room <= 0) {
            return;
        }

        recruitPlayersFreshman(qbNeed, rbNeed, fbNeed, wrNeed, teNeed, olNeed, kNeed, pNeed, sNeed, cbNeed, edgeNeed, dlNeed, lbNeed);
        enforceScholarshipCap();
    }

    public void enforceScholarshipCap() {
        ArrayList<Player> all = getAllPlayers();
        Collections.sort(all, new Comparator<Player>() {
            @Override
            public int compare(Player a, Player b) {
                return a.ratOvr - b.ratOvr;
            }
        });
        while (getScholarshipCount() > NilMoney.SCHOLARSHIP_CAP) {
            boolean demoted = false;
            for (Player p : all) {
                if (p.rosterStatus != null && p.rosterStatus.usesScholarship()) {
                    p.applyOffer(RosterStatus.PWO, 0);
                    demoted = true;
                    break;
                }
            }
            if (!demoted) break;
        }
    }

    /**
     * Recruit all players given in a string
     */
    public void recruitPlayersFromStr(String playersStr) {
        String[] players = playersStr.split("%\n");
        String currLine = players[0];
        int i = 0;
        while (!currLine.equals("END_RECRUITS")) {
            recruitPlayerCSV(currLine, false);
            currLine = players[++i];
        }

        // Recruit Walk-ons before any leftover players so they don't affect position needs
        recruitWalkOns();

        // Legacy saves may include a redshirt block after END_RECRUITS; recruit those as freshmen
        ++i; // skip over END_RECRUITS line
        while (i < players.length && !players[i].equals("END_REDSHIRTS")) {
            recruitPlayerCSV(players[i], false);
            ++i;
        }

    }

    /**
     * Recruit player given a CSV string
     * @param line player to be recruited
     * @param isRedshirt whether that player should be recruited as a RS
     */
    private void recruitPlayerCSV(String line, boolean isRedshirt) {
        parsePlayerSaveLine(line, isRedshirt, true);
    }

    /**
     * Parse a player CSV save line. When addToRoster is false, player is detached (portal/HS).
     */
    public Player parsePlayerSaveLine(String line, boolean isRedshirt, boolean addToRoster) {
        String histSuffix = "";
        String seasonSuffix = "";
        int histIdx = line.indexOf("|HIST");
        if (histIdx >= 0) {
            histSuffix = line.substring(histIdx);
            line = line.substring(0, histIdx);
        }
        int seasonIdx = line.indexOf("|SEASON");
        if (seasonIdx >= 0) {
            seasonSuffix = line.substring(seasonIdx);
            line = line.substring(0, seasonIdx);
        }
        String[] playerInfo = line.split(",");
        if (playerInfo.length > 2 && playerInfo[2].equals("0")) playerInfo[2] = "1";
        if ("F7".equals(playerInfo[0])) return null;
        Player loaded = PlayerSaveCodec.fromFields(this, playerInfo, isRedshirt);
        if (loaded == null) return null;
        if (addToRoster) addPlayerToRoster(loaded);
        applyLoadedPlayerExtras(loaded, playerInfo, histSuffix, seasonSuffix);
        if (!addToRoster) {
            loaded.team = null;
        }
        return loaded;
    }

    /** One CSV line for a player (no trailing newline). SAVE_VERSION 8 may include |SEASON. */
    public String playerToSaveLine(Player p) {
        if (p == null) return "";
        return PlayerSaveCodec.toLine(p) + playerSaveExtras(p);
    }

    /**
     * For news stories or other info gathering, setup player groups by student standing
     * Run through each type of player, add them to the appropriate year
     */
    private void groupPlayerStandingCSV() {
        for (Player p : teamQBs){
            if (p.year == 1) teamFRs.add(p);
            else if (p.year == 2) teamSOs.add(p);
            else if (p.year == 3) teamJRs.add(p);
            else if (p.year == 4) teamSRs.add(p);
            else if (p.year == 5) teamGrads.add(p);
        }
        for (Player p : teamRBs){
            if (p.year == 1) teamFRs.add(p);
            else if (p.year == 2) teamSOs.add(p);
            else if (p.year == 3) teamJRs.add(p);
            else if (p.year == 4) teamSRs.add(p);
            else if (p.year == 5) teamGrads.add(p);
        }
        for (Player p : teamWRs){
            if (p.year == 1) teamFRs.add(p);
            else if (p.year == 2) teamSOs.add(p);
            else if (p.year == 3) teamJRs.add(p);
            else if (p.year == 4) teamSRs.add(p);
            else if (p.year == 5) teamGrads.add(p);
        }
        for (Player p : teamKs){
            if (p.year == 1) teamFRs.add(p);
            else if (p.year == 2) teamSOs.add(p);
            else if (p.year == 3) teamJRs.add(p);
            else if (p.year == 4) teamSRs.add(p);
            else if (p.year == 5) teamGrads.add(p);
        }
        for (Player p : teamPs){
            if (p.year == 1) teamFRs.add(p);
            else if (p.year == 2) teamSOs.add(p);
            else if (p.year == 3) teamJRs.add(p);
            else if (p.year == 4) teamSRs.add(p);
            else if (p.year == 5) teamGrads.add(p);
        }
        for (Player p : teamOLs){
            if (p.year == 1) teamFRs.add(p);
            else if (p.year == 2) teamSOs.add(p);
            else if (p.year == 3) teamJRs.add(p);
            else if (p.year == 4) teamSRs.add(p);
            else if (p.year == 5) teamGrads.add(p);
        }
        for (Player p : teamSs){
            if (p.year == 1) teamFRs.add(p);
            else if (p.year == 2) teamSOs.add(p);
            else if (p.year == 3) teamJRs.add(p);
            else if (p.year == 4) teamSRs.add(p);
            else if (p.year == 5) teamGrads.add(p);
        }
        for (Player p : teamCBs){
            if (p.year == 1) teamFRs.add(p);
            else if (p.year == 2) teamSOs.add(p);
            else if (p.year == 3) teamJRs.add(p);
            else if (p.year == 4) teamSRs.add(p);
            else if (p.year == 5) teamGrads.add(p);
        }
        groupStandingList(teamFBs);
        groupStandingList(teamTEs);
        groupStandingList(teamEDGEs);
        groupStandingList(teamDLs);
        groupStandingList(teamLBs);
    }

    private void groupStandingList(ArrayList<? extends Player> players) {
        for (Player p : players) {
            if (p.year == 1) teamFRs.add(p);
            else if (p.year == 2) teamSOs.add(p);
            else if (p.year == 3) teamJRs.add(p);
            else if (p.year == 4) teamSRs.add(p);
            else if (p.year == 5) teamGrads.add(p);
        }
    }

    /**
     * Resets all team stats to 0.
     */
    public void resetStats() {
        //reset stats
        gameSchedule = new ArrayList<Game>();
        gameWinsAgainst = new ArrayList<Team>();
        gameWLSchedule = new ArrayList<String>();
        byeWeek = -1;
        confChampion = "";
        semiFinalWL = "";
        natChampWL = "";
        wins = 0;
        losses = 0;

        //set stats
        teamPoints = 0;
        teamOppPoints = 0;
        teamYards = 0;
        teamOppYards = 0;
        teamPassYards = 0;
        teamRushYards = 0;
        teamOppPassYards = 0;
        teamOppRushYards = 0;
        teamTODiff = 0;

    }

    /**
     * Updates poll score based on team stats.
     */
    public void updatePollScore() {
        updateStrengthOfWins();
        int preseasonBias = 8 - (wins + losses);
        if (preseasonBias < 0) preseasonBias = 0;
        teamPollScore = (wins*200 + 3*(teamPoints-teamOppPoints) +
                (teamYards-teamOppYards)/40 +
                3*(preseasonBias)*(programProfile.programPower + getOffTalent() + getDefTalent()) +
                teamStrengthOfWins)/10;
        if ( "CC".equals(confChampion) ) {
            //bonus for winning conference
            teamPollScore += 50;
        }
        if ( "NCW".equals(natChampWL) ) {
            //bonus for winning champ game
            teamPollScore += 100;
        }
        if ( losses == 0 ) {
            teamPollScore += 30;
        } else if ( losses == 1 ) {
            teamPollScore += 15;
        }

        teamOffTalent = getOffTalent();
        teamDefTalent = getDefTalent();
    }

    /**
     * Updates team history.
     */
    public void updateTeamHistory() {
        String histYear = league.getYear() + ": #" + rankTeamPollScore + " " + abbr + " (" + wins + "-" + losses + ") "
                + confChampion + " " + semiFinalWL + natChampWL;

        for (int i = League.REGULAR_SEASON_WEEKS; i < gameSchedule.size(); ++i) {
            Game g = gameSchedule.get(i);
            histYear += ">" + g.gameName + ": ";
            String[] gameSum = getGameSummaryStr(i);
            histYear += gameSum[1] + " " + gameSum[2];
        }

        teamHistory.add(histYear);
    }

    /**
     * Gets the team history as a String array
     * @return team history
     */
    public String[] getTeamHistoryList() {
        String[] hist = new String[teamHistory.size()+5];
        hist[0] = "Overall W-L: " + totalWins + "-" + totalLosses;
        hist[1] = "Conf Champ Record: " + totalCCs + "-" + totalCCLosses;
        hist[2] = "Bowl Game Record: " + totalBowls + "-" + totalBowlLosses;
        hist[3] = "National Champ Record: " + totalNCs + "-" + totalNCLosses;
        hist[4] = " ";
        for (int i = 0; i < teamHistory.size(); ++i) {
            hist[i+5] = teamHistory.get(i);
        }
        return hist;
    }

    /**
     * Gets a string of the entire team history
     */
    public String getTeamHistoryStr() {
        String hist = "";
        hist += "Overall W-L: " + totalWins + "-" + totalLosses + "\n";
        hist += "Conf Champ Record: " + totalCCs + "-" + totalCCLosses + "\n";
        hist += "Bowl Game Record: " + totalBowls + "-" + totalBowlLosses + "\n";
        hist += "National Champ Record: " + totalNCs + "-" + totalNCLosses + "\n";
        hist += "\nYear by year summary:\n";
        for (int i = 0; i < teamHistory.size(); ++i) {
            hist += teamHistory.get(i) + "\n";
        }
        return hist;
    }

    /**
     * Updates strength of wins based on how opponents have fared.
     */
    public void updateStrengthOfWins() {
        int strWins = 0;
        int weeks = Math.min(gameSchedule.size(), League.REGULAR_SEASON_WEEKS);
        for (int i = 0; i < weeks; ++i) {
            Game g = gameSchedule.get(i);
            if (g == null || isByeWeek(i)) {
                continue;
            }
            if (g.homeTeam == this) {
                strWins += Math.pow(league.teamList.size() - g.awayTeam.rankTeamPollScore, 2);
            } else {
                strWins += Math.pow(league.teamList.size() - g.homeTeam.rankTeamPollScore, 2);
            }
        }
        teamStrengthOfWins = strWins / 50;
        for (Team t : gameWinsAgainst) {
            teamStrengthOfWins += Math.pow(t.wins, 2);
        }
    }

    /**
     * Sorts players so that best players are higher in depth chart.
     * Locked healthy players keep their slots; unlocked players sort around them.
     * Injured players always sort below healthy (lock does not override injury).
     */
    public void sortPlayers() {
        sortPositionDepth(teamQBs);
        sortPositionDepth(teamRBs);
        sortPositionDepth(teamFBs);
        sortPositionDepth(teamWRs);
        sortPositionDepth(teamTEs);
        sortPositionDepth(teamKs);
        sortPositionDepth(teamPs);
        sortPositionDepth(teamOLs);
        sortPositionDepth(teamCBs);
        sortPositionDepth(teamSs);
        sortPositionDepth(teamEDGEs);
        sortPositionDepth(teamDLs);
        sortPositionDepth(teamLBs);
        DepthChart.applySystems(this);

        Collections.sort(teamFRs, new PlayerComparator());
        Collections.sort(teamSOs, new PlayerComparator());
        Collections.sort(teamJRs, new PlayerComparator());
        Collections.sort(teamSRs, new PlayerComparator());
        Collections.sort(teamGrads, new PlayerComparator());
        ensureSpecialTeamsDepth();
    }

    /**
     * Lock-aware depth sort: pinned healthy players keep indices; gaps fill by OVR.
     */
    public <T extends Player> void sortPositionDepth(ArrayList<T> players) {
        if (players == null || players.size() <= 1) return;

        int n = players.size();
        boolean[] pinned = new boolean[n];
        Player[] pinnedPlayers = new Player[n];
        ArrayList<T> movable = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            T p = players.get(i);
            if (p.depthLocked && !p.isInjured) {
                pinned[i] = true;
                pinnedPlayers[i] = p;
            } else {
                movable.add(p);
            }
        }

        if (movable.size() == n) {
            Collections.sort(players, new PlayerComparator());
            return;
        }

        Collections.sort(movable, new PlayerComparator());
        players.clear();
        int m = 0;
        for (int i = 0; i < n; i++) {
            if (pinned[i]) {
                @SuppressWarnings("unchecked")
                T locked = (T) pinnedPlayers[i];
                players.add(locked);
            } else {
                players.add(movable.get(m++));
            }
        }
    }

    /**
     * May injure players.
     * Guaranteed not to injure more than the amount of starters for each position.
     */
    public void checkForInjury() {
        playersInjured = new ArrayList<>();
        playersRecovered = new ArrayList<>();
        checkInjuryPosition(teamQBs, 1);
        checkInjuryPosition(teamRBs, 2);
        checkInjuryPosition(teamFBs, 1);
        checkInjuryPosition(teamWRs, 3);
        checkInjuryPosition(teamTEs, 1);
        checkInjuryPosition(teamOLs, 5);
        checkInjuryPosition(teamKs, 1);
        checkInjuryPosition(teamPs, 1);
        checkInjuryPosition(teamSs, 1);
        checkInjuryPosition(teamCBs, 3);
        checkInjuryPosition(teamEDGEs, 2);
        checkInjuryPosition(teamDLs, 3);
        checkInjuryPosition(teamLBs, 3);
    }

    private void checkInjuryPosition(ArrayList<? extends Player> players, int numStarters) {
        int numInjured = 0;

        for (Player p : players) {
            if (p.injury != null) {
                p.injury.advanceGame();
                numInjured++;
                if (p.injury == null) {
                    playersRecovered.add(p);
                    playersInjuredAll.remove(p);
                }
            }
        }

        // Only injure if there are people left to injure
        if (numInjured < numStarters) {
            for (int i = 0; i < numStarters; ++i) {
                Player p = players.get(i);
                if (Math.random() < Math.pow(1 - (double)p.ratDur/100, 3) && numInjured < numStarters) {
                    // injury!
                    p.injury = new Injury(p);
                    playersInjured.add(p);
                    playersInjuredAll.add(p);
                    numInjured++;
                }
            }
        }

        if (numInjured > 0) sortPositionDepthList(players);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void sortPositionDepthList(ArrayList<? extends Player> players) {
        sortPositionDepth((ArrayList) players);
    }

    /** Lock-aware OVR sort for one position index (0–7). */
    public void sortPositionDepth(int position) {
        ArrayList<? extends Player> list = positionList(position);
        if (list != null) sortPositionDepthList(list);
    }

    /**
     * Gets a list of all the players that were injured that week.
     * @return list of players in string
     */
    public String[] getInjuryReport() {
        if (playersInjured.size() > 0 || playersRecovered.size() > 0) {
            String[] injuries;

            if (playersRecovered.size() > 0) injuries = new String[playersInjured.size() + playersRecovered.size() + 1];
            else injuries = new String[playersInjured.size()];

            for (int i = 0; i < playersInjured.size(); ++i) {
                injuries[i] = playersInjured.get(i).getPosNameYrOvrPot_Str();
            }

            if (playersRecovered.size() > 0) {
                injuries[ playersInjured.size() ] = "Players Recovered from Injuries:> ";
                for (int i = 0; i < playersRecovered.size(); ++i) {
                    injuries[ playersInjured.size() + i + 1 ] = playersRecovered.get(i).getPosNameYrOvrPot_Str();
                }
            }

            return injuries;
        }
        else return null;
    }

    /**
     * Get rid of all injuries
     */
    public void curePlayers() {
        curePlayersPosition(teamQBs);
        curePlayersPosition(teamRBs);
        curePlayersPosition(teamFBs);
        curePlayersPosition(teamWRs);
        curePlayersPosition(teamTEs);
        curePlayersPosition(teamOLs);
        curePlayersPosition(teamKs);
        curePlayersPosition(teamPs);
        curePlayersPosition(teamSs);
        curePlayersPosition(teamCBs);
        curePlayersPosition(teamEDGEs);
        curePlayersPosition(teamDLs);
        curePlayersPosition(teamLBs);
        sortPlayers();
    }

    private void curePlayersPosition(ArrayList<? extends Player> players) {
        for (Player p : players) {
            p.injury = null;
            p.isInjured = false;
        }
    }

    /**
     * Calculates offensive talent level of team.
     * @return Offensive Talent Level
     */
    public int getOffTalent() {
        int qb = teamQBs.isEmpty() ? 0 : getQB(0).ratOvr;
        // Sum (not average) of available depth so thin portal rosters don't crash.
        int wrSum = sumOvr(teamWRs, 3);
        int rbSum = sumOvr(teamRBs, 2);
        // Keep the historical /12 divisor even when a mid-offseason portal thinned depth.
        return (qb * 5 + wrSum + rbSum + getCompositeOLPass() + getCompositeOLRush()) / 12;
    }

    /**
     * Calculates defensive talent level of team.
     * @return Defensive Talent Level
     */
    public int getDefTalent() {
        return ( getRushDef() + getPassDef() ) / 2;
    }

    /**
     * Special teams talent from K/P/RET overalls.
     * Kickers handle punts in this sim, so K and P use the same specialist.
     * RET is the average of punt and kick returner overalls.
     * @return Special Teams Talent Level
     */
    public int getSTTalent() {
        ensureSpecialTeamsDepth();
        int k = getK(0).ratOvr;
        int p = k;
        Player pr = getPuntReturner();
        Player kr = getKickReturner();
        int prOvr = pr != null ? pr.ratOvr : k;
        int krOvr = kr != null ? kr.ratOvr : prOvr;
        int ret = (prOvr + krOvr) / 2;
        return (k + p + ret) / 3;
    }

    /**
     * Get the composite Football IQ of the team. Is used in game simulation.
     * @return football iq of the team
     */
    public int getCompositeFootIQ() {
        int comp = 0;
        comp += getQB(0).ratFootIQ * 5;
        comp += getRB(0).ratFootIQ + getRB(1).ratFootIQ;
        comp += getWR(0).ratFootIQ + getWR(1).ratFootIQ + getWR(2).ratFootIQ;
        for (int i = 0; i < 5; ++i) {
            comp += getOL(i).ratFootIQ/5;
        }
        comp += getS(0).ratFootIQ * 5;
        comp += getCB(0).ratFootIQ + getCB(1).ratFootIQ + getCB(2).ratFootIQ;
        if (!teamFBs.isEmpty()) comp += getFB(0).ratFootIQ;
        if (!teamTEs.isEmpty()) comp += getTE(0).ratFootIQ;
        for (int i = 0; i < 2; ++i) {
            if (i < teamEDGEs.size()) comp += getEDGE(i).ratFootIQ / 2;
        }
        for (int i = 0; i < 3; ++i) {
            if (i < teamDLs.size()) comp += getDL(i).ratFootIQ / 3;
        }
        for (int i = 0; i < 3; ++i) {
            if (i < teamLBs.size()) comp += getLB(i).ratFootIQ / 3;
        }
        return comp / 24;
    }

    /**
     * Gets the recruiting class strength.
     * Adds up all the ovrs of freshman
     * @return class strength as a number
     */
    public int getRecruitingClassRat() {
        int classStrength = 0;
        int numFreshman = 0;
        ArrayList<Player> allPlayers = getAllPlayers();
        for (Player p : allPlayers) {
            if (p.year == 1 && p.ratOvr > 65) {
                // Is freshman
                classStrength += p.ratOvr - 30;
                numFreshman++;
            }
        }

        if (numFreshman > 0)
            return classStrength * (classStrength/numFreshman) / 100;
        else return 0;
    }

    public Player getQB(int depth) {
        if ( depth < teamQBs.size() && depth >= 0 ) {
            return teamQBs.get(depth);
        } else {
            return teamQBs.get(0);
        }
    }

    public Player getRB(int depth) {
        if ( depth < teamRBs.size() && depth >= 0 ) {
            return teamRBs.get(depth);
        } else {
            return teamRBs.get(0);
        }
    }

    public Player getWR(int depth) {
        if ( depth < teamWRs.size() && depth >= 0 ) {
            return teamWRs.get(depth);
        } else {
            return teamWRs.get(0);
        }
    }

    public Player getK(int depth) {
        if (teamKs.isEmpty()) return null;
        if ( depth < teamKs.size() && depth >= 0 ) {
            return teamKs.get(depth);
        } else {
            return teamKs.get(0);
        }
    }

    public Player getP(int depth) {
        if (teamPs == null || teamPs.isEmpty()) return null;
        if ( depth < teamPs.size() && depth >= 0 ) {
            return teamPs.get(depth);
        }
        return teamPs.get(0);
    }

    /** Punter with emergency K fallback. */
    public Player getPunter(int depth) {
        Player p = getP(depth);
        if (p != null) return p;
        return getK(0);
    }

    public Player getPuntReturner() {
        if (puntReturner != null && !puntReturner.isInjured) return puntReturner;
        ensureSpecialTeamsDepth();
        return puntReturner;
    }

    public Player getKickReturner() {
        if (kickReturner != null && !kickReturner.isInjured) return kickReturner;
        ensureSpecialTeamsDepth();
        return kickReturner;
    }

    public Player getGunner1() {
        if (gunner1 != null && !gunner1.isInjured) return gunner1;
        ensureSpecialTeamsDepth();
        return gunner1;
    }

    public Player getGunner2() {
        if (gunner2 != null && !gunner2.isInjured) return gunner2;
        ensureSpecialTeamsDepth();
        return gunner2;
    }

    public Player getLongSnapper() {
        if (longSnapper != null && !longSnapper.isInjured) return longSnapper;
        ensureSpecialTeamsDepth();
        return longSnapper;
    }

    /** Fill missing/injured ST overlays from best available roster talent. */
    public void ensureSpecialTeamsDepth() {
        ArrayList<Player> returnPool = specialTeamsReturnPool();
        ArrayList<Player> gunnerPool = specialTeamsGunnerPool();
        ArrayList<Player> snapPool = specialTeamsSnapPool();

        if (!isHealthyOnRoster(puntReturner)) {
            puntReturner = pickBestSpeed(returnPool, null, null);
        }
        if (!isHealthyOnRoster(kickReturner)) {
            kickReturner = pickBestSpeed(returnPool, puntReturner, null);
            if (kickReturner == null) kickReturner = puntReturner;
        }
        if (!isHealthyOnRoster(gunner1)) {
            gunner1 = pickBestSpeed(gunnerPool, puntReturner, kickReturner);
            if (gunner1 == null) gunner1 = pickBestSpeed(returnPool, puntReturner, kickReturner);
        }
        if (!isHealthyOnRoster(gunner2)) {
            gunner2 = pickBestSpeed(gunnerPool, gunner1, puntReturner);
            if (gunner2 == null) gunner2 = pickBestSpeed(returnPool, gunner1, kickReturner);
        }
        if (!isHealthyOnRoster(longSnapper)) {
            longSnapper = snapPool.isEmpty() ? null : snapPool.get(0);
        }
    }

    public String specialTeamsDepthSaveLine() {
        ensureSpecialTeamsDepth();
        return "ST_DEPTH,"
                + playerRefOrEmpty(puntReturner) + ","
                + playerRefOrEmpty(kickReturner) + ","
                + playerRefOrEmpty(gunner1) + ","
                + playerRefOrEmpty(gunner2) + ","
                + playerRefOrEmpty(longSnapper);
    }

    public void loadSpecialTeamsDepth(String line) {
        if (line == null || !line.startsWith("ST_DEPTH,")) {
            ensureSpecialTeamsDepth();
            return;
        }
        String[] parts = line.split(",", -1);
        puntReturner = findRosterPlayerRef(parts.length > 1 ? parts[1] : null);
        kickReturner = findRosterPlayerRef(parts.length > 2 ? parts[2] : null);
        gunner1 = findRosterPlayerRef(parts.length > 3 ? parts[3] : null);
        gunner2 = findRosterPlayerRef(parts.length > 4 ? parts[4] : null);
        longSnapper = findRosterPlayerRef(parts.length > 5 ? parts[5] : null);
        ensureSpecialTeamsDepth();
    }

    /** Candidate pool for PR/KR depth UI (WR/RB/CB/S). */
    public ArrayList<Player> specialTeamsReturnPool() {
        ArrayList<Player> pool = new ArrayList<>();
        pool.addAll(teamWRs);
        pool.addAll(teamRBs);
        pool.addAll(teamCBs);
        pool.addAll(teamSs);
        sortPoolByReturnSkill(pool);
        return pool;
    }

    public ArrayList<Player> specialTeamsGunnerPool() {
        ArrayList<Player> pool = new ArrayList<>();
        pool.addAll(teamWRs);
        pool.addAll(teamCBs);
        sortPoolByReturnSkill(pool);
        return pool;
    }

    public ArrayList<Player> specialTeamsSnapPool() {
        ArrayList<Player> pool = new ArrayList<>();
        pool.addAll(teamOLs);
        pool.addAll(teamTEs);
        Collections.sort(pool, new PlayerComparator());
        return pool;
    }

    public void setSpecialTeamsSlot(int slotIndex, Player player) {
        if (player == null) return;
        switch (slotIndex) {
            case 0: puntReturner = player; break;
            case 1: kickReturner = player; break;
            case 2: gunner1 = player; break;
            case 3: gunner2 = player; break;
            case 4: longSnapper = player; break;
            default: break;
        }
    }

    public Player getSpecialTeamsSlot(int slotIndex) {
        switch (slotIndex) {
            case 0: return getPuntReturner();
            case 1: return getKickReturner();
            case 2: return getGunner1();
            case 3: return getGunner2();
            case 4: return getLongSnapper();
            default: return null;
        }
    }

    /** Save token: position:name:year (name-only still accepted on load). */
    private static String playerRefOrEmpty(Player p) {
        if (p == null || p.name == null || p.name.isEmpty()) {
            return "";
        }
        String pos = p.position != null ? p.position : "";
        return pos + ":" + p.name + ":" + p.year;
    }

    private boolean isHealthyOnRoster(Player p) {
        return p != null && !p.isInjured && getAllPlayers().contains(p);
    }

    private Player findRosterPlayerRef(String ref) {
        if (ref == null || ref.isEmpty()) {
            return null;
        }
        String pos = null;
        String name = ref;
        int year = -1;
        String[] bits = ref.split(":", -1);
        if (bits.length >= 3) {
            pos = bits[0];
            // Name may contain ':' — year is the final segment.
            year = parseTrailingInt(bits[bits.length - 1], -1);
            StringBuilder nameSb = new StringBuilder(bits[1]);
            for (int i = 2; i < bits.length - 1; i++) {
                nameSb.append(':').append(bits[i]);
            }
            name = nameSb.toString();
        }
        Player namedOnly = null;
        for (Player p : getAllPlayers()) {
            if (p.name == null || !name.equals(p.name)) {
                continue;
            }
            if (pos != null && year >= 0
                    && pos.equals(p.position)
                    && p.year == year) {
                return p;
            }
            if (namedOnly == null) {
                namedOnly = p;
            }
        }
        return namedOnly;
    }

    private static int parseTrailingInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static Player pickBestSpeed(ArrayList<Player> pool, Player excludeA, Player excludeB) {
        Player best = null;
        int bestScore = Integer.MIN_VALUE;
        for (Player p : pool) {
            if (p == null || p.isInjured) continue;
            if (p == excludeA || p == excludeB) continue;
            int score = returnSkill(p);
            if (score > bestScore) {
                bestScore = score;
                best = p;
            }
        }
        return best;
    }

    private static void sortPoolByReturnSkill(ArrayList<Player> pool) {
        Collections.sort(pool, new Comparator<Player>() {
            @Override
            public int compare(Player a, Player b) {
                boolean ai = a != null && a.isInjured;
                boolean bi = b != null && b.isInjured;
                if (ai != bi) return ai ? 1 : -1;
                return Integer.compare(returnSkill(b), returnSkill(a));
            }
        });
    }

    /** Speed + elusiveness proxy for returner ranking. */
    public static int returnSkill(Player p) {
        if (p == null) return 0;
        PositionGroup g = PositionGroup.fromToken(p.position);
        if (g == PositionGroup.WR) {
            return p.ratings.spd * 2 + p.ratings.elu;
        }
        if (g == PositionGroup.RB) {
            return p.ratings.spd * 2 + p.ratings.elu;
        }
        if (g == PositionGroup.CB) {
            return p.ratings.spd * 2 + p.ratOvr / 2;
        }
        if (g == PositionGroup.S) {
            return p.ratings.spd * 2 + p.ratOvr / 2;
        }
        return p.ratOvr;
    }

    public Player getOL(int depth) {
        if ( depth < teamOLs.size() && depth >= 0 ) {
            return teamOLs.get(depth);
        } else {
            return teamOLs.get(0);
        }
    }

    public Player getS(int depth) {
        if ( depth < teamSs.size() && depth >= 0 ) {
            return teamSs.get(depth);
        } else {
            return teamSs.get(0);
        }
    }

    public Player getCB(int depth) {
        if ( depth < teamCBs.size() && depth >= 0 ) {
            return teamCBs.get(depth);
        } else {
            return teamCBs.get(0);
        }
    }

    public Player getFB(int depth) {
        if (depth < teamFBs.size() && depth >= 0) {
            return teamFBs.get(depth);
        } else if (!teamFBs.isEmpty()) {
            return teamFBs.get(0);
        }
        return null;
    }

    public Player getTE(int depth) {
        if (depth < teamTEs.size() && depth >= 0) {
            return teamTEs.get(depth);
        } else if (!teamTEs.isEmpty()) {
            return teamTEs.get(0);
        }
        return null;
    }

    public Player getEDGE(int depth) {
        if (depth < teamEDGEs.size() && depth >= 0) {
            return teamEDGEs.get(depth);
        } else if (!teamEDGEs.isEmpty()) {
            return teamEDGEs.get(0);
        }
        return null;
    }

    public Player getDL(int depth) {
        if (depth < teamDLs.size() && depth >= 0) {
            return teamDLs.get(depth);
        } else if (!teamDLs.isEmpty()) {
            return teamDLs.get(0);
        }
        return null;
    }

    public Player getLB(int depth) {
        if (depth < teamLBs.size() && depth >= 0) {
            return teamLBs.get(depth);
        } else if (!teamLBs.isEmpty()) {
            return teamLBs.get(0);
        }
        return null;
    }

    /**
     * Get pass proficiency. The higher the more likely the team is to pass.
     * @return integer of how good the team is at passing
     */
    public int getPassProf() {
        int avgWRs = averageOvr(teamWRs, 3);
        int qb = teamQBs.isEmpty() ? 0 : getQB(0).ratOvr;
        return (getCompositeOLPass() + qb * 2 + avgWRs) / 4;
    }

    /**
     * Get run proficiency. The higher the more likely the team is to run.
     * @return integer of how good the team is at rushing
     */
    public int getRushProf() {
        int avgRBs = averageOvr(teamRBs, 2);
        return (getCompositeOLRush() + avgRBs) / 2;
    }

    /**
     * Get how good the team is at defending the pass
     * @return integer of how good
     */
    public int getPassDef() {
        int avgCBs = averageOvr(teamCBs, 3);
        int safety = teamSs.isEmpty() ? 0 : teamSs.get(0).ratOvr;
        return (avgCBs * 3 + safety + getCompositeFrontPass() * 2) / 6;
    }

    /**
     * Get how good the team is at defending the rush
     * @return integer of how good
     */
    public int getRushDef() {
        return getCompositeFrontRush();
    }

    /**
     * Get how good the OL is at defending the pass
     * Is the average of power and pass blocking.
     * @return how good they are at blocking the pass.
     */
    private static int sumOvr(List<? extends Player> players, int maxCount) {
        if (players == null || players.isEmpty() || maxCount <= 0) {
            return 0;
        }
        int n = Math.min(maxCount, players.size());
        int sum = 0;
        for (int i = 0; i < n; ++i) {
            sum += players.get(i).ratOvr;
        }
        return sum;
    }

    private static int averageOvr(List<? extends Player> players, int maxCount) {
        if (players == null || players.isEmpty() || maxCount <= 0) {
            return 0;
        }
        int n = Math.min(maxCount, players.size());
        return sumOvr(players, n) / n;
    }

    public int getCompositeOLPass() {
        if (teamOLs == null || teamOLs.isEmpty()) {
            return 0;
        }
        int n = Math.min(5, teamOLs.size());
        int compositeOL = 0;
        for (int i = 0; i < n; ++i) {
            compositeOL += (teamOLs.get(i).ratings.stre + teamOLs.get(i).ratings.pbk) / 2;
        }
        return compositeOL / n;
    }

    /**
     * Get how good the OL is at defending the rush
     * Is the average of power and rush blocking.
     * @return how good they are at blocking the rush.
     */
    public int getCompositeOLRush() {
        if (teamOLs == null || teamOLs.isEmpty()) {
            return 0;
        }
        int n = Math.min(5, teamOLs.size());
        int compositeOL = 0;
        for (int i = 0; i < n; ++i) {
            compositeOL += (teamOLs.get(i).ratings.stre + teamOLs.get(i).ratings.rbk) / 2;
        }
        return compositeOL / n;
    }

    public int getCompositeFrontPass() {
        int total = 0;
        int count = 0;
        for (int i = 0; i < 2; ++i) {
            if (i < teamEDGEs.size()) {
                Player e = getEDGE(i);
                total += (e.ratings.stre + e.ratings.prs) / 2;
                count++;
            }
        }
        for (int i = 0; i < 3; ++i) {
            if (i < teamDLs.size()) {
                Player d = getDL(i);
                total += (d.ratings.stre + d.ratings.prs) / 2;
                count++;
            }
        }
        for (int i = 0; i < 3; ++i) {
            if (i < teamLBs.size()) {
                Player lb = getLB(i);
                total += (lb.ratings.stre + lb.ratings.pcv) / 2;
                count++;
            }
        }
        return count > 0 ? total / count : 0;
    }

    public int getCompositeFrontRush() {
        int total = 0;
        int count = 0;
        for (int i = 0; i < 2; ++i) {
            if (i < teamEDGEs.size()) {
                Player e = getEDGE(i);
                total += (e.ratings.stre + e.ratings.rns) / 2;
                count++;
            }
        }
        for (int i = 0; i < 3; ++i) {
            if (i < teamDLs.size()) {
                Player d = getDL(i);
                total += (d.ratings.stre + d.ratings.rns) / 2;
                count++;
            }
        }
        for (int i = 0; i < 3; ++i) {
            if (i < teamLBs.size()) {
                Player lb = getLB(i);
                total += (lb.ratings.stre + lb.ratings.rns) / 2;
                count++;
            }
        }
        return count > 0 ? total / count : 0;
    }

    /**
     * Get comma separated value of the team stats and their rankings.
     * @return String of CSV stat,name,ranking
     */
    public String getTeamStatsStrCSV() {
        StringBuilder ts0 = new StringBuilder();

        ArrayList<Team> confTeams = new ArrayList<>();
        for (Conference c : league.conferences) {
            if (c.confName.equals(conference)) {
                confTeams.addAll(c.confTeams);
                Collections.sort(confTeams, new TeamCompConfWins());
                int confRank = 11;
                for (int i = 0; i < confTeams.size(); ++i) {
                    if (confTeams.get(i).equals(this)) {
                        confRank = i+1;
                        break;
                    }
                }
                ts0.append(getConfWins()+"-"+getConfLosses() + ",");
                ts0.append("Conf W-L" + ",");
                ts0.append(getRankStr(confRank) + "%\n");
            }
        }

        ts0.append(teamPollScore + ",");
        ts0.append("AP Votes" + ",");
        ts0.append(getRankStr(rankTeamPollScore) + "%\n");

        ts0.append(teamStrengthOfWins + ",");
        ts0.append("SOS" + ",");
        ts0.append(getRankStr(rankTeamStrengthOfWins) + "%\n");

        ts0.append(teamPoints/numGames() + ",");
        ts0.append("Points" + ",");
        ts0.append(getRankStr(rankTeamPoints) + "%\n");

        ts0.append(teamOppPoints/numGames() + ",");
        ts0.append("Opp Points" + ",");
        ts0.append(getRankStr(rankTeamOppPoints) + "%\n");

        ts0.append(teamYards/numGames() + ",");
        ts0.append("Yards" + ",");
        ts0.append(getRankStr(rankTeamYards) + "%\n");

        ts0.append(teamOppYards/numGames() + ",");
        ts0.append("Opp Yards" + ",");
        ts0.append(getRankStr(rankTeamOppYards) + "%\n");

        ts0.append(teamPassYards/numGames() + ",");
        ts0.append("Pass Yards" + ",");
        ts0.append(getRankStr(rankTeamPassYards) + "%\n");

        ts0.append(teamRushYards/numGames() + ",");
        ts0.append("Rush Yards" + ",");
        ts0.append(getRankStr(rankTeamRushYards) + "%\n");

        ts0.append(teamOppPassYards/numGames() + ",");
        ts0.append("Opp Pass YPG" + ",");
        ts0.append(getRankStr(rankTeamOppPassYards) + "%\n");

        ts0.append(teamOppRushYards/numGames() + ",");
        ts0.append("Opp Rush YPG" + ",");
        ts0.append(getRankStr(rankTeamOppRushYards) + "%\n");

        if (teamTODiff > 0) ts0.append("+" + teamTODiff + ",");
        else ts0.append(teamTODiff + ",");
        ts0.append("TO Diff" + ",");
        ts0.append(getRankStr(rankTeamTODiff) + "%\n");

        ts0.append(teamOffTalent + ",");
        ts0.append("Off Talent" + ",");
        ts0.append(getRankStr(rankTeamOffTalent) + "%\n");

        ts0.append(teamDefTalent + ",");
        ts0.append("Def Talent" + ",");
        ts0.append(getRankStr(rankTeamDefTalent) + "%\n");

        ts0.append(programProfile.programPower + ",");
        ts0.append("Program Power" + ",");
        ts0.append(getRankStr(rankTeamProgramPower) + "%\n");

        ts0.append(programProfile.tradition + ",Tradition,—%\n");
        ts0.append(programProfile.fanbase + ",Fanbase,—%\n");
        ts0.append(programProfile.donors + ",Donors,—%\n");
        ts0.append(programProfile.footprint + ",Recruiting Footprint,—%\n");
        ts0.append(programProfile.pipeline + ",NFL Pipeline,—%\n");
        ts0.append(programProfile.momentum + ",Momentum,—%\n");
        ts0.append(NilMoney.format(NilMoney.yearlyBudget(programProfile))
                + ",Purse,—%\n");

        ts0.append(getRecruitingClassRat() + ",");
        ts0.append("Recruit Class" + ",");
        ts0.append(getRankStr(rankTeamRecruitClass) + "%\n");

        return ts0.toString();
    }

    /**
     * Get the game summary of a played game.
     * [gameName, score summary, who they played]
     * @param gameNumber number of the game desired
     * @return array of name, score, who was played
     */
    public String[] getGameSummaryStr(int gameNumber) {
        String[] gs = new String[3];
        if (isByeWeek(gameNumber)) {
            gs[0] = "BYE";
            if (gameNumber < gameWLSchedule.size() && "BYE".equals(gameWLSchedule.get(gameNumber))) {
                gs[1] = "BYE";
            } else {
                gs[1] = "---";
            }
            gs[2] = "";
            return gs;
        }
        Game g = gameSchedule.get(gameNumber);
        if (g == null) {
            // Open OOC slot (before scheduling completes).
            gs[0] = "OOC";
            gs[1] = "---";
            gs[2] = "TBD";
            return gs;
        }
        gs[0] = g.gameName;
        if (gameNumber < gameWLSchedule.size()) {
            gs[1] = gameWLSchedule.get(gameNumber) + " " + gameSummaryStrScore(g);
            if (g.numOT > 0) gs[1] += " (" + g.numOT + "OT)";
        } else {
            gs[1] = "---";
        }
        gs[2] = gameSummaryStrOpponent(g);
        return gs;
    }

    public boolean isByeWeek(int week) {
        return week == byeWeek;
    }

    /**
     * True when this week is still open for an OOC game (not conference, not bye, not filled).
     */
    public boolean isOpenOocWeek(int week) {
        return week >= 0
                && week < gameSchedule.size()
                && week != byeWeek
                && gameSchedule.get(week) == null;
    }

    /**
     * Get a summary of your team's season.
     * Tells how they finished, if they beat/fell short of expecations, and if they won rivalry game.
     * @return String of season summary
     */
    public String seasonSummaryStr() {
        String summary = "Your team, " + name + ", finished the season ranked #" + rankTeamPollScore + " with " + wins + " wins and " + losses + " losses.";
        int expectedPollFinish = programProfile.expectedPollFinish(league.teamList.size());
        int diffExpected = expectedPollFinish - rankTeamPollScore;

        if ( natChampWL.equals("NCW") ) {
            summary += "\n\nYou won the National Championship. Momentum, donor support, fan demand, and long-term tradition will all benefit.";
        }

        if (diffExpected > 2) {
            summary += "\n\nYou beat the program's #" + expectedPollFinish
                    + " expectation. Momentum and collective support should rise.";
        } else if (diffExpected < -2) {
            summary += "\n\nYou fell short of the program's #" + expectedPollFinish
                    + " expectation. Momentum and donor support should soften.";
        } else {
            summary += "\n\nThe season landed near the program's #" + expectedPollFinish
                    + " expectation, so its market profile should remain stable.";
        }

        if (programProfileUpdatedThisOffseason) {
            summary += "\n\nProgram changes: Power " + signed(programProfile.diffProgramPower)
                    + " · Momentum " + signed(programProfile.diffMomentum)
                    + " · Donors " + signed(programProfile.diffDonors)
                    + " · Fans " + signed(programProfile.diffFanbase)
                    + " · Tradition " + signed(programProfile.diffTradition)
                    + " · Footprint " + signed(programProfile.diffFootprint)
                    + " · NFL pipeline " + signed(programProfile.diffPipeline) + ".";
        }

        summary += rivalryMomentumSummaryLines();
        for (String note : RivalryDynamics.previewNotes(this)) {
            summary += "\n\n" + note;
        }

        return summary;
    }

    private static String signed(int value) {
        return value > 0 ? "+" + value : Integer.toString(value);
    }

    public Player findBenchPlayer(String line) {
        for (Player p : teamQBs) {
            if (p.getPosNameYrOvrPot_Str().equals(line)) return p;
        }
        for (Player p : teamRBs) {
            if (p.getPosNameYrOvrPot_Str().equals(line)) return p;
        }
        for (Player p : teamWRs) {
            if (p.getPosNameYrOvrPot_Str().equals(line)) return p;
        }
        for (Player p : teamOLs) {
            if (p.getPosNameYrOvrPot_Str().equals(line)) return p;
        }
        for (Player p : teamKs) {
            if (p.getPosNameYrOvrPot_Str().equals(line)) return p;
        }
        for (Player p : teamSs) {
            if (p.getPosNameYrOvrPot_Str().equals(line)) return p;
        }
        for (Player p : teamCBs) {
            if (p.getPosNameYrOvrPot_Str().equals(line)) return p;
        }
        for (Player p : teamFBs) {
            if (p.getPosNameYrOvrPot_Str().equals(line)) return p;
        }
        for (Player p : teamTEs) {
            if (p.getPosNameYrOvrPot_Str().equals(line)) return p;
        }
        for (Player p : teamEDGEs) {
            if (p.getPosNameYrOvrPot_Str().equals(line)) return p;
        }
        for (Player p : teamDLs) {
            if (p.getPosNameYrOvrPot_Str().equals(line)) return p;
        }
        for (Player p : teamLBs) {
            if (p.getPosNameYrOvrPot_Str().equals(line)) return p;
        }
        return null;
    }

    /**
     * Gets rank str, i.e. 12 -> 12th, 3 -> 3rd
     * @param num ranking
     * @return string of the ranking with correct ending
     */
    public String getRankStr(int num) {
        if (num == 11) {
            return "11th";
        } else if (num == 12) {
            return "12th";
        } else if (num == 13) {
            return "13th";
        } else if (num%10 == 1) {
            return num + "st";
        } else if (num%10 == 2) {
            return num + "nd";
        } else if (num%10 == 3){
            return num + "rd";
        } else {
            return num + "th";
        }
    }

    /**
     * Get rank string of the user (no longer used?)
     * @param num ranking
     * @return ranking with correct ending
     */
    public String getRankStrStarUser(int num) {
        if (true) {
            if (num == 11) {
                return "11th";
            } else if (num == 12) {
                return "12th";
            } else if (num == 13) {
                return "13th";
            } else if (num % 10 == 1) {
                return num + "st";
            } else if (num % 10 == 2) {
                return num + "nd";
            } else if (num % 10 == 3) {
                return num + "rd";
            } else {
                return num + "th";
            }
        } else {
            if (num == 11) {
                return "** 11th **";
            } else if (num == 12) {
                return "** 12th **";
            } else if (num == 13) {
                return "** 13th **";
            } else if (num % 10 == 1) {
                return "** " + num + "st **";
            } else if (num % 10 == 2) {
                return "** " + num + "nd **";
            } else if (num % 10 == 3) {
                return "** " + num + "rd **";
            } else {
                return "** " + num + "th **";
            }
        }
    }

    /**
     * Gets the number of games played so far
     * @return number of games played
     */
    public int numGames() {
        if ( wins + losses > 0 ) {
            return wins + losses;
        } else return 1;
    }

    public String getStrAbbrWL() {
        return abbr + " (" + wins + "-" + losses + ")";
    }

    public String getStrAbbrWL_2Lines() {
        return abbr + "\n(" + wins + "-" + losses + ")";
    }

    /**
     * Gets the number of in-conference wins, used for CCG rankings
     * @return number of in-conf wins
     */
    public int getConfWins() {
        int confWins = 0;
        for (int i = 0; i < gameWLSchedule.size(); ++i) {
            Game g = gameSchedule.get(i);
            if (g == null || "BYE".equals(gameWLSchedule.get(i))) {
                continue;
            }
            if (isConferenceGame(g)) {
                if (g.homeTeam == this && g.homeScore > g.awayScore) {
                    confWins++;
                } else if (g.awayTeam == this && g.homeScore < g.awayScore) {
                    confWins++;
                }
            }
        }
        return confWins;
    }

    /**
     * Gets the number of in-conference losses, used for CCG rankings
     * @return number of in-conf losses
     */
    public int getConfLosses() {
        int confLosses = 0;
        for (int i = 0; i < gameWLSchedule.size(); ++i) {
            Game g = gameSchedule.get(i);
            if (g == null || "BYE".equals(gameWLSchedule.get(i))) {
                continue;
            }
            if (isConferenceGame(g)) {
                if (g.homeTeam == this && g.homeScore < g.awayScore) {
                    confLosses++;
                } else if (g.awayTeam == this && g.homeScore > g.awayScore) {
                    confLosses++;
                }
            }
        }
        return confLosses;
    }

    private boolean isConferenceGame(Game game) {
        if (game == null) {
            return false;
        }
        Team opponent = game.homeTeam == this ? game.awayTeam : game.homeTeam;
        return opponent.conference.equals(conference)
                && (game.gameName.equals("In Conf")
                || game.gameName.equals("Rivalry Game"));
    }

    /**
     * Str rep of team, no bowl results
     * @return ranking abbr (w-l)
     */
    public String strRep() {
        return "#" + rankTeamPollScore + " " + abbr + " (" + wins + "-" + losses + ")";
    }

    /**
     * Str rep of team, with bowl results
     * @return ranking abbr (w-l) BW
     */
    public String strRepWithBowlResults() {
        return "#" + rankTeamPollScore + " " + abbr + " (" + wins + "-" + losses + ") " + confChampion + " " + semiFinalWL + natChampWL;
    }

    /**
     * String representation of team with program power.
     * @return ranking abbr (Pres: XX)
     */
    public String strRepWithProgramPower() {
        return "#" + rankTeamPollScore + " " + abbr
                + " (Power: " + programProfile.programPower + ")";
    }

    /**
     * Get what happened during the week for the team
     * @return name W/L gameSum, new poll rank #1
     */
    public String weekSummaryStr() {
        int i = -1;
        for (int week = gameWLSchedule.size() - 1; week >= 0; week--) {
            if (!"BYE".equals(gameWLSchedule.get(week))) {
                i = week;
                break;
            }
        }
        if (i < 0 || gameSchedule.get(i) == null) {
            return name + " bye week\nNew poll rank: #" + rankTeamPollScore + " " + abbr
                    + " (" + wins + "-" + losses + ")";
        }
        Game g = gameSchedule.get(i);
        String gameSummary = gameWLSchedule.get(i) + " " + gameSummaryStr(g);
        String rivalryGameStr = "";
        if (g.gameName.equals("Rivalry Game") || g.gameName.equals("OOC Rivalry")) {
            int strength = g.rivalryStrength();
            String label = strength > 0
                    ? Rivalry.band(strength) + " (" + strength + ") "
                    : "";
            if ( gameWLSchedule.get(i).equals("W") ) rivalryGameStr = "Won against " + label + "Rival!\n";
            else rivalryGameStr = "Lost against " + label + "Rival!\n";
        }
        return rivalryGameStr + name + " " + gameSummary + "\nNew poll rank: #" + rankTeamPollScore + " " + abbr + " (" + wins + "-" + losses + ")";
    }

    /**
     * Gets the one-line summary of a game
     * @param g Game to get summary from
     * @return 31 - 43 @ GEO #60
     */
    public String gameSummaryStr(Game g) {
        if (g.homeTeam == this) {
            return g.homeScore + " - " + g.awayScore + " vs " + g.awayTeam.abbr + " #" + g.awayTeam.rankTeamPollScore;
        } else {
            return g.awayScore + " - " + g.homeScore + " @ " + g.homeTeam.abbr + " #" + g.homeTeam.rankTeamPollScore;
        }
    }

    /**
     * Get just the score of the game
     * @param g Game to get score from
     * @return "myTeamScore - otherTeamScore"
     */
    public String gameSummaryStrScore(Game g) {
        if (g.homeTeam == this) {
            return g.homeScore + " - " + g.awayScore;
        } else {
            return g.awayScore + " - " + g.homeScore;
        }
    }

    /**
     * Get the vs/@ part of the game summary
     * @param g Game to get from
     * @return vs OPP #45
     */
    public String gameSummaryStrOpponent(Game g) {
        if (g.homeTeam == this) {
            return "vs " + g.awayTeam.abbr + " #" + g.awayTeam.rankTeamPollScore;
        } else {
            return "@ " + g.homeTeam.abbr + " #" + g.homeTeam.rankTeamPollScore;
        }
    }

    /**
     * Get String of who all is graduating from the team
     * @return string of everyone who is graduating sorted by position
     */
    public String getGraduatingPlayersStr() {
        StringBuilder sb = new StringBuilder();
        for (Player p : playersLeaving) {
            sb.append(p.getPosNameYrOvrPot_OneLine() +"\n");
        }
        return sb.toString();
    }

    public String[] getGradPlayersList() {
        String[] playersLeavingList = new String[playersLeaving.size()];
        for (int i = 0; i < playersLeavingList.length; ++i) {
            playersLeavingList[i] = playersLeaving.get(i).getPosNameYrOvrPot_Str();
        }
        return playersLeavingList;
    }

    /**
     * Get string of the current team needs (not used anymore?)
     * @return String of all the position needs
     */
    public String getTeamNeeds() {
        StringBuilder needs = new StringBuilder();
        needs.append("\t\t"+(2-teamQBs.size())+ "QBs, ");
        needs.append((4-teamRBs.size())+ "RBs, ");
        needs.append((6-teamWRs.size())+ "WRs, ");
        needs.append((2-teamKs.size())+ "Ks\n");
        needs.append("\t\t"+(10-teamOLs.size())+ "OLs, ");
        needs.append((2-teamSs.size())+ "Ss, ");
        needs.append((6-teamCBs.size())+ "CBs, ");
        needs.append((NilMoney.SUG_FB-teamFBs.size())+ "FBs, ");
        needs.append((NilMoney.SUG_TE-teamTEs.size())+ "TEs, ");
        needs.append((NilMoney.SUG_EDGE-teamEDGEs.size())+ "EDGE, ");
        needs.append((NilMoney.SUG_DL-teamDLs.size())+ "DL, ");
        needs.append((NilMoney.SUG_LB-teamLBs.size())+ "LB");
        return needs.toString();
    }

    public Player[] getQBRecruits() {
        Player[] recruits = new Player[numRecruits];
        int stars;
        for (int i = 0; i < numRecruits; ++i) {
            stars = (int)(5*(float)(numRecruits - i/2)/numRecruits);
            recruits[i] = PlayerFactory.fromStars(PositionGroup.QB, league.getRandName(), 1, stars, this, new java.util.Random());
        }
        Arrays.sort(recruits, new PlayerComparator());
        return recruits;
    }

    public Player[] getRBRecruits() {
        int numRBrecruits = 2*numRecruits;
        Player[] recruits = new Player[numRBrecruits];
        int stars;
        for (int i = 0; i < numRBrecruits; ++i) {
            stars = (int)(5*(float)(numRBrecruits - i/2)/numRBrecruits);
            recruits[i] = PlayerFactory.fromStars(PositionGroup.RB, league.getRandName(), 1, stars, this, new java.util.Random());
        }
        Arrays.sort(recruits, new PlayerComparator());
        return recruits;
    }

    public Player[] getWRRecruits() {
        int adjNumRecruits = 2*numRecruits;
        Player[] recruits = new Player[adjNumRecruits];
        int stars;
        for (int i = 0; i < adjNumRecruits; ++i) {
            stars = (int)(5*(float)(adjNumRecruits - i/2)/adjNumRecruits);
            recruits[i] = PlayerFactory.fromStars(PositionGroup.WR, league.getRandName(), 1, stars, this, new java.util.Random());
        }
        Arrays.sort(recruits, new PlayerComparator());
        return recruits;
    }

    public Player[] getOLRecruits() {
        int adjNumRecruits = 3*numRecruits;
        Player[] recruits = new Player[adjNumRecruits];
        int stars;
        for (int i = 0; i < adjNumRecruits; ++i) {
            stars = (int)(5*(float)(adjNumRecruits - i/2)/adjNumRecruits);
            recruits[i] = PlayerFactory.fromStars(PositionGroup.OL, league.getRandName(), 1, stars, this, new java.util.Random());
        }
        Arrays.sort(recruits, new PlayerComparator());
        return recruits;
    }

    public Player[] getPRecruits() {
        int adjNumRecruits = numRecruits;
        Player[] recruits = new Player[adjNumRecruits];
        int stars;
        for (int i = 0; i < adjNumRecruits; ++i) {
            stars = (int)(5*(float)(adjNumRecruits - i/2)/adjNumRecruits);
            recruits[i] = PlayerFactory.fromStars(PositionGroup.P, league.getRandName(), 1, stars, this, new java.util.Random());
        }
        Arrays.sort(recruits, new PlayerComparator());
        return recruits;
    }

    public Player[] getKRecruits() {
        int adjNumRecruits = numRecruits;
        Player[] recruits = new Player[adjNumRecruits];
        int stars;
        for (int i = 0; i < adjNumRecruits; ++i) {
            stars = (int)(5*(float)(adjNumRecruits - i/2)/adjNumRecruits);
            recruits[i] = PlayerFactory.fromStars(PositionGroup.K, league.getRandName(), 1, stars, this, new java.util.Random());
        }
        Arrays.sort(recruits, new PlayerComparator());
        return recruits;
    }

    public Player[] getSRecruits() {
        int adjNumRecruits = numRecruits;
        Player[] recruits = new Player[adjNumRecruits];
        int stars;
        for (int i = 0; i < adjNumRecruits; ++i) {
            stars = (int)(5*(float)(adjNumRecruits - i/2)/adjNumRecruits);
            recruits[i] = PlayerFactory.fromStars(PositionGroup.S, league.getRandName(), 1, stars, this, new java.util.Random());
        }
        Arrays.sort(recruits, new PlayerComparator());
        return recruits;
    }

    public Player[] getCBRecruits() {
        int adjNumRecruits = 2*numRecruits;
        Player[] recruits = new Player[adjNumRecruits];
        int stars;
        for (int i = 0; i < adjNumRecruits; ++i) {
            stars = (int)(5*(float)(adjNumRecruits - i/2)/adjNumRecruits);
            recruits[i] = PlayerFactory.fromStars(PositionGroup.CB, league.getRandName(), 1, stars, this, new java.util.Random());
        }
        Arrays.sort(recruits, new PlayerComparator());
        return recruits;
    }

    public Player[] getFBRecruits() {
        int adjNumRecruits = numRecruits;
        Player[] recruits = new Player[adjNumRecruits];
        int stars;
        for (int i = 0; i < adjNumRecruits; ++i) {
            stars = (int)(5*(float)(adjNumRecruits - i/2)/adjNumRecruits);
            recruits[i] = PlayerFactory.fromStars(PositionGroup.FB, league.getRandName(), 1, stars, this, new java.util.Random());
        }
        Arrays.sort(recruits, new PlayerComparator());
        return recruits;
    }

    public Player[] getTERecruits() {
        int adjNumRecruits = numRecruits;
        Player[] recruits = new Player[adjNumRecruits];
        int stars;
        for (int i = 0; i < adjNumRecruits; ++i) {
            stars = (int)(5*(float)(adjNumRecruits - i/2)/adjNumRecruits);
            recruits[i] = PlayerFactory.fromStars(PositionGroup.TE, league.getRandName(), 1, stars, this, new java.util.Random());
        }
        Arrays.sort(recruits, new PlayerComparator());
        return recruits;
    }

    public Player[] getEDGERecruits() {
        int adjNumRecruits = 2*numRecruits;
        Player[] recruits = new Player[adjNumRecruits];
        int stars;
        for (int i = 0; i < adjNumRecruits; ++i) {
            stars = (int)(5*(float)(adjNumRecruits - i/2)/adjNumRecruits);
            recruits[i] = PlayerFactory.fromStars(PositionGroup.EDGE, league.getRandName(), 1, stars, this, new java.util.Random());
        }
        Arrays.sort(recruits, new PlayerComparator());
        return recruits;
    }

    public Player[] getDLRecruits() {
        int adjNumRecruits = 2*numRecruits;
        Player[] recruits = new Player[adjNumRecruits];
        int stars;
        for (int i = 0; i < adjNumRecruits; ++i) {
            stars = (int)(5*(float)(adjNumRecruits - i/2)/adjNumRecruits);
            recruits[i] = PlayerFactory.fromStars(PositionGroup.DL, league.getRandName(), 1, stars, this, new java.util.Random());
        }
        Arrays.sort(recruits, new PlayerComparator());
        return recruits;
    }

    public Player[] getLBRecruits() {
        int adjNumRecruits = 2*numRecruits;
        Player[] recruits = new Player[adjNumRecruits];
        int stars;
        for (int i = 0; i < adjNumRecruits; ++i) {
            stars = (int)(5*(float)(adjNumRecruits - i/2)/adjNumRecruits);
            recruits[i] = PlayerFactory.fromStars(PositionGroup.LB, league.getRandName(), 1, stars, this, new java.util.Random());
        }
        Arrays.sort(recruits, new PlayerComparator());
        return recruits;
    }

    /**
     * Save all the recruits into a string to be used by RecruitingActivity
     * @return String of all the recruits
     */
    public String getRecruitsInfoSaveFile() {
        StringBuilder sb = new StringBuilder();
        for (String pos : NilMoney.POSITIONS) {
            Player[] recruits;
            switch (pos) {
                case "QB": recruits = getQBRecruits(); break;
                case "RB": recruits = getRBRecruits(); break;
                case "FB": recruits = getFBRecruits(); break;
                case "WR": recruits = getWRRecruits(); break;
                case "TE": recruits = getTERecruits(); break;
                case "OL": recruits = getOLRecruits(); break;
                case "K": recruits = getKRecruits(); break;
                case "P": recruits = getPRecruits(); break;
                case "S": recruits = getSRecruits(); break;
                case "CB": recruits = getCBRecruits(); break;
                case "EDGE": recruits = getEDGERecruits(); break;
                case "DL": recruits = getDLRecruits(); break;
                case "LB": recruits = getLBRecruits(); break;
                default: continue;
            }
            for (Player p : recruits) {
                sb.append(p.position).append(',').append(p.name).append(',').append(p.year).append(',')
                        .append(p.ratings.toCsvSegment()).append(',').append(p.ratOvr).append(',')
                        .append(p.cost).append("%\n");
            }
        }
        return sb.toString();
    }

    public String getPlayerInfoSaveFile() {
        StringBuilder sb = new StringBuilder();
        for (Player p : getAllPlayers()) {
            sb.append(playerToSaveLine(p)).append("%\n");
        }
        return sb.toString();
    }

    private String playerSaveExtras(Player p) {
        return "," + p.rosterStatusSave()
                + PlayerSaveCodec.seasonSuffix(p)
                + p.careerSeasonsSaveSuffix();
    }

    private void applyLoadedPlayerExtras(
            Player p, String[] playerInfo, String histSuffix, String seasonSuffix) {
        if (p == null) return;
        // Last CSV field may be rosterStatusSave()
        if (playerInfo.length > 0) {
            String last = playerInfo[playerInfo.length - 1];
            if (last.contains(":") && (last.startsWith("PWO") || last.startsWith("SCHOLARSHIP"))) {
                p.loadRosterStatus(last);
            } else if (p.rosterStatus == null) {
                p.applyOffer(RosterStatus.SCHOLARSHIP, 0);
            }
        }
        if (seasonSuffix != null && !seasonSuffix.isEmpty()) {
            PlayerSaveCodec.loadSeasonFromSuffix(p, seasonSuffix);
            if (p.isInjured) {
                if (playersInjuredAll == null) {
                    playersInjuredAll = new ArrayList<>();
                }
                if (!playersInjuredAll.contains(p)) {
                    playersInjuredAll.add(p);
                }
            }
        }
        if (histSuffix != null && !histSuffix.isEmpty()) {
            p.loadCareerSeasonsFromSuffix(histSuffix);
        }
    }

    /**
     * Replace the full depth chart for a position with the given order.
     * Index 0 is the top of the chart (QB1 / starter, etc.). Order is preserved.
     */
    public void setDepthChart(ArrayList<Player> ordered, int position) {
        if (ordered == null) return;
        switch (position) {
            case 0:
                teamQBs.clear();
                for (Player p : ordered) teamQBs.add(p);
                break;
            case 1:
                teamRBs.clear();
                for (Player p : ordered) teamRBs.add(p);
                break;
            case 2:
                teamFBs.clear();
                for (Player p : ordered) teamFBs.add(p);
                break;
            case 3:
                teamWRs.clear();
                for (Player p : ordered) teamWRs.add(p);
                break;
            case 4:
                teamTEs.clear();
                for (Player p : ordered) teamTEs.add(p);
                break;
            case 5:
                teamOLs.clear();
                for (Player p : ordered) teamOLs.add(p);
                break;
            case 6:
                teamKs.clear();
                for (Player p : ordered) teamKs.add(p);
                break;
            case 7:
                teamPs.clear();
                for (Player p : ordered) teamPs.add(p);
                break;
            case 8:
                teamSs.clear();
                for (Player p : ordered) teamSs.add(p);
                break;
            case 9:
                teamCBs.clear();
                for (Player p : ordered) teamCBs.add(p);
                break;
            case 10:
                teamEDGEs.clear();
                for (Player p : ordered) teamEDGEs.add(p);
                break;
            case 11:
                teamDLs.clear();
                for (Player p : ordered) teamDLs.add(p);
                break;
            case 12:
                teamLBs.clear();
                for (Player p : ordered) teamLBs.add(p);
                break;
            default:
                return;
        }
        if (league != null) league.setTeamRanks();
    }

    public ArrayList<? extends Player> positionList(int position) {
        switch (position) {
            case 0: return teamQBs;
            case 1: return teamRBs;
            case 2: return teamFBs;
            case 3: return teamWRs;
            case 4: return teamTEs;
            case 5: return teamOLs;
            case 6: return teamKs;
            case 7: return teamPs;
            case 8: return teamSs;
            case 9: return teamCBs;
            case 10: return teamEDGEs;
            case 11: return teamDLs;
            case 12: return teamLBs;
            default: return null;
        }
    }

    public static int starterCountForPosition(int position) {
        switch (position) {
            case 0: return 1;
            case 1: return 2;
            case 2: return 1;
            case 3: return 3;
            case 4: return 1;
            case 5: return 5;
            case 6: return 1;
            case 7: return 1;
            case 8: return 3;
            case 9: return 2;
            case 10: return 3;
            case 11: return 3;
            default: return 1;
        }
    }

    /** Lock or unlock every starter (first N) or every bench player at a position. */
    public void setDepthLocks(int position, boolean starters, boolean locked) {
        ArrayList<? extends Player> list = positionList(position);
        if (list == null) return;
        int starterCount = starterCountForPosition(position);
        for (int i = 0; i < list.size(); i++) {
            boolean isStarter = i < starterCount;
            if (isStarter == starters) {
                list.get(i).depthLocked = locked;
            }
        }
    }

    /**
     * Add one gamePlayed to all the starters.
     * The number of games played affects how much players improve.
     */
    public void addGamePlayedPlayers(boolean wonGame) {
        addGamePlayedList(teamQBs, 1, wonGame);
        addGamePlayedList(teamRBs, 2, wonGame);
        addGamePlayedList(teamFBs, 1, wonGame);
        addGamePlayedList(teamWRs, 3, wonGame);
        addGamePlayedList(teamTEs, 1, wonGame);
        addGamePlayedList(teamOLs, 5, wonGame);
        addGamePlayedList(teamKs, 1, wonGame);
        addGamePlayedList(teamPs, 1, wonGame);
        addGamePlayedList(teamSs, 1, wonGame);
        addGamePlayedList(teamCBs, 3, wonGame);
        addGamePlayedList(teamEDGEs, 2, wonGame);
        addGamePlayedList(teamDLs, 3, wonGame);
        addGamePlayedList(teamLBs, 3, wonGame);
    }

    private void addGamePlayedList(ArrayList<? extends Player> playerList, int starters, boolean wonGame) {
        for (int i = 0; i < starters; ++i) {
            playerList.get(i).gamesPlayed++;
            if (wonGame) playerList.get(i).statsWins++;
        }
    }
}

/**
 * Comparator used to sort players by overall
 */
class PlayerComparator implements Comparator<Player> {
    @Override
    public int compare( Player a, Player b ) {
        if (!a.isInjured && !b.isInjured) {
            // If both players aren't injured, sort by overall then potential
            if (a.ratOvr > b.ratOvr) return -1;
            else if (a.ratOvr == b.ratOvr)
                return a.ratPot > b.ratPot ? -1 : a.ratPot == b.ratPot ? 0 : 1;
            else return 1;
        } else if (!a.isInjured) {
            return -1;
        } else if (!b.isInjured) {
            return  1;
        } else {
            return a.ratOvr > b.ratOvr ? -1 : a.ratOvr == b.ratOvr ? 0 : 1;
        }
    }
}

/**
 * Comparator used to sort players by position group order
 */
class PlayerPositionComparator implements Comparator<Player> {
    @Override
    public int compare( Player a, Player b ) {
        int aPos = Player.getPosNumber(a.position);
        int bPos = Player.getPosNumber(b.position);
        return aPos < bPos ? -1 : aPos == bPos ? 0 : 1;
    }
}
