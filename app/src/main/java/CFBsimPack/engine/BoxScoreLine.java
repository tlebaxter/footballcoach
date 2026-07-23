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
            int recTd
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
    }
}
