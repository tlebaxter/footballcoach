package achijones.footballcoach.ui.util

import android.content.Context
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

object SaveSlots {
    const val SLOT_COUNT = 10

    fun infos(context: Context): List<String> {
        val infos = ArrayList<String>(SLOT_COUNT)
        for (i in 0 until SLOT_COUNT) {
            val saveFile = File(context.filesDir, "saveFile$i.cfb")
            if (saveFile.exists()) {
                try {
                    BufferedReader(FileReader(saveFile)).use { reader ->
                        val line = reader.readLine()
                        infos.add(
                            if (line != null && line.isNotEmpty()) {
                                line.substring(0, line.length - 1)
                            } else {
                                "EMPTY"
                            }
                        )
                    }
                } catch (_: Exception) {
                    infos.add("EMPTY")
                }
            } else {
                infos.add("EMPTY")
            }
        }
        return infos
    }

    fun file(context: Context, index: Int): File =
        File(context.filesDir, "saveFile$index.cfb")
}
