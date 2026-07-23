package CFBsimPack;

import java.util.ArrayList;
import java.util.Vector;

/** Fullback — lead block / short yardage. */
public class PlayerFB extends Player {

    public int ratRushPow;
    public int ratBlock;
    public int ratRec;
    public RoleTag roleTag = RoleTag.FB;
    public int statsRushYards;
    public int statsTD;
    public int statsFumbles;

    public PlayerFB(String nm, Team t, int yr, int pot, int iq, int pow, int blk, int rec, boolean rs, int dur) {
        init(nm, t, yr, pot, iq, pow, blk, rec, rs, dur, 0, 0, 0, 0, 0);
    }

    public PlayerFB(String nm, Team t, int yr, int pot, int iq, int pow, int blk, int rec, boolean rs, int dur,
                    int cGamesPlayed, int cHeismans, int cAA, int cAC, int cWins) {
        init(nm, t, yr, pot, iq, pow, blk, rec, rs, dur, cGamesPlayed, cHeismans, cAA, cAC, cWins);
    }

    public PlayerFB(String nm, int yr, int stars, Team t) {
        name = nm;
        year = yr;
        team = t;
        gamesPlayed = 0;
        isInjured = false;
        ratPot = (int) (50 + 50 * Math.random());
        ratFootIQ = (int) (50 + 50 * Math.random());
        ratDur = (int) (50 + 50 * Math.random());
        ratRushPow = (int) (60 + year * 5 + stars * 5 - 25 * Math.random());
        ratBlock = (int) (60 + year * 5 + stars * 5 - 25 * Math.random());
        ratRec = (int) (50 + year * 5 + stars * 4 - 25 * Math.random());
        ratOvr = (ratRushPow + ratBlock * 3 + ratRec) / 5;
        position = "FB";
        cost = (int) (Math.pow((float) ratOvr - 55, 2) / 6) + 40 + (int) (Math.random() * 80) - 40;
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
        statsRushYards = 0;
        statsTD = 0;
        statsFumbles = 0;
    }

    private void init(String nm, Team t, int yr, int pot, int iq, int pow, int blk, int rec, boolean rs, int dur,
                      int cGamesPlayed, int cHeismans, int cAA, int cAC, int cWins) {
        team = t;
        name = nm;
        year = yr;
        gamesPlayed = 0;
        isInjured = false;
        ratRushPow = pow;
        ratBlock = blk;
        ratRec = rec;
        ratOvr = (pow + blk * 3 + rec) / 5;
        ratPot = pot;
        ratFootIQ = iq;
        ratDur = dur;
        isRedshirt = rs;
        position = "FB";
        cost = (int) (Math.pow((float) ratOvr - 55, 2) / 6) + 40 + (int) (Math.random() * 80) - 40;
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
        statsRushYards = 0;
        statsTD = 0;
        statsFumbles = 0;
    }

    @Override
    public void advanceSeason() {
        recordSeasonSnapshot();
        year++;
        int oldOvr = ratOvr;
        ratFootIQ += (int) (Math.random() * (ratPot + gamesPlayed - 35)) / 10;
        ratRushPow += (int) (Math.random() * (ratPot + gamesPlayed - 35)) / 10;
        ratBlock += (int) (Math.random() * (ratPot + gamesPlayed - 35)) / 10;
        ratRec += (int) (Math.random() * (ratPot + gamesPlayed - 35)) / 10;
        ratOvr = (ratRushPow + ratBlock * 3 + ratRec) / 5;
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
        pStats.add("Games: " + gamesPlayed + ">Rush Yds: " + statsRushYards);
        pStats.add("Power: " + getLetterGrade(ratRushPow) + ">Block: " + getLetterGrade(ratBlock));
        pStats.add("Hands: " + getLetterGrade(ratRec) + ">Durability: " + getLetterGrade(ratDur));
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
                getLetterGrade(ratRushPow) + ", " + getLetterGrade(ratBlock) + ", " + getLetterGrade(ratRec) + ")";
    }
}
