package CFBsimPack;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.lang.StringBuilder;


/**
 * A conference with a variable number of member teams.
 * @author Achi
 */
public class Conference {

    public String confName;
    public int mediaShare;

    public ArrayList<Team> confTeams;
    public boolean evenYear;
    public final boolean hasChampionship;

    public League league;

    private Game ccg;

    public int week;
    public int robinWeek;

    public String allConfStr;
    public ArrayList<Player> allConfPlayers;

    /**
     * Sets up Conference with empty list of teams.
     * @param name
     * @param league
     */
    public Conference( String name, League league ) {
        this(name, league, !"Independents".equals(name));
    }

    public Conference(String name, League league, boolean hasChampionship) {
        confName = name;
        mediaShare = mediaShareFor(name);
        confTeams = new ArrayList<Team>();
        this.league = league;
        this.hasChampionship = hasChampionship;
        week = 0;
        robinWeek = 0;
        allConfPlayers = new ArrayList<Player>();
    }

    public static int mediaShareFor(String conferenceName) {
        if (conferenceName == null) return 50;
        switch (conferenceName) {
            case "SEC": return 95;
            case "Big Ten": return 94;
            case "ACC": return 83;
            case "Big 12": return 82;
            case "Pac-12": return 68;
            case "American": return 60;
            case "Mountain West": return 55;
            case "Sun Belt": return 53;
            case "MAC": return 49;
            case "Conference USA": return 47;
            case "Independents": return 65;
            default: return 50;
        }
    }

    /**
     * Sets up schedule for in-conference games using round robin scheduling.
     */
    public void setUpSchedule() {
        robinWeek = 0;
        evenYear = (league.leagueHistory.size()%2==0);
        ConferenceScheduleBuilder.schedule(this);
    }

    public void resetSeason() {
        week = 0;
        robinWeek = 0;
        ccg = null;
        allConfPlayers.clear();
    }
    
    /**
     * Plays week for each team. If CCG week, play the CCG.
     */
    public void playWeek() {
        if ( week == League.WEEK_CCG ) {
            if (hasChampionship) {
                playConfChamp();
            }
        } else {
            for ( int i = 0; i < confTeams.size(); ++i ) {
                Team team = confTeams.get(i);
                if (team.isByeWeek(week)) {
                    team.gameWLSchedule.add("BYE");
                    continue;
                }
                Game game = team.gameSchedule.get(week);
                if (game != null) {
                    game.playGame();
                }
            }
            if (week == League.REGULAR_SEASON_WEEKS - 1 && hasChampionship) {
                schedConfChamp();
            }
            week++;
        } 
    }
    
    /**
     * Schedule the CCG based on team rankings.
     */
    public void schedConfChamp() {
        if (!hasChampionship || confTeams.size() < 2) {
            return;
        }
        // Play CCG between top 2 teams
        for ( int i = 0; i < confTeams.size(); ++i ) {
            confTeams.get(i).updatePollScore();
        }
        Collections.sort( confTeams, new TeamCompConfWins() );

        int winsFirst = confTeams.get(0).getConfWins();
        Team t = confTeams.get(0);
        int i = 0;
        ArrayList<Team> teamTB = new ArrayList<>();
        while (i < confTeams.size() && t.getConfWins() == winsFirst) {
            teamTB.add(t);
            ++i;
            if (i < confTeams.size()) {
                t = confTeams.get(i);
            }
        }
        if (teamTB.size() > 2) {
            // ugh 3 way tiebreaker
            Collections.sort(teamTB, new TeamCompPoll());
            for (int j = 0; j < teamTB.size(); ++j) {
                confTeams.set(j, teamTB.get(j));
            }

        }

        int winsSecond = confTeams.get(1).getConfWins();
        t = confTeams.get(1);
        i = 1;
        teamTB.clear();
        while (i < confTeams.size() && t.getConfWins() == winsSecond) {
            teamTB.add(t);
            ++i;
            if (i < confTeams.size()) {
                t = confTeams.get(i);
            }
        }
        if (teamTB.size() > 2) {
            // ugh 3 way tiebreaker
            Collections.sort(teamTB, new TeamCompPoll());
            for (int j = 0; j < teamTB.size(); ++j) {
                confTeams.set(1+j, teamTB.get(j));
            }

        }

        ccg = new Game ( confTeams.get(0), confTeams.get(1), confName + " CCG" );
        confTeams.get(0).gameSchedule.add(ccg);
        confTeams.get(1).gameSchedule.add(ccg);
    }
    
    /**
     * Play the CCG. Add the "CC" tag to the winner.
     */
    public void playConfChamp() {
        if (!hasChampionship || ccg == null) {
            return;
        }
        // Play CCG between top 2 teams
        ccg.playGame();
        if ( ccg.homeScore > ccg.awayScore ) {
            confTeams.get(0).confChampion = "CC";
            confTeams.get(0).totalCCs++;
            confTeams.get(1).totalCCLosses++;
        } else { 
            confTeams.get(1).confChampion = "CC"; 
            confTeams.get(1).totalCCs++;
            confTeams.get(0).totalCCLosses++;
        }
        Collections.sort(confTeams, new TeamCompPoll());
    }

