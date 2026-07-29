package achijones.footballcoach.save

import CFBsimPack.League
import CFBsimPack.OffseasonSession
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Maps live [League] state to versioned [SaveDocument] JSON and back.
 *
 * v12 documents store canonical league state in [SaveDocument.cfbPayload]
 * ([League.buildSaveString] / [League.fromSaveString]), gzip+Base64 packed as `gz1:…`
 * so Room CursorWindow limits are not hit. Structured fields and
 * [toCfbText] / [fromCfbText] exist only to migrate v10/v11 JSON on decode.
 */
object CareerSaveMapper {
    private const val GZIP_PREFIX = "gz1:"

    private val json = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        prettyPrint = false
    }

    fun encode(document: SaveDocument): String = json.encodeToString(SaveDocument.serializer(), document)

    fun decode(text: String): SaveDocument {
        val raw = json.decodeFromString(SaveDocument.serializer(), text)
        val doc = migrateToCurrent(raw)
        validate(doc)
        return doc
    }

    /** Upgrade v10/v11 structured documents to a v12 CFB envelope. */
    fun migrateToCurrent(doc: SaveDocument): SaveDocument {
        return when (doc.saveVersion) {
            CURRENT_SAVE_VERSION -> {
                if (doc.cfbPayload.isBlank()) {
                    throw CorruptSaveException("Missing cfbPayload")
                }
                doc
            }
            10, 11 -> {
                if (doc.teams.isEmpty()) {
                    throw CorruptSaveException("Legacy save has no teams")
                }
                validateLegacyStructured(doc)
                val cfb = toCfbText(doc)
                SaveDocument(
                    saveVersion = CURRENT_SAVE_VERSION,
                    summary = doc.summary,
                    currentWeek = doc.currentWeek,
                    hasScheduledBowls = doc.hasScheduledBowls,
                    userTeamAbbr = doc.userTeamAbbr,
                    offseasonPhase = doc.offseasonPhase ?: doc.offseason?.phase,
                    cfbPayload = packCfb(cfb),
                )
            }
            else -> throw IncompatibleSaveException("Unsupported save version ${doc.saveVersion}")
        }
    }

    fun packCfb(cfb: String): String {
        val bytes = cfb.toByteArray(Charsets.UTF_8)
        val baos = ByteArrayOutputStream()
        GZIPOutputStream(baos).use { it.write(bytes) }
        return GZIP_PREFIX + Base64.getEncoder().encodeToString(baos.toByteArray())
    }

    fun unpackCfb(packed: String): String {
        if (!packed.startsWith(GZIP_PREFIX)) {
            return packed
        }
        val raw = Base64.getDecoder().decode(packed.substring(GZIP_PREFIX.length))
        return GZIPInputStream(ByteArrayInputStream(raw)).use { stream ->
            stream.readBytes().toString(Charsets.UTF_8)
        }
    }

    fun validate(doc: SaveDocument) {
        if (doc.saveVersion != CURRENT_SAVE_VERSION) {
            throw IncompatibleSaveException("Unsupported save version ${doc.saveVersion}")
        }
        if (doc.summary.isBlank()) {
            throw CorruptSaveException("Missing summary")
        }
        if (doc.userTeamAbbr.isBlank()) {
            throw CorruptSaveException("Missing userTeamAbbr")
        }
        if (doc.cfbPayload.isBlank()) {
            throw CorruptSaveException("Missing cfbPayload")
        }
        if (doc.currentWeek < 0) {
            throw CorruptSaveException("Invalid currentWeek")
        }
    }

    /** Structural checks used only when migrating v10/v11 documents. */
    private fun validateLegacyStructured(doc: SaveDocument) {
        if (doc.summary.isBlank()) {
            throw CorruptSaveException("Missing summary")
        }
        if (doc.userTeamAbbr.isBlank()) {
            throw CorruptSaveException("Missing userTeamAbbr")
        }
        if (doc.teams.isEmpty()) {
            throw CorruptSaveException("Save has no teams")
        }
        if (doc.currentWeek < 0) {
            throw CorruptSaveException("Invalid currentWeek")
        }
        doc.schedule?.forEach { teamSched ->
            if (teamSched.weeks.size != League.REGULAR_SEASON_WEEKS) {
                throw CorruptSaveException("Schedule for ${teamSched.teamAbbr} has wrong week count")
            }
            teamSched.weeks.forEach { slot ->
                when (slot.kind) {
                    "BYE", "EMPTY" -> Unit
                    "MATCHUP" -> {
                        if (slot.opponentAbbr.isBlank()) {
                            throw CorruptSaveException("Matchup missing opponent")
                        }
                        if (slot.played && slot.result == null && slot.home) {
                            throw CorruptSaveException("Played home game missing result")
                        }
                    }
                    else -> throw CorruptSaveException("Unknown schedule slot kind ${slot.kind}")
                }
            }
        }
        doc.teamSeason?.forEach { row ->
            if (row.abbr.isBlank()) throw CorruptSaveException("Team season missing abbr")
            if (row.wins < 0 || row.losses < 0) {
                throw CorruptSaveException("Invalid W-L for ${row.abbr}")
            }
        }
    }

    fun fromLeague(league: League): SaveDocument {
        val cfb = league.buildSaveString()
        val meta = fromCfbText(cfb, league)
        return SaveDocument(
            saveVersion = CURRENT_SAVE_VERSION,
            summary = meta.summary,
            currentWeek = meta.currentWeek,
            hasScheduledBowls = meta.hasScheduledBowls,
            userTeamAbbr = meta.userTeamAbbr,
            offseasonPhase = meta.offseasonPhase,
            cfbPayload = packCfb(cfb),
        )
    }

    fun toLeague(document: SaveDocument, namesCSV: String, lastNamesCSV: String): League {
        val doc = if (document.saveVersion == CURRENT_SAVE_VERSION) {
            validate(document)
            document
        } else {
            val migrated = migrateToCurrent(document)
            validate(migrated)
            migrated
        }
        return try {
            League.fromSaveString(unpackCfb(doc.cfbPayload), namesCSV, lastNamesCSV)
        } catch (e: Exception) {
            throw CorruptSaveException("Unable to hydrate league: ${e.message}", e)
        }
    }

    fun summaryLine(document: SaveDocument): String = document.summary

    /**
     * Parse CFB text into a structured document.
     * Used for metadata extraction and as input shape for [toCfbText] migration.
     */
    fun fromCfbText(cfb: String, leagueHint: League? = null): SaveDocument {
        val lines = cfb.split('\n')
        if (lines.isEmpty() || lines[0].isBlank()) {
            throw CorruptSaveException("Empty career payload")
        }
        val header = lines[0]
        val summary = header.trimEnd('%', '\r')
        var i = 1

        val leagueHistory = ArrayList<List<String>>()
        while (i < lines.size && lines[i] != "END_LEAGUE_HIST") {
            val parts = lines[i].split('%').filter { it.isNotEmpty() }
            leagueHistory.add(parts)
            i++
        }
        if (i >= lines.size || lines[i] != "END_LEAGUE_HIST") {
            throw CorruptSaveException("Missing END_LEAGUE_HIST")
        }
        i++

        val heismanHistory = ArrayList<String>()
        while (i < lines.size && lines[i] != "END_HEISMAN_HIST") {
            heismanHistory.add(lines[i])
            i++
        }
        if (i >= lines.size || lines[i] != "END_HEISMAN_HIST") {
            throw CorruptSaveException("Missing END_HEISMAN_HIST")
        }
        i++

        if (i >= lines.size || !lines[i].startsWith("SAVE_VERSION,")) {
            throw CorruptSaveException("Missing SAVE_VERSION")
        }
        val innerVersion = lines[i].substringAfter("SAVE_VERSION,").trim().toIntOrNull()
            ?: throw CorruptSaveException("Bad SAVE_VERSION")
        if (innerVersion !in 6..9) {
            throw IncompatibleSaveException("Unsupported inner CFB version $innerVersion")
        }
        i++
        if (i >= lines.size || !lines[i].startsWith("TEAM_COUNT,")) {
            throw CorruptSaveException("Missing TEAM_COUNT")
        }
        val teamCount = lines[i].substringAfter("TEAM_COUNT,").trim().toIntOrNull()
            ?: throw CorruptSaveException("Bad TEAM_COUNT")
        i++

        val teams = ArrayList<TeamSaveDoc>(teamCount)
        repeat(teamCount) {
            if (i >= lines.size) throw CorruptSaveException("Truncated team block")
            val teamHeader = lines[i]
            i++
            val pct = teamHeader.indexOf('%')
            if (pct < 0) throw CorruptSaveException("Malformed team header")
            val before = teamHeader.substring(0, pct)
            val evenYear = teamHeader.substring(pct + 1).trimEnd('%')
            val csv = before.split(',', limit = 4)
            if (csv.size < 4) throw CorruptSaveException("Team header too short")
            val conference = csv[0]
            val name = csv[1]
            val abbr = csv[2]
            val profileCsv = csv[3]

            val playerLines = ArrayList<String>()
            while (i < lines.size && lines[i] != "END_PLAYERS") {
                val pl = lines[i].trimEnd('%')
                if (pl.isNotEmpty()) playerLines.add(pl)
                i++
            }
            if (i >= lines.size || lines[i] != "END_PLAYERS") {
                throw CorruptSaveException("Missing END_PLAYERS for $abbr")
            }
            i++
            var stDepth: String? = null
            if (i < lines.size && lines[i].startsWith("ST_DEPTH,")) {
                stDepth = lines[i]
                i++
            }
            teams.add(
                TeamSaveDoc(
                    conference = conference,
                    name = name,
                    abbr = abbr,
                    profileCsv = profileCsv,
                    evenYearHomeOpp = evenYear,
                    playerLines = playerLines,
                    specialTeamsDepth = stDepth,
                ),
            )
        }

        if (i >= lines.size) throw CorruptSaveException("Missing user team")
        // user team name line — abbr resolved from teams / hint
        i++ // skip name
        val userTeamHistory = ArrayList<String>()
        while (i < lines.size && lines[i] != "END_USER_TEAM") {
            userTeamHistory.add(lines[i])
            i++
        }
        if (i >= lines.size || lines[i] != "END_USER_TEAM") {
            throw CorruptSaveException("Missing END_USER_TEAM")
        }
        i++

        // bless/curse
        while (i < lines.size && lines[i] != "END_BLESS_TEAM") i++
        if (i < lines.size && lines[i] == "END_BLESS_TEAM") i++
        while (i < lines.size && lines[i] != "END_CURSE_TEAM") i++
        if (i < lines.size && lines[i] == "END_CURSE_TEAM") i++

        val leagueRecordLines = ArrayList<String>()
        while (i < lines.size && lines[i] != "END_LEAGUE_RECORDS") {
            if (lines[i].isNotEmpty()) leagueRecordLines.add(lines[i])
            i++
        }
        if (i >= lines.size || lines[i] != "END_LEAGUE_RECORDS") {
            throw CorruptSaveException("Missing END_LEAGUE_RECORDS")
        }
        i++

        val leagueWinStreakCsv = if (i < lines.size) lines[i] else "0,XXX,0,0"
        i++
        if (i < lines.size && lines[i] == "END_LEAGUE_WIN_STREAK") i++

        val userTeamRecordLines = ArrayList<String>()
        while (i < lines.size && lines[i] != "END_USER_TEAM_RECORDS") {
            if (lines[i].isNotEmpty()) userTeamRecordLines.add(lines[i])
            i++
        }
        if (i >= lines.size || lines[i] != "END_USER_TEAM_RECORDS") {
            throw CorruptSaveException("Missing END_USER_TEAM_RECORDS")
        }
        i++

        val hallOfFame = ArrayList<String>()
        while (i < lines.size && lines[i] != "END_HALL_OF_FAME") {
            if (lines[i].isNotEmpty()) hallOfFame.add(lines[i])
            i++
        }
        if (i >= lines.size || lines[i] != "END_HALL_OF_FAME") {
            throw CorruptSaveException("Missing END_HALL_OF_FAME")
        }
        i++

        var currentWeek = leagueHint?.currentWeek ?: 0
        var hasScheduledBowls = leagueHint?.hasScheduledBowls ?: false
        if (i < lines.size && lines[i] == "SEASON_PROGRESS") {
            i++
            while (i < lines.size && lines[i] != "END_SEASON_PROGRESS") {
                val line = lines[i]
                if (line.startsWith("currentWeek,")) {
                    currentWeek = line.substringAfter("currentWeek,").trim().toIntOrNull()
                        ?: throw CorruptSaveException("Bad currentWeek")
                } else if (line.startsWith("hasScheduledBowls,")) {
                    hasScheduledBowls = line.substringAfter("hasScheduledBowls,").trim() == "1"
                }
                i++
            }
            if (i < lines.size && lines[i] == "END_SEASON_PROGRESS") i++
        }

        var schedule: List<ScheduleTeamDoc>? = null
        if (i < lines.size && lines[i] == "SCHEDULE") {
            i++
            val sched = ArrayList<ScheduleTeamDoc>()
            while (i < lines.size && lines[i] != "END_SCHEDULE") {
                sched.add(parseScheduleLine(lines[i]))
                i++
            }
            if (i >= lines.size || lines[i] != "END_SCHEDULE") {
                throw CorruptSaveException("Missing END_SCHEDULE")
            }
            i++
            schedule = sched
        }

        var teamSeason: List<TeamSeasonDoc>? = null
        if (i < lines.size && lines[i] == "TEAM_SEASON") {
            i++
            val seasons = ArrayList<TeamSeasonDoc>()
            while (i < lines.size && lines[i] != "END_TEAM_SEASON") {
                seasons.add(parseTeamSeasonLine(lines[i]))
                i++
            }
            if (i >= lines.size || lines[i] != "END_TEAM_SEASON") {
                throw CorruptSaveException("Missing END_TEAM_SEASON")
            }
            i++
            teamSeason = seasons
        }

        var postseason: PostseasonDoc? = null
        if (i < lines.size && lines[i] == "POSTSEASON") {
            val parsed = parsePostseasonBlock(lines, i + 1)
            postseason = parsed.doc
            i = parsed.nextIndex
        }

        var ooc: OocBookDoc? = null
        if (i < lines.size && lines[i] == "OOC_CONTRACTS") {
            i++
            if (i >= lines.size || !lines[i].startsWith("NEXT_ID,")) {
                throw CorruptSaveException("Missing OOC NEXT_ID")
            }
            val nextId = lines[i].substringAfter("NEXT_ID,").trim().toIntOrNull()
                ?: throw CorruptSaveException("Bad NEXT_ID")
            i++
            val contractLines = ArrayList<String>()
            while (i < lines.size && lines[i] != "END_OOC_CONTRACTS") {
                if (lines[i].isNotEmpty()) contractLines.add(lines[i])
                i++
            }
            if (i >= lines.size || lines[i] != "END_OOC_CONTRACTS") {
                throw CorruptSaveException("Missing END_OOC_CONTRACTS")
            }
            i++
            ooc = OocBookDoc(nextId = nextId, contractLines = contractLines)
        }

        var offseason: OffseasonSaveDoc? = null
        if (i < lines.size && lines[i].startsWith("OFFSEASON,")) {
            val phase = lines[i].substringAfter("OFFSEASON,").trim()
            i++
            val budgets = ArrayList<TeamBudgetDoc>()
            val retained = ArrayList<RetainedKeyDoc>()
            val portal = ArrayList<PortalPlayerDoc>()
            val hs = ArrayList<String>()
            if (i < lines.size && lines[i] == "BUDGETS") {
                i++
                while (i < lines.size && lines[i] != "END_BUDGETS") {
                    val p = lines[i].split(',')
                    if (p.size >= 2) {
                        budgets.add(
                            TeamBudgetDoc(
                                abbr = p[0],
                                recruitMoney = p[1].toIntOrNull()
                                    ?: throw CorruptSaveException("Bad budget for ${p[0]}"),
                            ),
                        )
                    }
                    i++
                }
                if (i < lines.size && lines[i] == "END_BUDGETS") i++
            }
            if (i < lines.size && lines[i] == "RETAINED") {
                i++
                while (i < lines.size && lines[i] != "END_RETAINED") {
                    val p = lines[i].split(',')
                    if (p.size >= 4) {
                        retained.add(
                            RetainedKeyDoc(
                                teamAbbr = p[0],
                                position = p[1],
                                name = p[2],
                                year = p[3].toIntOrNull()
                                    ?: throw CorruptSaveException("Bad retained year"),
                            ),
                        )
                    }
                    i++
                }
                if (i < lines.size && lines[i] == "END_RETAINED") i++
            }
            if (i < lines.size && lines[i] == "PORTAL") {
                i++
                while (i < lines.size && lines[i] != "END_PORTAL") {
                    val line = lines[i]
                    val pipe = line.indexOf('|')
                    if (pipe > 0) {
                        portal.add(
                            PortalPlayerDoc(
                                priorTeamAbbr = line.substring(0, pipe),
                                playerLine = line.substring(pipe + 1).trimEnd('%'),
                            ),
                        )
                    }
                    i++
                }
                if (i < lines.size && lines[i] == "END_PORTAL") i++
            }
            if (i < lines.size && lines[i] == "HS") {
                i++
                while (i < lines.size && lines[i] != "END_HS") {
                    val pl = lines[i].trimEnd('%')
                    if (pl.isNotEmpty()) hs.add(pl)
                    i++
                }
                if (i < lines.size && lines[i] == "END_HS") i++
            }
            if (i < lines.size && lines[i] == "END_OFFSEASON") i++
            offseason = OffseasonSaveDoc(
                phase = phase,
                budgets = budgets,
                retained = retained,
                portal = portal,
                hsClass = hs,
            )
        }

        val userAbbr = leagueHint?.userTeam?.abbr
            ?: summary.substringAfter(": ").substringBefore(" (").trim().ifBlank {
                teams.firstOrNull()?.abbr ?: ""
            }
        val offPhase = offseason?.phase
            ?: Regex("""\[OFF:([A-Z_]+)]""").find(summary)?.groupValues?.getOrNull(1)
            ?: if (OffseasonSession.ready() && OffseasonSession.league === leagueHint) {
                OffseasonSession.phase.name
            } else {
                null
            }

        // Structured v11 shape for metadata / migrate tests — not a live v12 envelope.
        return SaveDocument(
            saveVersion = 11,
            summary = summary,
            currentWeek = currentWeek,
            hasScheduledBowls = hasScheduledBowls,
            userTeamAbbr = userAbbr,
            offseasonPhase = offPhase,
            leagueHistory = leagueHistory,
            heismanHistory = heismanHistory,
            teams = teams,
            userTeamHistory = userTeamHistory,
            leagueRecordLines = leagueRecordLines,
            leagueWinStreakCsv = leagueWinStreakCsv,
            userTeamRecordLines = userTeamRecordLines,
            hallOfFame = hallOfFame,
            schedule = schedule,
            teamSeason = teamSeason,
            postseason = postseason,
            oocContracts = ooc,
            offseason = offseason,
        )
    }

    /** Rebuild CFB text from a structured v10/v11 document (migration only). */
    fun toCfbText(doc: SaveDocument): String {
        val sb = StringBuilder()
        sb.append(doc.summary.trimEnd('%')).append("%\n")
        for (year in doc.leagueHistory) {
            for (entry in year) {
                sb.append(entry).append('%')
            }
            sb.append('\n')
        }
        sb.append("END_LEAGUE_HIST\n")
        for (h in doc.heismanHistory) {
            sb.append(h).append('\n')
        }
        sb.append("END_HEISMAN_HIST\n")
        sb.append("SAVE_VERSION,9\n")
        sb.append("TEAM_COUNT,").append(doc.teams.size).append('\n')
        for (t in doc.teams) {
            sb.append(t.conference).append(',').append(t.name).append(',').append(t.abbr)
                .append(',').append(t.profileCsv).append('%')
                .append(t.evenYearHomeOpp).append("%\n")
            for (p in t.playerLines) {
                sb.append(p)
                if (!p.endsWith('%')) sb.append('%')
                sb.append('\n')
            }
            sb.append("END_PLAYERS\n")
            if (t.specialTeamsDepth != null) {
                sb.append(t.specialTeamsDepth).append('\n')
            }
        }
        val userName = doc.teams.firstOrNull { it.abbr == doc.userTeamAbbr }?.name
            ?: doc.teams.firstOrNull()?.name
            ?: doc.userTeamAbbr
        sb.append(userName).append('\n')
        for (h in doc.userTeamHistory) sb.append(h).append('\n')
        sb.append("END_USER_TEAM\n")
        sb.append("NULL\nEND_BLESS_TEAM\nNULL\nEND_CURSE_TEAM\n")
        for (r in doc.leagueRecordLines) sb.append(r).append('\n')
        sb.append("END_LEAGUE_RECORDS\n")
        sb.append(doc.leagueWinStreakCsv).append("\nEND_LEAGUE_WIN_STREAK\n")
        for (r in doc.userTeamRecordLines) sb.append(r).append('\n')
        sb.append("END_USER_TEAM_RECORDS\n")
        for (h in doc.hallOfFame) sb.append(h).append('\n')
        sb.append("END_HALL_OF_FAME\n")

        sb.append("SEASON_PROGRESS\n")
        sb.append("currentWeek,").append(doc.currentWeek).append('\n')
        sb.append("hasScheduledBowls,").append(if (doc.hasScheduledBowls) 1 else 0).append('\n')
        sb.append("END_SEASON_PROGRESS\n")

        val schedule = doc.schedule
        if (schedule != null) {
            sb.append("SCHEDULE\n")
            for (team in schedule) {
                sb.append(team.teamAbbr).append(',').append(team.byeWeek)
                for (week in team.weeks) {
                    sb.append(',')
                    sb.append(emitScheduleSlot(week))
                }
                sb.append('\n')
            }
            sb.append("END_SCHEDULE\n")
        }

        val teamSeason = doc.teamSeason
        if (teamSeason != null) {
            sb.append("TEAM_SEASON\n")
            for (row in teamSeason) {
                sb.append(emitTeamSeasonLine(row)).append('\n')
            }
            sb.append("END_TEAM_SEASON\n")
        }

        val postseason = doc.postseason
        if (postseason != null) {
            emitPostseasonBlock(sb, postseason)
        }

        val ooc = doc.oocContracts
        if (ooc != null) {
            sb.append("OOC_CONTRACTS\n")
            sb.append("NEXT_ID,").append(ooc.nextId).append('\n')
            for (line in ooc.contractLines) sb.append(line).append('\n')
            sb.append("END_OOC_CONTRACTS\n")
        }

        val off = doc.offseason
        if (off != null) {
            sb.append("OFFSEASON,").append(off.phase).append('\n')
            sb.append("BUDGETS\n")
            for (b in off.budgets) {
                sb.append(b.abbr).append(',').append(b.recruitMoney).append('\n')
            }
            sb.append("END_BUDGETS\n")
            sb.append("RETAINED\n")
            for (r in off.retained) {
                sb.append(r.teamAbbr).append(',').append(r.position).append(',')
                    .append(r.name).append(',').append(r.year).append('\n')
            }
            sb.append("END_RETAINED\n")
            sb.append("PORTAL\n")
            for (p in off.portal) {
                sb.append(p.priorTeamAbbr).append('|').append(p.playerLine)
                if (!p.playerLine.endsWith('%')) sb.append('%')
                sb.append('\n')
            }
            sb.append("END_PORTAL\n")
            sb.append("HS\n")
            for (p in off.hsClass) {
                sb.append(p)
                if (!p.endsWith('%')) sb.append('%')
                sb.append('\n')
            }
            sb.append("END_HS\n")
            sb.append("END_OFFSEASON\n")
        }
        return sb.toString()
    }

    private fun parseScheduleLine(line: String): ScheduleTeamDoc {
        val parts = line.split(',', limit = Int.MAX_VALUE)
        if (parts.size < 2) throw CorruptSaveException("Bad schedule line")
        val abbr = parts[0]
        val bye = parts[1].toIntOrNull() ?: throw CorruptSaveException("Bad bye for $abbr")
        val weeks = ArrayList<ScheduleSlotDoc>(League.REGULAR_SEASON_WEEKS)
        for (w in 0 until League.REGULAR_SEASON_WEEKS) {
            val token = parts.getOrNull(w + 2).orEmpty()
            weeks.add(parseScheduleToken(token))
        }
        return ScheduleTeamDoc(teamAbbr = abbr, byeWeek = bye, weeks = weeks)
    }

    private fun parseScheduleToken(token: String): ScheduleSlotDoc {
        if (token.isEmpty() || token == "-") {
            return ScheduleSlotDoc(kind = "EMPTY")
        }
        if (token == "BYE") {
            return ScheduleSlotDoc(kind = "BYE")
        }
        val pipe = token.split('|')
        val matchup = pipe[0]
        if (matchup.length < 3 || (matchup[0] != 'H' && matchup[0] != 'A') || matchup[1] != ':') {
            throw CorruptSaveException("Bad matchup token $token")
        }
        val home = matchup[0] == 'H'
        val opp = matchup.substring(2)
        val played = pipe.size >= 2 && pipe[1] == "1"
        val result = if (played && home && pipe.size >= 11) {
            GameResultDoc(
                homeScore = pipe[2].toInt(),
                awayScore = pipe[3].toInt(),
                homeYards = pipe[4].toInt(),
                awayYards = pipe[5].toInt(),
                homeTOs = pipe[6].toInt(),
                awayTOs = pipe[7].toInt(),
                numOT = pipe[8].toInt(),
                homeQScore = pipe[9].split('#').map { it.toIntOrNull() ?: 0 }.padQuarters(),
                awayQScore = pipe[10].split('#').map { it.toIntOrNull() ?: 0 }.padQuarters(),
            )
        } else {
            null
        }
        if (played && home && result == null) {
            throw CorruptSaveException("Corrupt home result for $token")
        }
        return ScheduleSlotDoc(
            kind = "MATCHUP",
            home = home,
            opponentAbbr = opp,
            played = played,
            result = result,
        )
    }

    private fun emitScheduleSlot(slot: ScheduleSlotDoc): String {
        return when (slot.kind) {
            "BYE" -> "BYE"
            "EMPTY" -> "-"
            "MATCHUP" -> {
                val base = (if (slot.home) "H:" else "A:") + slot.opponentAbbr
                if (!slot.played) return base
                if (!slot.home) return "$base|1"
                val r = slot.result ?: throw CorruptSaveException("Played home missing result")
                buildString {
                    append(base).append("|1|")
                        .append(r.homeScore).append('|').append(r.awayScore).append('|')
                        .append(r.homeYards).append('|').append(r.awayYards).append('|')
                        .append(r.homeTOs).append('|').append(r.awayTOs).append('|')
                        .append(r.numOT).append('|')
                        .append(r.homeQScore.padQuarters().joinToString("#")).append('|')
                        .append(r.awayQScore.padQuarters().joinToString("#"))
                }
            }
            else -> throw CorruptSaveException("Unknown slot kind ${slot.kind}")
        }
    }

    private fun parseTeamSeasonLine(line: String): TeamSeasonDoc {
        val p = line.split('|')
        if (p.size < 12) throw CorruptSaveException("Bad TEAM_SEASON row")
        fun intAt(i: Int) = p[i].toIntOrNull() ?: throw CorruptSaveException("Bad int in team season")
        val rivalry = LinkedHashMap<String, Boolean>()
        if (p.size > 15 && p[15].isNotEmpty()) {
            for (pair in p[15].split(';')) {
                val colon = pair.lastIndexOf(':')
                if (colon <= 0) continue
                rivalry[pair.substring(0, colon)] = pair.substring(colon + 1) == "1"
            }
        }
        return TeamSeasonDoc(
            abbr = p[0],
            wins = intAt(1),
            losses = intAt(2),
            teamPoints = intAt(3),
            teamOppPoints = intAt(4),
            teamYards = intAt(5),
            teamOppYards = intAt(6),
            teamPassYards = intAt(7),
            teamRushYards = intAt(8),
            teamOppPassYards = intAt(9),
            teamOppRushYards = intAt(10),
            teamTODiff = intAt(11),
            winStreakCsv = p.getOrElse(12) { "0,XXX,0,0" },
            gameWLSchedule = if (p.size > 13 && p[13].isNotEmpty()) p[13].split(';') else emptyList(),
            gameWinsAgainstAbbrs = if (p.size > 14 && p[14].isNotEmpty()) {
                p[14].split(';').filter { it.isNotEmpty() }
            } else {
                emptyList()
            },
            rivalryResults = rivalry,
            confChampion = p.getOrElse(16) { "" },
            semiFinalWL = p.getOrElse(17) { "" },
            natChampWL = p.getOrElse(18) { "" },
        )
    }

    private fun emitTeamSeasonLine(row: TeamSeasonDoc): String {
        val rivalry = row.rivalryResults.entries.joinToString(";") { (k, v) ->
            "$k:${if (v) 1 else 0}"
        }
        return buildString {
            append(row.abbr).append('|')
                .append(row.wins).append('|').append(row.losses).append('|')
                .append(row.teamPoints).append('|').append(row.teamOppPoints).append('|')
                .append(row.teamYards).append('|').append(row.teamOppYards).append('|')
                .append(row.teamPassYards).append('|').append(row.teamRushYards).append('|')
                .append(row.teamOppPassYards).append('|').append(row.teamOppRushYards).append('|')
                .append(row.teamTODiff).append('|')
                .append(row.winStreakCsv).append('|')
                .append(row.gameWLSchedule.joinToString(";")).append('|')
                .append(row.gameWinsAgainstAbbrs.joinToString(";")).append('|')
                .append(rivalry).append('|')
                .append(row.confChampion).append('|')
                .append(row.semiFinalWL).append('|')
                .append(row.natChampWL)
        }
    }

    private data class PostseasonParse(val doc: PostseasonDoc, val nextIndex: Int)

    private fun parsePostseasonBlock(lines: List<String>, start: Int): PostseasonParse {
        var i = start
        var cfpField = emptyList<String>()
        var cfpAutoBids = emptyList<String>()
        var cfpFirstRound: List<PostseasonGameDoc>? = null
        var cfpQuarters: List<PostseasonGameDoc>? = null
        var cfpSemis: List<PostseasonGameDoc>? = null
        var ncg: PostseasonGameDoc? = null
        val bowlGames = ArrayList<PostseasonGameDoc>()
        val conferenceCcgs = ArrayList<ConferenceCcgDoc>()

        while (i < lines.size && lines[i] != "END_POSTSEASON") {
            val line = lines[i]
            when {
                line.startsWith("FIELD") -> {
                    cfpField = line.split(',').drop(1).filter { it.isNotEmpty() }
                    i++
                }
                line.startsWith("AUTO") -> {
                    cfpAutoBids = line.split(',').drop(1).filter { it.isNotEmpty() }
                    i++
                }
                line == "FR" -> {
                    val round = parsePostseasonRound(lines, i + 1, "END_FR")
                    cfpFirstRound = round.games
                    i = round.nextIndex
                }
                line == "QF" -> {
                    val round = parsePostseasonRound(lines, i + 1, "END_QF")
                    cfpQuarters = round.games
                    i = round.nextIndex
                }
                line == "SF" -> {
                    val round = parsePostseasonRound(lines, i + 1, "END_SF")
                    cfpSemis = round.games
                    i = round.nextIndex
                }
                line == "NCG" -> {
                    i++
                    while (i < lines.size && lines[i] != "END_NCG") {
                        if (lines[i].isNotEmpty()) {
                            ncg = parsePostseasonGame(lines[i])
                        }
                        i++
                    }
                    if (i < lines.size && lines[i] == "END_NCG") i++
                }
                line == "BOWLS" -> {
                    i++
                    while (i < lines.size && lines[i] != "END_BOWLS") {
                        if (lines[i].isNotEmpty()) {
                            bowlGames.add(parsePostseasonGame(lines[i]))
                        }
                        i++
                    }
                    if (i < lines.size && lines[i] == "END_BOWLS") i++
                }
                line == "CCG" -> {
                    i++
                    while (i < lines.size && lines[i] != "END_CCG") {
                        if (lines[i].isNotEmpty()) {
                            val firstPipe = lines[i].indexOf('|')
                            if (firstPipe > 0) {
                                conferenceCcgs.add(
                                    ConferenceCcgDoc(
                                        conference = lines[i].substring(0, firstPipe),
                                        game = parsePostseasonGame(lines[i].substring(firstPipe + 1)),
                                    ),
                                )
                            }
                        }
                        i++
                    }
                    if (i < lines.size && lines[i] == "END_CCG") i++
                }
                else -> i++
            }
        }
        if (i >= lines.size || lines[i] != "END_POSTSEASON") {
            throw CorruptSaveException("Missing END_POSTSEASON")
        }
        i++
        return PostseasonParse(
            doc = PostseasonDoc(
                cfpField = cfpField,
                cfpAutoBids = cfpAutoBids,
                cfpFirstRound = cfpFirstRound,
                cfpQuarters = cfpQuarters,
                cfpSemis = cfpSemis,
                ncg = ncg,
                bowlGames = bowlGames,
                conferenceCcgs = conferenceCcgs,
            ),
            nextIndex = i,
        )
    }

    private data class RoundParse(val games: List<PostseasonGameDoc>, val nextIndex: Int)

    private fun parsePostseasonRound(lines: List<String>, start: Int, endTag: String): RoundParse {
        var i = start
        val games = ArrayList<PostseasonGameDoc>()
        while (i < lines.size && lines[i] != endTag) {
            if (lines[i].isNotEmpty()) {
                games.add(parsePostseasonGame(lines[i]))
            }
            i++
        }
        if (i >= lines.size || lines[i] != endTag) {
            throw CorruptSaveException("Missing $endTag")
        }
        return RoundParse(games = games, nextIndex = i + 1)
    }

    private fun parsePostseasonGame(line: String): PostseasonGameDoc {
        val pipe = line.split('|')
        if (pipe.size < 4) {
            throw CorruptSaveException("Bad postseason game: $line")
        }
        val played = pipe[3] == "1"
        val result = if (played && pipe.size >= 13) {
            GameResultDoc(
                homeScore = pipe[4].toIntOrNull() ?: throw CorruptSaveException("Bad postseason score"),
                awayScore = pipe[5].toIntOrNull() ?: throw CorruptSaveException("Bad postseason score"),
                homeYards = pipe[6].toIntOrNull() ?: throw CorruptSaveException("Bad postseason yards"),
                awayYards = pipe[7].toIntOrNull() ?: throw CorruptSaveException("Bad postseason yards"),
                homeTOs = pipe[8].toIntOrNull() ?: throw CorruptSaveException("Bad postseason TOs"),
                awayTOs = pipe[9].toIntOrNull() ?: throw CorruptSaveException("Bad postseason TOs"),
                numOT = pipe[10].toIntOrNull() ?: throw CorruptSaveException("Bad postseason OT"),
                homeQScore = pipe[11].split('#').map { it.toIntOrNull() ?: 0 }.padQuarters(),
                awayQScore = pipe[12].split('#').map { it.toIntOrNull() ?: 0 }.padQuarters(),
            )
        } else {
            null
        }
        return PostseasonGameDoc(
            gameName = pipe[0],
            homeAbbr = pipe[1],
            awayAbbr = pipe[2],
            played = played,
            result = result,
        )
    }

    private fun emitPostseasonBlock(sb: StringBuilder, doc: PostseasonDoc) {
        sb.append("POSTSEASON\n")
        sb.append("FIELD")
        for (abbr in doc.cfpField) {
            sb.append(',').append(abbr)
        }
        sb.append('\n')
        sb.append("AUTO")
        for (abbr in doc.cfpAutoBids) {
            sb.append(',').append(abbr)
        }
        sb.append('\n')
        doc.cfpFirstRound?.let { emitPostseasonRound(sb, "FR", it) }
        doc.cfpQuarters?.let { emitPostseasonRound(sb, "QF", it) }
        doc.cfpSemis?.let { emitPostseasonRound(sb, "SF", it) }
        doc.ncg?.let { ncg ->
            sb.append("NCG\n")
            sb.append(emitPostseasonGame(ncg)).append('\n')
            sb.append("END_NCG\n")
        }
        sb.append("BOWLS\n")
        for (g in doc.bowlGames) {
            sb.append(emitPostseasonGame(g)).append('\n')
        }
        sb.append("END_BOWLS\n")
        sb.append("CCG\n")
        for (ccg in doc.conferenceCcgs) {
            sb.append(ccg.conference).append('|').append(emitPostseasonGame(ccg.game)).append('\n')
        }
        sb.append("END_CCG\n")
        sb.append("END_POSTSEASON\n")
    }

    private fun emitPostseasonRound(sb: StringBuilder, tag: String, games: List<PostseasonGameDoc>) {
        sb.append(tag).append('\n')
        for (g in games) {
            sb.append(emitPostseasonGame(g)).append('\n')
        }
        sb.append("END_").append(tag).append('\n')
    }

    private fun emitPostseasonGame(game: PostseasonGameDoc): String {
        return buildString {
            append(game.gameName).append('|')
                .append(game.homeAbbr).append('|')
                .append(game.awayAbbr).append('|')
                .append(if (game.played) 1 else 0)
            if (game.played) {
                val r = game.result ?: throw CorruptSaveException("Played postseason game missing result")
                append('|').append(r.homeScore).append('|').append(r.awayScore).append('|')
                    .append(r.homeYards).append('|').append(r.awayYards).append('|')
                    .append(r.homeTOs).append('|').append(r.awayTOs).append('|')
                    .append(r.numOT).append('|')
                    .append(r.homeQScore.padQuarters().joinToString("#")).append('|')
                    .append(r.awayQScore.padQuarters().joinToString("#"))
            }
        }
    }

    private fun List<Int>.padQuarters(): List<Int> {
        val out = MutableList(4) { 0 }
        for (i in indices) {
            if (i < 4) out[i] = this[i]
        }
        return out
    }
}

open class SaveException(message: String, cause: Throwable? = null) : Exception(message, cause)

class CorruptSaveException(message: String, cause: Throwable? = null) : SaveException(message, cause)

class IncompatibleSaveException(message: String) : SaveException(message)
