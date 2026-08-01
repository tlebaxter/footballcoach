package achijones.footballcoach.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import achijones.footballcoach.ui.components.ConferenceLogo
import achijones.footballcoach.ui.components.TeamLogo
import achijones.footballcoach.ui.components.TeamLogoResolver
import achijones.footballcoach.ui.components.rememberLogoNeedsContrastBoost
import achijones.footballcoach.ui.components.rememberTeamColors

@Composable
private fun TeamPickerStatChip(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .widthIn(min = 76.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.22f),
                        Color.Black.copy(alpha = 0.38f),
                    ),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.35f), shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.72f),
            letterSpacing = 0.8.sp,
            maxLines = 1,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeamPickerProgramSheet(
    team: TeamPickerTeamUi,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val rows = listOf(
        "Program Power" to team.programPower.toString(),
        "Tradition" to team.tradition.toString(),
        "Fans" to team.fanbase.toString(),
        "Donors" to team.donors.toString(),
        "Footprint" to team.footprint.toString(),
        "NFL Pipeline" to team.pipeline.toString(),
        "Momentum" to team.momentum.toString(),
        "Purse" to team.purse,
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = team.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Program attributes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            rows.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
fun TeamPickerScreen(
    conferences: List<TeamPickerConfUi>,
    onPick: (String) -> Unit,
) {
    if (conferences.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    var selectedConfIndex by remember { mutableIntStateOf(0) }
    var teamIndex by remember { mutableIntStateOf(0) }
    var slideForward by remember { mutableStateOf(true) }
    var showProgramSheet by remember { mutableStateOf(false) }
    val confIndex = selectedConfIndex.coerceIn(conferences.indices)
    val selectedConf = conferences[confIndex]
    val currentTeam = selectedConf.teams.getOrNull(teamIndex.coerceIn(0, (selectedConf.teams.size - 1).coerceAtLeast(0)))
    val teamColors = rememberTeamColors(currentTeam?.name, currentTeam?.abbr ?: "TEAM")
    val animPrimary by animateColorAsState(
        targetValue = teamColors.primary,
        animationSpec = tween(350),
        label = "pickerPrimary",
    )
    val animSecondary by animateColorAsState(
        targetValue = teamColors.secondary,
        animationSpec = tween(350),
        label = "pickerSecondary",
    )
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            animPrimary,
            animSecondary.copy(alpha = 0.95f),
            Color(0xFF0A0A0A),
        ),
    )
    val swipeThresholdPx = with(LocalDensity.current) { 48.dp.toPx() }
    var dragAccum by remember { mutableFloatStateOf(0f) }

    fun goToTeam(nextIndex: Int, forward: Boolean) {
        val size = selectedConf.teams.size
        if (size == 0) return
        val wrapped = ((nextIndex % size) + size) % size
        if (wrapped == teamIndex) return
        slideForward = forward
        teamIndex = wrapped
    }

    LaunchedEffect(confIndex) {
        slideForward = true
        teamIndex = 0
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush),
    ) {
        val context = LocalContext.current
        val logoSize = 200.dp
        val logoTextGap = 20.dp
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(top = 20.dp, bottom = 16.dp),
        ) {
            val watermarkSize = maxOf(maxWidth, maxHeight) * 0.92f
            AnimatedContent(
                targetState = currentTeam?.name,
                transitionSpec = {
                    fadeIn(tween(320)) togetherWith fadeOut(tween(220))
                },
                label = "teamWatermark",
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { teamName ->
                val watermark = remember(teamName) {
                    if (teamName.isNullOrBlank()) null
                    else TeamLogoResolver.loadTeam(context, teamName)
                }
                if (watermark != null) {
                    Image(
                        bitmap = watermark,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(watermarkSize)
                            .graphicsLayer { alpha = 0.14f },
                    )
                }
            }
            if (selectedConf.teams.isEmpty() || currentTeam == null) {
                Text(
                    text = "No teams in this conference.",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val canCycleTeams = selectedConf.teams.size > 1
                    IconButton(
                        onClick = { goToTeam(teamIndex - 1, forward = false) },
                        enabled = canCycleTeams,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous team",
                            tint = Color.White.copy(
                                alpha = if (canCycleTeams) 0.95f else 0.3f,
                            ),
                            modifier = Modifier.size(40.dp),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .pointerInput(confIndex, selectedConf.teams.size, teamIndex) {
                                detectHorizontalDragGestures(
                                    onDragStart = { dragAccum = 0f },
                                    onDragEnd = {
                                        when {
                                            dragAccum <= -swipeThresholdPx ->
                                                goToTeam(teamIndex + 1, forward = true)
                                            dragAccum >= swipeThresholdPx ->
                                                goToTeam(teamIndex - 1, forward = false)
                                        }
                                        dragAccum = 0f
                                    },
                                    onDragCancel = { dragAccum = 0f },
                                    onHorizontalDrag = { _, amount -> dragAccum += amount },
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        AnimatedContent(
                            targetState = currentTeam,
                            transitionSpec = {
                                val enter = fadeIn(tween(240)) + slideInHorizontally(tween(280)) {
                                    if (slideForward) it / 4 else -it / 4
                                }
                                val exit = fadeOut(tween(180)) + slideOutHorizontally(tween(220)) {
                                    if (slideForward) -it / 4 else it / 4
                                }
                                (enter togetherWith exit).using(SizeTransform(clip = false))
                            },
                            label = "teamLogoSwap",
                            contentAlignment = Alignment.Center,
                        ) { team ->
                            val teamBoostBg = rememberTeamColors(team.name, team.abbr).primary
                            val needsContrastBoost = rememberLogoNeedsContrastBoost(
                                teamName = team.name,
                                background = teamBoostBg,
                            )
                            TeamLogo(
                                teamName = team.name,
                                abbr = team.abbr,
                                size = logoSize,
                                framed = false,
                                contrastBoost = needsContrastBoost,
                            )
                        }
                    }
                    IconButton(
                        onClick = { goToTeam(teamIndex + 1, forward = true) },
                        enabled = canCycleTeams,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next team",
                            tint = Color.White.copy(
                                alpha = if (canCycleTeams) 0.95f else 0.3f,
                            ),
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }

                AnimatedContent(
                    targetState = currentTeam,
                    transitionSpec = {
                        fadeIn(tween(240)) togetherWith fadeOut(tween(180))
                    },
                    label = "teamTextSwap",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .offset(y = maxHeight / 2 + logoSize / 2 + logoTextGap),
                    contentAlignment = Alignment.TopCenter,
                ) { team ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = team.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(10.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                TeamPickerStatChip(
                                    label = "POWER",
                                    value = team.programPower.toString(),
                                    onClick = { showProgramSheet = true },
                                )
                                TeamPickerStatChip(label = "OFF", value = team.offTalent.toString())
                                TeamPickerStatChip(label = "DEF", value = team.defTalent.toString())
                                TeamPickerStatChip(label = "ST", value = team.stTalent.toString())
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Choose your team",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(conferences) { index, conf ->
                        val selected = index == confIndex
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(22.dp))
                                .background(
                                    if (selected) Color.White.copy(alpha = 0.24f)
                                    else Color.Black.copy(alpha = 0.32f),
                                )
                                .border(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) Color.White.copy(alpha = 0.9f)
                                    else Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(22.dp),
                                )
                                .clickable { selectedConfIndex = index }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            ConferenceLogo(conferenceName = conf.name, size = 24.dp)
                            Text(
                                text = conf.name,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = Color.White,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (currentTeam != null && selectedConf.teams.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        val pageCount = selectedConf.teams.size
                        val maxDots = 11
                        val current = teamIndex
                        val windowStart = when {
                            pageCount <= maxDots -> 0
                            current <= maxDots / 2 -> 0
                            current >= pageCount - (maxDots / 2) -> pageCount - maxDots
                            else -> current - maxDots / 2
                        }
                        val windowEnd = (windowStart + maxDots).coerceAtMost(pageCount)
                        for (i in windowStart until windowEnd) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .size(if (i == current) 9.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (i == current) Color.White
                                        else Color.White.copy(alpha = 0.35f),
                                    ),
                            )
                        }
                    }
                }

                Button(
                    onClick = { if (currentTeam != null) onPick(currentTeam.abbr) },
                    enabled = currentTeam != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black,
                        disabledContainerColor = Color.White.copy(alpha = 0.4f),
                        disabledContentColor = Color.Black.copy(alpha = 0.5f),
                    ),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("Select", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            }
        }

    }

    if (showProgramSheet && currentTeam != null) {
        TeamPickerProgramSheet(
            team = currentTeam,
            onDismiss = { showProgramSheet = false },
        )
    }
}
