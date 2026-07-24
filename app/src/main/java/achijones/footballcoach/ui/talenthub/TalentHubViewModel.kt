package achijones.footballcoach.ui.talenthub

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import CFBsimPack.GameSession
import CFBsimPack.League
import CFBsimPack.LeagueOffseason
import CFBsimPack.NilMoney
import CFBsimPack.OffseasonSession
import CFBsimPack.Player
import CFBsimPack.PlayerRatings
import CFBsimPack.PositionOvr
import CFBsimPack.ProgramOffers
import CFBsimPack.RosterStatus
import CFBsimPack.Team
import achijones.footballcoach.ui.theme.UserBrandTheme
import achijones.footballcoach.ui.util.SaveSlots
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

enum class HubTab { RETAIN, PORTAL, HS, MONEY }

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
    val purseLabel: String = "",
    val y1Label: String = "",
    val schollyLabel: String = "",
    val rosterLabel: String = "",
    val search: String = "",
    val positionFilter: String = "ALL",
    val sortMode: Int = 0,
    val affordableOnly: Boolean = false,
    val rows: List<TalentRowUi> = emptyList(),
    val primaryLabel: String? = null,
    val message: String? = null,
    val offerSheet: OfferSheetState? = null,
    val showSaveDialog: Boolean = false,
    val saveSlotInfos: List<String> = emptyList(),
    val confirmOverwriteIndex: Int? = null,
    val showBuyoutConfirm: Boolean = false,
    val buyoutPlayerName: String? = null,
    val buyoutCostLabel: String? = null,
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
        GameSession.setStayingOnMainDuringOffseason(false)
        league = OffseasonSession.league
        offseason = OffseasonSession.offseason
        user = league?.userTeam
        UserBrandTheme.setFrom(user)
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

    /** Leaves for Main without discarding the live offseason session. */
    fun requestBackToMain() {
        GameSession.setStayingOnMainDuringOffseason(true)
        _uiState.update { it.copy(navigateToMain = true) }
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
        if (ok) {
            GameSession.setActiveSaveSlot(index)
        }
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
                    buyoutLabel = buyoutButtonLabel(u.buyoutCost(player)),
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
                buyoutCostLabel = if (cost > 0) NilMoney.format(cost) else null,
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
                message = if (ok) {
                    "Released ${player.name}"
                } else {
                    "Cannot afford buyout."
                },
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
                // Scheduling moved to ScheduleScreen; bounce back to Main.
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
                                (if (p.transferReason != null) " · ${p.transferReason.label}" else "") +
                                " · ${attrSummary(p)}",
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
                            secondary = "Pot ${p.ratPot} · ${attrSummary(p)}",
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

        val primaryLabel = when {
            tab == HubTab.RETAIN && phase == OffseasonSession.Phase.RETENTION ->
                "Approve Retention → Portal"
            tab == HubTab.PORTAL && phase == OffseasonSession.Phase.PORTAL ->
                "Done — Begin Scheduling"
            tab == HubTab.HS && phase == OffseasonSession.Phase.HS ->
                "Done — Start Season"
            else -> null
        }

        _uiState.update {
            it.copy(
                teamName = u.name,
                phaseLabel = OffseasonSession.phaseLabel(phase),
                browsing = !canAct && tab != HubTab.MONEY,
                cashLabel = u.budgetCashLabel(),
                purseLabel = u.budgetPurseLabel(),
                y1Label = u.budgetY1FreeLabel(),
                schollyLabel = u.budgetSchollyLabel(),
                rosterLabel = u.budgetRosterLabel(),
                primaryLabel = primaryLabel,
            )
        }
        applyFilters()
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
                purseLabel = u.budgetPurseLabel(),
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
        OffseasonSession.Phase.HS -> HubTab.HS
        OffseasonSession.Phase.SCHEDULE -> HubTab.RETAIN // scheduling is ScheduleScreen
        else -> HubTab.RETAIN
    }

    private fun phaseForTab(tab: HubTab): OffseasonSession.Phase = when (tab) {
        HubTab.PORTAL -> OffseasonSession.Phase.PORTAL
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

    private fun buyoutButtonLabel(cost: Int): String {
        return if (cost > 0) {
            "Cut / Buy out (${NilMoney.format(cost)})"
        } else {
            "Cut"
        }
    }

    /** Short key-attr preview for recruit cards (not full 3-skill legacy). */
    private fun attrSummary(p: Player): String {
        val bag = p.ratings ?: return "OVR ${p.ratOvr}"
        val primary = PositionOvr.primaryGroup(p)
        val keys = when (primary) {
            CFBsimPack.PositionGroup.QB -> arrayOf("tha", "thp", "spd")
            CFBsimPack.PositionGroup.RB -> arrayOf("spd", "elu", "stre")
            CFBsimPack.PositionGroup.WR -> arrayOf("hnd", "spd", "rtr")
            CFBsimPack.PositionGroup.OL -> arrayOf("pbk", "rbk", "stre")
            CFBsimPack.PositionGroup.EDGE -> arrayOf("prs", "spd", "stre")
            CFBsimPack.PositionGroup.K -> arrayOf("kpw", "kac")
            CFBsimPack.PositionGroup.P -> arrayOf("ppw", "pac")
            else -> arrayOf("spd", "stre", "endu")
        }
        return keys.joinToString(" ") { k ->
            "${PlayerRatings.displayLabel(k)} ${bag.get(k)}"
        }
    }

    private data class Tuple4(
        val a: String,
        val b: String,
        val c: String,
        val d: Int,
    )
}
