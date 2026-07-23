package CFBsimPack;

/**
 * One season of an OOC multi-year contract.
 */
public final class OocContractGame {
    public final int year;
    public final String homeAbbr;
    public final String awayAbbr;
    /** Paid by home to away when the game is played. */
    public final int guarantee;
    /** Extra paid by home to away if away wins. */
    public final int winBonus;
    public boolean settled;

    public OocContractGame(int year, String homeAbbr, String awayAbbr, int guarantee, int winBonus) {
        this.year = year;
        this.homeAbbr = homeAbbr;
        this.awayAbbr = awayAbbr;
        this.guarantee = Math.max(0, guarantee);
        this.winBonus = Math.max(0, winBonus);
        this.settled = false;
    }

    public String encode() {
        return year + ":" + homeAbbr + ":" + awayAbbr + ":" + guarantee + ":" + winBonus
                + ":" + (settled ? "1" : "0");
    }

    public static OocContractGame parse(String raw) {
        String[] p = raw.split(":");
        if (p.length < 5) {
            throw new IllegalArgumentException("Invalid OOC contract game: " + raw);
        }
        OocContractGame g = new OocContractGame(
                Integer.parseInt(p[0]),
                p[1],
                p[2],
                Integer.parseInt(p[3]),
                Integer.parseInt(p[4]));
        if (p.length >= 6) {
            g.settled = "1".equals(p[5]);
        }
        return g;
    }
}
