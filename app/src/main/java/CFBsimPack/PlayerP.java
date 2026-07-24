package CFBsimPack;

import java.util.ArrayList;
import java.util.Random;

/** Punter. */
public class PlayerP extends Player {

    public int ratPuntPow;
    public int ratPuntAcc;

    public int statsPuntAtt;
    public int statsPuntYards;

    public int careerPuntAtt;
    public int careerPuntYards;

    public PlayerP() { position = "P"; }

    public PlayerP(String nm, Team t, int yr, PlayerRatings bag, boolean rs) {
        this();
        name = nm; team = t; year = yr; isRedshirt = rs;
        applyRatings(bag);
        recomputeCost(new Random(nm != null ? nm.hashCode() : 0));
    }

    public PlayerP(String nm, Team t, int yr, int pot, int iq, int pow, int acc, int unused, boolean rs, int dur) {
        this();
        name = nm; team = t; year = yr; isRedshirt = rs;
        PlayerRatings bag = PlayerFactory.rollRatings(PositionGroup.P, yr, 3, new Random(nm.hashCode()));
        bag.pot = pot; bag.footIq = iq; bag.dur = dur;
        bag.ppw = pow; bag.pac = acc;
        applyRatings(bag);
        recomputeCost(new Random());
    }

    public PlayerP(String nm, Team t, int yr, int pot, int iq, int pow, int acc, int unused, boolean rs, int dur,
                    int cGamesPlayed, int cPuntAtt, int cPuntYards,
                    int cHeismans, int cAA, int cAC, int cWins) {
        this(nm, t, yr, pot, iq, pow, acc, unused, rs, dur);
        careerPuntAtt = cPuntAtt; careerPuntYards = cPuntYards;
        careerGamesPlayed = cGamesPlayed; careerHeismans = cHeismans;
        careerAllAmerican = cAA; careerAllConference = cAC; careerWins = cWins;
    }

    public PlayerP(String nm, int yr, int stars, Team t) {
        this();
        Player p = PlayerFactory.fromStars(PositionGroup.P, nm, yr, stars, t, new Random());
        name = p.name; team = p.team; year = p.year; applyRatings(p.ratings); cost = p.cost; position = "P";
    }

    @Override protected void syncLegacySkillsFromRatings() {
        ratPuntPow = ratings.ppw; ratPuntAcc = ratings.pac;
    }
    @Override protected double costDivisor() { return 3.5; }
    @Override protected int costBase() { return 80; }

    @Override protected void bankPositionCareerStats() {
        careerPuntAtt += statsPuntAtt; careerPuntYards += statsPuntYards;
        statsPuntAtt = 0; statsPuntYards = 0;
        super.bankPositionCareerStats();
    }

    @Override public int getHeismanScore() { return ratOvr + statsPuntYards / 20; }

    @Override public ArrayList<String> getDetailStatsList(int games) {
        ArrayList<String> pStats = new ArrayList<>();
        if (statsPuntAtt > 0) {
            pStats.add("Punts: " + statsPuntAtt + ">Avg: " + (statsPuntYards / statsPuntAtt) + " yds");
        } else {
            pStats.add("Punts: 0>Avg: 0 yds");
        }
        pStats.add("Games: " + gamesPlayed + " (" + statsWins + "-" + (gamesPlayed-statsWins) + ")> ");
        pStats.addAll(getRatingsDetailLines());
        return pStats;
    }

    @Override public ArrayList<String> getCareerStatsList() {
        ArrayList<String> pStats = new ArrayList<>();
        int att = statsPuntAtt + careerPuntAtt;
        int yds = statsPuntYards + careerPuntYards;
        pStats.add(att > 0 ? "Punts: " + att + ">Avg: " + (yds / att) + " yds" : "Punts: 0>Avg: 0 yds");
        pStats.addAll(super.getCareerStatsList());
        return pStats;
    }
}
