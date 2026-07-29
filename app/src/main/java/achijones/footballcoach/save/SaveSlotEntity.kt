package achijones.footballcoach.save

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "save_slots")
data class SaveSlotEntity(
    @PrimaryKey val slotIndex: Int,
    val status: String,
    val summary: String,
    val saveVersion: Int,
    val updatedAtMillis: Long,
    val payloadJson: String?,
)

@Entity(tableName = "session_meta")
data class SessionMetaEntity(
    @PrimaryKey val id: Int = 0,
    val lastActiveSlotIndex: Int? = null,
)
