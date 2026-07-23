package achijones.footballcoach.ui.coach

import androidx.lifecycle.ViewModel
import CFBsimPack.Formation
import CFBsimPack.Game
import CFBsimPack.GameSession
import CFBsimPack.engine.AutoSimUntil
import CFBsimPack.engine.DefenseConcept
import CFBsimPack.engine.GameSituation
import CFBsimPack.engine.OffenseConcept
import CFBsimPack.engine.Playbook
import CFBsimPack.engine.TempoCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class CoachTab {
    CALL_PLAYS,
    LOG,
    BOX_SCORE,
}

data class CoachUiState(
    val situation: GameSituation? = null,
    val selectedOffense: OffenseConcept = Playbook.defaultOffense(),
    val selectedDefense: DefenseConcept = Playbook.defaultDefense(),
    val selectedTempo: TempoCall = TempoCall.NORMAL,
    val aiCallMode: Boolean = false,
    val showPlayPicker: Boolean = false,
    /** Formation filter in the offense play picker. */
    val playPickerFormation: Formation = Formation.SHOTGUN,
    val tab: CoachTab = CoachTab.CALL_PLAYS,
    val showSimUntilMenu: Boolean = false,
    val showCoinToss: Boolean = false,
    val showTryChoice: Boolean = false,
    val finished: Boolean = false,
    val error: String? = null,
)

class CoachGameViewModel : ViewModel() {

    private val game: Game? = GameSession.getActiveCoachGame()
    private val _uiState = MutableStateFlow(CoachUiState())
    val uiState: StateFlow<CoachUiState> = _uiState.asStateFlow()

    init {
        val g = game
        if (g == null) {
            _uiState.update { it.copy(error = "No game to coach.", finished = true) }
        } else {
            if (!g.hasPlayed) g.startGame()
            refresh()
            if (!_uiState.value.showCoinToss && !_uiState.value.showTryChoice) {
                maybeSuggestInitial()
            }
        }
    }

    private fun maybeSuggestInitial() {
        val sit = _uiState.value.situation ?: return
        if (sit.awaitingCoinToss || sit.tryAwaitingChoice) return
        applySuggestion(sit.userOnOffense)
    }

    private fun refresh() {
        val g = game ?: return
        // Opponent AI try choices (kick XP / go for 2) resolve when user isn't choosing
        g.autoResolveTryIfNeeded()
        val sit = g.getSituation()
        _uiState.update { prev ->
            var offense = prev.selectedOffense
            var defense = prev.selectedDefense
            if (sit.pendingKickoff && sit.userOnOffense) {
                offense = Playbook.offenseById("kickoff") ?: offense
            }
            if (sit.tryIsTwoPoint && sit.userOnOffense) {
                val pool = Playbook.situationalOffense(g.state, false)
                if (pool.none { it.id == offense.id }) {
                    offense = pool.firstOrNull() ?: offense
                }
            }
            // Keep a valid ST package selected when defending 4th / kickoff
            if (sit.specialTeamsDown && !sit.userOnOffense && g.state != null) {
                val pool = Playbook.situationalDefense(g.state)
                if (pool.none { it.id == defense.id }) {
                    defense = pool.firstOrNull() ?: defense
                }
            }
            prev.copy(
                situation = sit,
                selectedOffense = offense,
                selectedDefense = defense,
                showCoinToss = sit.awaitingCoinToss && sit.userWonToss,
                showTryChoice = sit.userChoosesTry,
                finished = g.hasPlayed || (g.state?.gameOver == true),
            )
        }
    }

    fun confirmCoinToss(receive: Boolean, defendLeft: Boolean) {
        val g = game ?: return
        if (!g.applyTossChoice(receive, defendLeft)) return
        refresh()
        maybeSuggestInitial()
    }

    fun chooseKickXp() {
        val g = game ?: return
        if (!g.chooseKickXp()) return
        refresh()
        maybeSuggestInitial()
    }

    fun chooseGoForTwo() {
        val g = game ?: return
        if (!g.chooseGoForTwo()) return
        refresh()
        applySuggestion(true)
    }

    fun selectTab(tab: CoachTab) {
        _uiState.update { it.copy(tab = tab) }
    }

    fun selectTempo(t: TempoCall) {
        _uiState.update { it.copy(selectedTempo = t) }
    }

    fun setAiCallMode(enabled: Boolean) {
        _uiState.update { it.copy(aiCallMode = enabled) }
    }

    fun openPlayPicker() {
        val sit = _uiState.value.situation
        if (_uiState.value.showCoinToss || sit?.awaitingCoinToss == true) return
        if (_uiState.value.showTryChoice || sit?.tryAwaitingChoice == true) return
        val current = _uiState.value.selectedOffense.formation
        _uiState.update {
            it.copy(
                showPlayPicker = true,
                playPickerFormation = current,
            )
        }
    }

