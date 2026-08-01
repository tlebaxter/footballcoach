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
import achijones.footballcoach.save.CareerPersistence
import achijones.footballcoach.save.CareerSessionRestorer
import achijones.footballcoach.save.OffseasonFlow
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
import java.util.Locale

enum class HubTab { RETAIN, PORTAL, HS, MONEY }

data class TalentRowUi(
    val id: String,
    val playerName: String?,
    val position: String,
    val ovr: Int,
    val primary: String,
    val secondary: String,
    /** Purse / NIL cash label for the row (not scholarship slots). */
    val cashLine: String,
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
    val impact: OfferImpactUi? = null,
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
    val pwoLabel: String = "",
    val depthBoard: DepthBoardUi? = null,
    val showDepthBoard: Boolean = false,
    val positionContext: PositionDepthUi? = null,
    val needLabels: List<String> = emptyList(),
    val search: String = "",
    val positionFilter: String = "ALL",
    val sortMode: Int = 0,
    val affordableOnly: Boolean = false,
    val rows: List<TalentRowUi> = emptyList(),
    val primaryLabel: String? = null,
    val message: String? = null,
    val offerSheet: OfferSheetState? = null,
    val showSaveDialog: Boolean = false,
    val saveSlotInfos: List<achijones.footballcoach.save.SlotInfo> = emptyList(),
    val confirmOverwriteIndex: Int? = null,
    val confirmDeleteIndex: Int? = null,
    val showBuyoutConfirm: Boolean = false,
    val buyoutPlayerName: String? = null,
    val buyoutCostLabel: String? = null,
    val navigateToMain: Boolean = false,
    val navigateToSchedule: Boolean = false,
    val navigateHome: Boolean = false,
)

class TalentHubViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = SaveRepository.get(application)

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
        viewModelScope.launch {
            if (!GameSession.readyOffseason()) {
                val result = withContext(Dispatchers.IO) {
                    CareerSessionRestorer.resumeIfNeeded(getApplication(), repo)
                }
                val ok = result is CareerSessionRestorer.ResumeResult.Success ||
                    result is CareerSessionRestorer.ResumeResult.AlreadyLoaded
                if (!ok || !GameSession.readyOffseason()) {
                    val msg = (result as? CareerSessionRestorer.ResumeResult.Failed)?.message
                    _uiState.update {
                        it.copy(
                            missingSession = true,
                            ready = false,
                            navigateHome = true,
                            message = msg,
                        )
                    }
                    return@launch
                }
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

    fun openDepthBoard() {
        _uiState.update { it.copy(showDepthBoard = true) }
    }

    fun dismissDepthBoard() {
        _uiState.update { it.copy(showDepthBoard = false) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun consumeNavigateToMain() {
        _uiState.update { it.copy(navigateToMain = false) }
    }

    fun consumeNavigateToSchedule() {
        _uiState.update { it.copy(navigateToSchedule = false) }
    }

    fun consumeNavigateHome() {
        _uiState.update { it.copy(navigateHome = false) }
    }

    private suspend fun autosaveLive(): Boolean {
        val l = league ?: OffseasonSession.league ?: return false
        if (!GameSession.hasActiveSaveSlot()) return false
        return withContext(Dispatchers.IO) {
            CareerPersistence.saveActive(l, repo).isSuccess
        }
    }

    /** Leaves for Main without discarding the live offseason session. */
    fun requestBackToMain() {
        GameSession.setStayingOnMainDuringOffseason(true)
        _uiState.update { it.copy(navigateToMain = true) }
    }

    fun openSaveDialog() {
        viewModelScope.launch {
            val slots = repo.listSlots()
            _uiState.update {
                it.copy(
                    showSaveDialog = true,
                    saveSlotInfos = slots,
                    confirmOverwriteIndex = null,
                )
            }
        }
    }

    fun dismissSaveDialog() {
        _uiState.update {
            it.copy(showSaveDialog = false, confirmOverwriteIndex = null, confirmDeleteIndex = null)
        }
    }

    fun pickSaveSlot(index: Int) {
        val infos = _uiState.value.saveSlotInfos
        if (index < 0 || index >= infos.size) return
        if (infos[index].status == SlotStatus.EMPTY) {
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

    fun requestDeleteSlot(index: Int) {
        _uiState.update { it.copy(confirmDeleteIndex = index) }
    }

    fun dismissDelete() {
        _uiState.update { it.copy(confirmDeleteIndex = null) }
    }

    fun confirmDelete() {
        val index = _uiState.value.confirmDeleteIndex ?: return
        viewModelScope.launch {
            repo.delete(index)
            val slots = repo.listSlots()
            _uiState.update {
                it.copy(confirmDeleteIndex = null, saveSlotInfos = slots, message = "Slot ${index + 1} deleted")
            }
        }
    }

    private fun writeSave(index: Int) {
        val l = league ?: return
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { repo.save(index, l).isSuccess }
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
    }

    private fun persistAfterMutation() {
        viewModelScope.launch {
            val ok = autosaveLive()
            if (!ok && GameSession.hasActiveSaveSlot()) {
                _uiState.update { it.copy(message = "Signed — save failed") }
            }
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
        val costPreview = if (draftStay) {
            "Stay bonus: ${NilMoney.format(stayBonus)}"
        } else {
            costPreviewText(player, u, startStatus, startYears)
        }
        val impact = computeOfferImpact(
            u = u,
            player = player,
            draftStay = draftStay,
            retention = retention,
            status = startStatus,
            years = startYears,
            stayBonus = stayBonus,
        )
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
                    costPreview = costPreview,
                    confirmLabel = when {
                        draftStay -> "Pay to stay"
                        retention -> "Select offer"
                        else -> "Sign"
                    },
                    showBuyout = retention && !draftStay,
                    buyoutLabel = buyoutButtonLabel(u.buyoutCost(player)),
                    stayBonusLabel = NilMoney.format(stayBonus),
                    suggestionKey = row.suggestionKey,
                    impact = impact,
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
        val impact = computeOfferImpact(
            u = u,
            player = player,
            draftStay = false,
            retention = sheet.retention,
            status = status,
            years = sheet.years,
            stayBonus = 0,
        )
        _uiState.update {
            it.copy(offerSheet = sheet.copy(status = status, costPreview = preview, impact = impact))
        }
    }

    fun updateOfferYears(years: Int) {
        val sheet = _uiState.value.offerSheet ?: return
        if (sheet.draftStay) return
        val player = offerPlayer ?: return
        val u = user ?: return
        val y = years.coerceIn(1, sheet.maxYears)
        val preview = costPreviewText(player, u, sheet.status, y)
        val impact = computeOfferImpact(
            u = u,
            player = player,
            draftStay = false,
            retention = sheet.retention,
            status = sheet.status,
            years = y,
            stayBonus = 0,
        )
        _uiState.update {
            it.copy(offerSheet = sheet.copy(years = y, costPreview = preview, impact = impact))
        }
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
            persistAfterMutation()
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
            persistAfterMutation()
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
        if (ok) persistAfterMutation()
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
        if (ok) persistAfterMutation()
    }

    fun onPrimary() {
        when (OffseasonSession.phase) {
            OffseasonSession.Phase.RETENTION -> approveRetention()
            OffseasonSession.Phase.PORTAL -> finishPortalPhase()
            OffseasonSession.Phase.SCHEDULE -> {
                // Scheduling moved to ScheduleScreen; bounce back to Main.
                _uiState.update { it.copy(navigateToMain = true) }
            }
            OffseasonSession.Phase.HS -> finishRecruitingPhase()
        }
    }

    private fun approveRetention() {
        viewModelScope.launch {
            try {
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
                OffseasonFlow.finishRetention()
                league = OffseasonSession.league
                offseason = OffseasonSession.offseason
                user = league?.userTeam
                val saved = autosaveLive()
                val tab = HubTab.PORTAL
                _uiState.update {
                    it.copy(
                        selectedTab = tab,
                        message = if (saved || !GameSession.hasActiveSaveSlot()) {
                            "Retention applied ($applied). Opening portal…"
                        } else {
                            "Retention applied ($applied) — save failed"
                        },
                    )
                }
                reloadTab()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(message = e.message ?: "Could not finish retention")
                }
            }
        }
    }

    private fun finishPortalPhase() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) { OffseasonFlow.finishPortal() }
                league = OffseasonSession.league
                offseason = OffseasonSession.offseason
                user = league?.userTeam
                val saved = autosaveLive()
                _uiState.update {
                    it.copy(
                        navigateToSchedule = true,
                        message = if (!saved && GameSession.hasActiveSaveSlot()) {
                            "Portal closed — save failed"
                        } else {
                            null
                        },
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(message = e.message ?: "Could not finish portal")
                }
            }
        }
    }

    private fun finishRecruitingPhase() {
        viewModelScope.launch {
            try {
                val budget = user?.recruitMoney ?: 0
                withContext(Dispatchers.Default) { OffseasonFlow.finishRecruiting(budget) }
                league = GameSession.getLeague()
                user = league?.userTeam
                offseason = null
                val saved = autosaveLive()
                _uiState.update {
                    it.copy(
                        navigateToMain = true,
                        message = if (!saved && GameSession.hasActiveSaveSlot()) {
                            "Recruiting done — save failed"
                        } else {
                            null
                        },
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(message = e.message ?: "Could not finish recruiting")
                }
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
                            cashLine = "—",
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
                suggestions = mergeRetainSuggestions(off.suggestUserRetains(u), suggestions)
                for (s in suggestions) {
                    val p = s.player
                    val key = suggestionKey(s)
                    val id = playerKey(p, key)
                    map[id] = p
                    sugMap[key] = s
                    val (secondary, cash, status, sortCost) = when (s.bucket) {
                        "DRAFT_STAY" -> Tuple4(
                            "Pay to stay from draft",
                            NilMoney.format(s.stayBonus),
                            "Stay",
                            s.stayBonus,
                        )
                        "RISK" -> Tuple4(
                            p.transferReasonText ?: "At risk",
                            offerCashLine(s.status, s.yearOneNilPurse(u)),
                            s.status.displayName(),
                            s.yearOneNilPurse(u),
                        )
                        else -> Tuple4(
                            "Deal expired — renew",
                            offerCashLine(s.status, s.yearOneNilPurse(u)),
                            "Renew",
                            s.yearOneNilPurse(u),
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
                            cashLine = cash,
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
                    val cost = u.nilPurseCost(min, nil)
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
                            cashLine = offerCashLine(min, cost),
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
                    val cost = u.nilPurseCost(min, nil)
                    built.add(
                        TalentRowUi(
                            id = id,
                            playerName = p.name,
                            position = p.position,
                            ovr = p.ratOvr,
                            primary = "${p.name} Fr",
                            secondary = "Pot ${p.ratPot} · ${attrSummary(p)}",
                            cashLine = offerCashLine(min, cost),
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
                            cashLine = parts.getOrElse(2) { "" },
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

        val depth = buildDepthBoard(u)
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
                pwoLabel = depth.pwoLabel,
                depthBoard = depth,
                needLabels = depth.worstNeedLabels,
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
        val depth = state.depthBoard ?: buildDepthBoard(u)
        val positionContext = if (state.positionFilter == "ALL") {
            null
        } else {
            depth.positions.find { it.position == state.positionFilter }
                ?: buildPositionDepth(u, state.positionFilter)
        }
        _uiState.update {
            it.copy(
                rows = filtered,
                cashLabel = u.budgetCashLabel(),
                purseLabel = u.budgetPurseLabel(),
                y1Label = u.budgetY1FreeLabel(),
                schollyLabel = u.budgetSchollyLabel(),
                rosterLabel = u.budgetRosterLabel(),
                pwoLabel = depth.pwoLabel,
                depthBoard = depth,
                needLabels = depth.worstNeedLabels,
                positionContext = positionContext,
            )
        }
    }

    private fun computeOfferImpact(
        u: Team,
        player: Player,
        draftStay: Boolean,
        retention: Boolean,
        status: RosterStatus,
        years: Int,
        stayBonus: Int,
    ): OfferImpactUi {
        val kind = when {
            draftStay -> OfferImpactKind.DRAFT_STAY
            retention -> OfferImpactKind.RETENTION
            else -> OfferImpactKind.PORTAL_OR_HS
        }
        val nil = if (status == RosterStatus.SCHOLARSHIP_PLUS_NIL) {
            ProgramOffers.annualNilFor(player, u, years)
        } else {
            0
        }
        return buildOfferImpact(
            team = u,
            player = player,
            kind = kind,
            proposedStatus = status,
            proposedNil = nil,
            years = years,
            stayBonus = stayBonus,
        )
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
        "${s.bucket}|${System.identityHashCode(s.player)}"

    /**
     * Refresh candidates from the league, but keep the user's prior retain choices
     * (selected + offer terms) for players still in the list.
     */
    private fun mergeRetainSuggestions(
        fresh: List<LeagueOffseason.RetainSuggestion>,
        prior: List<LeagueOffseason.RetainSuggestion>,
    ): List<LeagueOffseason.RetainSuggestion> {
        if (prior.isEmpty()) return fresh
        val priorByKey = prior.associateBy { suggestionKey(it) }
        for (s in fresh) {
            val old = priorByKey[suggestionKey(s)] ?: continue
            s.selected = old.selected
            s.status = old.status
            s.years = old.years
            s.nil = old.nil
            s.stayBonus = old.stayBonus
        }
        return fresh
    }

    private fun offerCashLine(status: RosterStatus, nilPurse: Int): String {
        return when (status) {
            RosterStatus.SCHOLARSHIP_PLUS_NIL ->
                if (nilPurse > 0) "NIL ${NilMoney.format(nilPurse)}" else "$0 cash"
            else -> "$0 cash"
        }
    }

    private fun costPreviewText(p: Player, u: Team, st: RosterStatus, y: Int): String {
        val nil = if (st == RosterStatus.SCHOLARSHIP_PLUS_NIL) {
            ProgramOffers.annualNilFor(p, u, y)
        } else {
            0
        }
        val resources = u.offerResources(st, nil)
        val slots = resources.scholarshipSlots
        val slotLabel = if (slots == 1) "1 scholarship" else "$slots scholarships"
        return if (st == RosterStatus.SCHOLARSHIP_PLUS_NIL && resources.annualNilCash > 0) {
            "${st.chipLabel()} · NIL ${NilMoney.format(resources.annualNilCash)}/yr · ${y}yr · " +
                "year-1 purse ${NilMoney.format(resources.annualNilCash)} · $slotLabel"
        } else {
            "${st.chipLabel()} · $0 cash · $slotLabel · ${y}yr"
        }
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
