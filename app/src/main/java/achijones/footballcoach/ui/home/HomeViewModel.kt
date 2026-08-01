package achijones.footballcoach.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import CFBsimPack.GameSession
import CFBsimPack.League
import achijones.footballcoach.save.CareerSessionRestorer
import achijones.footballcoach.save.SaveRepository
import achijones.footballcoach.save.SlotInfo
import achijones.footballcoach.save.SlotStatus
import achijones.footballcoach.ui.theme.UserBrandTheme
import achijones.footballcoach.ui.util.SaveSlots
import achijones.footballcoach.ui.util.SeedAssets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeUiState(
    val showLoadDialog: Boolean = false,
    val saveSlots: List<SlotInfo> = emptyList(),
    val resumeSlot: SlotInfo? = null,
    val loading: Boolean = false,
    val errorMessage: String? = null,
    val navigateToMain: Boolean = false,
    val confirmDeleteSlot: Int? = null,
    val showImportPicker: Boolean = false,
    val importTargetSlot: Int? = null,
    val confirmImportOverwrite: Boolean = false,
    val pendingImportJson: String? = null,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = SaveRepository.get(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refreshResume()
    }

    fun refreshResume() {
        viewModelScope.launch {
            val last = repo.getLastActiveSlot()
            val slots = repo.listSlots()
            val resume = last?.let { slots.getOrNull(it) }?.takeIf { it.status == SlotStatus.OK }
            _uiState.update { it.copy(resumeSlot = resume, saveSlots = slots) }
        }
    }

    fun resumeCareer() {
        val slot = _uiState.value.resumeSlot ?: return
        loadSlot(slot.index)
    }

    fun openLoadDialog() {
        viewModelScope.launch {
            val slots = repo.listSlots()
            _uiState.update {
                it.copy(showLoadDialog = true, saveSlots = slots, errorMessage = null)
            }
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

    fun requestDeleteSlot(index: Int) {
        _uiState.update { it.copy(confirmDeleteSlot = index) }
    }

    fun dismissDeleteConfirm() {
        _uiState.update { it.copy(confirmDeleteSlot = null) }
    }

    fun confirmDeleteSlot() {
        val index = _uiState.value.confirmDeleteSlot ?: return
        viewModelScope.launch {
            repo.delete(index)
            val slots = repo.listSlots()
            val last = repo.getLastActiveSlot()
            val resume = last?.let { slots.getOrNull(it) }?.takeIf { it.status == SlotStatus.OK }
            _uiState.update {
                it.copy(
                    confirmDeleteSlot = null,
                    saveSlots = slots,
                    resumeSlot = resume,
                )
            }
        }
    }

    fun startNewLeague() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(loading = true, errorMessage = null)
            }
            try {
                withContext(Dispatchers.Default) {
                    val app = getApplication<Application>()
                    val names = SeedAssets.namePools(app)
                    val league = League(
                        names.first,
                        names.last,
                        SeedAssets.teamsJson(app),
                    )
                    GameSession.setLeague(league)
                    GameSession.clearOffseason()
                    GameSession.setNeedsTeamPicker(true)
                    GameSession.setActiveSaveSlot(null)
                }
                repo.setLastActiveSlot(null)
                UserBrandTheme.clear()
                _uiState.update { it.copy(loading = false, navigateToMain = true, resumeSlot = null) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(loading = false, errorMessage = e.message ?: "Failed to create league")
                }
            }
        }
    }

    fun loadSlot(index: Int) {
        val infos = _uiState.value.saveSlots.ifEmpty { null }
        viewModelScope.launch {
            val slots = infos ?: repo.listSlots()
            val info = slots.getOrNull(index)
            if (info == null || !SaveSlots.canLoad(info)) {
                val msg = when (info?.status) {
                    SlotStatus.EMPTY, null -> "Cannot load empty file!"
                    SlotStatus.CORRUPT -> "Save damaged — cannot load"
                    SlotStatus.INCOMPATIBLE -> "Incompatible save — start a new career"
                    else -> "Cannot load this slot"
                }
                _uiState.update { it.copy(errorMessage = msg, saveSlots = slots) }
                return@launch
            }
            _uiState.update { it.copy(loading = true, showLoadDialog = false, errorMessage = null) }
            try {
                val loadedUser = withContext(Dispatchers.IO) {
                    val app = getApplication<Application>()
                    val league = repo.load(index)
                    CareerSessionRestorer.applyLoadedLeague(league, index)
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

    fun beginImportToSlot(index: Int) {
        _uiState.update {
            it.copy(importTargetSlot = index, showImportPicker = true, showLoadDialog = false)
        }
    }

    fun dismissImport() {
        _uiState.update {
            it.copy(
                showImportPicker = false,
                importTargetSlot = null,
                confirmImportOverwrite = false,
                pendingImportJson = null,
            )
        }
    }

    fun onImportJsonRead(json: String) {
        val slot = _uiState.value.importTargetSlot ?: return
        viewModelScope.launch {
            val slots = repo.listSlots()
            val occupied = slots.getOrNull(slot)?.status == SlotStatus.OK
            if (occupied) {
                _uiState.update {
                    it.copy(
                        pendingImportJson = json,
                        confirmImportOverwrite = true,
                        showImportPicker = false,
                    )
                }
            } else {
                performImport(slot, json)
            }
        }
    }

    fun confirmImportOverwrite() {
        val slot = _uiState.value.importTargetSlot ?: return
        val json = _uiState.value.pendingImportJson ?: return
        viewModelScope.launch { performImport(slot, json) }
    }

    private suspend fun performImport(slot: Int, json: String) {
        try {
            repo.importJson(slot, json)
            val slots = repo.listSlots()
            _uiState.update {
                it.copy(
                    showImportPicker = false,
                    importTargetSlot = null,
                    confirmImportOverwrite = false,
                    pendingImportJson = null,
                    saveSlots = slots,
                    errorMessage = null,
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    confirmImportOverwrite = false,
                    pendingImportJson = null,
                    errorMessage = e.message ?: "Import failed",
                )
            }
        }
    }

    fun exportSlot(index: Int, onJson: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val json = repo.exportJson(index)
                onJson(json)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Export failed")
                }
            }
        }
    }
}
