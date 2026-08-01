package achijones.footballcoach.save

import CFBsimPack.League
import achijones.footballcoach.ui.util.SeedAssets
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class SaveRepository(
    private val context: Context,
    private val db: SaveDatabase = SaveDatabase.get(context),
) {
    private val dao = db.saveSlotDao()
    private val migrateMutex = Mutex()

    @Volatile
    private var migrated = false

    private fun nameCsvPair(): Pair<String, String> {
        val pools = SeedAssets.namePools(context)
        return pools.first.joinToString(",") to pools.last.joinToString(",")
    }

    suspend fun ensureMigrated() = withContext(Dispatchers.IO) {
        migrateMutex.withLock {
            if (migrated) return@withLock
            ensureSlotRows()
            migrateLegacyCfbFiles()
            migrated = true
        }
    }

    suspend fun listSlots(): List<SlotInfo> = withContext(Dispatchers.IO) {
        ensureMigrated()
        dao.listSlots().map { it.toSlotInfo() }
    }

    suspend fun save(slotIndex: Int, league: League, setLastActive: Boolean = true): Result<Unit> =
        withContext(Dispatchers.IO) {
            ensureMigrated()
            if (slotIndex !in 0 until SLOT_COUNT) {
                return@withContext Result.failure(IllegalArgumentException("Bad slot $slotIndex"))
            }
            val previous = dao.getSlot(slotIndex)
            try {
                val document = CareerSaveMapper.fromLeague(league)
                dao.writeSlotTransactional(
                    SaveSlotEntity(
                        slotIndex = slotIndex,
                        status = SlotStatus.OK.name,
                        summary = document.summary,
                        saveVersion = document.saveVersion,
                        updatedAtMillis = System.currentTimeMillis(),
                        payloadJson = CareerSaveMapper.encodeForStorage(document),
                    ),
                    setLastActive = setLastActive,
                )
                Result.success(Unit)
            } catch (e: Exception) {
                // Leave prior OK payload intact (transactional upsert only on success path).
                if (previous != null && previous.status == SlotStatus.OK.name) {
                    // nothing — failed before/during upsert; Room transaction rolls back
                }
                Result.failure(e)
            }
        }

    suspend fun load(slotIndex: Int): League {
        val (names, lastNames) = nameCsvPair()
        return load(slotIndex, names, lastNames)
    }

    suspend fun load(slotIndex: Int, namesCSV: String, lastNamesCSV: String): League =
        withContext(Dispatchers.IO) {
            ensureMigrated()
            val entity = dao.getSlot(slotIndex)
                ?: throw CorruptSaveException("Slot $slotIndex is empty")
            when (entity.status) {
                SlotStatus.EMPTY.name -> throw CorruptSaveException("Slot $slotIndex is empty")
                SlotStatus.CORRUPT.name -> throw CorruptSaveException(
                    entity.summary.ifBlank { "Save damaged" },
                )
                SlotStatus.INCOMPATIBLE.name -> throw IncompatibleSaveException(
                    "Incompatible save in slot ${slotIndex + 1}",
                )
            }
            val payload = entity.payloadJson
                ?: throw CorruptSaveException("Slot $slotIndex has no payload")
            try {
                val priorVersion = entity.saveVersion
                val document = CareerSaveMapper.decode(payload, namesCSV, lastNamesCSV)
                val league = CareerSaveMapper.toLeague(document, namesCSV, lastNamesCSV)
                if (priorVersion < CURRENT_SAVE_VERSION || !SaveCompression.isPacked(payload)) {
                    dao.writeSlotTransactional(
                        SaveSlotEntity(
                            slotIndex = slotIndex,
                            status = SlotStatus.OK.name,
                            summary = document.summary,
                            saveVersion = document.saveVersion,
                            updatedAtMillis = System.currentTimeMillis(),
                            payloadJson = CareerSaveMapper.encodeForStorage(document),
                        ),
                        setLastActive = true,
                    )
                } else {
                    dao.upsertSessionMeta(SessionMetaEntity(0, slotIndex))
                }
                league
            } catch (e: IncompatibleSaveException) {
                // Keep payload so the user can export; mark for UI only.
                dao.upsert(
                    entity.copy(
                        status = SlotStatus.INCOMPATIBLE.name,
                        summary = e.message ?: "Incompatible save",
                        updatedAtMillis = System.currentTimeMillis(),
                        payloadJson = entity.payloadJson,
                    ),
                )
                throw e
            } catch (e: Exception) {
                // Do not brand OK slots CORRUPT on mapper/app bugs — payload stays exportable.
                throw CorruptSaveException(e.message ?: "Save damaged", e)
            }
        }

    suspend fun delete(slotIndex: Int) = withContext(Dispatchers.IO) {
        ensureMigrated()
        dao.upsert(
            SaveSlotEntity(
                slotIndex = slotIndex,
                status = SlotStatus.EMPTY.name,
                summary = "",
                saveVersion = 0,
                updatedAtMillis = System.currentTimeMillis(),
                payloadJson = null,
            ),
        )
        val meta = dao.getSessionMeta()
        if (meta?.lastActiveSlotIndex == slotIndex) {
            dao.upsertSessionMeta(SessionMetaEntity(0, null))
        }
    }

    suspend fun exportJson(slotIndex: Int): String = withContext(Dispatchers.IO) {
        ensureMigrated()
        val entity = dao.getSlot(slotIndex)
            ?: throw CorruptSaveException("Slot empty")
        val payload = entity.payloadJson
        if (payload.isNullOrBlank()) {
            throw CorruptSaveException("Slot not exportable")
        }
        // Pretty-print when possible (migrates older versions → v13). Fall back to raw
        // bytes so load bugs never strand a recoverable export.
        try {
            val (names, lastNames) = nameCsvPair()
            val doc = CareerSaveMapper.decode(payload, names, lastNames)
            CareerSaveMapper.encodePretty(doc)
        } catch (_: Exception) {
            if (SaveCompression.isPacked(payload)) {
                try {
                    SaveCompression.unpack(payload)
                } catch (_: Exception) {
                    payload
                }
            } else {
                payload
            }
        }
    }

    suspend fun importJson(slotIndex: Int, jsonText: String) = withContext(Dispatchers.IO) {
        ensureMigrated()
        if (slotIndex !in 0 until SLOT_COUNT) {
            throw IllegalArgumentException("Bad slot")
        }
        val document = try {
            val (names, lastNames) = nameCsvPair()
            CareerSaveMapper.decode(jsonText, names, lastNames)
        } catch (e: IncompatibleSaveException) {
            throw e
        } catch (e: Exception) {
            throw CorruptSaveException("Invalid JSON save: ${e.message}", e)
        }
        dao.writeSlotTransactional(
            SaveSlotEntity(
                slotIndex = slotIndex,
                status = SlotStatus.OK.name,
                summary = document.summary,
                saveVersion = document.saveVersion,
                updatedAtMillis = System.currentTimeMillis(),
                payloadJson = CareerSaveMapper.encodeForStorage(document),
            ),
            setLastActive = false,
        )
    }

    suspend fun getLastActiveSlot(): Int? = withContext(Dispatchers.IO) {
        ensureMigrated()
        dao.getSessionMeta()?.lastActiveSlotIndex
    }

    suspend fun setLastActiveSlot(slotIndex: Int?) = withContext(Dispatchers.IO) {
        ensureMigrated()
        dao.upsertSessionMeta(SessionMetaEntity(0, slotIndex))
    }

    suspend fun findFirstEmptySlot(): Int? = withContext(Dispatchers.IO) {
        ensureMigrated()
        dao.listSlots().firstOrNull { it.status == SlotStatus.EMPTY.name }?.slotIndex
    }

    private suspend fun ensureSlotRows() {
        val existing = dao.listSlots().associateBy { it.slotIndex }
        val now = System.currentTimeMillis()
        for (i in 0 until SLOT_COUNT) {
            if (!existing.containsKey(i)) {
                dao.upsert(
                    SaveSlotEntity(
                        slotIndex = i,
                        status = SlotStatus.EMPTY.name,
                        summary = "",
                        saveVersion = 0,
                        updatedAtMillis = now,
                        payloadJson = null,
                    ),
                )
            }
        }
        if (dao.getSessionMeta() == null) {
            dao.upsertSessionMeta(SessionMetaEntity(0, null))
        }
    }

    private suspend fun migrateLegacyCfbFiles() {
        for (i in 0 until SLOT_COUNT) {
            val file = File(context.filesDir, "saveFile$i.cfb")
            if (!file.exists()) continue
            val unsupported = File(context.filesDir, "saveFile$i.cfb.unsupported")
            if (!file.renameTo(unsupported)) {
                file.delete()
            }
        }
    }

    private fun SaveSlotEntity.toSlotInfo(): SlotInfo {
        val st = try {
            SlotStatus.valueOf(status)
        } catch (_: Exception) {
            SlotStatus.CORRUPT
        }
        val display = when (st) {
            SlotStatus.EMPTY -> "EMPTY"
            SlotStatus.OK -> summary.ifBlank { "Saved career" }
            SlotStatus.CORRUPT -> summary.ifBlank { "CORRUPT — cannot load" }
            SlotStatus.INCOMPATIBLE -> summary.ifBlank { "Incompatible save" }
        }
        return SlotInfo(index = slotIndex, status = st, summary = display, saveVersion = saveVersion)
    }

    companion object {
        @Volatile
        private var instance: SaveRepository? = null

        fun get(context: Context): SaveRepository {
            return instance ?: synchronized(this) {
                instance ?: SaveRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
