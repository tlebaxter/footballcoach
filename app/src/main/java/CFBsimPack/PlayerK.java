package CFBsimPack;

import java.util.ArrayList;
import java.util.Random;
import java.util.Vector;

/** Placekicker (FG/XP). Punting is {@link PlayerP}. */
public class PlayerK extends Player {

    public int ratKickPow;
    public int ratKickAcc;
    public int ratKickFum;

    public int statsXPAtt;
    public int statsXPMade;
    public int statsFGAtt;
    public int statsFGMade;

    public int careerXPAtt;
    public int careerXPMade;
    public int careerFGAtt;
    public int careerFGMade;

    public PlayerK() { position = "K"; }

    public PlayerK(String nm, Team t, int yr, PlayerRatings bag, boolean rs) {
        this();
        name = nm; team = t; year = yr; isRedshirt = rs;
        applyRatings(bag);
        recomputeCost(new Random(nm != null ? nm.hashCode() : 0));
    }

    public PlayerK(String nm, Team t, int yr, int pot, int iq, int pow, int acc, int fum, boolean rs, int dur) {
        this();
        name = nm; team = t; year = yr; isRedshirt = rs;
        PlayerRatings bag = PlayerFactory.rollRatings(PositionGroup.K, yr, 3, new Random(nm.hashCode()));
        bag.pot = pot; bag.footIq = iq; bag.dur = dur;
        bag.kpw = pow; bag.kac = acc; bag.bsc = fum;
        applyRatings(bag);
        recomputeCost(new Random());
    }

    public PlayerK(String nm, Team t, int yr, int pot, int iq, int pow, int acc, int fum, boolean rs, int dur,
                    int cGamesPlayed, int cXPA, int cXPM, int cFGA, int cFGM,
                    int cHeismans, int cAA, int cAC, int cWins) {
        this(nm, t, yr, pot, iq, pow, acc, fum, rs, dur);
        careerXPAtt = cXPA; careerXPMade = cXPM; careerFGAtt = cFGA; careerFGMade = cFGM;
        careerGamesPlayed = cGamesPlayed; careerHeismans = cHeismans;
        careerAllAmerican = cAA; careerAllConference = cAC; careerWins = cWins;
    }

    public PlayerK(String nm, int yr, int stars, Team t) {
        this();
        Player p = PlayerFactory.fromStars(PositionGroup.K, nm, yr, stars, t, new Random());
        name = p.name; team = p.team; year = p.year; applyRatings(p.ratings); cost = p.cost; position = "K";
    }

    @Override protected void syncLegacySkillsFromRatings() {
        ratKickPow = ratings.kpw; ratKickAcc = ratings.kac; ratKickFum = Math.max(40, 100 - ratings.bsc);
    }
    @Override protected double costDivisor() { return 3.5; }
    @Override protected int costBase() { return 100; }

    @Override protected void bankPositionCareerStats() {
        careerXPAtt += statsXPAtt; careerXPMade += statsXPMade;
        careerFGAtt += statsFGAtt; careerFGMade += statsFGMade;
        statsXPAtt = 0; statsXPMade = 0; statsFGAtt = 0; statsFGMade = 0;
        super.bankPositionCareerStats();
    }

    public Vector getStatsVector() {
        Vector v = new Vector(6);
        v.add(statsXPMade); v.add(statsXPAtt);
        v.add(statsXPAtt > 0 ? (float)((int)(1000f*statsXPMade/statsXPAtt))/10 : 0f);
        v.add(statsFGMade); v.add(statsFGAtt);
        v.add(statsFGAtt > 0 ? (float)((int)(1000f*statsFGMade/statsFGAtt))/10 : 0f);
        return v;
    }

    @Override public int getHeismanScore() {
        if (statsFGAtt <= 0) return ratOvr;
        return (int)((statsFGMade*5 + statsXPMade)*((double)statsFGMade/statsFGAtt)) + ratOvr;
    }

    @Override public ArrayList<String> getDetailStatsList(int games) {
        ArrayList<String> pStats = new ArrayList<>();
        if (statsXPAtt > 0) pStats.add("XP Made/Att: " + statsXPMade + "/" + statsXPAtt + ">XP Percent: " + (100 * statsXPMade / statsXPAtt) + "%");
        else pStats.add("XP Made/Att: 0/0>XP Percent: 0%");
        if (statsFGAtt > 0) pStats.add("FG Made/Att: " + statsFGMade + "/" + statsFGAtt + ">FG Percent: " + (100 * statsFGMade / statsFGAtt) + "%");
        else pStats.add("FG Made/Att: 0/0>FG Percent: 0%");
        pStats.add("Games: " + gamesPlayed + " (" + statsWins + "-" + (gamesPlayed-statsWins) + ")> ");
        pStats.addAll(getRatingsDetailLines());
        return pStats;
    }

    @Override public ArrayList<String> getCareerStatsList() {
        ArrayList<String> pStats = new ArrayList<>();
        int xpa = statsXPAtt+careerXPAtt, xpm = statsXPMade+careerXPMade;
        int fga = statsFGAtt+careerFGAtt, fgm = statsFGMade+careerFGMade;
        pStats.add(xpa > 0 ? "XP Made/Att: " + xpm + "/" + xpa + ">XP Percentage: " + (100*xpm/xpa) + "%" : "XP Made/Att: 0/0>XP Percentage: 0%");
        pStats.add(fga > 0 ? "FG Made/Att: " + fgm + "/" + fga + ">FG Percentage: " + (100*fgm/fga) + "%" : "FG Made/Att: 0/0>FG Percentage: 0%");
        pStats.addAll(super.getCareerStatsList());
        return pStats;
    }
}
