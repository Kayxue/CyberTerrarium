package com.kay.cyberterrarium.jobmanagement.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import job.model.Job
import job.model.JobDependency
import job.model.flow.FlowJobLink
import job.model.stage.FlowStage
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun FlowGraphView(
    selectedFlowId: String?,
    flowStages: List<FlowStage>,
    flowJobLinks: List<FlowJobLink>,
    jobsById: Map<String, Job>,
    dependencies: List<JobDependency>,
    selection: GraphSelection?,
    onSelect: (GraphSelection) -> Unit
) {
    if (selectedFlowId.isNullOrBlank()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("選擇任何 flow 來顯示關係圖", style = MaterialTheme.typography.titleMedium)
        }
        return
    }

    val sortedLinks = flowJobLinks.sortedBy { it.position }
    val graphJobs = sortedLinks.mapNotNull { jobsById[it.jobId] }
    val stageById = flowStages.associateBy { it.id }
    val stageIds = buildList {
        addAll(flowStages.sortedBy { it.order }.map { it.id })
        for (job in graphJobs) {
            if (job.stageId !in this) {
                add(job.stageId)
            }
        }
    }.distinct().ifEmpty { listOf("unassigned-stage") }
    val stageLabelById = stageIds.associateWith { stageId ->
        stageById[stageId]?.displayName?.ifBlank { stageId } ?: stageId
    }
    val stageIndexById = stageIds.withIndex().associate { it.value to it.index }
    val density = LocalDensity.current
    val colors = MaterialTheme.colorScheme

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .heightIn(min = 460.dp)
            .background(colors.surfaceVariant.copy(alpha = 0.2f))
    ) {
        val graphWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val graphHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val laneCount = stageIds.size.coerceAtLeast(1)
        val laneWidthPx = graphWidthPx / laneCount.toFloat()

        val nodeWidthPx = with(density) { 170.dp.toPx() }
        val nodeHeightPx = with(density) { 74.dp.toPx() }
        val lanePaddingPx = with(density) { 14.dp.toPx() }
        val laneHeaderHeightPx = with(density) { 54.dp.toPx() }
        val rowGapPx = with(density) { 92.dp.toPx() }
        val staggerOffsetPx = with(density) { 28.dp.toPx() }
        val rowTopOffsetPx = with(density) { 18.dp.toPx() }
        val rowBottomPaddingPx = with(density) { 10.dp.toPx() }
        val edgeStartMinDeltaPx = with(density) { 20.dp.toPx() }
        val edgeMarkerHalfPx = with(density) { 8.dp.toPx() }
        val stageRowCursor = mutableMapOf<String, Int>()

        val nodeLayouts = sortedLinks.mapNotNull { link ->
            val job = jobsById[link.jobId] ?: return@mapNotNull null
            val stageId = job.stageId
            val stageIndex = stageIndexById[stageId] ?: 0
            val rowIndex = stageRowCursor.getOrDefault(stageId, 0)
            stageRowCursor[stageId] = rowIndex + 1

            val laneStartX = stageIndex * laneWidthPx
            val laneEndX = laneStartX + laneWidthPx
            val intraOffset = if (rowIndex % 2 == 0) 0f else staggerOffsetPx
            val x = (laneStartX + lanePaddingPx + intraOffset)
                .coerceAtMost(laneEndX - nodeWidthPx - lanePaddingPx)
            val y = (laneHeaderHeightPx + rowTopOffsetPx + rowIndex * rowGapPx)
                .coerceAtMost(graphHeightPx - nodeHeightPx - rowBottomPaddingPx)

            NodeLayout(job = job, topLeft = Offset(x, y))
        }
        val nodeById = nodeLayouts.associateBy { it.job.id }

        val visibleDependencies = dependencies.filter { dependency ->
            nodeById.containsKey(dependency.jobId) && nodeById.containsKey(dependency.upstreamJobId)
        }

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
                        .weight(1f)
                        .fillMaxSize()
                        .background(stageTint)
                        .clickable { onSelect(GraphSelection.StageSelection(stageId)) }
                ) {
                    Text(
                        text = stageLabelById[stageId] ?: stageId,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        color = colors.onSurface
                    )
                }
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            for (dividerIndex in 1 until laneCount) {
                val x = dividerIndex * laneWidthPx
                drawLine(
                    color = colors.outline.copy(alpha = 0.55f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }

            visibleDependencies.forEach { dependency ->
                val source = nodeById[dependency.upstreamJobId] ?: return@forEach
                val target = nodeById[dependency.jobId] ?: return@forEach

                val start = Offset(source.topLeft.x + nodeWidthPx, source.topLeft.y + nodeHeightPx / 2f)
                val end = Offset(target.topLeft.x, target.topLeft.y + nodeHeightPx / 2f)
                val controlDelta = ((end.x - start.x).coerceAtLeast(edgeStartMinDeltaPx)) * 0.35f

                val path = Path().apply {
                    moveTo(start.x, start.y)
                    cubicTo(
                        start.x + controlDelta, start.y,
                        end.x - controlDelta, end.y,
                        end.x, end.y
                    )
                }

                val selectedEdge = selection is GraphSelection.DependencySelection &&
                    selection.jobId == dependency.jobId &&
                    selection.upstreamJobId == dependency.upstreamJobId
                val edgeColor = if (selectedEdge) {
                    colors.primary
                } else {
                    colors.outline
                }
                drawPath(path = path, color = edgeColor, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))

                drawArrowHead(start = start, end = end, color = edgeColor)
            }
        }

        nodeLayouts.forEach { node ->
            val selectedNode = selection is GraphSelection.JobSelection && selection.jobId == node.job.id
            val nodeColor = if (selectedNode) {
                colors.primaryContainer
            } else {
                colors.surface
            }
            val nodeBorder = if (selectedNode) {
                colors.primary
            } else {
                colors.outline.copy(alpha = 0.5f)
            }
            Card(
                modifier = Modifier
                    .offset { IntOffset(node.topLeft.x.roundToInt(), node.topLeft.y.roundToInt()) }
                    .width(170.dp)
                    .heightIn(min = 74.dp)
                    .clickable { onSelect(GraphSelection.JobSelection(node.job.id)) },
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, nodeBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(nodeColor)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "${node.job.title}\n${node.job.id.take(8)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Small click target on each dependency edge.
        visibleDependencies.forEach { dependency ->
            val source = nodeById[dependency.upstreamJobId] ?: return@forEach
            val target = nodeById[dependency.jobId] ?: return@forEach
            val startX = source.topLeft.x + nodeWidthPx
            val startY = source.topLeft.y + nodeHeightPx / 2f
            val endX = target.topLeft.x
            val endY = target.topLeft.y + nodeHeightPx / 2f
            val markerX = ((startX + endX) / 2f) - edgeMarkerHalfPx
            val markerY = ((startY + endY) / 2f) - edgeMarkerHalfPx
            val selectedEdge = selection is GraphSelection.DependencySelection &&
                selection.jobId == dependency.jobId &&
                selection.upstreamJobId == dependency.upstreamJobId

            Box(
                modifier = Modifier
                    .offset { IntOffset(markerX.roundToInt(), markerY.roundToInt()) }
                    .width(16.dp)
                    .heightIn(min = 16.dp)
                    .background(
                        color = if (selectedEdge) {
                            colors.primary
                        } else {
                            colors.outline
                        },
                        shape = RoundedCornerShape(50)
                    )
                    .clickable {
                        onSelect(GraphSelection.DependencySelection(dependency.jobId, dependency.upstreamJobId))
                    }
            )
        }
    }
}

private data class NodeLayout(
    val job: Job,
    val topLeft: Offset
)

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
