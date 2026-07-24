package CFBsimPack;

import java.util.ArrayList;
import java.util.Random;

public class PlayerS extends Player {

    public int ratSCov;
    public int ratSSpd;
    public int ratSTkl;



    public PlayerS() {
        position = "S";
    }

    public PlayerS(String nm, Team t, int yr, PlayerRatings bag, boolean rs) {
        this();
        name = nm;
        team = t;
        year = yr;
        isRedshirt = rs;
        applyRatings(bag);
        recomputeCost(new Random(nm != null ? nm.hashCode() : 0));
    }

    public PlayerS(String nm, Team t, int yr, int pot, int iq, int s1, int s2, int s3, boolean rs, int dur) {
        this();
        name = nm;
        team = t;
        year = yr;
        isRedshirt = rs;
        PlayerRatings bag = PlayerFactory.rollRatings(PositionGroup.S, yr, 3, new Random(nm.hashCode()));
        bag.pot = pot;
        bag.footIq = iq;
        bag.dur = dur;
        bag.pcv = s1;
        bag.spd = s2;
        bag.tck = s3;
        applyRatings(bag);
        recomputeCost(new Random());
    }

    public PlayerS(String nm, Team t, int yr, int pot, int iq, int s1, int s2, int s3, boolean rs, int dur,
                    int cGamesPlayed, int cHeismans, int cAA, int cAC, int cWins) {
        this(nm, t, yr, pot, iq, s1, s2, s3, rs, dur);
        careerGamesPlayed = cGamesPlayed;
        careerHeismans = cHeismans;
        careerAllAmerican = cAA;
        careerAllConference = cAC;
        careerWins = cWins;
    }

    public PlayerS(String nm, int yr, int stars, Team t) {
        this();
        Player p = PlayerFactory.fromStars(PositionGroup.S, nm, yr, stars, t, new Random());
        name = p.name;
        team = p.team;
        year = p.year;
        applyRatings(p.ratings);
        cost = p.cost;
        position = "S";
    }

    @Override
    protected void syncLegacySkillsFromRatings() {
        ratSCov = ratings.pcv;
        ratSSpd = ratings.spd;
        ratSTkl = ratings.tck;
    }

    @Override
    protected double costDivisor() { return 4.5; }

    @Override
    protected int costBase() { return 50; }

    @Override
    protected void bankPositionCareerStats() {
        super.bankPositionCareerStats();
    }

    @Override
    public int getHeismanScore() {
        return ratOvr * gamesPlayed;
    }

    @Override
    public ArrayList<String> getDetailStatsList(int games) {
        ArrayList<String> pStats = new ArrayList<>();
        pStats.add("Games: " + gamesPlayed + " (" + statsWins + "-" + (gamesPlayed-statsWins) + ")> ");
        pStats.addAll(getRatingsDetailLines());
        return pStats;
    }
}
