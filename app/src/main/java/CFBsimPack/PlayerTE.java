package CFBsimPack;

import java.util.ArrayList;
import java.util.Random;

public class PlayerTE extends Player {

    public int ratRecCat;
    public int ratRecSpd;
    public int ratBlock;
    public int statsTargets;
    public int statsReceptions;
    public int statsRecYards;
    public int statsTD;
    public int careerTargets;
    public int careerReceptions;
    public int careerRecYards;
    public int careerTDs;

    public PlayerTE() {
        position = "TE";
    }

    public PlayerTE(String nm, Team t, int yr, PlayerRatings bag, boolean rs) {
        this();
        name = nm;
        team = t;
        year = yr;
        isRedshirt = rs;
        applyRatings(bag);
        recomputeCost(new Random(nm != null ? nm.hashCode() : 0));
    }

    public PlayerTE(String nm, Team t, int yr, int pot, int iq, int s1, int s2, int s3, boolean rs, int dur) {
        this();
        name = nm;
        team = t;
        year = yr;
        isRedshirt = rs;
        PlayerRatings bag = PlayerFactory.rollRatings(PositionGroup.TE, yr, 3, new Random(nm.hashCode()));
        bag.pot = pot;
        bag.footIq = iq;
        bag.dur = dur;
        bag.hnd = s1;
        bag.spd = s2;
        bag.rbk = s3;
        applyRatings(bag);
        recomputeCost(new Random());
    }

    public PlayerTE(String nm, Team t, int yr, int pot, int iq, int s1, int s2, int s3, boolean rs, int dur,
                    int cGamesPlayed, int cHeismans, int cAA, int cAC, int cWins) {
        this(nm, t, yr, pot, iq, s1, s2, s3, rs, dur);
        careerGamesPlayed = cGamesPlayed;
        careerHeismans = cHeismans;
        careerAllAmerican = cAA;
        careerAllConference = cAC;
        careerWins = cWins;
    }

    public PlayerTE(String nm, int yr, int stars, Team t) {
        this();
        Player p = PlayerFactory.fromStars(PositionGroup.TE, nm, yr, stars, t, new Random());
        name = p.name;
        team = p.team;
        year = p.year;
        applyRatings(p.ratings);
        cost = p.cost;
        position = "TE";
    }

    @Override
    protected void syncLegacySkillsFromRatings() {
        ratRecCat = ratings.hnd;
        ratRecSpd = ratings.spd;
        ratBlock = ratings.rbk;
    }

    @Override
    protected double costDivisor() { return 5.0; }

    @Override
    protected int costBase() { return 50; }

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
        super.bankPositionCareerStats();
    }

    @Override
    public int getHeismanScore() {
        return statsTD * 100 + statsRecYards;
    }

    @Override
    public ArrayList<String> getDetailStatsList(int games) {
        ArrayList<String> pStats = new ArrayList<>();
        pStats.add("Rec: " + statsReceptions + ">Yards: " + statsRecYards);
        pStats.add("TDs: " + statsTD + ">Games: " + gamesPlayed);
        pStats.addAll(getRatingsDetailLines());
        return pStats;
    }
}
