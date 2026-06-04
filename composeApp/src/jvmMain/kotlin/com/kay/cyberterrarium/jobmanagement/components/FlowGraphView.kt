package com.kay.cyberterrarium.jobmanagement.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import job.model.Job
import job.model.JobDependency
import job.model.flow.FlowJobLink
import job.model.stage.FlowStage
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun FlowGraphView(
    selectedFlowId: String?,
    flowStages: List<FlowStage>,
    flowJobLinks: List<FlowJobLink>,
    jobsById: Map<String, Job>,
    dependencies: List<JobDependency>,
    selection: GraphSelection?,
    onSelect: (GraphSelection) -> Unit,
    onCreateDependency: (jobId: String, upstreamJobId: String) -> Unit,
    onDependencyRejected: (message: String) -> Unit = {},
    onDependencyControlPointChanged: (jobId: String, upstreamJobId: String, bendXDp: Double, bendYDp: Double) -> Unit = { _, _, _, _ -> },
    onStageWidthChanged: (stageId: String, stageWidthDp: Double) -> Unit = { _, _ -> },
    onJobOrderChanged: (jobId: String, targetStageId: String, targetOrder: Int) -> Unit = { _, _, _ -> },
    onJobPositionChanged: (jobId: String, stageRelativeX: Double, stageRelativeY: Double) -> Unit
) {
    if (selectedFlowId.isNullOrBlank()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("選擇任何 flow 來顯示關係圖", style = MaterialTheme.typography.titleMedium)
        }
        return
    }

    var draggingFromJobId by remember(selectedFlowId) { mutableStateOf<String?>(null) }
    var dragStart by remember(selectedFlowId) { mutableStateOf<Offset?>(null) }
    var dragCurrent by remember(selectedFlowId) { mutableStateOf<Offset?>(null) }

    var dragJobId by remember(selectedFlowId) { mutableStateOf<String?>(null) }
    var dragJobStartTopLeft by remember(selectedFlowId) { mutableStateOf<Offset?>(null) }
    var dragJobOffset by remember(selectedFlowId) { mutableStateOf(Offset.Zero) }
    var dragDependencyKey by remember(selectedFlowId) { mutableStateOf<String?>(null) }
    var dragDependencyBend by remember(selectedFlowId) { mutableStateOf<Offset?>(null) }
    var dragDependencyHandle by remember(selectedFlowId) { mutableStateOf<Offset?>(null) }
    var resizingStageBoundary by remember(selectedFlowId) { mutableStateOf(false) }
    val dependencyBendOverridePx = remember(selectedFlowId) { mutableStateMapOf<String, Offset>() }
    val pendingJobPlacementById = remember(selectedFlowId) { mutableStateMapOf<String, Pair<String, Int>>() }

    val sortedLinks = flowJobLinks.sortedBy { it.position }
    val graphJobs = sortedLinks.mapNotNull { jobsById[it.jobId] }
    fun effectiveStageId(job: Job): String = pendingJobPlacementById[job.id]?.first ?: job.stageId
    fun effectiveOrder(job: Job): Int = pendingJobPlacementById[job.id]?.second ?: job.order

    LaunchedEffect(graphJobs, selectedFlowId) {
        pendingJobPlacementById.keys.toList().forEach { jobId ->
            val pendingPlacement = pendingJobPlacementById[jobId] ?: return@forEach
            val job = jobsById[jobId] ?: return@forEach
            if (job.stageId == pendingPlacement.first && job.order == pendingPlacement.second) {
                pendingJobPlacementById.remove(jobId)
            }
        }
    }

    val stageById = flowStages.associateBy { it.id }
    val stageIds = buildList {
        addAll(flowStages.sortedBy { it.order }.map { it.id })
        for (job in graphJobs) {
            val stageId = effectiveStageId(job)
            if (stageId !in this) add(stageId)
        }
    }.distinct().ifEmpty { listOf("unassigned-stage") }

    val stageLabelById = stageIds.associateWith { stageId ->
        stageById[stageId]?.displayName?.ifBlank { stageId } ?: stageId
    }

    val actualOrdersByStage = stageIds.associateWith { stageId ->
        graphJobs
            .asSequence()
            .filter { effectiveStageId(it) == stageId }
            .map { effectiveOrder(it) }
            .distinct()
            .sorted()
            .toList()
    }
    val renderOrdersByStage = stageIds.associateWith { stageId ->
        val actual = actualOrdersByStage[stageId] ?: emptyList()
        val next = (actual.maxOrNull() ?: -1) + 1
        (actual + next).distinct().sorted()
    }

    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val horizontalScroll = rememberScrollState()
    val stageWidthOverridePx = remember(selectedFlowId) { mutableStateMapOf<String, Float>() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surfaceVariant.copy(alpha = 0.2f))
    ) {
        val viewportWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val viewportHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        val nodeWidthPx = with(density) { 170.dp.toPx() }
        val nodeHeightPx = with(density) { 74.dp.toPx() }
        val lanePaddingPx = with(density) { 14.dp.toPx() }
        val laneHeaderHeightPx = with(density) { 54.dp.toPx() }
        val rowGapPx = with(density) { 90.dp.toPx() }
        val rowTopOffsetPx = with(density) { 18.dp.toPx() }
        val rowBottomPaddingPx = with(density) { 10.dp.toPx() }
        val orderColumnWidthPx = nodeWidthPx + with(density) { 56.dp.toPx() }
        val edgeStartMinDeltaPx = with(density) { 20.dp.toPx() }
        val edgeMarkerHalfPx = with(density) { 8.dp.toPx() }
        val connectorRadiusPx = with(density) { 6.dp.toPx() }
        val connectorSnapRadiusPx = with(density) { 18.dp.toPx() }
        val boundaryHandleHalfPx = with(density) { 10.dp.toPx() }
        val interactiveMinStageWidthPx = with(density) { 160.dp.toPx() }

        val autoMinStageWidthById = stageIds.associateWith { stageId ->
            val orderCount = (renderOrdersByStage[stageId] ?: emptyList()).size.coerceAtLeast(1)
            val requiredForOrders = lanePaddingPx * 2f + orderCount * orderColumnWidthPx
            maxOf(requiredForOrders, with(density) { 260.dp.toPx() })
        }
        val stageWidthById = stageIds.associateWith { stageId ->
            val minWidth = autoMinStageWidthById[stageId] ?: with(density) { 260.dp.toPx() }
            val persistedWidthPx = stageById[stageId]
                ?.stageWidth
                ?.takeIf { it > 0.0 }
                ?.let { with(density) { it.dp.toPx() } }
            (stageWidthOverridePx[stageId] ?: persistedWidthPx ?: minWidth).coerceAtLeast(minWidth)
        }
        val stageStartById = mutableMapOf<String, Float>()
        var cursorX = 0f
        for (stageId in stageIds) {
            stageStartById[stageId] = cursorX
            cursorX += stageWidthById[stageId] ?: 0f
        }
        val graphWidthPx = cursorX.coerceAtLeast(viewportWidthPx)
        val graphWidthDp = with(density) { graphWidthPx.toDp() }

        val stageRangeById = stageIds.associateWith { stageId ->
            val start = stageStartById[stageId] ?: 0f
            val width = stageWidthById[stageId] ?: 0f
            start to (start + width)
        }

        val rowCursorByStageOrder = mutableMapOf<Pair<String, Int>, Int>()
        val orderedJobs = graphJobs.sortedWith(
            compareBy<Job> { effectiveStageId(it) }
                .thenBy { effectiveOrder(it) }
                .thenBy { it.id }
        )
        val baseNodeLayouts = orderedJobs.map { job ->
            val stageId = effectiveStageId(job)
            val orders = renderOrdersByStage[stageId] ?: listOf(0)
            val orderIndex = orders.indexOf(effectiveOrder(job)).let { idx -> if (idx < 0) 0 else idx }
            val orderValue = orders[orderIndex]
            val (laneStartX, laneEndX) = stageRangeById[stageId] ?: (0f to graphWidthPx)
            val laneWidthPx = laneEndX - laneStartX
            val key = stageId to orderValue
            val rowIndex = rowCursorByStageOrder.getOrDefault(key, 0)
            rowCursorByStageOrder[key] = rowIndex + 1

            val columnStart = laneStartX + lanePaddingPx + orderIndex * orderColumnWidthPx
            val x = (columnStart + (orderColumnWidthPx - nodeWidthPx) / 2f)
                .coerceIn(laneStartX + lanePaddingPx, laneEndX - nodeWidthPx - lanePaddingPx)
            val y = (laneHeaderHeightPx + rowTopOffsetPx + rowIndex * rowGapPx)
                .coerceAtMost(viewportHeightPx - nodeHeightPx - rowBottomPaddingPx)
            NodeLayout(job = job, topLeft = Offset(x, y), stageId = stageId, order = orderValue)
        }

        val nodeLayouts = baseNodeLayouts.map { node ->
            if (dragJobId == node.job.id && dragJobStartTopLeft != null) {
                node.copy(topLeft = dragJobStartTopLeft!! + dragJobOffset)
            } else {
                node
            }
        }
        val nodeById = nodeLayouts.associateBy { it.job.id }
        val inputCenterByJobId = nodeLayouts.associate { layout ->
            layout.job.id to Offset(layout.topLeft.x, layout.topLeft.y + nodeHeightPx / 2f)
        }
        val outputCenterByJobId = nodeLayouts.associate { layout ->
            layout.job.id to Offset(layout.topLeft.x + nodeWidthPx, layout.topLeft.y + nodeHeightPx / 2f)
        }
        val visibleDependencies = dependencies.filter { dependency ->
            nodeById.containsKey(dependency.jobId) && nodeById.containsKey(dependency.upstreamJobId)
        }
        fun dependencyKey(dependency: JobDependency): String = "${dependency.jobId}|${dependency.upstreamJobId}"
        fun defaultBend(start: Offset, end: Offset): Offset = Offset((start.x + end.x) / 2f, (start.y + end.y) / 2f)
        val bendHandleT = 0.5f
        fun pointOnQuadratic(start: Offset, control: Offset, end: Offset, t: Float): Offset {
            val oneMinusT = 1f - t
            val x = oneMinusT * oneMinusT * start.x +
                2f * oneMinusT * t * control.x +
                t * t * end.x
            val y = oneMinusT * oneMinusT * start.y +
                2f * oneMinusT * t * control.y +
                t * t * end.y
            return Offset(x, y)
        }
        fun controlFromQuadraticPoint(start: Offset, end: Offset, pointOnCurve: Offset, t: Float): Offset {
            val oneMinusT = 1f - t
            val factor = 2f * oneMinusT * t
            if (factor == 0f) return defaultBend(start, end)
            val x = (pointOnCurve.x - (oneMinusT * oneMinusT * start.x + t * t * end.x)) / factor
            val y = (pointOnCurve.y - (oneMinusT * oneMinusT * start.y + t * t * end.y)) / factor
            return Offset(x, y)
        }
        fun clampBendControl(start: Offset, end: Offset, candidate: Offset): Offset {
            val dx = end.x - start.x
            val dy = end.y - start.y
            val length = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
            val ux = dx / length
            val uy = dy / length
            val px = -uy
            val py = ux

            val rx = candidate.x - start.x
            val ry = candidate.y - start.y
            val projected = rx * ux + ry * uy
            val perpendicular = rx * px + ry * py

            val minProjected = min(24f, length * 0.25f)
            val maxProjected = max(length - 24f, length * 0.75f)
            val maxPerpendicular = max(56f, min(160f, length * 0.55f))

            val clampedProjected = projected.coerceIn(minProjected, maxProjected)
            val clampedPerpendicular = perpendicular.coerceIn(-maxPerpendicular, maxPerpendicular)

            val clamped = Offset(
                x = start.x + ux * clampedProjected + px * clampedPerpendicular,
                y = start.y + uy * clampedProjected + py * clampedPerpendicular
            )
            val viewportPadding = 12f
            return Offset(
                x = clamped.x.coerceIn(viewportPadding, graphWidthPx - viewportPadding),
                y = clamped.y.coerceIn(viewportPadding, viewportHeightPx - viewportPadding)
            )
        }

        fun stageSortOrder(stageId: String): Int {
            return stageById[stageId]?.order ?: stageIds.indexOf(stageId).takeIf { it >= 0 } ?: 0
        }

        fun dependencyMoveRejection(jobId: String, targetStageId: String, targetOrder: Int): String? {
            val targetStageOrder = stageSortOrder(targetStageId)
            val upstreamNodes = dependencies
                .asSequence()
                .filter { it.jobId == jobId }
                .mapNotNull { nodeById[it.upstreamJobId] }
                .toList()

            val maxUpstreamOrder = upstreamNodes.maxOfOrNull { it.order } ?: return null
            if (targetOrder <= maxUpstreamOrder) {
                return "Job order must be greater than dependency max order $maxUpstreamOrder."
            }

            val blockingUpstreamStage = upstreamNodes
                .asSequence()
                .filter { upstreamNode ->
                    val upstreamStageOrder = stageSortOrder(upstreamNode.stageId)
                    targetStageOrder < upstreamStageOrder
                }
                .maxWithOrNull(
                    compareBy<NodeLayout> { stageSortOrder(it.stageId) }
                        .thenBy { it.order }
                        .thenBy { it.job.id }
                )

            return blockingUpstreamStage?.let { upstreamNode ->
                "Job must be in the same or a later stage than dependency ${upstreamNode.job.id.take(8)}."
            }
        }

        fun resolveDropTarget(dropCenterX: Float, fallbackStageId: String, fallbackOrder: Int): Pair<String, Int> {
            val targetStageId = stageIds.firstOrNull { stageId ->
                val (start, end) = stageRangeById[stageId] ?: return@firstOrNull false
                dropCenterX >= start && dropCenterX <= end
            } ?: fallbackStageId
            val orders = renderOrdersByStage[targetStageId] ?: listOf(fallbackOrder)
            val startX = (stageStartById[targetStageId] ?: 0f) + lanePaddingPx
            val orderIndex = floor(((dropCenterX - startX) / orderColumnWidthPx).toDouble()).toInt()
                .coerceIn(0, orders.lastIndex)
            return targetStageId to orders[orderIndex]
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .horizontalScroll(horizontalScroll, enabled = !resizingStageBoundary)
            ) {
                Box(
                    modifier = Modifier
                        .width(graphWidthDp)
                        .fillMaxSize()
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        stageIds.forEach { stageId ->
                            val highlighted = selection is GraphSelection.StageSelection && selection.stageId == stageId
                            val stageTint = if (highlighted) {
                                colors.primary.copy(alpha = 0.12f)
                            } else {
                                colors.surface.copy(alpha = 0.6f)
                            }
                            Box(
                                modifier = Modifier
                                    .width(with(density) { (stageWidthById[stageId] ?: 0f).toDp() })
                                    .fillMaxSize()
                                    .background(stageTint)
                                    .clickable { onSelect(GraphSelection.StageSelection(stageId)) }
                            ) {
                                Text(
                                    text = stageLabelById[stageId] ?: stageId,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.offset { IntOffset(12.dp.roundToPx(), 10.dp.roundToPx()) },
                                    color = colors.onSurface
                                )
                            }
                        }
                    }

                    stageIds.drop(1).forEachIndexed { index, rightStageId ->
                        val leftStageId = stageIds[index]
                        val boundaryX = stageStartById[rightStageId] ?: return@forEachIndexed
                        Box(
                            modifier = Modifier
                                .offset { IntOffset((boundaryX - boundaryHandleHalfPx).roundToInt(), 0) }
                                .width(20.dp)
                                .fillMaxSize()
                                .zIndex(2f)
                                .pointerInput(selectedFlowId, leftStageId, rightStageId, autoMinStageWidthById) {
                                    detectHorizontalDragGestures(
                                        onDragStart = { resizingStageBoundary = true },
                                        onDragEnd = {
                                            resizingStageBoundary = false
                                            val persistedWidthPx = stageWidthOverridePx[leftStageId]
                                            if (persistedWidthPx != null) {
                                                onStageWidthChanged(
                                                    leftStageId,
                                                    with(density) { persistedWidthPx.toDp().value.toDouble() }
                                                )
                                            }
                                        },
                                        onDragCancel = { resizingStageBoundary = false }
                                    ) { change, dragAmount ->
                                        change.consume()
                                        val leftWidth = stageWidthOverridePx[leftStageId]
                                            ?: stageWidthById[leftStageId]
                                            ?: return@detectHorizontalDragGestures
                                        val leftMin = interactiveMinStageWidthPx
                                        var appliedDelta = dragAmount
                                        if (leftWidth + appliedDelta < leftMin) {
                                            appliedDelta = leftMin - leftWidth
                                        }
                                        if (appliedDelta == 0f) {
                                            return@detectHorizontalDragGestures
                                        }
                                        stageWidthOverridePx[leftStageId] = leftWidth + appliedDelta
                                    }
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .fillMaxHeight()
                                    .width(2.dp)
                                    .background(colors.outline.copy(alpha = 0.6f))
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .width(10.dp)
                                    .height(46.dp)
                                    .background(
                                        color = colors.surface.copy(alpha = 0.92f),
                                        shape = RoundedCornerShape(7.dp)
                                    )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .width(3.dp)
                                        .height(24.dp)
                                        .background(colors.outline.copy(alpha = 0.85f), shape = RoundedCornerShape(6.dp))
                                )
                            }
                        }
                    }

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        stageIds.drop(1).forEach { stageId ->
                            val x = stageStartById[stageId] ?: return@forEach
                            drawLine(
                                color = colors.outline.copy(alpha = 0.55f),
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        if (dragStart != null && dragCurrent != null) {
                            val start = dragStart!!
                            val end = dragCurrent!!
                            val controlDelta = ((end.x - start.x).coerceAtLeast(edgeStartMinDeltaPx)) * 0.35f
                            val dragPath = Path().apply {
                                moveTo(start.x, start.y)
                                cubicTo(
                                    start.x + controlDelta, start.y,
                                    end.x - controlDelta, end.y,
                                    end.x, end.y
                                )
                            }
                            drawPath(
                                path = dragPath,
                                color = colors.primary.copy(alpha = 0.85f),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx())
                            )
                        }
                    }

                    nodeLayouts.forEach { node ->
                        val selectedNode = selection is GraphSelection.JobSelection && selection.jobId == node.job.id
                        val nodeColor = if (selectedNode) colors.primaryContainer else colors.surface
                        val nodeBorder = if (selectedNode) colors.primary else colors.outline.copy(alpha = 0.5f)
                        Card(
                            modifier = Modifier
                                .offset { IntOffset(node.topLeft.x.roundToInt(), node.topLeft.y.roundToInt()) }
                                .width(170.dp)
                                .heightIn(min = 74.dp)
                                .pointerInput(node.job.id) {
                                    detectTapGestures(onTap = { onSelect(GraphSelection.JobSelection(node.job.id)) })
                                }
                                .pointerInput(
                                    node.job.id,
                                    selectedFlowId,
                                    node.stageId,
                                    node.order,
                                    dependencies
                                ) {
                                    detectDragGestures(
                                        onDragStart = {
                                            val jobId = node.job.id
                                            val latestNode = nodeById[jobId] ?: node
                                            dragJobId = jobId
                                            dragJobStartTopLeft = latestNode.topLeft
                                            dragJobOffset = Offset.Zero
                                        },
                                        onDragCancel = {
                                            dragJobId = null
                                            dragJobStartTopLeft = null
                                            dragJobOffset = Offset.Zero
                                        },
                                        onDragEnd = {
                                            val jobId = dragJobId
                                            val startTopLeft = if (jobId != null) {
                                                dragJobStartTopLeft ?: nodeById[jobId]?.topLeft
                                            } else {
                                                null
                                            }
                                            if (jobId != null && startTopLeft != null) {
                                                val currentNode = nodeById[jobId]
                                                if (currentNode != null) {
                                                    val finalTopLeft = startTopLeft + dragJobOffset
                                                    val dropCenterX = finalTopLeft.x + nodeWidthPx / 2f
                                                    val (targetStageId, targetOrder) = resolveDropTarget(
                                                        dropCenterX = dropCenterX,
                                                        fallbackStageId = currentNode.stageId,
                                                        fallbackOrder = currentNode.order
                                                    )
                                                    if (targetStageId != currentNode.stageId || targetOrder != currentNode.order) {
                                                        val rejection = dependencyMoveRejection(jobId, targetStageId, targetOrder)
                                                        if (rejection != null) {
                                                            onDependencyRejected(rejection)
                                                        } else {
                                                            pendingJobPlacementById[jobId] = targetStageId to targetOrder
                                                            onJobOrderChanged(jobId, targetStageId, targetOrder)
                                                        }
                                                    }
                                                }
                                            }
                                            dragJobId = null
                                            dragJobStartTopLeft = null
                                            dragJobOffset = Offset.Zero
                                        }
                                    ) { change, dragAmount ->
                                        change.consume()
                                        if (dragJobId == node.job.id) {
                                            dragJobOffset += dragAmount
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, nodeBorder)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().background(nodeColor)) {
                                Text(
                                    text = "${node.job.title}\n${node.job.id.take(8)}\norder ${node.order}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.offset { IntOffset(10.dp.roundToPx(), 10.dp.roundToPx()) }
                                )
                            }
                        }
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(3f)
                    ) {
                        visibleDependencies.forEach { dependency ->
                            val start = outputCenterByJobId[dependency.upstreamJobId] ?: return@forEach
                            val end = inputCenterByJobId[dependency.jobId] ?: return@forEach
                            val key = dependencyKey(dependency)
                            val persistedBend = if (dependency.bendX >= 0.0 && dependency.bendY >= 0.0) {
                                Offset(
                                    with(density) { dependency.bendX.dp.toPx() },
                                    with(density) { dependency.bendY.dp.toPx() }
                                )
                            } else {
                                null
                            }
                            val bend = if (dragDependencyKey == key && dragDependencyBend != null) {
                                clampBendControl(start, end, dragDependencyBend!!)
                            } else {
                                clampBendControl(start, end, dependencyBendOverridePx[key] ?: persistedBend ?: defaultBend(start, end))
                            }
                            val usesBendControl = persistedBend != null ||
                                dependencyBendOverridePx[key] != null ||
                                dragDependencyKey == key
                            val path = if (usesBendControl) {
                                // Single smooth curve: start -> end, bend acts as the control handle.
                                Path().apply {
                                    moveTo(start.x, start.y)
                                    quadraticTo(bend.x, bend.y, end.x, end.y)
                                }
                            } else {
                                val controlDelta = ((end.x - start.x).coerceAtLeast(edgeStartMinDeltaPx)) * 0.35f
                                Path().apply {
                                    moveTo(start.x, start.y)
                                    cubicTo(
                                        start.x + controlDelta, start.y,
                                        end.x - controlDelta, end.y,
                                        end.x, end.y
                                    )
                                }
                            }

                            val selectedEdge = selection is GraphSelection.DependencySelection &&
                                selection.jobId == dependency.jobId &&
                                selection.upstreamJobId == dependency.upstreamJobId
                            val edgeColor = if (selectedEdge) colors.primary else colors.outline
                            drawPath(path = path, color = edgeColor, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                            val arrowTangentStart = if (usesBendControl) {
                                bend
                            } else {
                                val controlDelta = ((end.x - start.x).coerceAtLeast(edgeStartMinDeltaPx)) * 0.35f
                                Offset(end.x - controlDelta, end.y)
                            }
                            drawArrowHead(start = arrowTangentStart, end = end, color = edgeColor)
                        }
                    }

                    visibleDependencies.forEach { dependency ->
                        val start = outputCenterByJobId[dependency.upstreamJobId] ?: return@forEach
                        val end = inputCenterByJobId[dependency.jobId] ?: return@forEach
                        val key = dependencyKey(dependency)
                        val persistedBend = if (dependency.bendX >= 0.0 && dependency.bendY >= 0.0) {
                            Offset(
                                with(density) { dependency.bendX.dp.toPx() },
                                with(density) { dependency.bendY.dp.toPx() }
                            )
                        } else {
                            null
                        }
                        val bend = if (dragDependencyKey == key && dragDependencyBend != null) {
                            clampBendControl(start, end, dragDependencyBend!!)
                        } else {
                            clampBendControl(start, end, dependencyBendOverridePx[key] ?: persistedBend ?: defaultBend(start, end))
                        }
                        val handleOnCurve = pointOnQuadratic(start, bend, end, bendHandleT)
                        val selectedEdge = selection is GraphSelection.DependencySelection &&
                            selection.jobId == dependency.jobId &&
                            selection.upstreamJobId == dependency.upstreamJobId
                        Box(
                            modifier = Modifier
                                .offset { IntOffset((handleOnCurve.x - edgeMarkerHalfPx).roundToInt(), (handleOnCurve.y - edgeMarkerHalfPx).roundToInt()) }
                                .size(16.dp)
                                .zIndex(4f)
                                .background(
                                    color = if (selectedEdge) colors.primary else colors.outline,
                                    shape = RoundedCornerShape(50)
                                )
                                .pointerInput(key) {
                                    detectTapGestures {
                                        onSelect(GraphSelection.DependencySelection(dependency.jobId, dependency.upstreamJobId))
                                    }
                                }
                                .pointerInput(key, selectedFlowId) {
                                    detectDragGestures(
                                        onDragStart = {
                                            dragDependencyKey = key
                                            dragDependencyBend = clampBendControl(start, end, bend)
                                            dragDependencyHandle = pointOnQuadratic(start, dragDependencyBend!!, end, bendHandleT)
                                        },
                                        onDragCancel = {
                                            dragDependencyKey = null
                                            dragDependencyBend = null
                                            dragDependencyHandle = null
                                        },
                                        onDragEnd = {
                                            val finalBend = dragDependencyBend?.let { clampBendControl(start, end, it) }
                                            if (dragDependencyKey == key && finalBend != null) {
                                                dependencyBendOverridePx[key] = finalBend
                                                onDependencyControlPointChanged(
                                                    dependency.jobId,
                                                    dependency.upstreamJobId,
                                                    with(density) { finalBend.x.toDp().value.toDouble() },
                                                    with(density) { finalBend.y.toDp().value.toDouble() }
                                                )
                                            }
                                            dragDependencyKey = null
                                            dragDependencyBend = null
                                            dragDependencyHandle = null
                                        }
                                    ) { change, dragAmount ->
                                        change.consume()
                                        if (dragDependencyKey == key && dragDependencyBend != null && dragDependencyHandle != null) {
                                            val nextHandle = Offset(
                                                x = dragDependencyHandle!!.x + dragAmount.x,
                                                y = dragDependencyHandle!!.y + dragAmount.y
                                            )
                                            val candidateControl = controlFromQuadraticPoint(
                                                start = start,
                                                end = end,
                                                pointOnCurve = nextHandle,
                                                t = bendHandleT
                                            )
                                            val clampedControl = clampBendControl(start, end, candidateControl)
                                            dragDependencyBend = clampedControl
                                            dragDependencyHandle = pointOnQuadratic(start, clampedControl, end, bendHandleT)
                                        }
                                    }
                                }
                        )
                    }

                    nodeLayouts.forEach { node ->
                        val input = inputCenterByJobId[node.job.id] ?: return@forEach
                        val output = outputCenterByJobId[node.job.id] ?: return@forEach

                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        (input.x - connectorRadiusPx).roundToInt(),
                                        (input.y - connectorRadiusPx).roundToInt()
                                    )
                                }
                                .size(12.dp)
                                .background(colors.surface, shape = RoundedCornerShape(50))
                                .clickable { onSelect(GraphSelection.JobSelection(node.job.id)) }
                        )

                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        (output.x - connectorRadiusPx).roundToInt(),
                                        (output.y - connectorRadiusPx).roundToInt()
                                    )
                                }
                                .size(12.dp)
                                .background(colors.primary, shape = RoundedCornerShape(50))
                                .pointerInput(node.job.id, output) {
                                    detectDragGestures(
                                        onDragStart = {
                                            draggingFromJobId = node.job.id
                                            dragStart = output
                                            dragCurrent = output
                                        },
                                        onDragEnd = {
                                            val sourceJobId = draggingFromJobId
                                            val endOffset = dragCurrent
                                            if (sourceJobId != null && endOffset != null) {
                                                val target = inputCenterByJobId.entries
                                                    .asSequence()
                                                    .filter { (jobId, _) -> jobId != sourceJobId }
                                                    .map { entry -> entry to distance(entry.value, endOffset) }
                                                .filter { (_, d) -> d <= connectorSnapRadiusPx }
                                                .minByOrNull { (_, d) -> d }
                                                ?.first
                                                if (target != null) {
                                                    val sourceNode = nodeById[sourceJobId]
                                                    val targetNode = nodeById[target.key]
                                                    if (sourceNode == null || targetNode == null) {
                                                        onDependencyRejected("Dependency target is invalid.")
                                                    } else if (sourceNode.stageId == targetNode.stageId &&
                                                        sourceNode.order > targetNode.order
                                                    ) {
                                                        onDependencyRejected("Order $sourceNode.order cannot link to lower order ${targetNode.order}.")
                                                    } else {
                                                        val sourceStageOrder = stageById[sourceNode.stageId]?.order ?: 0
                                                        val targetStageOrder = stageById[targetNode.stageId]?.order ?: 0
                                                        if (sourceStageOrder > targetStageOrder) {
                                                            onDependencyRejected("Cannot link from later stage to earlier stage.")
                                                        } else {
                                                            onCreateDependency(target.key, sourceJobId)
                                                        }
                                                    }
                                                } else {
                                                    onDependencyRejected("No target job matched for dependency link.")
                                                }
                                            }
                                            draggingFromJobId = null
                                            dragStart = null
                                            dragCurrent = null
                                        },
                                        onDragCancel = {
                                            draggingFromJobId = null
                                            dragStart = null
                                            dragCurrent = null
                                        }
                                    ) { change, dragAmount ->
                                        change.consume()
                                        val current = dragCurrent ?: output
                                        dragCurrent = Offset(current.x + dragAmount.x, current.y + dragAmount.y)
                                    }
                                }
                        )
                    }
                }
            }
            HorizontalScrollbar(
                adapter = rememberScrollbarAdapter(horizontalScroll),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }
    }
}

private data class NodeLayout(
    val job: Job,
    val topLeft: Offset,
    val stageId: String,
    val order: Int
)

private fun distance(a: Offset, b: Offset): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return sqrt(dx * dx + dy * dy)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrowHead(
    start: Offset,
    end: Offset,
    color: Color
) {
    val angle = atan2(end.y - start.y, end.x - start.x)
    val arrowLength = 10.dp.toPx()
    val arrowAngle = Math.toRadians(22.0).toFloat()

    val point1 = Offset(
        x = end.x - arrowLength * cos(angle - arrowAngle),
        y = end.y - arrowLength * sin(angle - arrowAngle)
    )
    val point2 = Offset(
        x = end.x - arrowLength * cos(angle + arrowAngle),
        y = end.y - arrowLength * sin(angle + arrowAngle)
    )
    drawLine(color = color, start = end, end = point1, strokeWidth = 2.dp.toPx())
    drawLine(color = color, start = end, end = point2, strokeWidth = 2.dp.toPx())
}
