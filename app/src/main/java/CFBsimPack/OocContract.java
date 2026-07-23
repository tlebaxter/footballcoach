package CFBsimPack;

import java.util.ArrayList;
import java.util.List;

/**
 * Multi-year out-of-conference series (single, buy game, or home-and-home).
 */
public final class OocContract {

    public enum Type {
        SINGLE,
        BUY,
        HOME_AND_HOME;

        static Type parse(String raw) {
            if (raw == null) {
                return BUY;
            }
            switch (raw.trim().toUpperCase()) {
                case "SINGLE":
                case "S":
                    return SINGLE;
                case "HOME_AND_HOME":
                case "HH":
                case "H":
                    return HOME_AND_HOME;
                case "BUY":
                case "B":
                default:
                    return BUY;
            }
        }

        String encode() {
            switch (this) {
                case SINGLE:
                    return "S";
                case HOME_AND_HOME:
                    return "H";
                case BUY:
                default:
                    return "B";
            }
        }
    }

    public final String id;
    public final String teamA;
    public final String teamB;
    public final int startYear;
    public final int lengthYears;
    public final Type type;
    /** Last year by which all games must be played or the deal breaches. */
    public final int mustFulfillByYear;
    /** Cancel fee charged to the cancelling team (recruit money). */
    public final int buyout;
    public final ArrayList<OocContractGame> games;

    public OocContract(
            String id,
            String teamA,
            String teamB,
            int startYear,
            int lengthYears,
            Type type,
            int mustFulfillByYear,
            int buyout,
            List<OocContractGame> games) {
        this.id = id;
        this.teamA = teamA;
        this.teamB = teamB;
        this.startYear = startYear;
        this.lengthYears = lengthYears;
        this.type = type != null ? type : Type.BUY;
        this.mustFulfillByYear = mustFulfillByYear;
        this.buyout = Math.max(0, buyout);
        this.games = new ArrayList<>(games);
    }

    public boolean involves(String abbr) {
        return teamA.equals(abbr) || teamB.equals(abbr);
    }

    public OocContractGame gameForYear(int year) {
        for (OocContractGame g : games) {
            if (g.year == year) {
                return g;
            }
        }
        return null;
    }

    public int maxGameYear() {
        int max = startYear;
        for (OocContractGame g : games) {
            if (g.year > max) {
                max = g.year;
            }
        }
        return max;
    }

    public int remainingGuaranteeTotal(int currentYear) {
        int sum = 0;
        for (OocContractGame g : games) {
            if (g.year >= currentYear && !g.settled) {
                sum += g.guarantee;
            }
        }
        return sum;
    }

    public boolean isFullySettled() {
        for (OocContractGame g : games) {
            if (!g.settled) {
                return false;
            }
        }
        return true;
    }

    public boolean hasFutureGames(int currentYear) {
        for (OocContractGame g : games) {
            if (g.year >= currentYear && !g.settled) {
                return true;
            }
        }
        return false;
    }

    public boolean hasUnsettledGames() {
        for (OocContractGame g : games) {
            if (!g.settled) {
                return true;
            }
        }
        return false;
    }

    public String encode() {
        StringBuilder sb = new StringBuilder();
        sb.append(id).append(',')
                .append(teamA).append(',')
                .append(teamB).append(',')
                .append(startYear).append(',')
                .append(lengthYears).append(',')
                .append(type.encode()).append(',')
                .append(mustFulfillByYear).append(',')
                .append(buyout).append(',');
        for (int i = 0; i < games.size(); i++) {
            if (i > 0) {
                sb.append('|');
            }
            sb.append(games.get(i).encode());
        }
        return sb.toString();
    }

    public static OocContract parse(String line) {
        // New: id,A,B,start,len,type,fulfillBy,buyout,games
        // Old: id,A,B,start,len,games
        String[] parts = line.split(",", 9);
        if (parts.length >= 9) {
            ArrayList<OocContractGame> games = parseGames(parts[8]);
            return new OocContract(
                    parts[0],
                    parts[1],
                    parts[2],
                    Integer.parseInt(parts[3]),
                    Integer.parseInt(parts[4]),
                    Type.parse(parts[5]),
                    Integer.parseInt(parts[6]),
                    Integer.parseInt(parts[7]),
                    games);
        }
        parts = line.split(",", 6);
        if (parts.length < 6) {
            throw new IllegalArgumentException("Invalid OOC contract line: " + line);
        }
        ArrayList<OocContractGame> games = parseGames(parts[5]);
        Type inferred = inferLegacyType(games);
        int start = Integer.parseInt(parts[3]);
        int len = Integer.parseInt(parts[4]);
        int maxYear = start;
        for (OocContractGame g : games) {
            if (g.year > maxYear) {
                maxYear = g.year;
            }
        }
        int remaining = 0;
        for (OocContractGame g : games) {
            if (!g.settled) {
                remaining += g.guarantee;
            }
        }
        int buyout = NilMoney.oocCancelBuyout(inferred, remaining, len);
        return new OocContract(
                parts[0],
                parts[1],
                parts[2],
                start,
                len,
                inferred,
                maxYear,
                buyout,
                games);
    }

    private static ArrayList<OocContractGame> parseGames(String raw) {
        ArrayList<OocContractGame> games = new ArrayList<>();
        if (raw == null || raw.isEmpty()) {
            return games;
        }
        for (String g : raw.split("\\|")) {
            if (!g.isEmpty()) {
                games.add(OocContractGame.parse(g));
            }
        }
        return games;
    }

    private static Type inferLegacyType(List<OocContractGame> games) {
        if (games == null || games.isEmpty()) {
            return Type.BUY;
        }
        if (games.size() == 1) {
            return games.get(0).guarantee > 0 ? Type.BUY : Type.SINGLE;
        }
        boolean anyGuarantee = false;
        for (OocContractGame g : games) {
            if (g.guarantee > 0) {
                anyGuarantee = true;
                break;
            }
        }
        if (!anyGuarantee && games.size() == 2) {
            return Type.HOME_AND_HOME;
        }
        return Type.BUY;
    }
}
