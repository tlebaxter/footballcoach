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
    /**
     * Preferred schedule week index (0-based), or {@code -1} for automatic placement.
     */
    public int preferredWeek;
    public boolean settled;

    public OocContractGame(int year, String homeAbbr, String awayAbbr, int guarantee, int winBonus) {
        this(year, homeAbbr, awayAbbr, guarantee, winBonus, -1);
    }

    public OocContractGame(
            int year,
            String homeAbbr,
            String awayAbbr,
            int guarantee,
            int winBonus,
            int preferredWeek) {
        this.year = year;
        this.homeAbbr = homeAbbr;
        this.awayAbbr = awayAbbr;
        this.guarantee = Math.max(0, guarantee);
        this.winBonus = Math.max(0, winBonus);
        this.preferredWeek = preferredWeek < 0 ? -1 : preferredWeek;
        this.settled = false;
    }

    /** Copy with a new year; home/away/money/preferred week unchanged. */
    public OocContractGame withYear(int newYear) {
        OocContractGame g = new OocContractGame(
                newYear, homeAbbr, awayAbbr, guarantee, winBonus, preferredWeek);
        g.settled = settled;
        return g;
    }

    public String encode() {
        return year + ":" + homeAbbr + ":" + awayAbbr + ":" + guarantee + ":" + winBonus
                + ":" + (settled ? "1" : "0") + ":" + preferredWeek;
    }

    public static OocContractGame parse(String raw) {
        String[] p = raw.split(":");
        if (p.length < 5) {
            throw new IllegalArgumentException("Invalid OOC contract game: " + raw);
        }
        int preferred = -1;
        if (p.length >= 7) {
            preferred = Integer.parseInt(p[6]);
        }
        OocContractGame g = new OocContractGame(
                Integer.parseInt(p[0]),
                p[1],
                p[2],
                Integer.parseInt(p[3]),
                Integer.parseInt(p[4]),
                preferred);
        if (p.length >= 6) {
            g.settled = "1".equals(p[5]);
        }
        return g;
    }
}
