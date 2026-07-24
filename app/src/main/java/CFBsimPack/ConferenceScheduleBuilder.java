package CFBsimPack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

    /** 1-based calendar weeks preferred for strongest in-conference rivalries. */
    private static final int RIVALRY_WEEK_MIN = 8;
    private static final int RIVALRY_WEEK_MAX = 11;

    public static void schedule(Conference conference) {
        if (!conference.hasChampionship || conference.confTeams.size() < 2) {
            return;
        }

        int scheduledRounds = conferenceGameTarget(conference.confTeams.size());
        int rotationSize = conference.confTeams.size() % 2 == 0
                ? conference.confTeams.size()
                : conference.confTeams.size() + 1;
        int completeRoundRobinRounds = rotationSize - 1;
        Set<Integer> selectedRounds = selectRounds(
                completeRoundRobinRounds,
                scheduledRounds,
                conference.league.leagueHistory.size());
        int[] weekForSlot = conferenceWeeks(scheduledRounds);
        int rivalryRound = pickLateRivalryRound(selectedRounds, weekForSlot);
        ArrayList<Team> rotation = arrangeRivalsForRound(conference.confTeams, rivalryRound);
        boolean reverseHomeField = conference.league.leagueHistory.size() % 2 == 1;
        int scheduleSlot = 0;
        ArrayList<Team> shortTeams = new ArrayList<>();

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
     * Among selected rounds (in schedule order), pick the latest whose mapped week
     * is in calendar weeks {@link #RIVALRY_WEEK_MIN}–{@link #RIVALRY_WEEK_MAX}.
     * Falls back to the last selected round when none fall in that band.
     */
    static int pickLateRivalryRound(Set<Integer> selectedRounds, int[] weekForSlot) {
        ArrayList<Integer> ordered = new ArrayList<>(selectedRounds);
        Collections.sort(ordered);
        if (ordered.isEmpty()) {
            return 0;
        }
        int fallback = ordered.get(ordered.size() - 1);
        int best = -1;
        for (int slot = 0; slot < ordered.size() && slot < weekForSlot.length; slot++) {
            int calendarWeek = weekForSlot[slot] + 1;
            if (calendarWeek >= RIVALRY_WEEK_MIN && calendarWeek <= RIVALRY_WEEK_MAX) {
                best = ordered.get(slot);
            }
        }
        return best >= 0 ? best : fallback;
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

    /**
     * Seats strongest reciprocal in-conf rivals opposite each other for
     * {@code targetRound}, then reverse-rotates so that meeting occurs in that
     * round under the circle method.
     */
    private static ArrayList<Team> arrangeRivalsForRound(List<Team> teams, int targetRound) {
        int rotationSize = teams.size() % 2 == 0 ? teams.size() : teams.size() + 1;
        ArrayList<Team> arranged = new ArrayList<>();
        for (int i = 0; i < rotationSize; i++) {
            arranged.add(null);
        }

        Set<Team> placed = new HashSet<>();
        if (teams.size() % 2 == 1) {
            Team unmatched = findTeamWithoutStrongInConfRival(teams);
            arranged.set(0, unmatched);
            placed.add(unmatched);
        }

        seatStrongReciprocalPairs(teams, arranged, placed, rotationSize);

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

        int rounds = Math.max(0, targetRound);
        for (int i = 0; i < rounds; i++) {
            reverseRotate(arranged);
        }
        return arranged;
    }

    /**
     * Seat reciprocal same-conf pairs with min strength ≥ {@link Rivalry#SEAT_THRESHOLD},
     * strongest pairs first.
     */
    private static void seatStrongReciprocalPairs(
            List<Team> teams,
            ArrayList<Team> arranged,
            Set<Team> placed,
            int rotationSize) {
        ArrayList<int[]> pairs = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Team team : teams) {
            if (team.rivalries == null) {
                continue;
            }
            for (Rivalry r : team.rivalries) {
                Team rival = findTeamByAbbreviation(teams, r.opponentAbbr);
                if (rival == null) {
                    continue;
                }
                Rivalry back = rival.rivalryWith(team.abbr);
                if (back == null) {
                    continue;
                }
                int min = Math.min(r.strength, back.strength);
                if (min < Rivalry.SEAT_THRESHOLD) {
                    continue;
                }
                String key = team.abbr.compareTo(rival.abbr) < 0
                        ? team.abbr + "-" + rival.abbr
                        : rival.abbr + "-" + team.abbr;
                if (!seen.add(key)) {
                    continue;
                }
                pairs.add(new int[] {
                        min,
                        teams.indexOf(team),
                        teams.indexOf(rival)
                });
            }
        }
        Collections.sort(pairs, new Comparator<int[]>() {
            @Override
            public int compare(int[] a, int[] b) {
                return Integer.compare(b[0], a[0]);
            }
        });

        int pairSlot = 0;
        while (pairSlot < rotationSize / 2 && arranged.get(pairSlot) != null) {
            pairSlot++;
        }
        for (int[] pair : pairs) {
            if (pairSlot >= rotationSize / 2) {
                break;
            }
            Team a = teams.get(pair[1]);
            Team b = teams.get(pair[2]);
            if (placed.contains(a) || placed.contains(b)) {
                continue;
            }
            arranged.set(pairSlot, a);
            arranged.set(rotationSize - 1 - pairSlot, b);
            placed.add(a);
            placed.add(b);
            pairSlot++;
            while (pairSlot < rotationSize / 2 && arranged.get(pairSlot) != null) {
                pairSlot++;
            }
        }
    }

    private static Team findTeamWithoutStrongInConfRival(List<Team> teams) {
        for (Team team : teams) {
            boolean hasStrong = false;
            if (team.rivalries != null) {
                for (Rivalry r : team.rivalries) {
                    Team rival = findTeamByAbbreviation(teams, r.opponentAbbr);
                    if (rival == null) {
                        continue;
                    }
                    Rivalry back = rival.rivalryWith(team.abbr);
                    if (back != null
                            && Math.min(r.strength, back.strength) >= Rivalry.SEAT_THRESHOLD) {
                        hasStrong = true;
                        break;
                    }
                }
            }
            if (!hasStrong) {
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

    /** Inverse of {@link #rotate}: move index-1 team to the end. */
    private static void reverseRotate(ArrayList<Team> rotation) {
        if (rotation.size() < 2) {
            return;
        }
        Team moved = rotation.remove(1);
        rotation.add(moved);
    }
}
