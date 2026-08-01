package achijones.footballcoach.ui.main

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import achijones.footballcoach.ui.icons.DragHandle
import achijones.footballcoach.ui.icons.LockOpen
import achijones.footballcoach.ui.theme.FcOvrStarter
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DepthChartPanel(state: MainUiState, viewModel: MainViewModel) {
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        viewModel.moveLineupPlayer(from.index, to.index)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SpinnerDropdown(
            label = "Position",
            options = listOf(
                "QB (1 starter)", "RB (2 starters)", "FB (1 starter)", "WR (3 starters)",
                "TE (1 starter)", "OL (5 starters)", "K (1 starter)", "P (1 starter)", "S (1 starter)",
                "CB (3 starters)", "EDGE (2 starters)", "DL (3 starters)", "LB (3 starters)",
                "PR (punt return)", "KR (kick return)", "Gunner 1", "Gunner 2", "LS (long snap)",
            ),
            selectedIndex = state.lineupPositionIndex,
            onSelect = viewModel::selectLineupPosition,
        )
        DepthChartToolbar(
            starterEnabled = state.lineupStarterCount > 0,
            benchEnabled = state.lineupBenchCount > 0,
            onLockStarters = { viewModel.setLineupSectionLocks(starters = true, locked = true) },
            onUnlockStarters = { viewModel.setLineupSectionLocks(starters = true, locked = false) },
            onLockBench = { viewModel.setLineupSectionLocks(starters = false, locked = true) },
            onUnlockBench = { viewModel.setLineupSectionLocks(starters = false, locked = false) },
            onSortPosition = viewModel::autoSortUnlockedLineup,
            onSortAll = viewModel::autoSortAllLineups,
            modifier = Modifier.padding(top = 8.dp),
        )
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.weight(1f).padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.lineupRows, key = { it.playerKey }) { row ->
                ReorderableItem(reorderableState, key = row.playerKey) { isDragging ->
                    val elevation by animateDpAsState(if (isDragging) 6.dp else 0.dp, label = "depthDrag")
                    DepthChartCard(
                        row = row,
                        elevation = elevation,
                        dragHandleModifier = Modifier.draggableHandle(),
                        onPlayerClick = { viewModel.openPlayerCareer(row.playerKey) },
                        onToggleLock = { viewModel.toggleLineupLock(row.playerKey) },
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
        Text(
            "Starters: first ${state.lineupRequired} · ${state.lineupStarterCount} starters · ${state.lineupBenchCount} backups",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
}

@Composable
private fun DepthChartToolbar(
    starterEnabled: Boolean,
    benchEnabled: Boolean,
    onLockStarters: () -> Unit,
    onUnlockStarters: () -> Unit,
    onLockBench: () -> Unit,
    onUnlockBench: () -> Unit,
    onSortPosition: () -> Unit,
    onSortAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilledTonalButton(
            onClick = onSortPosition,
            modifier = Modifier.weight(1f),
        ) {
            Text("Sort Position", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        FilledTonalButton(
            onClick = onSortAll,
            modifier = Modifier.weight(1f),
        ) {
            Text("Sort All", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "Bulk lock options",
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Lock Starters") },
                    enabled = starterEnabled,
                    leadingIcon = {
                        Icon(Icons.Filled.Lock, contentDescription = null)
                    },
                    onClick = {
                        menuExpanded = false
                        onLockStarters()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Unlock Starters") },
                    enabled = starterEnabled,
                    leadingIcon = {
                        Icon(Icons.Filled.LockOpen, contentDescription = null)
                    },
                    onClick = {
                        menuExpanded = false
                        onUnlockStarters()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Lock Bench") },
                    enabled = benchEnabled,
                    leadingIcon = {
                        Icon(Icons.Filled.Lock, contentDescription = null)
                    },
                    onClick = {
                        menuExpanded = false
                        onLockBench()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Unlock Bench") },
                    enabled = benchEnabled,
                    leadingIcon = {
                        Icon(Icons.Filled.LockOpen, contentDescription = null)
                    },
                    onClick = {
                        menuExpanded = false
                        onUnlockBench()
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DepthChartCard(
    row: LineupRowUi,
    elevation: Dp,
    dragHandleModifier: Modifier,
    onPlayerClick: () -> Unit,
    onToggleLock: () -> Unit,
) {
    val cardShape = RoundedCornerShape(12.dp)
    val surfaceAlpha = when {
        row.injured -> 1f
        row.starter -> 1f
        else -> 0.72f
    }
    val borderColor = when {
        row.injured -> MaterialTheme.colorScheme.error.copy(alpha = 0.55f)
        row.starter -> FcOvrStarter.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    }
    val surfaceColor = when {
        row.injured -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = surfaceAlpha)
    }
    Surface(
        shadowElevation = elevation,
        shape = cardShape,
        color = surfaceColor,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, cardShape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onPlayerClick)
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = row.depthLabel,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (row.starter) FcOvrStarter else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .width(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (row.starter) FcOvrStarter.copy(alpha = 0.25f)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    )
                    .padding(vertical = 6.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = row.name,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    RosterMetaChip(row.primaryPos)
                    RosterMetaChip(row.yearLabel)
                    if (row.injured && row.injuryLabel != null) {
                        RosterInjuryChip(row.injuryLabel)
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            OvrPotBadge(ovr = row.posOvr, potGrade = row.potGrade)
            IconButton(onClick = onToggleLock) {
                Icon(
                    imageVector = if (row.locked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    contentDescription = if (row.locked) "Unlock player" else "Lock player",
                    tint = if (row.locked) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            IconButton(
                modifier = dragHandleModifier,
                onClick = {},
            ) {
                Icon(
                    imageVector = Icons.Filled.DragHandle,
                    contentDescription = "Drag to reorder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
