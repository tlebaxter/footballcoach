package achijones.footballcoach.save

import CFBsimPack.Conference
import CFBsimPack.DefensiveSystem
import CFBsimPack.DepthChart
import CFBsimPack.Game
import CFBsimPack.League
import CFBsimPack.LeagueOffseason
import CFBsimPack.NilMoney
import CFBsimPack.OffensivePhilosophy
import CFBsimPack.OffseasonSession
import CFBsimPack.OocContract
import CFBsimPack.OocContractGame
import CFBsimPack.Player
import CFBsimPack.PressureResponse
import CFBsimPack.ProgramProfile
import CFBsimPack.QbPressurePolicy
import CFBsimPack.Rivalry
import CFBsimPack.Team
import CFBsimPack.TeamStreak

/**
 * Builds a live [League] from a typed v13 [SaveDocument] without CFB text.
 */
internal object LeagueHydrator {
    fun fromDocument(document: SaveDocument, namesCSV: String, lastNamesCSV: String): League {
        if (document.teams.isEmpty()) {
            throw CorruptSaveException("Save has no teams")
        }
        val league = League.createEmptyShell(namesCSV, lastNamesCSV)
        league.leagueHistory = ArrayList(document.leagueHistory.map { it.toTypedArray() })
        league.heismanHistory = ArrayList(document.heismanHistory)

        for (teamDoc in document.teams) {
            val team = teamFromDoc(league, teamDoc)
            for (playerDoc in teamDoc.players) {
                val player = PlayerSaveMapping.toPlayer(playerDoc, team)
                team.addPlayerToRoster(player)
                if (player.isInjured && !team.playersInjuredAll.contains(player)) {
                    team.playersInjuredAll.add(player)
                }
            }
            team.regroupClassStanding()
            DepthChart.applySystems(team)
            applySpecialTeams(team, teamDoc.specialTeams)
            league.getOrCreateConference(team.conference).confTeams.add(team)
            league.teamList.add(team)
        }

        val user = league.findTeamAbbrOrNull(document.userTeamAbbr)
            ?: throw CorruptSaveException("Missing user team ${document.userTeamAbbr}")
        user.userControlled = true
        user.teamHistory = ArrayList(document.userTeamHistory)
        user.hallOfFame = ArrayList(document.hallOfFame)
        league.userTeam = user

        applyRecords(league.leagueRecords, document.leagueRecords)
        applyRecords(league.userTeamRecords, document.userTeamRecords)
        val streak = streakFromDoc(document.leagueWinStreak)
        league.yearStartLongestWinStreak = streak
        league.longestWinStreak = TeamStreak(
            streak.startYear,
            streak.endYear,
            streak.streakLength,
            streak.team,
        )

        league.currentWeek = document.currentWeek
        league.hasScheduledBowls = document.hasScheduledBowls

        val scheduleRestored = document.schedule != null
        if (scheduleRestored) {
            restoreSchedule(league, document.schedule!!)
        }

        if (document.teamSeason != null) {
            restoreTeamSeason(league, document.teamSeason!!)
        }

        if (document.postseason != null) {
            restorePostseason(league, document.postseason!!)
        }

        if (document.oocContracts != null) {
            restoreOoc(league, document.oocContracts!!)
            if (scheduleRestored) {
                league.oocContracts.relinkScheduleContractIds()
            }
        }

        if (document.offseason != null) {
            restoreOffseason(league, document.offseason!!)
        } else if (!scheduleRestored) {
            league.prepareSeasonSchedule()
            league.completeOocSchedule()
        }

        league.updateLongestActiveWinStreak()
        if (scheduleRestored || document.currentWeek > 0 || document.hasScheduledBowls) {
            league.syncConferenceWeeksFromCurrentWeek()
            league.setTeamRanks()
        }
        return league
    }

    private fun teamFromDoc(league: League, doc: TeamDoc): Team {
        val profile = ProgramProfile(
            doc.profile.tradition,
            doc.profile.fanbase,
            doc.profile.donors,
            doc.profile.footprint,
            doc.profile.pipeline,
            doc.profile.momentum,
            Conference.mediaShareFor(doc.conference),
        )
        profile.restoreHistory(
            doc.profile.finishHistory.joinToString(","),
            doc.profile.draftHistory.joinToString(","),
        )
        profile.restoreAnnualDeltas(
            listOf(
                doc.profile.diffProgramPower,
                doc.profile.diffMomentum,
                doc.profile.diffDonors,
                doc.profile.diffFanbase,
                doc.profile.diffTradition,
                doc.profile.diffFootprint,
                doc.profile.diffPipeline,
            ).joinToString(":"),
        )
        profile.refreshDerived(Conference.mediaShareFor(doc.conference))

        val rivalries = doc.rivalries.map { Rivalry(it.opponentAbbr, it.strength) }
        val streak = streakFromDoc(doc.yearStartWinStreak)
        val off = try {
            OffensivePhilosophy.valueOf(doc.offPhilosophy)
        } catch (_: Exception) {
            OffensivePhilosophy.MULTIPLE
        }
        val def = try {
            DefensiveSystem.valueOf(doc.defSystem)
        } catch (_: Exception) {
            DefensiveSystem.BASE_4_3
        }
        val qb = QbPressurePolicy(
            pressure(doc.qbPressure.normal, PressureResponse.AUTO),
            pressure(doc.qbPressure.convert, PressureResponse.TAKE_THE_FIRST_DOWN),
            pressure(doc.qbPressure.protectLead, PressureResponse.SLIDE_SECURE),
            pressure(doc.qbPressure.lateTrailing, PressureResponse.SCRAMBLE_FOR_IT),
            pressure(doc.qbPressure.backedUp, PressureResponse.THROW_IT_AWAY),
        )
        return Team.createHydrateShell(
            league,
            doc.conference,
            doc.name,
            doc.abbr,
            profile,
            doc.careerWins,
            doc.careerLosses,
            doc.totalCCs,
            doc.totalNCs,
            rivalries,
            doc.totalNCLosses,
            doc.totalCCLosses,
            doc.totalBowls,
            doc.totalBowlLosses,
            doc.showPopups,
            streak,
            doc.teamTVDeal,
            doc.confTVDeal,
            off,
            def,
            doc.programProfileUpdatedThisOffseason,
            qb,
            doc.evenYearHomeOpp,
        )
    }

