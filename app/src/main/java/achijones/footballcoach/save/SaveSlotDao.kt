package achijones.footballcoach.save

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface SaveSlotDao {
    @Query("SELECT * FROM save_slots ORDER BY slotIndex ASC")
    suspend fun listSlots(): List<SaveSlotEntity>

    @Query("SELECT * FROM save_slots WHERE slotIndex = :index LIMIT 1")
    suspend fun getSlot(index: Int): SaveSlotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(slot: SaveSlotEntity)

    @Query("DELETE FROM save_slots WHERE slotIndex = :index")
    suspend fun deleteSlot(index: Int)

    @Query("SELECT * FROM session_meta WHERE id = 0 LIMIT 1")
    suspend fun getSessionMeta(): SessionMetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSessionMeta(meta: SessionMetaEntity)

    @Transaction
    suspend fun writeSlotTransactional(slot: SaveSlotEntity, setLastActive: Boolean) {
        upsert(slot)
        if (setLastActive) {
            upsertSessionMeta(SessionMetaEntity(id = 0, lastActiveSlotIndex = slot.slotIndex))
        }
    }
}
