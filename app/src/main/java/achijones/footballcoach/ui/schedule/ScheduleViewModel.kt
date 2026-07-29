package achijones.footballcoach.ui.schedule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import CFBsimPack.GameSession
import CFBsimPack.League
import CFBsimPack.NilMoney
import CFBsimPack.OffseasonSession
import CFBsimPack.OocContract
import CFBsimPack.OocScheduleBuilder
import CFBsimPack.Rivalry
import CFBsimPack.Team
import achijones.footballcoach.ui.theme.UserBrandTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs

/** Series return legs land a few years out, matching how real home-and-homes are booked. */
private const val DEFAULT_HH_RETURN_OFFSET = 2

enum class DealSite {
    HOME,
    AWAY,
}

enum class OpponentMoneyKind {
    PAY,
    EARN,
    NONE,
}

enum class OpponentSection {
    OPEN_CONTRACT,
    RIVALRY,
    BUY,
    EARN,
    PEER,
}

data class ScheduleWeekUi(
    val week: Int,
    val weekLabel: String,
    val status: String,
    val detail: String,
    val locked: Boolean,
    val open: Boolean,
    val opponentAbbr: String?,
    val opponentName: String? = null,
    val homeGame: Boolean = true,
    val contractLocked: Boolean = false,
    val contractId: String? = null,
    val rivalryLabel: String? = null,
    val moneyLabel: String? = null,
    val isBye: Boolean = false,
    val isOoc: Boolean = false,
)

data class ContractGameChipUi(
    val year: Int,
    val role: String,
    val opponentAbbr: String,
    val opponentName: String?,
    val guaranteeLabel: String?,
    val canReschedule: Boolean = false,
    val preferredWeek: Int = -1,
)

data class ContractCardUi(
    val id: String,
    val typeLabel: String,
    val teamAAbbr: String,
    val teamAName: String?,
    val teamBAbbr: String,
    val teamBName: String?,
    val fulfillByYear: Int,
    val buyoutLabel: String,
    val games: List<ContractGameChipUi>,
    val statusLabel: String,
    val involvesCurrentYear: Boolean,
)

data class OpponentOptionUi(
    val abbr: String,
    val name: String,
    val conference: String,
    val rivalryLabel: String?,
    val moneyKind: OpponentMoneyKind,
    val moneyLabel: String,
    val moneyAmount: Int = 0,
    val tierGap: Int = 0,
    val preferredWeek: Int = -1,
    val affordable: Boolean = true,
    val matchupLabel: String? = null,
    val dealTypeChip: String? = null,
    val section: OpponentSection,
    val openContractId: String? = null,
    val userIsHome: Boolean = true,
)

data class DonePreviewLineUi(
    val week: Int,
    val label: String,
)

