package CFBsimPack.engine;

/**
 * Lightweight box-score row for coach HUD (no Player reference needed in UI).
 */
public final class BoxScoreLine {
    public final String name;
    public final String position;
    public final boolean home;
    public final int passComp;
    public final int passAtt;
    public final int passYards;
    public final int passTd;
    public final int passInt;
    public final int rushAtt;
    public final int rushYards;
    public final int rushTd;
    public final int receptions;
    public final int recYards;
    public final int recTd;
    public final int prAtt;
    public final int prYards;
    public final int prTd;
    public final int krAtt;
    public final int krYards;
    public final int krTd;
    public final int fgMade;
    public final int fgAtt;
    public final int xpMade;
    public final int xpAtt;
    public final int puntAtt;
    public final int puntYards;

    public BoxScoreLine(
            String name,
            String position,
            boolean home,
            int passComp,
            int passAtt,
            int passYards,
            int passTd,
            int passInt,
            int rushAtt,
            int rushYards,
            int rushTd,
            int receptions,
            int recYards,
            int recTd,
            int prAtt,
            int prYards,
            int prTd,
            int krAtt,
            int krYards,
            int krTd,
            int fgMade,
            int fgAtt,
            int xpMade,
            int xpAtt,
            int puntAtt,
            int puntYards
    ) {
        this.name = name != null ? name : "";
        this.position = position != null ? position : "";
        this.home = home;
        this.passComp = passComp;
        this.passAtt = passAtt;
        this.passYards = passYards;
        this.passTd = passTd;
        this.passInt = passInt;
        this.rushAtt = rushAtt;
        this.rushYards = rushYards;
        this.rushTd = rushTd;
        this.receptions = receptions;
        this.recYards = recYards;
        this.recTd = recTd;
        this.prAtt = prAtt;
        this.prYards = prYards;
        this.prTd = prTd;
        this.krAtt = krAtt;
        this.krYards = krYards;
        this.krTd = krTd;
        this.fgMade = fgMade;
        this.fgAtt = fgAtt;
        this.xpMade = xpMade;
        this.xpAtt = xpAtt;
        this.puntAtt = puntAtt;
        this.puntYards = puntYards;
    }
}
