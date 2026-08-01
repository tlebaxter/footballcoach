package CFBsimPack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Evolves rivalry strengths and forms new rivalries after each season.
 */
public final class RivalryDynamics {

    private RivalryDynamics() {
    }

    /**
     * Evolve all teams then form new rivalries. Safe to call while schedules are intact.
     */
    public static void applyEndOfSeason(League league) {
        if (league == null || league.teamList == null) {
            return;
        }
        for (Team team : league.teamList) {
            evolveTeam(team);
            team.rivalryDynamicsAppliedThisOffseason = true;
        }
        formNewRivalries(league);
    }

    /**
     * Apply play/decay updates for one team. Call before clearing {@link Team#rivalryResults}.
     * Notes are stored on {@link Team#rivalryDynamicsNotes}.
     */
    public static void evolveTeam(Team team) {
        if (team == null || team.rivalries == null || team.league == null) {
            return;
        }
        if (team.rivalryDynamicsNotes == null) {
            team.rivalryDynamicsNotes = new ArrayList<>();
        } else {
            team.rivalryDynamicsNotes.clear();
        }

        ArrayList<Rivalry> snapshot = new ArrayList<>(team.rivalries);
        for (Rivalry rivalry : snapshot) {
            Team opponent = team.league.findTeamAbbr(rivalry.opponentAbbr);
            if (opponent == null) {
                continue;
            }
            Game game = findPlayedGame(team, opponent);
            int before = rivalry.strength;
            if (game != null && game.hasPlayed) {
                int delta = deltaForPlayedGame(team, opponent, game);
                rivalry.strength = Rivalry.clamp(rivalry.strength + delta);
                mirrorLink(opponent, team, Math.min(rivalry.strength, 50));
                if (rivalry.strength != before) {
                    team.rivalryDynamicsNotes.add(
                            "Rivalry with " + rivalry.opponentAbbr + " "
                                    + (rivalry.strength > before ? "grew" : "cooled")
                                    + " to " + rivalry.strength + " (" + rivalry.band() + ").");
                }
            } else {
                rivalry.strength = Rivalry.clamp(rivalry.strength - 2);
                if (rivalry.strength != before) {
                    team.rivalryDynamicsNotes.add(
                            "Rivalry with " + rivalry.opponentAbbr + " cooled to "
                                    + rivalry.strength + " (idle).");
                }
            }
        }

        Iterator<Rivalry> it = team.rivalries.iterator();
        while (it.hasNext()) {
            Rivalry r = it.next();
            if (r.strength <= 0) {
                team.rivalryDynamicsNotes.add(
                        "Rivalry with " + r.opponentAbbr + " faded away.");
                it.remove();
            }
        }
    }

    /**
     * Preview notes for season summary without mutating state.
     */
    public static List<String> previewNotes(Team team) {
        ArrayList<String> notes = new ArrayList<>();
        if (team == null || team.rivalries == null || team.league == null) {
            return notes;
        }
        for (Rivalry rivalry : team.rivalries) {
            Team opponent = team.league.findTeamAbbr(rivalry.opponentAbbr);
            if (opponent == null) {
                continue;
            }
            Game game = findPlayedGame(team, opponent);
            int projected;
            if (game != null && game.hasPlayed) {
                projected = Rivalry.clamp(
                        rivalry.strength + deltaForPlayedGame(team, opponent, game));
            } else {
                projected = Rivalry.clamp(rivalry.strength - 2);
            }
            if (projected != rivalry.strength) {
                String verb = projected > rivalry.strength ? "grow" : "cool";
                notes.add("Rivalry with " + rivalry.opponentAbbr + " will " + verb
                        + " to " + projected + " (" + Rivalry.band(projected) + ").");
            } else if (projected <= 0) {
                notes.add("Rivalry with " + rivalry.opponentAbbr + " will fade away.");
            }
        }
        return notes;
    }

