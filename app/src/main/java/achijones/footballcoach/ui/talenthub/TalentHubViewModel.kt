package achijones.footballcoach.ui.talenthub

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import CFBsimPack.GameSession
import CFBsimPack.League
import CFBsimPack.LeagueOffseason
import CFBsimPack.NilMoney
import CFBsimPack.OffseasonSession
import CFBsimPack.OocScheduleBuilder
import CFBsimPack.Player
import CFBsimPack.ProgramOffers
import CFBsimPack.Rivalry
import CFBsimPack.RivalryDynamics
import CFBsimPack.RosterStatus
import CFBsimPack.Team
import achijones.footballcoach.ui.util.SaveSlots
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

enum class HubTab { RETAIN, PORTAL, SCHEDULE, HS, MONEY }

data class ScheduleWeekUi(
    val week: Int,
    val weekLabel: String,
    val status: String,
    val detail: String,
    val locked: Boolean,
    val open: Boolean,
    val opponentAbbr: String?,
    val contractLocked: Boolean = false,
    val rivalryLabel: String? = null,
)

data class ContractRowUi(
    val id: String,
    val summary: String,
)

data class TalentRowUi(
    val id: String,
    val playerName: String?,
    val position: String,
    val ovr: Int,
    val primary: String,
    val secondary: String,
    val costLine: String,
    val statusLabel: String,
    val showCheck: Boolean,
    val checked: Boolean,
    val locked: Boolean,
    val moneyRow: Boolean,
    val sortOvr: Int,
    val sortCost: Int,
    val suggestionKey: String?,
)

data class OfferSheetState(
    val playerName: String,
    val position: String,
    val ovr: Int,
    val yearLine: String,
    val secondary: String,
    val draftStay: Boolean,
    val retention: Boolean,
    val portal: Boolean,
    val status: RosterStatus,
    val years: Int,
    val maxYears: Int,
    val costPreview: String,
    val confirmLabel: String,
    val showBuyout: Boolean,
    val buyoutLabel: String,
    val stayBonusLabel: String,
    val suggestionKey: String?,
)

data class TalentHubUiState(
    val ready: Boolean = false,
    val missingSession: Boolean = false,
    val teamName: String = "",
    val phaseLabel: String = "",
    val browsing: Boolean = false,
    val selectedTab: HubTab = HubTab.RETAIN,
    val cashLabel: String = "",
    val y1Label: String = "",
    val schollyLabel: String = "",
    val rosterLabel: String = "",
    val search: String = "",
    val positionFilter: String = "ALL",
    val sortMode: Int = 0,
    val affordableOnly: Boolean = false,
    val rows: List<TalentRowUi> = emptyList(),
    val scheduleWeeks: List<ScheduleWeekUi> = emptyList(),
    val openOocSlots: Int = 0,
    val filledOocSlots: Int = 0,
    val contractRows: List<ContractRowUi> = emptyList(),
    val rivalSummary: String = "",
    val opponentPickerWeek: Int? = null,
    val opponentOptions: List<String> = emptyList(),
    val opponentAbbrs: List<String> = emptyList(),
    val dealOpponentAbbr: String? = null,
    val dealQuote: String = "",
    val primaryLabel: String? = null,
    val message: String? = null,
    val offerSheet: OfferSheetState? = null,
    val showSaveDialog: Boolean = false,
    val saveSlotInfos: List<String> = emptyList(),
    val confirmOverwriteIndex: Int? = null,
    val showBuyoutConfirm: Boolean = false,
    val buyoutPlayerName: String? = null,
    val buyoutCostLabel: String? = null,
    val showLeaveConfirm: Boolean = false,
    val navigateToMain: Boolean = false,
    val navigateHome: Boolean = false,
)

class TalentHubViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(TalentHubUiState())
    val uiState: StateFlow<TalentHubUiState> = _uiState.asStateFlow()

    private var league: League? = null
    private var offseason: LeagueOffseason? = null
    private var user: Team? = null
    private var suggestions: List<LeagueOffseason.RetainSuggestion> = emptyList()
    private var allRows: List<TalentRowUi> = emptyList()
    private var playerByKey: Map<String, Player> = emptyMap()
    private var suggestionByKey: Map<String, LeagueOffseason.RetainSuggestion> = emptyMap()
    private var offerPlayer: Player? = null

    init {
        bootstrap()
    }

    private fun bootstrap() {
        if (!GameSession.readyOffseason()) {
            _uiState.update { it.copy(missingSession = true, ready = false) }
            return
        }
        league = OffseasonSession.league
        offseason = OffseasonSession.offseason
        user = league?.userTeam
        if (OffseasonSession.phase == null) {
            OffseasonSession.phase = OffseasonSession.Phase.RETENTION
        }
        val tab = tabForPhase(OffseasonSession.phase)
        _uiState.update {
            it.copy(ready = true, missingSession = false, selectedTab = tab)
        }
        reloadTab()
    }

    fun selectTab(tab: HubTab) {
        _uiState.update { it.copy(selectedTab = tab) }
        reloadTab()
    }

    fun setSearch(q: String) {
        _uiState.update { it.copy(search = q) }
        applyFilters()
    }

    fun setPositionFilter(pos: String) {
        _uiState.update { it.copy(positionFilter = pos) }
        applyFilters()
    }

    fun setSortMode(mode: Int) {
        _uiState.update { it.copy(sortMode = mode) }
        applyFilters()
    }

    fun setAffordableOnly(v: Boolean) {
        _uiState.update { it.copy(affordableOnly = v) }
        applyFilters()
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun consumeNavigateToMain() {
        _uiState.update { it.copy(navigateToMain = false) }
    }

    fun consumeNavigateHome() {
        _uiState.update { it.copy(navigateHome = false) }
    }

    fun requestLeave() {
        _uiState.update { it.copy(showLeaveConfirm = true) }
    }

    fun dismissLeave() {
        _uiState.update { it.copy(showLeaveConfirm = false) }
    }

    fun confirmLeave() {
        GameSession.clearAll()
        _uiState.update { it.copy(showLeaveConfirm = false, navigateHome = true) }
    }

    fun openSaveDialog() {
        _uiState.update {
            it.copy(
                showSaveDialog = true,
                saveSlotInfos = SaveSlots.infos(getApplication()),
                confirmOverwriteIndex = null,
            )
        }
    }

    fun dismissSaveDialog() {
        _uiState.update { it.copy(showSaveDialog = false, confirmOverwriteIndex = null) }
    }

    fun pickSaveSlot(index: Int) {
        val infos = _uiState.value.saveSlotInfos
        if (index < 0 || index >= infos.size) return
        if (infos[index] == "EMPTY") {
            writeSave(index)
        } else {
            _uiState.update { it.copy(confirmOverwriteIndex = index) }
        }
    }

    fun confirmOverwrite() {
        val index = _uiState.value.confirmOverwriteIndex ?: return
        writeSave(index)
    }

    fun dismissOverwrite() {
        _uiState.update { it.copy(confirmOverwriteIndex = null) }
    }

    private fun writeSave(index: Int) {
        val l = league ?: return
        val file = SaveSlots.file(getApplication(), index)
        val ok = l.saveLeague(file)
        _uiState.update {
            it.copy(
                showSaveDialog = false,
                confirmOverwriteIndex = null,
                message = if (ok) "Saved league!" else "Save failed.",
            )
        }
    }

    fun toggleSuggestion(rowId: String) {
        if (!canActOnTab(_uiState.value.selectedTab)) {
            _uiState.update {
                it.copy(message = "Browsing only — advance the current phase to act here.")
            }
            return
        }
        val row = allRows.find { it.id == rowId } ?: return
        val key = row.suggestionKey ?: return
        val suggestion = suggestionByKey[key] ?: return
        suggestion.selected = !suggestion.selected
        reloadTab()
    }

    fun onRowTap(rowId: String) {
        val row = _uiState.value.rows.find { it.id == rowId } ?: return
        if (row.moneyRow) return
        if (!canActOnTab(_uiState.value.selectedTab)) {
            _uiState.update {
                it.copy(message = "Browsing only — advance the current phase to act here.")
            }
            return
        }
        if (_uiState.value.selectedTab == HubTab.RETAIN) {
            if (row.locked) {
                _uiState.update { it.copy(message = "Declared for draft — cannot retain.") }
                return
            }
            if (row.suggestionKey == null) return
        }
        openOfferSheet(row)
    }

    private fun openOfferSheet(row: TalentRowUi) {
        val u = user ?: return
        val player = playerByKey[row.id] ?: return
        offerPlayer = player
        val retention = _uiState.value.selectedTab == HubTab.RETAIN
        val portal = _uiState.value.selectedTab == HubTab.PORTAL
        val suggestion = row.suggestionKey?.let { suggestionByKey[it] }
        val draftStay = retention && suggestion != null && suggestion.bucket == "DRAFT_STAY"
        val maxY = ProgramOffers.maxContractYears(player)
        var startStatus = RosterStatus.SCHOLARSHIP
        var startYears = 1
        if (suggestion != null) {
            startStatus = suggestion.status
            startYears = suggestion.years.coerceIn(1, maxY)
        } else if (portal) {
            startStatus = ProgramOffers.minimumAcceptable(player, maxOf(1, player.portalRiskTier))
            startYears = ProgramOffers.suggestedContractYears(player)
        } else {
            startStatus = when {
                player.ratOvr >= 82 -> RosterStatus.SCHOLARSHIP_PLUS_NIL
                player.ratOvr >= 60 -> RosterStatus.SCHOLARSHIP
                else -> RosterStatus.PWO
            }
            startYears = ProgramOffers.suggestedContractYears(player)
        }
        val stayBonus = if (draftStay) (suggestion?.stayBonus ?: 0) else 0
        _uiState.update {
            it.copy(
                offerSheet = OfferSheetState(
                    playerName = player.name,
                    position = player.position,
                    ovr = player.ratOvr,
                    yearLine = player.getYrStr(),
                    secondary = row.secondary,
                    draftStay = draftStay,
                    retention = retention,
                    portal = portal,
                    status = startStatus,
                    years = startYears,
                    maxYears = maxY,
                    costPreview = if (draftStay) {
                        "Stay bonus: ${NilMoney.format(stayBonus)}"
                    } else {
                        costPreviewText(player, u, startStatus, startYears)
                    },
                    confirmLabel = when {
                        draftStay -> "Pay to stay"
                        retention -> "Select offer"
                        else -> "Sign"
                    },
                    showBuyout = retention && !draftStay,
                    buyoutLabel = "Cut / Buy out (${NilMoney.format(u.buyoutCost(player))})",
                    stayBonusLabel = NilMoney.format(stayBonus),
                    suggestionKey = row.suggestionKey,
                ),
            )
        }
    }

    fun updateOfferStatus(status: RosterStatus) {
        val sheet = _uiState.value.offerSheet ?: return
        if (sheet.draftStay) return
        val player = offerPlayer ?: return
        val u = user ?: return
        val preview = costPreviewText(player, u, status, sheet.years)
        _uiState.update { it.copy(offerSheet = sheet.copy(status = status, costPreview = preview)) }
    }

    fun updateOfferYears(years: Int) {
        val sheet = _uiState.value.offerSheet ?: return
        if (sheet.draftStay) return
        val player = offerPlayer ?: return
        val u = user ?: return
        val y = years.coerceIn(1, sheet.maxYears)
        val preview = costPreviewText(player, u, sheet.status, y)
        _uiState.update { it.copy(offerSheet = sheet.copy(years = y, costPreview = preview)) }
    }

    fun dismissOfferSheet() {
        offerPlayer = null
        _uiState.update { it.copy(offerSheet = null) }
    }

    fun confirmOffer() {
        val sheet = _uiState.value.offerSheet ?: return
        val player = offerPlayer ?: return
        val u = user ?: return
        val off = offseason ?: return
        if (sheet.draftStay) {
            val suggestion = sheet.suggestionKey?.let { suggestionByKey[it] }
            if (suggestion != null) {
                suggestion.selected = true
                suggestion.stayBonus = ProgramOffers.draftStayBonus(player, u)
            }
            dismissOfferSheet()
            reloadTab()
            return
        }
        val st = sheet.status
        val y = sheet.years
        val nil = if (st == RosterStatus.SCHOLARSHIP_PLUS_NIL) {
            ProgramOffers.annualNilFor(player, u, y)
        } else {
            0
        }
        if (sheet.retention) {
            val suggestion = sheet.suggestionKey?.let { suggestionByKey[it] }
            if (suggestion != null) {
                suggestion.status = st
                suggestion.years = y
                suggestion.nil = nil
                suggestion.selected = true
            }
            dismissOfferSheet()
            reloadTab()
            return
        }
        val ok = if (sheet.portal) {
            off.userSignTransfer(u, player, st, nil, y)
        } else {
            off.userSignHs(u, player, st, nil, y)
        }
        dismissOfferSheet()
        _uiState.update {
            it.copy(
                message = if (ok) "Signed ${player.name}"
                else "Could not sign (budget, slots, future commitments, or offer).",
            )
        }
        reloadTab()
    }

    fun requestBuyout() {
        val player = offerPlayer ?: return
        val u = user ?: return
        val cost = u.buyoutCost(player)
        dismissOfferSheet()
        _uiState.update {
            it.copy(
                showBuyoutConfirm = true,
                buyoutPlayerName = player.name,
                buyoutCostLabel = NilMoney.format(cost),
            )
        }
        // keep player for confirm
        offerPlayer = player
    }

    fun dismissBuyout() {
        offerPlayer = null
        _uiState.update {
            it.copy(showBuyoutConfirm = false, buyoutPlayerName = null, buyoutCostLabel = null)
        }
    }

    fun confirmBuyout() {
        val player = offerPlayer ?: return
        val u = user ?: return
        val ok = u.cutOrBuyout(player, true)
        offerPlayer = null
        _uiState.update {
            it.copy(
                showBuyoutConfirm = false,
                buyoutPlayerName = null,
                buyoutCostLabel = null,
                message = if (ok) "Released ${player.name}" else "Cannot afford buyout.",
            )
        }
        reloadTab()
    }

    fun onPrimary() {
        when (OffseasonSession.phase) {
            OffseasonSession.Phase.RETENTION -> approveRetention()
            OffseasonSession.Phase.PORTAL -> {
                GameSession.setPendingOffseasonResult(GameSession.OffseasonResult.DONE_TRANSFER_PORTAL)
                _uiState.update { it.copy(navigateToMain = true) }
            }
            OffseasonSession.Phase.SCHEDULE -> {
                val u = user ?: return
                val openLeft = (0 until League.REGULAR_SEASON_WEEKS).count { u.isOpenOocWeek(it) }
                if (openLeft > 0) {
                    _uiState.update {
                        it.copy(message = "Schedule all $openLeft remaining OOC weeks before continuing.")
                    }
                    return
                }
                GameSession.setPendingOffseasonResult(GameSession.OffseasonResult.DONE_SCHEDULE)
                _uiState.update { it.copy(navigateToMain = true) }
            }
            OffseasonSession.Phase.HS -> {
                val budget = user?.recruitMoney ?: 0
                GameSession.setPendingRemainingBudget(budget)
                GameSession.setPendingOffseasonResult(GameSession.OffseasonResult.DONE_RECRUITING)
                _uiState.update { it.copy(navigateToMain = true) }
            }
        }
    }

    fun openOpponentPicker(week: Int) {
        val u = user ?: return
        val l = league ?: return
        val existing = u.gameSchedule.getOrNull(week)
        if (existing?.contractId != null) {
            _uiState.update {
                it.copy(message = "That week is locked by an OOC contract. Cancel the deal to change it.")
            }
            return
        }
        if (!u.isOpenOocWeek(week) && existing?.let {
                it.gameName == "OOC" || it.gameName == "OOC Rivalry"
                    || it.gameName == "Rivalry Game OOC"
            } != true) {
            return
        }
        if (!u.isOpenOocWeek(week)) {
            OocScheduleBuilder.clearUserOocGame(u, week)
        }
        val eligible = OocScheduleBuilder.eligibleOpponents(u, week, l.teamList)
        _uiState.update {
            it.copy(
                opponentPickerWeek = week,
                opponentOptions = eligible.map { t -> formatOpponentOption(u, t) },
                opponentAbbrs = eligible.map { t -> t.abbr },
                dealOpponentAbbr = null,
                dealQuote = "",
            )
        }
    }

    private fun formatOpponentOption(user: Team, t: Team): String {
        val strength = Team.strongestRivalryBetween(user, t)
        val rivalTag = if (strength > 0) {
            " · ${Rivalry.band(strength)} rival ($strength)"
        } else {
            ""
        }
        val book = league?.oocContracts
        val quote = if (book != null && user.teamPrestige >= t.teamPrestige) {
            " · Buy: you pay ${NilMoney.format(NilMoney.buyGameGuarantee(user.teamPrestige, t.teamPrestige))}"
        } else if (book != null) {
            " · Buy: you get ${NilMoney.format(NilMoney.buyGameGuarantee(t.teamPrestige, user.teamPrestige))}"
        } else {
            ""
        }
        return "${t.name} (${t.abbr}) — ${t.conference}$rivalTag$quote"
    }

    fun dismissOpponentPicker() {
        _uiState.update {
            it.copy(
                opponentPickerWeek = null,
                opponentOptions = emptyList(),
                opponentAbbrs = emptyList(),
                dealOpponentAbbr = null,
                dealQuote = "",
            )
        }
    }

    fun pickOpponent(index: Int) {
        val u = user ?: return
        val l = league ?: return
        val week = _uiState.value.opponentPickerWeek ?: return
        val abbrs = _uiState.value.opponentAbbrs
        if (index < 0 || index >= abbrs.size) return
        val opponent = l.findTeamAbbr(abbrs[index]) ?: return
        if (!OocScheduleBuilder.placeUserOocGame(u, opponent, week)) {
            _uiState.update { it.copy(message = "Could not schedule that opponent in week ${week + 1}.") }
            return
        }
        dismissOpponentPicker()
        reloadTab()
    }

    fun selectDealOpponent(index: Int) {
        val u = user ?: return
        val l = league ?: return
        val abbrs = _uiState.value.opponentAbbrs
        if (index < 0 || index >= abbrs.size) return
        val opponent = l.findTeamAbbr(abbrs[index]) ?: return
        val book = l.oocContracts ?: return
        val quote = if (u.teamPrestige >= opponent.teamPrestige) {
            book.quoteBuyGame(u, opponent) + " · Or sign a 2-year home-and-home (no guarantee)."
        } else {
            book.quoteReceiveBuyGame(opponent, u) + " · Or sign a 2-year home-and-home (no guarantee)."
        }
        _uiState.update {
            it.copy(dealOpponentAbbr = opponent.abbr, dealQuote = quote)
        }
    }

    fun signBuyGameYears(years: Int) {
        val u = user ?: return
        val l = league ?: return
        val book = l.oocContracts ?: return
        val week = _uiState.value.opponentPickerWeek ?: return
        val oppAbbr = _uiState.value.dealOpponentAbbr ?: return
        val opponent = l.findTeamAbbr(oppAbbr) ?: return
        val home: Team
        val away: Team
        if (u.teamPrestige >= opponent.teamPrestige) {
            home = u
            away = opponent
        } else {
            home = opponent
            away = u
        }
        val contract = book.signBuyGame(home, away, l.year, years) ?: run {
            _uiState.update {
                it.copy(message = "Could not sign buy game (affordability or conflict).")
            }
            return
        }
        if (!OocScheduleBuilder.placeFixedHomeOocGame(home, away, week, contract.id)) {
            book.materializeCurrentYear()
        }
        dismissOpponentPicker()
        reloadTab()
    }

    fun signHomeAndHomeDeal() {
        val u = user ?: return
        val l = league ?: return
        val book = l.oocContracts ?: return
        val week = _uiState.value.opponentPickerWeek ?: return
        val oppAbbr = _uiState.value.dealOpponentAbbr ?: return
        val opponent = l.findTeamAbbr(oppAbbr) ?: return
        val contract = book.signHomeAndHome(u, opponent, l.year, true) ?: run {
            _uiState.update { it.copy(message = "Could not sign home-and-home.") }
            return
        }
        val cg = contract.gameForYear(l.year)
        if (cg == null
            || !OocScheduleBuilder.placeFixedHomeOocGame(
                l.findTeamAbbr(cg.homeAbbr) ?: u,
                l.findTeamAbbr(cg.awayAbbr) ?: opponent,
                week,
                contract.id,
            )
        ) {
            book.materializeCurrentYear()
        }
        dismissOpponentPicker()
        reloadTab()
    }

    fun declareRival(opponentAbbr: String) {
        val u = user ?: return
        val l = league ?: return
        val opponent = l.findTeamAbbr(opponentAbbr) ?: return
        val error = RivalryDynamics.declareRival(u, opponent)
        if (error != null) {
            _uiState.update { it.copy(message = error) }
            return
        }
        _uiState.update {
            it.copy(message = "Declared ${opponent.abbr} as a rival (${u.rivalryWith(opponent.abbr)?.displayLabel()}).")
        }
        reloadTab()
    }

    fun cancelContract(contractId: String) {
        val l = league ?: return
        val book = l.oocContracts ?: return
        val u = user ?: return
        val contract = book.findById(contractId) ?: return
        val year = l.year
        val cg = contract.gameForYear(year)
        if (cg != null && !cg.settled) {
            for (week in 0 until League.REGULAR_SEASON_WEEKS) {
                val g = u.gameSchedule.getOrNull(week) ?: continue
                if (g.contractId == contractId) {
                    OocScheduleBuilder.clearUserOocGame(u, week)
                }
            }
        }
        if (!book.cancel(contractId)) {
            _uiState.update { it.copy(message = "Cannot cancel a deal that already paid out.") }
            return
        }
        reloadTab()
    }

    fun clearScheduleWeek(week: Int) {
        val u = user ?: return
        val game = u.gameSchedule.getOrNull(week)
        if (game?.contractId != null) {
            _uiState.update {
                it.copy(message = "Cancel the OOC contract to clear this week.")
            }
            return
        }
        if (OocScheduleBuilder.clearUserOocGame(u, week)) {
            reloadTab()
        }
    }

    fun resuggestOocSchedule() {
        val u = user ?: return
        val l = league ?: return
        OocScheduleBuilder.clearAllUserOocGames(u)
        OocScheduleBuilder.suggestUserOocSchedule(u, l.teamList)
        reloadTab()
    }

    /** Pre-fills OOC when the Schedule phase opens with an empty slate. */
    private fun maybeAutoSuggestOoc() {
        val u = user ?: return
        val l = league ?: return
        if (OffseasonSession.phase != OffseasonSession.Phase.SCHEDULE) return
        var open = 0
        var filled = 0
        for (week in 0 until League.REGULAR_SEASON_WEEKS) {
            if (u.isOpenOocWeek(week)) {
                open++
                continue
            }
            val game = u.gameSchedule.getOrNull(week) ?: continue
            if (game.gameName == "OOC" || game.gameName == "OOC Rivalry"
                || game.gameName == "Rivalry Game OOC"
            ) {
                filled++
            }
        }
        if (open > 0 && filled == 0) {
            OocScheduleBuilder.suggestUserOocSchedule(u, l.teamList)
        }
    }

    private fun approveRetention() {
        viewModelScope.launch {
            val off = offseason ?: return@launch
            val u = user ?: return@launch
            val applied = withContext(Dispatchers.Default) {
                var count = 0
                for (s in suggestions) {
                    if (!s.selected) continue
                    if (off.applyUserRetain(u, s)) count++
                }
                off.finalizeDraftDeclares(u)
                count
            }
            OffseasonSession.phase = OffseasonSession.Phase.PORTAL
            GameSession.setPendingOffseasonResult(GameSession.OffseasonResult.DONE_RETENTION)
            _uiState.update {
                it.copy(
                    message = "Retention applied ($applied). Opening portal…",
                    navigateToMain = true,
                )
            }
        }
    }

    private fun reloadTab() {
        val u = user ?: return
        val off = offseason ?: return
        val tab = _uiState.value.selectedTab
        val phase = OffseasonSession.phase
        if (tab == HubTab.SCHEDULE) {
            maybeAutoSuggestOoc()
        }
        val canAct = canActOnTab(tab)
        val phaseName = OffseasonSession.phaseLabel(phase)
        val map = LinkedHashMap<String, Player>()
        val sugMap = LinkedHashMap<String, LeagueOffseason.RetainSuggestion>()
        val built = ArrayList<TalentRowUi>()

        when (tab) {
            HubTab.RETAIN -> {
                for (p in off.userDraftLocked(u)) {
                    val id = playerKey(p, "draft")
                    map[id] = p
                    built.add(
                        TalentRowUi(
                            id = id,
                            playerName = p.name,
                            position = p.position,
                            ovr = p.ratOvr,
                            primary = "${p.name} ${p.getYrStr()}",
                            secondary = if (p.year >= 5) "Graduated — leaving" else "Leaving for draft",
                            costLine = "—",
                            statusLabel = "Draft",
                            showCheck = false,
                            checked = false,
                            locked = true,
                            moneyRow = false,
                            sortOvr = p.ratOvr,
                            sortCost = Int.MAX_VALUE,
                            suggestionKey = null,
                        )
                    )
                }
                suggestions = off.suggestUserRetains(u)
                for (s in suggestions) {
                    val p = s.player
                    val key = suggestionKey(s)
                    val id = playerKey(p, key)
                    map[id] = p
                    sugMap[key] = s
                    val (secondary, cost, status, sortCost) = when (s.bucket) {
                        "DRAFT_STAY" -> Tuple4(
                            "Pay to stay from draft",
                            NilMoney.format(s.stayBonus),
                            "Stay",
                            s.stayBonus,
                        )
                        "RISK" -> Tuple4(
                            p.transferReasonText ?: "At risk",
                            NilMoney.format(s.yearOneCost(u)),
                            s.status.displayName(),
                            s.yearOneCost(u),
                        )
                        else -> Tuple4(
                            "Deal expired — renew",
                            NilMoney.format(s.yearOneCost(u)),
                            "Renew",
                            s.yearOneCost(u),
                        )
                    }
                    built.add(
                        TalentRowUi(
                            id = id,
                            playerName = p.name,
                            position = p.position,
                            ovr = p.ratOvr,
                            primary = "${p.name} ${p.getYrStr()}",
                            secondary = secondary,
                            costLine = cost,
                            statusLabel = status,
                            showCheck = true,
                            checked = s.selected,
                            locked = false,
                            moneyRow = false,
                            sortOvr = p.ratOvr,
                            sortCost = sortCost,
                            suggestionKey = key,
                        )
                    )
                }
            }
            HubTab.PORTAL -> {
                for (p in off.transferPortal) {
                    val id = playerKey(p, "portal")
                    map[id] = p
                    val years = ProgramOffers.suggestedContractYears(p)
                    val min = ProgramOffers.minimumAcceptable(p, maxOf(1, p.portalRiskTier))
                    val nil = if (min == RosterStatus.SCHOLARSHIP_PLUS_NIL) {
                        ProgramOffers.annualNilFor(p, u, years)
                    } else {
                        0
                    }
                    val cost = u.offerTotalCost(min, nil)
                    val prior = p.priorTeam?.abbr ?: "?"
                    built.add(
                        TalentRowUi(
                            id = id,
                            playerName = p.name,
                            position = p.position,
                            ovr = p.ratOvr,
                            primary = "${p.name} ${p.getYrStr()}",
                            secondary = "From $prior" +
                                (if (p.transferReason != null) " · ${p.transferReason.label}" else ""),
                            costLine = NilMoney.format(cost),
                            statusLabel = min.displayName(),
                            showCheck = false,
                            checked = false,
                            locked = false,
                            moneyRow = false,
                            sortOvr = p.ratOvr,
                            sortCost = cost,
                            suggestionKey = null,
                        )
                    )
                }
            }
            HubTab.SCHEDULE -> {
                // schedule weeks populated below
            }
            HubTab.HS -> {
                for (p in off.hsClass) {
                    val id = playerKey(p, "hs")
                    map[id] = p
                    val years = ProgramOffers.suggestedContractYears(p)
                    val min = when {
                        p.ratOvr >= 82 -> RosterStatus.SCHOLARSHIP_PLUS_NIL
                        p.ratOvr >= 60 -> RosterStatus.SCHOLARSHIP
                        else -> RosterStatus.PWO
                    }
                    val nil = if (min == RosterStatus.SCHOLARSHIP_PLUS_NIL) {
                        ProgramOffers.annualNilFor(p, u, years)
                    } else {
                        0
                    }
                    val cost = u.offerTotalCost(min, nil)
                    built.add(
                        TalentRowUi(
                            id = id,
                            playerName = p.name,
                            position = p.position,
                            ovr = p.ratOvr,
                            primary = "${p.name} Fr",
                            secondary = "Pot ${p.ratPot}",
                            costLine = NilMoney.format(cost),
                            statusLabel = min.displayName(),
                            showCheck = false,
                            checked = false,
                            locked = false,
                            moneyRow = false,
                            sortOvr = p.ratOvr,
                            sortCost = cost,
                            suggestionKey = null,
                        )
                    )
                }
            }
            HubTab.MONEY -> {
                for ((index, line) in u.budgetLedgerRows().withIndex()) {
                    val parts = line.split("\n")
                    built.add(
                        TalentRowUi(
                            id = "money-$index",
                            playerName = null,
                            position = "ALL",
                            ovr = 0,
                            primary = parts.getOrElse(0) { line },
                            secondary = parts.getOrElse(1) { "" },
                            costLine = parts.getOrElse(2) { "" },
                            statusLabel = "Ledger",
                            showCheck = false,
                            checked = false,
                            locked = false,
                            moneyRow = true,
                            sortOvr = 0,
                            sortCost = 0,
                            suggestionKey = null,
                        )
                    )
                }
            }
        }

        playerByKey = map
        suggestionByKey = sugMap
        allRows = built

        val scheduleWeeks = buildScheduleWeeks(u)
        val openOoc = scheduleWeeks.count { it.open }
        val filledOoc = scheduleWeeks.count {
            !it.locked && !it.open
                && (it.status == "OOC" || it.status == "OOC Rivalry" || it.status == "Rivalry Game OOC")
        }
        val contractRows = buildContractRows(u)
        val rivalSummary = u.rivalries.joinToString(" · ") { r ->
            r.displayLabel()
        }

        val primaryLabel = when {
            tab == HubTab.RETAIN && phase == OffseasonSession.Phase.RETENTION ->
                "Approve Retention → Portal"
            tab == HubTab.PORTAL && phase == OffseasonSession.Phase.PORTAL ->
                "Done — Begin Scheduling"
            tab == HubTab.SCHEDULE && phase == OffseasonSession.Phase.SCHEDULE ->
                if (GameSession.needsOocScheduling()) {
                    "Done — Start Season"
                } else {
                    "Done — Begin HS Recruiting"
                }
            tab == HubTab.HS && phase == OffseasonSession.Phase.HS ->
                "Done — Start Season"
            else -> null
        }

        _uiState.update {
            it.copy(
                teamName = u.name,
                phaseLabel = phaseName,
                browsing = !canAct && tab != HubTab.MONEY,
                cashLabel = u.budgetCashLabel(),
                y1Label = u.budgetY1FreeLabel(),
                schollyLabel = u.budgetSchollyLabel(),
                rosterLabel = u.budgetRosterLabel(),
                scheduleWeeks = scheduleWeeks,
                openOocSlots = openOoc,
                filledOocSlots = filledOoc,
                contractRows = contractRows,
                rivalSummary = rivalSummary,
                primaryLabel = primaryLabel,
            )
        }
        applyFilters()
    }

    private fun buildContractRows(u: Team): List<ContractRowUi> {
        val book = league?.oocContracts ?: return emptyList()
        val year = league?.year ?: 0
        return book.forTeam(u.abbr).map { c ->
            val games = c.games.joinToString("; ") { g ->
                val role = when {
                    g.homeAbbr == u.abbr -> "home"
                    else -> "away"
                }
                "${g.year} $role vs ${if (g.homeAbbr == u.abbr) g.awayAbbr else g.homeAbbr}" +
                    if (g.guarantee > 0) " (${NilMoney.format(g.guarantee)})" else ""
            }
            ContractRowUi(c.id, "${c.teamA}–${c.teamB}: $games")
        }.filter { row ->
            book.findById(row.id)?.hasFutureGames(year) == true
                || book.findById(row.id)?.gameForYear(year) != null
        }
    }

    private fun buildScheduleWeeks(u: Team): List<ScheduleWeekUi> {
        val weeks = ArrayList<ScheduleWeekUi>()
        for (week in 0 until League.REGULAR_SEASON_WEEKS) {
            if (u.isByeWeek(week)) {
                weeks.add(
                    ScheduleWeekUi(
                        week = week,
                        weekLabel = "Week ${week + 1}",
                        status = "BYE",
                        detail = "Immovable bye week",
                        locked = true,
                        open = false,
                        opponentAbbr = null,
                    ),
                )
                continue
            }
            val game = u.gameSchedule.getOrNull(week)
            if (game == null) {
                weeks.add(
                    ScheduleWeekUi(
                        week = week,
                        weekLabel = "Week ${week + 1}",
                        status = "Open",
                        detail = "Tap to choose OOC opponent",
                        locked = false,
                        open = true,
                        opponentAbbr = null,
                    ),
                )
                continue
            }
            val opp = if (game.homeTeam == u) game.awayTeam else game.homeTeam
            val homeAway = if (game.homeTeam == u) "vs" else "@"
            val isOoc = game.gameName == "OOC" || game.gameName == "OOC Rivalry"
                || game.gameName == "Rivalry Game OOC"
            val contractLocked = game.contractId != null
            val rivalry = game.rivalryStrength()
            val moneyNote = if (contractLocked && game.contractId != null) {
                val cg = league?.oocContracts?.findById(game.contractId)?.gameForYear(league?.year ?: 0)
                if (cg != null && cg.guarantee > 0) {
                    if (game.homeTeam == u) " · Pay ${NilMoney.format(cg.guarantee)}"
                    else " · Earn ${NilMoney.format(cg.guarantee)}"
                } else if (contractLocked) " · Contract" else ""
            } else ""
            weeks.add(
                ScheduleWeekUi(
                    week = week,
                    weekLabel = "Week ${week + 1}",
                    status = game.gameName,
                    detail = "$homeAway ${opp.name} (${opp.abbr})$moneyNote",
                    locked = !isOoc || contractLocked,
                    open = false,
                    opponentAbbr = opp.abbr,
                    contractLocked = contractLocked,
                    rivalryLabel = if (rivalry > 0) "${Rivalry.band(rivalry)} ($rivalry)" else null,
                ),
            )
        }
        return weeks
    }

    private fun applyFilters() {
        val state = _uiState.value
        val u = user ?: return
        val filtered = if (state.selectedTab == HubTab.MONEY) {
            allRows
        } else {
            val q = state.search.trim().lowercase(Locale.US)
            allRows.filter { r ->
                (state.positionFilter == "ALL" || state.positionFilter == r.position) &&
                    (q.isEmpty() || (r.playerName?.lowercase(Locale.US)?.contains(q) == true)) &&
                    (!state.affordableOnly || r.sortCost <= u.recruitMoney)
            }.sortedWith { a, b ->
                when (state.sortMode) {
                    1 -> a.sortCost - b.sortCost
                    2 -> (a.playerName ?: "").compareTo(b.playerName ?: "", ignoreCase = true)
                    else -> b.sortOvr - a.sortOvr
                }
            }
        }
        _uiState.update {
            it.copy(
                rows = filtered,
                cashLabel = u.budgetCashLabel(),
                y1Label = u.budgetY1FreeLabel(),
                schollyLabel = u.budgetSchollyLabel(),
                rosterLabel = u.budgetRosterLabel(),
            )
        }
    }

    private fun canActOnTab(tab: HubTab): Boolean {
        if (tab == HubTab.MONEY) return false
        return OffseasonSession.phase == phaseForTab(tab)
    }

    private fun tabForPhase(phase: OffseasonSession.Phase?): HubTab = when (phase) {
        OffseasonSession.Phase.PORTAL -> HubTab.PORTAL
        OffseasonSession.Phase.SCHEDULE -> HubTab.SCHEDULE
        OffseasonSession.Phase.HS -> HubTab.HS
        else -> HubTab.RETAIN
    }

    private fun phaseForTab(tab: HubTab): OffseasonSession.Phase = when (tab) {
        HubTab.PORTAL -> OffseasonSession.Phase.PORTAL
        HubTab.SCHEDULE -> OffseasonSession.Phase.SCHEDULE
        HubTab.HS -> OffseasonSession.Phase.HS
        else -> OffseasonSession.Phase.RETENTION
    }

    private fun playerKey(p: Player, suffix: String): String =
        "${p.position}|${p.name}|$suffix|${System.identityHashCode(p)}"

    private fun suggestionKey(s: LeagueOffseason.RetainSuggestion): String =
        "${s.player.position}|${s.player.name}|${s.bucket}|${System.identityHashCode(s)}"

    private fun costPreviewText(p: Player, u: Team, st: RosterStatus, y: Int): String {
        val nil = if (st == RosterStatus.SCHOLARSHIP_PLUS_NIL) {
            ProgramOffers.annualNilFor(p, u, y)
        } else {
            0
        }
        val cost = u.offerTotalCost(st, nil)
        return st.displayName() +
            (if (nil > 0) " ${NilMoney.format(nil)}/yr" else "") +
            " · ${y}yr · year-1 ${NilMoney.format(cost)}"
    }

    private data class Tuple4(
        val a: String,
        val b: String,
        val c: String,
        val d: Int,
    )
}
