package CFBsimPack;

import java.util.ArrayList;
import java.util.Random;

public class PlayerLB extends Player {

    public int ratPow;
    public int ratRush;
    public int ratCov;



    public PlayerLB() {
        position = "LB";
    }

    public PlayerLB(String nm, Team t, int yr, PlayerRatings bag, boolean rs) {
        this();
        name = nm;
        team = t;
        year = yr;
        isRedshirt = rs;
        applyRatings(bag);
        recomputeCost(new Random(nm != null ? nm.hashCode() : 0));
    }

    public PlayerLB(String nm, Team t, int yr, int pot, int iq, int s1, int s2, int s3, boolean rs, int dur) {
        this();
        name = nm;
        team = t;
        year = yr;
        isRedshirt = rs;
        PlayerRatings bag = PlayerFactory.rollRatings(PositionGroup.LB, yr, 3, new Random(nm.hashCode()));
        bag.pot = pot;
        bag.footIq = iq;
        bag.dur = dur;
        bag.stre = s1;
        bag.rns = s2;
        bag.pcv = s3;
        applyRatings(bag);
        recomputeCost(new Random());
    }

    public PlayerLB(String nm, Team t, int yr, int pot, int iq, int s1, int s2, int s3, boolean rs, int dur,
                    int cGamesPlayed, int cHeismans, int cAA, int cAC, int cWins) {
        this(nm, t, yr, pot, iq, s1, s2, s3, rs, dur);
        careerGamesPlayed = cGamesPlayed;
        careerHeismans = cHeismans;
        careerAllAmerican = cAA;
        careerAllConference = cAC;
        careerWins = cWins;
    }

    public PlayerLB(String nm, int yr, int stars, Team t) {
        this();
        Player p = PlayerFactory.fromStars(PositionGroup.LB, nm, yr, stars, t, new Random());
        name = p.name;
        team = p.team;
        year = p.year;
        applyRatings(p.ratings);
        cost = p.cost;
        position = "LB";
    }

    @Override
    protected void syncLegacySkillsFromRatings() {
        ratPow = ratings.stre;
        ratRush = ratings.rns;
        ratCov = ratings.pcv;
    }

    @Override
    protected double costDivisor() { return 4.5; }

    @Override
    protected int costBase() { return 70; }

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