    fun closePlayPicker() {
        _uiState.update { it.copy(showPlayPicker = false) }
    }

    fun setPlayPickerFormation(formation: Formation) {
        _uiState.update { it.copy(playPickerFormation = formation) }
    }

    fun selectOffenseConcept(concept: OffenseConcept) {
        _uiState.update {
            it.copy(
                selectedOffense = concept,
                playPickerFormation = concept.formation,
                showPlayPicker = false,
                aiCallMode = false,
            )
        }
    }

    fun selectDefenseConcept(concept: DefenseConcept) {
        _uiState.update {
            it.copy(
                selectedDefense = concept,
                showPlayPicker = false,
                aiCallMode = false,
            )
        }
    }

    fun showSimUntilMenu(show: Boolean) {
        _uiState.update { it.copy(showSimUntilMenu = show) }
    }

    fun applySuggestion(forOffense: Boolean = _uiState.value.situation?.userOnOffense == true) {
        val g = game ?: return
        val state = g.state ?: return
        val offense = if (state.possessionHome) g.homeTeam else g.awayTeam
        val defense = if (state.possessionHome) g.awayTeam else g.homeTeam
        val ai = g.aiCaller
        if (forOffense) {
            val concept = ai.suggestOffense(offense, defense, state)
            _uiState.update {
                it.copy(
                    selectedOffense = concept,
                    playPickerFormation = concept.formation,
                    selectedTempo = TempoCall.NORMAL,
                    aiCallMode = false,
                )
            }
        } else {
            val offHint = if (state.pendingTry && state.tryIsTwoPoint) {
                ai.suggestOffense(offense, defense, state)
            } else {
                null
            }
            val concept = ai.suggestDefense(defense, state, offHint)
            _uiState.update { it.copy(selectedDefense = concept, aiCallMode = false) }
        }
    }

    fun simPlay() {
        val g = game ?: return
        if (g.hasPlayed || g.state == null) return
        if (g.state.awaitingCoinToss) return
        if (g.state.pendingTry && g.state.tryAwaitingChoice) return
        val state = g.state
        val sit = g.getSituation()
        val s = _uiState.value
        val ai = g.aiCaller

        val call = if (s.aiCallMode) {
            g.buildMatchedCall(null, null, s.selectedTempo)
        } else if (state.pendingKickoff) {
            if (sit.userOnOffense) {
                g.buildMatchedCall(Playbook.offenseById("kickoff"), null, s.selectedTempo)
            } else {
                g.buildMatchedCall(Playbook.offenseById("kickoff"), s.selectedDefense, s.selectedTempo)
            }
        } else if (sit.userOnOffense) {
            // User offense matched vs AI defense (aware of the called concept)
            val def = ai.suggestDefense(
                if (state.possessionHome) g.awayTeam else g.homeTeam,
                state,
                s.selectedOffense,
            )
            g.buildMatchedCall(s.selectedOffense, def, s.selectedTempo)
        } else {
            g.buildMatchedCall(null, s.selectedDefense, s.selectedTempo)
        }

        g.executeSnap(call)
        if (g.state?.gameOver == true && !g.hasPlayed) g.finalizeGame()
        refresh()
        if (s.aiCallMode) {
            val next = g.getSituation()
            if (!next.gameOver && !next.userChoosesTry) {
                applySuggestion(next.userOnOffense)
                _uiState.update { it.copy(aiCallMode = true) }
            }
        }
    }

    fun callTimeout() {
        val g = game ?: return
        val userIsHome = g.homeTeam.userControlled
        g.callTimeout(userIsHome)
        refresh()
    }

    fun autoSim(until: AutoSimUntil) {
        val g = game ?: return
        if (g.state?.awaitingCoinToss == true) return
        if (g.state?.pendingTry == true && g.state?.tryAwaitingChoice == true
            && g.getSituation().userChoosesTry
        ) {
            return
        }
        g.autoSimUntil(until)
        if (g.state?.gameOver == true && !g.hasPlayed) g.finalizeGame()
        refresh()
        _uiState.update { it.copy(showSimUntilMenu = false) }
    }

    fun finishAndClose() {
        val g = game ?: return
        if (!g.hasPlayed && g.state != null) {
            if (!g.state.gameOver) g.autoSimUntil(AutoSimUntil.GAME)
            if (!g.hasPlayed) g.finalizeGame()
        }
        GameSession.clearActiveCoachGame()
        _uiState.update { it.copy(finished = true) }
    }
}
