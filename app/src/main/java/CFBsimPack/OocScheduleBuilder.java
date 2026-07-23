package CFBsimPack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fills open regular-season weeks (excluding byes) with out-of-conference games.
 * Honors any games already placed (e.g. user OOC picks).
 */
public final class OocScheduleBuilder {

    private OocScheduleBuilder() {
    }

    /**
     * Completes the OOC schedule for all teams, skipping bye weeks and existing games.
     */
    public static void scheduleRemaining(List<Team> teams) {
        Set<String> usedMatchups = collectScheduledMatchups(teams);
        Map<Team, Integer> homeGames = countHomeGames(teams);

        for (int week = 0; week < League.REGULAR_SEASON_WEEKS; week++) {
            ArrayList<Team> available = new ArrayList<>();
            for (Team team : teams) {
                if (team.isOpenOocWeek(week)) {
                    available.add(team);
                }
            }
            if (available.isEmpty()) {
                continue;
            }
            if (available.size() % 2 != 0) {
                throw new IllegalStateException(
                        "Cannot schedule OOC week " + week + ": odd number of available teams.");
            }

            ArrayList<Matchup> matchups = new ArrayList<>();
            reserveAvailableRivalries(available, usedMatchups, matchups);
            if (!matchRemainingTeams(available, usedMatchups, matchups, week)) {
                Map<String, Integer> byConf = new HashMap<>();
                // Rebuild available for error context
                ArrayList<Team> stillOpen = new ArrayList<>();
                for (Team team : teams) {
                    if (team.isOpenOocWeek(week)) {
                        stillOpen.add(team);
                        Integer count = byConf.get(team.conference);
                        byConf.put(team.conference, count == null ? 1 : count + 1);
                    }
                }
                throw new IllegalStateException(
                        "Unable to create a complete OOC schedule for week " + week
                                + " available=" + stillOpen.size() + " byConf=" + byConf);
            }

            for (Matchup matchup : matchups) {
                placeOocGame(matchup.first, matchup.second, week, homeGames, usedMatchups);
            }
        }
    }

    /**
     * Places a user-selected OOC game. Returns false if the matchup/week is illegal.
     */
    public static boolean placeUserOocGame(Team user, Team opponent, int week) {
        if (user == null || opponent == null || user == opponent) {
            return false;
        }
        if (!user.isOpenOocWeek(week) || !opponent.isOpenOocWeek(week)) {
            return false;
        }
        if (user.conference.equals(opponent.conference)) {
            return false;
        }
        if (alreadyMatched(user, opponent)) {
            return false;
        }
        Map<Team, Integer> homeGames = new HashMap<>();
        homeGames.put(user, countHomeGamesFor(user));
        homeGames.put(opponent, countHomeGamesFor(opponent));
        Set<String> used = new HashSet<>();
        placeOocGame(user, opponent, week, homeGames, used);
        return true;
    }

    /**
     * Clears a previously placed OOC game for the user in {@code week}, if editable.
     */
    public static boolean clearUserOocGame(Team user, int week) {
        if (user == null || week < 0 || week >= user.gameSchedule.size() || user.isByeWeek(week)) {
            return false;
        }
        Game game = user.gameSchedule.get(week);
        if (game == null) {
            return true;
        }
        if (!isOocGame(game)) {
            return false;
        }
        Team opponent = game.homeTeam == user ? game.awayTeam : game.homeTeam;
        user.gameSchedule.set(week, null);
        opponent.gameSchedule.set(week, null);
        return true;
    }

    public static List<Team> eligibleOpponents(Team user, int week, List<Team> allTeams) {
        ArrayList<Team> eligible = new ArrayList<>();
        if (user == null || !user.isOpenOocWeek(week)) {
            return eligible;
        }
        for (Team candidate : allTeams) {
            if (candidate == user) {
                continue;
            }
            if (!candidate.isOpenOocWeek(week)) {
                continue;
            }
            if (candidate.conference.equals(user.conference)) {
                continue;
            }
            if (alreadyMatched(user, candidate)) {
                continue;
            }
            eligible.add(candidate);
        }
        Collections.sort(eligible, new Comparator<Team>() {
            @Override
            public int compare(Team a, Team b) {
                return a.name.compareToIgnoreCase(b.name);
            }
        });
        return eligible;
    }

    private static void placeOocGame(
            Team first,
            Team second,
            int week,
            Map<Team, Integer> homeGames,
            Set<String> usedMatchups) {
        Team home = chooseHomeTeam(first, second, homeGames, week);
        Team away = home == first ? second : first;
        Game game = new Game(home, away, "OOC");
        home.gameSchedule.set(week, game);
        away.gameSchedule.set(week, game);
        homeGames.put(home, homeGames.get(home) + 1);
        usedMatchups.add(matchupKey(home, away));
    }

    private static boolean isOocGame(Game game) {
        return game.gameName.equals("OOC") || game.gameName.equals("OOC Rivalry");
    }

