package com.kay.cyberterrarium.jobmanagement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kay.cyberterrarium.jobmanagement.components.FlowGraphView
import com.kay.cyberterrarium.jobmanagement.components.FlowJobCatalogPage
import com.kay.cyberterrarium.jobmanagement.components.GraphSelection
import com.kay.cyberterrarium.jobmanagement.components.JobManagementHeader
import com.kay.cyberterrarium.jobmanagement.components.SelectionInspectorPanel
import job.controller.JobController
import job.model.Job
import job.model.JobDependency
import job.model.flow.FlowJobLink
import job.model.result.FlowRun
import job.model.script.ScriptLanguage
import job.model.stage.BarrierMode
import job.model.stage.FlowStage
import job.model.stage.StageFailMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun JobManagement() {
    val controller = remember { JobController.createDefault() }
    val scope = rememberCoroutineScope()

    var selectedFlowId by remember { mutableStateOf<String?>(null) }
    var maxWorkersText by remember { mutableStateOf("2") }
    var currentPage by remember { mutableStateOf(JobManagementPage.GRAPH) }
    var catalogShowsFlows by remember { mutableStateOf(true) }
    var selection by remember { mutableStateOf<GraphSelection?>(null) }
    var message by remember { mutableStateOf("") }

    var allJobs by remember { mutableStateOf<List<Job>>(emptyList()) }
    var allFlowLinks by remember { mutableStateOf<List<FlowJobLink>>(emptyList()) }
    var allFlowRuns by remember { mutableStateOf<List<FlowRun>>(emptyList()) }
    var allFlowStages by remember { mutableStateOf<List<FlowStage>>(emptyList()) }
    var currentFlowDependencies by remember { mutableStateOf<List<JobDependency>>(emptyList()) }

    var showCreateFlowDialog by remember { mutableStateOf(false) }
    var showCreateStageDialog by remember { mutableStateOf(false) }
    var showCreateJobDialog by remember { mutableStateOf(false) }

    val flowIds = remember(allFlowLinks, allFlowStages, allFlowRuns) {
        buildSet {
            allFlowLinks.mapTo(this) { it.flowId }
            allFlowStages.mapTo(this) { it.flowId }
            allFlowRuns.mapTo(this) { it.flowId }
        }.filter { it.isNotBlank() }.sorted()
    }

    val currentFlowLinks = remember(selectedFlowId, allFlowLinks) {
        val id = selectedFlowId
        if (id == null) emptyList() else allFlowLinks.filter { it.flowId == id }.sortedBy { it.position }
    }
    val currentFlowStages = remember(selectedFlowId, allFlowStages) {
        val id = selectedFlowId
        if (id == null) emptyList() else allFlowStages.filter { it.flowId == id }.sortedBy { it.order }
    }
    val currentFlowJobsById = remember(currentFlowLinks, allJobs) {
        val allById = allJobs.associateBy { it.id }
        currentFlowLinks.mapNotNull { allById[it.jobId] }.associateBy { it.id }
    }

    fun refresh() {
        scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) {
                    val jobs = controller.listJobs()
                    val links = controller.listAllFlowJobs()
                    val runs = controller.listFlowRuns()
                    val stages = controller.listAllFlowStages()

                    val targetFlowId = selectedFlowId
                    val dependencies = if (targetFlowId == null) {
                        emptyList()
                    } else {
                        val flowJobIds = links
                            .filter { it.flowId == targetFlowId }
                            .map { it.jobId }
                        controller.listJobDependenciesByJobIds(flowJobIds)
                    }
                    DashboardSnapshot(jobs, links, runs, stages, dependencies)
                }

                allJobs = snapshot.jobs
                allFlowLinks = snapshot.flowLinks
                allFlowRuns = snapshot.flowRuns
                allFlowStages = snapshot.flowStages
                currentFlowDependencies = snapshot.dependencies
            } catch (e: Exception) {
                message = "Refresh failed: ${e.message}"
            }
        }
    }

    fun selectFlow(flowId: String?) {
        selectedFlowId = flowId?.takeIf { it.isNotBlank() }
        selection = null
        refresh()
    }

    fun selectedJob(): Job? = when (val selected = selection) {
        is GraphSelection.JobSelection -> currentFlowJobsById[selected.jobId]
        else -> null
    }

    fun selectedStage(): FlowStage? = when (val selected = selection) {
        is GraphSelection.StageSelection -> currentFlowStages.firstOrNull { it.id == selected.stageId }
        else -> null
    }

    fun selectedDependency(): JobDependency? = when (val selected = selection) {
        is GraphSelection.DependencySelection -> currentFlowDependencies.firstOrNull {
            it.jobId == selected.jobId && it.upstreamJobId == selected.upstreamJobId
        }
        else -> null
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    if (showCreateFlowDialog) {
        CreateFlowDialog(
            onDismiss = { showCreateFlowDialog = false },
            onCreate = { flowId, stageId, stageName ->
                scope.launch {
                    withContext(Dispatchers.IO) {
                        controller.createFlow(flowId, stageId, stageName)
                    }
                    message = "Flow created: $flowId"
                    selectFlow(flowId)
                    showCreateFlowDialog = false
                }
            }
        )
    }

    if (showCreateStageDialog) {
        val defaultFlowId = selectedFlowId ?: flowIds.firstOrNull().orEmpty()
        CreateStageDialog(
            defaultFlowId = defaultFlowId,
            onDismiss = { showCreateStageDialog = false },
            onCreate = { flowId, stageId, stageName, order ->
                scope.launch {
                    withContext(Dispatchers.IO) {
                        controller.createOrUpdateFlowStage(
                            flowId,
                            stageId,
                            stageName,
                            order,
                            BarrierMode.SOFT,
                            StageFailMode.STOP
                        )
                    }
                    message = "Stage created: $stageId"
                    selectFlow(flowId)
                    showCreateStageDialog = false
                }
            }
        )
    }

    if (showCreateJobDialog) {
        val defaultFlowId = selectedFlowId ?: flowIds.firstOrNull().orEmpty()
        val defaultStageId = currentFlowStages.firstOrNull()?.id ?: "default-stage"
        CreateJobDialog(
            defaultFlowId = defaultFlowId,
            defaultStageId = defaultStageId,
            defaultPosition = controller.suggestNextPosition(defaultFlowId),
            onDismiss = { showCreateJobDialog = false },
            onCreate = { flowId, stageId, title, description, languageText, script, position ->
                scope.launch {
                    try {
                        val language = runCatching { ScriptLanguage.valueOf(languageText.trim().uppercase()) }
                            .getOrElse { ScriptLanguage.SHELL }
                        withContext(Dispatchers.IO) {
                            controller.createJobForFlow(
                                flowId,
                                stageId,
                                title,
                                description,
                                language,
                                script,
                                position
                            )
                        }
                        message = "Job created: $title"
                        selectFlow(flowId)
                        showCreateJobDialog = false
                    } catch (e: Exception) {
                        message = "Create job failed: ${e.message}"
                    }
                }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .safeContentPadding()
            .padding(10.dp)
    ) {
        Spacer(
            modifier = Modifier
                .width(64.dp)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f))
        )

        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            JobManagementHeader(
                currentPageIsGraph = currentPage == JobManagementPage.GRAPH,
                selectedFlowId = selectedFlowId,
                flowIds = flowIds,
                maxWorkersText = maxWorkersText,
                onMaxWorkersChange = { maxWorkersText = it },
                onSelectFlow = { selectFlow(it) },
                onClearFlow = { selectFlow(null) },
                onOpenCreateFlow = { showCreateFlowDialog = true },
                onOpenCreateJob = { showCreateJobDialog = true },
                onOpenCreateStage = { showCreateStageDialog = true },
                onRunFlow = {
                    val flowId = selectedFlowId ?: return@JobManagementHeader
                    scope.launch {
                        try {
                            val workers = maxWorkersText.toIntOrNull() ?: 1
                            val run = withContext(Dispatchers.IO) { controller.runFlow(flowId, workers) }
                            message = "Flow run #${run.id} ${run.status}"
                            refresh()
                        } catch (e: Exception) {
                            message = "Run failed: ${e.message}"
                        }
                    }
                },
                onRefresh = { refresh() },
                onTogglePage = {
                    currentPage = if (currentPage == JobManagementPage.GRAPH) {
                        JobManagementPage.CATALOG
                    } else {
                        JobManagementPage.GRAPH
                    }
                }
            )

            if (message.isNotBlank()) {
                Text(message, color = MaterialTheme.colorScheme.primary)
            }

            when (currentPage) {
                JobManagementPage.CATALOG -> {
                    FlowJobCatalogPage(
                        showFlows = catalogShowsFlows,
                        flowIds = flowIds,
                        jobs = allJobs,
                        flowLinks = allFlowLinks,
                        flowStages = allFlowStages,
                        flowRuns = allFlowRuns,
                        onShowFlows = { catalogShowsFlows = true },
                        onShowJobs = { catalogShowsFlows = false },
                        onSelectFlow = {
                            selectFlow(it)
                            currentPage = JobManagementPage.GRAPH
                        },
                        onDeleteFlow = { flowId ->
                            scope.launch {
                                withContext(Dispatchers.IO) { controller.deleteFlow(flowId) }
                                if (selectedFlowId == flowId) {
                                    selectedFlowId = null
                                    selection = null
                                }
                                message = "Flow deleted: $flowId"
                                refresh()
                            }
                        },
                        onDeleteJob = { jobId ->
                            scope.launch {
                                withContext(Dispatchers.IO) { controller.deleteJob(jobId) }
                                message = "Job deleted: $jobId"
                                selection = null
                                refresh()
                            }
                        }
                    )
                }

                JobManagementPage.GRAPH -> {
                    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                            FlowGraphView(
                                selectedFlowId = selectedFlowId,
                                flowStages = currentFlowStages,
                                flowJobLinks = currentFlowLinks,
                                jobsById = currentFlowJobsById,
                                dependencies = currentFlowDependencies,
                                selection = selection,
                                onSelect = { selection = it }
                            )
                        }

                        if (selection != null) {
                            SelectionInspectorPanel(
                                selectedJob = selectedJob(),
                                selectedStage = selectedStage(),
                                selectedDependency = selectedDependency(),
                                onClose = { selection = null },
                                onSaveJob = { jobId, title, description, stageId, enabled ->
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            controller.updateJob(jobId, title, description, stageId, enabled)
                                        }
                                        message = "Job updated: $jobId"
                                        refresh()
                                    }
                                },
                                onDeleteJob = { jobId ->
                                    scope.launch {
                                        withContext(Dispatchers.IO) { controller.deleteJob(jobId) }
                                        message = "Job deleted: $jobId"
                                        selection = null
                                        refresh()
                                    }
                                },
                                onSaveStage = { stageId, name, order, barrierMode, failMode ->
                                    val flowId = selectedFlowId ?: return@SelectionInspectorPanel
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            controller.createOrUpdateFlowStage(
                                                flowId,
                                                stageId,
                                                name,
                                                order,
                                                barrierMode,
                                                failMode
                                            )
                                        }
                                        message = "Stage updated: $stageId"
                                        refresh()
                                    }
                                },
                                onDeleteStage = { stageId ->
                                    scope.launch {
                                        withContext(Dispatchers.IO) { controller.deleteFlowStage(stageId) }
                                        message = "Stage deleted: $stageId"
                                        selection = null
                                        refresh()
                                    }
                                },
                                onDeleteDependency = { jobId, upstreamJobId ->
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            controller.deleteJobDependency(jobId, upstreamJobId)
                                        }
                                        message = "Dependency deleted: $upstreamJobId -> $jobId"
                                        selection = null
                                        refresh()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateFlowDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String) -> Unit
) {
    var flowId by remember { mutableStateOf("demo-flow") }
    var stageId by remember { mutableStateOf("demo-stage-1") }
    var stageName by remember { mutableStateOf("Stage 1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Flow") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = flowId, onValueChange = { flowId = it }, label = { Text("Flow ID") })
                OutlinedTextField(value = stageId, onValueChange = { stageId = it }, label = { Text("Initial Stage ID") })
                OutlinedTextField(value = stageName, onValueChange = { stageName = it }, label = { Text("Initial Stage Name") })
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(flowId.trim(), stageId.trim(), stageName.trim()) }) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CreateStageDialog(
    defaultFlowId: String,
    onDismiss: () -> Unit,
    onCreate: (String, String, String, Int) -> Unit
) {
    var flowId by remember(defaultFlowId) { mutableStateOf(defaultFlowId) }
    var stageId by remember { mutableStateOf("stage-${System.currentTimeMillis() % 10000}") }
    var stageName by remember { mutableStateOf("New Stage") }
    var orderText by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Stage") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = flowId, onValueChange = { flowId = it }, label = { Text("Flow ID") })
                OutlinedTextField(value = stageId, onValueChange = { stageId = it }, label = { Text("Stage ID") })
                OutlinedTextField(value = stageName, onValueChange = { stageName = it }, label = { Text("Name") })
                OutlinedTextField(value = orderText, onValueChange = { orderText = it }, label = { Text("Order") })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val order = orderText.toIntOrNull() ?: 0
                onCreate(flowId.trim(), stageId.trim(), stageName.trim(), order)
            }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CreateJobDialog(
    defaultFlowId: String,
    defaultStageId: String,
    defaultPosition: Int,
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String, String, String, Int) -> Unit
) {
    var flowId by remember(defaultFlowId) { mutableStateOf(defaultFlowId) }
    var stageId by remember(defaultStageId) { mutableStateOf(defaultStageId) }
    var title by remember { mutableStateOf("Untitled Job") }
    var description by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("SHELL") }
    var script by remember { mutableStateOf("echo hello") }
    var positionText by remember(defaultPosition) { mutableStateOf(defaultPosition.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Job") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = flowId, onValueChange = { flowId = it }, label = { Text("Flow ID") })
                OutlinedTextField(value = stageId, onValueChange = { stageId = it }, label = { Text("Stage ID") })
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
                OutlinedTextField(value = language, onValueChange = { language = it }, label = { Text("Language") })
                OutlinedTextField(value = positionText, onValueChange = { positionText = it }, label = { Text("Position") })
                OutlinedTextField(value = script, onValueChange = { script = it }, label = { Text("Script") })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val position = positionText.toIntOrNull() ?: defaultPosition
                onCreate(flowId.trim(), stageId.trim(), title.trim(), description, language, script, position)
            }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private enum class JobManagementPage {
    GRAPH,
    CATALOG
}

private data class DashboardSnapshot(
    val jobs: List<Job>,
    val flowLinks: List<FlowJobLink>,
    val flowRuns: List<FlowRun>,
    val flowStages: List<FlowStage>,
    val dependencies: List<JobDependency>
)
