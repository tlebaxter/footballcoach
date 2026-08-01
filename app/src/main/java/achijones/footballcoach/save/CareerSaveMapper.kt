package achijones.footballcoach.save

import CFBsimPack.League

/**
 * Maps live [League] state to versioned [SaveDocument] JSON and back.
 *
 * v13 documents are typed league snapshots. Room stores the whole JSON gzip+Base64
 * (`gz1:…`). Saves older than v13 are rejected at [migrateToCurrent].
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

    /** Parse and migrate to v13. Only v13 is accepted; older versions throw. */
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
            in 10..12 -> throw IncompatibleSaveException(
                "Save version ${doc.saveVersion} is no longer supported. " +
                    "Legacy CFB and v10–v12 saves cannot be loaded; export a v13 JSON save instead.",
            )
            else -> throw IncompatibleSaveException("Unsupported save version ${doc.saveVersion}")
        }
    }

    private fun sanitizeV13(doc: SaveDocument): SaveDocument {
        if (doc.teams.isEmpty()) {
            throw CorruptSaveException("Missing teams")
        }
        return doc
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
}

open class SaveException(message: String, cause: Throwable? = null) : Exception(message, cause)

class CorruptSaveException(message: String, cause: Throwable? = null) : SaveException(message, cause)

class IncompatibleSaveException(message: String) : SaveException(message)