    private fun pressure(name: String, fallback: PressureResponse): PressureResponse {
        return try {
            PressureResponse.valueOf(name)
        } catch (_: Exception) {
            fallback
        }
    }

    private fun streakFromDoc(doc: StreakDoc): TeamStreak {
        return TeamStreak(doc.startYear, doc.endYear, doc.length, doc.team.ifBlank { "XXX" })
    }

    private fun applyRecords(records: CFBsimPack.LeagueRecords, docs: List<RecordDoc>) {
        for (doc in docs) {
            if (doc.number != -1) {
                records.checkRecord(doc.key, doc.number, doc.holder, doc.year)
            }
        }
    }

    private fun applySpecialTeams(team: Team, st: SpecialTeamsDepthDoc) {
        fun find(ref: PlayerRefDoc?): Player? {
            if (ref == null) return null
            return team.allPlayers.firstOrNull {
                it.position == ref.position && it.name == ref.name && it.year == ref.year
            }
        }
        team.puntReturner = find(st.puntReturner)
        team.kickReturner = find(st.kickReturner)
        team.gunner1 = find(st.gunner1)
        team.gunner2 = find(st.gunner2)
        team.longSnapper = find(st.longSnapper)
        team.ensureSpecialTeamsDepth()
    }

    private fun restoreSchedule(league: League, schedule: List<ScheduleTeamDoc>) {
        for (team in league.teamList) {
            team.gameSchedule.clear()
            team.byeWeek = -1
            repeat(League.REGULAR_SEASON_WEEKS) { team.gameSchedule.add(null) }
        }
        for (teamDoc in schedule) {
            val team = league.findTeamAbbrOrNull(teamDoc.teamAbbr) ?: continue
            team.byeWeek = teamDoc.byeWeek
            for (week in teamDoc.weeks.indices) {
                if (week >= League.REGULAR_SEASON_WEEKS) break
                val slot = teamDoc.weeks[week]
                when (slot.kind) {
                    "BYE", "EMPTY" -> Unit
                    "MATCHUP" -> applyMatchup(league, team, week, slot)
                }
            }
        }
        for (conference in league.conferences) {
            conference.resetSeason()
        }
    }

    private fun applyMatchup(league: League, team: Team, week: Int, slot: ScheduleSlotDoc) {
        val opponent = league.findTeamAbbrOrNull(slot.opponentAbbr) ?: return
        var game = team.gameSchedule.getOrNull(week)
        if (game == null) {
            val sameConf = team.conference == opponent.conference
            val name = if (sameConf) "In Conf" else "OOC"
            val homeTeam = if (slot.home) team else opponent
            val awayTeam = if (slot.home) opponent else team
            game = Game(homeTeam, awayTeam, name)
            homeTeam.gameSchedule[week] = game
            awayTeam.gameSchedule[week] = game
        }
        if (slot.home && slot.played && slot.result != null && !game.hasPlayed) {
            applyResult(game, slot.result)
        } else if (slot.played && !game.hasPlayed && slot.result != null) {
            applyResult(game, slot.result)
        }
    }

    private fun applyResult(game: Game, result: GameResultDoc) {
        League.applyGameResult(
            game,
            result.homeScore,
            result.awayScore,
            result.homeYards,
            result.awayYards,
            result.homeTOs,
            result.awayTOs,
            result.numOT,
            result.homeQScore.toIntArray(),
            result.awayQScore.toIntArray(),
        )
    }

