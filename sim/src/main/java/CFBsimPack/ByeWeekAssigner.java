package CFBsimPack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Assigns each team one immovable bye among its open weeks.
 *
 * Goals (close to real CFB, still schedulable):
 * <ul>
 *   <li>Stagger byes inside a conference (few teammates share a bye week)</li>
 *   <li>Same calendar week may host byes from different conferences</li>
 *   <li>Each week keeps an even count of teams still needing a game</li>
 *   <li>No conference exceeds half of a week's available OOC pool</li>
 *   <li>Non-independents prefer byeing on busy open weeks (flex), preserving
 *       sparse odd-conference holes for independent OOC partners</li>
 * </ul>
 */
public final class ByeWeekAssigner {

    private static final int MAX_ATTEMPTS = 80;
    /** Soft cap — enough for Pac-12 extras (need ~4 byes on a 12-team open week). */
    private static final int MAX_SAME_CONF_BYES_PER_WEEK = 4;
    private static final int SPARSE_OPEN_THRESHOLD = 8;

    private ByeWeekAssigner() {
    }

    public static void assign(List<Team> teams) {
        String lastReason = "unknown";
        int[] openDensity = computeOpenDensity(teams);
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            clearByes(teams);
            proposeStaggeredByes(teams, attempt, openDensity);
            if (!fixEvenByeCounts(teams, attempt)) {
                lastReason = "could not repair odd bye counts on attempt " + attempt;
                continue;
            }
            if (!repairMatchability(teams, attempt, openDensity)) {
                lastReason = matchabilityFailure(teams);
                if (lastReason == null) {
                    lastReason = "repair failed on attempt " + attempt;
                }
                continue;
            }
            return;
        }
        throw new IllegalStateException(
                "Unable to assign bye weeks with even OOC availability: " + lastReason);
    }

    private static int[] computeOpenDensity(List<Team> teams) {
        int[] density = new int[League.REGULAR_SEASON_WEEKS];
        for (Team team : teams) {
            for (int week = 0; week < League.REGULAR_SEASON_WEEKS; week++) {
                if (team.gameSchedule.get(week) == null) {
                    density[week]++;
                }
            }
        }
        return density;
    }

    private static void proposeStaggeredByes(List<Team> teams, int attempt, int[] openDensity) {
        Map<String, ArrayList<Team>> byConf = groupByConference(teams);
        ArrayList<String> confOrder = new ArrayList<>(byConf.keySet());
        Collections.sort(confOrder, new RotatingStringOrder(attempt));
        if (confOrder.remove("Independents")) {
            confOrder.add(0, "Independents");
        }

        Map<Integer, Integer> globalByeCounts = new HashMap<>();
        // Pre-assign byes on weeks a conference would dominate the OOC pool.
        for (String conf : confOrder) {
            if ("Independents".equals(conf)) {
                continue;
            }
            preAssignDominantWeeks(byConf.get(conf), conf, teams, openDensity, globalByeCounts, attempt);
        }

        for (String conf : confOrder) {
            ArrayList<Team> confTeams = byConf.get(conf);
            Collections.sort(confTeams, new RotatingTeamOrder(attempt));
            Map<Integer, Integer> confByeCounts = new HashMap<>();
            for (Team team : confTeams) {
                if (team.byeWeek >= 0) {
                    bump(confByeCounts, team.byeWeek);
                    continue;
                }
                boolean independent = "Independents".equals(conf);
                int week = chooseByeWeek(
                        team,
                        confByeCounts,
                        globalByeCounts,
                        openDensity,
                        independent,
                        confTeams.size(),
                        attempt);
                team.byeWeek = week;
                bump(confByeCounts, week);
                bump(globalByeCounts, week);
            }
            if (!"Independents".equals(conf)) {
                rebalanceConferenceByes(confTeams, teams, confByeCounts, attempt);
            }
        }
    }

    /**
     * Spread in-conference byes toward even use of shared open weeks. Swaps with
     * any league team on the lighter week so global bye counts stay unchanged.
     */
    private static void rebalanceConferenceByes(
            ArrayList<Team> confTeams,
            List<Team> allTeams,
            Map<Integer, Integer> confByeCounts,
            int attempt) {
        int softCap = softConfByeCap(confTeams.size());
        for (int guard = 0; guard < confTeams.size() * 6; guard++) {
            int heavyWeek = heaviestConfByeWeek(confByeCounts);
            if (heavyWeek < 0) {
                return;
            }
            int heavyCount = confByeCounts.get(heavyWeek);
            if (heavyCount <= softCap) {
                return;
            }
            int lightWeek = lightestOpenWeek(confTeams, confByeCounts);
            if (lightWeek < 0 || lightWeek == heavyWeek) {
                return;
            }
            int lightCount = confByeCounts.containsKey(lightWeek) ? confByeCounts.get(lightWeek) : 0;
            if (heavyCount - lightCount <= 1) {
                return;
            }

            ArrayList<Team> heavyTeams = teamsOnBye(confTeams, heavyWeek);
            Collections.sort(heavyTeams, new RotatingTeamOrder(attempt + guard));
            boolean moved = false;
            for (Team mover : heavyTeams) {
                if (mover.gameSchedule.get(lightWeek) != null) {
                    continue;
                }
                Team partner = null;
                for (Team candidate : allTeams) {
                    if (candidate == mover
                            || candidate.byeWeek != lightWeek
                            || candidate.conference.equals(mover.conference)) {
                        continue;
                    }
                    if (candidate.gameSchedule.get(heavyWeek) == null) {
                        partner = candidate;
                        break;
                    }
                }
                if (partner != null) {
                    int partnerOld = partner.byeWeek;
                    int moverOld = mover.byeWeek;
                    partner.byeWeek = heavyWeek;
                    mover.byeWeek = lightWeek;
                    if (matchabilityFailure(allTeams) != null) {
                        partner.byeWeek = partnerOld;
                        mover.byeWeek = moverOld;
                    } else {
                        confByeCounts.put(heavyWeek, heavyCount - 1);
                        bump(confByeCounts, lightWeek);
                        moved = true;
                        break;
                    }
                }
                // One-way move when both weeks remain even and matchable.
                int[] global = byeCountArray(allTeams);
                if ((global[heavyWeek] - 1) % 2 == 0 && (global[lightWeek] + 1) % 2 == 0) {
                    int moverOld = mover.byeWeek;
                    mover.byeWeek = lightWeek;
                    if (matchabilityFailure(allTeams) != null) {
                        mover.byeWeek = moverOld;
                    } else {
                        confByeCounts.put(heavyWeek, heavyCount - 1);
                        bump(confByeCounts, lightWeek);
                        moved = true;
                        break;
                    }
                }
            }
            if (!moved) {
                return;
            }
        }
    }

    private static int softConfByeCap(int confSize) {
        // Ideal spread across 4 flex weeks (9-game conferences).
        int ideal = (confSize + 3) / 4;
        return Math.max(2, Math.min(MAX_SAME_CONF_BYES_PER_WEEK, ideal));
    }

    private static int heaviestConfByeWeek(Map<Integer, Integer> confByeCounts) {
        int bestWeek = -1;
        int bestCount = -1;
        for (Map.Entry<Integer, Integer> entry : confByeCounts.entrySet()) {
            if (entry.getValue() > bestCount) {
                bestCount = entry.getValue();
                bestWeek = entry.getKey();
            }
        }
        return bestWeek;
    }

    private static int lightestOpenWeek(ArrayList<Team> confTeams, Map<Integer, Integer> confByeCounts) {
        int bestWeek = -1;
        int bestCount = Integer.MAX_VALUE;
        for (Team team : confTeams) {
            for (int week : openWeeks(team)) {
                int count = confByeCounts.containsKey(week) ? confByeCounts.get(week) : 0;
                if (count < bestCount) {
                    bestCount = count;
                    bestWeek = week;
                }
            }
        }
        return bestWeek;
    }

    private static int[] byeCountArray(List<Team> teams) {
        int[] counts = new int[League.REGULAR_SEASON_WEEKS];
        for (Team team : teams) {
            if (team.byeWeek >= 0) {
                counts[team.byeWeek]++;
            }
        }
        return counts;
    }

    /**
     * If conference C has confOpen teams free on week W and
     * 2*confOpen - totalOpen > 0, at least that many C teams must bye on W
     * or OOC matching is impossible. Pre-assign an even number of those byes.
     */
    private static void preAssignDominantWeeks(
            ArrayList<Team> confTeams,
            String conf,
            List<Team> allTeams,
            int[] openDensity,
            Map<Integer, Integer> globalByeCounts,
            int attempt) {
        Collections.sort(confTeams, new RotatingTeamOrder(attempt + 11));
        for (int week = 0; week < League.REGULAR_SEASON_WEEKS; week++) {
            int confOpen = 0;
            ArrayList<Team> candidates = new ArrayList<>();
            for (Team team : confTeams) {
                if (team.byeWeek < 0 && team.gameSchedule.get(week) == null) {
                    confOpen++;
                    candidates.add(team);
                }
            }
            int totalOpen = openDensity[week];
            int minByes = 2 * confOpen - totalOpen;
            if (minByes <= 0) {
                continue;
            }
            if (minByes % 2 != 0) {
                minByes++;
            }
            minByes = Math.min(minByes, evenFloor(candidates.size()));
            for (int i = 0; i < minByes; i++) {
                Team team = candidates.get(i);
                team.byeWeek = week;
                bump(globalByeCounts, week);
            }
        }
    }

    private static int evenFloor(int value) {
        return value - (value % 2);
    }

    private static int chooseByeWeek(
            Team team,
            Map<Integer, Integer> confByeCounts,
            Map<Integer, Integer> globalByeCounts,
            int[] openDensity,
            boolean independent,
            int confSize,
            int attempt) {
        ArrayList<Integer> opens = openWeeks(team);
        if (opens.isEmpty()) {
            throw new IllegalStateException("Team " + team.abbr + " has no open week for a bye.");
        }
        final int confCap = independent
                ? MAX_SAME_CONF_BYES_PER_WEEK
                : softConfByeCap(confSize);
        Collections.sort(opens, new Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                // Keep non-independents off sparse odd-conf holes; independents prefer them.
                // Among non-sparse weeks, do not rank by tiny density gaps (catch-up noise) —
                // that collapses large conferences onto the same 3 flex weeks.
                if (independent) {
                    if (openDensity[a] != openDensity[b]) {
                        return Integer.compare(openDensity[a], openDensity[b]);
                    }
                } else {
                    boolean aSparse = openDensity[a] < SPARSE_OPEN_THRESHOLD;
                    boolean bSparse = openDensity[b] < SPARSE_OPEN_THRESHOLD;
                    if (aSparse != bSparse) {
                        return aSparse ? 1 : -1;
                    }
                }

                // Stagger within conference (real CFB spreads byes across the calendar).
                int ca = confByeCounts.containsKey(a) ? confByeCounts.get(a) : 0;
                int cb = confByeCounts.containsKey(b) ? confByeCounts.get(b) : 0;
                boolean aOver = ca >= confCap;
                boolean bOver = cb >= confCap;
                if (aOver != bOver) {
                    return aOver ? 1 : -1;
                }
                if (ca != cb) {
                    return Integer.compare(ca, cb);
                }

                int ga = globalByeCounts.containsKey(a) ? globalByeCounts.get(a) : 0;
                int gb = globalByeCounts.containsKey(b) ? globalByeCounts.get(b) : 0;
                if (ga != gb) {
                    return Integer.compare(ga, gb);
                }
                int ao = Math.floorMod(team.abbr.hashCode() + a * 31 + attempt * 17, 997);
                int bo = Math.floorMod(team.abbr.hashCode() + b * 31 + attempt * 17, 997);
                if (ao != bo) {
                    return Integer.compare(ao, bo);
                }
                return Integer.compare(a, b);
            }
        });
        return opens.get(0);
    }

    private static boolean fixEvenByeCounts(List<Team> teams, int attempt) {
        for (int guard = 0; guard < teams.size() * 4; guard++) {
            ArrayList<Integer> oddWeeks = new ArrayList<>();
            int[] byeCounts = new int[League.REGULAR_SEASON_WEEKS];
            for (Team team : teams) {
                if (team.byeWeek >= 0) {
                    byeCounts[team.byeWeek]++;
                }
            }
            for (int week = 0; week < League.REGULAR_SEASON_WEEKS; week++) {
                if (byeCounts[week] % 2 != 0) {
                    oddWeeks.add(week);
                }
            }
            if (oddWeeks.isEmpty()) {
                return true;
            }
            if (oddWeeks.size() == 1) {
                return false;
            }

            Collections.sort(oddWeeks, new RotatingWeekOrder(attempt + guard));
            int fromWeek = oddWeeks.get(0);
            boolean moved = false;
            ArrayList<Team> movers = teamsOnBye(teams, fromWeek);
            Collections.sort(movers, new RotatingTeamOrder(attempt + guard));
            for (Team mover : movers) {
                for (int i = 1; i < oddWeeks.size(); i++) {
                    int toWeek = oddWeeks.get(i);
                    if (mover.gameSchedule.get(toWeek) == null && mover.byeWeek != toWeek) {
                        mover.byeWeek = toWeek;
                        moved = true;
                        break;
                    }
                }
                if (moved) {
                    break;
                }
            }
            if (!moved) {
                for (Team mover : movers) {
                    for (int toWeek = 0; toWeek < League.REGULAR_SEASON_WEEKS; toWeek++) {
                        if (toWeek == fromWeek || byeCounts[toWeek] % 2 != 0) {
                            continue;
                        }
                        if (mover.gameSchedule.get(toWeek) == null) {
                            mover.byeWeek = toWeek;
                            moved = true;
                            break;
                        }
                    }
                    if (moved) {
                        break;
                    }
                }
            }
            if (!moved) {
                return false;
            }
        }
        return false;
    }

    /**
     * Move byes off weeks that fail OOC matchability, preferring dense destinations.
     */
    private static boolean repairMatchability(List<Team> teams, int attempt, int[] openDensity) {
        for (int guard = 0; guard < teams.size() * 6; guard++) {
            String failure = matchabilityFailure(teams);
            if (failure == null) {
                return true;
            }
            int badWeek = parseWeek(failure);
            if (badWeek < 0) {
                return false;
            }
            ArrayList<Team> byeing = teamsOnBye(teams, badWeek);
            // Also consider moving a non-bye team onto this week (as bye) only when
            // the week is odd — not needed if evenness already holds.
            // Primary fix: move a byeing non-independent OFF this week if the week is sparse,
            // or move an independent ONTO this week if they're available elsewhere.
            boolean moved = false;

            // If week is dominated / stranded, try moving byes away from this week first
            // for teams that have denser alternate open weeks.
            Collections.sort(byeing, new RotatingTeamOrder(attempt + guard));
            for (Team mover : byeing) {
                int dest = bestAlternateBye(mover, badWeek, teams, openDensity, attempt + guard);
                if (dest >= 0) {
                    int old = mover.byeWeek;
                    mover.byeWeek = dest;
                    // Keep global bye parity: if dest count becomes wrong, swap with someone on dest
                    if (!byeCountsEven(teams)) {
                        Team swap = findByeSwapPartner(teams, dest, old, mover);
                        if (swap != null) {
                            swap.byeWeek = old;
                            moved = true;
                            break;
                        }
                        mover.byeWeek = old;
                    } else {
                        moved = true;
                        break;
                    }
                }
            }

            if (!moved) {
                // Try giving an independent a bye on the bad week (if open) by swapping
                // with someone currently byeing there.
                for (Team ind : teams) {
                    if (!"Independents".equals(ind.conference) || ind.byeWeek == badWeek) {
                        continue;
                    }
                    if (ind.gameSchedule.get(badWeek) != null) {
                        continue;
                    }
                    if (byeing.isEmpty()) {
                        continue;
                    }
                    Team other = byeing.get(0);
                    int indOld = ind.byeWeek;
                    // other moves to ind's old bye if open; ind takes badWeek
                    if (other.gameSchedule.get(indOld) == null) {
                        other.byeWeek = indOld;
                        ind.byeWeek = badWeek;
                        if (byeCountsEven(teams) && matchabilityFailure(teams) == null) {
                            return true;
                        }
                        // keep if even and continue repairs
                        if (byeCountsEven(teams)) {
                            moved = true;
                            break;
                        }
                        other.byeWeek = badWeek;
                        ind.byeWeek = indOld;
                    }
                }
            }

            if (!moved) {
                return false;
            }
        }
        return matchabilityFailure(teams) == null;
    }

    private static int bestAlternateBye(
            Team team,
            int avoidWeek,
            List<Team> teams,
            int[] openDensity,
            int attempt) {
        ArrayList<Integer> opens = openWeeks(team);
        int best = -1;
        int bestScore = Integer.MIN_VALUE;
        for (int week : opens) {
            if (week == avoidWeek) {
                continue;
            }
            int confByes = 0;
            for (Team other : teams) {
                if (other != team
                        && other.byeWeek == week
                        && other.conference.equals(team.conference)) {
                    confByes++;
                }
            }
            int score = openDensity[week] * 10 - confByes * 50
                    + Math.floorMod(team.abbr.hashCode() + week * 31 + attempt, 7);
            if (score > bestScore) {
                bestScore = score;
                best = week;
            }
        }
        return best;
    }

    private static Team findByeSwapPartner(List<Team> teams, int fromWeek, int toWeek, Team exclude) {
        for (Team team : teams) {
            if (team == exclude || team.byeWeek != fromWeek) {
                continue;
            }
            if (team.gameSchedule.get(toWeek) == null) {
                return team;
            }
        }
        return null;
    }

    private static boolean byeCountsEven(List<Team> teams) {
        int[] byeCounts = new int[League.REGULAR_SEASON_WEEKS];
        for (Team team : teams) {
            if (team.byeWeek >= 0) {
                byeCounts[team.byeWeek]++;
            }
        }
        for (int count : byeCounts) {
            if (count % 2 != 0) {
                return false;
            }
        }
        return true;
    }

    private static int parseWeek(String failure) {
        if (failure == null || !failure.startsWith("week ")) {
            return -1;
        }
        int end = failure.indexOf(' ', 5);
        if (end < 0) {
            end = failure.length();
        }
        try {
            return Integer.parseInt(failure.substring(5, end));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static ArrayList<Team> teamsOnBye(List<Team> teams, int week) {
        ArrayList<Team> list = new ArrayList<>();
        for (Team team : teams) {
            if (team.byeWeek == week) {
                list.add(team);
            }
        }
        return list;
    }

    private static String matchabilityFailure(List<Team> teams) {
        for (int week = 0; week < League.REGULAR_SEASON_WEEKS; week++) {
            Map<String, Integer> byConf = new LinkedHashMap<>();
            int available = 0;
            for (Team team : teams) {
                if (team.gameSchedule.get(week) == null && team.byeWeek != week) {
                    available++;
                    Integer count = byConf.get(team.conference);
                    byConf.put(team.conference, count == null ? 1 : count + 1);
                }
            }
            if (available == 0) {
                continue;
            }
            if (available % 2 != 0) {
                return "week " + week + " odd available=" + available + " byConf=" + byConf;
            }
            for (Map.Entry<String, Integer> entry : byConf.entrySet()) {
                if (entry.getValue() > available / 2) {
                    return "week " + week + " conf " + entry.getKey() + "=" + entry.getValue()
                            + " of " + available + " byConf=" + byConf;
                }
            }
        }
        return null;
    }

    private static Map<String, ArrayList<Team>> groupByConference(List<Team> teams) {
        Map<String, ArrayList<Team>> byConf = new LinkedHashMap<>();
        for (Team team : teams) {
            ArrayList<Team> list = byConf.get(team.conference);
            if (list == null) {
                list = new ArrayList<>();
                byConf.put(team.conference, list);
            }
            list.add(team);
        }
        return byConf;
    }

    private static void clearByes(List<Team> teams) {
        for (Team team : teams) {
            team.byeWeek = -1;
        }
    }

    private static ArrayList<Integer> openWeeks(Team team) {
        ArrayList<Integer> weeks = new ArrayList<>();
        for (int week = 0; week < League.REGULAR_SEASON_WEEKS; week++) {
            if (team.gameSchedule.get(week) == null) {
                weeks.add(week);
            }
        }
        return weeks;
    }

    private static void bump(Map<Integer, Integer> counts, int week) {
        Integer current = counts.get(week);
        counts.put(week, current == null ? 1 : current + 1);
    }

    private static final class RotatingTeamOrder implements Comparator<Team> {
        private final int attempt;

        private RotatingTeamOrder(int attempt) {
            this.attempt = attempt;
        }

        @Override
        public int compare(Team a, Team b) {
            int ah = Math.floorMod(a.abbr.hashCode() + attempt * 41, 997);
            int bh = Math.floorMod(b.abbr.hashCode() + attempt * 41, 997);
            if (ah != bh) {
                return Integer.compare(ah, bh);
            }
            return a.abbr.compareTo(b.abbr);
        }
    }

    private static final class RotatingStringOrder implements Comparator<String> {
        private final int attempt;

        private RotatingStringOrder(int attempt) {
            this.attempt = attempt;
        }

        @Override
        public int compare(String a, String b) {
            int ah = Math.floorMod(a.hashCode() + attempt * 13, 997);
            int bh = Math.floorMod(b.hashCode() + attempt * 13, 997);
            if (ah != bh) {
                return Integer.compare(ah, bh);
            }
            return a.compareTo(b);
        }
    }

    private static final class RotatingWeekOrder implements Comparator<Integer> {
        private final int seed;

        private RotatingWeekOrder(int seed) {
            this.seed = seed;
        }

        @Override
        public int compare(Integer a, Integer b) {
            int ao = Math.floorMod(a * 31 + seed * 17, 997);
            int bo = Math.floorMod(b * 31 + seed * 17, 997);
            if (ao != bo) {
                return Integer.compare(ao, bo);
            }
            return Integer.compare(a, b);
        }
    }
}
