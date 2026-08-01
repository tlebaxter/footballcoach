package achijones.footballcoach.ui.util

import CFBsimPack.NamePools
import CFBsimPack.NamePoolsFile
import android.content.Context

/** Loads FBS seed + name pools from Android assets (backed by :sim resources). */
object SeedAssets {
    fun teamsJson(context: Context): String = AssetReader.read(context, "fbs_2026.json")

    fun namePools(context: Context): NamePoolsFile {
        val text = AssetReader.read(context, "names.json")
        return NamePools.parseJson(text)
    }
}
