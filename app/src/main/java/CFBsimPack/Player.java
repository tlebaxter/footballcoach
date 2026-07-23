package CFBsimPack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

/**
 * Base player class that others extend. Has name, overall, potential, and football IQ.
 * @author Achi
 */
public class Player {
    
    public Team team;
    public String name;
    public String position;
    public int year;
    public int ratOvr;
    public int ratPot;
    public int ratFootIQ;
    public int ratDur;
    public int ratImprovement;
    public int cost;

    public int gamesPlayed;
    public int statsWins;
    public boolean wonHeisman;
    public boolean wonAllAmerican;
    public boolean wonAllConference;

    public int careerGamesPlayed;
    public int careerHeismans;
    public int careerAllAmerican;
    public int careerAllConference;
    public int careerWins;

    public boolean isRedshirt;

    public boolean isInjured;
    public Injury injury;

    /**
     * When true, this player keeps their depth-chart slot through auto-sorts
     * (unlocked teammates still sort around them). Injury still bumps them down.
     */
    public boolean depthLocked;

    public RosterStatus rosterStatus = RosterStatus.SCHOLARSHIP;
    public int nilDealAmount;
    /** Original signed length (1–4). */
    public int contractLength;
    /** Future seasons still owed after the installment already paid this offseason. */
    public int contractYearsRemaining;
    public boolean retainedThisOffseason;
    /** 1–7 projected round; 0 = UDFA / not a draft prospect. */
    public int projectedDraftRound;
    public boolean draftDeclared;
    public ArrayList<PlayerSeasonRecord> careerSeasons = new ArrayList<>();
    public TransferReason transferReason;
    public String transferReasonText;
    public Team priorTeam;
    public int portalRiskTier; // 0 safe, 1-3 at risk

    protected final String[] letterGrades = {"F", "F+", "D", "D+", "C", "C+", "B", "B+", "A", "A+"};
    
    public Vector ratingsVector;

    public void recordSeasonSnapshot() {
        if (team == null || team.league == null) return;
        careerSeasons.add(new PlayerSeasonRecord(this, team.league.getYear()));
    }

    public void applyOffer(RosterStatus status, int nilAmount) {
        applyOffer(status, nilAmount, 1);
    }

    /**
     * Apply roster status + annual NIL and multi-year length.
     * Caller pays the year-1 installment; remaining years are future encumbrance.
     */
    public void applyOffer(RosterStatus status, int nilAmount, int years) {
        this.rosterStatus = status != null ? status : RosterStatus.SCHOLARSHIP;
        this.nilDealAmount = (this.rosterStatus == RosterStatus.SCHOLARSHIP_PLUS_NIL) ? Math.max(0, nilAmount) : 0;
        int y = Math.max(1, years);
        int max = ProgramOffers.maxContractYears(this);
        if (y > max) y = max;
        this.contractLength = y;
        this.contractYearsRemaining = Math.max(0, y - 1);
        this.retainedThisOffseason = true;
        this.draftDeclared = false;
    }

    public void clearContract() {
        rosterStatus = RosterStatus.PWO;
        nilDealAmount = 0;
        contractLength = 0;
        contractYearsRemaining = 0;
        retainedThisOffseason = false;
    }

    public boolean needsDealRenewal() {
        if (year >= 5) return false;
        if (rosterStatus == null || rosterStatus == RosterStatus.PWO) return false;
        return contractYearsRemaining <= 0;
    }

    public int annualDealCash(int prestige) {
        return NilMoney.offerCashCost(rosterStatus, nilDealAmount, prestige);
    }

    /** Future-year encumbrance from the recruiting purse (NIL only — COA is year-1). */
    public int futureNilCommitment() {
        if (rosterStatus != RosterStatus.SCHOLARSHIP_PLUS_NIL) return 0;
        return Math.max(0, nilDealAmount);
    }

    public String schoolsAttendedSummary() {
        Map<String, int[]> ranges = new LinkedHashMap<>();
        for (PlayerSeasonRecord r : careerSeasons) {
            String key = r.teamAbbr;
            int[] range = ranges.get(key);
            if (range == null) {
                ranges.put(key, new int[]{r.seasonYear, r.seasonYear});
            } else {
                if (r.seasonYear < range[0]) range[0] = r.seasonYear;
                if (r.seasonYear > range[1]) range[1] = r.seasonYear;
            }
        }
        if (team != null && !ranges.containsKey(team.abbr)) {
            int y = team.league != null ? team.league.getYear() : 0;
            ranges.put(team.abbr, new int[]{y, y});
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, int[]> e : ranges.entrySet()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(e.getKey()).append(" (").append(e.getValue()[0]);
            if (e.getValue()[0] != e.getValue()[1]) sb.append("-").append(e.getValue()[1]);
            sb.append(")");
        }
        return sb.length() == 0 ? (team != null ? team.abbr : "—") : sb.toString();
    }

