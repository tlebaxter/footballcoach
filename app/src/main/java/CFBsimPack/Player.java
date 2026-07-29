package CFBsimPack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Single player type (ZenGM-style). Position is {@link #position};
 * ratings live in {@link PlayerRatings}; skill counters in {@link #seasonStats}/{@link #careerStats}.
 */
public class Player {
    
    public Team team;
    public String name;
    public String position;
    public int year;
    public PlayerRatings ratings = new PlayerRatings();
    public int ratOvr;
    public int ratPot;
    public int ratFootIQ;
    public int ratDur;
    public int ratImprovement;
    public int cost;

    public int gamesPlayed;
    public int statsWins;
    /** On-field snaps this season (offense or defense elevens). */
    public int seasonSnaps;
    public boolean wonHeisman;
    public boolean wonAllAmerican;
    public boolean wonAllConference;

    public int careerGamesPlayed;
    public int careerSnaps;
    public int careerHeismans;
    public int careerAllAmerican;
    public int careerAllConference;
    public int careerWins;

    /** Flat skill counters for the current season / career totals. */
    public final PlayerSkillStats seasonStats = new PlayerSkillStats();
    public final PlayerSkillStats careerStats = new PlayerSkillStats();

    /** Special-teams return stats (any position can return). */
    public int statsPrAtt;
    public int statsPrYards;
    public int statsPrTd;
    public int statsKrAtt;
    public int statsKrYards;
    public int statsKrTd;
    public int statsFairCatches;
    public int careerPrAtt;
    public int careerPrYards;
    public int careerPrTd;
    public int careerKrAtt;
    public int careerKrYards;
    public int careerKrTd;
    public int careerFairCatches;

    public boolean isRedshirt;

    public boolean isInjured;
    public Injury injury;
    /** Ejected for the remainder of the current game (targeting, etc.). */
    public boolean isEjected;

    /**
     * When true, this player keeps their depth-chart slot through auto-sorts
     * (unlocked teammates still sort around them). Injury still bumps them down.
     */
    public boolean depthLocked;

    /** Optional depth/system role label (EDGE/MIKE/etc.). */
    public RoleTag roleTag;

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
    public void recordSeasonSnapshot() {
        if (team == null || team.league == null) return;
        careerSeasons.add(new PlayerSeasonRecord(this, team.league.getYear()));
        bankReturnSeasonStats();
    }

    /** Roll season return stats into career and zero season counters. */
    protected void bankReturnSeasonStats() {
        careerPrAtt += statsPrAtt;
        careerPrYards += statsPrYards;
        careerPrTd += statsPrTd;
        careerKrAtt += statsKrAtt;
        careerKrYards += statsKrYards;
        careerKrTd += statsKrTd;
        careerFairCatches += statsFairCatches;
        statsPrAtt = 0;
        statsPrYards = 0;
        statsPrTd = 0;
        statsKrAtt = 0;
        statsKrYards = 0;
        statsKrTd = 0;
        statsFairCatches = 0;
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

    public int annualDealCash(ProgramProfile profile) {
        return NilMoney.offerCashCost(rosterStatus, nilDealAmount, profile);
    }

    /** Future-year encumbrance from the recruiting purse (NIL only). */
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
    
    /**
     * Apply full ratings bag and recompute primary OVR.
     */
    public void applyRatings(PlayerRatings bag) {
        this.ratings = bag != null ? bag : new PlayerRatings();
        this.ratPot = ratings.pot;
        this.ratFootIQ = ratings.footIq;
        this.ratDur = ratings.dur;
        PositionGroup g = PositionOvr.primaryGroup(this);
        this.ratOvr = PositionOvr.ovr(ratings, g);
    }

    public int ovrFor(PositionGroup pos) {
        return PositionOvr.ovr(this, pos);
    }

    public int potFor(PositionGroup pos) {
        return PositionOvr.pot(this, pos);
    }

    public PositionGroup positionGroup() {
        PositionGroup g = PositionGroup.fromToken(position);
        return g != null ? g : PositionGroup.LB;
    }

    public void recomputeCost(Random rng) {
        Random r = rng != null ? rng : new Random();
        double div = costDivisor();
        int base = costBase();
        cost = (int) (Math.pow(Math.max(0, ratOvr - 55), 2) / div) + base + (int) (r.nextDouble() * 100) - 50;
        if (cost < 1) cost = 1;
    }

    protected double costDivisor() {
        switch (positionGroup()) {
            case QB: return 1.5;
            case RB: return 3.0;
            case WR: return 3.5;
            case TE: return 5.0;
            case FB: return 6.0;
            case OL: return 5.0;
            case K: return 3.5;
            case P: return 3.5;
            case CB: return 4.5;
            case S: return 4.5;
            case DL: return 4.5;
            case EDGE: return 4.5;
            case LB: return 4.5;
            default: return 4.0;
        }
    }

    protected int costBase() {
        switch (positionGroup()) {
            case QB: return 150;
            case RB: return 100;
            case WR: return 100;
            case TE: return 50;
            case FB: return 40;
            case OL: return 50;
            case K: return 100;
            case P: return 80;
            case CB: return 50;
            case S: return 50;
            case DL: return 70;
            case EDGE: return 70;
            case LB: return 70;
            default: return 80;
        }
    }

    public String ratingsSaveCsv() {
        return ratings.toCsvSegment() + "," + ratOvr + "," + ratImprovement;
    }

    public void advanceSeason() {
        int bonus = 0;
        if (team != null && team.programProfile != null) {
            bonus = team.programProfile.developmentBonus();
        }
        Random rng = new Random((long) name.hashCode() * 31L + year * 17L + seasonSnaps);
        DevelopmentCurve.advance(this, bonus, rng);
        bankPositionCareerStats();
    }

    /** Credit one on-field snap (offense or defense). */
    public void recordSnap() {
        seasonSnaps++;
    }

    /** Bank season skill stats into career and zero season counters / awards flags. */
    protected void bankPositionCareerStats() {
        careerStats.addFrom(seasonStats);
        seasonStats.clear();
        careerGamesPlayed += gamesPlayed;
        careerSnaps += seasonSnaps;
        careerWins += statsWins;
        if (wonHeisman) careerHeismans++;
        if (wonAllAmerican) careerAllAmerican++;
        if (wonAllConference) careerAllConference++;
        gamesPlayed = 0;
        seasonSnaps = 0;
        statsWins = 0;
        wonHeisman = false;
        wonAllAmerican = false;
        wonAllConference = false;
    }
    
    public int getHeismanScore() {
        PlayerSkillStats s = seasonStats;
        switch (positionGroup()) {
            case QB:
                return s.passTd * 140 - s.passInt * 250 + s.passYards + s.rushYards + s.rushTd * 100;
            case RB:
                return s.rushTd * 120 + s.rushYards;
            case WR:
                return s.recTd * 120 + s.recYards;
            case TE:
                return s.recTd * 100 + s.recYards;
            case K:
                if (s.fgAtt <= 0) return ratOvr;
                return (int) ((s.fgMade * 5 + s.xpMade) * ((double) s.fgMade / s.fgAtt)) + ratOvr;
            case P:
                return ratOvr + s.puntYards / 20;
            case CB:
            case S:
            case DL:
            case EDGE:
            case LB:
                return s.tackles * 8 + s.tfl * 40 + s.sacksDef * 80 + s.defInt * 120
                        + s.passDef * 25 + s.forcedFumbles * 50 + s.fumbleRec * 40
                        + ratOvr * gamesPlayed / 2;
            case FB:
            case OL:
                return ratOvr * gamesPlayed;
            default: {
                int adjGames = gamesPlayed;
                if (adjGames > 10) adjGames = 10;
                return ratOvr * adjGames;
            }
        }
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
    }public String getMockDraftStr() {
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
        ArrayList<String> pStats = new ArrayList<>();
        PlayerSkillStats s = seasonStats;
        switch (positionGroup()) {
            case QB:
                pStats.add("TD/Int: " + s.passTd + "/" + s.passInt + ">Comp Percent: "
                        + (100 * s.passComp / (s.passAtt + 1)) + "%");
                pStats.add("Pass Yards: " + s.passYards + " yds>Yards/Att: "
                        + ((double) (10 * s.passYards / (s.passAtt + 1)) / 10) + " yds");
                pStats.add("Yds/Game: " + (s.passYards / getGamesPlayed()) + " yds/g>Sacks: " + s.sacked);
                if (s.rushAtt > 0) {
                    pStats.add("Rush: " + s.rushAtt + " for " + s.rushYards + ">Rush TD: " + s.rushTd);
                }
                pStats.add("Games: " + gamesPlayed + " (" + statsWins + "-" + (gamesPlayed - statsWins) + ")> ");
                break;
            case RB:
                pStats.add("Rush Yards: " + s.rushYards + " yds>Yds/Att: "
                        + ((double) (10 * s.rushYards / (s.rushAtt + 1)) / 10) + " yds");
                pStats.add("TDs: " + s.rushTd + ">Fumbles: " + s.fumbles);
                pStats.add("Games: " + gamesPlayed + " (" + statsWins + "-" + (gamesPlayed - statsWins) + ")> ");
                break;
            case WR:
                pStats.add("Rec: " + s.receptions + "/" + s.targets + ">Yards: " + s.recYards);
                pStats.add("TDs: " + s.recTd + ">Drops: " + s.drops);
                pStats.add("Games: " + gamesPlayed + " (" + statsWins + "-" + (gamesPlayed - statsWins) + ")> ");
                break;
            case TE:
                pStats.add("Rec: " + s.receptions + ">Yards: " + s.recYards);
                pStats.add("TDs: " + s.recTd + ">Games: " + gamesPlayed);
                break;
            case FB:
                pStats.add("Rush: " + s.rushAtt + " for " + s.rushYards + ">TDs: " + s.rushTd);
                pStats.add("Games: " + gamesPlayed + " (" + statsWins + "-" + (gamesPlayed - statsWins) + ")> ");
                break;
            case K:
                if (s.xpAtt > 0) {
                    pStats.add("XP Made/Att: " + s.xpMade + "/" + s.xpAtt + ">XP Percent: "
                            + (100 * s.xpMade / s.xpAtt) + "%");
                } else {
                    pStats.add("XP Made/Att: 0/0>XP Percent: 0%");
                }
                if (s.fgAtt > 0) {
                    pStats.add("FG Made/Att: " + s.fgMade + "/" + s.fgAtt + ">FG Percent: "
                            + (100 * s.fgMade / s.fgAtt) + "%");
                } else {
                    pStats.add("FG Made/Att: 0/0>FG Percent: 0%");
                }
                pStats.add("Games: " + gamesPlayed + " (" + statsWins + "-" + (gamesPlayed - statsWins) + ")> ");
                break;
            case P:
                if (s.puntAtt > 0) {
                    pStats.add("Punts: " + s.puntAtt + ">Avg: " + (s.puntYards / s.puntAtt) + " yds");
                } else {
                    pStats.add("Punts: 0>Avg: 0 yds");
                }
                pStats.add("Games: " + gamesPlayed + " (" + statsWins + "-" + (gamesPlayed - statsWins) + ")> ");
                break;
            case CB:
            case S:
            case DL:
            case EDGE:
            case LB:
                pStats.add("Tackles: " + s.tackles + ">TFL: " + s.tfl);
                pStats.add("Sacks: " + s.sacksDef + ">INT: " + s.defInt);
                pStats.add("PD: " + s.passDef + ">FF/FR: " + s.forcedFumbles + "/" + s.fumbleRec);
                pStats.add("Games: " + gamesPlayed + " (" + statsWins + "-" + (gamesPlayed - statsWins) + ")> ");
                break;
            default:
                pStats.add("Games: " + gamesPlayed + " (" + statsWins + "-" + (gamesPlayed - statsWins) + ")> ");
                break;
        }
        pStats.addAll(getRatingsDetailLines());
        return pStats;
    }

    public ArrayList<String> getCareerStatsList() {
        ArrayList<String> pStats = new ArrayList<>();
        PlayerSkillStats s = seasonStats;
        PlayerSkillStats c = careerStats;
        switch (positionGroup()) {
            case QB:
                pStats.add("TD/Int: " + (s.passTd + c.passTd) + "/" + (s.passInt + c.passInt) + ">Comp Percent: "
                        + (100 * (s.passComp + c.passComp) / (s.passAtt + c.passAtt + 1)) + "%");
                pStats.add("Pass Yards: " + (s.passYards + c.passYards) + " yds>Yards/Att: "
                        + ((double) (10 * (s.passYards + c.passYards) / (s.passAtt + c.passAtt + 1)) / 10)
                        + " yds");
                pStats.add("Yds/Game: " + ((s.passYards + c.passYards) / (getGamesPlayed() + careerGamesPlayed))
                        + " yds/g>Sacks: " + (s.sacked + c.sacked));
                break;
            case K: {
                int xpa = s.xpAtt + c.xpAtt;
                int xpm = s.xpMade + c.xpMade;
                int fga = s.fgAtt + c.fgAtt;
                int fgm = s.fgMade + c.fgMade;
                pStats.add(xpa > 0
                        ? "XP Made/Att: " + xpm + "/" + xpa + ">XP Percentage: " + (100 * xpm / xpa) + "%"
                        : "XP Made/Att: 0/0>XP Percentage: 0%");
                pStats.add(fga > 0
                        ? "FG Made/Att: " + fgm + "/" + fga + ">FG Percentage: " + (100 * fgm / fga) + "%"
                        : "FG Made/Att: 0/0>FG Percentage: 0%");
                break;
            }
            case P: {
                int att = s.puntAtt + c.puntAtt;
                int yds = s.puntYards + c.puntYards;
                pStats.add(att > 0 ? "Punts: " + att + ">Avg: " + (yds / att) + " yds" : "Punts: 0>Avg: 0 yds");
                break;
            }
            case CB:
            case S:
            case DL:
            case EDGE:
            case LB:
                pStats.add("Tackles: " + (s.tackles + c.tackles) + ">TFL: " + (s.tfl + c.tfl));
                pStats.add("Sacks: " + (s.sacksDef + c.sacksDef) + ">INT: " + (s.defInt + c.defInt));
                pStats.add("PD: " + (s.passDef + c.passDef) + ">FF/FR: "
                        + (s.forcedFumbles + c.forcedFumbles) + "/" + (s.fumbleRec + c.fumbleRec));
                break;
            default:
                break;
        }
        pStats.add("Games: " + (gamesPlayed + careerGamesPlayed) + " (" + (statsWins + careerWins) + "-"
                + (gamesPlayed + careerGamesPlayed - (statsWins + careerWins)) + ")"
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
    }public int getGamesPlayed() {
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
            case "P": return 7;
            case "S": return 8;
            case "CB": return 9;
            case "EDGE": return 10;
            case "DL": return 11;
            case "LB": return 12;
            default: return 13;
        }
    }

    public ArrayList<String> getRatingsDetailLines() {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Durability: " + getLetterGrade(ratDur) + ">Football IQ: " + getLetterGrade(ratFootIQ));
        String[] keys = primaryAttrKeys();
        for (int i = 0; i < keys.length; i += 2) {
            String a = keys[i];
            String left = PlayerRatings.displayLabel(a) + ": " + getLetterGrade(ratings.get(a));
            if (i + 1 < keys.length) {
                String b = keys[i + 1];
                lines.add(left + ">" + PlayerRatings.displayLabel(b) + ": " + getLetterGrade(ratings.get(b)));
            } else {
                lines.add(left + "> ");
            }
        }
        PositionGroup primary = PositionOvr.primaryGroup(this);
        StringBuilder secondary = new StringBuilder("Pos OVR: ");
        int shown = 0;
        for (PositionGroup g : PositionGroup.values()) {
            if (g == primary) continue;
            int o = ovrFor(g);
            if (o < 45) continue;
            if (shown > 0) secondary.append(", ");
            secondary.append(g.token).append(" ").append(o);
            if (++shown >= 3) break;
        }
        if (shown > 0) lines.add(secondary + "> ");
        return lines;
    }

    protected String[] primaryAttrKeys() {
        PositionGroup g = PositionOvr.primaryGroup(this);
        switch (g) {
            case QB: return new String[]{"tha", "thp", "thv", "elu", "spd", "bsc"};
            case RB: return new String[]{"spd", "elu", "stre", "bsc", "hnd", "rtr"};
            case FB: return new String[]{"rbk", "pbk", "stre", "bsc", "hnd", "spd"};
            case WR: return new String[]{"hnd", "rtr", "spd", "elu", "hgt", "bsc"};
            case TE: return new String[]{"hnd", "rbk", "pbk", "rtr", "hgt", "stre"};
            case OL: return new String[]{"pbk", "rbk", "stre", "hgt", "endu", "spd"};
            case EDGE: return new String[]{"prs", "spd", "stre", "tck", "rns", "hgt"};
            case DL: return new String[]{"rns", "prs", "stre", "tck", "hgt", "endu"};
            case LB: return new String[]{"tck", "rns", "pcv", "prs", "spd", "stre"};
            case CB: return new String[]{"pcv", "spd", "tck", "hgt", "endu", "stre"};
            case S: return new String[]{"pcv", "tck", "spd", "stre", "hgt", "endu"};
            case K: return new String[]{"kpw", "kac", "endu", "stre"};
            case P: return new String[]{"ppw", "pac", "endu", "stre"};
            default: return new String[]{"spd", "stre", "endu", "hgt"};
        }
    }
    
}
