package achijones.footballcoach.ui.coach

import androidx.lifecycle.ViewModel
import CFBsimPack.Formation
import CFBsimPack.Game
import CFBsimPack.GameSession
import CFBsimPack.engine.AutoSimUntil
import CFBsimPack.engine.CoverageCall
import CFBsimPack.engine.GameSituation
import CFBsimPack.engine.OffensePlay
import CFBsimPack.engine.PlayCall
import CFBsimPack.engine.TempoCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CoachUiState(
    val situation: GameSituation? = null,
    val selectedCoverage: CoverageCall = CoverageCall.COVER_3,
    val selectedTempo: TempoCall = TempoCall.NORMAL,
    val selectedFormation: Formation = Formation.SHOTGUN,
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
        }
    }

    private fun refresh() {
        val g = game ?: return
        _uiState.update {
            it.copy(
                situation = g.getSituation(),
                finished = g.hasPlayed || (g.state?.gameOver == true),
            )
        }
    }

    fun selectCoverage(c: CoverageCall) {
        _uiState.update { it.copy(selectedCoverage = c) }
    }

    fun selectTempo(t: TempoCall) {
        _uiState.update { it.copy(selectedTempo = t) }
    }

    fun selectFormation(f: Formation) {
        _uiState.update { it.copy(selectedFormation = f) }
    }

    fun callPlay(play: OffensePlay) {
        val g = game ?: return
        if (g.hasPlayed) return
        val s = _uiState.value
        g.executeSnap(PlayCall(play, s.selectedFormation, s.selectedCoverage, s.selectedTempo))
        if (g.state?.gameOver == true && !g.hasPlayed) g.finalizeGame()
        refresh()
    }

    fun callDefenseOnly() {
        // When user is on defense, offense is AI — execute with AI offense + user coverage
        val g = game ?: return
        if (g.hasPlayed || g.state == null) return
        val state = g.state
        val offense = if (state.possessionHome) g.homeTeam else g.awayTeam
        val defense = if (state.possessionHome) g.awayTeam else g.homeTeam
        val ai = CFBsimPack.engine.AiPlayCaller(java.util.Random())
        val aiCall = ai.choose(offense, defense, state)
        val userCall = PlayCall(
            aiCall.offensePlay,
            aiCall.formation,
            _uiState.value.selectedCoverage,
            aiCall.tempo,
        )
        g.executeSnap(userCall)
        if (g.state?.gameOver == true && !g.hasPlayed) g.finalizeGame()
        refresh()
    }

    fun callTimeout() {
        val g = game ?: return
        val sit = g.getSituation()
        val userIsHome = g.homeTeam.userControlled
        g.callTimeout(userIsHome)
        refresh()
    }

    fun autoSim(until: AutoSimUntil) {
        val g = game ?: return
        g.autoSimUntil(until)
        if (g.state?.gameOver == true && !g.hasPlayed) g.finalizeGame()
        refresh()
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
