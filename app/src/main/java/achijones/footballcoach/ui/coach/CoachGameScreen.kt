package achijones.footballcoach.ui.coach

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import CFBsimPack.Formation
import CFBsimPack.engine.AutoSimUntil
import CFBsimPack.engine.CoverageCall
import CFBsimPack.engine.GameSituation
import CFBsimPack.engine.OffensePlay
import CFBsimPack.engine.TempoCall
import achijones.footballcoach.ui.components.SegmentedControl
import achijones.footballcoach.ui.theme.FcAccent
import achijones.footballcoach.ui.theme.FcOnAccent
import achijones.footballcoach.ui.theme.FcOnPrimary
import achijones.footballcoach.ui.theme.FcPrimary
import achijones.footballcoach.ui.theme.FcPrimaryDark
import achijones.footballcoach.ui.theme.FcSurface
import achijones.footballcoach.ui.theme.FcSurfaceVariant

private val PanelShape = RoundedCornerShape(16.dp)
private val ChipShape = RoundedCornerShape(10.dp)
private val PlayButtonShape = RoundedCornerShape(14.dp)
private val FieldShape = RoundedCornerShape(12.dp)

private val FieldGreen = Color(0xFF1B5E20)
private val FieldGreenDark = Color(0xFF0F3D14)
private val FieldEndZone = Color(0xFF14532D)
private val ScoreboardBg = Color(0xFF0D1117)
private val PanelBg = Color(0xFF121A14)
private val GhostBorder = Color(0xFF3A4A3C)
private val MutedText = Color(0xFF9CA3AF)
private val BallOrange = Color(0xFFF59E0B)
private val FirstDownYellow = Color(0xFFFDE047)

@Composable
fun CoachGameScreen(
    onFinished: () -> Unit,
    viewModel: CoachGameViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    BackHandler {
        viewModel.finishAndClose()
        onFinished()
    }

    if (state.finished) {
        Column(
            Modifier
                .fillMaxSize()
                .background(ScoreboardBg)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                state.error ?: "Final",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Head back to the season hub.",
                color = MutedText,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    viewModel.finishAndClose()
                    onFinished()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = FcPrimary,
                    contentColor = FcOnPrimary,
                ),
                shape = PlayButtonShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text("Back to season", fontWeight = FontWeight.SemiBold)
            }
        }
        return
    }

    val sit = state.situation
    Column(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF071208), Color(0xFF0B1F0B), Color(0xFF0A160C)),
                ),
            ),
    ) {
        if (sit != null) {
            ScoreboardHeader(sit)

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
            ) {
                Spacer(Modifier.height(10.dp))
                SituationStrip(sit)
                Spacer(Modifier.height(10.dp))
                EspnField(
                    yardLine = sit.yardLine,
                    distance = sit.distance,
                    possessionHome = sit.possessionHome,
                    homeAbbr = sit.homeAbbr,
                    awayAbbr = sit.awayAbbr,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp),
                )
                Spacer(Modifier.height(10.dp))
                LastPlayBanner(sit.lastPlay)
                Spacer(Modifier.height(12.dp))

                CallSheetPanel {
                    if (sit.userOnOffense) {
                        OffenseCallSheet(
                            selectedFormation = state.selectedFormation,
                            selectedTempo = state.selectedTempo,
                            onSelectFormation = viewModel::selectFormation,
                            onSelectTempo = viewModel::selectTempo,
                            onCallPlay = viewModel::callPlay,
                            onTimeout = viewModel::callTimeout,
                        )
                    } else {
                        DefenseCallSheet(
                            selectedCoverage = state.selectedCoverage,
                            onSelectCoverage = viewModel::selectCoverage,
                            onDefend = viewModel::callDefenseOnly,
                            onTimeout = viewModel::callTimeout,
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                AutoSimRow(onAutoSim = viewModel::autoSim)
                Spacer(Modifier.height(4.dp))
            }
        } else {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("Loading game…", color = MutedText)
            }
        }

        TextButton(
            onClick = {
                viewModel.finishAndClose()
                onFinished()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text("Exit · sim rest if needed", color = MutedText)
        }
    }
}

