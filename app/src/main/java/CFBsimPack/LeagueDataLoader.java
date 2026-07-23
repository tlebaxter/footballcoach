package CFBsimPack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Parses the versioned FBS team data used when a new league is created.
 */
public final class LeagueDataLoader {

    private static final int EXPECTED_TEAM_COUNT = 138;

    private LeagueDataLoader() {
    }

    public static void load2026Teams(League league, String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            throw new IllegalArgumentException("2026 FBS team data is empty.");
        }

        Map<String, Conference> conferencesByName = new LinkedHashMap<>();
        Set<String> abbreviations = new HashSet<>();
        Set<String> names = new HashSet<>();
        ArrayList<TeamSeed> seeds = new ArrayList<>();

        String[] lines = csv.split("\\r?\\n");
        for (int lineNumber = 0; lineNumber < lines.length; lineNumber++) {
            String line = lines[lineNumber].trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (lineNumber == 0 && line.startsWith("conference,")) {
                continue;
            }

            String[] values = line.split(",", -1);
            if (values.length != 5) {
                throw new IllegalArgumentException(
                        "Invalid FBS team data on line " + (lineNumber + 1) + ": expected 5 columns.");
            }

            String conference = values[0].trim();
            String name = values[1].trim();
            String abbreviation = values[2].trim();
            int prestige;
            try {
                prestige = Integer.parseInt(values[3].trim());
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(
                        "Invalid prestige on line " + (lineNumber + 1) + ".", exception);
            }
            String rivalAbbreviation = values[4].trim();

            if (conference.isEmpty() || name.isEmpty() || rivalAbbreviation.isEmpty()) {
                throw new IllegalArgumentException(
                        "Conference, team name, and rival are required on line " + (lineNumber + 1) + ".");
            }
            if (abbreviation.length() != 3) {
                throw new IllegalArgumentException(
                        "Team abbreviation must contain 3 characters on line " + (lineNumber + 1) + ".");
            }
            if (prestige < 0 || prestige > 100) {
                throw new IllegalArgumentException(
                        "Prestige must be between 0 and 100 on line " + (lineNumber + 1) + ".");
            }
            if (!abbreviations.add(abbreviation)) {
                throw new IllegalArgumentException("Duplicate team abbreviation: " + abbreviation);
            }
            if (!names.add(name)) {
                throw new IllegalArgumentException("Duplicate team name: " + name);
            }

            Conference conf = conferencesByName.get(conference);
            if (conf == null) {
                conf = new Conference(conference, league, !"Independents".equals(conference));
                conferencesByName.put(conference, conf);
            }
            seeds.add(new TeamSeed(conf, name, abbreviation, prestige, rivalAbbreviation));
        }

        if (seeds.size() != EXPECTED_TEAM_COUNT) {
            throw new IllegalArgumentException(
                    "Expected " + EXPECTED_TEAM_COUNT + " FBS teams but found " + seeds.size() + ".");
        }
        for (TeamSeed seed : seeds) {
            if (!abbreviations.contains(seed.rivalAbbreviation)) {
                throw new IllegalArgumentException(
                        "Unknown rival " + seed.rivalAbbreviation + " for " + seed.abbreviation + ".");
            }
        }

        league.conferences = new ArrayList<>(conferencesByName.values());
        league.teamList = new ArrayList<>();
        for (TeamSeed seed : seeds) {
            Team team = new Team(
                    seed.name,
                    seed.abbreviation,
                    seed.conference.confName,
                    league,
                    seed.prestige,
                    seed.rivalAbbreviation);
            seed.conference.confTeams.add(team);
            league.teamList.add(team);
        }
    }

    private static final class TeamSeed {
        private final Conference conference;
        private final String name;
        private final String abbreviation;
        private final int prestige;
        private final String rivalAbbreviation;

        private TeamSeed(
                Conference conference,
                String name,
                String abbreviation,
                int prestige,
                String rivalAbbreviation) {
            this.conference = conference;
            this.name = name;
            this.abbreviation = abbreviation;
            this.prestige = prestige;
            this.rivalAbbreviation = rivalAbbreviation;
        }
    }
}
