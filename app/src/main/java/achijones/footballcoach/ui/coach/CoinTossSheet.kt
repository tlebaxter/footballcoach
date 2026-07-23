package achijones.footballcoach.ui.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import achijones.footballcoach.ui.theme.FcOnPrimary
import achijones.footballcoach.ui.theme.FcPrimary

private val SheetBg = Color(0xFF121A14)
private val ChipShape = RoundedCornerShape(10.dp)
private val Muted = Color(0xFF9CA3AF)
private val GhostBorder = Color(0xFF3A4A3C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinTossSheet(
    winnerAbbr: String,
    onConfirm: (receive: Boolean, defendLeft: Boolean) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var receive by remember { mutableStateOf(true) }
    var defendLeft by remember { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = { /* must elect */ },
        sheetState = sheetState,
        containerColor = SheetBg,
    ) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 28.dp)) {
            Text(
                "Coin Toss",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "$winnerAbbr won the toss — choose ball and end",
                color = Muted,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(16.dp))

            Text(
                "Ball",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TossOptionChip(
                    label = "Receive",
                    selected = receive,
                    onClick = { receive = true },
                    modifier = Modifier.weight(1f),
                )
                TossOptionChip(
                    label = "Defer",
                    selected = !receive,
                    onClick = { receive = false },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "Defend",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TossOptionChip(
                    label = "Left end",
                    selected = defendLeft,
                    onClick = { defendLeft = true },
                    modifier = Modifier.weight(1f),
                )
                TossOptionChip(
                    label = "Right end",
                    selected = !defendLeft,
                    onClick = { defendLeft = false },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { onConfirm(receive, defendLeft) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = FcPrimary,
                    contentColor = FcOnPrimary,
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text("Confirm election", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun TossOptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (selected) FcPrimary.copy(alpha = 0.35f) else Color(0xFF1A241C)
    val border = if (selected) FcPrimary else GhostBorder
    Text(
        label,
        color = Color.White,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(ChipShape)
            .background(bg)
            .border(1.dp, border, ChipShape)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 10.dp),
    )
}