    /**
     * String of who is playing in the CCG and the result if available.
     * @return conf champ summary
     */
    public String getCCGStr() {
        if (!hasChampionship) {
            return "";
        }
        if (ccg == null) {
            // Give prediction, find top 2 teams
            Team team1 = null, team2 = null;
            int score1 = 0, score2 = 0;
            for (int i = confTeams.size()-1; i >= 0; --i) { //count backwards so higher ranked teams are predicted
                Team t = confTeams.get(i);
                if (t.getConfWins() >= score1) {
                    score2 = score1;
                    score1 = t.getConfWins();
                    team2 = team1;
                    team1 = t;
                } else if (t.getConfWins() > score2) {
                    score2 = t.getConfWins();
                    team2 = t;
                }
            }
            return confName + " Conference Championship:\n\t\t" +
                    team1.strRep() + " vs " + team2.strRep();
        } else {
            if (!ccg.hasPlayed) {
                return confName + " Conference Championship:\n\t\t" +
                        ccg.homeTeam.strRep() + " vs " + ccg.awayTeam.strRep();
            } else {
                StringBuilder sb = new StringBuilder();
                Team winner, loser;
                sb.append(confName + " Conference Championship:\n");
                if (ccg.homeScore > ccg.awayScore) {
                    winner = ccg.homeTeam;
                    loser = ccg.awayTeam;
                    sb.append(winner.strRep() + " W ");
                    sb.append(ccg.homeScore + "-" + ccg.awayScore + " ");
                    sb.append("vs " + loser.strRep());
                    return sb.toString();
                } else {
                    winner = ccg.awayTeam;
                    loser = ccg.homeTeam;
                    sb.append(winner.strRep() + " W ");
                    sb.append(ccg.awayScore + "-" + ccg.homeScore + " ");
                    sb.append("@ " + loser.strRep());
                    return sb.toString();
                }
            }
        }
    }

    /**
     * Get the allConfPlayers by sorting all the conf's players by their Heisman score
     * Should be only called after week 13
     */
    public ArrayList<Player> getAllConfPlayers() {
        if (allConfPlayers.isEmpty()) {
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

            for (Team t : confTeams) {
                qbs.addAll(t.teamQBs);
                rbs.addAll(t.teamRBs);
                fbs.addAll(t.teamFBs);
                wrs.addAll(t.teamWRs);
                tes.addAll(t.teamTEs);
                ols.addAll(t.teamOLs);
                ks.addAll(t.teamKs);
                ss.addAll(t.teamSs);
                cbs.addAll(t.teamCBs);
                edges.addAll(t.teamEDGEs);
                dls.addAll(t.teamDLs);
                lbs.addAll(t.teamLBs);
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

            allConfPlayers.add(qbs.get(0));
            qbs.get(0).wonAllConference = true;
            allConfPlayers.add(rbs.get(0));
            rbs.get(0).wonAllConference = true;
            allConfPlayers.add(rbs.get(1));
            rbs.get(1).wonAllConference = true;
            if (!fbs.isEmpty()) {
                allConfPlayers.add(fbs.get(0));
                fbs.get(0).wonAllConference = true;
            }
            for (int i = 0; i < 3; ++i) {
                allConfPlayers.add(wrs.get(i));
                wrs.get(i).wonAllConference = true;
            }
            if (!tes.isEmpty()) {
                allConfPlayers.add(tes.get(0));
                tes.get(0).wonAllConference = true;
            }
            for (int i = 0; i < 5; ++i) {
                allConfPlayers.add(ols.get(i));
                ols.get(i).wonAllConference = true;
            }
            allConfPlayers.add(ks.get(0));
            ks.get(0).wonAllConference = true;
            allConfPlayers.add(ss.get(0));
            ss.get(0).wonAllConference = true;
            for (int i = 0; i < 3; ++i) {
                allConfPlayers.add(cbs.get(i));
                cbs.get(i).wonAllConference = true;
            }
            for (int i = 0; i < 2; ++i) {
                allConfPlayers.add(edges.get(i));
                edges.get(i).wonAllConference = true;
            }
            for (int i = 0; i < 3; ++i) {
                allConfPlayers.add(dls.get(i));
                dls.get(i).wonAllConference = true;
            }
            for (int i = 0; i < 3; ++i) {
                allConfPlayers.add(lbs.get(i));
                lbs.get(i).wonAllConference = true;
            }
        }

        return allConfPlayers;
    }
   
}

class TeamCompConfWins implements Comparator<Team> {
    @Override
    public int compare( Team a, Team b ) {
        if (a.confChampion.equals("CC")) return -1;
        else if (b.confChampion.equals("CC")) return 1;
        else if (a.getConfWins() > b.getConfWins()) {
            return -1;
        } else if (a.getConfWins() == b.getConfWins()) {
            //check for h2h tiebreaker
            if (a.gameWinsAgainst.contains(b)) {
                return -1;
            } else if (b.gameWinsAgainst.contains(a)) {
                return 1;
            } else {
                return 0;
            }
        } else {
            return 1;
        }
    }
}
