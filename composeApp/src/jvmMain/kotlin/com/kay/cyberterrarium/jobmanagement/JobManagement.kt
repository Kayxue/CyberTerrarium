package com.kay.cyberterrarium.jobmanagement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kay.cyberterrarium.jobmanagement.components.*
import job.controller.JobController
import job.model.Job
import job.model.JobDependency
import job.model.flow.FlowJobLink
import job.model.result.FlowRun
import job.model.result.FlowRunJob
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
    var scriptEditorJobId by remember { mutableStateOf<String?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    var allJobs by remember { mutableStateOf<List<Job>>(emptyList()) }
    var allFlowLinks by remember { mutableStateOf<List<FlowJobLink>>(emptyList()) }
    var allFlowRuns by remember { mutableStateOf<List<FlowRun>>(emptyList()) }
    var allFlowRunJobs by remember { mutableStateOf<List<FlowRunJob>>(emptyList()) }
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
    val scriptEditorJob = remember(scriptEditorJobId, allJobs) {
        val id = scriptEditorJobId ?: return@remember null
        allJobs.firstOrNull { it.id == id }
    }

    fun refresh() {
        scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) {
                    val jobs = controller.listJobs()
                    val links = controller.listAllFlowJobs()
                    val runs = controller.listFlowRuns()
                    val runJobs = controller.listFlowRunJobs(runs.map { it.id })
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
                    DashboardSnapshot(jobs, links, runs, runJobs, stages, dependencies)
                }

                allJobs = snapshot.jobs
                allFlowLinks = snapshot.flowLinks
                allFlowRuns = snapshot.flowRuns
                allFlowRunJobs = snapshot.flowRunJobs
                allFlowStages = snapshot.flowStages
                currentFlowDependencies = snapshot.dependencies
            } catch (e: Exception) {
                snackbarMessage = "Refresh failed: ${e.message}"
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

    LaunchedEffect(snackbarMessage) {
        val text = snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(text)
        snackbarMessage = null
    }

    if (showCreateFlowDialog) {
        CreateFlowDialog(
            onDismiss = { showCreateFlowDialog = false },
            onCreate = { flowName ->
                scope.launch {
                    val createdFlowId = withContext(Dispatchers.IO) {
                        controller.createFlow(flowName)
                    }
                    snackbarMessage = "Flow created: $flowName ($createdFlowId)"
                    selectFlow(createdFlowId)
                    showCreateFlowDialog = false
                }
            }
        )
    }

    if (showCreateStageDialog) {
        val defaultFlowId = selectedFlowId ?: flowIds.firstOrNull().orEmpty()
        CreateStageDialog(
            defaultFlowId = defaultFlowId,
            flowIds = flowIds,
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
                    snackbarMessage = "Stage created: $stageId"
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
            stageOptions = currentFlowStages,
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
                        snackbarMessage = "Job created: $title"
                        selectFlow(flowId)
                        showCreateJobDialog = false
                    } catch (e: Exception) {
                        snackbarMessage = "Create job failed: ${e.message}"
                    }
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .safeContentPadding()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            JobManagementHeader(
                currentPageIsGraph = currentPage == JobManagementPage.GRAPH,
                currentPageIsCatalog = currentPage == JobManagementPage.CATALOG,
                currentPageIsResults = currentPage == JobManagementPage.RESULTS,
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
                            snackbarMessage = "Flow run #${run.id} ${run.status}"
                            refresh()
                        } catch (e: Exception) {
                            snackbarMessage = "Run failed: ${e.message}"
                        }
                    }
                },
                onRefresh = { refresh() },
                onOpenCatalog = { currentPage = JobManagementPage.CATALOG },
                onOpenResults = { currentPage = JobManagementPage.RESULTS },
                onOpenGraph = { currentPage = JobManagementPage.GRAPH }
            )

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
                                snackbarMessage = "Flow deleted: $flowId"
                                refresh()
                            }
                        },
                        onDeleteJob = { jobId ->
                            scope.launch {
                                withContext(Dispatchers.IO) { controller.deleteJob(jobId) }
                                snackbarMessage = "Job deleted: $jobId"
                                selection = null
                                refresh()
                            }
                        }
                    )
                }

                JobManagementPage.GRAPH -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            FlowGraphView(
                                selectedFlowId = selectedFlowId,
                                flowStages = currentFlowStages,
                                flowJobLinks = currentFlowLinks,
                                jobsById = currentFlowJobsById,
                                dependencies = currentFlowDependencies,
                                selection = selection,
                                onSelect = { selection = it },
                                onCreateDependency = { jobId, upstreamJobId ->
                                    scope.launch {
                                        try {
                                            withContext(Dispatchers.IO) {
                                                controller.saveJobDependency(jobId, upstreamJobId)
                                            }
                                            snackbarMessage = "Dependency added: $upstreamJobId -> $jobId"
                                            refresh()
                                        } catch (e: Exception) {
                                            snackbarMessage = "Add dependency failed: ${e.message}"
                                        }
                                    }
                                },
                                onDependencyRejected = { reason ->
                                    snackbarMessage = reason
                                },
                                onDependencyControlPointChanged = { jobId, upstreamJobId, bendXDp, bendYDp ->
                                    currentFlowDependencies = currentFlowDependencies.map { dependency ->
                                        if (dependency.jobId == jobId && dependency.upstreamJobId == upstreamJobId) {
                                            JobDependency(jobId, upstreamJobId, bendXDp, bendYDp)
                                        } else {
                                            dependency
                                        }
                                    }
                                    scope.launch {
                                        try {
                                            withContext(Dispatchers.IO) {
                                                controller.updateJobDependencyControlPoint(
                                                    jobId,
                                                    upstreamJobId,
                                                    bendXDp,
                                                    bendYDp
                                                )
                                            }
                                        } catch (_: Exception) {
                                            // keep silent for dependency bend persistence failure
                                        }
                                    }
                                },
                                onStageWidthChanged = { stageId, stageWidthDp ->
                                    allFlowStages = allFlowStages.map { stage ->
                                        if (stage.id == stageId) {
                                            FlowStage(
                                                stage.id,
                                                stage.flowId,
                                                stage.displayName,
                                                stage.order,
                                                stage.barrierMode,
                                                stage.failMode,
                                                stageWidthDp
                                            )
                                        } else {
                                            stage
                                        }
                                    }
                                    scope.launch {
                                        try {
                                            withContext(Dispatchers.IO) {
                                                controller.updateFlowStageWidth(stageId, stageWidthDp)
                                            }
                                        } catch (_: Exception) {
                                            // keep silent for stage width persistence failure
                                        }
                                    }
                                },
                                onJobOrderChanged = { jobId, targetStageId, targetOrder ->
                                    val existing = currentFlowJobsById[jobId] ?: return@FlowGraphView
                                    allJobs = allJobs.map { job ->
                                        if (job.id == jobId) {
                                            Job(
                                                job.id,
                                                targetStageId,
                                                job.title,
                                                job.description,
                                                job.script,
                                                job.config,
                                                job.trigger,
                                                job.dependencies,
                                                job.isEnabled,
                                                targetOrder
                                            )
                                        } else {
                                            job
                                        }
                                    }
                                    scope.launch {
                                        try {
                                            withContext(Dispatchers.IO) {
                                                controller.updateJob(
                                                    jobId,
                                                    existing.title,
                                                    existing.description,
                                                    targetStageId,
                                                    targetOrder,
                                                    existing.isEnabled
                                                )
                                            }
                                        } catch (_: Exception) {
                                            // keep silent for drag reorder persistence failure
                                        }
                                    }
                                },
                                onJobPositionChanged = { jobId, stageRelativeX, stageRelativeY ->
                                    val flowId = selectedFlowId ?: return@FlowGraphView
                                    allFlowLinks = allFlowLinks.map { link ->
                                        if (link.flowId == flowId && link.jobId == jobId) {
                                            FlowJobLink(
                                                link.flowId,
                                                link.jobId,
                                                link.position,
                                                stageRelativeX,
                                                stageRelativeY
                                            )
                                        } else {
                                            link
                                        }
                                    }
                                    scope.launch {
                                        try {
                                            withContext(Dispatchers.IO) {
                                                controller.updateFlowJobStageRelativePosition(
                                                    flowId,
                                                    jobId,
                                                    stageRelativeX,
                                                    stageRelativeY
                                                )
                                            }
                                        } catch (_: Exception) {
                                            // keep silent for drag persistence failure to avoid noisy UI
                                        }
                                    }
                                }
                            )
                        }

                        if (selection != null) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.TopEnd
                            ) {
                                SelectionInspectorPanel(
                                    selectedJob = selectedJob(),
                                    selectedStage = selectedStage(),
                                    flowJobs = currentFlowJobsById.values.toList(),
                                    flowStages = currentFlowStages,
                                    selectedDependency = selectedDependency(),
                                    onClose = { selection = null },
                                    onSaveJob = { jobId, title, description, stageId, order, enabled ->
                                        scope.launch {
                                            withContext(Dispatchers.IO) {
                                                controller.updateJob(jobId, title, description, stageId, order, enabled)
                                            }
                                            snackbarMessage = "Job updated: $jobId"
                                            refresh()
                                        }
                                    },
                                    onEditJobScript = { jobId ->
                                        scriptEditorJobId = jobId
                                        currentPage = JobManagementPage.SCRIPT_EDITOR
                                    },
                                    onDeleteJob = { jobId ->
                                        scope.launch {
                                            withContext(Dispatchers.IO) { controller.deleteJob(jobId) }
                                            snackbarMessage = "Job deleted: $jobId"
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
                                            snackbarMessage = "Stage updated: $stageId"
                                            refresh()
                                        }
                                    },
                                    onDeleteStage = { stageId ->
                                        scope.launch {
                                            withContext(Dispatchers.IO) { controller.deleteFlowStage(stageId) }
                                            snackbarMessage = "Stage deleted: $stageId"
                                            selection = null
                                            refresh()
                                        }
                                    },
                                    onDeleteDependency = { jobId, upstreamJobId ->
                                        scope.launch {
                                            withContext(Dispatchers.IO) {
                                                controller.deleteJobDependency(jobId, upstreamJobId)
                                            }
                                            snackbarMessage = "Dependency deleted: $upstreamJobId -> $jobId"
                                            selection = null
                                            refresh()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                JobManagementPage.RESULTS -> {
                    FlowRunResultsPage(
                        flowRuns = allFlowRuns,
                        flowRunJobs = allFlowRunJobs,
                        selectedFlowId = selectedFlowId,
                        onSelectFlow = { flowId ->
                            selectFlow(flowId)
                            currentPage = JobManagementPage.GRAPH
                        }
                    )
                }

                JobManagementPage.SCRIPT_EDITOR -> {
                    val job = scriptEditorJob
                    if (job == null) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Script target job not found.")
                            AppTextButton(onClick = {
                                scriptEditorJobId = null
                                currentPage = JobManagementPage.GRAPH
                            }) { Text("Back to Graph") }
                        }
                    } else {
                        JobScriptEditorPage(
                            job = job,
                            onBack = {
                                currentPage = JobManagementPage.GRAPH
                            },
                            onSave = { jobId, language, content ->
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            controller.updateJobScript(jobId, language, content)
                                        }
                                        snackbarMessage = "Script updated: $jobId (${language.name})"
                                        refresh()
                                        currentPage = JobManagementPage.GRAPH
                                    } catch (e: Exception) {
                                        snackbarMessage = "Save script failed: ${e.message}"
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun CreateFlowDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var flowName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Flow") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Flow ID will be auto-generated.")
                OutlinedTextField(
                    value = flowName,
                    onValueChange = { flowName = it },
                    label = { Text("Flow Name") }
                )
            }
        },
        confirmButton = {
            AppTextButton(onClick = { onCreate(flowName.trim()) }) { Text("Create") }
        },
        dismissButton = {
            AppTextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CreateStageDialog(
    defaultFlowId: String,
    flowIds: List<String>,
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
                SelectDropdownField(
                    label = "Flow ID",
                    value = flowId,
                    options = flowIds,
                    onSelect = { flowId = it }
                )
                OutlinedTextField(
                    value = stageId,
                    onValueChange = { stageId = it },
                    label = { Text("Stage ID") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = stageName,
                    onValueChange = { stageName = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = orderText,
                    onValueChange = { orderText = it },
                    label = { Text("Order") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            AppTextButton(onClick = {
                val order = orderText.toIntOrNull() ?: 0
                onCreate(flowId.trim(), stageId.trim(), stageName.trim(), order)
            }) { Text("Create") }
        },
        dismissButton = { AppTextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CreateJobDialog(
    defaultFlowId: String,
    defaultStageId: String,
    stageOptions: List<FlowStage>,
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
                OutlinedTextField(
                    value = flowId,
                    onValueChange = { flowId = it },
                    label = { Text("Flow ID") },
                    modifier = Modifier.fillMaxWidth()
                )
                SelectDropdownField(
                    label = "Stage",
                    value = stageOptions.firstOrNull { it.id == stageId }?.let { "${it.displayName} (${it.id})" }
                        ?: stageId,
                    options = stageOptions.map { "${it.displayName} (${it.id})" },
                    onSelect = { selectedText ->
                        val selected = stageOptions.firstOrNull { "${it.displayName} (${it.id})" == selectedText }
                        if (selected != null) {
                            stageId = selected.id
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
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
                OutlinedTextField(
                    value = language,
                    onValueChange = { language = it },
                    label = { Text("Language") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = positionText,
                    onValueChange = { positionText = it },
                    label = { Text("Position") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = script,
                    onValueChange = { script = it },
                    label = { Text("Script") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            AppTextButton(onClick = {
                val position = positionText.toIntOrNull() ?: defaultPosition
                onCreate(flowId.trim(), stageId.trim(), title.trim(), description, language, script, position)
            }) { Text("Create") }
        },
        dismissButton = { AppTextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private enum class JobManagementPage {
    GRAPH,
    CATALOG,
    RESULTS,
    SCRIPT_EDITOR
}

private data class DashboardSnapshot(
    val jobs: List<Job>,
    val flowLinks: List<FlowJobLink>,
    val flowRuns: List<FlowRun>,
    val flowRunJobs: List<FlowRunJob>,
    val flowStages: List<FlowStage>,
    val dependencies: List<JobDependency>
)
