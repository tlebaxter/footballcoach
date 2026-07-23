package CFBsimPack;

import java.util.ArrayList;
import java.util.Vector;

/** Linebacker (MIKE/WILL/SAM/ILB/OLB roles). */
public class PlayerLB extends Player {

    public int ratPow;
    public int ratRush;
    public int ratCov;
    public RoleTag roleTag = RoleTag.MIKE;

    public PlayerLB(String nm, Team t, int yr, int pot, int iq, int pow, int rsh, int cov, boolean rs, int dur) {
        init(nm, t, yr, pot, iq, pow, rsh, cov, rs, dur, 0, 0, 0, 0, 0);
    }

    public PlayerLB(String nm, Team t, int yr, int pot, int iq, int pow, int rsh, int cov, boolean rs, int dur,
                    int cGamesPlayed, int cHeismans, int cAA, int cAC, int cWins) {
        init(nm, t, yr, pot, iq, pow, rsh, cov, rs, dur, cGamesPlayed, cHeismans, cAA, cAC, cWins);
    }

    public PlayerLB(String nm, int yr, int stars, Team t) {
        name = nm;
        year = yr;
        team = t;
        gamesPlayed = 0;
        isInjured = false;
        ratPot = (int) (50 + 50 * Math.random());
        ratFootIQ = (int) (50 + 50 * Math.random());
        ratDur = (int) (50 + 50 * Math.random());
        ratPow = (int) (60 + year * 5 + stars * 5 - 25 * Math.random());
        ratRush = (int) (60 + year * 5 + stars * 5 - 25 * Math.random());
        ratCov = (int) (60 + year * 5 + stars * 5 - 25 * Math.random());
        ratOvr = (ratPow + ratRush * 2 + ratCov * 2) / 5;
        position = "LB";
        cost = (int) (Math.pow((float) ratOvr - 55, 2) / 6) + 50 + (int) (Math.random() * 100) - 50;
        ratingsVector = new Vector();
        wonHeisman = false;
        wonAllAmerican = false;
        wonAllConference = false;
        statsWins = 0;
        careerGamesPlayed = 0;
        careerHeismans = 0;
        careerAllAmerican = 0;
        careerAllConference = 0;
        careerWins = 0;
    }

    private void init(String nm, Team t, int yr, int pot, int iq, int pow, int rsh, int cov, boolean rs, int dur,
                      int cGamesPlayed, int cHeismans, int cAA, int cAC, int cWins) {
        team = t;
        name = nm;
        year = yr;
        gamesPlayed = 0;
        isInjured = false;
        ratPow = pow;
        ratRush = rsh;
        ratCov = cov;
        ratOvr = (pow + rsh * 2 + cov * 2) / 5;
        ratPot = pot;
        ratFootIQ = iq;
        ratDur = dur;
        isRedshirt = rs;
        position = "LB";
        cost = (int) (Math.pow((float) ratOvr - 55, 2) / 6) + 50 + (int) (Math.random() * 100) - 50;
        ratingsVector = new Vector();
        wonHeisman = false;
        wonAllAmerican = false;
        wonAllConference = false;
        statsWins = 0;
        careerGamesPlayed = cGamesPlayed;
        careerHeismans = cHeismans;
        careerAllAmerican = cAA;
        careerAllConference = cAC;
        careerWins = cWins;
    }

    @Override
    public void advanceSeason() {
        recordSeasonSnapshot();
        year++;
        int oldOvr = ratOvr;
        ratFootIQ += (int) (Math.random() * (ratPot + gamesPlayed - 35)) / 10;
        ratPow += (int) (Math.random() * (ratPot + gamesPlayed - 35)) / 10;
        ratRush += (int) (Math.random() * (ratPot + gamesPlayed - 35)) / 10;
        ratCov += (int) (Math.random() * (ratPot + gamesPlayed - 35)) / 10;
        if (Math.random() * 100 < ratPot) {
            ratPow += (int) (Math.random() * (ratPot + gamesPlayed - 40)) / 10;
            ratRush += (int) (Math.random() * (ratPot + gamesPlayed - 40)) / 10;
            ratCov += (int) (Math.random() * (ratPot + gamesPlayed - 40)) / 10;
        }
        ratOvr = (ratPow + ratRush * 2 + ratCov * 2) / 5;
        ratImprovement = ratOvr - oldOvr;
        careerGamesPlayed += gamesPlayed;
        careerWins += statsWins;
        if (wonHeisman) careerHeismans++;
        if (wonAllAmerican) careerAllAmerican++;
        if (wonAllConference) careerAllConference++;
    }

    @Override
    public ArrayList<String> getDetailStatsList(int games) {
        ArrayList<String> pStats = new ArrayList<>();
        pStats.add("Games: " + gamesPlayed + " (" + statsWins + "-" + (gamesPlayed - statsWins) + ")>Durability: " + getLetterGrade(ratDur));
        pStats.add("Football IQ: " + getLetterGrade(ratFootIQ) + ">Strength: " + getLetterGrade(ratPow));
        pStats.add("Run Stop: " + getLetterGrade(ratRush) + ">Coverage: " + getLetterGrade(ratCov));
        pStats.add(" > ");
        return pStats;
    }

    @Override
    public ArrayList<String> getDetailAllStatsList(int games) {
        ArrayList<String> pStats = getDetailStatsList(games);
        pStats.set(pStats.size() - 1, "[B]CAREER STATS:");
        pStats.addAll(getCareerStatsList());
        return pStats;
    }

    @Override
    public String getInfoForLineup() {
        if (injury != null) return getInitialName() + " [" + getYrStr() + "] " + ratOvr + "/" + getLetterGrade(ratPot) + " " + injury.toString();
        return getInitialName() + " [" + getYrStr() + "] " + ratOvr + "/" + getLetterGrade(ratPot) + " (" +
                getLetterGrade(ratPow) + ", " + getLetterGrade(ratRush) + ", " + getLetterGrade(ratCov) + ")";
    }
}
