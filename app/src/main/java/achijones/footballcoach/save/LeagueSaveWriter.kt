package achijones.footballcoach.save

import CFBsimPack.DefensiveSystem
import CFBsimPack.Game
import CFBsimPack.League
import CFBsimPack.LeagueRecords
import CFBsimPack.OffensivePhilosophy
import CFBsimPack.OffseasonSession
import CFBsimPack.ProgramProfile
import CFBsimPack.QbPressurePolicy
import CFBsimPack.Team
import CFBsimPack.TeamStreak

/** Maps live [League] state into a typed v13 [SaveDocument]. */
object LeagueSaveWriter {
    fun fromLeague(league: League): SaveDocument {
        val offPhase = if (OffseasonSession.ready() && OffseasonSession.league === league) {
            OffseasonSession.phase.name
        } else {
            null
        }
        val summary = buildSummary(league, offPhase)
        return SaveDocument(
            saveVersion = CURRENT_SAVE_VERSION,
            summary = summary,
            currentWeek = league.currentWeek,
            hasScheduledBowls = league.hasScheduledBowls,
            userTeamAbbr = league.userTeam.abbr,
            offseasonPhase = offPhase,
            leagueHistory = league.leagueHistory.map { it.toList() },
            heismanHistory = ArrayList(league.heismanHistory),
            teams = league.teamList.map { teamDoc(it) },
            userTeamHistory = ArrayList(league.userTeam.teamHistory),
            leagueRecords = parseRecords(league.leagueRecords),
            leagueWinStreak = streakDoc(league.yearStartLongestWinStreak),
            userTeamRecords = parseRecords(league.userTeamRecords),
            hallOfFame = ArrayList(league.userTeam.hallOfFame),
            schedule = scheduleDocs(league),
            teamSeason = teamSeasonDocs(league),
            postseason = postseasonDoc(league),
            oocContracts = oocDoc(league),
            offseason = offseasonDoc(league, offPhase),
        )
    }

    private fun buildSummary(league: League, offPhase: String?): String {
        val user = league.userTeam
        val offTag = if (offPhase != null) "[OFF:$offPhase]" else ""
        return "${league.year}: ${user.abbr} (${user.totalWins - user.wins}-${user.totalLosses - user.losses}) " +
            "${user.totalCCs} CCs, ${user.totalNCs} NCs>$offTag"
    }

    private fun teamDoc(t: Team): TeamDoc {
        val profile = t.programProfile
        val off = t.offPhilosophy ?: OffensivePhilosophy.MULTIPLE
        val def = t.defSystem ?: DefensiveSystem.BASE_4_3
        val qb = t.qbPressurePolicy ?: QbPressurePolicy.defaults()
        return TeamDoc(
            conference = t.conference,
            name = t.name,
            abbr = t.abbr,
            profile = profileDoc(profile),
            careerWins = t.totalWins - t.wins,
            careerLosses = t.totalLosses - t.losses,
            totalCCs = t.totalCCs,
            totalNCs = t.totalNCs,
            rivalries = t.rivalries.map { RivalryDoc(it.opponentAbbr, it.strength) },
            totalNCLosses = t.totalNCLosses,
            totalCCLosses = t.totalCCLosses,
            totalBowls = t.totalBowls,
            totalBowlLosses = t.totalBowlLosses,
            showPopups = t.showPopups,
            yearStartWinStreak = streakDoc(t.yearStartWinStreak),
            teamTVDeal = t.teamTVDeal,
            confTVDeal = t.confTVDeal,
            offPhilosophy = off.name,
            defSystem = def.name,
            programProfileUpdatedThisOffseason = t.programProfileUpdatedThisOffseason,
            qbPressure = QbPressureDoc(
                normal = qb.normal.name,
                convert = qb.convert.name,
                protectLead = qb.protectLead.name,
                lateTrailing = qb.lateTrailing.name,
                backedUp = qb.backedUp.name,
            ),
            evenYearHomeOpp = t.evenYearHomeOpp ?: "",
            players = t.allPlayers.map { PlayerSaveMapping.fromPlayer(it) },
            specialTeams = specialTeamsDoc(t),
        )
    }

