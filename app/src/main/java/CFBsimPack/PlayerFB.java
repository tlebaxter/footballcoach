package CFBsimPack;

import java.util.ArrayList;
import java.util.Random;

public class PlayerFB extends Player {

    public int ratRushPow;
    public int ratBlock;
    public int ratRec;
    public int statsRushAtt;
    public int statsRushYards;
    public int statsTD;
    public int careerRushAtt;
    public int careerRushYards;
    public int careerTDs;

    public PlayerFB() {
        position = "FB";
    }

    public PlayerFB(String nm, Team t, int yr, PlayerRatings bag, boolean rs) {
        this();
        name = nm;
        team = t;
        year = yr;
        isRedshirt = rs;
        applyRatings(bag);
        recomputeCost(new Random(nm != null ? nm.hashCode() : 0));
    }

    public PlayerFB(String nm, Team t, int yr, int pot, int iq, int s1, int s2, int s3, boolean rs, int dur) {
        this();
        name = nm;
        team = t;
        year = yr;
        isRedshirt = rs;
        PlayerRatings bag = PlayerFactory.rollRatings(PositionGroup.FB, yr, 3, new Random(nm.hashCode()));
        bag.pot = pot;
        bag.footIq = iq;
        bag.dur = dur;
        bag.stre = s1;
        bag.rbk = s2;
        bag.hnd = s3;
        applyRatings(bag);
        recomputeCost(new Random());
    }

    public PlayerFB(String nm, Team t, int yr, int pot, int iq, int s1, int s2, int s3, boolean rs, int dur,
                    int cGamesPlayed, int cHeismans, int cAA, int cAC, int cWins) {
        this(nm, t, yr, pot, iq, s1, s2, s3, rs, dur);
        careerGamesPlayed = cGamesPlayed;
        careerHeismans = cHeismans;
        careerAllAmerican = cAA;
        careerAllConference = cAC;
        careerWins = cWins;
    }

    public PlayerFB(String nm, int yr, int stars, Team t) {
        this();
        Player p = PlayerFactory.fromStars(PositionGroup.FB, nm, yr, stars, t, new Random());
        name = p.name;
        team = p.team;
        year = p.year;
        applyRatings(p.ratings);
        cost = p.cost;
        position = "FB";
    }

    @Override
    protected void syncLegacySkillsFromRatings() {
        ratRushPow = ratings.stre;
        ratBlock = ratings.rbk;
        ratRec = ratings.hnd;
    }

    @Override
    protected double costDivisor() { return 6.0; }

    @Override
    protected int costBase() { return 40; }

    @Override
    protected void bankPositionCareerStats() {
        careerRushAtt += statsRushAtt;
        statsRushAtt = 0;
        careerRushYards += statsRushYards;
        statsRushYards = 0;
        careerTDs += statsTD;
        statsTD = 0;
        super.bankPositionCareerStats();
    }

    @Override
    public int getHeismanScore() {
        return ratOvr * gamesPlayed;
    }

    @Override
    public ArrayList<String> getDetailStatsList(int games) {
        ArrayList<String> pStats = new ArrayList<>();
        pStats.add("Rush: " + statsRushAtt + " for " + statsRushYards + ">TDs: " + statsTD);
        pStats.add("Games: " + gamesPlayed + " (" + statsWins + "-" + (gamesPlayed-statsWins) + ")> ");
        pStats.addAll(getRatingsDetailLines());
        return pStats;
    }
}
