package jobmanagement

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import job.model.result.FlowRun
import job.model.result.FlowRunJob
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun FlowRunResultsPage(
    flowRuns: List<FlowRun>,
    flowRunJobs: List<FlowRunJob>,
    selectedFlowId: String?,
    onSelectFlow: (String) -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())
    val runs = flowRuns
        .filter { selectedFlowId == null || it.flowId == selectedFlowId }
        .sortedByDescending { it.id }
    val runJobsByRunId = flowRunJobs.groupBy { it.runId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Run Results", style = MaterialTheme.typography.titleLarge)
        Text(
            "Selected Flow: ${selectedFlowId ?: "All"}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (runs.isEmpty()) {
            Text("No run results yet. Click Run on a flow to generate one.")
            return@Column
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(runs) { run ->
                val jobLogs = runJobsByRunId[run.id].orEmpty()
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Run #${run.id}", style = MaterialTheme.typography.titleMedium)
                            AppButton(
                                onClick = { onSelectFlow(run.flowId) },
                                variant = AppButtonVariant.MUTED
                            ) { Text("Open Flow") }
                        }
                        Text("Flow: ${run.flowId}")
                        Text("Status: ${run.status}")
                        Text("Started: ${run.startedAt?.let { formatter.format(it) } ?: "-"}")
                        Text("Ended: ${run.endedAt?.let { formatter.format(it) } ?: "-"}")

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF11161F),
                                contentColor = Color(0xFFD6DEEB)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 72.dp)
                                .border(1.dp, Color(0xFF2B3444))
                        ) {
                            if (jobLogs.isEmpty()) {
                                Text(
                                    text = "No job logs stored for this run.",
                                    modifier = Modifier.padding(10.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFC8CDD8),
                                    fontFamily = FontFamily.Monospace
                                )
                            } else {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    jobLogs.forEach { log ->
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = "[job:${log.jobId}] status=${log.status} exit=${log.exitCode} duration=${log.durationMs}ms",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFFB7C4FF),
                                                fontFamily = FontFamily.Monospace
                                            )
                                            if (log.errorMessage.isNotBlank()) {
                                                Text(
                                                    text = "error: ${log.errorMessage}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFFFF8A80),
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                            if (log.stdoutText.isNotBlank()) {
                                                Text(
                                                    text = "stdout:\n${log.stdoutText}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFFC8CDD8),
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                            if (log.stderrText.isNotBlank()) {
                                                Text(
                                                    text = "stderr:\n${log.stderrText}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFFFFB4AB),
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
