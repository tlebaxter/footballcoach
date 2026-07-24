package CFBsimPack;

import java.util.ArrayList;
import java.util.Random;

public class PlayerOL extends Player {

    public int ratOLPow;
    public int ratOLBkR;
    public int ratOLBkP;



    public PlayerOL() {
        position = "OL";
    }

    public PlayerOL(String nm, Team t, int yr, PlayerRatings bag, boolean rs) {
        this();
        name = nm;
        team = t;
        year = yr;
        isRedshirt = rs;
        applyRatings(bag);
        recomputeCost(new Random(nm != null ? nm.hashCode() : 0));
    }

    public PlayerOL(String nm, Team t, int yr, int pot, int iq, int s1, int s2, int s3, boolean rs, int dur) {
        this();
        name = nm;
        team = t;
        year = yr;
        isRedshirt = rs;
        PlayerRatings bag = PlayerFactory.rollRatings(PositionGroup.OL, yr, 3, new Random(nm.hashCode()));
        bag.pot = pot;
        bag.footIq = iq;
        bag.dur = dur;
        bag.stre = s1;
        bag.rbk = s2;
        bag.pbk = s3;
        applyRatings(bag);
        recomputeCost(new Random());
    }

    public PlayerOL(String nm, Team t, int yr, int pot, int iq, int s1, int s2, int s3, boolean rs, int dur,
                    int cGamesPlayed, int cHeismans, int cAA, int cAC, int cWins) {
        this(nm, t, yr, pot, iq, s1, s2, s3, rs, dur);
        careerGamesPlayed = cGamesPlayed;
        careerHeismans = cHeismans;
        careerAllAmerican = cAA;
        careerAllConference = cAC;
        careerWins = cWins;
    }

    public PlayerOL(String nm, int yr, int stars, Team t) {
        this();
        Player p = PlayerFactory.fromStars(PositionGroup.OL, nm, yr, stars, t, new Random());
        name = p.name;
        team = p.team;
        year = p.year;
        applyRatings(p.ratings);
        cost = p.cost;
        position = "OL";
    }

    @Override
    protected void syncLegacySkillsFromRatings() {
        ratOLPow = ratings.stre;
        ratOLBkR = ratings.rbk;
        ratOLBkP = ratings.pbk;
    }

    @Override
    protected double costDivisor() { return 5.0; }

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
