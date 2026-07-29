package achijones.footballcoach.ui.util

import achijones.footballcoach.save.SlotInfo
import achijones.footballcoach.save.SlotStatus

/** Display helpers for save-slot UI. Persistence lives in [achijones.footballcoach.save.SaveRepository]. */
object SaveSlots {
    const val SLOT_COUNT: Int = achijones.footballcoach.save.SLOT_COUNT

    fun label(info: SlotInfo): String {
        val n = info.index + 1
        return when (info.status) {
            SlotStatus.EMPTY -> "Slot $n — EMPTY"
            SlotStatus.OK -> "$n. ${info.summary}"
            SlotStatus.CORRUPT -> "$n. CORRUPT — cannot load"
            SlotStatus.INCOMPATIBLE -> "$n. Incompatible save"
        }
    }

    fun canLoad(info: SlotInfo): Boolean = info.status == SlotStatus.OK

    fun isEmpty(info: SlotInfo): Boolean = info.status == SlotStatus.EMPTY
}
