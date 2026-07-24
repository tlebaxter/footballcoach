package achijones.footballcoach.ui.coach

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val FieldShape = RoundedCornerShape(12.dp)
private val FieldGreen = Color(0xFF1B5E20)
private val FieldGreenDark = Color(0xFF0F3D14)
private val FieldEndZone = Color(0xFF14532D)
private val FirstDownYellow = Color(0xFFFDE047)
private val LosWhite = Color(0xFFF8FAFC)
private val ToGoBand = Color(0x66FDE047)
private val BallColor = Color(0xFFF59E0B)
private val PossessionChipBg = Color(0xE6111820)

/**
 * Maps offense-relative yard line (0–100 toward opponent) to absolute field fraction (0 = left, 1 = right).
 */
fun offenseYardToAbsolute(
    offenseYard: Int,
    possessionHome: Boolean,
    homeDefendsLeft: Boolean,
): Float {
    val y = offenseYard.coerceIn(0, 100)
    val offenseAttacksRight = if (possessionHome) homeDefendsLeft else !homeDefendsLeft
    return if (offenseAttacksRight) y / 100f else (100 - y) / 100f
}

@Composable
fun CoachField(
    yardLine: Int,
    firstDownYard: Int,
    possessionHome: Boolean,
    homeDefendsLeft: Boolean,
    homeName: String,
    awayName: String,
    possessionAbbr: String,
    modifier: Modifier = Modifier,
) {
    val leftName = if (homeDefendsLeft) homeName else awayName
    val rightName = if (homeDefendsLeft) awayName else homeName
    val density = LocalDensity.current
    val ballFraction by animateFloatAsState(
        targetValue = offenseYardToAbsolute(yardLine, possessionHome, homeDefendsLeft),
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "ballX",
    )
    val showFirstDown = firstDownYard in 0..100
    val firstDownFraction = if (showFirstDown) {
        offenseYardToAbsolute(firstDownYard, possessionHome, homeDefendsLeft)
    } else {
        ballFraction
    }

    Box(
        modifier = modifier
            .clip(FieldShape)
            .border(1.dp, Color(0xFF2F5D34), FieldShape),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val endZoneW = w * 0.09f
            val playableW = w - endZoneW * 2f

            drawRect(
                brush = Brush.verticalGradient(listOf(FieldGreen, FieldGreenDark)),
                size = Size(w, h),
            )
            drawRect(FieldEndZone, Offset(0f, 0f), Size(endZoneW, h))
            drawRect(FieldEndZone, Offset(w - endZoneW, 0f), Size(endZoneW, h))

            // Yard lines — majors every 10; labels only at 20 / 50 / 20
            for (i in 0..10) {
                val x = endZoneW + playableW * i / 10f
                val major = i % 5 == 0
                drawLine(
                    Color.White.copy(alpha = if (major) 0.4f else 0.16f),
                    Offset(x, 0f),
                    Offset(x, h),
                    strokeWidth = if (major) 2.5f else 1.2f,
                )
                if (i == 2 || i == 5 || i == 8) {
                    val yard = if (i <= 5) i * 10 else (10 - i) * 10
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(170, 255, 255, 255)
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = with(density) { 12.sp.toPx() }
                        isFakeBoldText = true
                        isAntiAlias = true
                    }
                    drawContext.canvas.nativeCanvas.drawText(yard.toString(), x, h * 0.22f, paint)
                }
            }

            val endZonePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(200, 255, 255, 255)
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = with(density) { 11.sp.toPx() }
                isFakeBoldText = true
                isAntiAlias = true
            }
            val maxNameLen = h * 0.88f
            fun paintForName(name: String): android.graphics.Paint {
                val p = android.graphics.Paint(endZonePaint)
                val measured = p.measureText(name)
                if (measured > maxNameLen && measured > 0f) {
                    p.textSize *= maxNameLen / measured
                }
                return p
            }
            val native = drawContext.canvas.nativeCanvas
            val leftPaint = paintForName(leftName)
            native.save()
            native.rotate(90f, endZoneW / 2f, h / 2f)
            native.drawText(leftName, endZoneW / 2f, h / 2f + leftPaint.textSize * 0.35f, leftPaint)
            native.restore()
            val rightPaint = paintForName(rightName)
            native.save()
            native.rotate(90f, w - endZoneW / 2f, h / 2f)
            native.drawText(rightName, w - endZoneW / 2f, h / 2f + rightPaint.textSize * 0.35f, rightPaint)
            native.restore()

            drawRoundRect(
                color = Color.White.copy(alpha = 0.18f),
                topLeft = Offset(1f, 1f),
                size = Size(w - 2f, h - 2f),
                cornerRadius = CornerRadius(12.dp.toPx()),
                style = Stroke(width = 1.5f),
            )

            val losX = endZoneW + playableW * ballFraction
            val fdX = endZoneW + playableW * firstDownFraction

            if (showFirstDown && kotlin.math.abs(fdX - losX) > 2f) {
                val left = minOf(losX, fdX)
                val width = kotlin.math.abs(fdX - losX)
                drawRect(ToGoBand, Offset(left, 0f), Size(width, h))
                drawLine(FirstDownYellow, Offset(fdX, 0f), Offset(fdX, h), strokeWidth = 4.5f)
            }

            drawLine(LosWhite, Offset(losX, 0f), Offset(losX, h), strokeWidth = 4f)

            // Ball marker at LOS
            val ballR = 6.dp.toPx()
            drawCircle(BallColor, radius = ballR, center = Offset(losX, h * 0.55f))
            drawCircle(
                Color.White.copy(alpha = 0.85f),
                radius = ballR,
                center = Offset(losX, h * 0.55f),
                style = Stroke(width = 1.5f),
            )

            // Possession chip above LOS
            val chipLabel = possessionAbbr.ifBlank { "?" }
            val chipPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = with(density) { 10.sp.toPx() }
                isFakeBoldText = true
                isAntiAlias = true
            }
            val chipPadX = 6.dp.toPx()
            val chipPadY = 3.dp.toPx()
            val textW = chipPaint.measureText(chipLabel)
            val chipW = textW + chipPadX * 2f
            val chipH = chipPaint.textSize + chipPadY * 2f
            val chipLeft = (losX - chipW / 2f).coerceIn(4f, w - chipW - 4f)
            val chipTop = 6.dp.toPx()
            drawRoundRect(
                color = PossessionChipBg,
                topLeft = Offset(chipLeft, chipTop),
                size = Size(chipW, chipH),
                cornerRadius = CornerRadius(4.dp.toPx()),
            )
            native.drawText(
                chipLabel,
                chipLeft + chipW / 2f,
                chipTop + chipH / 2f + chipPaint.textSize * 0.35f,
                chipPaint,
            )
        }
    }
}
