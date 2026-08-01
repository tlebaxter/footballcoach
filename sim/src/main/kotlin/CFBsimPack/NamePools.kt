package CFBsimPack

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class NamePoolsFile(
    val first: List<String> = emptyList(),
    val last: List<String> = emptyList(),
)

object NamePools {
    private val json = Json { ignoreUnknownKeys = true }

    @JvmStatic
    fun parseJson(text: String): NamePoolsFile {
        if (text.isBlank()) {
            throw IllegalArgumentException("Name pools JSON is empty.")
        }
        return json.decodeFromString(NamePoolsFile.serializer(), text)
    }

    /** Split a legacy comma-separated name list (tests / migration). */
    @JvmStatic
    fun splitCsv(csv: String): ArrayList<String> {
        val out = ArrayList<String>()
        for (part in csv.split(",")) {
            val trimmed = part.trim()
            if (trimmed.isNotEmpty()) {
                out.add(trimmed)
            }
        }
        return out
    }
}