@Composable
private fun ScoreboardHeader(sit: GameSituation) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(ScoreboardBg)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TeamScoreBlock(
                abbr = sit.awayAbbr,
                score = sit.awayScore,
                hasBall = !sit.possessionHome,
                modifier = Modifier.weight(1f),
                alignEnd = false,
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                Text(
                    if (sit.playingOT) "OT" else "Q${sit.quarter}",
                    color = FcPrimary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
                Text(
                    sit.clock,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            TeamScoreBlock(
                abbr = sit.homeAbbr,
                score = sit.homeScore,
                hasBall = sit.possessionHome,
                modifier = Modifier.weight(1f),
                alignEnd = true,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TimeoutPips(count = sit.awayTimeouts, label = sit.awayAbbr)
            Text("TIMEOUTS", color = Color(0xFF6B7280), style = MaterialTheme.typography.labelSmall)
            TimeoutPips(count = sit.homeTimeouts, label = sit.homeAbbr)
        }
    }
}

@Composable
private fun TeamScoreBlock(
    abbr: String,
    score: Int,
    hasBall: Boolean,
    alignEnd: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start,
        ) {
            if (hasBall && !alignEnd) {
                PossessionDot()
                Spacer(Modifier.width(6.dp))
            }
            Text(
                abbr,
                color = if (hasBall) Color.White else MutedText,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
            if (hasBall && alignEnd) {
                Spacer(Modifier.width(6.dp))
                PossessionDot()
            }
        }
        Text(
            "$score",
            color = Color.White,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun PossessionDot() {
    Box(
        Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(BallOrange),
    )
}

@Composable
private fun TimeoutPips(count: Int, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = MutedText, style = MaterialTheme.typography.labelSmall)
        repeat(3) { i ->
            Box(
                Modifier
                    .size(width = 14.dp, height = 6.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (i < count) FcPrimary else Color(0xFF374151)),
            )
        }
    }
}

@Composable
private fun SituationStrip(sit: GameSituation) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF152018))
            .border(1.dp, GhostBorder.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            sit.downDistanceLabel,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            if (sit.userOnOffense) "YOUR BALL" else "DEFENDING",
            color = if (sit.userOnOffense) FcPrimary else FcAccent,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium,
            letterSpacing = 0.8.sp,
        )
    }
}

