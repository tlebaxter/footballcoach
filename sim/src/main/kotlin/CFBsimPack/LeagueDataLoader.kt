package CFBsimPack

import kotlinx.serialization.json.Json

/**
 * Parses the versioned FBS team JSON used when a new league is created.
 */
object LeagueDataLoader {
    private const val EXPECTED_TEAM_COUNT = 140

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @JvmStatic
    fun load2026Teams(league: League, teamsJson: String) {
        if (teamsJson.isBlank()) {
            throw IllegalArgumentException("2026 FBS team data is empty.")
        }
        val seed = try {
            json.decodeFromString(FbsSeedFile.serializer(), teamsJson)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid FBS team JSON: ${e.message}", e)
        }
        if (seed.teams.size != EXPECTED_TEAM_COUNT) {
            throw IllegalArgumentException(
                "Expected $EXPECTED_TEAM_COUNT FBS teams but found ${seed.teams.size}.",
            )
        }

        val conferencesByName = LinkedHashMap<String, Conference>()
        val abbreviations = HashSet<String>()
        val names = HashSet<String>()
        val seeds = ArrayList<ValidatedSeed>()

        for ((index, team) in seed.teams.withIndex()) {
            val line = index + 1
            val conference = team.conference.trim()
            val name = team.name.trim()
            val abbreviation = team.abbr.trim()
            if (conference.isEmpty() || name.isEmpty()) {
                throw IllegalArgumentException(
                    "Conference and team name are required for team $line.",
                )
            }
            if (abbreviation.length != 3) {
                throw IllegalArgumentException(
                    "Team abbreviation must contain 3 characters for team $line.",
                )
            }
            if (!abbreviations.add(abbreviation)) {
                throw IllegalArgumentException("Duplicate team abbreviation: $abbreviation")
            }
            if (!names.add(name)) {
                throw IllegalArgumentException("Duplicate team name: $name")
            }
            validateFactor(team.tradition, "tradition", line)
            validateFactor(team.fanbase, "fanbase", line)
            validateFactor(team.donors, "donors", line)
            validateFactor(team.footprint, "footprint", line)
            validateFactor(team.pipeline, "pipeline", line)
            validateFactor(team.momentum, "momentum", line)
            if (team.rivals.isEmpty()) {
                throw IllegalArgumentException("At least one rival is required for team $line.")
            }
            val rivalries = ArrayList<Rivalry>()
            for (rival in team.rivals) {
                if (rival.strength < 0 || rival.strength > 100) {
                    throw IllegalArgumentException("Rival strength must be 0–100 for team $line.")
                }
                rivalries.add(Rivalry(rival.abbr, rival.strength))
            }
            var conf = conferencesByName[conference]
            if (conf == null) {
                conf = Conference(conference, league, conference != "Independents")
                conferencesByName[conference] = conf
            }
            seeds.add(
                ValidatedSeed(
                    conference = conf,
                    name = name,
                    abbreviation = abbreviation,
                    tradition = team.tradition,
                    fanbase = team.fanbase,
                    donors = team.donors,
                    footprint = team.footprint,
                    pipeline = team.pipeline,
                    momentum = team.momentum,
                    rivalsEncoded = Rivalry.encode(rivalries),
                ),
            )
        }

        for (s in seeds) {
            for (rivalry in Rivalry.parseEncoded(s.rivalsEncoded)) {
                if (!abbreviations.contains(rivalry.opponentAbbr)) {
                    throw IllegalArgumentException(
                        "Unknown rival ${rivalry.opponentAbbr} for ${s.abbreviation}.",
                    )
                }
                if (rivalry.opponentAbbr == s.abbreviation) {
                    throw IllegalArgumentException(
                        "Team ${s.abbreviation} cannot rival itself.",
                    )
                }
            }
        }

        league.conferences = ArrayList(conferencesByName.values)
        league.teamList = ArrayList()
        for (s in seeds) {
            val team = Team(
                s.name,
                s.abbreviation,
                s.conference.confName,
                league,
                s.tradition,
                s.fanbase,
                s.donors,
                s.footprint,
                s.pipeline,
                s.momentum,
                s.rivalsEncoded,
            )
            s.conference.confTeams.add(team)
            league.teamList.add(team)
        }
    }

    private fun validateFactor(value: Int, factor: String, line: Int) {
        if (value < 0 || value > 100) {
            throw IllegalArgumentException(
                "$factor must be between 0 and 100 for team $line.",
            )
        }
    }

    private data class ValidatedSeed(
        val conference: Conference,
        val name: String,
        val abbreviation: String,
        val tradition: Int,
        val fanbase: Int,
        val donors: Int,
        val footprint: Int,
        val pipeline: Int,
        val momentum: Int,
        val rivalsEncoded: String,
    )
}
