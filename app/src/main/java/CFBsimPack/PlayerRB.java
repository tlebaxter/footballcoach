package CFBsimPack;

import java.util.ArrayList;
import java.util.Random;

public class PlayerRB extends Player {

    public int ratRushPow;
    public int ratRushSpd;
    public int ratRushEva;
    public int statsRushAtt;
    public int statsRushYards;
    public int statsTD;
    public int statsFumbles;
    public int careerRushAtt;
    public int careerRushYards;
    public int careerTDs;
    public int careerFumbles;

    public PlayerRB() {
        position = "RB";
    }

    public PlayerRB(String nm, Team t, int yr, PlayerRatings bag, boolean rs) {
        this();
        name = nm;
        team = t;
        year = yr;
        isRedshirt = rs;
        applyRatings(bag);
        recomputeCost(new Random(nm != null ? nm.hashCode() : 0));
    }

    public PlayerRB(String nm, Team t, int yr, int pot, int iq, int s1, int s2, int s3, boolean rs, int dur) {
        this();
        name = nm;
        team = t;
        year = yr;
        isRedshirt = rs;
        PlayerRatings bag = PlayerFactory.rollRatings(PositionGroup.RB, yr, 3, new Random(nm.hashCode()));
        bag.pot = pot;
        bag.footIq = iq;
        bag.dur = dur;
        bag.stre = s1;
        bag.spd = s2;
        bag.elu = s3;
        applyRatings(bag);
        recomputeCost(new Random());
    }

    public PlayerRB(String nm, Team t, int yr, int pot, int iq, int s1, int s2, int s3, boolean rs, int dur,
                    int cGamesPlayed, int cHeismans, int cAA, int cAC, int cWins) {
        this(nm, t, yr, pot, iq, s1, s2, s3, rs, dur);
        careerGamesPlayed = cGamesPlayed;
        careerHeismans = cHeismans;
        careerAllAmerican = cAA;
        careerAllConference = cAC;
        careerWins = cWins;
    }

    public PlayerRB(String nm, int yr, int stars, Team t) {
        this();
        Player p = PlayerFactory.fromStars(PositionGroup.RB, nm, yr, stars, t, new Random());
        name = p.name;
        team = p.team;
        year = p.year;
        applyRatings(p.ratings);
        cost = p.cost;
        position = "RB";
    }

    @Override
    protected void syncLegacySkillsFromRatings() {
        ratRushPow = ratings.stre;
        ratRushSpd = ratings.spd;
        ratRushEva = ratings.elu;
    }

    @Override
    protected double costDivisor() { return 3.0; }

    @Override
    protected int costBase() { return 100; }

    @Override
    protected void bankPositionCareerStats() {
        careerRushAtt += statsRushAtt;
        statsRushAtt = 0;
        careerRushYards += statsRushYards;
        statsRushYards = 0;
        careerTDs += statsTD;
        statsTD = 0;
        careerFumbles += statsFumbles;
        statsFumbles = 0;
        super.bankPositionCareerStats();
    }

    @Override
    public int getHeismanScore() {
        return statsTD * 120 + statsRushYards;
    }

    @Override
    public ArrayList<String> getDetailStatsList(int games) {
        ArrayList<String> pStats = new ArrayList<>();
        pStats.add("Rush Yards: " + statsRushYards + " yds>Yds/Att: " + ((double)(10*statsRushYards/(statsRushAtt+1))/10) + " yds");
        pStats.add("TDs: " + statsTD + ">Fumbles: " + statsFumbles);
        pStats.add("Games: " + gamesPlayed + " (" + statsWins + "-" + (gamesPlayed-statsWins) + ")> ");
        pStats.addAll(getRatingsDetailLines());
        return pStats;
    }
}
