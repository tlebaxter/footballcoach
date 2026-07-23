package CFBsimPack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds the conference portion of a 13-week schedule for any conference size.
 * Shared flex weeks are left open league-wide so OOC/bye slots are interspersed
 * and remain matchable across conferences.
 */
public final class ConferenceScheduleBuilder {

    private static final int MAX_CONFERENCE_GAMES = 9;
    /**
     * Weeks reserved for OOC/bye across every conference. Sized for the maximum
     * conference load (9 games → 3 OOC + 1 bye = 4 open weeks).
     */
    private static final int[] FLEX_WEEKS = {2, 5, 8, 11};

    private ConferenceScheduleBuilder() {
    }

    public static void schedule(Conference conference) {
        if (!conference.hasChampionship || conference.confTeams.size() < 2) {
            return;
        }

        ArrayList<Team> rotation = arrangeRivalsInOpeningRound(conference.confTeams);
        int scheduledRounds = conferenceGameTarget(conference.confTeams.size());
        int completeRoundRobinRounds = rotation.size() - 1;
        Set<Integer> selectedRounds = selectRounds(
                completeRoundRobinRounds,
                scheduledRounds,
                conference.league.leagueHistory.size());
        boolean reverseHomeField = conference.league.leagueHistory.size() % 2 == 1;
        int scheduleSlot = 0;
        ArrayList<Team> shortTeams = new ArrayList<>();
        int[] weekForSlot = conferenceWeeks(scheduledRounds);

        for (int round = 0; round < completeRoundRobinRounds; round++) {
            if (selectedRounds.contains(round)) {
                Team byeTeam = null;
                for (int pairIndex = 0; pairIndex < rotation.size() / 2; pairIndex++) {
                    Team first = rotation.get(pairIndex);
                    Team second = rotation.get(rotation.size() - 1 - pairIndex);
                    if (first == null || second == null) {
                        byeTeam = first == null ? second : first;
                        continue;
                    }
                    int week = weekForSlot[scheduleSlot];

                    boolean firstIsHome = (round + pairIndex) % 2 == 0;
                    if (reverseHomeField) {
                        firstIsHome = !firstIsHome;
                    }
                    Team home = firstIsHome ? first : second;
                    Team away = firstIsHome ? second : first;
                    placeConferenceGame(home, away, week);
                }
                if (byeTeam != null) {
                    shortTeams.add(byeTeam);
                }
                scheduleSlot++;
            }
            rotate(rotation);
        }

        placeCatchUpGames(shortTeams, conference.league.leagueHistory.size());
    }

    /**
     * Non-flex weeks used for conference games. Conferences with fewer than 9 games
     * leave the same trailing non-flex weeks open so their OOC holes align.
     */
    static int[] conferenceWeeks(int scheduledRounds) {
        ArrayList<Integer> confWeeks = new ArrayList<>();
        Set<Integer> flex = flexWeekSet();
        for (int week = 0; week < League.REGULAR_SEASON_WEEKS; week++) {
            if (!flex.contains(week)) {
                confWeeks.add(week);
            }
        }
        int extras = confWeeks.size() - scheduledRounds;
        if (extras < 0) {
            throw new IllegalStateException(
                    "Not enough non-flex weeks for " + scheduledRounds + " conference games.");
        }
        for (int i = 0; i < extras; i++) {
            confWeeks.remove(confWeeks.size() - 1);
        }
        int[] weeks = new int[scheduledRounds];
        for (int i = 0; i < scheduledRounds; i++) {
            weeks[i] = confWeeks.get(i);
        }
        return weeks;
    }

    static Set<Integer> flexWeekSet() {
        Set<Integer> flex = new HashSet<>();
        for (int week : FLEX_WEEKS) {
            flex.add(week);
        }
        return flex;
    }

    private static void placeCatchUpGames(ArrayList<Team> shortTeams, int seasonIndex) {
        if (shortTeams.size() < 2 || shortTeams.size() % 2 != 0) {
            return;
        }

        ArrayList<Team[]> pairing = findCatchUpPairing(shortTeams);
        if (pairing == null) {
            throw new IllegalStateException(
                    "Unable to find catch-up conference pairing for odd conference.");
        }

        for (int pairIndex = 0; pairIndex < pairing.size(); pairIndex++) {
            Team first = pairing.get(pairIndex)[0];
            Team opponent = pairing.get(pairIndex)[1];
            int week = firstOpenSharedWeek(first, opponent);
            if (week < 0) {
                throw new IllegalStateException(
                        "Unable to find catch-up week for "
                                + first.abbr + " vs " + opponent.abbr);
            }
            boolean firstIsHome = (pairIndex + seasonIndex) % 2 == 0;
            Team home = firstIsHome ? first : opponent;
            Team away = firstIsHome ? opponent : first;
            placeConferenceGame(home, away, week);
        }
    }

    private static ArrayList<Team[]> findCatchUpPairing(ArrayList<Team> shortTeams) {
        for (int start = 0; start < shortTeams.size(); start++) {
            ArrayList<Team> remaining = new ArrayList<>(shortTeams.size());
            for (int offset = 0; offset < shortTeams.size(); offset++) {
                remaining.add(shortTeams.get((start + offset) % shortTeams.size()));
            }
            ArrayList<Team[]> pairing = new ArrayList<>();
            if (searchCatchUpPairing(remaining, pairing)) {
                return pairing;
            }
        }
        return null;
    }

