package page

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.reactivex.rxjava3.subjects.BehaviorSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.github.androidpasswordstore.sublimefuzzy.Fuzzy
import process.ProcessManager
import process.ProcessTreeNode
import process.TerminationResult
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

private const val PROCESS_REFRESH_INTERVAL_MILLIS = 2_000L
private const val MAX_VISIBLE_TERMINATION_TARGETS = 3

private class ProcessTerminationTarget(
    val pid: Long,
    val name: String
)

@Composable
fun Processes() {
    val processManager: ProcessManager = remember { ProcessManager() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var processTrees by remember { mutableStateOf<List<ProcessTreeNode>>(emptyList()) }
    var selectedPids by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var pendingTerminationTargets by remember { mutableStateOf<List<ProcessTerminationTarget>?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var filteredProcessTrees by remember { mutableStateOf<List<ProcessTreeNode>>(emptyList()) }

    // RxJava subject that receives raw user input
    val searchSubject = remember { BehaviorSubject.createDefault("") }

    // Wire up the RxJava pipeline: debounce 1 s + distinctUntilChanged → fuzzy filter
    DisposableEffect(Unit) {
        val disposable = searchSubject
            .debounce(500L, TimeUnit.MILLISECONDS)
            .distinctUntilChanged()
            .subscribe { query ->
                filteredProcessTrees = fuzzyFilterProcessTrees(processTrees, query)
            }
        onDispose { disposable.dispose() }
    }

    // Keep filteredProcessTrees in sync when processTrees refreshes (re-apply current query)
    LaunchedEffect(processTrees) {
        filteredProcessTrees = fuzzyFilterProcessTrees(processTrees, searchQuery)
    }

    suspend fun refreshProcesses() {
        val latestTrees: List<ProcessTreeNode> = loadProcessTrees(processManager)
        val livePids: Set<Long> = collectProcessPids(latestTrees)
        processTrees = latestTrees
        selectedPids = retainLivePids(selectedPids, livePids)
    }

    LaunchedEffect(processManager) {
        while (true) {
            refreshProcesses()
            delay(PROCESS_REFRESH_INTERVAL_MILLIS.milliseconds)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProcessPageHeader(
                hasSelection = selectedPids.isNotEmpty(),
                selectedCount = selectedPids.size,
                searchQuery = searchQuery,
                onSearchQueryChange = { newValue ->
                    searchQuery = newValue
                    searchSubject.onNext(newValue)
                },
                onTerminateSelected = {
                    if (selectedPids.isNotEmpty()) {
                        pendingTerminationTargets = buildTerminationTargets(processTrees, selectedPids)
                    }
                }
            )

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(1.dp))
                }

                items(
                    items = filteredProcessTrees,
                    key = { process -> process.pid }
                ) { rootProcess ->
                    ProcessTreeCard(
                        rootProcess = rootProcess,
                        selectedPids = selectedPids,
                        onToggleSelection = { pid ->
                            selectedPids = togglePidSelection(selectedPids, pid)
                        },
                        onTerminateProcess = { process ->
                            val targetsToTerminate: ArrayList<ProcessTerminationTarget> = ArrayList()
                            targetsToTerminate.add(createTerminationTarget(process))
                            pendingTerminationTargets = targetsToTerminate
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(1.dp))
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        )
    }

    val targetsToConfirm = pendingTerminationTargets
    if (targetsToConfirm != null) {
        ConfirmTerminateDialog(
            targets = targetsToConfirm,
            onDismiss = {
                pendingTerminationTargets = null
            },
            onConfirm = {
                val targetsToTerminate: ArrayList<ProcessTerminationTarget> = ArrayList(targetsToConfirm)
                val pidsToTerminate: ArrayList<Long> = getTargetPids(targetsToTerminate)
                pendingTerminationTargets = null

                scope.launch {
                    val result: TerminationResult =
                        terminateProcesses(processManager, pidsToTerminate)
                    selectedPids = removePidSelections(selectedPids, pidsToTerminate)
                    refreshProcesses()
                    snackbarHostState.showSnackbar(buildTerminationMessage(result, targetsToTerminate))
                }
            }
        )
    }
}

@Composable
private fun ProcessPageHeader(
    hasSelection: Boolean,
    selectedCount: Int,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onTerminateSelected: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Process List",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search processes…") },
                singleLine = true,
                modifier = Modifier.width(240.dp)
            )

            Spacer(Modifier.width(8.dp))

            Button(
                onClick = onTerminateSelected,
                enabled = hasSelection,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(if (selectedCount > 0) "Terminate ($selectedCount)" else "Terminate")
            }
        }
    }
}

