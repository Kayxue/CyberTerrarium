package com.kay.cyberterrarium.jobmanagement.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import job.model.Job
import job.model.JobDependency
import job.model.stage.BarrierMode
import job.model.stage.FlowStage
import job.model.stage.StageFailMode

@Composable
fun SelectionInspectorPanel(
    selectedJob: Job?,
    selectedStage: FlowStage?,
    flowJobs: List<Job>,
    flowStages: List<FlowStage>,
    selectedDependency: JobDependency?,
    onClose: () -> Unit,
    onSaveJob: (String, String, String, String, Int, Boolean) -> Unit,
    onEditJobScript: (String) -> Unit,
    onDeleteJob: (String) -> Unit,
    onSaveStage: (String, String, Int, BarrierMode, StageFailMode) -> Unit,
    onDeleteStage: (String) -> Unit,
    onDeleteDependency: (String, String) -> Unit
) {
    Card(
        modifier = Modifier
            .width(330.dp)
            .fillMaxHeight()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Inspector", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onClose,
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .width(32.dp).height(32.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Close")
                }
            }

            if (selectedJob != null) {
                JobInspector(
                    selectedJob = selectedJob,
                    flowJobs = flowJobs,
                    flowStages = flowStages,
                    onSave = onSaveJob,
                    onEditScript = onEditJobScript,
                    onDelete = onDeleteJob
                )
            } else if (selectedStage != null) {
                StageInspector(
                    selectedStage = selectedStage,
                    onSave = onSaveStage,
                    onDelete = onDeleteStage
                )
            } else if (selectedDependency != null) {
                DependencyInspector(
                    selectedDependency = selectedDependency,
                    onDelete = onDeleteDependency
                )
            }
        }
    }
}

@Composable
private fun JobInspector(
    selectedJob: Job,
    flowJobs: List<Job>,
    flowStages: List<FlowStage>,
    onSave: (String, String, String, String, Int, Boolean) -> Unit,
    onEditScript: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    var title by remember(selectedJob.id) { mutableStateOf(selectedJob.title) }
    var description by remember(selectedJob.id) { mutableStateOf(selectedJob.description) }
    var stageId by remember(selectedJob.id) { mutableStateOf(selectedJob.stageId) }
    var orderValue by remember(selectedJob.id) { mutableStateOf(selectedJob.order) }
    var enabled by remember(selectedJob.id) { mutableStateOf(selectedJob.isEnabled) }

    val orderOptions = remember(stageId, flowJobs, selectedJob.id) {
        val ordersInStage = flowJobs.filter { it.stageId == stageId }.map { it.order }.toSet().toMutableSet()
        ordersInStage.add(orderValue)
        val next = (ordersInStage.maxOrNull() ?: 0) + 1
        ordersInStage.add(next)
        ordersInStage.toList().sorted()
    }

    Text("Job", style = MaterialTheme.typography.titleSmall)
    Text("ID: ${selectedJob.id}")
    OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        label = { Text("Title") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = description,
        onValueChange = { description = it },
        label = { Text("Description") },
        modifier = Modifier.fillMaxWidth()
    )
    SelectDropdownField(
        label = "Stage",
        value = flowStages.firstOrNull { it.id == stageId }?.let { "${it.displayName} (${it.id})" } ?: stageId,
        options = flowStages.map { "${it.displayName} (${it.id})" },
        onSelect = { selectedText ->
            val selected = flowStages.firstOrNull { "${it.displayName} (${it.id})" == selectedText }
            if (selected != null) {
                stageId = selected.id
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
    SelectDropdownField(
        label = "Order",
        value = orderValue.toString(),
        options = orderOptions.map { it.toString() },
        onSelect = { selected ->
            orderValue = selected.toIntOrNull() ?: orderValue
        },
        modifier = Modifier.fillMaxWidth()
    )
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Enabled: ")
        Switch(checked = enabled, onCheckedChange = { enabled = it })
    }
    AppButton(
        onClick = { onEditScript(selectedJob.id) },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
    ) { Text("Edit Script") }
    Row(horizontalArrangement = Arrangement.SpaceBetween) {
        AppButton(
            onClick = { onSave(selectedJob.id, title, description, stageId, orderValue, enabled) },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
        ) { Text("Save") }
        Spacer(modifier = Modifier.width(8.dp))
        AppButton(
            onClick = { onDelete(selectedJob.id) },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) { Text("Delete") }
    }
}

@Composable
private fun StageInspector(
    selectedStage: FlowStage,
    onSave: (String, String, Int, BarrierMode, StageFailMode) -> Unit,
    onDelete: (String) -> Unit
) {
    var name by remember(selectedStage.id) { mutableStateOf(selectedStage.displayName) }
    var orderText by remember(selectedStage.id) { mutableStateOf(selectedStage.order.toString()) }
    var barrierMode by remember(selectedStage.id) { mutableStateOf(selectedStage.barrierMode) }
    var failMode by remember(selectedStage.id) { mutableStateOf(selectedStage.failMode) }

    Text("Stage", style = MaterialTheme.typography.titleSmall)
    Text("ID: ${selectedStage.id}")
    Text("Flow: ${selectedStage.flowId}")
    OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Name") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = orderText,
        onValueChange = { orderText = it },
        label = { Text("Order") },
        modifier = Modifier.fillMaxWidth()
    )

    SelectDropdownField(
        label = "Barrier Mode",
        value = barrierMode.name,
        options = BarrierMode.values().map { it.name },
        onSelect = { selected -> barrierMode = BarrierMode.valueOf(selected) },
        modifier = Modifier.fillMaxWidth()
    )
    SelectDropdownField(
        label = "Fail Mode",
        value = failMode.name,
        options = StageFailMode.values().map { it.name },
        onSelect = { selected -> failMode = StageFailMode.valueOf(selected) },
        modifier = Modifier.fillMaxWidth()
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppButton(
            onClick = {
                val order = orderText.toIntOrNull() ?: selectedStage.order
                onSave(selectedStage.id, name, order, barrierMode, failMode)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
            modifier = Modifier.weight(1f)
        ) { Text("Save") }
        AppButton(
            onClick = { onDelete(selectedStage.id) },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.weight(1f)
        ) { Text("Delete") }
    }
}

@Composable
private fun DependencyInspector(
    selectedDependency: JobDependency,
    onDelete: (String, String) -> Unit
) {
    Text("Dependency", style = MaterialTheme.typography.titleSmall)
    Text("Job: ${selectedDependency.jobId}")
    Text("Depends on: ${selectedDependency.upstreamJobId}")
    AppButton(onClick = {
        onDelete(selectedDependency.jobId, selectedDependency.upstreamJobId)
    }) { Text("Delete Dependency") }
}
