package achijones.footballcoach.save

import CFBsimPack.League

/**
 * Hydrates a [League] from a typed v13 [SaveDocument].
 */
object LeagueSaveReader {
    fun toLeague(document: SaveDocument, namesCSV: String, lastNamesCSV: String): League {
        return LeagueHydrator.fromDocument(document, namesCSV, lastNamesCSV)
    }
}
