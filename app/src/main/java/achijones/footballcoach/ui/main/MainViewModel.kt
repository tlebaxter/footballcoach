package achijones.footballcoach.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import CFBsimPack.Conference
import CFBsimPack.Game
import CFBsimPack.GameSession
import CFBsimPack.League
import CFBsimPack.LeagueOffseason
import CFBsimPack.NilMoney
import CFBsimPack.OffseasonSession
import CFBsimPack.Player
import CFBsimPack.PlayerSeasonRecord
import CFBsimPack.RosterStatus
import CFBsimPack.DefensiveSystem
import CFBsimPack.OffensivePhilosophy
import CFBsimPack.PlayerRatings
import CFBsimPack.PositionGroup
import CFBsimPack.PositionOvr
import CFBsimPack.PressureResponse
import CFBsimPack.QbPressurePolicy
import CFBsimPack.Rivalry
import CFBsimPack.Team
import CFBsimPack.TransferReason
import achijones.footballcoach.save.CareerPersistence
import achijones.footballcoach.save.CareerSessionRestorer
import achijones.footballcoach.save.SaveRepository
import achijones.footballcoach.save.SlotStatus
import achijones.footballcoach.ui.theme.UserBrandTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = SaveRepository.get(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var league: League? = null
    private var userTeam: Team? = null
    private var currentTeam: Team? = null
    private var currentConference: Conference? = null

    private var showToasts = true
    private var showInjuryReport = true
    private var recruitingStage = -1
    private var wantUpdateConf = 2
    private var browseConfSyncPending = false

    private val playerByKey = mutableMapOf<Int, Player>()
    private val gameByKey = mutableMapOf<Int, Game>()
    private val lineupOrderKeys = mutableListOf<Int>()

    private val lineupPositions = listOf(
        "QB (1 starter)",
        "RB (2 starters)",
        "FB (1 starter)",
        "WR (3 starters)",
        "TE (1 starter)",
        "OL (5 starters)",
        "K (1 starter)",
        "P (1 starter)",
        "S (1 starter)",
        "CB (3 starters)",
        "EDGE (2 starters)",
        "DL (3 starters)",
        "LB (3 starters)",
        "PR (punt return)",
        "KR (kick return)",
        "Gunner 1",
        "Gunner 2",
        "LS (long snap)",
    )
    private val lineupRequired = intArrayOf(1, 2, 1, 3, 1, 5, 1, 1, 1, 3, 2, 3, 3, 1, 1, 1, 1, 1)
    private val stSlotBase = 13
    private val lineupPosGroups = listOf(
        PositionGroup.QB, PositionGroup.RB, PositionGroup.FB, PositionGroup.WR,
        PositionGroup.TE, PositionGroup.OL, PositionGroup.K, PositionGroup.P,
        PositionGroup.S, PositionGroup.CB, PositionGroup.EDGE, PositionGroup.DL, PositionGroup.LB,
    )
    private val rankingsModes = arrayOf(
        "Poll Votes", "Conference Standings", "Strength of Sched", "Points Per Game",
        "Opp Points Per Game", "Yards Per Game", "Opp Yards Per Game", "Pass Yards Per Game",
        "Rush Yards Per Game", "Opp Pass YPG", "Opp Rush YPG", "TO Differential",
        "Off Talent", "Def Talent", "Program Power", "Recruiting Class",
    )

    init {
        bootstrap()
    }

    /** Call whenever Main becomes visible (sync live session; offseason transitions apply at source). */
    fun onScreenEntered() {
        if (GameSession.hasLeague()) {
            continueOnScreenEntered()
            return
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                CareerSessionRestorer.resumeIfNeeded(getApplication(), repo)
            }
            when (result) {
                CareerSessionRestorer.ResumeResult.Success,
                CareerSessionRestorer.ResumeResult.AlreadyLoaded,
                -> continueOnScreenEntered()
                CareerSessionRestorer.ResumeResult.NoSlot ->
                    _uiState.update { it.copy(navigateHome = true) }
                is CareerSessionRestorer.ResumeResult.Failed ->
                    _uiState.update {
                        it.copy(navigateHome = true, snackbarMessage = result.message)
                    }
            }
        }
    }

    private fun continueOnScreenEntered() {
        league = GameSession.getLeague()
        if (GameSession.isStayingOnMainDuringOffseason() && OffseasonSession.ready()) {
            syncFromOffseasonSession()
            return
        }
        if (lLoadedInOffseason()) {
            navigateToOffseasonDestination()
            return
        }
        val l = league ?: return
        userTeam = l.userTeam
        if (userTeam == null) return
        currentTeam = currentTeam ?: userTeam
        currentConference = l.findConference(userTeam!!.conference)
        val coachSnack = handlePendingCoachResult()
        rebuildSnapshot(ready = true, snackbar = coachSnack)
    }

    /** Autosaves (if a slot is known) and returns a snackbar for a finished coach game. */
    private fun handlePendingCoachResult(): String? {
        if (!GameSession.consumePendingCoachResultSave()) return null
        val summary = GameSession.consumePendingCoachResultSummary()
        val l = league
        if (GameSession.hasActiveSaveSlot() && l != null) {
            val ok = CareerPersistence.saveActiveBlocking(l, repo)
            return if (ok) {
                if (summary != null) "Game saved · $summary" else "Game saved"
            } else {
                "Result applied — save failed"
            }
        }
        return if (summary != null) {
            "Result applied · $summary — save your career to keep it"
        } else {
            "Result applied — save your career to keep it"
        }
    }

    /** Refreshes Main against the live offseason league after browsing away from Talent Hub. */
    private fun syncFromOffseasonSession() {
        league = OffseasonSession.league
        val l = league ?: return
        GameSession.setLeague(l)
        userTeam = l.userTeam ?: return
        currentTeam = userTeam
        currentConference = l.findConference(userTeam!!.conference)
        rebuildSnapshot(ready = true)
    }

    private fun lLoadedInOffseason(): Boolean {
        val l = league ?: return false
        return l.loadedInOffseason && OffseasonSession.ready()
    }

    private fun bootstrap() {
        viewModelScope.launch {
            if (!GameSession.hasLeague()) {
                val result = withContext(Dispatchers.IO) {
                    CareerSessionRestorer.resumeIfNeeded(getApplication(), repo)
                }
                when (result) {
                    CareerSessionRestorer.ResumeResult.Success,
                    CareerSessionRestorer.ResumeResult.AlreadyLoaded,
                    -> Unit
                    CareerSessionRestorer.ResumeResult.NoSlot -> {
                        _uiState.update { it.copy(navigateHome = true) }
                        return@launch
                    }
                    is CareerSessionRestorer.ResumeResult.Failed -> {
                        _uiState.update {
                            it.copy(navigateHome = true, snackbarMessage = result.message)
                        }
                        return@launch
                    }
                }
            }
            finishBootstrap()
        }
    }

    private fun finishBootstrap() {
        league = GameSession.getLeague()
        val l = league ?: return
        if (l.loadedInOffseason && OffseasonSession.ready() &&
            !GameSession.isStayingOnMainDuringOffseason()
        ) {
            navigateToOffseasonDestination()
            return
        }
        if (GameSession.isStayingOnMainDuringOffseason() && OffseasonSession.ready()) {
            syncFromOffseasonSession()
            return
        }
        val needsPicker = GameSession.needsTeamPicker() || l.userTeam == null
        if (needsPicker) {
            val seed = l.userTeam ?: l.teamList[0]
            l.userTeam = seed
            seed.userControlled = true
            UserBrandTheme.clear()
            _uiState.update {
                it.copy(
                    showTeamPicker = true,
                    teamPickerConferences = buildTeamPickerConferences(l),
                )
            }
        }
        userTeam = l.userTeam
        currentTeam = userTeam
        currentConference = l.findConference(userTeam!!.conference)
        showToasts = userTeam!!.showPopups
        showInjuryReport = true
        l.setTeamRanks()
        if (!needsPicker) {
            UserBrandTheme.setFrom(userTeam)
        }
        if (l.getYear() != League.FIRST_SEASON_YEAR) {
            showRecruitingClassDialogInternal()
        }
        rebuildSnapshot(ready = true)
    }

    fun consumeNavigateHome() {
        _uiState.update { it.copy(navigateHome = false) }
    }

    fun consumeNavigateToTalentHub() {
        _uiState.update { it.copy(navigateToTalentHub = false, showTeamPicker = false) }
    }

    fun consumeNavigateToSchedule() {
        _uiState.update { it.copy(navigateToSchedule = false, showTeamPicker = false) }
    }

    fun openScheduleScreen() {
        _uiState.update { it.copy(navigateToSchedule = true) }
    }

    fun openTalentHub() {
        if (!OffseasonSession.ready()) return
        GameSession.setStayingOnMainDuringOffseason(false)
        _uiState.update { it.copy(navigateToTalentHub = true, showReturnToTalentHub = false) }
    }

    private fun navigateToOffseasonDestination() {
        if (OffseasonSession.ready() && OffseasonSession.phase == OffseasonSession.Phase.SCHEDULE) {
            _uiState.update { it.copy(navigateToSchedule = true) }
        } else {
            _uiState.update { it.copy(navigateToTalentHub = true) }
        }
    }

    fun consumeSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun consumeScrollSchedule() {
        _uiState.update { it.copy(scrollScheduleToIndex = null) }
    }

    fun selectTab(tab: MainTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun selectHomeSegment(segment: HomeSegment) {
        _uiState.update { it.copy(homeSegment = segment) }
        rebuildSnapshot()
    }

    fun selectBrowseSegment(segment: BrowseSegment) {
        _uiState.update { it.copy(browseSegment = segment) }
        rebuildSnapshot()
    }

    fun selectTeamSegment(segment: TeamPanelSegment) {
        _uiState.update { it.copy(teamSegment = segment) }
        rebuildSnapshot()
    }

    fun selectAwardsSegment(segment: AwardsSegment) {
        val l = league ?: return
        if (segment == AwardsSegment.BOWLS && l.currentWeek < League.WEEK_CCG) return
        _uiState.update { it.copy(awardsSegment = segment) }
        rebuildSnapshot()
    }

    fun setBrowseRosterFilter(filter: String) {
        _uiState.update { it.copy(browseRosterPosFilter = filter) }
        rebuildSnapshot()
    }

    fun selectConfIndex(index: Int) {
        val l = league ?: return
        if (index < 0 || index >= l.conferences.size) return
        if (wantUpdateConf < 2) {
            wantUpdateConf++
            return
        }
        currentConference = l.conferences[index]
        browseConfSyncPending = true
        currentTeam = currentConference!!.confTeams[0]
        _uiState.update {
            it.copy(
                selectedConfIndex = index,
                selectedBrowseTeamIndex = 0,
            )
        }
        rebuildSnapshot()
    }

    fun selectBrowseTeamIndex(index: Int) {
        val conf = currentConference ?: return
        if (index < 0 || index >= conf.confTeams.size) return
        if (browseConfSyncPending) {
            browseConfSyncPending = false
            return
        }
        if (wantUpdateConf < 2) {
            wantUpdateConf++
            return
        }
        currentTeam = conf.confTeams[index]
        _uiState.update { it.copy(selectedBrowseTeamIndex = index) }
        rebuildSnapshot()
    }

    fun selectLineupPosition(index: Int) {
        if (index < 0 || index >= lineupPositions.size) return
        initLineupOrder(index)
        _uiState.update { it.copy(lineupPositionIndex = index) }
        rebuildSnapshot()
    }

    fun moveLineupPlayer(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        if (fromIndex !in lineupOrderKeys.indices || toIndex !in lineupOrderKeys.indices) return
        val key = lineupOrderKeys.removeAt(fromIndex)
        lineupOrderKeys.add(toIndex, key)
        applyLineupOrder(persistMessage = false)
    }

    fun toggleLineupLock(playerKey: Int) {
        val player = playerByKey[playerKey] ?: return
        player.depthLocked = !player.depthLocked
        rebuildSnapshot()
    }

    fun setLineupSectionLocks(starters: Boolean, locked: Boolean) {
        val user = userTeam ?: return
        val posIndex = _uiState.value.lineupPositionIndex
        if (posIndex >= stSlotBase) {
            postSnackbar("Special teams slots have no lock groups")
            return
        }
        user.setDepthLocks(posIndex, starters, locked)
        val section = if (starters) "starters" else "bench"
        postSnackbar(if (locked) "Locked $section" else "Unlocked $section")
        rebuildSnapshot()
    }

    fun autoSortUnlockedLineup() {
        val user = userTeam ?: return
        val posIndex = _uiState.value.lineupPositionIndex
        if (posIndex >= stSlotBase) {
            user.ensureSpecialTeamsDepth()
            initLineupOrder(posIndex)
            postSnackbar("Reset ${lineupPositions[posIndex]} from best available")
            rebuildSnapshot()
            return
        }
        user.sortPositionDepth(posIndex)
        initLineupOrder(posIndex)
        postSnackbar("Sorted ${lineupPositions[posIndex]} by OVR")
        rebuildSnapshot()
    }

    fun autoSortAllLineups() {
        val user = userTeam ?: return
        user.sortPlayers()
        initLineupOrder(_uiState.value.lineupPositionIndex)
        postSnackbar("Sorted all positions by OVR")
        rebuildSnapshot()
    }

    private fun applyLineupOrder(persistMessage: Boolean) {
        val user = userTeam ?: return
        val posIndex = _uiState.value.lineupPositionIndex
        val ordered = java.util.ArrayList(lineupOrderKeys.mapNotNull { playerByKey[it] })
        if (ordered.isEmpty()) return
        if (posIndex >= stSlotBase) {
            // First player in the ST candidate list is the assigned slot holder.
            user.setSpecialTeamsSlot(posIndex - stSlotBase, ordered[0])
        } else {
            val group = lineupPosGroups.getOrNull(posIndex)
            val primaryOnly = if (group != null) {
                java.util.ArrayList(ordered.filter { it.position == group.token })
            } else {
                ordered
            }
            if (primaryOnly.isNotEmpty()) {
                user.setDepthChart(primaryOnly, posIndex)
            }
        }
        if (persistMessage) {
            postSnackbar("Updated depth chart for ${lineupPositions[posIndex]}")
        }
        rebuildSnapshot()
    }

    fun selectAwardCategory(index: Int) {
        _uiState.update { it.copy(selectedAwardCategory = index) }
        rebuildSnapshot()
    }

    fun selectBowlOption(index: Int) {
        _uiState.update { it.copy(selectedBowlOption = index) }
        rebuildSnapshot()
    }

    fun playWeek() {
        val l = league ?: return
        val user = userTeam ?: return
        if (_uiState.value.playingWeek) return
        if (l.currentWeek == League.WEEK_SEASON_END || recruitingStage >= 0) {
            beginRecruiting()
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(playingWeek = true, playWeekEnabled = false) }
            val numGamesPlayed = user.gameWLSchedule.size
            withContext(Dispatchers.Default) {
                l.playWeek()
            }
            var snack: String? = null
            var showInjury = false
            var showSummary = false
            var summaryTitle = ""
            var summaryMessage = ""
            var switchToAwards = false
            var scrollIndex: Int? = null

            val lastWl = user.gameWLSchedule.lastOrNull()
            val playedRealGame = user.gameWLSchedule.size > numGamesPlayed && lastWl != "BYE"
            if (l.currentWeek != League.WEEK_SEASON_END && showInjuryReport &&
                playedRealGame
            ) {
                showInjury = true
            }
            if (l.currentWeek == League.WEEK_SEASON_END) {
                l.checkLeagueRecords()
                summaryTitle = "${League.FIRST_SEASON_YEAR + user.teamHistory.size} Season Summary"
                summaryMessage = l.seasonSummaryStr()
                showSummary = true
            } else if (user.gameWLSchedule.size > numGamesPlayed && showToasts) {
                snack = user.weekSummaryStr()
            }
            if (l.currentWeek >= League.WEEK_CCG && user.gameSchedule.isNotEmpty()) {
                val lastGame = user.gameSchedule[user.gameSchedule.size - 1]
                if (lastGame != null && !lastGame.hasPlayed) {
                    if (showToasts) {
                        snack = if (lastGame.gameName == "NCG") {
                            "Congratulations! ${user.name} was invited to the National Championship Game!"
                        } else {
                            "Congratulations! ${user.name} was invited to the ${lastGame.gameName}!"
                        }
                    }
                } else when (l.currentWeek) {
                    League.WEEK_CCG -> if (showToasts) snack = "${user.name} was not invited to the Conference Championship."
                    League.WEEK_CFP_FIRST_ROUND -> if (showToasts) {
                        snack = "${user.name} was not invited to the playoff or a bowl game."
                    }
                }
            }
            if (l.currentWeek == League.WEEK_CFP_FIRST_ROUND) {
                switchToAwards = true
            }
            if (l.currentWeek >= League.WEEK_SEASON_END) {
                l.curePlayers()
                recruitingStage = 0
            }
            if (_uiState.value.homeSegment == HomeSegment.GAMES && l.currentWeek > 2) {
                scrollIndex = (user.numGames() - 3).coerceAtLeast(0)
            }
            if (GameSession.hasActiveSaveSlot()) {
                val saved = autosaveIfPossible()
                if (!saved && snack == null) {
                    snack = "Week played — save failed"
                }
            }
            rebuildSnapshot(
                snackbar = snack,
                showInjuryDialog = showInjury,
                showSeasonSummary = showSummary,
                seasonSummaryTitle = summaryTitle,
                seasonSummaryMessage = summaryMessage,
                switchToAwardsHonors = switchToAwards,
                scrollScheduleToIndex = scrollIndex,
            )
            _uiState.update { it.copy(playingWeek = false, playWeekEnabled = true) }
        }
    }

    private fun beginRecruiting() {
        val l = league ?: return
        val user = userTeam ?: return
        l.getPlayersLeaving()
        recruitingStage = 0
        val grad = user.getGradPlayersList().toList()
        val mock = l.getMockDraftPlayersList().toList()
        _uiState.update {
            it.copy(
                showPlayersLeavingDialog = true,
                playersLeavingDialog = PlayersLeavingDialogUi(
                    title = "${user.abbr} Players Leaving",
                    tab = PlayersLeavingTab.GRADUATES,
                    gradLines = grad,
                    mockDraftLines = mock,
                ),
            )
        }
    }

    fun setPlayersLeavingTab(tab: PlayersLeavingTab) {
        val dialog = _uiState.value.playersLeavingDialog ?: return
        _uiState.update { it.copy(playersLeavingDialog = dialog.copy(tab = tab)) }
    }

    fun dismissPlayersLeavingDialog() {
        _uiState.update { it.copy(showPlayersLeavingDialog = false, playersLeavingDialog = null) }
    }

    fun confirmBeginRetention() {
        val l = league ?: return
        dismissPlayersLeavingDialog()
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                val off = LeagueOffseason(l)
                l.offseason = off
                off.grantAllBudgets()
                off.resolveCoachingChanges()
                off.aiRetainAll()
                off.scorePortalRiskForTeam(l.userTeam)
                GameSession.beginOffseason(l, off)
                autosaveActiveSlot()
            }
            _uiState.update { it.copy(navigateToTalentHub = true) }
        }
    }

    fun pickTeam(abbr: String) {
        viewModelScope.launch {
            val empty = withContext(Dispatchers.IO) { repo.findFirstEmptySlot() }
            if (empty == null && !GameSession.hasActiveSaveSlot()) {
                val slots = withContext(Dispatchers.IO) { repo.listSlots() }
                _uiState.update {
                    it.copy(
                        showChooseSlotForNewCareer = true,
                        pendingPickTeamAbbr = abbr,
                        saveSlotInfos = slots,
                    )
                }
                return@launch
            }
            applyPickTeam(abbr, empty)
        }
    }

    fun dismissChooseSlotForNewCareer() {
        _uiState.update {
            it.copy(showChooseSlotForNewCareer = false, pendingPickTeamAbbr = null)
        }
    }

    fun chooseSlotForNewCareer(index: Int) {
        val abbr = _uiState.value.pendingPickTeamAbbr ?: return
        _uiState.update {
            it.copy(showChooseSlotForNewCareer = false, pendingPickTeamAbbr = null)
        }
        viewModelScope.launch { applyPickTeam(abbr, index) }
    }

    private suspend fun applyPickTeam(abbr: String, bindSlot: Int?) {
        val l = league ?: return
        val picked = l.teamList.firstOrNull { it.abbr == abbr } ?: return
        userTeam?.userControlled = false
        userTeam = picked
        l.userTeam = userTeam
        userTeam!!.userControlled = true
        UserBrandTheme.setFrom(userTeam)
        currentTeam = userTeam
        currentConference = l.findConference(userTeam!!.conference)
        l.setTeamRanks()
        wantUpdateConf = 0
        GameSession.setNeedsTeamPicker(false)
        // Year 1: rebuild conf + byes and let the user schedule OOC before kickoff.
        l.prepareSeasonSchedule()
        val off = LeagueOffseason(l)
        l.offseason = off
        GameSession.beginOffseason(l, off, OffseasonSession.Phase.SCHEDULE)
        GameSession.setNeedsOocScheduling(true)

        val slot = GameSession.getActiveSaveSlot() ?: bindSlot
        if (slot != null) {
            val ok = withContext(Dispatchers.IO) { repo.save(slot, l).isSuccess }
            if (ok) {
                GameSession.setActiveSaveSlot(slot)
            } else {
                postSnackbar("Could not create save slot — progress may be lost if the app closes")
            }
        }

        // Keep showTeamPicker true until navigation starts so MainScreen does not
        // briefly paint the roster under the outgoing picker.
        _uiState.update { it.copy(navigateToSchedule = true) }
        rebuildSnapshot()
        wantUpdateConf = 2
    }

    fun openGameDialog(gameKey: Int) {
        val game = gameByKey[gameKey] ?: return
        val user = userTeam ?: return
        if (game.hasPlayed) {
            val summary = game.getGameSummaryStr()
            val rivalry = game.rivalryStrength()
            val rivalryLabel = if (rivalry > 0) {
                "${Rivalry.band(rivalry)} rivalry ($rivalry)"
            } else {
                null
            }
            fun boxTeam(team: Team, score: Int, home: Boolean) = GameBoxTeamUi(
                name = team.name,
                abbr = team.abbr,
                rank = team.rankTeamPollScore,
                record = "${team.wins}-${team.losses}",
                score = score,
                passYards = game.getPassYards(home),
                rushYards = game.getRushYards(home),
                turnovers = if (home) game.homeTOs else game.awayTOs,
                offPhilosophy = team.offPhilosophy.displayName,
                defSystem = team.defSystem.displayName,
            )
            val schemeLine = Regex("""^\S+\s+(Offense|Defense):\s+.+$""")
            val logLines = (game.gameEventLog ?: summary[3])
                .lineSequence()
                .map { it.trimEnd() }
                .filter { it.isNotBlank() }
                .filterNot { schemeLine.matches(it.trim()) }
                .toList()
            val awayWon = when {
                game.awayScore > game.homeScore -> true
                game.homeScore > game.awayScore -> false
                else -> null
            }
            _uiState.update {
                it.copy(
                    showGameDialog = true,
                    gameDialog = GameDialogUi(
                        title = "${game.awayTeam.abbr} @ ${game.homeTeam.abbr}: ${game.gameName}",
                        played = true,
                        awayScore = game.awayScore.toString(),
                        homeScore = game.homeScore.toString(),
                        awayName = game.awayTeam.getStrAbbrWL_2Lines(),
                        homeName = game.homeTeam.getStrAbbrWL_2Lines(),
                        otLabel = if (game.numOT > 0) "${game.numOT}OT" else "@",
                        left = summary[0],
                        center = summary[1],
                        right = summary[2],
                        bottom = summary[3] + "\n\n",
                        canCoach = false,
                        gameKey = gameKey,
                        gameName = game.gameName,
                        rivalryLabel = rivalryLabel,
                        awayBox = boxTeam(game.awayTeam, game.awayScore, home = false),
                        homeBox = boxTeam(game.homeTeam, game.homeScore, home = true),
                        awayWon = awayWon,
                        gameLogLines = logLines,
                    ),
                )
            }
        } else {
            val scout = game.getGameScoutStr()
            val involvesUser = game.awayTeam == user || game.homeTeam == user
            val rivalry = game.rivalryStrength()
            val rivalryLabel = if (rivalry > 0) {
                "${Rivalry.band(rivalry)} rivalry ($rivalry)"
            } else {
                null
            }
            fun scoutTeam(team: Team) = GameScoutTeamUi(
                name = team.name,
                abbr = team.abbr,
                rank = team.rankTeamPollScore,
                offPhilosophy = team.offPhilosophy.displayName,
                defSystem = team.defSystem.displayName,
                offTalent = team.getOffTalent(),
                defTalent = team.getDefTalent(),
                programPower = team.programProfile.programPower,
            )
            _uiState.update {
                it.copy(
                    showGameDialog = true,
                    gameDialog = GameDialogUi(
                        title = "${game.awayTeam.abbr} @ ${game.homeTeam.abbr}: ${game.gameName}",
                        played = false,
                        awayScore = null,
                        homeScore = null,
                        awayName = null,
                        homeName = null,
                        otLabel = null,
                        left = scout[0],
                        center = scout[1],
                        right = scout[2],
                        bottom = scout[3],
                        canCoach = involvesUser,
                        gameKey = gameKey,
                        gameName = game.gameName,
                        rivalryLabel = rivalryLabel,
                        awayScout = scoutTeam(game.awayTeam),
                        homeScout = scoutTeam(game.homeTeam),
                    ),
                )
            }
        }
    }

    fun startCoachGame(gameKey: Int) {
        val game = gameByKey[gameKey] ?: return
        if (game.hasPlayed) return
        GameSession.setActiveCoachGame(game)
        _uiState.update {
            it.copy(showGameDialog = false, gameDialog = null, navigateToCoach = true)
        }
    }

    fun consumeNavigateToCoach() {
        _uiState.update { it.copy(navigateToCoach = false) }
    }

    fun dismissGameDialog() {
        _uiState.update { it.copy(showGameDialog = false, gameDialog = null) }
    }

    fun setOffPhilosophy(index: Int) {
        val user = userTeam ?: return
        val values = OffensivePhilosophy.values()
        if (index !in values.indices) return
        user.setOffPhilosophy(values[index])
        rebuildSnapshot()
        postSnackbar("Depth chart updated for ${values[index].displayName}")
    }

    fun setDefSystem(index: Int) {
        val user = userTeam ?: return
        val values = DefensiveSystem.values()
        if (index !in values.indices) return
        user.setDefSystem(values[index])
        rebuildSnapshot()
        postSnackbar("Depth chart updated for ${values[index].displayName}")
    }

    fun setPressureResponse(slotIndex: Int, responseIndex: Int) {
        val user = userTeam ?: return
        val slots = QbPressurePolicy.Slot.values()
        val responses = PressureResponse.values()
        if (slotIndex !in slots.indices || responseIndex !in responses.indices) return
        val slot = slots[slotIndex]
        val response = responses[responseIndex]
        user.setPressureResponse(slot, response)
        rebuildSnapshot()
        postSnackbar("${slot.displayName}: ${response.displayName}")
    }

    fun openPlayerCareer(playerKey: Int) {
        val player = playerByKey[playerKey] ?: return
        val user = userTeam ?: return
        val displayTeam = player.team ?: user
        val seasonChips = parseStatChips(player.getDetailStatsList(displayTeam.numGames()))
        val careerChips = parseStatChips(
            player.getCareerStatsList(),
            skipLabels = setOf("Schools", "Status", "Awards"),
        )
        val attrChips = buildAttrChips(player)
        val secondaryPos = buildSecondaryPosOvrs(player)
        _uiState.update {
            it.copy(
                showPlayerCareer = true,
                playerCareer = PlayerCareerUi(
                    name = player.name,
                    position = player.position,
                    ovr = player.ratOvr,
                    yearLabel = player.getYrStr(),
                    potGrade = letterGrade(player.ratPot),
                    teamName = displayTeam.name,
                    teamAbbr = displayTeam.abbr,
                    rosterStatus = player.rosterStatus.displayName(),
                    nilLabel = if (player.nilDealAmount > 0) {
                        NilMoney.format(player.nilDealAmount)
                    } else {
                        null
                    },
                    attrChips = attrChips,
                    secondaryPosOvrs = secondaryPos,
                    seasonRatings = seasonChips.ratings,
                    seasonStats = seasonChips.stats,
                    careerTotals = careerChips.stats,
                    seasonYears = buildSeasonYears(player),
                    timeline = buildPlayerTimeline(player, displayTeam),
                ),
            )
        }
    }

    fun dismissPlayerCareer() {
        _uiState.update { it.copy(showPlayerCareer = false, playerCareer = null) }
    }

    fun examineTeam(teamName: String) {
        val l = league ?: return
        var found = userTeam
        for (t in l.teamList) {
            if (t.name == teamName) {
                found = t
                break
            }
        }
        currentTeam = found
        currentConference = l.findConference(currentTeam!!.conference)
        wantUpdateConf = 0
        syncBrowseIndicesToCurrentTeam()
        _uiState.update {
            it.copy(
                selectedTab = MainTab.LEAGUE,
                browseSegment = BrowseSegment.ROSTER,
            )
        }
        rebuildSnapshot()
        wantUpdateConf = 2
    }

    fun examineTeamFromRankingLine(line: String) {
        teamFromRankingLine(line)?.let { examineTeam(it.name) }
    }

    fun examineTeamFromRankingRow(row: RankingRowUi) {
        row.teamName?.let { examineTeam(it) }
    }

    fun requestExit() {
        _uiState.update { it.copy(showExitConfirm = true) }
    }

    fun dismissExitConfirm() {
        _uiState.update { it.copy(showExitConfirm = false) }
    }

    fun confirmExit() {
        viewModelScope.launch {
            repo.setLastActiveSlot(null)
            GameSession.clearAll()
            UserBrandTheme.clear()
            _uiState.update { it.copy(showExitConfirm = false, navigateHome = true) }
        }
    }

    fun openSaveDialog() {
        viewModelScope.launch {
            val slots = repo.listSlots()
            _uiState.update {
                it.copy(
                    showSaveDialog = true,
                    saveSlotInfos = slots,
                )
            }
        }
    }

    fun dismissSaveDialog() {
        _uiState.update {
            it.copy(showSaveDialog = false, confirmOverwriteSlot = null, confirmDeleteSlot = null)
        }
    }

    fun saveToSlot(index: Int) {
        val infos = _uiState.value.saveSlotInfos
        if (index < 0 || index >= infos.size) return
        val info = infos[index]
        if (info.status == SlotStatus.EMPTY) {
            performSave(index)
        } else {
            _uiState.update { it.copy(confirmOverwriteSlot = index) }
        }
    }

    fun dismissOverwriteConfirm() {
        _uiState.update { it.copy(confirmOverwriteSlot = null) }
    }

    fun confirmOverwriteSave() {
        val index = _uiState.value.confirmOverwriteSlot ?: return
        performSave(index)
        _uiState.update { it.copy(confirmOverwriteSlot = null, showSaveDialog = false) }
    }

    fun requestDeleteSlot(index: Int) {
        _uiState.update { it.copy(confirmDeleteSlot = index) }
    }

    fun dismissDeleteSlot() {
        _uiState.update { it.copy(confirmDeleteSlot = null) }
    }

    fun confirmDeleteSlot() {
        val index = _uiState.value.confirmDeleteSlot ?: return
        viewModelScope.launch {
            repo.delete(index)
            if (GameSession.getActiveSaveSlot() == index) {
                GameSession.setActiveSaveSlot(null)
            }
            val slots = repo.listSlots()
            _uiState.update {
                it.copy(confirmDeleteSlot = null, saveSlotInfos = slots)
            }
            postSnackbar("Slot ${index + 1} deleted")
        }
    }

    fun exportActiveOrSlot(index: Int, onJson: (String) -> Unit) {
        viewModelScope.launch {
            try {
                onJson(repo.exportJson(index))
            } catch (e: Exception) {
                postSnackbar(e.message ?: "Export failed")
            }
        }
    }

    private fun performSave(index: Int) {
        val l = league ?: return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { repo.save(index, l) }
            if (result.isSuccess) {
                GameSession.setActiveSaveSlot(index)
                postSnackbar("Saved league!")
                _uiState.update { it.copy(showSaveDialog = false) }
            } else {
                postSnackbar(result.exceptionOrNull()?.message ?: "Save failed")
            }
        }
    }

    /** Writes the live league to the active slot when one is known. */
    private fun autosaveIfPossible(): Boolean {
        val l = league ?: return false
        if (!GameSession.hasActiveSaveSlot()) return false
        return CareerPersistence.saveActiveBlocking(l, repo)
    }

    private fun autosaveActiveSlot() {
        val l = league ?: return
        if (!GameSession.hasActiveSaveSlot()) return
        CareerPersistence.saveActiveBlocking(l, repo)
    }

    fun openRankingsDialog(modeIndex: Int = 0) {
        val mode = modeIndex.coerceIn(rankingsModes.indices)
        _uiState.update {
            it.copy(
                showRankingsDialog = true,
                rankingsModeIndex = mode,
                rankingsRows = buildRankingRows(mode),
            )
        }
    }

    fun setRankingsMode(index: Int) {
        _uiState.update {
            it.copy(rankingsModeIndex = index, rankingsRows = buildRankingRows(index))
        }
    }

    fun dismissRankingsDialog() {
        _uiState.update { it.copy(showRankingsDialog = false) }
    }

    fun openLeagueHistoryDialog() {
        _uiState.update {
            it.copy(
                showLeagueHistoryDialog = true,
                leagueHistoryModeIndex = 0,
                leagueHistoryRows = buildLeagueHistoryRows(0),
            )
        }
    }

    fun setLeagueHistoryMode(index: Int) {
        _uiState.update {
            it.copy(
                leagueHistoryModeIndex = index,
                leagueHistoryRows = buildLeagueHistoryRows(index),
            )
        }
    }

    fun dismissLeagueHistoryDialog() {
        _uiState.update { it.copy(showLeagueHistoryDialog = false) }
    }

    fun openTeamHistoryDialog() {
        _uiState.update {
            it.copy(
                showTeamHistoryDialog = true,
                teamHistoryModeIndex = 0,
                teamHistoryRows = buildTeamHistoryRows(0),
            )
        }
    }

    fun setTeamHistoryMode(index: Int) {
        _uiState.update {
            it.copy(
                teamHistoryModeIndex = index,
                teamHistoryRows = buildTeamHistoryRows(index),
            )
        }
    }

    fun dismissTeamHistoryDialog() {
        _uiState.update { it.copy(showTeamHistoryDialog = false) }
    }

    fun openRenameDialog() {
        val user = userTeam ?: return
        _uiState.update {
            it.copy(
                showRenameDialog = true,
                renameDialog = RenameDialogUi(
                    name = user.name,
                    abbr = user.abbr,
                    nameError = "",
                    abbrError = "",
                    showPopups = showToasts,
                    showInjuryReport = showInjuryReport,
                ),
            )
        }
    }

    fun updateRenameName(name: String) {
        val l = league ?: return
        val dialog = _uiState.value.renameDialog ?: return
        val trimmed = name.trim()
        val err = if (!l.isNameValid(trimmed)) {
            "Name already in use or has illegal characters!"
        } else ""
        _uiState.update { it.copy(renameDialog = dialog.copy(name = name, nameError = err)) }
    }

    fun updateRenameAbbr(abbr: String) {
        val l = league ?: return
        val dialog = _uiState.value.renameDialog ?: return
        val trimmed = abbr.trim().uppercase()
        val err = if (!l.isAbbrValid(trimmed)) {
            "Abbreviation already in use or has illegal characters!"
        } else ""
        _uiState.update { it.copy(renameDialog = dialog.copy(abbr = abbr, abbrError = err)) }
    }

    fun updateRenameShowPopups(checked: Boolean) {
        val dialog = _uiState.value.renameDialog ?: return
        _uiState.update { it.copy(renameDialog = dialog.copy(showPopups = checked)) }
    }

    fun updateRenameShowInjury(checked: Boolean) {
        val dialog = _uiState.value.renameDialog ?: return
        _uiState.update { it.copy(renameDialog = dialog.copy(showInjuryReport = checked)) }
    }

    fun cancelRename() {
        val dialog = _uiState.value.renameDialog ?: return
        showToasts = dialog.showPopups
        showInjuryReport = dialog.showInjuryReport
        userTeam?.showPopups = showToasts
        _uiState.update { it.copy(showRenameDialog = false, renameDialog = null) }
    }

    fun confirmRename() {
        val l = league ?: return
        val user = userTeam ?: return
        val dialog = _uiState.value.renameDialog ?: return
        val newName = dialog.name.trim()
        val newAbbr = dialog.abbr.trim().uppercase()
        showToasts = dialog.showPopups
        showInjuryReport = dialog.showInjuryReport
        user.showPopups = showToasts
        if (l.isNameValid(newName) && l.isAbbrValid(newAbbr)) {
            val oldAbbr = user.abbr
            l.changeAbbrHistoryRecords(oldAbbr, newAbbr)
            user.name = newName
            user.abbr = newAbbr
            for (t in l.teamList) {
                if (t !== user) {
                    t.retargetRivalAbbr(oldAbbr, newAbbr)
                }
            }
            l.oocContracts?.retargetAbbr(oldAbbr, newAbbr)
            syncBrowseIndicesToCurrentTeam()
            rebuildSnapshot()
        } else if (showToasts) {
            postSnackbar("Invalid name/abbr! Name not changed.")
        }
        _uiState.update { it.copy(showRenameDialog = false, renameDialog = null) }
    }

    fun dismissSeasonSummary() {
        _uiState.update { it.copy(showSeasonSummary = false) }
    }

    fun dismissInjuryDialog() {
        _uiState.update { it.copy(showInjuryDialog = false) }
    }

    fun goToLineupFromInjury() {
        dismissInjuryDialog()
        _uiState.update {
            it.copy(
                selectedTab = MainTab.TEAM,
                teamSegment = TeamPanelSegment.DEPTH_CHART,
            )
        }
        rebuildSnapshot()
    }

    fun setInjuryReportEnabled(enabled: Boolean) {
        showInjuryReport = enabled
        _uiState.update { state ->
            state.copy(injuryReport = state.injuryReport?.copy(showReportsEnabled = enabled))
        }
    }

    fun dismissRecruitingClassDialog() {
        _uiState.update { it.copy(showRecruitingClassDialog = false) }
    }

    private fun showRecruitingClassDialogInternal() {
        _uiState.update {
            it.copy(
                showRecruitingClassDialog = true,
                recruitingClassRows = buildRankingRows(15),
            )
        }
    }

    private fun postSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    private fun playerKey(player: Player): Int {
        val key = System.identityHashCode(player)
        playerByKey[key] = player
        return key
    }

    private fun gameKey(game: Game): Int {
        val key = System.identityHashCode(game)
        gameByKey[key] = game
        return key
    }

    private fun teamFromRankingLine(line: String): Team? {
        val l = league ?: return null
        val parts = line.split(",")
        if (parts.size < 2) return null
        val tokens = parts[1].trim().split("\\s+".toRegex())
        if (tokens.size < 2) return null
        return l.findTeamAbbr(tokens[1])
    }

    private fun buildRankingRows(mode: Int): List<RankingRowUi> {
        val l = league ?: return emptyList()
        val user = userTeam ?: return emptyList()
        return l.getTeamRankingsStr(mode).map { line ->
            val parts = line.split(",")
            val rankRaw = parts.getOrNull(0)?.trim().orEmpty()
            val mid = parts.getOrNull(1)?.trim().orEmpty()
            val statRaw = parts.getOrNull(2)?.trim().orEmpty()
            if (rankRaw.isEmpty() && mid.contains("Conference", ignoreCase = true)) {
                RankingRowUi(
                    line = line,
                    teamName = null,
                    isSectionHeader = true,
                    sectionTitle = mid,
                )
            } else {
                val team = teamFromRankingLine(line)
                val rankNum = rankRaw.filter { it.isDigit() }.toIntOrNull() ?: 0
                RankingRowUi(
                    line = line,
                    teamName = team?.name,
                    abbr = team?.abbr,
                    rankLabel = rankRaw,
                    rankNum = rankNum,
                    pollRank = team?.rankTeamPollScore,
                    record = team?.let { "${it.wins}-${it.losses}" },
                    statValue = statRaw,
                    isUserTeam = team != null && team === user,
                )
            }
        }
    }

    private fun buildLeagueHistoryRows(mode: Int): List<HistoryRowUi> {
        val l = league ?: return emptyList()
        val user = userTeam ?: return emptyList()
        return if (mode == 1) {
            parseRecordsCsv(l.getLeagueRecordsStr(), user.abbr)
        } else {
            parseLeagueHistoryYears(l.getLeagueHistoryStr(), user.abbr)
        }
    }

    private fun buildTeamHistoryRows(mode: Int): List<HistoryRowUi> {
        val user = userTeam ?: return emptyList()
        val l = league ?: return emptyList()
        return when (mode) {
            0 -> parseTeamHistoryList(user)
            1 -> parseRecordsCsv(l.userTeamRecords.getRecordsStr(), user.abbr)
            else -> user.hallOfFame.map { line ->
                val parts = line.split("&")
                HistoryRowUi(
                    kind = HistoryRowKind.HOF,
                    title = parts.firstOrNull().orEmpty(),
                    text = parts.drop(1).joinToString(" · "),
                    teamName = user.name,
                    abbr = user.abbr,
                    isUserRelated = true,
                )
            }
        }
    }

    private fun parseLeagueHistoryYears(raw: String, userAbbr: String): List<HistoryRowUi> {
        return raw.split("%")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { block ->
                val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
                val year = lines.firstOrNull()?.removeSuffix(":")?.trim().orEmpty()
                val champLine = lines.firstOrNull { it.startsWith("Champions:") }
                    ?.removePrefix("Champions:")?.trim().orEmpty()
                val potyLine = lines.firstOrNull { it.startsWith("POTY:") }
                    ?.removePrefix("POTY:")?.trim().orEmpty()
                val champAbbr = champLine.substringBefore(" ").trim().takeIf { it.isNotBlank() }
                val champTeam = teamByAbbr(champAbbr)
                HistoryRowUi(
                    kind = HistoryRowKind.YEAR,
                    title = year,
                    value = champLine,
                    holder = potyLine,
                    abbr = champAbbr,
                    teamName = champTeam?.name,
                    isUserRelated = champAbbr == userAbbr || potyLine.contains(userAbbr),
                    highlightAbbr = userAbbr,
                )
            }
    }

    private fun parseTeamHistoryList(user: Team): List<HistoryRowUi> {
        val rows = mutableListOf<HistoryRowUi>()
        user.getTeamHistoryList().forEach { line ->
            when {
                line.isBlank() -> Unit
                line.startsWith("Overall W-L:") ||
                    line.startsWith("Conf Champ Record:") ||
                    line.startsWith("Bowl Game Record:") ||
                    line.startsWith("National Champ Record:") -> {
                    val parts = line.split(":", limit = 2)
                    rows.add(
                        HistoryRowUi(
                            kind = HistoryRowKind.SUMMARY_STAT,
                            title = parts.getOrNull(0)?.trim(),
                            value = parts.getOrNull(1)?.trim().orEmpty(),
                            teamName = user.name,
                            abbr = user.abbr,
                            isUserRelated = true,
                        ),
                    )
                }
                else -> {
                    val yearPart = line.substringBefore(":").trim()
                    val rest = line.substringAfter(":", "").trim()
                    val main = rest.substringBefore(">").trim()
                    val bowl = rest.substringAfter(">", "").trim().takeIf { it.isNotEmpty() }
                    rows.add(
                        HistoryRowUi(
                            kind = HistoryRowKind.YEAR,
                            title = yearPart,
                            value = main,
                            holder = bowl,
                            teamName = user.name,
                            abbr = user.abbr,
                            isUserRelated = true,
                        ),
                    )
                }
            }
        }
        return rows
    }

    private fun parseRecordsCsv(raw: String, userAbbr: String): List<HistoryRowUi> {
        val sectionKeys = setOf("TEAM", "SEASON", "CAREER")
        return raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line ->
                val parts = line.split(",")
                if (parts.isEmpty()) return@mapNotNull null
                val key = parts[0].trim()
                if (key in sectionKeys || (parts.size >= 2 && parts[1].trim() == "-1")) {
                    HistoryRowUi(
                        kind = HistoryRowKind.SECTION,
                        title = key,
                    )
                } else if (parts.size >= 4) {
                    val holder = parts[2].trim()
                    val holderAbbr = holder.substringBefore(" ").trim().takeIf { it.isNotBlank() && it != "XXX" }
                    val team = teamByAbbr(holderAbbr)
                    HistoryRowUi(
                        kind = HistoryRowKind.RECORD,
                        title = key,
                        value = parts[1].trim(),
                        holder = holder,
                        yearLabel = parts[3].trim(),
                        abbr = holderAbbr,
                        teamName = team?.name,
                        isUserRelated = holderAbbr == userAbbr || holder.contains(userAbbr),
                        highlightAbbr = userAbbr,
                    )
                } else {
                    HistoryRowUi(kind = HistoryRowKind.TEXT, text = line)
                }
            }
            .toList()
    }

    private fun teamByAbbr(abbr: String?): Team? {
        if (abbr.isNullOrBlank() || abbr == "XXX") return null
        val l = league ?: return null
        return l.teamList.firstOrNull { it.abbr.equals(abbr, ignoreCase = true) }
    }

    private fun syncBrowseIndicesToCurrentTeam() {
        val l = league ?: return
        val team = currentTeam ?: return
        currentConference = l.findConference(team.conference)
        var confIdx = 0
        for (i in l.conferences.indices) {
            if (l.conferences[i].confName == team.conference) {
                confIdx = i
                break
            }
        }
        var teamIdx = 0
        val conf = currentConference
        if (conf != null) {
            for (i in conf.confTeams.indices) {
                val rep = conf.confTeams[i].strRep()
                val tokens = rep.split(" ")
                if (tokens.size > 1 && tokens[1] == team.abbr) {
                    teamIdx = i
                    break
                }
            }
        }
        _uiState.update {
            it.copy(selectedConfIndex = confIdx, selectedBrowseTeamIndex = teamIdx)
        }
    }

    private fun initLineupOrder(positionIndex: Int) {
        val user = userTeam ?: return
        lineupOrderKeys.clear()
        val players = playersForLineupPosition(user, positionIndex)
        for (p in players) {
            lineupOrderKeys.add(playerKey(p))
        }
    }

    private fun playersForLineupPosition(user: Team, position: Int): List<Player> {
        if (position >= stSlotBase) {
            val slot = position - stSlotBase
            val pool: MutableList<Player> = when (slot) {
                0, 1 -> ArrayList(user.specialTeamsReturnPool())
                2, 3 -> ArrayList(user.specialTeamsGunnerPool())
                4 -> ArrayList(user.specialTeamsSnapPool())
                else -> mutableListOf()
            }
            val assigned = user.getSpecialTeamsSlot(slot)
            if (assigned != null) {
                pool.remove(assigned)
                pool.add(0, assigned)
            }
            return pool
        }
        val group = lineupPosGroups.getOrNull(position) ?: return emptyList()
        val primary = user.playersForGroup(group)?.toList().orEmpty()
        val minOvr = 40
        val byOvr = user.allPlayers
            .sortedByDescending { PositionOvr.ovr(it, group) }
            .filter { PositionOvr.ovr(it, group) >= minOvr || it.position == group.token }
        val ordered = LinkedHashSet<Player>()
        primary.forEach { ordered.add(it) }
        byOvr.forEach { ordered.add(it) }
        return ordered.toList()
    }

    private fun rebuildSnapshot(
        ready: Boolean? = null,
        snackbar: String? = null,
        showInjuryDialog: Boolean = false,
        showSeasonSummary: Boolean = false,
        seasonSummaryTitle: String = "",
        seasonSummaryMessage: String = "",
        switchToAwardsHonors: Boolean = false,
        scrollScheduleToIndex: Int? = null,
    ) {
        val l = league
        val user = userTeam
        val browse = currentTeam
        if (l == null || user == null || browse == null) return

        val prev = _uiState.value
        val tab = if (switchToAwardsHonors) MainTab.AWARDS else prev.selectedTab
        val awardsSeg = if (switchToAwardsHonors) AwardsSegment.HONORS else prev.awardsSegment

        val playLabel = when {
            l.currentWeek < League.WEEK_CCG -> "Play Week"
            l.currentWeek == League.WEEK_CCG -> "Play Conf Championships"
            l.currentWeek == League.WEEK_CFP_FIRST_ROUND -> "Play CFP First Round & Bowls"
            l.currentWeek == League.WEEK_CFP_QUARTERS -> "Play CFP Quarters"
            l.currentWeek == League.WEEK_CFP_SEMIS -> "Play CFP Semis"
            l.currentWeek == League.WEEK_NCG -> "Play National Championship"
            else -> "Begin Recruiting"
        }

        val confNames = l.conferences.map { it.confName }
        val conf = currentConference ?: l.findConference(browse.conference)
        val browseTeams = conf.confTeams.map {
            BrowseTeamOptionUi(
                label = it.strRep(),
                name = it.name,
                abbr = it.abbr,
            )
        }

        val posIdx = prev.lineupPositionIndex.coerceIn(lineupPositions.indices)
        // Team depth lists are source of truth (injuries / auto-sort may reorder).
        initLineupOrder(posIdx)
        val required = lineupRequired[posIdx]
        val lineupPlayers = playersForLineupPosition(user, posIdx)
        val lensGroup = lineupPosGroups.getOrNull(posIdx)
        val lineupRows = lineupPlayers.mapIndexed { index, p ->
            val starter = index < required && (lensGroup == null || p.position == lensGroup.token)
            val injured = p.isInjured && p.injury != null
            val posOvr = if (lensGroup != null) PositionOvr.ovr(p, lensGroup) else p.ratOvr
            LineupRowUi(
                name = p.name,
                ovr = p.ratOvr,
                posOvr = posOvr,
                primaryPos = p.position ?: "",
                yearLabel = p.getYrStr(),
                potGrade = letterGrade(
                    if (lensGroup != null) PositionOvr.pot(p, lensGroup) else p.ratPot,
                ),
                injured = injured,
                injuryLabel = if (injured) p.injury.toString() else null,
                playerKey = playerKey(p),
                depthIndex = index,
                depthLabel = if (starter) "ST${index + 1}" else "BK${index - required + 1}",
                starter = starter,
                locked = p.depthLocked,
            )
        }

        val injuryReport = if (showInjuryDialog) buildInjuryReport(user) else null

        val showTalentHub = OffseasonSession.ready() &&
            OffseasonSession.phase != OffseasonSession.Phase.SCHEDULE

        _uiState.update { state ->
            state.copy(
                ready = ready ?: state.ready,
                selectedTab = tab,
                awardsSegment = awardsSeg,
                playWeekLabel = playLabel,
                showReturnToTalentHub = showTalentHub,
                homeStats = buildTeamStats(user),
                homeRoster = buildRoster(user),
                homeSchedule = buildSchedule(user),
                browseStats = buildTeamStats(browse),
                browseRoster = buildRoster(browse, prev.browseRosterPosFilter),
                browseSchedule = buildSchedule(browse),
                confNames = confNames,
                browseTeamOptions = browseTeams,
                lineupPositionLabel = lineupPositions[posIdx],
                lineupRequired = required,
                lineupRows = lineupRows,
                lineupStarterCount = lineupRows.count { it.starter },
                lineupBenchCount = lineupRows.count { !it.starter },
                offPhilosophyNames = OffensivePhilosophy.values().map { it.displayName },
                defSystemNames = DefensiveSystem.values().map { it.displayName },
                offPhilosophyIndex = user.offPhilosophy?.ordinal
                    ?: OffensivePhilosophy.MULTIPLE.ordinal,
                defSystemIndex = user.defSystem?.ordinal
                    ?: DefensiveSystem.BASE_4_3.ordinal,
                pressureResponseNames = PressureResponse.values().map { it.displayName },
                pressureSlotLabels = QbPressurePolicy.Slot.values().map { it.displayName },
                pressureResponseIndices = QbPressurePolicy.Slot.values().map { slot ->
                    val policy = user.qbPressurePolicy ?: QbPressurePolicy.defaults()
                    policy.forSlot(slot).ordinal
                },
                awardsBowlsUnlocked = l.currentWeek >= League.WEEK_CCG,
                snackbarMessage = snackbar ?: state.snackbarMessage,
                showInjuryDialog = showInjuryDialog || state.showInjuryDialog,
                injuryReport = injuryReport ?: state.injuryReport,
                showSeasonSummary = showSeasonSummary || state.showSeasonSummary,
                seasonSummaryTitle = seasonSummaryTitle.ifEmpty { state.seasonSummaryTitle },
                seasonSummaryMessage = seasonSummaryMessage.ifEmpty { state.seasonSummaryMessage },
                scrollScheduleToIndex = scrollScheduleToIndex ?: state.scrollScheduleToIndex,
            ).let { s -> buildAwardsSnapshot(l, user, s) }
        }
    }

    private fun buildAwardsSnapshot(l: League, user: Team, state: MainUiState): MainUiState {
        if (l.currentWeek < League.WEEK_CFP_FIRST_ROUND) {
            val rows = if (l.heismanHistory.isNullOrEmpty()) {
                listOf(
                    AwardRowUi(
                        isMessage = true,
                        title = "Season awards unlock after conference championships.",
                    ),
                )
            } else {
                l.heismanHistory.indices.reversed().map { histIndex ->
                    val year = League.FIRST_SEASON_YEAR + histIndex
                    val hist = l.heismanHistory[histIndex]
                    val abbr = Regex("""\b([A-Z]{2,4})\s+\(""").find(hist)?.groupValues?.getOrNull(1)
                    val team = teamByAbbr(abbr)
                    AwardRowUi(
                        title = "$year Player of the Year",
                        subtitle = hist,
                        abbr = abbr,
                        teamName = team?.name,
                        highlightUser = hist.contains(user.abbr),
                    )
                }
            }
            return state.copy(
                awardCategories = emptyList(),
                awardsSectionLabel = "Historic Awards",
                potyHeader = null,
                potySubhead = null,
                potyStats = null,
                potyTeamName = null,
                potyAbbr = null,
                awardRows = rows,
                bowlSpinnerOptions = bowlOptions(l),
                bowlRows = buildBowlRows(l, state),
            )
        }
        val categories = buildList {
            add("Player of the Year")
            add("All Americans")
            for (c in l.conferences) add("All-${c.confName}")
        }
        val cat = state.selectedAwardCategory.coerceIn(categories.indices)
        var potyHeader: String? = null
        var potySub: String? = null
        var potyStats: String? = null
        var potyTeamName: String? = null
        var potyAbbr: String? = null
        var section = categories[cat]
        val rows: List<AwardRowUi> = when (cat) {
            0 -> {
                val candidates = l.getHeisman()
                val winner = candidates.firstOrNull()
                if (winner != null) {
                    potyHeader = "${winner.position} ${winner.name}"
                    potySub = "${winner.team.name} • ${winner.getYrStr()} • Player of the Year"
                    potyStats = l.heismanWinnerStatsLine(winner)
                    potyTeamName = winner.team.name
                    potyAbbr = winner.team.abbr
                } else {
                    potyHeader = "Player of the Year"
                }
                section = "Voting results"
                candidates.take(5).mapIndexed { index, p ->
                    awardRowFromPlayer(
                        player = p,
                        rankNum = index + 1,
                        metaLine = "${p.getHeismanScore()} votes · ${p.team.wins}-${p.team.losses}",
                        statsLine = l.heismanWinnerStatsLine(p).replace(" · ", ", "),
                        userAbbr = user.abbr,
                    )
                }
            }
            1 -> awardRowsFromAllAmericanStr(l.getAllAmericanStr(), user.abbr)
            else -> {
                val confIdx = cat - 2
                val conf = l.conferences.getOrNull(confIdx)
                if (conf != null) {
                    conf.getAllConfPlayers().map { p ->
                        awardRowFromPlayer(
                            player = p,
                            metaLine = "${p.team.wins}-${p.team.losses} · ${p.getYrStr()}",
                            statsLine = l.heismanWinnerStatsLine(p).replace(" · ", ", "),
                            userAbbr = user.abbr,
                        )
                    }
                } else {
                    emptyList()
                }
            }
        }
        return state.copy(
            awardCategories = categories,
            selectedAwardCategory = cat,
            awardsSectionLabel = section,
            potyHeader = potyHeader,
            potySubhead = potySub,
            potyStats = potyStats,
            potyTeamName = potyTeamName,
            potyAbbr = potyAbbr,
            awardRows = rows,
            bowlSpinnerOptions = bowlOptions(l),
            bowlRows = buildBowlRows(l, state),
        )
    }

    private fun awardRowFromPlayer(
        player: Player,
        userAbbr: String,
        rankNum: Int = 0,
        metaLine: String?,
        statsLine: String?,
    ): AwardRowUi {
        return AwardRowUi(
            highlightUser = player.team.abbr == userAbbr,
            rankLabel = if (rankNum > 0) rankNum.toString() else null,
            rankNum = rankNum,
            teamName = player.team.name,
            abbr = player.team.abbr,
            position = player.position,
            playerName = player.name,
            yearLabel = player.getYrStr(),
            metaLine = metaLine,
            statsLine = statsLine,
        )
    }

    private fun awardRowsFromAllAmericanStr(raw: String, userAbbr: String): List<AwardRowUi> {
        return raw.split(">")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { block ->
                val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
                val header = lines.firstOrNull().orEmpty()
                // "ALA(12-1) -  QB Name [Jr]"
                val abbr = header.substringBefore("(").trim().takeIf { it.isNotBlank() }
                val afterDash = header.substringAfter("-", "").trim()
                val tokens = afterDash.split("\\s+".toRegex()).filter { it.isNotEmpty() }
                val position = tokens.getOrNull(0)
                val yearTok = tokens.lastOrNull()?.takeIf { it.startsWith("[") && it.endsWith("]") }
                val name = tokens
                    .drop(1)
                    .dropLast(if (yearTok != null) 1 else 0)
                    .joinToString(" ")
                val team = teamByAbbr(abbr)
                AwardRowUi(
                    highlightUser = abbr == userAbbr,
                    teamName = team?.name,
                    abbr = abbr,
                    position = position,
                    playerName = name.ifBlank { afterDash },
                    yearLabel = yearTok?.removeSurrounding("[", "]"),
                    metaLine = header.substringAfter("(").substringBefore(")").takeIf { it.contains("-") }
                        ?.let { "($it)" },
                    statsLine = lines.drop(1).joinToString(" · ") { it.replace("\t", "").trim() },
                )
            }
    }

    private fun bowlOptions(l: League): List<String> {
        if (l.currentWeek < League.WEEK_CCG) return emptyList()
        val options = mutableListOf<String>()
        options.add("Conf Championships")
        if (l.currentWeek >= League.WEEK_CFP_FIRST_ROUND || l.hasScheduledBowls) {
            options.add("CFP & Bowls")
        }
        return options
    }

    private fun buildBowlRows(l: League, state: MainUiState): List<BowlRowUi> {
        if (l.currentWeek < League.WEEK_CCG) return emptyList()
        val options = bowlOptions(l)
        if (options.isEmpty()) return emptyList()
        val selected = state.selectedBowlOption.coerceIn(options.indices)
        val label = options[selected]
        val showBowls = label == "CFP & Bowls"
        val user = userTeam
        val rows = mutableListOf<BowlRowUi>()
        if (!showBowls) {
            for (c in l.conferences) {
                val s = c.getCCGStr() ?: continue
                if (s.isEmpty()) continue
                val lines = s.split("\n")
                val detail = if (lines.size > 1) lines[1].trim() else s
                rows.add(parseBowlDetail(c.confName + " CCG", detail, user))
            }
        } else if (l.currentWeek >= League.WEEK_CFP_FIRST_ROUND || l.hasScheduledBowls) {
            fun addGame(g: Game?) {
                if (g == null) return
                rows.add(bowlRowFromGame(g, user))
            }
            l.cfpFirstRound?.forEach { addGame(it) }
            l.cfpQuarters?.forEach { addGame(it) }
            l.cfpSemis?.forEach { addGame(it) }
            addGame(l.ncg)
            l.bowlGames?.forEach { addGame(it) }
        }
        return rows
    }

    private fun bowlRowFromGame(g: Game, user: Team?): BowlRowUi {
        val away = g.awayTeam
        val home = g.homeTeam
        return BowlRowUi(
            name = g.gameName ?: "Bowl",
            awayAbbr = away?.abbr ?: "?",
            homeAbbr = home?.abbr ?: "?",
            score = if (g.hasPlayed) "${g.awayScore} - ${g.homeScore}" else "vs",
            awayName = away?.name,
            homeName = home?.name,
            awayRank = away?.rankTeamPollScore,
            homeRank = home?.rankTeamPollScore,
            awayRecord = away?.let { "${it.wins}-${it.losses}" },
            homeRecord = home?.let { "${it.wins}-${it.losses}" },
            played = g.hasPlayed,
            isUserInvolved = user != null && (away == user || home == user),
        )
    }

    private fun parseBowlDetail(name: String, detail: String, user: Team?): BowlRowUi {
        val cleaned = detail.replace("\t", " ").trim()
        val teamPattern = Regex("""#(\d+)\s+(\S+)\s+\(([^)]+)\)""")
        val teams = teamPattern.findAll(cleaned).toList()
        val left = teams.getOrNull(0)
        val right = teams.getOrNull(1)
        val scoreMatch = Regex("""\bW\s+(\d+-\d+)\b""").find(cleaned)
        val leftAbbr = left?.groupValues?.getOrNull(2) ?: "?"
        val rightAbbr = right?.groupValues?.getOrNull(2) ?: "?"
        val leftTeam = teamByAbbr(leftAbbr)
        val rightTeam = teamByAbbr(rightAbbr)
        val played = scoreMatch != null
        return BowlRowUi(
            name = name,
            awayAbbr = leftAbbr,
            homeAbbr = rightAbbr,
            score = scoreMatch?.groupValues?.getOrNull(1) ?: if (cleaned.contains(" vs ")) "vs" else "vs",
            awayName = leftTeam?.name,
            homeName = rightTeam?.name,
            awayRank = left?.groupValues?.getOrNull(1)?.toIntOrNull(),
            homeRank = right?.groupValues?.getOrNull(1)?.toIntOrNull(),
            awayRecord = left?.groupValues?.getOrNull(3),
            homeRecord = right?.groupValues?.getOrNull(3),
            played = played,
            isUserInvolved = user != null && (leftAbbr == user.abbr || rightAbbr == user.abbr),
        )
    }

    private fun buildTeamStats(team: Team): List<StatRowUi> {
        // CSV format from Team.getTeamStatsStrCSV: value,label,rank
        return team.getTeamStatsStrCSV()
            .split("%\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line ->
                val parts = line.split(",")
                if (parts.size < 3) return@mapNotNull null
                val value = parts[0].trim()
                val label = parts[1].trim()
                val rank = parts[2].trim()
                StatRowUi(
                    label = label,
                    value = value,
                    rank = rank,
                    rankNum = rank.filter { ch -> ch.isDigit() }.toIntOrNull() ?: Int.MAX_VALUE,
                    category = categoryForTeamStat(label),
                    rankingsMode = rankingsModeForTeamStat(label),
                )
            }
    }

    private fun categoryForTeamStat(label: String): String = when (label) {
        "Conf W-L", "AP Votes", "SOS" -> "Standing"
        "Points", "Yards", "Pass Yards", "Rush Yards" -> "Offense"
        "Opp Points", "Opp Yards", "Opp Pass YPG", "Opp Rush YPG", "TO Diff" -> "Defense"
        else -> "Program"
    }

    private fun rankingsModeForTeamStat(label: String): Int? = when (label) {
        "AP Votes" -> 0
        "Conf W-L" -> 1
        "SOS" -> 2
        "Points" -> 3
        "Opp Points" -> 4
        "Yards" -> 5
        "Opp Yards" -> 6
        "Pass Yards" -> 7
        "Rush Yards" -> 8
        "Opp Pass YPG" -> 9
        "Opp Rush YPG" -> 10
        "TO Diff" -> 11
        "Off Talent" -> 12
        "Def Talent" -> 13
        "Program Power" -> 14
        "Recruit Class" -> 15
        else -> null
    }

    private fun buildInjuryReport(team: Team): InjuryReportUi {
        fun rows(players: List<Player>?): List<InjuryReportPlayerUi> =
            players.orEmpty().map { p ->
                InjuryReportPlayerUi(
                    name = p.name,
                    pos = p.position,
                    yearLabel = p.getYrStr(),
                    ovr = p.ratOvr,
                    potGrade = letterGrade(p.ratPot),
                    injuryLabel = p.injury?.toString(),
                )
            }
        return InjuryReportUi(
            injured = rows(team.playersInjured),
            recovered = rows(team.playersRecovered),
            showReportsEnabled = showInjuryReport,
        )
    }

    private fun buildRoster(team: Team, filter: String = "ALL"): List<RosterRowUi> {
        return team.getRosterDisplayPlayers()
            .filter { filter == "ALL" || filter == it.player.position }
            .map { row ->
                val p = row.player
                val injured = p.isInjured && p.injury != null
                RosterRowUi(
                    name = p.name,
                    pos = p.position,
                    ovr = p.ratOvr,
                    yearLabel = p.getYrStr(),
                    potGrade = letterGrade(p.ratPot),
                    injured = injured,
                    injuryLabel = if (injured) p.injury.toString() else null,
                    starter = row.starter,
                    playerKey = playerKey(p),
                )
            }
    }

    private fun buildSchedule(team: Team): List<ScheduleRowUi> {
        return team.gameSchedule.mapIndexed { index, game ->
            if (team.isByeWeek(index)) {
                val summary = team.getGameSummaryStr(index)
                val played = index < team.gameWLSchedule.size && team.gameWLSchedule[index] == "BYE"
                return@mapIndexed ScheduleRowUi(
                    weekLabel = "Week ${index + 1}",
                    result = summary[1],
                    opponent = "BYE",
                    isWin = null,
                    isLoss = null,
                    gameKey = -1 - index,
                    opponentTeamName = null,
                    gameName = "BYE",
                    homeAway = "",
                    opponentLabel = "BYE",
                    opponentAbbr = "BYE",
                    opponentRank = 0,
                    scoreLine = summary[1],
                    played = played,
                    isHome = true,
                    isBye = true,
                )
            }
            if (game == null) {
                // Open OOC week before the Schedule phase is finished.
                return@mapIndexed ScheduleRowUi(
                    weekLabel = "Week ${index + 1}",
                    result = "---",
                    opponent = "TBD",
                    isWin = null,
                    isLoss = null,
                    gameKey = -1 - index,
                    opponentTeamName = null,
                    gameName = "OOC",
                    homeAway = "",
                    opponentLabel = "TBD",
                    opponentAbbr = "TBD",
                    opponentRank = 0,
                    scoreLine = "---",
                    played = false,
                    isHome = true,
                )
            }
            gameKey(game)
            val summary = team.getGameSummaryStr(index)
            val wl = team.gameWLSchedule
            // Prefer hasPlayed so a just-coached game shows after Main rebuilds,
            // even if gameWLSchedule indexing is temporarily odd.
            val played = game.hasPlayed || (index < wl.size && wl[index] != "BYE")
            val isWin = when {
                index < wl.size && wl[index] == "W" -> true
                index < wl.size && wl[index] == "L" -> false
                played && game.homeTeam == team -> game.homeScore > game.awayScore
                played && game.awayTeam == team -> game.awayScore > game.homeScore
                else -> null
            }
            val isLoss = when {
                isWin == null -> null
                else -> !isWin
            }
            val isHome = game.homeTeam == team
            val oppTeam = if (isHome) game.awayTeam else game.homeTeam
            val homeAway = if (isHome) "vs" else "@"
            val opponentLabel = "${oppTeam.abbr} #${oppTeam.rankTeamPollScore}"
            ScheduleRowUi(
                weekLabel = summary[0],
                result = summary[1],
                opponent = summary[2],
                isWin = isWin,
                isLoss = isLoss,
                gameKey = gameKey(game),
                opponentTeamName = oppTeam.name,
                gameName = game.gameName,
                homeAway = homeAway,
                opponentLabel = opponentLabel,
                opponentAbbr = oppTeam.abbr,
                opponentRank = oppTeam.rankTeamPollScore,
                scoreLine = summary[1],
                played = played,
                isHome = isHome,
            )
        }
    }

    private data class ParsedStatChips(
        val ratings: List<StatChipUi>,
        val stats: List<StatChipUi>,
    )

    private fun buildAttrChips(player: Player): List<StatChipUi> {
        val bag = player.ratings ?: return emptyList()
        val out = mutableListOf<StatChipUi>()
        out.add(StatChipUi("DUR", bag.dur.toString()))
        out.add(StatChipUi("IQ", bag.footIq.toString()))
        out.add(StatChipUi("POT", bag.pot.toString()))
        for (k in PlayerRatings.KEYS) {
            out.add(StatChipUi(PlayerRatings.displayLabel(k), bag.get(k).toString()))
        }
        return out
    }

    private fun buildSecondaryPosOvrs(player: Player): List<String> {
        val primary = PositionOvr.primaryGroup(player)
        val ranked = PositionGroup.values()
            .filter { it != primary }
            .map { it to player.ovrFor(it) }
            .filter { it.second >= 45 }
            .sortedByDescending { it.second }
            .take(3)
        return ranked.map { "${it.first.token} ${it.second}" }
    }

    private fun parseStatChips(
        lines: List<String>?,
        skipLabels: Set<String> = emptySet(),
    ): ParsedStatChips {
        if (lines == null) return ParsedStatChips(emptyList(), emptyList())
        val ratings = mutableListOf<StatChipUi>()
        val stats = mutableListOf<StatChipUi>()
        for (raw in lines) {
            val cleaned = raw
                .replace("[B]", "")
                .replace("[I]", "Injury: ")
            for (part in cleaned.split(">")) {
                val trimmed = part.trim()
                if (trimmed.isEmpty()) continue
                val colon = trimmed.indexOf(':')
                if (colon <= 0 || colon >= trimmed.length - 1) continue
                val label = trimmed.substring(0, colon).trim()
                val value = trimmed.substring(colon + 1).trim()
                if (label.isEmpty() || value.isEmpty()) continue
                if (label in skipLabels) continue
                val chip = StatChipUi(label = label, value = value)
                if (LETTER_GRADE_VALUE.matches(value)) {
                    ratings.add(chip)
                } else {
                    stats.add(chip)
                }
            }
        }
        return ParsedStatChips(ratings = ratings, stats = stats)
    }

    private fun buildSeasonYears(player: Player): List<SeasonYearUi> {
        val years = mutableListOf<SeasonYearUi>()
        val leagueYear = player.team?.league?.year
        if (leagueYear != null && player.gamesPlayed > 0) {
            years.add(seasonYearUi(PlayerSeasonRecord(player, leagueYear), isCurrent = true))
        }
        val past = player.careerSeasons ?: emptyList()
        for (i in past.indices.reversed()) {
            years.add(seasonYearUi(past[i], isCurrent = false))
        }
        return years
    }

    private fun seasonYearUi(record: PlayerSeasonRecord, isCurrent: Boolean): SeasonYearUi {
        val losses = maxOf(0, record.gamesPlayed - record.wins)
        val awards = buildList {
            if (record.wonHeisman) add("POTY")
            if (record.wonAllAmerican) add("All-American")
            if (record.wonAllConference) add("All-Conference")
        }
        return SeasonYearUi(
            year = record.seasonYear,
            teamAbbr = record.teamAbbr,
            teamName = record.teamName,
            classLabel = record.classStr(),
            recordLine = "${record.gamesPlayed}G (${record.wins}-$losses)",
            stats = positionStatsFromRecord(record),
            awards = awards,
            isCurrent = isCurrent,
        )
    }

    private fun positionStatsFromRecord(record: PlayerSeasonRecord): List<StatChipUi> {
        val base = when (record.position) {
            "QB" -> mutableListOf(
                StatChipUi("Pass Yds", record.passYards.toString()),
                StatChipUi("TD", record.passTd.toString()),
                StatChipUi("INT", record.passInt.toString()),
                StatChipUi("Comp", "${record.passComp}/${record.passAtt}"),
                StatChipUi("Sacks", record.sacked.toString()),
            )
            "RB" -> mutableListOf(
                StatChipUi("Rush Yds", record.rushYards.toString()),
                StatChipUi("TD", record.rushTd.toString()),
                StatChipUi("Att", record.rushAtt.toString()),
                StatChipUi("Fum", record.rushFumbles.toString()),
            )
            "WR" -> mutableListOf(
                StatChipUi("Rec", record.receptions.toString()),
                StatChipUi("Yards", record.recYards.toString()),
                StatChipUi("TD", record.recTd.toString()),
                StatChipUi("Tgts", record.targets.toString()),
                StatChipUi("Drops", record.drops.toString()),
            )
            "K" -> mutableListOf(
                StatChipUi("FG", "${record.fgMade}/${record.fgAtt}"),
                StatChipUi("XP", "${record.xpMade}/${record.xpAtt}"),
            )
            "CB", "S", "EDGE", "DL", "LB" -> mutableListOf(
                StatChipUi("Tck", record.tackles.toString()),
                StatChipUi("TFL", record.tfl.toString()),
                StatChipUi("Sacks", record.sacksDef.toString()),
                StatChipUi("INT", record.defInt.toString()),
                StatChipUi("PD", record.passDef.toString()),
                StatChipUi("FF", record.forcedFumbles.toString()),
                StatChipUi("FR", record.fumbleRec.toString()),
            )
            else -> mutableListOf()
        }
        if (record.position == "K" && record.puntAtt > 0) {
            base += StatChipUi("Punt", "${record.puntAtt}/${record.puntYards}")
        }
        if (record.prAtt > 0) {
            base += StatChipUi("PR Yds", record.prYards.toString())
            if (record.prTd > 0) base += StatChipUi("PR TD", record.prTd.toString())
        }
        if (record.krAtt > 0) {
            base += StatChipUi("KR Yds", record.krYards.toString())
            if (record.krTd > 0) base += StatChipUi("KR TD", record.krTd.toString())
        }
        return base
    }

    private fun buildPlayerTimeline(player: Player, displayTeam: Team): List<TimelineEventUi> {
        val events = mutableListOf<TimelineEventUi>()
        val currentYear = displayTeam.league?.year ?: 0
        val yearLabel = if (currentYear > 0) currentYear.toString() else "Now"

        val dealTitle = when (player.rosterStatus) {
            RosterStatus.SCHOLARSHIP_PLUS_NIL -> "NIL deal active"
            RosterStatus.SCHOLARSHIP -> "Scholarship secured"
            RosterStatus.PWO -> "Walk-on roster spot"
            else -> "Roster status"
        }
        val dealDetail = buildString {
            append(player.rosterStatus.displayName())
            if (player.contractLength > 0) {
                append(" · ${player.contractLength}yr deal")
            }
            if (player.contractYearsRemaining > 0) {
                append(" · ${player.contractYearsRemaining}yr left")
            } else if (player.rosterStatus != RosterStatus.PWO) {
                append(" · renewal due")
            }
        }
        val dealAmount = when {
            player.nilDealAmount > 0 -> "${NilMoney.format(player.nilDealAmount)}/yr"
            else -> null
        }
        events.add(
            TimelineEventUi(
                yearLabel = yearLabel,
                title = dealTitle,
                detail = dealDetail,
                amountLabel = dealAmount,
                kind = TimelineKind.DEAL,
            ),
        )

        val prior = player.priorTeam
        val reason = player.transferReason
        if (prior != null || (reason != null && reason.isIssue())) {
            val from = prior?.abbr ?: "previous school"
            val reasonLabel = when {
                !player.transferReasonText.isNullOrBlank() -> player.transferReasonText
                reason != null && reason != TransferReason.NONE -> reason.label
                else -> "Entered transfer portal"
            }
            events.add(
                TimelineEventUi(
                    yearLabel = yearLabel,
                    title = "Transferred to ${displayTeam.abbr}",
                    detail = "From $from · $reasonLabel",
                    amountLabel = null,
                    kind = TimelineKind.TRANSFER,
                ),
            )
        }

        val seasons = player.careerSeasons ?: emptyList()
        var lastAbbr: String? = null
        for (record in seasons) {
            if (record.teamAbbr != lastAbbr) {
                events.add(
                    TimelineEventUi(
                        yearLabel = record.seasonYear.toString(),
                        title = "Joined ${record.teamAbbr}",
                        detail = record.teamName,
                        amountLabel = null,
                        kind = TimelineKind.SCHOOL,
                    ),
                )
                lastAbbr = record.teamAbbr
            }
            if (record.wonHeisman) {
                events.add(
                    TimelineEventUi(
                        yearLabel = record.seasonYear.toString(),
                        title = "Player of the Year",
                        detail = record.teamAbbr,
                        amountLabel = null,
                        kind = TimelineKind.AWARD,
                    ),
                )
            }
            if (record.wonAllAmerican) {
                events.add(
                    TimelineEventUi(
                        yearLabel = record.seasonYear.toString(),
                        title = "All-American",
                        detail = record.teamAbbr,
                        amountLabel = null,
                        kind = TimelineKind.AWARD,
                    ),
                )
            }
            if (record.wonAllConference) {
                events.add(
                    TimelineEventUi(
                        yearLabel = record.seasonYear.toString(),
                        title = "All-Conference",
                        detail = record.teamAbbr,
                        amountLabel = null,
                        kind = TimelineKind.AWARD,
                    ),
                )
            }
        }

        if (seasons.isEmpty() && prior == null) {
            events.add(
                TimelineEventUi(
                    yearLabel = yearLabel,
                    title = "Joined ${displayTeam.abbr}",
                    detail = displayTeam.name,
                    amountLabel = null,
                    kind = TimelineKind.SCHOOL,
                ),
            )
        }

        return events.sortedWith(
            compareByDescending<TimelineEventUi> { it.yearLabel.toIntOrNull() ?: Int.MAX_VALUE }
                .thenBy { kindOrder(it.kind) },
        )
    }

    private fun kindOrder(kind: TimelineKind): Int = when (kind) {
        TimelineKind.DEAL -> 0
        TimelineKind.TRANSFER -> 1
        TimelineKind.AWARD -> 2
        TimelineKind.SCHOOL -> 3
    }

    private fun letterGrade(num: Int): String {
        val ind = ((num - 50) / 5).coerceIn(0, LETTER_GRADES.lastIndex)
        return LETTER_GRADES[ind]
    }

    private fun buildTeamPickerConferences(l: League): List<TeamPickerConfUi> {
        return l.conferences.map { conf ->
            TeamPickerConfUi(
                name = conf.confName,
                teams = conf.confTeams.map { team ->
                    TeamPickerTeamUi(
                        name = team.name,
                        abbr = team.abbr,
                        programPower = team.programProfile.programPower,
                        tradition = team.programProfile.tradition,
                        fanbase = team.programProfile.fanbase,
                        donors = team.programProfile.donors,
                        footprint = team.programProfile.footprint,
                        pipeline = team.programProfile.pipeline,
                        momentum = team.programProfile.momentum,
                        purse = NilMoney.format(NilMoney.yearlyBudget(team.programProfile)),
                        offTalent = team.getOffTalent(),
                        defTalent = team.getDefTalent(),
                        stTalent = team.getSTTalent(),
                    )
                },
            )
        }.filter { it.teams.isNotEmpty() }
    }

    companion object {
        private val LETTER_GRADES = arrayOf("F", "F+", "D", "D+", "C", "C+", "B", "B+", "A", "A+")
        private val LETTER_GRADE_VALUE = Regex("^[A-F][+-]?$")
    }
}
