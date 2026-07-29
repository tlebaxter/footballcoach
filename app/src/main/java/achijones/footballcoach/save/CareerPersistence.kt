package achijones.footballcoach.save

import CFBsimPack.GameSession
import CFBsimPack.League
import android.content.Context
import kotlinx.coroutines.runBlocking

/**
 * Single entry point for writing the live career to the active Room slot.
 */
object CareerPersistence {
    suspend fun saveActive(
        league: League,
        repository: SaveRepository,
    ): Result<Unit> {
        val slot = GameSession.getActiveSaveSlot()
            ?: return Result.failure(IllegalStateException("No active save slot"))
        return repository.save(slot, league)
    }

    suspend fun saveActive(
        context: Context,
        league: League,
        repository: SaveRepository = SaveRepository.get(context),
    ): Result<Unit> = saveActive(league, repository)

    fun saveActiveBlocking(
        league: League,
        repository: SaveRepository,
    ): Boolean = runBlocking { saveActive(league, repository).isSuccess }
}
