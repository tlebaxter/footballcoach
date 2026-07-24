package achijones.footballcoach.ui.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import CFBsimPack.Formation
import CFBsimPack.engine.DefenseConcept
import CFBsimPack.engine.GameSituation
import CFBsimPack.engine.GameState
import CFBsimPack.engine.OffenseConcept
import CFBsimPack.engine.Playbook

private val SheetBg = Color(0xFF121A14)
private val ChipShape = RoundedCornerShape(10.dp)
private val RowShape = RoundedCornerShape(12.dp)
private val Muted = Color(0xFF9CA3AF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayPickerSheet(
    userOnOffense: Boolean,
    situation: GameSituation?,
    selectedFormation: Formation,
    selectedOffenseId: String?,
    selectedDefenseId: String?,
    onFormationChange: (Formation) -> Unit,
    onSelectOffense: (OffenseConcept) -> Unit,
    onSelectDefense: (DefenseConcept) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SheetBg,
    ) {
        val pendingKo = situation?.pendingKickoff == true
        val stSituation = situation?.specialTeamsDown == true && !userOnOffense
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text(
                when {
                    userOnOffense && pendingKo -> "Kickoff"
                    userOnOffense -> "Change Play"
                    stSituation && pendingKo -> "Return Package"
                    stSituation -> "4th Down Package"
                    else -> "Change Coverage"
                },
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                when {
                    userOnOffense && pendingKo -> "Kickoff is automatic — snap when ready"
                    userOnOffense -> "Pick a formation, then a call"
                    stSituation && pendingKo ->
                        "KR: ${situation?.userKickReturnerName ?: "auto"} · return or fair catch"
                    stSituation ->
                        "PR: ${situation?.userPuntReturnerName ?: "auto"} · return, fair catch, block, or defend"
                    else -> "Pick a coverage shell"
                },
                color = Muted,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))

            if (userOnOffense) {
                if (situation?.pendingKickoff == true) {
                    val ko = Playbook.offenseById("kickoff")
                    if (ko != null) {
                        OffenseConceptRow(
                            concept = ko,
                            selected = true,
                            onClick = { onSelectOffense(ko) },
                        )
                    }
                } else {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Playbook.offenseFormations().forEach { form ->
                            FilterChipLabel(
                                label = form.displayName,
                                selected = selectedFormation == form,
                                onClick = { onFormationChange(form) },
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    val gs = situation?.toFilterState()
                    val pool = Playbook.situationalOffenseInFormation(gs, selectedFormation, true)

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(420.dp),
                    ) {
                        if (pool.isEmpty()) {
                            item {
                                Text("No plays for this formation in this situation.", color = Muted)
                            }
                        }
                        items(pool, key = { it.id }) { concept ->
                            OffenseConceptRow(
                                concept = concept,
                                selected = concept.id == selectedOffenseId,
                                onClick = { onSelectOffense(concept) },
                            )
                        }
                    }
                }
            } else {
                val gs = situation?.toFilterState()
                val pool = Playbook.situationalDefense(gs)
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(420.dp),
                ) {
                    items(pool, key = { it.id }) { concept ->
                        DefenseConceptRow(
                            concept = concept,
                            selected = concept.id == selectedDefenseId,
                            onClick = { onSelectDefense(concept) },
                        )
                    }
                }
            }
        }
    }
}

private fun GameSituation.toFilterState(): GameState {
    val gs = GameState()
    gs.down = down
    gs.yardsNeed = distance
    gs.yardLine = yardLine
    gs.gameTime = 2000
    gs.playingOT = playingOT
    gs.pendingKickoff = pendingKickoff
    gs.freeKick = freeKick
    return gs
}

@Composable
private fun FilterChipLabel(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(ChipShape)
            .background(if (selected) MaterialTheme.colorScheme.primary else Color(0xFF2A2A2A))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Text(
            label,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else Color(0xFFE5E7EB),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun OffenseConceptRow(
    concept: OffenseConcept,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RowShape)
            .background(Color(0xFF1A241C))
            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else Color(0xFF3A4A3C), RowShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                concept.displayName,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                concept.typeLabel().uppercase(),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            concept.metaLine(),
            color = Muted,
            style = MaterialTheme.typography.labelMedium,
        )
        if (concept.concept.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                concept.concept,
                color = Color(0xFFD1D5DB),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun DefenseConceptRow(
    concept: DefenseConcept,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RowShape)
            .background(Color(0xFF1A241C))
            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else Color(0xFF3A4A3C), RowShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            concept.displayName,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
        )
        if (concept.concept.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                concept.concept,
                color = Color(0xFFD1D5DB),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
