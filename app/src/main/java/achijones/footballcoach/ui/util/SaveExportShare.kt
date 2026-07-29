package achijones.footballcoach.ui.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object SaveExportShare {
    fun shareJson(context: Context, slotIndex: Int, json: String) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "FootballCoach-Slot${slotIndex + 1}.json")
        file.writeText(json)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export save"))
    }
}