    private fun profileDoc(p: ProgramProfile): ProgramProfileDoc {
        val finishes = p.finishHistoryCsv().split(',').mapNotNull { it.toIntOrNull() }
        val drafts = p.draftHistoryCsv().split(',').mapNotNull { it.toIntOrNull() }
        val deltas = p.annualDeltaCsv().split(':')
        return ProgramProfileDoc(
            tradition = p.tradition,
            fanbase = p.fanbase,
            donors = p.donors,
            footprint = p.footprint,
            pipeline = p.pipeline,
            momentum = p.momentum,
            finishHistory = finishes,
            draftHistory = drafts,
            diffProgramPower = deltas.getOrNull(0)?.toIntOrNull() ?: 0,
            diffMomentum = deltas.getOrNull(1)?.toIntOrNull() ?: 0,
            diffDonors = deltas.getOrNull(2)?.toIntOrNull() ?: 0,
            diffFanbase = deltas.getOrNull(3)?.toIntOrNull() ?: 0,
            diffTradition = deltas.getOrNull(4)?.toIntOrNull() ?: 0,
            diffFootprint = deltas.getOrNull(5)?.toIntOrNull() ?: 0,
            diffPipeline = deltas.getOrNull(6)?.toIntOrNull() ?: 0,
        )
    }

    private fun specialTeamsDoc(t: Team): SpecialTeamsDepthDoc {
        t.ensureSpecialTeamsDepth()
        return SpecialTeamsDepthDoc(
            puntReturner = refOrNull(t.puntReturner),
            kickReturner = refOrNull(t.kickReturner),
            gunner1 = refOrNull(t.gunner1),
            gunner2 = refOrNull(t.gunner2),
            longSnapper = refOrNull(t.longSnapper),
        )
    }

    private fun refOrNull(p: CFBsimPack.Player?): PlayerRefDoc? {
        if (p == null) return null
        return PlayerRefDoc(p.position, p.name, p.year)
    }

    private fun streakDoc(s: TeamStreak?): StreakDoc {
        if (s == null) return StreakDoc()
        return StreakDoc(
            length = s.streakLength,
            team = s.team,
            startYear = s.startYear,
            endYear = s.endYear,
        )
    }

    private fun parseRecords(records: LeagueRecords): List<RecordDoc> {
        return records.orderedRecordEntries.map { entry ->
            RecordDoc(
                key = entry.key,
                number = entry.number,
                holder = entry.holder,
                year = entry.year,
            )
        }
    }

    private fun scheduleDocs(league: League): List<ScheduleTeamDoc> {
        return league.teamList.map { team ->
            val weeks = ArrayList<ScheduleSlotDoc>(League.REGULAR_SEASON_WEEKS)
            for (week in 0 until League.REGULAR_SEASON_WEEKS) {
                if (team.byeWeek == week) {
                    weeks.add(ScheduleSlotDoc(kind = "BYE"))
                    continue
                }
                val game = if (week < team.gameSchedule.size) team.gameSchedule[week] else null
                if (game == null) {
                    weeks.add(ScheduleSlotDoc(kind = "EMPTY"))
                } else {
                    weeks.add(slotFromGame(game, homeSide = game.homeTeam === team))
                }
            }
            ScheduleTeamDoc(teamAbbr = team.abbr, byeWeek = team.byeWeek, weeks = weeks)
        }
    }

    private fun slotFromGame(game: Game, homeSide: Boolean): ScheduleSlotDoc {
        val opp = if (homeSide) game.awayTeam.abbr else game.homeTeam.abbr
        if (!game.hasPlayed) {
            return ScheduleSlotDoc(kind = "MATCHUP", home = homeSide, opponentAbbr = opp, played = false)
        }
        val result = if (homeSide) {
            GameResultDoc(
                homeScore = game.homeScore,
                awayScore = game.awayScore,
                homeYards = game.homeYards,
                awayYards = game.awayYards,
                homeTOs = game.homeTOs,
                awayTOs = game.awayTOs,
                numOT = game.numOT,
                homeQScore = game.homeQScore.toList().padQuarters(),
                awayQScore = game.awayQScore.toList().padQuarters(),
            )
        } else {
            null
        }
        return ScheduleSlotDoc(
            kind = "MATCHUP",
            home = homeSide,
            opponentAbbr = opp,
            played = true,
            result = result,
        )
    }

    private fun teamSeasonDocs(league: League): List<TeamSeasonDoc> {
        return league.teamList.map { t ->
            TeamSeasonDoc(
                abbr = t.abbr,
                wins = t.wins,
                losses = t.losses,
                teamPoints = t.teamPoints,
                teamOppPoints = t.teamOppPoints,
                teamYards = t.teamYards,
                teamOppYards = t.teamOppYards,
                teamPassYards = t.teamPassYards,
                teamRushYards = t.teamRushYards,
                teamOppPassYards = t.teamOppPassYards,
                teamOppRushYards = t.teamOppRushYards,
                teamTODiff = t.teamTODiff,
                winStreak = streakDoc(t.winStreak),
                gameWLSchedule = ArrayList(t.gameWLSchedule),
                gameWinsAgainstAbbrs = t.gameWinsAgainst.map { it.abbr },
                rivalryResults = LinkedHashMap(t.rivalryResults ?: emptyMap()),
                confChampion = t.confChampion ?: "",
                semiFinalWL = t.semiFinalWL ?: "",
                natChampWL = t.natChampWL ?: "",
            )
        }
    }