    /**
     * Form new mutual rivalries from memorable non-rival regular-season games.
     */
    public static void formNewRivalries(League league) {
        if (league == null || league.teamList == null) {
            return;
        }
        Set<String> seen = new HashSet<>();
        for (Team team : league.teamList) {
            if (team.gameSchedule == null || team.gameSchedule.isEmpty()) {
                continue;
            }
            int weeks = Math.min(League.REGULAR_SEASON_WEEKS, team.gameSchedule.size());
            for (int week = 0; week < weeks; week++) {
                Game game = team.gameSchedule.get(week);
                if (game == null || !game.hasPlayed) {
                    continue;
                }
                if (game.homeTeam != team) {
                    continue; // process once per game
                }
                Team a = game.homeTeam;
                Team b = game.awayTeam;
                String key = a.abbr.compareTo(b.abbr) < 0
                        ? a.abbr + "-" + b.abbr
                        : b.abbr + "-" + a.abbr;
                if (!seen.add(key)) {
                    continue;
                }
                if (a.isRival(b.abbr) || b.isRival(a.abbr)) {
                    continue;
                }
                if (!isMemorable(game, a, b)) {
                    continue;
                }
                Rivalry ra = a.ensureRivalry(b.abbr, 30);
                Rivalry rb = b.ensureRivalry(a.abbr, 30);
                if (ra != null && a.rivalryDynamicsNotes != null) {
                    a.rivalryDynamicsNotes.add(
                            "New rivalry forming with " + b.abbr + " (30 · Cold).");
                }
                if (rb != null && b.rivalryDynamicsNotes != null) {
                    b.rivalryDynamicsNotes.add(
                            "New rivalry forming with " + a.abbr + " (30 · Cold).");
                }
            }
        }
    }

    /**
     * User declares a rival: boost to at least 45; mirror on opponent at 35 if needed.
     *
     * @return null on success, otherwise an error message
     */
    public static String declareRival(Team user, Team opponent) {
        if (user == null || opponent == null || user == opponent) {
            return "Invalid opponent.";
        }
        int warmCount = 0;
        if (user.rivalries != null) {
            for (Rivalry r : user.rivalries) {
                if (r.strength >= Rivalry.WARM_THRESHOLD) {
                    warmCount++;
                }
            }
        }
        Rivalry existing = user.rivalryWith(opponent.abbr);
        if (existing == null && warmCount >= Rivalry.MAX_RIVALRIES) {
            return "Already have " + Rivalry.MAX_RIVALRIES
                    + " Hot/Warm rivals. Let a Cold rivalry fade first.";
        }
        Rivalry created = user.ensureRivalry(opponent.abbr, 45);
        if (created == null) {
            return "Could not add rival (roster of rivalries full).";
        }
        if (created.strength < 45) {
            created.strength = 45;
        }
        mirrorLink(opponent, user, 35);
        return null;
    }

    static int deltaForPlayedGame(Team team, Team opponent, Game game) {
        int margin = Math.abs(game.homeScore - game.awayScore);
        boolean won = (game.homeTeam == team && game.homeScore > game.awayScore)
                || (game.awayTeam == team && game.awayScore > game.homeScore);
        int ownPower = team.programProfile.programPower;
        int opponentPower = opponent.programProfile.programPower;

        if (won && opponentPower - ownPower >= 10) {
            return 8;
        }
        if (won && margin >= 28 && ownPower - opponentPower >= 15) {
            return -4;
        }
        if (margin <= 14) {
            return 6;
        }
        if (!won && Math.abs(ownPower - opponentPower) < 20) {
            return 4;
        }
        return 2;
    }

    private static boolean isMemorable(Game game, Team a, Team b) {
        int margin = Math.abs(game.homeScore - game.awayScore);
        if (margin <= 10) {
            return true;
        }
        boolean aWon = game.homeScore > game.awayScore
                ? game.homeTeam == a
                : game.awayTeam == a;
        Team winner = aWon ? a : b;
        Team loser = aWon ? b : a;
        if (loser.programProfile.programPower - winner.programProfile.programPower >= 10) {
            return true;
        }
        return a.rankTeamPollScore > 0 && b.rankTeamPollScore > 0
                && a.rankTeamPollScore <= 25 && b.rankTeamPollScore <= 25;
    }

    private static void mirrorLink(Team target, Team source, int minStrength) {
        if (target == null || source == null) {
            return;
        }
        if (target.rivalryWith(source.abbr) != null) {
            return;
        }
        target.ensureRivalry(source.abbr, minStrength);
    }

    private static Game findPlayedGame(Team team, Team opponent) {
        if (team.gameSchedule == null) {
            return null;
        }
        for (Game game : team.gameSchedule) {
            if (game == null || !game.hasPlayed) {
                continue;
            }
            Team opp = game.homeTeam == team ? game.awayTeam : game.homeTeam;
            if (opp == opponent) {
                return game;
            }
        }
        return null;
    }
}
