package achijones.footballcoach.ui.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import CFBsimPack.engine.BoxScoreLine
import CFBsimPack.engine.GameSituation
import CFBsimPack.engine.PlayLogEntry

private val CardShape = RoundedCornerShape(12.dp)
private val Muted = Color(0xFF9CA3AF)

@Composable
fun CoachLogTab(situation: GameSituation) {
    val entries = situation.playLog.asReversed()
    if (entries.isEmpty()) {
        Text("No plays yet.", color = Muted, modifier = Modifier.padding(16.dp))
        return
    }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        items(entries.size) { idx ->
            PlayLogRow(entries[idx])
        }
    }
}

@Composable
private fun PlayLogRow(entry: PlayLogEntry, modifier: Modifier = Modifier) {
    val why = entry.snapTrace?.summaryBullets.orEmpty()
    var expanded by remember(entry.clockLabel, entry.logLine, entry.yardsGained) { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Color(0xFF121A14))
            .border(1.dp, Color(0xFF3A4A3C), CardShape)
            .then(
                if (why.isNotEmpty()) {
                    Modifier.clickable { expanded = !expanded }
                } else {
                    Modifier
                },
            )
            .padding(12.dp),
    ) {
        val distanceLabel = if (entry.yardLineBefore + entry.distance >= 100) "Goal" else "${entry.distance}"
        Text(
            "Q${entry.quarter} ${entry.clockLabel} · ${ordinal(entry.down)} & $distanceLabel",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier.height(4.dp))
        Text(entry.logLine, color = Color(0xFFE5E7EB), style = MaterialTheme.typography.bodyMedium)
        if (entry.offenseConceptName.isNotBlank()) {
            Spacer(modifier.height(4.dp))
            Text(
                "${entry.offenseConceptName} vs ${entry.defenseConceptName}",
                color = Muted,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        if (why.isNotEmpty()) {
            Spacer(modifier.height(4.dp))
            Text(
                if (expanded) "Why (hide)" else "Why (show)",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (expanded) {
                Spacer(modifier.height(4.dp))
                why.take(4).forEach { bullet ->
                    Text(
                        "- $bullet",
                        color = Muted,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
fun CoachBoxScoreTab(situation: GameSituation) {
    val lines = situation.boxScore
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        item {
            TeamTotals(situation)
            Spacer(Modifier.height(12.dp))
            if (lines.isEmpty()) {
                Text("Stats will appear after snaps.", color = Muted)
            } else {
                Text("PLAYERS", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
            }
        }
        items(lines, key = { "${it.home}-${it.position}-${it.name}" }) { line ->
            BoxScoreRow(line)
        }
    }
}

@Composable
private fun TeamTotals(sit: GameSituation) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TeamTotalCard(sit.awayAbbr, sit.awayYards, sit.awayTOs, Modifier.weight(1f))
        TeamTotalCard(sit.homeAbbr, sit.homeYards, sit.homeTOs, Modifier.weight(1f))
    }
}

@Composable
private fun TeamTotalCard(abbr: String, yards: Int, tos: Int, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(CardShape)
            .background(Color(0xFF121A14))
            .border(1.dp, Color(0xFF3A4A3C), CardShape)
            .padding(12.dp),
    ) {
        Text(abbr, color = Color.White, fontWeight = FontWeight.Bold)
        Text("$yards yards", color = Muted, style = MaterialTheme.typography.bodyMedium)
        Text("$tos TO", color = Muted, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun BoxScoreRow(line: BoxScoreLine) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1A241C))
            .padding(10.dp),
    ) {
        Text(
            "${line.position} ${line.name} · ${if (line.home) "HOME" else "AWAY"}",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelLarge,
        )
        val parts = mutableListOf<String>()
        if (line.passAtt > 0) {
            parts += "${line.passComp}/${line.passAtt}, ${line.passYards} yds, ${line.passTd} TD, ${line.passInt} INT"
        }
        if (line.rushAtt > 0) {
            parts += "${line.rushAtt} rush, ${line.rushYards} yds, ${line.rushTd} TD"
        }
        if (line.receptions > 0) {
            parts += "${line.receptions} rec, ${line.recYards} yds, ${line.recTd} TD"
        }
        if (line.prAtt > 0) {
            parts += "${line.prAtt} PR, ${line.prYards} yds, ${line.prTd} TD"
        }
        if (line.krAtt > 0) {
            parts += "${line.krAtt} KR, ${line.krYards} yds, ${line.krTd} TD"
        }
        if (line.puntAtt > 0) {
            parts += "${line.puntAtt} punt, ${line.puntYards} yds"
        }
        if (line.fgAtt > 0) {
            parts += "FG ${line.fgMade}/${line.fgAtt}"
        }
        if (line.xpAtt > 0) {
            parts += "XP ${line.xpMade}/${line.xpAtt}"
        }
        if (line.tackles > 0 || line.tfl > 0 || line.sacksDef > 0 || line.defInt > 0
            || line.passDef > 0 || line.forcedFumbles > 0 || line.fumbleRec > 0
        ) {
            val def = mutableListOf<String>()
            if (line.tackles > 0) def += "${line.tackles} Tck"
            if (line.tfl > 0) def += "${line.tfl} TFL"
            if (line.sacksDef > 0) def += "${line.sacksDef} Sk"
            if (line.defInt > 0) def += "${line.defInt} INT"
            if (line.passDef > 0) def += "${line.passDef} PD"
            if (line.forcedFumbles > 0) def += "${line.forcedFumbles} FF"
            if (line.fumbleRec > 0) def += "${line.fumbleRec} FR"
            parts += def.joinToString(", ")
        }
        Text(parts.joinToString(" · "), color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}

private fun ordinal(d: Int): String = when (d) {
    1 -> "1st"
    2 -> "2nd"
    3 -> "3rd"
    else -> "${d}th"
}