    private static boolean alreadyMatched(Team first, Team second) {
        for (Game game : first.gameSchedule) {
            if (game == null) {
                continue;
            }
            Team opponent = game.homeTeam == first ? game.awayTeam : game.homeTeam;
            if (opponent == second) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> collectScheduledMatchups(List<Team> teams) {
        Set<String> matchups = new HashSet<>();
        for (Team team : teams) {
            for (Game game : team.gameSchedule) {
                if (game != null) {
                    matchups.add(matchupKey(game.homeTeam, game.awayTeam));
                }
            }
        }
        return matchups;
    }

    private static Map<Team, Integer> countHomeGames(List<Team> teams) {
        Map<Team, Integer> counts = new HashMap<>();
        for (Team team : teams) {
            counts.put(team, countHomeGamesFor(team));
        }
        return counts;
    }

    private static int countHomeGamesFor(Team team) {
        int count = 0;
        for (Game game : team.gameSchedule) {
            if (game != null && game.homeTeam == team) {
                count++;
            }
        }
        return count;
    }

    private static void reserveAvailableRivalries(
            ArrayList<Team> available,
            Set<String> usedMatchups,
            ArrayList<Matchup> matchups) {
        ArrayList<Team> candidates = new ArrayList<>(available);
        for (Team team : candidates) {
            if (!available.contains(team)) {
                continue;
            }
            Team rival = findByAbbreviation(available, team.rivalTeam);
            if (rival != null
                    && !team.conference.equals(rival.conference)
                    && !usedMatchups.contains(matchupKey(team, rival))) {
                matchups.add(new Matchup(team, rival));
                available.remove(team);
                available.remove(rival);
            }
        }
    }

    private static boolean matchRemainingTeams(
            ArrayList<Team> available,
            Set<String> usedMatchups,
            ArrayList<Matchup> matchups,
            int week) {
        if (available.isEmpty()) {
            return true;
        }

        Team first = teamWithFewestCandidates(available, usedMatchups);
        ArrayList<Team> opponents = legalOpponents(first, available, usedMatchups);
        Collections.sort(opponents, new OpponentOrder(week));

        available.remove(first);
        for (Team opponent : opponents) {
            if (!available.remove(opponent)) {
                continue;
            }
            matchups.add(new Matchup(first, opponent));
            if (matchRemainingTeams(available, usedMatchups, matchups, week)) {
                return true;
            }
            matchups.remove(matchups.size() - 1);
            available.add(opponent);
        }
        available.add(first);
        return false;
    }

    private static Team teamWithFewestCandidates(
            List<Team> available,
            Set<String> usedMatchups) {
        Team selected = available.get(0);
        int fewest = Integer.MAX_VALUE;
        for (Team team : available) {
            int count = legalOpponents(team, available, usedMatchups).size();
            if (count < fewest) {
                selected = team;
                fewest = count;
            }
        }
        return selected;
    }

    private static ArrayList<Team> legalOpponents(
            Team team,
            List<Team> available,
            Set<String> usedMatchups) {
        ArrayList<Team> opponents = new ArrayList<>();
        for (Team candidate : available) {
            if (candidate != team
                    && !candidate.conference.equals(team.conference)
                    && !usedMatchups.contains(matchupKey(team, candidate))) {
                opponents.add(candidate);
            }
        }
        return opponents;
    }

    private static Team findByAbbreviation(List<Team> teams, String abbreviation) {
        for (Team team : teams) {
            if (team.abbr.equals(abbreviation)) {
                return team;
            }
        }
        return null;
    }

    static Team chooseHomeTeam(
            Team first,
            Team second,
            Map<Team, Integer> homeGames,
            int week) {
        int firstHomes = homeGames.containsKey(first) ? homeGames.get(first) : 0;
        int secondHomes = homeGames.containsKey(second) ? homeGames.get(second) : 0;
        if (firstHomes != secondHomes) {
            return firstHomes < secondHomes ? first : second;
        }
        return (first.abbr.hashCode() + week & 1) == 0 ? first : second;
    }

    private static String matchupKey(Team first, Team second) {
        return first.abbr.compareTo(second.abbr) < 0
                ? first.abbr + "-" + second.abbr
                : second.abbr + "-" + first.abbr;
    }

    private static final class Matchup {
        private final Team first;
        private final Team second;

        private Matchup(Team first, Team second) {
            this.first = first;
            this.second = second;
        }
    }

    private static final class OpponentOrder implements Comparator<Team> {
        private final int week;

        private OpponentOrder(int week) {
            this.week = week;
        }

        @Override
        public int compare(Team first, Team second) {
            int firstOrder = Math.floorMod(first.abbr.hashCode() + week * 31, 997);
            int secondOrder = Math.floorMod(second.abbr.hashCode() + week * 31, 997);
            if (firstOrder != secondOrder) {
                return Integer.compare(firstOrder, secondOrder);
            }
            return first.abbr.compareTo(second.abbr);
        }
    }
}
