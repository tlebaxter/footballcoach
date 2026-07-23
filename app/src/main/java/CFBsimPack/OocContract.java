package CFBsimPack;

import java.util.ArrayList;
import java.util.List;

/**
 * Multi-year out-of-conference series (buy game or home-and-home).
 */
public final class OocContract {
    public final String id;
    public final String teamA;
    public final String teamB;
    public final int startYear;
    public final int lengthYears;
    public final ArrayList<OocContractGame> games;

    public OocContract(
            String id,
            String teamA,
            String teamB,
            int startYear,
            int lengthYears,
            List<OocContractGame> games) {
        this.id = id;
        this.teamA = teamA;
        this.teamB = teamB;
        this.startYear = startYear;
        this.lengthYears = lengthYears;
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

    public String encode() {
        StringBuilder sb = new StringBuilder();
        sb.append(id).append(',')
                .append(teamA).append(',')
                .append(teamB).append(',')
                .append(startYear).append(',')
                .append(lengthYears).append(',');
        for (int i = 0; i < games.size(); i++) {
            if (i > 0) {
                sb.append('|');
            }
            sb.append(games.get(i).encode());
        }
        return sb.toString();
    }

    public static OocContract parse(String line) {
        String[] parts = line.split(",", 6);
        if (parts.length < 6) {
            throw new IllegalArgumentException("Invalid OOC contract line: " + line);
        }
        ArrayList<OocContractGame> games = new ArrayList<>();
        if (!parts[5].isEmpty()) {
            for (String g : parts[5].split("\\|")) {
                if (!g.isEmpty()) {
                    games.add(OocContractGame.parse(g));
                }
            }
        }
        return new OocContract(
                parts[0],
                parts[1],
                parts[2],
                Integer.parseInt(parts[3]),
                Integer.parseInt(parts[4]),
                games);
    }
}
