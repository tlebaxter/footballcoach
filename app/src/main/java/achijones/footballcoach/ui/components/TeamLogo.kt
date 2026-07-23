package achijones.footballcoach.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

data class TeamColors(
    val primary: Color,
    val secondary: Color,
)

object TeamLogoResolver {
    private const val TEAM_LOGO_DIR = "cfb_logos/teams"
    private const val CONF_LOGO_DIR = "cfb_logos/conferences"
    private const val OPAQUE_ALPHA_THRESHOLD = 32
    private const val SAMPLE_STEP = 4
    private const val MONOCHROME_CONTRAST_THRESHOLD = 3.0
    private const val ANY_CONTRAST_THRESHOLD = 2.5
    private const val MONOCHROME_CHROMA_STD_MAX = 28.0
    private const val MONOCHROME_HUE_STD_MAX = 18.0

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
    private val contrastBoostCache = ConcurrentHashMap<String, Boolean>()

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

    fun needsContrastBoost(context: Context, teamName: String?, background: Color): Boolean {
        if (teamName.isNullOrBlank()) return false
        val cacheKey = "$teamName|${quantizeColor(background)}"
        return contrastBoostCache.computeIfAbsent(cacheKey) {
            val image = loadTeam(context, teamName) ?: return@computeIfAbsent false
            analyzeNeedsContrastBoost(image.asAndroidBitmap(), background)
        }
    }

    private fun analyzeNeedsContrastBoost(bitmap: Bitmap, background: Color): Boolean {
        val sample = sampleOpaquePixels(bitmap) ?: return false
        val contrast = contrastRatio(sample.dominant, background)
        if (contrast < ANY_CONTRAST_THRESHOLD) return true
        if (sample.nearMonochrome && contrast < MONOCHROME_CONTRAST_THRESHOLD) return true
        return false
    }

    private data class LogoSample(
        val dominant: Color,
        val nearMonochrome: Boolean,
    )

    private fun sampleOpaquePixels(bitmap: Bitmap): LogoSample? {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return null

        var count = 0
        var sumR = 0.0
        var sumG = 0.0
        var sumB = 0.0
        var sumChroma = 0.0
        var sumHueSin = 0.0
        var sumHueCos = 0.0
        var sumChromaSq = 0.0

        val stepX = max(1, width / 64).coerceAtLeast(SAMPLE_STEP)
        val stepY = max(1, height / 64).coerceAtLeast(SAMPLE_STEP)
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                val pixel = pixels[y * width + x]
                val alpha = (pixel ushr 24) and 0xFF
                if (alpha >= OPAQUE_ALPHA_THRESHOLD) {
                    val r = (pixel shr 16) and 0xFF
                    val g = (pixel shr 8) and 0xFF
                    val b = pixel and 0xFF
                    sumR += r
                    sumG += g
                    sumB += b
                    val maxC = max(r, max(g, b)).toDouble()
                    val minC = min(r, min(g, b)).toDouble()
                    val chroma = maxC - minC
                    sumChroma += chroma
                    sumChromaSq += chroma * chroma
                    if (chroma > 8.0) {
                        val hue = when (maxC) {
                            minC -> 0.0
                            r.toDouble() -> 60.0 * (((g - b) / chroma) % 6.0)
                            g.toDouble() -> 60.0 * (((b - r) / chroma) + 2.0)
                            else -> 60.0 * (((r - g) / chroma) + 4.0)
                        }
                        val hueRad = Math.toRadians(if (hue < 0) hue + 360.0 else hue)
                        sumHueSin += kotlin.math.sin(hueRad)
                        sumHueCos += kotlin.math.cos(hueRad)
                    }
                    count++
                }
                x += stepX
            }
            y += stepY
        }

        if (count == 0) return null

        val avgR = (sumR / count).toInt().coerceIn(0, 255)
        val avgG = (sumG / count).toInt().coerceIn(0, 255)
        val avgB = (sumB / count).toInt().coerceIn(0, 255)
        val meanChroma = sumChroma / count
        val chromaVariance = (sumChromaSq / count) - (meanChroma * meanChroma)
        val chromaStd = kotlin.math.sqrt(chromaVariance.coerceAtLeast(0.0))

        val hueMeanSin = sumHueSin / count
        val hueMeanCos = sumHueCos / count
        val hueCircularStd = Math.toDegrees(
            kotlin.math.sqrt((-2.0 * kotlin.math.ln(
                (kotlin.math.sqrt(hueMeanSin * hueMeanSin + hueMeanCos * hueMeanCos))
                    .coerceIn(1e-6, 1.0),
            )).coerceAtLeast(0.0)),
        )

        val nearMonochrome = chromaStd <= MONOCHROME_CHROMA_STD_MAX &&
            (meanChroma < 12.0 || hueCircularStd <= MONOCHROME_HUE_STD_MAX)

        return LogoSample(
            dominant = Color(avgR, avgG, avgB),
            nearMonochrome = nearMonochrome,
        )
    }

    private fun quantizeColor(color: Color): String {
        val r = (color.red * 15).toInt().coerceIn(0, 15)
        val g = (color.green * 15).toInt().coerceIn(0, 15)
        val b = (color.blue * 15).toInt().coerceIn(0, 15)
        return "$r,$g,$b"
    }

    private fun relativeLuminance(color: Color): Double {
        fun channel(c: Float): Double {
            val v = c.toDouble()
            return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(color.red) +
            0.7152 * channel(color.green) +
            0.0722 * channel(color.blue)
    }

    private fun contrastRatio(a: Color, b: Color): Double {
        val l1 = relativeLuminance(a)
        val l2 = relativeLuminance(b)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
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
fun rememberLogoNeedsContrastBoost(teamName: String?, background: Color): Boolean {
    val context = LocalContext.current
    val quantized = remember(background) {
        val r = (background.red * 15).toInt()
        val g = (background.green * 15).toInt()
        val b = (background.blue * 15).toInt()
        "$r,$g,$b"
    }
    return remember(teamName, quantized) {
        TeamLogoResolver.needsContrastBoost(context, teamName, background)
    }
}

@Composable
fun TeamLogo(
    teamName: String?,
    abbr: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    framed: Boolean = true,
    contrastBoost: Boolean = false,
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
        if (contrastBoost) {
            Box(
                modifier = imageModifier,
                contentAlignment = Alignment.Center,
            ) {
                val softGlow = ColorFilter.tint(Color.White.copy(alpha = 0.55f), BlendMode.SrcIn)
                val hardGlow = ColorFilter.tint(Color.White.copy(alpha = 0.35f), BlendMode.SrcIn)
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .size(size)
                        .graphicsLayer {
                            scaleX = 1.08f
                            scaleY = 1.08f
                            alpha = 0.45f
                        },
                    contentScale = ContentScale.Fit,
                    colorFilter = softGlow,
                )
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .size(size)
                        .offset(x = 2.dp, y = 3.dp)
                        .graphicsLayer {
                            scaleX = 1.04f
                            scaleY = 1.04f
                            alpha = 0.5f
                        },
                    contentScale = ContentScale.Fit,
                    colorFilter = hardGlow,
                )
                Image(
                    bitmap = bitmap,
                    contentDescription = teamName ?: abbr,
                    modifier = Modifier.size(size),
                    contentScale = ContentScale.Fit,
                )
            }
        } else {
            Image(
                bitmap = bitmap,
                contentDescription = teamName ?: abbr,
                modifier = imageModifier,
                contentScale = ContentScale.Fit,
            )
        }
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