@Composable
private fun LastPlayBanner(lastPlay: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1A241C))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "LAST",
            color = FcPrimary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(end = 10.dp),
        )
        Text(
            lastPlay.ifBlank { "Awaiting play call…" },
            color = Color(0xFFE5E7EB),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CallSheetPanel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(PanelShape)
            .background(PanelBg)
            .border(1.dp, GhostBorder.copy(alpha = 0.6f), PanelShape)
            .padding(14.dp),
        content = content,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OffenseCallSheet(
    selectedFormation: Formation,
    selectedTempo: TempoCall,
    onSelectFormation: (Formation) -> Unit,
    onSelectTempo: (TempoCall) -> Unit,
    onCallPlay: (OffensePlay) -> Unit,
    onTimeout: () -> Unit,
) {
    SectionLabel("Formation")
    Spacer(Modifier.height(8.dp))
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Formation.entries.forEach { formation ->
            SelectableChip(
                label = formation.displayName,
                selected = selectedFormation == formation,
                onClick = { onSelectFormation(formation) },
            )
        }
    }

    Spacer(Modifier.height(16.dp))
    SectionLabel("Play call")
    Spacer(Modifier.height(8.dp))
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PrimaryPlayButton(
            label = "RUN",
            subtitle = "Hand off / keep",
            container = FcPrimary,
            content = FcOnPrimary,
            onClick = { onCallPlay(OffensePlay.RUN) },
            modifier = Modifier.weight(1f),
        )
        PrimaryPlayButton(
            label = "PASS",
            subtitle = "Dropback / RPO",
            container = FcPrimaryDark,
            content = Color.White,
            onClick = { onCallPlay(OffensePlay.PASS) },
            modifier = Modifier.weight(1f),
        )
    }

    Spacer(Modifier.height(10.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SecondaryActionChip("Field Goal", onClick = { onCallPlay(OffensePlay.FIELD_GOAL) })
        SecondaryActionChip("Punt", onClick = { onCallPlay(OffensePlay.PUNT) })
        SecondaryActionChip("Spike", onClick = { onCallPlay(OffensePlay.SPIKE) })
        SecondaryActionChip("Kneel", onClick = { onCallPlay(OffensePlay.KNEEL) })
        SecondaryActionChip("Timeout", onClick = onTimeout, accent = true)
    }

    Spacer(Modifier.height(16.dp))
    SectionLabel("Tempo")
    Spacer(Modifier.height(8.dp))
    val tempos = TempoCall.entries
    SegmentedControl(
        labels = tempos.map { it.displayLabel() },
        selected = tempos.indexOf(selectedTempo).coerceAtLeast(0),
        onSelect = { onSelectTempo(tempos[it]) },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DefenseCallSheet(
    selectedCoverage: CoverageCall,
    onSelectCoverage: (CoverageCall) -> Unit,
    onDefend: () -> Unit,
    onTimeout: () -> Unit,
) {
    SectionLabel("Coverage")
    Spacer(Modifier.height(8.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CoverageCall.entries.forEach { coverage ->
            SelectableChip(
                label = coverage.displayLabel(),
                selected = selectedCoverage == coverage,
                onClick = { onSelectCoverage(coverage) },
            )
        }
    }

    Spacer(Modifier.height(16.dp))
    PrimaryPlayButton(
        label = "DEFEND SNAP",
        subtitle = "Lock coverage · AI offense",
        container = FcAccent,
        content = FcOnAccent,
        onClick = onDefend,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(10.dp))
    SecondaryActionChip("Timeout", onClick = onTimeout, accent = true, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun AutoSimRow(onAutoSim: (AutoSimUntil) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF101610))
            .padding(12.dp),
    ) {
        SectionLabel("Quick sim")
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AutoSimUntil.entries.forEach { until ->
                SecondaryActionChip(
                    label = until.displayLabel(),
                    onClick = { onAutoSim(until) },
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = FcPrimary,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.labelMedium,
        letterSpacing = 1.2.sp,
    )
}

@Composable
private fun PrimaryPlayButton(
    label: String,
    subtitle: String,
    container: Color,
    content: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .heightIn(min = 72.dp)
            .clip(PlayButtonShape)
            .background(container)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            label,
            color = content,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge,
            letterSpacing = 1.sp,
        )
        Text(
            subtitle,
            color = content.copy(alpha = 0.75f),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(
        targetValue = if (selected) FcPrimary else FcSurfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "chipBg",
    )
    val fg by animateColorAsState(
        targetValue = if (selected) FcOnPrimary else Color(0xFFE5E7EB),
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "chipFg",
    )
    val border by animateColorAsState(
        targetValue = if (selected) FcPrimary else GhostBorder,
        label = "chipBorder",
    )
    val interaction = remember { MutableInteractionSource() }

    Box(
        Modifier
            .clip(ChipShape)
            .background(bg)
            .border(1.dp, border, ChipShape)
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            color = fg,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun SecondaryActionChip(
    label: String,
    onClick: () -> Unit,
    accent: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clip(ChipShape)
            .background(if (accent) Color(0xFF2A2218) else FcSurface)
            .border(
                1.dp,
                if (accent) FcAccent.copy(alpha = 0.55f) else GhostBorder,
                ChipShape,
            )
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (accent) FcAccent else Color(0xFFE5E7EB),
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EspnField(
    yardLine: Int,
    distance: Int,
    possessionHome: Boolean,
    homeAbbr: String,
    awayAbbr: String,
    modifier: Modifier = Modifier,
) {
    val leftAbbr = if (possessionHome) awayAbbr else homeAbbr
    val rightAbbr = if (possessionHome) homeAbbr else awayAbbr
    val density = LocalDensity.current
    val ballFraction by animateFloatAsState(
        targetValue = yardLine.coerceIn(0, 100) / 100f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "ballX",
    )
    val firstDownYard = (yardLine + distance).coerceAtMost(100)

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
                size = size,
            )
            drawRect(FieldEndZone, Offset(0f, 0f), Size(endZoneW, h))
            drawRect(FieldEndZone, Offset(w - endZoneW, 0f), Size(endZoneW, h))

            // Hash / yard lines
            for (i in 0..10) {
                val x = endZoneW + playableW * i / 10f
                val major = i % 5 == 0
                drawLine(
                    Color.White.copy(alpha = if (major) 0.45f else 0.22f),
                    Offset(x, 0f),
                    Offset(x, h),
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
                    drawContext.canvas.nativeCanvas.drawText(
                        yard.toString(),
                        x,
                        h * 0.22f,
                        paint,
                    )
                }
            }

            // Sideline frame
            drawRoundRect(
                color = Color.White.copy(alpha = 0.2f),
                topLeft = Offset(1f, 1f),
                size = Size(w - 2f, h - 2f),
                cornerRadius = CornerRadius(12.dp.toPx()),
                style = Stroke(width = 1.5f),
            )

            val losX = endZoneW + playableW * ballFraction
            val fdX = endZoneW + playableW * (firstDownYard / 100f)

            // First down
            drawLine(
                FirstDownYellow.copy(alpha = 0.95f),
                Offset(fdX, 6f),
                Offset(fdX, h - 6f),
                strokeWidth = 3.5f,
            )
            // Line of scrimmage
            drawLine(
                Color.White,
                Offset(losX, 4f),
                Offset(losX, h - 4f),
                strokeWidth = 3f,
            )

            // Football
            val ballR = 9.dp.toPx()
            drawOval(
                color = BallOrange,
                topLeft = Offset(losX - ballR * 1.15f, h / 2f - ballR * 0.7f),
                size = Size(ballR * 2.3f, ballR * 1.4f),
            )
            drawLine(
                Color(0xFF3E1400).copy(alpha = 0.55f),
                Offset(losX - ballR * 0.55f, h / 2f),
                Offset(losX + ballR * 0.55f, h / 2f),
                strokeWidth = 2f,
            )

            // Direction chevron toward opponent end
            val chevron = Path().apply {
                val tip = losX + 18.dp.toPx()
                moveTo(tip, h / 2f)
                lineTo(tip - 10.dp.toPx(), h / 2f - 7.dp.toPx())
                lineTo(tip - 10.dp.toPx(), h / 2f + 7.dp.toPx())
                close()
            }
            drawPath(chevron, Color.White.copy(alpha = 0.55f))
        }

        Text(
            leftAbbr,
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, bottom = 6.dp),
        )
        Text(
            rightAbbr,
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = 6.dp),
        )
    }
}

private fun TempoCall.displayLabel(): String = when (this) {
    TempoCall.NORMAL -> "Normal"
    TempoCall.HURRY_UP -> "Hurry"
    TempoCall.CHEW_CLOCK -> "Chew"
}

private fun CoverageCall.displayLabel(): String = when (this) {
    CoverageCall.COVER_0 -> "Cover 0"
    CoverageCall.COVER_1 -> "Cover 1"
    CoverageCall.COVER_2 -> "Cover 2"
    CoverageCall.COVER_3 -> "Cover 3"
    CoverageCall.COVER_4 -> "Cover 4"
    CoverageCall.MAN -> "Man"
    CoverageCall.ZONE -> "Zone"
    CoverageCall.STACK_BOX -> "Stack Box"
    CoverageCall.SPY -> "Spy"
    CoverageCall.PRESS -> "Press"
    CoverageCall.OFF_COVERAGE -> "Off"
}

private fun AutoSimUntil.displayLabel(): String = when (this) {
    AutoSimUntil.DRIVE -> "Drive"
    AutoSimUntil.POSSESSION -> "Possession"
    AutoSimUntil.QUARTER -> "Quarter"
    AutoSimUntil.HALF -> "Half"
    AutoSimUntil.GAME -> "Game"
}
