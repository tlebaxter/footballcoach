package achijones.footballcoach.save

import CFBsimPack.GameSession
import CFBsimPack.League
import android.content.Context
import achijones.footballcoach.R

/**
 * Reloads the last active career into [GameSession] after process death.
 */
object CareerSessionRestorer {
    sealed class ResumeResult {
        data object Success : ResumeResult()
        data object AlreadyLoaded : ResumeResult()
        data object NoSlot : ResumeResult()
        data class Failed(val message: String) : ResumeResult()
    }

    suspend fun resumeIfNeeded(
        context: Context,
        repository: SaveRepository = SaveRepository.get(context),
    ): ResumeResult {
        if (GameSession.hasLeague()) return ResumeResult.AlreadyLoaded
        val slot = repository.getLastActiveSlot() ?: return ResumeResult.NoSlot
        val slots = repository.listSlots()
        val info = slots.getOrNull(slot) ?: return ResumeResult.NoSlot
        if (info.status == SlotStatus.EMPTY) return ResumeResult.NoSlot
        if (info.status == SlotStatus.INCOMPATIBLE) {
            return ResumeResult.Failed(info.summary.ifBlank { "Incompatible save" })
        }
        if (info.status == SlotStatus.CORRUPT) {
            return ResumeResult.Failed(info.summary.ifBlank { "Save damaged — cannot load" })
        }
        return try {
            val league = repository.load(
                slot,
                context.getString(R.string.league_player_names),
                context.getString(R.string.league_last_names),
            )
            applyLoadedLeague(league, slot)
            ResumeResult.Success
        } catch (e: Exception) {
            ResumeResult.Failed(e.message ?: "Failed to resume career")
        }
    }

    /** @return true when a league is available in [GameSession] after this call. */
    suspend fun resumeOrFalse(
        context: Context,
        repository: SaveRepository = SaveRepository.get(context),
    ): Boolean = when (resumeIfNeeded(context, repository)) {
        ResumeResult.Success, ResumeResult.AlreadyLoaded -> true
        is ResumeResult.Failed, ResumeResult.NoSlot -> false
    }

    fun applyLoadedLeague(league: League, slotIndex: Int) {
        GameSession.setLeague(league)
        GameSession.setNeedsTeamPicker(false)
        GameSession.setActiveSaveSlot(slotIndex)
        if (league.loadedInOffseason) {
            GameSession.setNeedsOocScheduling(
                CFBsimPack.OffseasonSession.ready() &&
                    CFBsimPack.OffseasonSession.phase == CFBsimPack.OffseasonSession.Phase.SCHEDULE,
            )
        }
    }
}