    private fun postseasonDoc(league: League): PostseasonDoc? {
        val hasAny = league.cfpField.isNotEmpty()
            || league.bowlGames.isNotEmpty()
            || league.ncg != null
            || league.conferences.any { it.ccg != null }
        if (!hasAny && league.cfpFirstRound == null) return null
        return PostseasonDoc(
            cfpField = league.cfpField.map { it.abbr },
            cfpAutoBids = league.cfpAutoBids.map { it.abbr },
            cfpFirstRound = league.cfpFirstRound?.map { postGame(it) },
            cfpQuarters = league.cfpQuarters?.map { postGame(it) },
            cfpSemis = league.cfpSemis?.map { postGame(it) },
            ncg = league.ncg?.let { postGame(it) },
            bowlGames = league.bowlGames.map { postGame(it) },
            conferenceCcgs = league.conferences.mapNotNull { conf ->
                val g = conf.ccg ?: return@mapNotNull null
                ConferenceCcgDoc(conference = conf.confName, game = postGame(g))
            },
        )
    }

    private fun postGame(game: Game): PostseasonGameDoc {
        val played = game.hasPlayed
        val result = if (played) {
            GameResultDoc(
                homeScore = game.homeScore,
                awayScore = game.awayScore,
                homeYards = game.homeYards,
                awayYards = game.awayYards,
                homeTOs = game.homeTOs,
                awayTOs = game.awayTOs,
                numOT = game.numOT,
                homeQScore = game.homeQScore.toList().padQuarters(),
                awayQScore = game.awayQScore.toList().padQuarters(),
            )
        } else {
            null
        }
        return PostseasonGameDoc(
            gameName = game.gameName ?: "",
            homeAbbr = game.homeTeam.abbr,
            awayAbbr = game.awayTeam.abbr,
            played = played,
            result = result,
        )
    }

    private fun oocDoc(league: League): OocBookDoc? {
        val book = league.oocContracts ?: return null
        return OocBookDoc(
            nextId = book.nextId,
            contracts = book.all().map { c ->
                OocContractDoc(
                    id = c.id,
                    teamA = c.teamA,
                    teamB = c.teamB,
                    startYear = c.startYear,
                    lengthYears = c.lengthYears,
                    type = c.type.name,
                    mustFulfillByYear = c.mustFulfillByYear,
                    buyout = c.buyout,
                    games = c.games.map { g ->
                        OocGameDoc(
                            year = g.year,
                            homeAbbr = g.homeAbbr,
                            awayAbbr = g.awayAbbr,
                            guarantee = g.guarantee,
                            winBonus = g.winBonus,
                            settled = g.settled,
                            preferredWeek = g.preferredWeek,
                        )
                    },
                )
            },
        )
    }

    private fun offseasonDoc(league: League, offPhase: String?): OffseasonSaveDoc? {
        if (offPhase == null || !OffseasonSession.ready() || OffseasonSession.league !== league) {
            return null
        }
        val off = OffseasonSession.offseason
        return OffseasonSaveDoc(
            phase = offPhase,
            budgets = league.teamList.map { TeamBudgetDoc(it.abbr, it.recruitMoney) },
            retained = league.teamList.flatMap { t ->
                t.allPlayers.filter { it.retainedThisOffseason }.map {
                    RetainedKeyDoc(t.abbr, it.position, it.name, it.year)
                }
            },
            portal = (off?.transferPortal ?: emptyList()).map { p ->
                val prior = p.priorTeam?.abbr ?: p.team?.abbr ?: "XXX"
                PortalPlayerDoc(priorTeamAbbr = prior, player = PlayerSaveMapping.fromPlayer(p))
            },
            hsClass = (off?.hsClass ?: emptyList()).map { PlayerSaveMapping.fromPlayer(it) },
        )
    }
}

internal fun List<Int>.padQuarters(): List<Int> {
    val out = MutableList(4) { 0 }
    for (i in indices) {
        if (i < 4) out[i] = this[i]
    }
    return out
}
