package achijones.footballcoach.save

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/** gzip+Base64 packing for Room TEXT payloads (`gz1:` prefix). */
object SaveCompression {
    const val PREFIX = "gz1:"

    fun pack(plain: String): String {
        val bytes = plain.toByteArray(Charsets.UTF_8)
        val baos = ByteArrayOutputStream()
        GZIPOutputStream(baos).use { it.write(bytes) }
        return PREFIX + Base64.getEncoder().encodeToString(baos.toByteArray())
    }

    fun unpack(packed: String): String {
        if (!packed.startsWith(PREFIX)) {
            return packed
        }
        val raw = Base64.getDecoder().decode(packed.substring(PREFIX.length))
        return GZIPInputStream(ByteArrayInputStream(raw)).use { stream ->
            stream.readBytes().toString(Charsets.UTF_8)
        }
    }

    fun isPacked(text: String): Boolean = text.startsWith(PREFIX)
}
