package achijones.footballcoach.save

import CFBsimPack.League

/**
 * Maps live [League] state to versioned [SaveDocument] JSON and back.
 *
 * v13 documents are typed league snapshots. Room stores the whole JSON gzip+Base64
 * (`gz1:…`). CFB text and v10–v12 envelopes are import-only via [migrateToCurrent].
 */
object CareerSaveMapper {
    private val json = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        prettyPrint = false
    }

    private val prettyJson = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    /** Compact JSON for in-memory use (tests / intermediate). */
    fun encode(document: SaveDocument): String =
        json.encodeToString(SaveDocument.serializer(), document)

    /** Room / slot storage: gzip the entire JSON document. */
    fun encodeForStorage(document: SaveDocument): String =
        SaveCompression.pack(encode(document))

    fun encodePretty(document: SaveDocument): String =
        prettyJson.encodeToString(SaveDocument.serializer(), document)

    /** Parse JSON (optionally gz-packed) without migrating. */
    fun parse(text: String): SaveDocument {
        val jsonText = if (SaveCompression.isPacked(text)) {
            SaveCompression.unpack(text)
        } else {
            text
        }
        return json.decodeFromString(SaveDocument.serializer(), jsonText)
    }

    /**
     * Parse and migrate to v13. [namesCSV]/[lastNamesCSV] required when the
     * payload is still v10–v12 (hydration goes through League).
     */
    fun decode(
        text: String,
        namesCSV: String? = null,
        lastNamesCSV: String? = null,
    ): SaveDocument {
        val raw = parse(text)
        val doc = migrateToCurrent(raw, namesCSV, lastNamesCSV)
        validate(doc)
        return doc
    }

    fun migrateToCurrent(
        doc: SaveDocument,
        namesCSV: String? = null,
        lastNamesCSV: String? = null,
    ): SaveDocument {
        return when (doc.saveVersion) {
            CURRENT_SAVE_VERSION -> sanitizeV13(doc)
            12 -> {
                if (doc.cfbPayload.isBlank()) {
                    throw CorruptSaveException("Missing cfbPayload")
                }
                val names = namesCSV
                    ?: throw CorruptSaveException("Names required to migrate v12")
                val last = lastNamesCSV
                    ?: throw CorruptSaveException("Names required to migrate v12")
                val league = try {
                    League.fromSaveString(LegacyCfbBridge.unpackCfb(doc.cfbPayload), names, last)
                } catch (e: Exception) {
                    throw CorruptSaveException("Unable to migrate v12: ${e.message}", e)
                }
                fromLeague(league)
            }
            10, 11 -> {
                if (doc.teams.isEmpty()) {
                    throw CorruptSaveException("Legacy save has no teams")
                }
                validateLegacyStructured(doc)
                val names = namesCSV
                    ?: throw CorruptSaveException("Names required to migrate v${doc.saveVersion}")
                val last = lastNamesCSV
                    ?: throw CorruptSaveException("Names required to migrate v${doc.saveVersion}")
                val cfb = LegacyCfbBridge.toCfbText(doc)
                val league = try {
                    League.fromSaveString(cfb, names, last)
                } catch (e: Exception) {
                    throw CorruptSaveException("Unable to migrate v${doc.saveVersion}: ${e.message}", e)
                }
                fromLeague(league)
            }
            else -> throw IncompatibleSaveException("Unsupported save version ${doc.saveVersion}")
        }
    }

    private fun sanitizeV13(doc: SaveDocument): SaveDocument {
        if (doc.teams.isEmpty()) {
            throw CorruptSaveException("Missing teams")
        }
        return doc.copy(
            cfbPayload = "",
            leagueRecordLines = emptyList(),
            leagueWinStreakCsv = "",
            userTeamRecordLines = emptyList(),
            teams = doc.teams.map { t ->
                t.copy(
                    profileCsv = "",
                    playerLines = emptyList(),
                    specialTeamsDepth = null,
                )
            },
            oocContracts = doc.oocContracts?.copy(contractLines = emptyList()),
            offseason = doc.offseason?.copy(hsClassLines = emptyList())?.let { off ->
                off.copy(
                    portal = off.portal.map { p ->
                        if (p.player != null) p.copy(playerLine = "") else p
                    },
                )
            },
            teamSeason = doc.teamSeason?.map { it.copy(winStreakCsv = "") },
        )
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
        if (doc.teams.isEmpty()) {
            throw CorruptSaveException("Save has no teams")
        }
        if (doc.currentWeek < 0) {
            throw CorruptSaveException("Invalid currentWeek")
        }
    }

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

    fun fromLeague(league: League): SaveDocument = LeagueSaveWriter.fromLeague(league)

    fun toLeague(document: SaveDocument, namesCSV: String, lastNamesCSV: String): League {
        val doc = if (document.saveVersion == CURRENT_SAVE_VERSION) {
            validate(document)
            document
        } else {
            val migrated = migrateToCurrent(document, namesCSV, lastNamesCSV)
            validate(migrated)
            migrated
        }
        return LeagueSaveReader.toLeague(doc, namesCSV, lastNamesCSV)
    }

    fun summaryLine(document: SaveDocument): String = document.summary

    fun packCfb(cfb: String): String = LegacyCfbBridge.packCfb(cfb)

    fun unpackCfb(packed: String): String = LegacyCfbBridge.unpackCfb(packed)

    fun fromCfbText(cfb: String, leagueHint: League? = null): SaveDocument =
        LegacyCfbBridge.fromCfbText(cfb, leagueHint)

    fun toCfbText(doc: SaveDocument): String = LegacyCfbBridge.toCfbText(doc)
}

open class SaveException(message: String, cause: Throwable? = null) : Exception(message, cause)

class CorruptSaveException(message: String, cause: Throwable? = null) : SaveException(message, cause)

class IncompatibleSaveException(message: String) : SaveException(message)
