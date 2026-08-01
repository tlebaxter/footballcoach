package achijones.footballcoach.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import achijones.footballcoach.ui.components.ConferenceLogo
import achijones.footballcoach.ui.components.SegmentedControl
import achijones.footballcoach.ui.components.TeamLogo
import achijones.footballcoach.ui.theme.gradeColor
import achijones.footballcoach.ui.theme.gradeColorBg
import achijones.footballcoach.ui.theme.ovrColor

internal val ROSTER_FILTERS = listOf(
    "ALL", "QB", "RB", "FB", "WR", "TE", "OL", "K", "P", "S", "CB", "EDGE", "DL", "LB",
)

@Composable
fun SegmentRow(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    SegmentedControl(
        labels = labels,
        selected = selected,
        onSelect = onSelect,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpinnerDropdown(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    showConferenceLogos: Boolean = false,
    teamLogoOptions: List<BrowseTeamOptionUi>? = null,
) {
    if (options.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    val idx = selectedIndex.coerceIn(options.indices)
    val selectedTeam = teamLogoOptions?.getOrNull(idx)
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = options[idx],
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = when {
                showConferenceLogos -> {
                    {
                        ConferenceLogo(
                            conferenceName = options[idx],
                            size = 24.dp,
                        )
                    }
                }
                selectedTeam != null -> {
                    {
                        TeamLogo(
                            teamName = selectedTeam.name,
                            abbr = selectedTeam.abbr,
                            size = 24.dp,
                        )
                    }
                }
                else -> null
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .padding(vertical = 4.dp),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { i, opt ->
                val team = teamLogoOptions?.getOrNull(i)
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            when {
                                showConferenceLogos -> {
                                    Box(
                                        modifier = Modifier.size(24.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        ConferenceLogo(conferenceName = opt, size = 24.dp)
                                    }
                                }
                                team != null -> {
                                    TeamLogo(
                                        teamName = team.name,
                                        abbr = team.abbr,
                                        size = 24.dp,
                                    )
                                }
                            }
                            Text(opt)
                        }
                    },
                    onClick = {
                        onSelect(i)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosFilterDropdown(filter: String, onSelect: (String) -> Unit) {
    SpinnerDropdown(
        label = "Position",
        options = ROSTER_FILTERS,
        selectedIndex = ROSTER_FILTERS.indexOf(filter).coerceAtLeast(0),
        onSelect = { onSelect(ROSTER_FILTERS[it]) },
    )
}

@Composable
fun OvrPotBadge(
    ovr: Int,
    potGrade: String,
    modifier: Modifier = Modifier,
) {
    val circleSize = 36.dp
    // Light overlap (~22%) so the pot letter stays fully readable/centered.
    val overlapOffset = 28.dp
    Box(modifier = modifier.size(width = circleSize + overlapOffset, height = circleSize)) {
        // Potential sits behind and peeks out to the right.
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = overlapOffset)
                .size(circleSize)
                .clip(CircleShape)
                .background(gradeColorBg(potGrade, MaterialTheme.colorScheme.primaryContainer))
                .border(
                    1.dp,
                    gradeColor(potGrade, MaterialTheme.colorScheme.primary).copy(alpha = 0.45f),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = potGrade,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = gradeColor(potGrade, MaterialTheme.colorScheme.primary),
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
        // Overall sits in front on the left.
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(circleSize)
                .clip(CircleShape)
                .background(ovrColor(ovr))
                .border(1.5.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = ovr.toString(),
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}
