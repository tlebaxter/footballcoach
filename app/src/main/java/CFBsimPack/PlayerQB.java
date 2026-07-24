package CFBsimPack;

import java.util.ArrayList;
import java.util.Random;
import java.util.Vector;

/**
 * QB player — stats + legacy skill aliases synced from {@link PlayerRatings}.
 */
public class PlayerQB extends Player {

    public int ratPassPow;
    public int ratPassAcc;
    public int ratPassEva;

    public int statsPassAtt;
    public int statsPassComp;
    public int statsTD;
    public int statsInt;
    public int statsPassYards;
    public int statsSacked;
    public int statsRushAtt;
    public int statsRushYards;
    public int statsRushTD;

    public int careerPassAtt;
    public int careerPassComp;
    public int careerTDs;
    public int careerInt;
    public int careerPassYards;
    public int careerSacked;
    public int careerRushAtt;
    public int careerRushYards;
    public int careerRushTD;

    public PlayerQB() {
        position = "QB";
    }

    public PlayerQB(String nm, Team t, int yr, PlayerRatings bag, boolean rs) {
        this();
        name = nm;
        team = t;
        year = yr;
        isRedshirt = rs;
        applyRatings(bag);
        recomputeCost(new Random(nm != null ? nm.hashCode() : 0));
    }

    /** Legacy 3-skill load — maps into ratings bag. */
    public PlayerQB(String nm, Team t, int yr, int pot, int iq, int pow, int acc, int eva, boolean rs, int dur) {
        this();
        name = nm;
        team = t;
        year = yr;
        isRedshirt = rs;
        PlayerRatings bag = PlayerFactory.rollRatings(PositionGroup.QB, yr, 3, new Random(nm.hashCode()));
        bag.pot = pot;
        bag.footIq = iq;
        bag.dur = dur;
        bag.thp = pow;
        bag.tha = acc;
        bag.elu = eva;
        applyRatings(bag);
        recomputeCost(new Random());
    }

    public PlayerQB(String nm, Team t, int yr, int pot, int iq, int pow, int acc, int eva, boolean rs, int dur,
                    int cGamesPlayed, int cPassAtt, int cPassComp, int cTDs, int cInt, int cPassYards, int cSacked,
                    int cHeismans, int cAA, int cAC, int cWins) {
        this(nm, t, yr, pot, iq, pow, acc, eva, rs, dur);
        careerPassAtt = cPassAtt;
        careerPassComp = cPassComp;
        careerTDs = cTDs;
        careerInt = cInt;
        careerPassYards = cPassYards;
        careerSacked = cSacked;
        careerGamesPlayed = cGamesPlayed;
        careerHeismans = cHeismans;
        careerAllAmerican = cAA;
        careerAllConference = cAC;
        careerWins = cWins;
    }

    public PlayerQB(String nm, int yr, int stars, Team t) {
        this();
        Player p = PlayerFactory.fromStars(PositionGroup.QB, nm, yr, stars, t, new Random());
        copyIdentityFrom(p);
    }

    private void copyIdentityFrom(Player p) {
        name = p.name;
        team = p.team;
        year = p.year;
        applyRatings(p.ratings);
        cost = p.cost;
        position = "QB";
    }

    @Override
    protected void syncLegacySkillsFromRatings() {
        ratPassPow = ratings.thp;
        ratPassAcc = ratings.tha;
        ratPassEva = ratings.elu;
    }

    @Override
    protected double costDivisor() {
        return 1.5;
    }

    @Override
    protected int costBase() {
        return 150;
    }

    @Override
    protected void bankPositionCareerStats() {
        careerPassAtt += statsPassAtt;
        careerPassComp += statsPassComp;
        careerTDs += statsTD;
        careerInt += statsInt;
        careerPassYards += statsPassYards;
        careerSacked += statsSacked;
        careerRushAtt += statsRushAtt;
        careerRushYards += statsRushYards;
        careerRushTD += statsRushTD;
        statsPassAtt = 0;
        statsPassComp = 0;
        statsTD = 0;
        statsInt = 0;
        statsPassYards = 0;
        statsSacked = 0;
        statsRushAtt = 0;
        statsRushYards = 0;
        statsRushTD = 0;
        super.bankPositionCareerStats();
    }

    public Vector getStatsVector() {
        Vector v = new Vector(9);
        v.add(statsPassComp);
        v.add(statsPassAtt);
        v.add((float) ((int) ((float) statsPassComp / Math.max(1, statsPassAtt) * 1000)) / 10);
        v.add(statsTD);
        v.add(statsInt);
        v.add(statsPassYards);
        v.add(statsPassYards);
        v.add((float) ((int) ((float) statsPassYards / Math.max(1, statsPassAtt) * 100)) / 100);
        v.add(statsSacked);
        return v;
    }

    @Override
    public int getHeismanScore() {
        return statsTD * 140 - statsInt * 250 + statsPassYards + statsRushYards + statsRushTD * 100;
    }

    @Override
    public ArrayList<String> getDetailStatsList(int games) {
        ArrayList<String> pStats = new ArrayList<>();
        pStats.add("TD/Int: " + statsTD + "/" + statsInt + ">Comp Percent: "
                + (100 * statsPassComp / (statsPassAtt + 1)) + "%");
        pStats.add("Pass Yards: " + statsPassYards + " yds>Yards/Att: "
                + ((double) (10 * statsPassYards / (statsPassAtt + 1)) / 10) + " yds");
        pStats.add("Yds/Game: " + (statsPassYards / getGamesPlayed()) + " yds/g>Sacks: " + statsSacked);
        if (statsRushAtt > 0) {
            pStats.add("Rush: " + statsRushAtt + " for " + statsRushYards + ">Rush TD: " + statsRushTD);
        }
        pStats.add("Games: " + gamesPlayed + " (" + statsWins + "-" + (gamesPlayed - statsWins) + ")> ");
        pStats.addAll(getRatingsDetailLines());
        return pStats;
    }

    @Override
    public ArrayList<String> getCareerStatsList() {
        ArrayList<String> pStats = new ArrayList<>();
        pStats.add("TD/Int: " + (statsTD + careerTDs) + "/" + (statsInt + careerInt) + ">Comp Percent: "
                + (100 * (statsPassComp + careerPassComp) / (statsPassAtt + careerPassAtt + 1)) + "%");
        pStats.add("Pass Yards: " + (statsPassYards + careerPassYards) + " yds>Yards/Att: "
                + ((double) (10 * (statsPassYards + careerPassYards) / (statsPassAtt + careerPassAtt + 1)) / 10)
                + " yds");
        pStats.add("Yds/Game: " + ((statsPassYards + careerPassYards) / (getGamesPlayed() + careerGamesPlayed))
                + " yds/g>Sacks: " + (statsSacked + careerSacked));
        pStats.addAll(super.getCareerStatsList());
        return pStats;
    }
}
