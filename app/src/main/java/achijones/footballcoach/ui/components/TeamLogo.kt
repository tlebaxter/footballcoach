package achijones.footballcoach.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import java.util.concurrent.ConcurrentHashMap

data class TeamColors(
    val primary: Color,
    val secondary: Color,
)

object TeamLogoResolver {
    private const val TEAM_LOGO_DIR = "cfb_logos/teams"
    private const val CONF_LOGO_DIR = "cfb_logos/conferences"

    private val conferenceAssetStem: Map<String, String> = mapOf(
        "SEC" to "Southeastern_Conference",
        "ACC" to "Atlantic_Coast_Conference",
        "American" to "American_Athletic",
        "MAC" to "Mid-American_Conference",
        "Big Ten" to "Big_Ten",
        "Big 12" to "Big_12",
        "Conference USA" to "Conference_USA",
        "Mountain West" to "Mountain_West",
        "Pac-12" to "Pac-12",
        "Sun Belt" to "Sun_Belt",
    )

    private val bitmapCache = ConcurrentHashMap<String, ImageBitmap?>()
    private val colorCache = ConcurrentHashMap<String, TeamColors>()

    fun teamAssetPath(teamName: String): String = "$TEAM_LOGO_DIR/$teamName.png"

    fun conferenceAssetPath(conferenceName: String): String? {
        val stem = conferenceAssetStem[conferenceName] ?: return null
        return "$CONF_LOGO_DIR/$stem.png"
    }

    fun load(context: Context, assetPath: String): ImageBitmap? {
        return bitmapCache.computeIfAbsent(assetPath) {
            try {
                context.assets.open(assetPath).use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    fun loadTeam(context: Context, teamName: String): ImageBitmap? =
        load(context, teamAssetPath(teamName))

    fun loadConference(context: Context, conferenceName: String): ImageBitmap? {
        val path = conferenceAssetPath(conferenceName) ?: return null
        return load(context, path)
    }

    fun colorsForTeam(context: Context, teamName: String?, abbr: String): TeamColors {
        val key = teamName?.takeIf { it.isNotBlank() } ?: abbr
        return colorCache.computeIfAbsent(key) {
            val image = if (!teamName.isNullOrBlank()) loadTeam(context, teamName) else null
            if (image != null) {
                extractColors(image.asAndroidBitmap()) ?: fallbackColors(abbr)
            } else {
                fallbackColors(abbr)
            }
        }
    }

    private fun extractColors(bitmap: Bitmap): TeamColors? {
        val palette = Palette.from(bitmap).generate()
        val primaryInt = palette.getVibrantColor(0)
            .takeUnless { it == 0 }
            ?: palette.getDarkVibrantColor(0).takeUnless { it == 0 }
            ?: palette.getMutedColor(0).takeUnless { it == 0 }
            ?: palette.getDominantColor(0).takeUnless { it == 0 }
            ?: return null
        val secondaryInt = palette.getDarkVibrantColor(0)
            .takeUnless { it == 0 || it == primaryInt }
            ?: palette.getDarkMutedColor(0).takeUnless { it == 0 || it == primaryInt }
            ?: palette.getMutedColor(0).takeUnless { it == 0 || it == primaryInt }
            ?: darken(primaryInt)
        return TeamColors(
            primary = Color(primaryInt),
            secondary = Color(secondaryInt),
        )
    }

    private fun darken(colorInt: Int): Int {
        val r = ((colorInt shr 16) and 0xFF) * 0.45
        val g = ((colorInt shr 8) and 0xFF) * 0.45
        val b = (colorInt and 0xFF) * 0.45
        return (0xFF shl 24) or (r.toInt() shl 16) or (g.toInt() shl 8) or b.toInt()
    }

    private fun fallbackColors(abbr: String): TeamColors {
        val primary = abbrLogoColor(abbr)
        return TeamColors(primary = primary, secondary = primary.copy(alpha = 1f).darkenCompose())
    }
}

@Composable
fun rememberTeamColors(teamName: String?, abbr: String): TeamColors {
    val context = LocalContext.current
    return remember(teamName, abbr) {
        TeamLogoResolver.colorsForTeam(context, teamName, abbr)
    }
}

@Composable
fun TeamLogo(
    teamName: String?,
    abbr: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    framed: Boolean = true,
) {
    val context = LocalContext.current
    val bitmap = remember(teamName) {
        if (teamName.isNullOrBlank()) null else TeamLogoResolver.loadTeam(context, teamName)
    }
    if (bitmap != null) {
        val imageModifier = if (framed) {
            modifier
                .size(size)
                .clip(CircleShape)
                .border(1.5.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        } else {
            modifier.size(size)
        }
        Image(
            bitmap = bitmap,
            contentDescription = teamName ?: abbr,
            modifier = imageModifier,
            contentScale = ContentScale.Fit,
        )
    } else {
        AbbrLogoCircle(abbr = abbr, size = size, modifier = modifier)
    }
}

@Composable
fun ConferenceLogo(
    conferenceName: String,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
) {
    val context = LocalContext.current
    val bitmap = remember(conferenceName) {
        TeamLogoResolver.loadConference(context, conferenceName)
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = conferenceName,
            modifier = modifier.size(size),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
fun AbbrLogoCircle(
    abbr: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val logoColor = remember(abbr) { abbrLogoColor(abbr) }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(logoColor)
            .border(1.5.dp, Color.White.copy(alpha = 0.25f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = abbr.take(4),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

private fun abbrLogoColor(abbr: String): Color {
    var hash = 0
    for (ch in abbr.uppercase()) {
        hash = 31 * hash + ch.code
    }
    val hue = ((hash % 360) + 360) % 360
    return Color.hsl(hue.toFloat(), 0.55f, 0.38f)
}

private fun Color.darkenCompose(): Color {
    return Color(
        red = red * 0.45f,
        green = green * 0.45f,
        blue = blue * 0.45f,
        alpha = 1f,
    )
}
