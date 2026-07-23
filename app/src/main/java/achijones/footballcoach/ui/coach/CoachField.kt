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
private val SidelineColor = Color(0xFF243528)
private val FirstDownYellow = Color(0xFFFDE047)
private val DriveBlock = Color(0xFF9CA3AF)
private val DriveTip = Color(0xFFEF5350)
private val DownMarkerOrange = Color(0xFFF97316)

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
    distance: Int,
    down: Int,
    drivePath: List<Int>,
    possessionHome: Boolean,
    homeDefendsLeft: Boolean,
    homeName: String,
    awayName: String,
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
    val firstDownYard = (yardLine + distance).coerceAtMost(100)
    val firstDownFraction = offenseYardToAbsolute(firstDownYard, possessionHome, homeDefendsLeft)
    val downLabel = down.coerceIn(1, 4).toString()

    Box(
        modifier = modifier
            .clip(FieldShape)
            .border(1.dp, Color(0xFF2F5D34), FieldShape),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val sidelineH = 22.dp.toPx()
            val fieldH = h - sidelineH
            val endZoneW = w * 0.09f
            val playableW = w - endZoneW * 2f

            drawRect(
                brush = Brush.verticalGradient(listOf(FieldGreen, FieldGreenDark)),
                size = Size(w, fieldH),
            )
            drawRect(FieldEndZone, Offset(0f, 0f), Size(endZoneW, fieldH))
            drawRect(FieldEndZone, Offset(w - endZoneW, 0f), Size(endZoneW, fieldH))
            drawRect(SidelineColor, Offset(0f, fieldH), Size(w, sidelineH))

            for (i in 0..10) {
                val x = endZoneW + playableW * i / 10f
                val major = i % 5 == 0
                drawLine(
                    Color.White.copy(alpha = if (major) 0.45f else 0.22f),
                    Offset(x, 0f),
                    Offset(x, fieldH),
                    strokeWidth = if (major) 2.5f else 1.5f,
                )
                if (major && i in 1..9) {
                    val yard = if (i <= 5) i * 10 else (10 - i) * 10
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(180, 255, 255, 255)
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = with(density) { 11.sp.toPx() }
                        isFakeBoldText = true
                    }
                    drawContext.canvas.nativeCanvas.drawText(yard.toString(), x, fieldH * 0.22f, paint)
                }
            }

            // Endzone names — rotated so letters read top → bottom
            val endZonePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(217, 255, 255, 255)
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = with(density) { 11.sp.toPx() }
                isFakeBoldText = true
                isAntiAlias = true
            }
            val maxNameLen = fieldH * 0.88f
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
            native.rotate(90f, endZoneW / 2f, fieldH / 2f)
            native.drawText(leftName, endZoneW / 2f, fieldH / 2f + leftPaint.textSize * 0.35f, leftPaint)
            native.restore()
            val rightPaint = paintForName(rightName)
            native.save()
            native.rotate(90f, w - endZoneW / 2f, fieldH / 2f)
            native.drawText(rightName, w - endZoneW / 2f, fieldH / 2f + rightPaint.textSize * 0.35f, rightPaint)
            native.restore()

            drawRoundRect(
                color = Color.White.copy(alpha = 0.2f),
                topLeft = Offset(1f, 1f),
                size = Size(w - 2f, h - 2f),
                cornerRadius = CornerRadius(12.dp.toPx()),
                style = Stroke(width = 1.5f),
            )
            // Sideline edge
            drawLine(
                Color.White.copy(alpha = 0.35f),
                Offset(0f, fieldH),
                Offset(w, fieldH),
                strokeWidth = 2f,
            )

            // Drive stair path
            if (drivePath.size >= 2) {
                val stepH = (fieldH * 0.55f) / drivePath.size.coerceAtLeast(1)
                val baseY = fieldH * 0.28f
                for (i in 0 until drivePath.lastIndex) {
                    val y1 = offenseYardToAbsolute(drivePath[i], possessionHome, homeDefendsLeft)
                    val y2 = offenseYardToAbsolute(drivePath[i + 1], possessionHome, homeDefendsLeft)
                    val x1 = endZoneW + playableW * y1
                    val x2 = endZoneW + playableW * y2
                    val top = baseY + i * stepH
                    val bot = top + stepH * 0.85f
                    val left = minOf(x1, x2)
                    val width = (kotlin.math.abs(x2 - x1)).coerceAtLeast(6.dp.toPx())
                    val color = if (i == drivePath.lastIndex - 1) DriveTip else DriveBlock.copy(alpha = 0.75f)
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(left, top),
                        size = Size(width, bot - top),
                        cornerRadius = CornerRadius(3.dp.toPx()),
                    )
                }
            }

            val losX = endZoneW + playableW * ballFraction
            val fdX = endZoneW + playableW * firstDownFraction

            drawLine(FirstDownYellow.copy(alpha = 0.95f), Offset(fdX, 4f), Offset(fdX, fieldH), strokeWidth = 3.5f)
            drawLine(Color.White, Offset(losX, 4f), Offset(losX, fieldH), strokeWidth = 3f)

            // Down marker on the sideline (below the field)
            val plateW = 16.dp.toPx()
            val plateH = 16.dp.toPx()
            val plateTop = fieldH + (sidelineH - plateH) / 2f
            drawRoundRect(
                color = DownMarkerOrange,
                topLeft = Offset(losX - plateW / 2f, plateTop),
                size = Size(plateW, plateH),
                cornerRadius = CornerRadius(3.dp.toPx()),
            )
            val downPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = with(density) { 11.sp.toPx() }
                isFakeBoldText = true
                isAntiAlias = true
            }
            native.drawText(
                downLabel,
                losX,
                plateTop + plateH / 2f + downPaint.textSize * 0.35f,
                downPaint,
            )
        }
    }
}