    public String careerSeasonsSaveSuffix() {
        if (careerSeasons == null || careerSeasons.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("|HIST");
        for (PlayerSeasonRecord r : careerSeasons) {
            sb.append("|").append(r.toSaveToken());
        }
        return sb.toString();
    }

    public void loadCareerSeasonsFromSuffix(String field) {
        careerSeasons = new ArrayList<>();
        if (field == null || !field.startsWith("|HIST")) return;
        String[] parts = field.split("\\|");
        for (int i = 2; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            PlayerSeasonRecord r = PlayerSeasonRecord.fromSaveToken(parts[i]);
            if (r != null) careerSeasons.add(r);
        }
    }

    public String rosterStatusSave() {
        return rosterStatus.name() + ":" + nilDealAmount + ":" + contractYearsRemaining + ":"
                + contractLength + ":" + (retainedThisOffseason ? "1" : "0")
                + ":" + (depthLocked ? "1" : "0");
    }

    public void loadRosterStatus(String field) {
        if (field == null || field.isEmpty()) {
            rosterStatus = RosterStatus.SCHOLARSHIP;
            nilDealAmount = 0;
            contractYearsRemaining = 0;
            contractLength = 1;
            retainedThisOffseason = false;
            depthLocked = false;
            return;
        }
        String[] p = field.split(":");
        rosterStatus = RosterStatus.fromString(p[0]);
        nilDealAmount = 0;
        contractYearsRemaining = 0;
        contractLength = 1;
        retainedThisOffseason = false;
        depthLocked = false;
        if (p.length > 1) {
            try {
                nilDealAmount = Integer.parseInt(p[1]);
            } catch (Exception e) {
                nilDealAmount = 0;
            }
        }
        if (p.length > 2) {
            try {
                contractYearsRemaining = Integer.parseInt(p[2]);
            } catch (Exception e) {
                contractYearsRemaining = 0;
            }
        }
        if (p.length > 3) {
            try {
                contractLength = Integer.parseInt(p[3]);
            } catch (Exception e) {
                contractLength = Math.max(1, contractYearsRemaining + 1);
            }
        }
        if (p.length > 4) {
            retainedThisOffseason = "1".equals(p[4]);
        }
        if (p.length > 5) {
            depthLocked = "1".equals(p[5]);
        }
    }
    
    public String getYrStr() {
        if ( year == 1 ) {
            return "Fr";
        } else if ( year == 2 ) {
            return "So";
        } else if ( year == 3 ) {
            return "Jr";
        } else if ( year == 4 ) {
            return "Sr";
        } else if ( year == 5 ) {
            return "Grad";
        }
        return "ERROR";
    }
    
    public void advanceSeason() {
        //add stuff
        year++;
    }
    
    public int getHeismanScore() {
        int adjGames = gamesPlayed;
        if (adjGames > 10) adjGames = 10;
        return ratOvr * adjGames;
    }

    public String getInitialName() {
        String[] names = name.split(" ");
        return names[0].substring(0,1) + ". " + names[1];
    }

    public String getPosNameYrOvrPot_Str() {
        if (injury != null) {
            return "[I]" + position + " " + getInitialName() + " [" + getYrStr() + "] Ovr: " + ratOvr + ">" + injury.toString();
        }
        return position + " " + name + " [" + getYrStr() + "]>" + "Ovr: " + ratOvr + ", Pot: " + getLetterGrade(ratPot);
    }

    public String getPosNameYrOvrPot_OneLine() {
        if (injury != null) {
            return position + " " + getInitialName() + " [" + getYrStr() + "] Ovr: " + ratOvr + " " + injury.toString();
        }
        return position + " " + getInitialName() + " [" + getYrStr() + "] " + "Ovr: " + ratOvr + ", Pot: " + getLetterGrade(ratPot);
    }

    public String getPosNameYrOvr_Str() {
        return position + " " + name + " [" + getYrStr() + "] Ovr: " + ratOvr;
    }

    public String getYrOvrPot_Str() {
        return "[" + getYrStr() + "] Ovr: " + ratOvr + ", Pot: " + getLetterGrade(ratPot);
    }

    public String getPosNameYrOvrPot_NoInjury() {
        return position + " " + getInitialName() + " [" + getYrStr() + "] Ovr: " + ratOvr + ", Pot: " + getLetterGrade(ratPot);
    }

    public String getMockDraftStr() {
        return position + " " + getInitialName() + " [" + getYrStr() + "]>" + team.strRep();
    }

    /**
     * Convert a rating into a letter grade. 90 -> A, 80 -> B, etc
     */
    protected String getLetterGrade(String num) {
        int ind = (Integer.parseInt(num) - 50)/5;
        if (ind > 9) ind = 9;
        if (ind < 0) ind = 0;
        return letterGrades[ind];
    }

    /**
     * Convert a rating into a letter grade for potential, so 50 is a C instead of F
     */
    protected String getLetterGradePot(String num) {
        int ind = (Integer.parseInt(num))/10;
        if (ind > 9) ind = 9;
        if (ind < 0) ind = 0;
        return letterGrades[ind];
    }

    /**
     * Convert a rating into a letter grade. 90 -> A, 80 -> B, etc
     */
    protected String getLetterGrade(int num) {
        int ind = (num-50)/5;
        if (ind > 9) ind = 9;
        if (ind < 0) ind = 0;
        return letterGrades[ind];
    }

    /**
     * Convert a rating into a letter grade for potential, so 50 is a C instead of F
     */
    protected String getLetterGradePot(int num) {
        int ind = num/10;
        if (ind > 9) ind = 9;
        if (ind < 0) ind = 0;
        return letterGrades[ind];
    }

    public ArrayList<String> getDetailStatsList(int games) {
        return null;
    }

    public ArrayList<String> getDetailAllStatsList(int games) {
        return null;
    }

    public ArrayList<String> getCareerStatsList() {
        ArrayList<String> pStats = new ArrayList<>();
        pStats.add("Games: " + (gamesPlayed+careerGamesPlayed) + " (" + (statsWins+careerWins) + "-" + (gamesPlayed+careerGamesPlayed-(statsWins+careerWins)) + ")"
                + ">Yrs: " + getYearsPlayed());
        pStats.add("Awards: " + getAwards() + ">Status: " + rosterStatus.displayName());
        pStats.add("Schools: " + schoolsAttendedSummary() + "> ");
        return pStats;
    }

    public ArrayList<String> getYearByYearStatsList() {
        ArrayList<String> lines = new ArrayList<>();
        if (careerSeasons != null) {
            for (int i = careerSeasons.size() - 1; i >= 0; i--) {
                lines.add(careerSeasons.get(i).summaryLine());
            }
        }
        // Current season in progress
        if (team != null && team.league != null && gamesPlayed > 0) {
            PlayerSeasonRecord current = new PlayerSeasonRecord(this, team.league.getYear());
            lines.add(0, current.summaryLine() + " *");
        }
        return lines;
    }

    public String getYearsPlayed() {
        if (careerSeasons != null && !careerSeasons.isEmpty()) {
            int start = careerSeasons.get(0).seasonYear;
            int end = careerSeasons.get(careerSeasons.size() - 1).seasonYear;
            if (team != null && team.league != null) {
                end = Math.max(end, team.league.getYear());
            }
            return start + "-" + end;
        }
        if (team == null || team.league == null) return "—";
        int startYear = team.league.getYear() - year + 1;
        int endYear = team.league.getYear();
        return startYear + "-" + endYear;
    }

    public String getAwards() {
        ArrayList<String> awards = new ArrayList<>();
        int heis = careerHeismans + (wonHeisman ? 1 : 0);
        int aa = careerAllAmerican + (wonAllAmerican ? 1 : 0);
        int ac = careerAllConference + (wonAllConference ? 1 : 0);
        if (heis > 0) awards.add(heis + "x POTY");
        if (aa > 0) awards.add(aa + "x All-Amer");
        if (ac > 0) awards.add(ac + "x All-Conf");

        String awardsStr = "";
        for (int i = 0; i < awards.size(); ++i) {
            awardsStr += awards.get(i);
            if (i != awards.size()-1) awardsStr += ", ";
        }

        return awardsStr;
    }

    public String getInfoForLineup() {
        return null;
    }

    public String getInfoLineupInjury() {
        if (injury != null) {
            return getInitialName() + " [" + getYrStr() + "] " + injury.toString();
        }
        return getInitialName() + " [" + getYrStr() + "] " + "Ovr: " + ratOvr + ", Pot: " + getLetterGrade(ratPot);
    }

    public int getGamesPlayed() {
        if (gamesPlayed == 0) return 1;
        else return gamesPlayed;
    }

    public static int getPosNumber(String pos) {
        switch (pos) {
            case "QB": return 0;
            case "RB": return 1;
            case "FB": return 2;
            case "WR": return 3;
            case "TE": return 4;
            case "OL": return 5;
            case "K": return 6;
            case "S": return 7;
            case "CB": return 8;
            case "EDGE": return 9;
            case "DL": return 10;
            case "LB": return 11;
            default: return 12;
        }
    }
    
}
