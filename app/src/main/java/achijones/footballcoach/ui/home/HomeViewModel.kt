package achijones.footballcoach.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import CFBsimPack.GameSession
import CFBsimPack.League
import achijones.footballcoach.R
import achijones.footballcoach.ui.theme.UserBrandTheme
import achijones.footballcoach.ui.util.AssetReader
import achijones.footballcoach.ui.util.SaveSlots
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeUiState(
    val showLoadDialog: Boolean = false,
    val saveSlotInfos: List<String> = emptyList(),
    val loading: Boolean = false,
    val errorMessage: String? = null,
    val navigateToMain: Boolean = false,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun openLoadDialog() {
        val infos = SaveSlots.infos(getApplication())
        _uiState.update {
            it.copy(showLoadDialog = true, saveSlotInfos = infos, errorMessage = null)
        }
    }

    fun dismissLoadDialog() {
        _uiState.update { it.copy(showLoadDialog = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun consumeNavigateToMain() {
        _uiState.update { it.copy(navigateToMain = false) }
    }

    fun startNewLeague() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(loading = true, errorMessage = null)
            }
            try {
                withContext(Dispatchers.Default) {
                    val app = getApplication<Application>()
                    val teamsCsv = AssetReader.read(app, "fbs_2026.csv")
                    val league = League(
                        app.getString(R.string.league_player_names),
                        app.getString(R.string.league_last_names),
                        teamsCsv,
                    )
                    GameSession.setLeague(league)
                    GameSession.clearOffseason()
                    GameSession.setNeedsTeamPicker(true)
                    GameSession.setActiveSaveSlot(null)
                }
                UserBrandTheme.clear()
                _uiState.update { it.copy(loading = false, navigateToMain = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(loading = false, errorMessage = e.message ?: "Failed to create league")
                }
            }
        }
    }

    fun loadSlot(index: Int) {
        val infos = _uiState.value.saveSlotInfos
        if (index < 0 || index >= infos.size || infos[index] == "EMPTY") {
            _uiState.update { it.copy(errorMessage = "Cannot load empty file!") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, showLoadDialog = false, errorMessage = null) }
            try {
                val loadedUser = withContext(Dispatchers.Default) {
                    val app = getApplication<Application>()
                    val file = SaveSlots.file(app, index)
                    val league = League(
                        file,
                        app.getString(R.string.league_player_names),
                        app.getString(R.string.league_last_names),
                    )
                    GameSession.setLeague(league)
                    GameSession.setNeedsTeamPicker(false)
                    GameSession.setActiveSaveSlot(index)
                    // Mid-offseason restore already calls OffseasonSession.begin inside League.
                    league.userTeam
                }
                UserBrandTheme.setFrom(loadedUser)
                _uiState.update { it.copy(loading = false, navigateToMain = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(loading = false, errorMessage = e.message ?: "Failed to load save")
                }
            }
        }
    }
}
