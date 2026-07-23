package CFBsimPack;

import java.util.ArrayList;
import java.util.Vector;

/** Tight end — catch + block mix. */
public class PlayerTE extends Player {

    public int ratRecCat;
    public int ratRecSpd;
    public int ratBlock;
    public RoleTag roleTag = RoleTag.TE;

    public PlayerTE(String nm, Team t, int yr, int pot, int iq, int cat, int spd, int blk, boolean rs, int dur) {
        init(nm, t, yr, pot, iq, cat, spd, blk, rs, dur, 0, 0, 0, 0, 0);
    }

    public PlayerTE(String nm, Team t, int yr, int pot, int iq, int cat, int spd, int blk, boolean rs, int dur,
                    int cGamesPlayed, int cHeismans, int cAA, int cAC, int cWins) {
        init(nm, t, yr, pot, iq, cat, spd, blk, rs, dur, cGamesPlayed, cHeismans, cAA, cAC, cWins);
    }

    public PlayerTE(String nm, int yr, int stars, Team t) {
        name = nm;
        year = yr;
        team = t;
        gamesPlayed = 0;
        isInjured = false;
        ratPot = (int) (50 + 50 * Math.random());
        ratFootIQ = (int) (50 + 50 * Math.random());
        ratDur = (int) (50 + 50 * Math.random());
        ratRecCat = (int) (60 + year * 5 + stars * 5 - 25 * Math.random());
        ratRecSpd = (int) (55 + year * 5 + stars * 5 - 25 * Math.random());
        ratBlock = (int) (60 + year * 5 + stars * 5 - 25 * Math.random());
        ratOvr = (ratRecCat * 2 + ratRecSpd + ratBlock * 2) / 5;
        position = "TE";
        cost = (int) (Math.pow((float) ratOvr - 55, 2) / 5) + 50 + (int) (Math.random() * 100) - 50;
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
        statsReceptions = 0;
        statsRecYards = 0;
        statsTD = 0;
        statsDrops = 0;
        statsFumbles = 0;
    }

    public int statsReceptions;
    public int statsRecYards;
    public int statsTD;
    public int statsDrops;
    public int statsFumbles;

    private void init(String nm, Team t, int yr, int pot, int iq, int cat, int spd, int blk, boolean rs, int dur,
                      int cGamesPlayed, int cHeismans, int cAA, int cAC, int cWins) {
        team = t;
        name = nm;
        year = yr;
        gamesPlayed = 0;
        isInjured = false;
        ratRecCat = cat;
        ratRecSpd = spd;
        ratBlock = blk;
        ratOvr = (cat * 2 + spd + blk * 2) / 5;
        ratPot = pot;
        ratFootIQ = iq;
        ratDur = dur;
        isRedshirt = rs;
        position = "TE";
        cost = (int) (Math.pow((float) ratOvr - 55, 2) / 5) + 50 + (int) (Math.random() * 100) - 50;
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
        statsReceptions = 0;
        statsRecYards = 0;
        statsTD = 0;
        statsDrops = 0;
        statsFumbles = 0;
    }

    @Override
    public void advanceSeason() {
        recordSeasonSnapshot();
        year++;
        int oldOvr = ratOvr;
        ratFootIQ += (int) (Math.random() * (ratPot + gamesPlayed - 35)) / 10;
        ratRecCat += (int) (Math.random() * (ratPot + gamesPlayed - 35)) / 10;
        ratRecSpd += (int) (Math.random() * (ratPot + gamesPlayed - 35)) / 10;
        ratBlock += (int) (Math.random() * (ratPot + gamesPlayed - 35)) / 10;
        ratOvr = (ratRecCat * 2 + ratRecSpd + ratBlock * 2) / 5;
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
        pStats.add("Games: " + gamesPlayed + ">" + "Rec: " + statsReceptions + " Yds: " + statsRecYards);
        pStats.add("Catch: " + getLetterGrade(ratRecCat) + ">Speed: " + getLetterGrade(ratRecSpd));
        pStats.add("Block: " + getLetterGrade(ratBlock) + ">Durability: " + getLetterGrade(ratDur));
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
                getLetterGrade(ratRecCat) + ", " + getLetterGrade(ratRecSpd) + ", " + getLetterGrade(ratBlock) + ")";
    }
}
