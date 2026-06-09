package jobmanagement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import job.model.Job
import job.model.flow.FlowJobLink
import job.model.result.FlowRun
import job.model.stage.FlowStage

@Composable
fun FlowJobCatalogPage(
    showFlows: Boolean,
    flowIds: List<String>,
    jobs: List<Job>,
    flowLinks: List<FlowJobLink>,
    flowStages: List<FlowStage>,
    flowRuns: List<FlowRun>,
    flowNameById: Map<String, String>,
    onShowFlows: () -> Unit,
    onShowJobs: () -> Unit,
    onSelectFlow: (String) -> Unit,
    onDeleteFlow: (String) -> Unit,
    onDeleteJob: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppButton(onClick = onShowFlows) { Text("Flows") }
            AppButton(onClick = onShowJobs) { Text("Jobs") }
        }

        if (showFlows) {
            val stageCountByFlow = flowStages.groupingBy { it.flowId }.eachCount()
            val jobCountByFlow = flowLinks.groupingBy { it.flowId }.eachCount()
            val runCountByFlow = flowRuns.groupingBy { it.flowId }.eachCount()

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(flowIds) { flowId ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    flowNameById[flowId] ?: flowId,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text("ID: $flowId")
                                Text("Jobs: ${jobCountByFlow[flowId] ?: 0}")
                                Text("Stages: ${stageCountByFlow[flowId] ?: 0}")
                                Text("Runs: ${runCountByFlow[flowId] ?: 0}")
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AppButton(onClick = { onSelectFlow(flowId) }) { Text("Open") }
                                AppButton(onClick = { onDeleteFlow(flowId) }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(jobs) { job ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(job.title, style = MaterialTheme.typography.titleMedium)
                                Text("ID: ${job.id}")
                                Text("Stage: ${job.stageId}")
                                Text("Language: ${job.script.language}")
                                Text("Enabled: ${job.isEnabled}")
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val flowId = flowLinks.firstOrNull { it.jobId == job.id }?.flowId
                                if (flowId != null) {
                                    AppButton(onClick = { onSelectFlow(flowId) }) { Text("Open Flow") }
                                }
                                AppButton(onClick = { onDeleteJob(job.id) }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }
    }
}
