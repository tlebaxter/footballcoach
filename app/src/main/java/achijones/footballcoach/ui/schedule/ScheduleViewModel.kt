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
import CFBsimPack.RivalryDynamics
import CFBsimPack.Team
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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
    val buyHint: String,
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
    val selectedHorizonYear: Int = 0,
    val horizonYears: List<Int> = emptyList(),
    val contractCards: List<ContractCardUi> = emptyList(),
    val filteredContracts: List<ContractCardUi> = emptyList(),
    val scheduleWeeks: List<ScheduleWeekUi> = emptyList(),
    val opponentPickerWeek: Int? = null,
    /** When non-null and week is null, signing future deals for this year only. */
    val dealTargetYear: Int? = null,
    val opponentOptions: List<OpponentOptionUi> = emptyList(),
    val dealOpponentAbbr: String? = null,
    val dealQuote: String = "",
    val hhReturnOffset: Int = 1,
    val cancelConfirmId: String? = null,
    val cancelConfirmLabel: String? = null,
    val primaryLabel: String? = null,
    val message: String? = null,
    val navigateToMain: Boolean = false,
)

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    private var league: League? = null
    private var user: Team? = null

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

    fun requestBack() {
        if (schedulingActive()) {
            _uiState.update {
                it.copy(message = "Finish the OOC slate (or fill open weeks) before leaving.")
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
        if (existing?.contractId != null) {
            _uiState.update {
                it.copy(message = "That week is locked by an OOC contract. Cancel the deal to change it.")
            }
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
        _uiState.update {
            it.copy(
                opponentPickerWeek = week,
                dealTargetYear = l.year,
                opponentOptions = eligible.map { t -> formatOpponent(u, t) },
                dealOpponentAbbr = null,
                dealQuote = "",
                hhReturnOffset = 1,
            )
        }
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
            .sortedByDescending { it.teamPrestige }
            .map { formatOpponent(u, it) }
        _uiState.update {
            it.copy(
                opponentPickerWeek = null,
                dealTargetYear = year,
                opponentOptions = options,
                dealOpponentAbbr = null,
                dealQuote = "",
                hhReturnOffset = 1,
            )
        }
    }

    fun dismissOpponentPicker() {
        _uiState.update {
            it.copy(
                opponentPickerWeek = null,
                dealTargetYear = null,
                opponentOptions = emptyList(),
                dealOpponentAbbr = null,
                dealQuote = "",
            )
        }
    }

    fun selectDealOpponent(abbr: String) {
        val u = user ?: return
        val l = league ?: return
        val opponent = l.findTeamAbbr(abbr) ?: return
        val book = l.oocContracts ?: return
        val quote = if (u.teamPrestige >= opponent.teamPrestige) {
            book.quoteBuyGame(u, opponent) + " · Or H&H / single game."
        } else {
            book.quoteReceiveBuyGame(opponent, u) + " · Or H&H / single game."
        }
        _uiState.update { it.copy(dealOpponentAbbr = opponent.abbr, dealQuote = quote) }
    }

    fun setHhReturnOffset(offset: Int) {
        _uiState.update { it.copy(hhReturnOffset = offset.coerceIn(1, 6)) }
    }

    fun signSingleGame(withGuarantee: Boolean) {
        val u = user ?: return
        val l = league ?: return
        val book = l.oocContracts ?: return
        val year = _uiState.value.dealTargetYear ?: l.year
        val oppAbbr = _uiState.value.dealOpponentAbbr ?: return
        val opponent = l.findTeamAbbr(oppAbbr) ?: return
        val home: Team
        val away: Team
        if (withGuarantee) {
            if (u.teamPrestige >= opponent.teamPrestige) {
                home = u
                away = opponent
            } else {
                home = opponent
                away = u
            }
        } else {
            home = u
            away = opponent
        }
        val contract = book.signSingleGame(home, away, year, withGuarantee) ?: run {
            _uiState.update { it.copy(message = "Could not sign single-game deal.") }
            return
        }
        placeIfCurrentWeek(contract.id, home, away, year)
        dismissOpponentPicker()
        reload()
    }

    fun signBuyGameYears(years: Int) {
        val u = user ?: return
        val l = league ?: return
        val book = l.oocContracts ?: return
        val year = _uiState.value.dealTargetYear ?: l.year
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
        val contract = book.signBuyGame(home, away, year, years) ?: run {
            _uiState.update {
                it.copy(message = "Could not sign buy game (affordability or conflict).")
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
        val returnYear = startYear + _uiState.value.hhReturnOffset
        val oppAbbr = _uiState.value.dealOpponentAbbr ?: return
        val opponent = l.findTeamAbbr(oppAbbr) ?: return
        val contract = book.signHomeAndHome(u, opponent, startYear, returnYear, true) ?: run {
            _uiState.update { it.copy(message = "Could not sign home-and-home.") }
            return
        }
        val cg = contract.gameForYear(startYear)
        if (cg != null) {
            val home = l.findTeamAbbr(cg.homeAbbr) ?: u
            val away = l.findTeamAbbr(cg.awayAbbr) ?: opponent
            placeIfCurrentWeek(contract.id, home, away, startYear)
        }
        dismissOpponentPicker()
        reload()
    }

    fun declareRival(opponentAbbr: String) {
        val u = user ?: return
        val opponent = league?.findTeamAbbr(opponentAbbr) ?: return
        val error = RivalryDynamics.declareRival(u, opponent)
        if (error != null) {
            _uiState.update { it.copy(message = error) }
            return
        }
        _uiState.update {
            it.copy(message = "Declared ${opponent.abbr} as a rival (${u.rivalryWith(opponent.abbr)?.displayLabel()}).")
        }
        reload()
    }

    fun requestCancelContract(contractId: String) {
        val book = league?.oocContracts ?: return
        val u = user ?: return
        val c = book.findById(contractId) ?: return
        val fee = NilMoney.oocCancelBuyout(
            c.type,
            c.remainingGuaranteeTotal(league?.year ?: 0),
            c.lengthYears,
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
        if (game?.contractId != null) {
            _uiState.update {
                it.copy(message = "Cancel the OOC contract to clear this week.")
            }
            return
        }
        if (OocScheduleBuilder.clearUserOocGame(u, week)) {
            reload()
        }
    }

    fun resuggestOocSchedule() {
        if (!schedulingActive()) return
        val u = user ?: return
        val l = league ?: return
        OocScheduleBuilder.clearAllUserOocGames(u)
        OocScheduleBuilder.suggestUserOocSchedule(u, l.teamList)
        reload()
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
        if (schedulingActive()) {
            maybeAutoSuggestOoc()
        }
        val notices = l.oocContracts?.consumeBreachNotices().orEmpty()
        val breachMsg = notices.firstOrNull()
        val weeks = buildScheduleWeeks(u)
        val open = weeks.count { it.open }
        val filled = weeks.count { it.isOoc && !it.open }
        val cards = buildContractCards(u)
        val active = schedulingActive()
        val yearOne = GameSession.needsOocScheduling()
        val primary = when {
            !active -> null
            yearOne -> "Done — Start Season"
            else -> "Done — Begin HS Recruiting"
        }
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
                horizonYears = (0..3).map { l.year + it },
                contractCards = cards,
                scheduleWeeks = weeks,
                primaryLabel = primary,
                message = breachMsg ?: it.message,
            )
        }
        applyContractFilter()
    }

    private fun applyContractFilter() {
        val year = _uiState.value.selectedHorizonYear
        val cards = _uiState.value.contractCards
        _uiState.update {
            it.copy(
                filteredContracts = cards.filter { c ->
                    c.games.any { g -> g.year == year } ||
                        (c.fulfillByYear >= year && c.games.any { g -> g.year >= year })
                },
            )
        }
    }

    private fun maybeAutoSuggestOoc() {
        val u = user ?: return
        val l = league ?: return
        if (!schedulingActive()) return
        val open = (0 until League.REGULAR_SEASON_WEEKS).count { u.isOpenOocWeek(it) }
        val anyOoc = u.gameSchedule.any { g ->
            g != null && (
                g.gameName == "OOC" || g.gameName == "OOC Rivalry"
                    || g.gameName == "Rivalry Game OOC"
                )
        }
        if (open > 0 && !anyOoc) {
            OocScheduleBuilder.suggestUserOocSchedule(u, l.teamList)
        }
    }

    private fun buildRivalSummary(u: Team): String {
        return u.rivalries
            .sortedByDescending { it.strength }
            .take(3)
            .joinToString(" · ") { "${it.opponentAbbr} ${it.strength}" }
    }

    private fun formatOpponent(user: Team, t: Team): OpponentOptionUi {
        val strength = Team.strongestRivalryBetween(user, t)
        val rivalTag = if (strength > 0) {
            "${Rivalry.band(strength)} ($strength)"
        } else {
            null
        }
        val buyHint = if (user.teamPrestige >= t.teamPrestige) {
            "Buy: you pay ${NilMoney.format(NilMoney.buyGameGuarantee(user.teamPrestige, t.teamPrestige))}"
        } else {
            "Buy: you get ${NilMoney.format(NilMoney.buyGameGuarantee(t.teamPrestige, user.teamPrestige))}"
        }
        return OpponentOptionUi(
            abbr = t.abbr,
            name = t.name,
            conference = t.conference,
            rivalryLabel = rivalTag,
            buyHint = buyHint,
        )
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
                    )
                }
            if (games.isEmpty()) return@mapNotNull null
            val typeLabel = when (c.type) {
                OocContract.Type.SINGLE -> "Single"
                OocContract.Type.BUY -> "Buy"
                OocContract.Type.HOME_AND_HOME -> "H&H"
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
                    contractLocked = contractLocked,
                    rivalryLabel = if (rivalry > 0) "${Rivalry.band(rivalry)} ($rivalry)" else null,
                    moneyLabel = moneyLabel,
                    isOoc = isOoc,
                ),
            )
        }
        return weeks
    }
}
