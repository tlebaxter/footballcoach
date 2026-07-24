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
     * Places an OOC game with a fixed home team (contract materialization / buy games).
     */
    public static boolean placeFixedHomeOocGame(Team home, Team away, int week, String contractId) {
        if (home == null || away == null || home == away) {
            return false;
        }
        if (!home.isOpenOocWeek(week) || !away.isOpenOocWeek(week)) {
            return false;
        }
        if (home.conference.equals(away.conference)) {
            return false;
        }
        if (alreadyMatched(home, away)) {
            return false;
        }
        Game game = new Game(home, away, "OOC");
        game.contractId = contractId;
        home.gameSchedule.set(week, game);
        away.gameSchedule.set(week, game);
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

    /** Clears editable free OOC games; skips contract-locked weeks. */
    public static boolean clearUserFreeOocGame(Team user, int week) {
        if (user == null || week < 0 || week >= user.gameSchedule.size()) {
            return false;
        }
        Game game = user.gameSchedule.get(week);
        if (game != null && game.contractId != null) {
            return false;
        }
        return clearUserOocGame(user, week);
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

    /**
     * Clears every editable OOC game on the user's schedule.
     */
    public static void clearAllUserOocGames(Team user) {
        if (user == null) {
            return;
        }
        for (int week = 0; week < League.REGULAR_SEASON_WEEKS; week++) {
            clearUserFreeOocGame(user, week);
        }
    }

    /**
     * Fills the user's currently open OOC weeks with a balanced program-tier slate.
     * Soft-prefers highest cross-conference rival (strength ≥ 50) when a shared
     * open week exists (never required). Then rotates tough / peer / easy by schedule tier.
     *
     * @return number of games placed
     */
    public static int suggestUserOocSchedule(Team user, List<Team> allTeams) {
        if (user == null || allTeams == null) {
            return 0;
        }
        int placed = 0;

        Team rival = highestCrossConfRival(user, allTeams);
        if (rival != null
                && !alreadyMatched(user, rival)) {
            int rivalryWeek = findSharedOpenWeek(user, rival);
            if (rivalryWeek >= 0 && placeUserOocGame(user, rival, rivalryWeek)) {
                placed++;
            }
        }

        ArrayList<Integer> openWeeks = new ArrayList<>();
        for (int week = 0; week < League.REGULAR_SEASON_WEEKS; week++) {
            if (user.isOpenOocWeek(week)) {
                openWeeks.add(week);
            }
        }

        int bandIndex = 0;
        for (int week : openWeeks) {
            List<Team> eligible = eligibleOpponents(user, week, allTeams);
            if (eligible.isEmpty()) {
                continue;
            }
            Team pick = pickBalancedOpponent(user, eligible, bandIndex % 3);
            if (pick != null && placeUserOocGame(user, pick, week)) {
                placed++;
                bandIndex++;
            }
        }
        return placed;
    }

    private static final int SCHEDULE_TIER_BAND = 8;

    private static int findSharedOpenWeek(Team user, Team opponent) {
        for (int week = 0; week < League.REGULAR_SEASON_WEEKS; week++) {
            if (user.isOpenOocWeek(week) && opponent.isOpenOocWeek(week)) {
                return week;
            }
        }
        return -1;
    }

    /**
     * @param bandIndex 0 = tough, 1 = peer, 2 = easy
     */
    private static Team pickBalancedOpponent(Team user, List<Team> eligible, int bandIndex) {
        Team bestInBand = null;
        int bestInBandDist = Integer.MAX_VALUE;
        Team bestOverall = null;
        int bestOverallDist = Integer.MAX_VALUE;

        for (Team candidate : eligible) {
            int diff = candidate.programProfile.scheduleTier - user.programProfile.scheduleTier;
            int dist = Math.abs(diff);
            boolean inBand;
            if (bandIndex == 0) {
                inBand = diff > SCHEDULE_TIER_BAND;
            } else if (bandIndex == 2) {
                inBand = diff < -SCHEDULE_TIER_BAND;
            } else {
                inBand = dist <= SCHEDULE_TIER_BAND;
            }

            if (inBand && isBetterPick(candidate, dist, bestInBand, bestInBandDist)) {
                bestInBand = candidate;
                bestInBandDist = dist;
            }
            if (isBetterPick(candidate, dist, bestOverall, bestOverallDist)) {
                bestOverall = candidate;
                bestOverallDist = dist;
            }
        }
        return bestInBand != null ? bestInBand : bestOverall;
    }

    private static boolean isBetterPick(Team candidate, int dist, Team current, int currentDist) {
        if (current == null) {
            return true;
        }
        if (dist != currentDist) {
            return dist < currentDist;
        }
        return candidate.name.compareToIgnoreCase(current.name) < 0;
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
        return game.gameName.equals("OOC")
                || game.gameName.equals("OOC Rivalry")
                || game.gameName.equals("Rivalry Game OOC");
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

    /** Highest-strength cross-conference rival at or above seat threshold. */
    private static Team highestCrossConfRival(Team user, List<Team> allTeams) {
        if (user.rivalries == null) {
            return null;
        }
        Team best = null;
        int bestStrength = Rivalry.SEAT_THRESHOLD - 1;
        for (Rivalry r : user.rivalries) {
            if (r.strength < Rivalry.SEAT_THRESHOLD) {
                continue;
            }
            Team rival = findByAbbreviation(allTeams, r.opponentAbbr);
            if (rival == null || user.conference.equals(rival.conference)) {
                continue;
            }
            if (r.strength > bestStrength) {
                best = rival;
                bestStrength = r.strength;
            }
        }
        return best;
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