@Composable
private fun ConfirmTerminateDialog(
    targets: List<ProcessTerminationTarget>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (targets.size == 1) "Terminate process?" else "Terminate processes?")
        },
        text = {
            Text(buildTerminationConfirmText(targets))
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Terminate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ProcessTreeCard(
    rootProcess: ProcessTreeNode,
    selectedPids: Set<Long>,
    onToggleSelection: (Long) -> Unit,
    onTerminateProcess: (ProcessTreeNode) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            ProcessTreeRow(
                process = rootProcess,
                depth = 0,
                selectedPids = selectedPids,
                onToggleSelection = onToggleSelection,
                onTerminateProcess = onTerminateProcess
            )

            if (rootProcess.children.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                for (child in rootProcess.children) {
                    ProcessTreeRow(
                        process = child,
                        depth = 1,
                        selectedPids = selectedPids,
                        onToggleSelection = onToggleSelection,
                        onTerminateProcess = onTerminateProcess
                    )
                }
            }
        }
    }
}

@Composable
private fun ProcessTreeRow(
    process: ProcessTreeNode,
    depth: Int,
    selectedPids: Set<Long>,
    onToggleSelection: (Long) -> Unit,
    onTerminateProcess: (ProcessTreeNode) -> Unit
) {
    val selected = selectedPids.contains(process.pid)

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onToggleSelection(process.pid) }) {
            Icon(
                imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = if (selected) "Deselect process" else "Select process",
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width((depth * 24).dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = process.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = getProcessSubtitle(process),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "CPU ${process.cpuUsageText}",
            modifier = Modifier.width(92.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = process.memoryUsageText,
            modifier = Modifier.width(100.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        IconButton(onClick = { onTerminateProcess(process) }) {
            Icon(
                imageVector = Icons.Outlined.StopCircle,
                contentDescription = "Terminate process",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

suspend fun loadProcessTrees(processManager: ProcessManager): List<ProcessTreeNode> {
    return withContext(Dispatchers.IO) {
        processManager.getProcessTrees()
    }
}

suspend fun terminateProcesses(
    processManager: ProcessManager,
    pids: Collection<Long>
): TerminationResult {
    return withContext(Dispatchers.IO) {
        processManager.terminateProcesses(pids)
    }
}

private fun togglePidSelection(selectedPids: Set<Long>, pid: Long): Set<Long> {
    val updatedSelection = HashSet(selectedPids)
    if (updatedSelection.contains(pid)) {
        updatedSelection.remove(pid)
    } else {
        updatedSelection.add(pid)
    }
    return updatedSelection
}

private fun removePidSelections(selectedPids: Set<Long>, pids: Collection<Long>): Set<Long> {
    val updatedSelection = HashSet(selectedPids)
    for (pid in pids) {
        updatedSelection.remove(pid)
    }
    return updatedSelection
}

private fun retainLivePids(selectedPids: Set<Long>, livePids: Set<Long>): Set<Long> {
    val updatedSelection = HashSet<Long>()
    for (pid in selectedPids) {
        if (livePids.contains(pid)) {
            updatedSelection.add(pid)
        }
    }
    return updatedSelection
}

private fun collectProcessPids(processes: List<ProcessTreeNode>): Set<Long> {
    val pids = HashSet<Long>()
    for (process in processes) {
        collectProcessPidsInto(process, pids)
    }
    return pids
}

private fun collectProcessPidsInto(
    process: ProcessTreeNode,
    target: HashSet<Long>
) {
    target.add(process.pid)
    for (child in process.children) {
        collectProcessPidsInto(child, target)
    }
}

private fun getProcessSubtitle(process: ProcessTreeNode): String {
    val user = process.user
    if (user.isBlank()) {
        return "PID ${process.pid}"
    }
    return "PID ${process.pid} - $user"
}

private fun buildTerminationConfirmText(targets: List<ProcessTerminationTarget>): String {
    if (targets.size == 1) {
        return "Are you sure you want to terminate process ${formatTerminationTarget(targets[0])}? This may cause unsaved work to be lost."
    }
    return "Are you sure you want to terminate ${targets.size} selected processes? Processes: ${formatTerminationTargetList(targets)}"
}

private fun buildTerminationMessage(
    result: TerminationResult,
    targets: List<ProcessTerminationTarget>
): String {
    if (result.isSuccess) {
        if (targets.size == 1) {
            return "Terminated process ${formatTerminationTarget(targets[0])}"
        }
        return "Terminated processes ${formatTerminationTargetList(targets)}"
    }

    if (targets.size == 1) {
        return "Failed to terminate process ${formatTerminationTarget(targets[0])}"
    }
    return "Terminated ${result.terminatedCount}/${result.requestedCount}; failed: ${formatFailedTerminationTargets(result.failedPids, targets)}"
}

private fun buildTerminationTargets(
    processes: List<ProcessTreeNode>,
    pids: Set<Long>
): ArrayList<ProcessTerminationTarget> {
    val targets = ArrayList<ProcessTerminationTarget>()
    for (pid in pids) {
        val process = findProcessByPid(processes, pid)
        if (process == null) {
            targets.add(ProcessTerminationTarget(pid, "Process"))
        } else {
            targets.add(createTerminationTarget(process))
        }
    }
    return targets
}

private fun findProcessByPid(processes: List<ProcessTreeNode>, pid: Long): ProcessTreeNode? {
    for (process in processes) {
        if (process.pid == pid) {
            return process
        }

        val child = findProcessByPid(process.children, pid)
        if (child != null) {
            return child
        }
    }
    return null
}

private fun createTerminationTarget(process: ProcessTreeNode): ProcessTerminationTarget {
    val name = if (process.name.isBlank()) "Process" else process.name
    return ProcessTerminationTarget(process.pid, name)
}

private fun getTargetPids(targets: List<ProcessTerminationTarget>): ArrayList<Long> {
    val pids = ArrayList<Long>()
    for (target in targets) {
        pids.add(target.pid)
    }
    return pids
}

private fun formatFailedTerminationTargets(
    failedPids: List<Long>,
    targets: List<ProcessTerminationTarget>
): String {
    val failedTargets = ArrayList<ProcessTerminationTarget>()
    for (pid in failedPids) {
        failedTargets.add(findTerminationTarget(targets, pid))
    }
    return formatTerminationTargetList(failedTargets)
}

private fun findTerminationTarget(
    targets: List<ProcessTerminationTarget>,
    pid: Long
): ProcessTerminationTarget {
    for (target in targets) {
        if (target.pid == pid) {
            return target
        }
    }
    return ProcessTerminationTarget(pid, "Process")
}

private fun formatTerminationTarget(target: ProcessTerminationTarget): String {
    return "${target.name} (${target.pid})"
}

private fun formatTerminationTargetList(targets: List<ProcessTerminationTarget>): String {
    val builder = StringBuilder()
    var index = 0

    while (index < targets.size && index < MAX_VISIBLE_TERMINATION_TARGETS) {
        if (index > 0) {
            builder.append(", ")
        }
        builder.append(formatTerminationTarget(targets[index]))
        index++
    }

    if (targets.size > MAX_VISIBLE_TERMINATION_TARGETS) {
        builder.append(", and ")
        builder.append(targets.size - MAX_VISIBLE_TERMINATION_TARGETS)
        builder.append(" more")
    }

    return builder.toString()
}

// ---------------------------------------------------------------------------
// Fuzzy filtering
// ---------------------------------------------------------------------------

/**
 * Filters a process tree list using sublime-fuzzy matching.
 * When [query] is blank every process is shown; otherwise processes whose name
 * or user field match the query are retained, sorted by descending match score.
 * A root card is kept if any node in its subtree matches.
 */
private fun fuzzyFilterProcessTrees(
    trees: List<ProcessTreeNode>,
    query: String
): List<ProcessTreeNode> {
    if (query.isBlank()) return trees

    val result = mutableListOf<Pair<ProcessTreeNode, Int>>()
    for (root in trees) {
        val score = bestFuzzyScore(query, root)
        if (score != null) {
            result.add(root to score)
        }
    }
    return result
        .sortedByDescending { (_, score) -> score }
        .map { (node, _) -> node }
}

/**
 * Returns the highest fuzzy score found anywhere in the subtree rooted at [node],
 * or null if nothing matched.
 */
private fun bestFuzzyScore(query: String, node: ProcessTreeNode): Int? {
    val (nameMatched, nameScore) = Fuzzy.fuzzyMatch(query, node.name)
    val nameResult = if (nameMatched) nameScore else null

    val userResult = if (node.user.isNotBlank()) {
        val (userMatched, userScore) = Fuzzy.fuzzyMatch(query, node.user)
        if (userMatched) userScore else null
    } else null

    val selfBest = listOfNotNull(nameResult, userResult).maxOrNull()

    val childBest = node.children
        .mapNotNull { bestFuzzyScore(query, it) }
        .maxOrNull()

    return listOfNotNull(selfBest, childBest).maxOrNull()
}