data class ScheduleUiState(
    val ready: Boolean = false,
    val missingLeague: Boolean = false,
    val teamName: String = "",
    val teamAbbr: String = "",
    val year: Int = 0,
    val schedulingActive: Boolean = false,
    val yearOneOoc: Boolean = false,
    val rivalSummary: String = "",
    val openOocSlots: Int = 0,
    val filledOocSlots: Int = 0,
    val openWeekNumbers: List<Int> = emptyList(),
    val unplacedOpenContractCount: Int = 0,
    val selectedHorizonYear: Int = 0,
    val horizonYears: List<Int> = emptyList(),
    val oocNetAmount: Int = 0,
    val oocNetLabel: String = "",
    val budgetAvailable: Int = 0,
    val guaranteesCommitted: Int = 0,
    val budgetLabel: String = "",
    val contractCards: List<ContractCardUi> = emptyList(),
    val filteredContracts: List<ContractCardUi> = emptyList(),
    val scheduleWeeks: List<ScheduleWeekUi> = emptyList(),
    val opponentPickerWeek: Int? = null,
    /** When non-null and week is null, signing future deals for this year only. */
    val dealTargetYear: Int? = null,
    val opponentOptions: List<OpponentOptionUi> = emptyList(),
    val availableConferences: List<String> = emptyList(),
    val conferenceFilter: String? = null,
    val siteFilter: DealSite? = null,
    val dealOpponentAbbr: String? = null,
    val dealQuote: String = "",
    val singleGameAllowed: Boolean = true,
    val hhQuote: String = "",
    val hhAllowed: Boolean = true,
    val twoForOneQuote: String = "",
    val twoForOneAllowed: Boolean = false,
    val dealSite: DealSite = DealSite.HOME,
    val hhReturnOffset: Int = DEFAULT_HH_RETURN_OFFSET,
    val cancelConfirmId: String? = null,
    val cancelConfirmLabel: String? = null,
    val rescheduleContractId: String? = null,
    val rescheduleFromYear: Int? = null,
    val rescheduleSelectedYear: Int? = null,
    val rescheduleSelectedWeek: Int? = null,
    val rescheduleEligibleYears: List<Int> = emptyList(),
    val rescheduleEligibleWeeks: List<Int> = emptyList(),
    val rescheduleFulfillByYear: Int? = null,
    val rescheduleOpponentLabel: String = "",
    val showDonePreview: Boolean = false,
    val donePreviewLines: List<DonePreviewLineUi> = emptyList(),
    val primaryLabel: String? = null,
    val message: String? = null,
    val navigateToMain: Boolean = false,
)

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    private var league: League? = null
    private var user: Team? = null
    private var pickerEligible: List<Team> = emptyList()

    init {
        bootstrap()
    }

    private fun bootstrap() {
        if (!GameSession.hasLeague()) {
            _uiState.update { it.copy(missingLeague = true, ready = false) }
            return
        }
        league = GameSession.getLeague()
        if (OffseasonSession.ready()) {
            league = OffseasonSession.league ?: league
            GameSession.setLeague(league)
        }
        user = league?.userTeam
        if (!GameSession.needsTeamPicker()) {
            UserBrandTheme.setFrom(user)
        }
        val year = league?.year ?: 0
        _uiState.update {
            it.copy(
                ready = true,
                teamName = user?.name.orEmpty(),
                teamAbbr = user?.abbr.orEmpty(),
                year = year,
                selectedHorizonYear = year,
                horizonYears = (0..3).map { year + it },
            )
        }
        reload()
    }

    fun selectHorizonYear(year: Int) {
        _uiState.update { it.copy(selectedHorizonYear = year) }
        applyContractFilter()
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun consumeNavigateToMain() {
        _uiState.update { it.copy(navigateToMain = false) }
    }

    fun onPrimary() {
        if (!schedulingActive()) return
        val u = user ?: return
        val l = league ?: return
        if (_uiState.value.openOocSlots <= 0) {
            finishScheduling()
            return
        }
        val fills = OocScheduleBuilder.previewUserRemainingFill(u, l.teamList)
        val lines = fills.map { fill ->
            val site = if (fill.userHome) "vs" else "@"
            DonePreviewLineUi(
                week = fill.week,
                label = "Week ${fill.week + 1} $site ${fill.opponentName}",
            )
        }
        _uiState.update {
            it.copy(
                showDonePreview = true,
                donePreviewLines = lines,
            )
        }
    }

    fun dismissDonePreview() {
        _uiState.update {
            it.copy(showDonePreview = false, donePreviewLines = emptyList())
        }
    }

    fun confirmDonePreview() {
        dismissDonePreview()
        finishScheduling()
    }

    private fun finishScheduling() {
        GameSession.setPendingOffseasonResult(GameSession.OffseasonResult.DONE_SCHEDULE)
        _uiState.update { it.copy(navigateToMain = true) }
    }

    fun requestBack() {
        if (schedulingActive()) {
            _uiState.update {
                it.copy(message = "Finish scheduling (Done) before leaving, or keep editing your slate.")
            }
            return
        }
        _uiState.update { it.copy(navigateToMain = true) }
    }

    fun openOpponentPicker(week: Int) {
        if (!schedulingActive()) {
            _uiState.update { it.copy(message = "Week picks are only available during the scheduling phase.") }
            return
        }
        val u = user ?: return
        val l = league ?: return
        val existing = u.gameSchedule.getOrNull(week)
        val lockedContractId = existing?.contractId
        if (lockedContractId != null) {
            openRescheduleDialog(lockedContractId, l.year)
            return
        }
        if (!u.isOpenOocWeek(week) && existing?.let {
                it.gameName == "OOC" || it.gameName == "OOC Rivalry"
                    || it.gameName == "Rivalry Game OOC"
            } != true
        ) {
            return
        }
        if (!u.isOpenOocWeek(week)) {
            OocScheduleBuilder.clearUserOocGame(u, week)
        }
        val eligible = OocScheduleBuilder.eligibleOpponents(u, week, l.teamList)
        publishPicker(week, l.year, eligible)
    }

    fun openFutureDealPicker() {
        val u = user ?: return
        val l = league ?: return
        val year = _uiState.value.selectedHorizonYear
        if (year < l.year) return
        val book = l.oocContracts ?: return
        val options = l.teamList
            .filter { it != u && it.conference != u.conference }
            .filter { !book.alreadyContracted(u, it, year) }
            .sortedByDescending { it.programProfile.scheduleTier }
        publishPicker(null, year, options)
    }

    fun dismissOpponentPicker() {
        pickerEligible = emptyList()
        _uiState.update {
            it.copy(
                opponentPickerWeek = null,
                dealTargetYear = null,
                opponentOptions = emptyList(),
                availableConferences = emptyList(),
                conferenceFilter = null,
                siteFilter = null,
                dealOpponentAbbr = null,
                dealQuote = "",
                singleGameAllowed = true,
                hhQuote = "",
                hhAllowed = true,
                twoForOneQuote = "",
                twoForOneAllowed = false,
                dealSite = DealSite.HOME,
            )
        }
    }

    fun clearDealOpponent() {
        _uiState.update {
            it.copy(
                dealOpponentAbbr = null,
                dealQuote = "",
                singleGameAllowed = true,
                hhQuote = "",
                hhAllowed = true,
                twoForOneQuote = "",
                twoForOneAllowed = false,
            )
        }
    }

    fun setConferenceFilter(conference: String?) {
        _uiState.update { it.copy(conferenceFilter = conference) }
    }

    fun setSiteFilter(site: DealSite?) {
        val u = user ?: return
        val year = _uiState.value.dealTargetYear ?: league?.year ?: return
        val week = _uiState.value.opponentPickerWeek
        _uiState.update { it.copy(siteFilter = site) }
        val options = buildOpponentOptions(u, year, week, pickerEligible, site)
        _uiState.update { it.copy(opponentOptions = options) }
    }

    fun selectDealOpponent(abbr: String) {
        val option = _uiState.value.opponentOptions.find { it.abbr == abbr } ?: return
        if (option.openContractId != null) {
            assignOpenContract(option.openContractId)
            return
        }
        val site = _uiState.value.siteFilter ?: defaultSiteFor(option.section)
        val quotes = buildDealQuotes(abbr, site, _uiState.value.hhReturnOffset)
        _uiState.update {
            it.copy(
                dealOpponentAbbr = abbr,
                dealSite = site,
            ).withQuotes(quotes)
        }
    }

    fun setDealSite(site: DealSite) {
        val abbr = _uiState.value.dealOpponentAbbr ?: return
        val quotes = buildDealQuotes(abbr, site, _uiState.value.hhReturnOffset)
        _uiState.update { it.copy(dealSite = site).withQuotes(quotes) }
    }

    fun setHhReturnOffset(offset: Int) {
        val clamped = offset.coerceIn(1, 6)
        val abbr = _uiState.value.dealOpponentAbbr
        val quotes = abbr?.let { buildDealQuotes(it, _uiState.value.dealSite, clamped) }
        _uiState.update {
            val next = it.copy(hhReturnOffset = clamped)
            if (quotes == null) next else next.withQuotes(quotes)
        }
    }

    fun placeAllOpenContracts() {
        val u = user ?: return
        val l = league ?: return
        val book = l.oocContracts ?: return
        val year = l.year
        val placedIds = u.gameSchedule.mapNotNull { it?.contractId }.toSet()
        var placed = 0
        var failed = 0
        book.forTeamInYear(u.abbr, year).forEach { contract ->
            if (contract.id in placedIds) return@forEach
            val cg = contract.gameForYear(year) ?: return@forEach
            if (cg.settled) return@forEach
            val home = l.findTeamAbbr(cg.homeAbbr) ?: return@forEach
            val away = l.findTeamAbbr(cg.awayAbbr) ?: return@forEach
            val preferred = cg.preferredWeek
            val week = when {
                preferred >= 0 && home.isOpenOocWeek(preferred) && away.isOpenOocWeek(preferred) ->
                    preferred
                else -> OocScheduleBuilder.findSharedOpenWeek(home, away)
            }
            if (week < 0) {
                failed++
                return@forEach
            }
            if (OocScheduleBuilder.placeFixedHomeOocGame(home, away, week, contract.id)) {
                cg.preferredWeek = week
                placed++
            } else {
                failed++
            }
        }
        if (_uiState.value.dealTargetYear != null) {
            dismissOpponentPicker()
        }
        _uiState.update {
            it.copy(message = "Placed $placed · $failed could not place")
        }
        reload()
    }

    fun signSingleGame() {
        val u = user ?: return
        val l = league ?: return
        val book = l.oocContracts ?: return
        val year = _uiState.value.dealTargetYear ?: l.year
        val oppAbbr = _uiState.value.dealOpponentAbbr ?: return
        val opponent = l.findTeamAbbr(oppAbbr) ?: return
        val site = _uiState.value.dealSite
        val home = if (site == DealSite.HOME) u else opponent
        val away = if (site == DealSite.HOME) opponent else u
        if (!_uiState.value.singleGameAllowed) {
            _uiState.update {
                it.copy(message = "${opponent.name} will not host you without a return series.")
            }
            return
        }
        val softInitiated = home.abbr == u.abbr
        val contract = book.signSingleGame(home, away, year, softInitiated) ?: run {
            _uiState.update {
                it.copy(message = "Could not sign deal (host may not afford the guarantee).")
            }
            return
        }
        placeIfCurrentWeek(contract.id, home, away, year)
        dismissOpponentPicker()
        reload()
    }

    fun signHomeAndHomeDeal() {
        val u = user ?: return
        val l = league ?: return
        val book = l.oocContracts ?: return
        val startYear = _uiState.value.dealTargetYear ?: l.year
        val offset = _uiState.value.hhReturnOffset
        val oppAbbr = _uiState.value.dealOpponentAbbr ?: return
        val opponent = l.findTeamAbbr(oppAbbr) ?: return
        if (!_uiState.value.hhAllowed) {
            _uiState.update {
                it.copy(message = "${opponent.name} will only visit as part of a 2-for-1.")
            }
            return
        }
        val userHomesFirst = _uiState.value.dealSite == DealSite.HOME
        val softInitiated = u.programProfile.scheduleTier < opponent.programProfile.scheduleTier
        val contract = book.signHomeAndHome(
            u,
            opponent,
            startYear,
            startYear + offset,
            userHomesFirst,
            softInitiated,
        ) ?: run {
            _uiState.update { it.copy(message = "Could not sign home-and-home.") }
            return
        }
        finishSeriesSign(contract.id, startYear, u, opponent)
    }

    fun signTwoForOneDeal() {
        val u = user ?: return
        val l = league ?: return
        val book = l.oocContracts ?: return
        val startYear = _uiState.value.dealTargetYear ?: l.year
        val offset = _uiState.value.hhReturnOffset
        val oppAbbr = _uiState.value.dealOpponentAbbr ?: return
        val opponent = l.findTeamAbbr(oppAbbr) ?: return
        if (!_uiState.value.twoForOneAllowed) {
            _uiState.update {
                it.copy(message = "A 2-for-1 needs a clear gap between the programs.")
            }
            return
        }
        val userHomesFirst = _uiState.value.dealSite == DealSite.HOME
        val contract = book.signTwoForOne(u, opponent, startYear, offset, userHomesFirst)
            ?: run {
                _uiState.update {
                    it.copy(message = "Could not sign 2-for-1 (host may not afford guarantees).")
                }
                return
            }
        finishSeriesSign(contract.id, startYear, u, opponent)
    }

    private fun finishSeriesSign(contractId: String, startYear: Int, u: Team, opponent: Team) {
        val l = league ?: return
        val contract = l.oocContracts?.findById(contractId)
        val cg = contract?.gameForYear(startYear)
        if (cg != null) {
            val home = l.findTeamAbbr(cg.homeAbbr) ?: u
            val away = l.findTeamAbbr(cg.awayAbbr) ?: opponent
            placeIfCurrentWeek(contractId, home, away, startYear)
        }
        dismissOpponentPicker()
        reload()
    }

    fun requestCancelContract(contractId: String) {
        val book = league?.oocContracts ?: return
        val u = user ?: return
        val c = book.findById(contractId) ?: return
        val year = league?.year ?: 0
        val fee = NilMoney.oocCancelBuyout(
            c.type,
            c.remainingGuaranteeTotal(year),
            c.lengthYears,
            c.unsettledGameCount(year),
        ).coerceAtLeast(c.buyout)
        _uiState.update {
            it.copy(
                cancelConfirmId = contractId,
                cancelConfirmLabel = "Cancel ${c.teamA}–${c.teamB}? Buyout ${NilMoney.format(fee)} from ${u.abbr}.",
            )
        }
    }

    fun dismissCancelConfirm() {
        _uiState.update { it.copy(cancelConfirmId = null, cancelConfirmLabel = null) }
    }

    fun openRescheduleDialog(contractId: String, fromYear: Int) {
        val book = league?.oocContracts ?: return
        if (!book.canReschedule(contractId, fromYear)) {
            _uiState.update {
                it.copy(message = "That game can no longer be moved (settled or past fulfill-by).")
            }
            return
        }
        val contract = book.findById(contractId) ?: return
        val game = contract.gameForYear(fromYear) ?: return
        val years = book.eligibleRescheduleYears(contractId, fromYear)
        val weeks = book.eligibleRescheduleWeeks(contractId, fromYear, fromYear)
        val u = user
        val oppAbbr = if (u != null && game.homeAbbr == u.abbr) game.awayAbbr else game.homeAbbr
        val opp = league?.findTeamAbbr(oppAbbr)
        val placedWeek = weeks.firstOrNull { w ->
            val slot = u?.gameSchedule?.getOrNull(w)
            slot?.contractId == contractId
        }
        val selectedWeek = when {
            game.preferredWeek >= 0 && weeks.contains(game.preferredWeek) -> game.preferredWeek
            placedWeek != null -> placedWeek
            weeks.isNotEmpty() -> weeks.first()
            else -> null
        }
        _uiState.update {
            it.copy(
                rescheduleContractId = contractId,
                rescheduleFromYear = fromYear,
                rescheduleSelectedYear = fromYear,
                rescheduleSelectedWeek = selectedWeek,
                rescheduleEligibleYears = years,
                rescheduleEligibleWeeks = weeks,
                rescheduleFulfillByYear = contract.mustFulfillByYear,
                rescheduleOpponentLabel = opp?.name ?: oppAbbr,
            )
        }
    }

    fun dismissRescheduleDialog() {
        _uiState.update {
            it.copy(
                rescheduleContractId = null,
                rescheduleFromYear = null,
                rescheduleSelectedYear = null,
                rescheduleSelectedWeek = null,
                rescheduleEligibleYears = emptyList(),
                rescheduleEligibleWeeks = emptyList(),
                rescheduleFulfillByYear = null,
                rescheduleOpponentLabel = "",
            )
        }
    }

    fun selectRescheduleYear(year: Int) {
        val contractId = _uiState.value.rescheduleContractId ?: return
        val fromYear = _uiState.value.rescheduleFromYear ?: return
        val book = league?.oocContracts ?: return
        if (!_uiState.value.rescheduleEligibleYears.contains(year)) return
        val weeks = book.eligibleRescheduleWeeks(contractId, fromYear, year)
        val preferred = book.findById(contractId)?.gameForYear(fromYear)?.preferredWeek ?: -1
        val selectedWeek = when {
            preferred >= 0 && weeks.contains(preferred) -> preferred
            weeks.isNotEmpty() -> weeks.first()
            else -> null
        }
        _uiState.update {
            it.copy(
                rescheduleSelectedYear = year,
                rescheduleEligibleWeeks = weeks,
                rescheduleSelectedWeek = selectedWeek,
            )
        }
    }

    fun selectRescheduleWeek(week: Int) {
        if (!_uiState.value.rescheduleEligibleWeeks.contains(week)) return
        _uiState.update { it.copy(rescheduleSelectedWeek = week) }
    }

    fun confirmReschedule() {
        val book = league?.oocContracts ?: return
        val contractId = _uiState.value.rescheduleContractId ?: return
        val fromYear = _uiState.value.rescheduleFromYear ?: return
        val toYear = _uiState.value.rescheduleSelectedYear ?: return
        val week = _uiState.value.rescheduleSelectedWeek
        if (toYear != fromYear) {
            if (!book.rescheduleYear(contractId, fromYear, toYear)) {
                _uiState.update {
                    it.copy(message = "Could not move that game to $toYear.")
                }
                return
            }
        }
        if (week != null) {
            if (!book.rescheduleWeek(contractId, toYear, week)) {
                _uiState.update {
                    it.copy(message = "Could not set week ${week + 1} (no shared open week).")
                }
                dismissRescheduleDialog()
                reload()
                return
            }
        }
        dismissRescheduleDialog()
        _uiState.update { it.copy(message = "Game date updated.") }
        reload()
    }

    fun confirmCancelContract() {
        val id = _uiState.value.cancelConfirmId ?: return
        val l = league ?: return
        val book = l.oocContracts ?: return
        val u = user ?: return
        dismissCancelConfirm()
        if (!book.cancel(id, u)) {
            _uiState.update { it.copy(message = "Cannot cancel a deal that already paid out.") }
            return
        }
        _uiState.update { it.copy(message = "Deal cancelled.") }
        reload()
    }

    fun clearScheduleWeek(week: Int) {
        if (!schedulingActive()) return
        val u = user ?: return
        val game = u.gameSchedule.getOrNull(week)
        val lockedContractId = game?.contractId
        if (lockedContractId != null) {
            openRescheduleDialog(lockedContractId, league?.year ?: return)
            return
        }
        if (OocScheduleBuilder.clearUserOocGame(u, week)) {
            reload()
        }
    }

    private fun assignOpenContract(contractId: String) {
        val l = league ?: return
        val book = l.oocContracts ?: return
        val week = _uiState.value.opponentPickerWeek
        val year = _uiState.value.dealTargetYear ?: l.year
        val contract = book.findById(contractId) ?: return
        val cg = contract.gameForYear(year) ?: return
        val home = l.findTeamAbbr(cg.homeAbbr) ?: return
        val away = l.findTeamAbbr(cg.awayAbbr) ?: return
        if (week == null) {
            openRescheduleDialog(contractId, year)
            dismissOpponentPicker()
            return
        }
        if (!OocScheduleBuilder.placeFixedHomeOocGame(home, away, week, contractId)) {
            _uiState.update {
                it.copy(message = "Could not place that contract in week ${week + 1}.")
            }
            return
        }
        cg.preferredWeek = week
        dismissOpponentPicker()
        reload()
    }

    private fun publishPicker(week: Int?, year: Int, eligible: List<Team>) {
        val u = user ?: return
        pickerEligible = eligible
        val options = buildOpponentOptions(u, year, week, eligible, null)
        val conferences = options
            .map { it.conference }
            .distinct()
            .sorted()
        _uiState.update {
            it.copy(
                opponentPickerWeek = week,
                dealTargetYear = year,
                opponentOptions = options,
                availableConferences = conferences,
                conferenceFilter = null,
                siteFilter = null,
                dealOpponentAbbr = null,
                dealQuote = "",
                singleGameAllowed = true,
                hhQuote = "",
                hhAllowed = true,
                twoForOneQuote = "",
                twoForOneAllowed = false,
                dealSite = DealSite.HOME,
                hhReturnOffset = DEFAULT_HH_RETURN_OFFSET,
            )
        }
    }

    private fun buildOpponentOptions(
        user: Team,
        year: Int,
        week: Int?,
        eligible: List<Team>,
        siteFilter: DealSite?,
    ): List<OpponentOptionUi> {
        val openRows = buildOpenContractOptions(user, year, week, siteFilter)
        val openAbbrs = openRows.map { it.abbr }.toSet()
        val teamRows = eligible
            .filter { it.abbr !in openAbbrs }
            .mapNotNull { formatOpponent(user, it, siteFilter) }
        return sortOpponentOptions(openRows + teamRows)
    }

    private fun sortOpponentOptions(options: List<OpponentOptionUi>): List<OpponentOptionUi> {
        return options.sortedWith(
            compareBy<OpponentOptionUi> { it.section.ordinal }
                .thenComparator { a, b ->
                    when (a.section) {
                        OpponentSection.OPEN_CONTRACT -> {
                            val weekA = if (a.preferredWeek >= 0) a.preferredWeek else Int.MAX_VALUE
                            val weekB = if (b.preferredWeek >= 0) b.preferredWeek else Int.MAX_VALUE
                            weekA.compareTo(weekB)
                                .takeIf { it != 0 }
                                ?: b.moneyAmount.compareTo(a.moneyAmount)
                        }
                        OpponentSection.PEER -> {
                            abs(a.tierGap).compareTo(abs(b.tierGap))
                        }
                        else -> b.moneyAmount.compareTo(a.moneyAmount)
                    }
                }
                .thenBy { it.name.lowercase() },
        )
    }

    private fun buildOpenContractOptions(
        user: Team,
        year: Int,
        week: Int?,
        siteFilter: DealSite?,
    ): List<OpponentOptionUi> {
        val l = league ?: return emptyList()
        val book = l.oocContracts ?: return emptyList()
        val placedIds = user.gameSchedule
            .mapNotNull { it?.contractId }
            .toSet()
        return book.forTeamInYear(user.abbr, year).mapNotNull { contract ->
            if (contract.id in placedIds) return@mapNotNull null
            val cg = contract.gameForYear(year) ?: return@mapNotNull null
            if (cg.settled) return@mapNotNull null
            val userIsHome = cg.homeAbbr == user.abbr
            if (siteFilter == DealSite.HOME && !userIsHome) return@mapNotNull null
            if (siteFilter == DealSite.AWAY && userIsHome) return@mapNotNull null
            val oppAbbr = if (userIsHome) cg.awayAbbr else cg.homeAbbr
            val opp = l.findTeamAbbr(oppAbbr) ?: return@mapNotNull null
            if (week != null && !opp.isOpenOocWeek(week)) return@mapNotNull null
            val money = moneyForGuarantee(userIsHome, cg.guarantee)
            val tierGap = user.programProfile.scheduleTier - opp.programProfile.scheduleTier
            OpponentOptionUi(
                abbr = opp.abbr,
                name = opp.name,
                conference = opp.conference,
                rivalryLabel = rivalryLabel(user, opp),
                moneyKind = money.first,
                moneyLabel = money.second,
                moneyAmount = cg.guarantee,
                tierGap = tierGap,
                preferredWeek = cg.preferredWeek,
                affordable = money.first != OpponentMoneyKind.PAY ||
                    user.recruitMoney >= cg.guarantee,
                matchupLabel = matchupLabelFor(tierGap),
                dealTypeChip = null,
                section = OpponentSection.OPEN_CONTRACT,
                openContractId = contract.id,
                userIsHome = userIsHome,
            )
        }
    }

    private fun formatOpponent(
        user: Team,
        t: Team,
        siteFilter: DealSite?,
    ): OpponentOptionUi? {
        val userTier = user.programProfile.scheduleTier
        val oppTier = t.programProfile.scheduleTier
        val tierGap = userTier - oppTier
        val userIsPower = tierGap > NilMoney.PEER_SERIES_TIER_GAP
        if (siteFilter == DealSite.AWAY && userIsPower) {
            return null
        }

        val section = when {
            Team.strongestRivalryBetween(user, t) > 0 -> OpponentSection.RIVALRY
            tierGap > NilMoney.PEER_SERIES_TIER_GAP -> OpponentSection.BUY
            -tierGap > NilMoney.PEER_SERIES_TIER_GAP -> OpponentSection.EARN
            else -> OpponentSection.PEER
        }

        val userIsHome = when (siteFilter) {
            DealSite.HOME -> true
            DealSite.AWAY -> false
            null -> section != OpponentSection.EARN
        }
        val home = if (userIsHome) user else t
        val away = if (userIsHome) t else user
        val fee = NilMoney.singleGameGuarantee(home.programProfile, away.programProfile)
        val money = moneyForGuarantee(userIsHome, fee)
        val dealTypeChip = when {
            section == OpponentSection.RIVALRY &&
                tierGap > NilMoney.PEER_SERIES_TIER_GAP -> "Buy game"
            section == OpponentSection.RIVALRY &&
                -tierGap > NilMoney.PEER_SERIES_TIER_GAP -> "Road payday"
            else -> null
        }
        return OpponentOptionUi(
            abbr = t.abbr,
            name = t.name,
            conference = t.conference,
            rivalryLabel = rivalryLabel(user, t),
            moneyKind = money.first,
            moneyLabel = money.second,
            moneyAmount = fee,
            tierGap = tierGap,
            preferredWeek = -1,
            affordable = money.first != OpponentMoneyKind.PAY || user.recruitMoney >= fee,
            matchupLabel = matchupLabelFor(tierGap),
            dealTypeChip = dealTypeChip,
            section = section,
            userIsHome = userIsHome,
        )
    }

    private fun matchupLabelFor(tierGap: Int): String {
        return when {
            tierGap > NilMoney.PEER_SERIES_TIER_GAP -> "Cupcake"
            tierGap < -NilMoney.PEER_SERIES_TIER_GAP -> "Tough"
            else -> "Peer"
        }
    }

    private fun rivalryLabel(user: Team, opp: Team): String? {
        val strength = Team.strongestRivalryBetween(user, opp)
        return if (strength > 0) {
            "${Rivalry.band(strength)} ($strength)"
        } else {
            null
        }
    }

    private fun defaultSiteFor(section: OpponentSection): DealSite {
        return when (section) {
            OpponentSection.EARN -> DealSite.AWAY
            else -> DealSite.HOME
        }
    }

    private data class DealQuotes(
        val single: String,
        val singleAllowed: Boolean,
        val homeAndHome: String,
        val homeAndHomeAllowed: Boolean,
        val twoForOne: String,
        val twoForOneAllowed: Boolean,
    )

    private fun ScheduleUiState.withQuotes(quotes: DealQuotes): ScheduleUiState {
        return copy(
            dealQuote = quotes.single,
            singleGameAllowed = quotes.singleAllowed,
            hhQuote = quotes.homeAndHome,
            hhAllowed = quotes.homeAndHomeAllowed,
            twoForOneQuote = quotes.twoForOne,
            twoForOneAllowed = quotes.twoForOneAllowed,
        )
    }

    private fun buildDealQuotes(oppAbbr: String, site: DealSite, offset: Int): DealQuotes {
        val empty = DealQuotes("", false, "", false, "", false)
        val u = user ?: return empty
        val l = league ?: return empty
        val opponent = l.findTeamAbbr(oppAbbr) ?: return empty
        val startYear = _uiState.value.dealTargetYear ?: l.year
        val clamped = offset.coerceIn(1, 6)
        val tierGap = u.programProfile.scheduleTier - opponent.programProfile.scheduleTier
        val userIsPower = tierGap > NilMoney.PEER_SERIES_TIER_GAP
        val userIsSoft = tierGap < -NilMoney.PEER_SERIES_TIER_GAP
        val mismatch = userIsPower || userIsSoft
        val home = if (site == DealSite.HOME) u else opponent
        val away = if (site == DealSite.HOME) opponent else u

        // A bigger program never bills a smaller host for a one-off visit; it has
        // to give up two home dates instead.
        val singleAllowed = !(userIsPower && site == DealSite.AWAY)
        val singleQuote = if (singleAllowed) {
            val fee = NilMoney.singleGameGuarantee(home.programProfile, away.programProfile)
            "$startYear @${home.name} · ${moneyLabelFor(home.abbr == u.abbr, fee)}"
        } else {
            "${opponent.name} will not host a one-off — offer a 2-for-1"
        }

        val hhAllowed = !userIsPower
        val returnYear = startYear + clamped
        val hhFirstHome = if (site == DealSite.HOME) u else opponent
        val hhSecondHome = if (site == DealSite.HOME) opponent else u
        val hhQuote = if (!hhAllowed) {
            "${opponent.name} only visits as part of a 2-for-1"
        } else {
            val firstFee = NilMoney.homeAndHomeLegFee(
                hhFirstHome.programProfile,
                hhSecondHome.programProfile,
            )
            val secondFee = NilMoney.homeAndHomeLegFee(
                hhSecondHome.programProfile,
                hhFirstHome.programProfile,
            )
            "$startYear @${hhFirstHome.name} · ${moneyLabelFor(hhFirstHome.abbr == u.abbr, firstFee)}" +
                " · $returnYear @${hhSecondHome.name} · " +
                moneyLabelFor(hhSecondHome.abbr == u.abbr, secondFee)
        }

        val twoForOneQuote = if (!mismatch) {
            "Programs are too evenly matched for a 2-for-1"
        } else {
            val power = if (tierGap > 0) u else opponent
            val soft = if (power.abbr == u.abbr) opponent else u
            val guarantee = NilMoney.buyGameGuarantee(power.programProfile, soft.programProfile)
            val powerMoney = moneyLabelFor(power.abbr == u.abbr, guarantee)
            val midYear = startYear + clamped
            val powerHostsFirst = home.abbr == power.abbr
            val legs = if (powerHostsFirst) {
                listOf(
                    "$startYear @${power.name} · $powerMoney",
                    "$midYear @${soft.name} · No fee",
                    "${midYear + 1} @${power.name} · $powerMoney",
                )
            } else {
                listOf(
                    "$startYear @${soft.name} · No fee",
                    "$midYear @${power.name} · $powerMoney",
                    "${midYear + 1} @${power.name} · $powerMoney",
                )
            }
            legs.joinToString(" · ")
        }

        return DealQuotes(
            single = singleQuote,
            singleAllowed = singleAllowed,
            homeAndHome = hhQuote,
            homeAndHomeAllowed = hhAllowed,
            twoForOne = twoForOneQuote,
            twoForOneAllowed = mismatch,
        )
    }

    private fun moneyLabelFor(userIsHome: Boolean, fee: Int): String {
        if (fee <= 0) {
            return "No fee"
        }
        return if (userIsHome) {
            "Pay ${NilMoney.format(fee)}"
        } else {
            "Earn ${NilMoney.format(fee)}"
        }
    }

    private fun moneyForGuarantee(userIsHome: Boolean, guarantee: Int): Pair<OpponentMoneyKind, String> {
        if (guarantee <= 0) {
            return OpponentMoneyKind.NONE to "No fee"
        }
        return if (userIsHome) {
            OpponentMoneyKind.PAY to "Pay ${NilMoney.format(guarantee)}"
        } else {
            OpponentMoneyKind.EARN to "Earn ${NilMoney.format(guarantee)}"
        }
    }

    private fun placeIfCurrentWeek(contractId: String, home: Team, away: Team, year: Int) {
        val l = league ?: return
        val week = _uiState.value.opponentPickerWeek
        if (week == null || year != l.year || !schedulingActive()) {
            if (year == l.year && schedulingActive()) {
                l.oocContracts?.materializeCurrentYear()
            }
            return
        }
        if (!OocScheduleBuilder.placeFixedHomeOocGame(home, away, week, contractId)) {
            l.oocContracts?.materializeCurrentYear()
        }
    }

    private fun schedulingActive(): Boolean {
        if (GameSession.needsOocScheduling()) return true
        return OffseasonSession.ready() && OffseasonSession.phase == OffseasonSession.Phase.SCHEDULE
    }

    private fun reload() {
        val u = user ?: return
        val l = league ?: return
        val notices = l.oocContracts?.consumeBreachNotices().orEmpty()
        val breachMsg = notices.firstOrNull()
        val weeks = buildScheduleWeeks(u)
        val openWeeks = weeks.filter { it.open }.map { it.week }
        val open = openWeeks.size
        val filled = weeks.count { it.isOoc && !it.open }
        val cards = buildContractCards(u)
        val active = schedulingActive()
        val yearOne = GameSession.needsOocScheduling()
        val primary = when {
            !active -> null
            yearOne -> "Done — Start Season"
            else -> "Done — Begin HS Recruiting"
        }
        val unplaced = countUnplacedOpenContracts(u, l.year)
        val committed = computeGuaranteesCommitted(u, l.year)
        val budgetAvailable = u.recruitMoney
        _uiState.update {
            it.copy(
                teamName = u.name,
                teamAbbr = u.abbr,
                year = l.year,
                schedulingActive = active,
                yearOneOoc = yearOne,
                rivalSummary = buildRivalSummary(u),
                openOocSlots = open,
                filledOocSlots = filled,
                openWeekNumbers = openWeeks,
                unplacedOpenContractCount = unplaced,
                horizonYears = (0..3).map { l.year + it },
                budgetAvailable = budgetAvailable,
                guaranteesCommitted = committed,
                budgetLabel = "Budget ${NilMoney.format(budgetAvailable)} · " +
                    "Guarantees committed ${NilMoney.format(committed)}",
                contractCards = cards,
                scheduleWeeks = weeks,
                primaryLabel = primary,
                message = breachMsg ?: it.message,
            )
        }
        applyContractFilter()
    }

    private fun countUnplacedOpenContracts(user: Team, year: Int): Int {
        val book = league?.oocContracts ?: return 0
        val placedIds = user.gameSchedule.mapNotNull { it?.contractId }.toSet()
        return book.forTeamInYear(user.abbr, year).count { contract ->
            contract.id !in placedIds &&
                contract.gameForYear(year)?.let { !it.settled } == true
        }
    }

    private fun computeGuaranteesCommitted(user: Team, year: Int): Int {
        val book = league?.oocContracts ?: return 0
        return book.forTeamInYear(user.abbr, year).sumOf { contract ->
            val cg = contract.gameForYear(year) ?: return@sumOf 0
            if (cg.settled || cg.homeAbbr != user.abbr) 0 else cg.guarantee
        }
    }

    private fun computeOocNet(user: Team, year: Int): Int {
        val book = league?.oocContracts ?: return 0
        return book.forTeamInYear(user.abbr, year).sumOf { contract ->
            val cg = contract.gameForYear(year) ?: return@sumOf 0
            if (cg.settled) return@sumOf 0
            if (cg.homeAbbr == user.abbr) -cg.guarantee else cg.guarantee
        }
    }

    private fun formatOocNetLabel(year: Int, net: Int): String {
        val formatted = when {
            net > 0 -> "+${NilMoney.format(net)}"
            net < 0 -> "−${NilMoney.format(-net)}"
            else -> NilMoney.format(0)
        }
        return "OOC net $year: $formatted"
    }

    private fun applyContractFilter() {
        val u = user
        val year = _uiState.value.selectedHorizonYear
        val cards = _uiState.value.contractCards
        val net = if (u != null) computeOocNet(u, year) else 0
        _uiState.update {
            it.copy(
                filteredContracts = cards.filter { c ->
                    c.games.any { g -> g.year == year } ||
                        (c.fulfillByYear >= year && c.games.any { g -> g.year >= year })
                },
                oocNetAmount = net,
                oocNetLabel = formatOocNetLabel(year, net),
            )
        }
    }

    private fun buildRivalSummary(u: Team): String {
        return u.rivalries
            .sortedByDescending { it.strength }
            .take(3)
            .joinToString(" · ") { "${it.opponentAbbr} ${it.strength}" }
    }

    private fun buildContractCards(u: Team): List<ContractCardUi> {
        val book = league?.oocContracts ?: return emptyList()
        val year = league?.year ?: 0
        return book.forTeam(u.abbr).mapNotNull { c ->
            if (!c.hasFutureGames(year) && c.gameForYear(year) == null) return@mapNotNull null
            val games = c.games
                .filter { !it.settled && it.year >= year }
                .map { g ->
                    val role = if (g.homeAbbr == u.abbr) "Home" else "Away"
                    val oppAbbr = if (g.homeAbbr == u.abbr) g.awayAbbr else g.homeAbbr
                    val opp = league?.findTeamAbbr(oppAbbr)
                    ContractGameChipUi(
                        year = g.year,
                        role = role,
                        opponentAbbr = oppAbbr,
                        opponentName = opp?.name,
                        guaranteeLabel = if (g.guarantee > 0) {
                            if (g.homeAbbr == u.abbr) {
                                "Pay ${NilMoney.format(g.guarantee)}"
                            } else {
                                "Earn ${NilMoney.format(g.guarantee)}"
                            }
                        } else {
                            null
                        },
                        canReschedule = book.canReschedule(c.id, g.year),
                        preferredWeek = g.preferredWeek,
                    )
                }
            if (games.isEmpty()) return@mapNotNull null
            val typeLabel = when (c.type) {
                OocContract.Type.SINGLE -> "Single"
                OocContract.Type.BUY -> "Buy"
                OocContract.Type.HOME_AND_HOME -> "H&H"
                OocContract.Type.TWO_FOR_ONE -> "2-for-1"
            }
            val status = when {
                c.mustFulfillByYear == year -> "Due this year"
                c.mustFulfillByYear == year + 1 -> "Due next year"
                c.gameForYear(year) != null -> "On this year's slate"
                else -> "Queued"
            }
            val a = league?.findTeamAbbr(c.teamA)
            val b = league?.findTeamAbbr(c.teamB)
            val fee = NilMoney.oocCancelBuyout(
                c.type,
                c.remainingGuaranteeTotal(year),
                c.lengthYears,
                c.unsettledGameCount(year),
            ).coerceAtLeast(c.buyout)
            ContractCardUi(
                id = c.id,
                typeLabel = typeLabel,
                teamAAbbr = c.teamA,
                teamAName = a?.name,
                teamBAbbr = c.teamB,
                teamBName = b?.name,
                fulfillByYear = c.mustFulfillByYear,
                buyoutLabel = NilMoney.format(fee),
                games = games,
                statusLabel = status,
                involvesCurrentYear = c.gameForYear(year) != null,
            )
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
                        isBye = true,
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
                        detail = if (schedulingActive()) {
                            "Tap to choose OOC opponent"
                        } else {
                            "Open OOC slot"
                        },
                        locked = !schedulingActive(),
                        open = schedulingActive(),
                        opponentAbbr = null,
                        isOoc = true,
                    ),
                )
                continue
            }
            val opp = if (game.homeTeam == u) game.awayTeam else game.homeTeam
            val homeGame = game.homeTeam == u
            val homeAway = if (homeGame) "vs" else "@"
            val isOoc = game.gameName == "OOC" || game.gameName == "OOC Rivalry"
                || game.gameName == "Rivalry Game OOC"
            val contractLocked = game.contractId != null
            val rivalry = game.rivalryStrength()
            var moneyLabel: String? = null
            if (contractLocked && game.contractId != null) {
                val cg = league?.oocContracts?.findById(game.contractId)
                    ?.gameForYear(league?.year ?: 0)
                if (cg != null && cg.guarantee > 0) {
                    moneyLabel = if (game.homeTeam == u) {
                        "Pay ${NilMoney.format(cg.guarantee)}"
                    } else {
                        "Earn ${NilMoney.format(cg.guarantee)}"
                    }
                } else if (contractLocked) {
                    moneyLabel = "Contract"
                }
            }
            weeks.add(
                ScheduleWeekUi(
                    week = week,
                    weekLabel = "Week ${week + 1}",
                    status = game.gameName,
                    detail = "$homeAway ${opp.name}",
                    locked = !isOoc || contractLocked || !schedulingActive(),
                    open = false,
                    opponentAbbr = opp.abbr,
                    opponentName = opp.name,
                    homeGame = homeGame,
                    contractLocked = contractLocked && isOoc,
                    contractId = if (contractLocked && isOoc) game.contractId else null,
                    rivalryLabel = if (rivalry > 0) "${Rivalry.band(rivalry)} ($rivalry)" else null,
                    moneyLabel = moneyLabel,
                    isOoc = isOoc,
                ),
            )
        }
        return weeks
    }
}
