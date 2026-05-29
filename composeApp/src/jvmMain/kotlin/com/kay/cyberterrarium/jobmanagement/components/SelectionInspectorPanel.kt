package com.kay.cyberterrarium.jobmanagement.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
    selectedDependency: JobDependency?,
    onClose: () -> Unit,
    onSaveJob: (String, String, String, String, Boolean) -> Unit,
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
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Inspector", style = MaterialTheme.typography.titleMedium)
                AppButton(onClick = onClose) { Text("Close") }
            }

            if (selectedJob != null) {
                JobInspector(
                    selectedJob = selectedJob,
                    onSave = onSaveJob,
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
    onSave: (String, String, String, String, Boolean) -> Unit,
    onDelete: (String) -> Unit
) {
    var title by remember(selectedJob.id) { mutableStateOf(selectedJob.title) }
    var description by remember(selectedJob.id) { mutableStateOf(selectedJob.description) }
    var stageId by remember(selectedJob.id) { mutableStateOf(selectedJob.stageId) }
    var enabled by remember(selectedJob.id) { mutableStateOf(selectedJob.isEnabled) }

    Text("Job", style = MaterialTheme.typography.titleSmall)
    Text("ID: ${selectedJob.id}")
    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
    OutlinedTextField(
        value = description,
        onValueChange = { description = it },
        label = { Text("Description") }
    )
    OutlinedTextField(value = stageId, onValueChange = { stageId = it }, label = { Text("Stage ID") })
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Checkbox(checked = enabled, onCheckedChange = { enabled = it })
        Text("Enabled")
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppButton(onClick = { onSave(selectedJob.id, title, description, stageId, enabled) }) { Text("Save") }
        AppButton(onClick = { onDelete(selectedJob.id) }) { Text("Delete") }
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
    var barrierExpanded by remember(selectedStage.id) { mutableStateOf(false) }
    var failExpanded by remember(selectedStage.id) { mutableStateOf(false) }

    Text("Stage", style = MaterialTheme.typography.titleSmall)
    Text("ID: ${selectedStage.id}")
    Text("Flow: ${selectedStage.flowId}")
    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
    OutlinedTextField(
        value = orderText,
        onValueChange = { orderText = it },
        label = { Text("Order") }
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppButton(onClick = { barrierExpanded = true }) { Text("Barrier: ${barrierMode.name}") }
        DropdownMenu(
            expanded = barrierExpanded,
            onDismissRequest = { barrierExpanded = false }
        ) {
            BarrierMode.values().forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.name) },
                    onClick = {
                        barrierMode = mode
                        barrierExpanded = false
                    }
                )
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppButton(onClick = { failExpanded = true }) { Text("Fail Mode: ${failMode.name}") }
        DropdownMenu(
            expanded = failExpanded,
            onDismissRequest = { failExpanded = false }
        ) {
            StageFailMode.values().forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.name) },
                    onClick = {
                        failMode = mode
                        failExpanded = false
                    }
                )
            }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppButton(onClick = {
            val order = orderText.toIntOrNull() ?: selectedStage.order
            onSave(selectedStage.id, name, order, barrierMode, failMode)
        }) { Text("Save") }
        AppButton(onClick = { onDelete(selectedStage.id) }) { Text("Delete") }
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
