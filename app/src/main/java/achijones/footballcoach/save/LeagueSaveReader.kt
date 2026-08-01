package achijones.footballcoach.save

import CFBsimPack.Injury
import CFBsimPack.League
import CFBsimPack.Player
import CFBsimPack.Team

/**
 * Hydrates a [League] from a typed v13 [SaveDocument].
 *
 * Implementation detail: emit CFB from the typed snapshot then reuse
 * [League.fromSaveString] so schedule/postseason/offseason restore stays parity-correct.
 * Stored careers never contain CFB — only this in-memory bridge does.
 * After hydrate, fields the CFB codec never carried (return stats, redshirt) are
 * reapplied from the typed document.
 */
object LeagueSaveReader {
    fun toLeague(document: SaveDocument, namesCSV: String, lastNamesCSV: String): League {
        if (document.teams.isEmpty()) {
            throw CorruptSaveException("Save has no teams")
        }
        val cfb = LegacyCfbBridge.toCfbText(document)
        val league = try {
            League.fromSaveString(cfb, namesCSV, lastNamesCSV)
        } catch (e: Exception) {
            throw CorruptSaveException("Unable to hydrate league: ${e.message}", e)
        }
        reapplyTypedExtras(league, document)
        return league
    }

    private fun reapplyTypedExtras(league: League, document: SaveDocument) {
        for (teamDoc in document.teams) {
            val team = league.findTeamAbbr(teamDoc.abbr) ?: continue
            for (playerDoc in teamDoc.players) {
                val player = findPlayer(team, playerDoc) ?: continue
                applyExtras(player, playerDoc)
            }
        }
        val off = document.offseason ?: return
        val sessionOff = CFBsimPack.OffseasonSession.offseason ?: return
        for (portal in off.portal) {
            val doc = portal.player ?: continue
            val live = sessionOff.transferPortal.firstOrNull {
                it.position == doc.position && it.name == doc.name && it.year == doc.year
            } ?: continue
            applyExtras(live, doc)
        }
        for (doc in off.hsClass) {
            val live = sessionOff.hsClass.firstOrNull {
                it.position == doc.position && it.name == doc.name && it.year == doc.year
            } ?: continue
            applyExtras(live, doc)
        }
    }

    private fun findPlayer(team: Team, doc: PlayerDoc): Player? {
        return team.allPlayers.firstOrNull {
            it.position == doc.position && it.name == doc.name && it.year == doc.year
        }
    }

    private fun applyExtras(player: Player, doc: PlayerDoc) {
        player.isRedshirt = doc.isRedshirt
        player.careerPrAtt = doc.careerPrAtt
        player.careerPrYards = doc.careerPrYards
        player.careerPrTd = doc.careerPrTd
        player.careerKrAtt = doc.careerKrAtt
        player.careerKrYards = doc.careerKrYards
        player.careerKrTd = doc.careerKrTd
        player.careerFairCatches = doc.careerFairCatches
        val season = doc.season
        if (season != null) {
            player.statsPrAtt = season.prAtt
            player.statsPrYards = season.prYards
            player.statsPrTd = season.prTd
            player.statsKrAtt = season.krAtt
            player.statsKrYards = season.krYards
            player.statsKrTd = season.krTd
            player.statsFairCatches = season.fairCatches
            val desc = season.injuryDescription
            if (desc != null && desc.isNotBlank() && season.injuryDuration > 0 && player.injury == null) {
                player.injury = Injury(season.injuryDuration, desc, player)
            }
        }
    }
}
