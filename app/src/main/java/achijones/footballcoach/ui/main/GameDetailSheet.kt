package achijones.footballcoach.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import achijones.footballcoach.ui.components.TeamLogo
import achijones.footballcoach.ui.components.rememberSheetFlingBlocker
import achijones.footballcoach.ui.components.rememberTeamColors
import achijones.footballcoach.ui.theme.FcLoss
import achijones.footballcoach.ui.theme.FcOvrElite
import achijones.footballcoach.ui.theme.FcWin
import achijones.footballcoach.ui.theme.ovrColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailSheet(dialog: GameDialogUi, viewModel: MainViewModel) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cardShape = RoundedCornerShape(14.dp)
    val awayScout = dialog.awayScout
    val homeScout = dialog.homeScout
    val awayBox = dialog.awayBox
    val homeBox = dialog.homeBox
    val showScoutUi = !dialog.played && awayScout != null && homeScout != null
    val showResultUi = dialog.played && awayBox != null && homeBox != null
    ModalBottomSheet(
        onDismissRequest = viewModel::dismissGameDialog,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
        ) {
            if (!showScoutUi && !showResultUi) {
                Text(
                    text = dialog.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .nestedScroll(rememberSheetFlingBlocker())
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when {
                    showScoutUi && awayScout != null && homeScout != null -> {
                        GameScoutMatchupHeader(
                            away = awayScout,
                            home = homeScout,
                            gameName = dialog.gameName.ifBlank { dialog.title },
                            rivalryLabel = dialog.rivalryLabel,
                        )
                        Text(
                            "SCOUT REPORT",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            GameScoutTeamCard(team = awayScout, modifier = Modifier.weight(1f))
                            GameScoutTeamCard(team = homeScout, modifier = Modifier.weight(1f))
                        }
                    }

                    showResultUi && awayBox != null && homeBox != null -> {
                        GameResultMatchupHeader(
                            away = awayBox,
                            home = homeBox,
                            gameName = dialog.gameName.ifBlank { dialog.title },
                            otLabel = dialog.otLabel,
                            awayWon = dialog.awayWon,
                            rivalryLabel = dialog.rivalryLabel,
                        )
                        Text(
                            "BOX SCORE",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            GameBoxTeamCard(
                                team = awayBox,
                                won = dialog.awayWon == true,
                                modifier = Modifier.weight(1f),
                            )
                            GameBoxTeamCard(
                                team = homeBox,
                                won = dialog.awayWon == false,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    else -> {
                        if (dialog.played) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(cardShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                        cardShape,
                                    )
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(
                                        dialog.awayName.orEmpty(),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        dialog.awayScore.orEmpty(),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.headlineMedium,
                                    )
                                }
                                Text(
                                    dialog.otLabel.orEmpty(),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                )
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(
                                        dialog.homeName.orEmpty(),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        dialog.homeScore.orEmpty(),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.headlineMedium,
                                    )
                                }
                            }
                        }

                        Text(
                            if (dialog.played) "BOX SCORE" else "SCOUT REPORT",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(cardShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    cardShape,
                                )
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                dialog.left.trimEnd(),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                dialog.center.trimEnd(),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                dialog.right.trimEnd(),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                when {
                    showResultUi && awayBox != null && homeBox != null -> {
                        Text(
                            "SCHEMES",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        GameSchemesCard(away = awayBox, home = homeBox)
                        if (dialog.gameLogLines.isNotEmpty()) {
                            Text(
                                "GAME LOG",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            GameLogCard(lines = dialog.gameLogLines)
                        }
                    }

                    !showResultUi && dialog.bottom.isNotBlank() -> {
                        Text(
                            if (dialog.played) "GAME LOG" else "NOTES",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = dialog.bottom.trim(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(cardShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                                    cardShape,
                                )
                                .padding(12.dp),
                        )
                    }
                }

                if (!dialog.played && dialog.canCoach) {
                    Button(
                        onClick = { viewModel.startCoachGame(dialog.gameKey) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(
                            "Coach this game",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun GameResultMatchupHeader(
    away: GameBoxTeamUi,
    home: GameBoxTeamUi,
    gameName: String,
    otLabel: String?,
    awayWon: Boolean?,
    rivalryLabel: String?,
) {
    val awayColors = rememberTeamColors(away.name, away.abbr)
    val homeColors = rememberTeamColors(home.name, home.abbr)
    val cardShape = RoundedCornerShape(14.dp)
    val headerBrush = when (awayWon) {
        true -> Brush.horizontalGradient(
            listOf(
                FcWin.copy(alpha = 0.55f),
                MaterialTheme.colorScheme.surfaceVariant,
                FcLoss.copy(alpha = 0.35f),
            ),
        )
        false -> Brush.horizontalGradient(
            listOf(
                FcLoss.copy(alpha = 0.35f),
                MaterialTheme.colorScheme.surfaceVariant,
                FcWin.copy(alpha = 0.55f),
            ),
        )
        null -> Brush.horizontalGradient(
            listOf(
                awayColors.primary.copy(alpha = 0.42f),
                MaterialTheme.colorScheme.surfaceVariant,
                homeColors.primary.copy(alpha = 0.42f),
            ),
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(headerBrush)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), cardShape)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GameResultHeaderTeam(
                team = away,
                won = awayWon == true,
                modifier = Modifier.weight(1f),
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                Text(
                    text = otLabel.takeUnless { it.isNullOrBlank() } ?: "@",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "FINAL",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            GameResultHeaderTeam(
                team = home,
                won = awayWon == false,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = gameName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (rivalryLabel != null) {
            Text(
                text = rivalryLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun GameResultHeaderTeam(
    team: GameBoxTeamUi,
    won: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TeamLogo(
            teamName = team.name,
            abbr = team.abbr,
            size = 52.dp,
        )
        Text(
            text = team.abbr,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "(${team.record})",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = team.score.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = if (won) {
                Color(0xFF81C784)
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
private fun GameBoxTeamCard(
    team: GameBoxTeamUi,
    won: Boolean,
    modifier: Modifier = Modifier,
) {
    val teamColors = rememberTeamColors(team.name, team.abbr)
    val cardShape = RoundedCornerShape(14.dp)
    val accent = if (won) FcWin else teamColors.primary
    Column(
        modifier = modifier
            .clip(cardShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, accent.copy(alpha = 0.45f), cardShape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(accent),
        )
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = team.abbr,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (won) {
                    Text(
                        text = "W",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF81C784),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                GameScoutMetricTile(
                    label = "PASS",
                    value = team.passYards.toString(),
                    modifier = Modifier.weight(1f),
                    valueColor = MaterialTheme.colorScheme.onSurface,
                )
                GameScoutMetricTile(
                    label = "RUSH",
                    value = team.rushYards.toString(),
                    modifier = Modifier.weight(1f),
                    valueColor = MaterialTheme.colorScheme.onSurface,
                )
                GameScoutMetricTile(
                    label = "TO",
                    value = team.turnovers.toString(),
                    modifier = Modifier.weight(1f),
                    valueColor = if (team.turnovers >= 3) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }
    }
}

@Composable
private fun GameSchemesCard(
    away: GameBoxTeamUi,
    home: GameBoxTeamUi,
) {
    val cardShape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f), cardShape)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        GameSchemeTeamColumn(team = away, modifier = Modifier.weight(1f))
        GameSchemeTeamColumn(team = home, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun GameSchemeTeamColumn(
    team: GameBoxTeamUi,
    modifier: Modifier = Modifier,
) {
    val teamColors = rememberTeamColors(team.name, team.abbr)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, teamColors.primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = team.abbr,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        GameScoutMetaLine(label = "OFF", value = team.offPhilosophy)
        GameScoutMetaLine(label = "DEF", value = team.defSystem)
    }
}

@Composable
private fun GameLogCard(lines: List<String>) {
    val cardShape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f), cardShape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        lines.forEach { line ->
            val kind = gameLogLineKind(line)
            Text(
                text = line,
                style = when (kind) {
                    GameLogLineKind.MARKER -> MaterialTheme.typography.labelMedium
                    GameLogLineKind.SCORE -> MaterialTheme.typography.bodySmall
                    GameLogLineKind.META -> MaterialTheme.typography.labelSmall
                    GameLogLineKind.NORMAL -> MaterialTheme.typography.bodySmall
                },
                fontWeight = when (kind) {
                    GameLogLineKind.MARKER, GameLogLineKind.SCORE -> FontWeight.SemiBold
                    else -> FontWeight.Normal
                },
                color = when (kind) {
                    GameLogLineKind.MARKER -> MaterialTheme.colorScheme.primary
                    GameLogLineKind.SCORE -> MaterialTheme.colorScheme.onSurface
                    GameLogLineKind.META -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    GameLogLineKind.NORMAL -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

private enum class GameLogLineKind {
    META,
    MARKER,
    SCORE,
    NORMAL,
}

private fun gameLogLineKind(line: String): GameLogLineKind {
    val trimmed = line.trim()
    if (trimmed.startsWith("LOG:") || trimmed.startsWith("---") || trimmed.startsWith("====")) {
        return GameLogLineKind.META
    }
    if (Regex("""^\dQ\s+\d+:\d+""").containsMatchIn(trimmed) ||
        trimmed.equals("FINAL", ignoreCase = true) ||
        trimmed.contains("Time has expired", ignoreCase = true)
    ) {
        return GameLogLineKind.MARKER
    }
    if (trimmed.contains("TOUCHDOWN", ignoreCase = true) ||
        trimmed.contains(" TD", ignoreCase = true) ||
        trimmed.contains("field goal", ignoreCase = true) ||
        trimmed.contains(" FG", ignoreCase = true) ||
        trimmed.contains("safety", ignoreCase = true)
    ) {
        return GameLogLineKind.SCORE
    }
    return GameLogLineKind.NORMAL
}

@Composable
private fun GameScoutMatchupHeader(
    away: GameScoutTeamUi,
    home: GameScoutTeamUi,
    gameName: String,
    rivalryLabel: String?,
) {
    val awayColors = rememberTeamColors(away.name, away.abbr)
    val homeColors = rememberTeamColors(home.name, home.abbr)
    val cardShape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        awayColors.primary.copy(alpha = 0.42f),
                        MaterialTheme.colorScheme.surfaceVariant,
                        homeColors.primary.copy(alpha = 0.42f),
                    ),
                ),
            )
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), cardShape)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GameScoutHeaderTeam(
                team = away,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "@",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            GameScoutHeaderTeam(
                team = home,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = gameName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (rivalryLabel != null) {
            Text(
                text = rivalryLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun GameScoutHeaderTeam(
    team: GameScoutTeamUi,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TeamLogo(
            teamName = team.name,
            abbr = team.abbr,
            size = 56.dp,
        )
        Text(
            text = team.abbr,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "#${team.rank}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GameScoutTeamCard(
    team: GameScoutTeamUi,
    modifier: Modifier = Modifier,
) {
    val teamColors = rememberTeamColors(team.name, team.abbr)
    val cardShape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .clip(cardShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, teamColors.primary.copy(alpha = 0.45f), cardShape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(teamColors.primary),
        )
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = team.abbr,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            GameScoutMetaLine(label = "OFF", value = team.offPhilosophy)
            GameScoutMetaLine(label = "DEF", value = team.defSystem)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                GameScoutMetricTile("OFF", team.offTalent.toString(), Modifier.weight(1f))
                GameScoutMetricTile("DEF", team.defTalent.toString(), Modifier.weight(1f))
                GameScoutMetricTile("PWR", team.programPower.toString(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GameScoutMetaLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun GameScoutMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = ovrColor(value.toIntOrNull() ?: 0),
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

