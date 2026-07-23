package achijones.footballcoach.ui.util

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

object AssetReader {
    fun read(context: Context, fileName: String): String {
        return try {
            BufferedReader(InputStreamReader(context.assets.open(fileName), "UTF-8")).use { reader ->
                buildString {
                    var line = reader.readLine()
                    while (line != null) {
                        append(line)
                        append('\n')
                        line = reader.readLine()
                    }
                }
            }
        } catch (_: Exception) {
            ""
        }
    }
}