    private fun restoreTeamSeason(league: League, seasons: List<TeamSeasonDoc>) {
        for (doc in seasons) {
            val t = league.findTeamAbbrOrNull(doc.abbr) ?: continue
            val careerWins = t.totalWins
            val careerLosses = t.totalLosses
            t.wins = doc.wins
            t.losses = doc.losses
            t.totalWins = careerWins + doc.wins
            t.totalLosses = careerLosses + doc.losses
            t.teamPoints = doc.teamPoints
            t.teamOppPoints = doc.teamOppPoints
            t.teamYards = doc.teamYards
            t.teamOppYards = doc.teamOppYards
            t.teamPassYards = doc.teamPassYards
            t.teamRushYards = doc.teamRushYards
            t.teamOppPassYards = doc.teamOppPassYards
            t.teamOppRushYards = doc.teamOppRushYards
            t.teamTODiff = doc.teamTODiff
            t.winStreak = streakFromDoc(doc.winStreak)
            t.gameWLSchedule = ArrayList(doc.gameWLSchedule)
            t.gameWinsAgainst.clear()
            for (abbr in doc.gameWinsAgainstAbbrs) {
                val opp = league.findTeamAbbrOrNull(abbr)
                if (opp != null) t.gameWinsAgainst.add(opp)
            }
            if (t.rivalryResults == null) {
                t.rivalryResults = HashMap()
            } else {
                t.rivalryResults.clear()
            }
            t.rivalryResults.putAll(doc.rivalryResults)
            t.confChampion = doc.confChampion
            t.semiFinalWL = doc.semiFinalWL
            t.natChampWL = doc.natChampWL
        }
    }

    private fun restorePostseason(league: League, doc: PostseasonDoc) {
        league.cfpField = ArrayList()
        league.cfpAutoBids = HashSet()
        for (abbr in doc.cfpField) {
            league.findTeamAbbrOrNull(abbr)?.let { league.cfpField.add(it) }
        }
        for (abbr in doc.cfpAutoBids) {
            league.findTeamAbbrOrNull(abbr)?.let { league.cfpAutoBids.add(it) }
        }
        league.cfpFirstRound = doc.cfpFirstRound?.mapNotNull { postGame(league, it) }?.toTypedArray()
        league.cfpQuarters = doc.cfpQuarters?.mapNotNull { postGame(league, it) }?.toTypedArray()
        league.cfpSemis = doc.cfpSemis?.mapNotNull { postGame(league, it) }?.toTypedArray()
        league.ncg = doc.ncg?.let { postGame(league, it) }
        league.bowlGames = doc.bowlGames.mapNotNull { postGame(league, it) }.toTypedArray()
        for (ccg in doc.conferenceCcgs) {
            val conf = league.findConferenceOrNull(ccg.conference) ?: continue
            val game = postGame(league, ccg.game) ?: continue
            conf.restoreCcg(game)
        }
    }

    private fun postGame(league: League, doc: PostseasonGameDoc): Game? {
        val home = league.findTeamAbbrOrNull(doc.homeAbbr) ?: return null
        val away = league.findTeamAbbrOrNull(doc.awayAbbr) ?: return null
        val game = Game(home, away, doc.gameName)
        if (doc.played && doc.result != null) {
            applyResult(game, doc.result)
        }
        home.gameSchedule.add(game)
        away.gameSchedule.add(game)
        return game
    }

    private fun restoreOoc(league: League, doc: OocBookDoc) {
        val contracts = doc.contracts.map { c ->
            val type = try {
                OocContract.Type.valueOf(c.type)
            } catch (_: Exception) {
                OocContract.Type.BUY
            }
            val games = c.games.map { g ->
                OocContractGame(
                    g.year,
                    g.homeAbbr,
                    g.awayAbbr,
                    g.guarantee,
                    g.winBonus,
                    g.preferredWeek,
                ).also { it.settled = g.settled }
            }
            OocContract(
                c.id,
                c.teamA,
                c.teamB,
                c.startYear,
                c.lengthYears,
                type,
                c.mustFulfillByYear,
                c.buyout,
                games,
            )
        }
        league.oocContracts.replaceAll(doc.nextId, contracts)
    }

    private fun restoreOffseason(league: League, doc: OffseasonSaveDoc) {
        league.loadedInOffseason = true
        league.loadedOffseasonPhase = OffseasonSession.phaseFromString(doc.phase)
        val off = LeagueOffseason(league)
        league.offseason = off

        for (budget in doc.budgets) {
            val team = league.findTeamAbbrOrNull(budget.abbr) ?: continue
            team.recruitMoney = budget.recruitMoney
        }
        for (key in doc.retained) {
            val team = league.findTeamAbbrOrNull(key.teamAbbr) ?: continue
            team.allPlayers.firstOrNull {
                it.position == key.position && it.name == key.name && it.year == key.year
            }?.retainedThisOffseason = true
        }
        for (portal in doc.portal) {
            val playerDoc = portal.player ?: continue
            val p = PlayerSaveMapping.toPlayer(playerDoc, null)
            p.team = null
            p.priorTeam = league.findTeamAbbrOrNull(portal.priorTeamAbbr)
            off.transferPortal.add(p)
        }
        for (hsDoc in doc.hsClass) {
            val p = PlayerSaveMapping.toPlayer(hsDoc, null)
            p.team = null
            p.cost = NilMoney.marketValue(p)
            off.hsClass.add(p)
        }
        OffseasonSession.begin(league, off, league.loadedOffseasonPhase)
    }
}
