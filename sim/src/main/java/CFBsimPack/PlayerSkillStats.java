package CFBsimPack;

/**
 * Flat ZenGM-style skill counters. Every player has season + career bags;
 * unused positions leave fields at 0.
 */
public final class PlayerSkillStats {

    // Passing (QB)
    public int passAtt;
    public int passComp;
    public int passYards;
    public int passTd;
    public int passInt;
    public int sacked;

    // Rushing (QB/RB/FB)
    public int rushAtt;
    public int rushYards;
    public int rushTd;
    public int fumbles;

    // Receiving (WR/TE)
    public int targets;
    public int receptions;
    public int recYards;
    public int recTd;
    public int drops;
    public int recFumbles;

    // Kicking
    public int xpAtt;
    public int xpMade;
    public int fgAtt;
    public int fgMade;

    // Punting
    public int puntAtt;
    public int puntYards;

    // Defense (order: tackles, tfl, sacksDef, defInt, passDef, forcedFumbles, fumbleRec)
    public int tackles;
    public int tfl;
    public int sacksDef;
    public int defInt;
    public int passDef;
    public int forcedFumbles;
    public int fumbleRec;

    public void addFrom(PlayerSkillStats other) {
        if (other == null) return;
        passAtt += other.passAtt;
        passComp += other.passComp;
        passYards += other.passYards;
        passTd += other.passTd;
        passInt += other.passInt;
        sacked += other.sacked;
        rushAtt += other.rushAtt;
        rushYards += other.rushYards;
        rushTd += other.rushTd;
        fumbles += other.fumbles;
        targets += other.targets;
        receptions += other.receptions;
        recYards += other.recYards;
        recTd += other.recTd;
        drops += other.drops;
        recFumbles += other.recFumbles;
        xpAtt += other.xpAtt;
        xpMade += other.xpMade;
        fgAtt += other.fgAtt;
        fgMade += other.fgMade;
        puntAtt += other.puntAtt;
        puntYards += other.puntYards;
        tackles += other.tackles;
        tfl += other.tfl;
        sacksDef += other.sacksDef;
        defInt += other.defInt;
        passDef += other.passDef;
        forcedFumbles += other.forcedFumbles;
        fumbleRec += other.fumbleRec;
    }

    public void clear() {
        passAtt = 0;
        passComp = 0;
        passYards = 0;
        passTd = 0;
        passInt = 0;
        sacked = 0;
        rushAtt = 0;
        rushYards = 0;
        rushTd = 0;
        fumbles = 0;
        targets = 0;
        receptions = 0;
        recYards = 0;
        recTd = 0;
        drops = 0;
        recFumbles = 0;
        xpAtt = 0;
        xpMade = 0;
        fgAtt = 0;
        fgMade = 0;
        puntAtt = 0;
        puntYards = 0;
        tackles = 0;
        tfl = 0;
        sacksDef = 0;
        defInt = 0;
        passDef = 0;
        forcedFumbles = 0;
        fumbleRec = 0;
    }
}