    private static boolean searchCatchUpPairing(
            ArrayList<Team> remaining,
            ArrayList<Team[]> pairing) {
        if (remaining.isEmpty()) {
            return true;
        }

        Team first = remaining.remove(0);
        for (int i = 0; i < remaining.size(); i++) {
            Team candidate = remaining.get(i);
            if (alreadyPlayed(first, candidate) || firstOpenSharedWeek(first, candidate) < 0) {
                continue;
            }
            remaining.remove(i);
            pairing.add(new Team[] {first, candidate});
            if (searchCatchUpPairing(remaining, pairing)) {
                return true;
            }
            pairing.remove(pairing.size() - 1);
            remaining.add(i, candidate);
        }
        remaining.add(0, first);
        return false;
    }

    private static boolean alreadyPlayed(Team first, Team second) {
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

    static int conferenceGameTarget(int conferenceSize) {
        int target = Math.min(MAX_CONFERENCE_GAMES, conferenceSize - 1);
        if (conferenceSize % 2 == 1 && target % 2 == 1) {
            target--;
        }
        return target;
    }

    private static void placeConferenceGame(Team home, Team away, int week) {
        Game game = new Game(home, away, "In Conf");
        home.gameSchedule.set(week, game);
        away.gameSchedule.set(week, game);
        home.evenYearHomeOpp += away.abbr + ",";
    }

    private static int firstOpenSharedWeek(Team first, Team second) {
        // Prefer flex weeks for catch-ups so odd-conference holes remain on
        // non-flex weeks (needed for independents and realistic OOC partners).
        for (int week : FLEX_WEEKS) {
            if (first.gameSchedule.get(week) == null && second.gameSchedule.get(week) == null) {
                return week;
            }
        }
        for (int week = 0; week < League.REGULAR_SEASON_WEEKS; week++) {
            if (first.gameSchedule.get(week) == null && second.gameSchedule.get(week) == null) {
                return week;
            }
        }
        return -1;
    }

    private static Set<Integer> selectRounds(
            int completeRoundRobinRounds,
            int scheduledRounds,
            int seasonIndex) {
        Set<Integer> selected = new HashSet<>();
        if (scheduledRounds >= completeRoundRobinRounds) {
            for (int round = 0; round < completeRoundRobinRounds; round++) {
                selected.add(round);
            }
            return selected;
        }

        selected.add(0);
        int rotatingRoundCount = completeRoundRobinRounds - 1;
        int start = Math.floorMod(seasonIndex * (scheduledRounds - 1), rotatingRoundCount);
        for (int index = 0; index < scheduledRounds - 1; index++) {
            selected.add(1 + (start + index) % rotatingRoundCount);
        }
        return selected;
    }

    private static ArrayList<Team> arrangeRivalsInOpeningRound(List<Team> teams) {
        int rotationSize = teams.size() % 2 == 0 ? teams.size() : teams.size() + 1;
        ArrayList<Team> arranged = new ArrayList<>();
        for (int i = 0; i < rotationSize; i++) {
            arranged.add(null);
        }

        Set<Team> placed = new HashSet<>();
        int pairSlot = teams.size() % 2 == 0 ? 0 : 1;
        if (teams.size() % 2 == 1) {
            Team unmatched = findTeamWithoutInternalReciprocalRival(teams);
            arranged.set(0, unmatched);
            placed.add(unmatched);
        }

        for (Team team : teams) {
            if (placed.contains(team)) {
                continue;
            }
            Team rival = findTeamByAbbreviation(teams, team.rivalTeam);
            if (rival != null
                    && !placed.contains(rival)
                    && team.abbr.equals(rival.rivalTeam)
                    && pairSlot < rotationSize / 2) {
                arranged.set(pairSlot, team);
                arranged.set(rotationSize - 1 - pairSlot, rival);
                placed.add(team);
                placed.add(rival);
                pairSlot++;
            }
        }

        int nextEmpty = 0;
        for (Team team : teams) {
            if (placed.contains(team)) {
                continue;
            }
            while (arranged.get(nextEmpty) != null) {
                nextEmpty++;
            }
            arranged.set(nextEmpty, team);
            placed.add(team);
        }
        return arranged;
    }

    private static Team findTeamWithoutInternalReciprocalRival(List<Team> teams) {
        for (Team team : teams) {
            Team rival = findTeamByAbbreviation(teams, team.rivalTeam);
            if (rival == null || !team.abbr.equals(rival.rivalTeam)) {
                return team;
            }
        }
        return teams.get(0);
    }

    private static Team findTeamByAbbreviation(List<Team> teams, String abbreviation) {
        for (Team team : teams) {
            if (team.abbr.equals(abbreviation)) {
                return team;
            }
        }
        return null;
    }

    private static void rotate(ArrayList<Team> rotation) {
        Team last = rotation.remove(rotation.size() - 1);
        rotation.add(1, last);
    }
}
