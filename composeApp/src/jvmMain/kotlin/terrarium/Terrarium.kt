package terrarium

import SystemUsageInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import state.rememberTerrariumSnapshot
import terrarium.model.TerrariumFishState

@Composable
fun Terrarium(
    systemUsage: SystemUsageInfo?,
    modifier: Modifier = Modifier
) {
    val snapshot = rememberTerrariumSnapshot(systemUsage)
    var selectedFish by remember { mutableStateOf<TerrariumFishState?>(null) }

    TerrariumScene(
        snapshot = snapshot,
        modifier = modifier,
        maxVisibleFish = 100,
        onFishClick = { selectedFish = it }
    )

    selectedFish?.let { fish ->
        FishDetailsDialog(
            fish = fish,
            onDismiss = { selectedFish = null }
        )
    }
}

@Composable
private fun FishDetailsDialog(
    fish: TerrariumFishState,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(fish.label) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FishDetailRow("Type", fish.kind.name)
                FishDetailRow("Reference", fish.sourceRef)
                FishDetailRow("Status", fish.status.name)
                FishDetailRow("Health", "${fish.health}%")
                FishDetailRow("Stress", "${fish.stress}%")
                FishDetailRow("Activity", "${fish.activity}%")
                FishDetailRow("Risk", "${fish.risk}%")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun FishDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value)
    }
}
