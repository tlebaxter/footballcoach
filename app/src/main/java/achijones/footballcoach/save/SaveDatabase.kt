package achijones.footballcoach.save

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SaveSlotEntity::class, SessionMetaEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class SaveDatabase : RoomDatabase() {
    abstract fun saveSlotDao(): SaveSlotDao

    companion object {
        @Volatile
        private var instance: SaveDatabase? = null

        fun get(context: Context): SaveDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SaveDatabase::class.java,
                    "careers.db",
                ).build().also { instance = it }
            }
        }

        /** Test helper. */
        fun createInMemory(context: Context): SaveDatabase {
            return Room.inMemoryDatabaseBuilder(context, SaveDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        }
    }
}
