package CFBsimPack;

import java.util.ArrayList;
import java.util.Random;

public class PlayerWR extends Player {

    public int ratRecCat;
    public int ratRecSpd;
    public int ratRecEva;
    public int statsTargets;
    public int statsReceptions;
    public int statsRecYards;
    public int statsTD;
    public int statsDrops;
    public int statsFumbles;
    public int careerTargets;
    public int careerReceptions;
    public int careerRecYards;
    public int careerTDs;
    public int careerDrops;
    public int careerFumbles;

    public PlayerWR() {
        position = "WR";
    }

    public PlayerWR(String nm, Team t, int yr, PlayerRatings bag, boolean rs) {
        this();
        name = nm;
        team = t;
        year = yr;
        isRedshirt = rs;
        applyRatings(bag);
        recomputeCost(new Random(nm != null ? nm.hashCode() : 0));
    }

    public PlayerWR(String nm, Team t, int yr, int pot, int iq, int s1, int s2, int s3, boolean rs, int dur) {
        this();
        name = nm;
        team = t;
        year = yr;
        isRedshirt = rs;
        PlayerRatings bag = PlayerFactory.rollRatings(PositionGroup.WR, yr, 3, new Random(nm.hashCode()));
        bag.pot = pot;
        bag.footIq = iq;
        bag.dur = dur;
        bag.hnd = s1;
        bag.spd = s2;
        bag.elu = s3;
        applyRatings(bag);
        recomputeCost(new Random());
    }

    public PlayerWR(String nm, Team t, int yr, int pot, int iq, int s1, int s2, int s3, boolean rs, int dur,
                    int cGamesPlayed, int cHeismans, int cAA, int cAC, int cWins) {
        this(nm, t, yr, pot, iq, s1, s2, s3, rs, dur);
        careerGamesPlayed = cGamesPlayed;
        careerHeismans = cHeismans;
        careerAllAmerican = cAA;
        careerAllConference = cAC;
        careerWins = cWins;
    }

    public PlayerWR(String nm, int yr, int stars, Team t) {
        this();
        Player p = PlayerFactory.fromStars(PositionGroup.WR, nm, yr, stars, t, new Random());
        name = p.name;
        team = p.team;
        year = p.year;
        applyRatings(p.ratings);
        cost = p.cost;
        position = "WR";
    }

    @Override
    protected void syncLegacySkillsFromRatings() {
        ratRecCat = ratings.hnd;
        ratRecSpd = ratings.spd;
        ratRecEva = ratings.elu;
    }

    @Override
    protected double costDivisor() { return 3.5; }

    @Override
    protected int costBase() { return 100; }

    @Override
    protected void bankPositionCareerStats() {
        careerTargets += statsTargets;
        statsTargets = 0;
        careerReceptions += statsReceptions;
        statsReceptions = 0;
        careerRecYards += statsRecYards;
        statsRecYards = 0;
        careerTDs += statsTD;
        statsTD = 0;
        careerDrops += statsDrops;
        statsDrops = 0;
        careerFumbles += statsFumbles;
        statsFumbles = 0;
        super.bankPositionCareerStats();
    }

    @Override
    public int getHeismanScore() {
        return statsTD * 120 + statsRecYards;
    }

    @Override
    public ArrayList<String> getDetailStatsList(int games) {
        ArrayList<String> pStats = new ArrayList<>();
        pStats.add("Rec: " + statsReceptions + "/" + statsTargets + ">Yards: " + statsRecYards);
        pStats.add("TDs: " + statsTD + ">Drops: " + statsDrops);
        pStats.add("Games: " + gamesPlayed + " (" + statsWins + "-" + (gamesPlayed-statsWins) + ")> ");
        pStats.addAll(getRatingsDetailLines());
        return pStats;
    }
}
