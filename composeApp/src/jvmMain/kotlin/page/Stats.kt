package page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.AnimationMode
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.IndicatorCount
import ir.ehsannarmani.compose_charts.models.LabelHelperProperties
import ir.ehsannarmani.compose_charts.models.LabelProperties
import ir.ehsannarmani.compose_charts.models.Line
import state.rememberSystemUsageHistory
import kotlin.math.roundToInt

private const val SAMPLE_INTERVAL_MILLIS = 1000L
private const val MAX_POINTS = 60

@Composable
fun Stats() {
    val usageHistory = rememberSystemUsageHistory(
        intervalMillis = SAMPLE_INTERVAL_MILLIS,
        maxPoints = MAX_POINTS
    )
    val latestUsage = usageHistory.latest

    val colors = MaterialTheme.colorScheme
    val cpuUsagePercent = latestUsage?.cpuUsagePercent?.roundToInt() ?: 0
    val cpuFrequency = Utils.formatFrequency(latestUsage?.cpuCurrentFrequencyHz ?: 0L)
    val memoryUsagePercent = latestUsage?.memoryUsagePercent?.roundToInt() ?: 0
    val memoryUsed = Utils.formatBytes(latestUsage?.memoryUsedBytes ?: 0L)
    val memoryTotal = Utils.formatBytes(latestUsage?.memoryTotalBytes ?: 0L)

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            UsageChartCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                title = "CPU Usage",
                headerValue = AnnotatedString("$cpuFrequency • $cpuUsagePercent%"),
                lines = listOf(
                    Line(
                        label = "CPU",
                        values = usageHistory.cpu,
                        color = SolidColor(colors.primary)
                    )
                ),
                indicatorFormatter = { "${it.roundToInt()}%" },
                maxValue = 100.0
            )

            UsageChartCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                title = "Memory Usage",
                headerValue = AnnotatedString("$memoryUsed / $memoryTotal • $memoryUsagePercent%"),
                lines = listOf(
                    Line(
                        label = "Memory",
                        values = usageHistory.memory,
                        color = SolidColor(colors.secondary)
                    )
                ),
                indicatorFormatter = { "${it.roundToInt()}%" },
                maxValue = 100.0
            )
        }

        UsageChartCard(
            title = "Network Speed (KB/s)",
            headerValue = buildAnnotatedString {
                withStyle(SpanStyle(color = colors.primary)) {
                    append("Down ${Utils.formatSpeed(latestUsage?.downloadBytesPerSecond ?: 0)}")
                }
                append(" • ")
                withStyle(SpanStyle(color = colors.tertiary)) {
                    append("Up ${Utils.formatSpeed(latestUsage?.uploadBytesPerSecond ?: 0)}")
                }
            },
            lines = listOf(
                Line(
                    label = "Download",
                    values = usageHistory.downloadKb,
                    color = SolidColor(colors.primary)
                ),
                Line(
                    label = "Upload",
                    values = usageHistory.uploadKb,
                    color = SolidColor(colors.tertiary)
                )
            ),
            indicatorFormatter = { "${it.roundToInt()} KB/s" },
            legendItems = listOf(
                LegendItem(label = "Download", color = colors.primary),
                LegendItem(label = "Upload", color = colors.tertiary)
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

private data class LegendItem(
    val label: String,
    val color: Color
)

@Composable
private fun UsageChartCard(
    modifier: Modifier = Modifier,
    title: String,
    headerValue: AnnotatedString,
    lines: List<Line>,
    indicatorFormatter: (Double) -> String,
    maxValue: Double? = null,
    legendItems: List<LegendItem> = emptyList()
) {
    val colors = MaterialTheme.colorScheme
    val indicatorProperties = HorizontalIndicatorProperties(
        textStyle = MaterialTheme.typography.labelSmall.copy(color = colors.onSurfaceVariant),
        count = IndicatorCount.CountBased(count = 4),
        contentBuilder = indicatorFormatter
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface,
            contentColor = colors.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = headerValue, style = MaterialTheme.typography.titleMedium.copy(color = colors.onSurfaceVariant))
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (maxValue != null) {
                LineChart(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                    data = lines,
                    animationMode = AnimationMode.None,
                    animationDelay = 0,
                    indicatorProperties = indicatorProperties,
                    labelProperties = LabelProperties(enabled = false),
                    labelHelperProperties = LabelHelperProperties(enabled = false),
                    minValue = 0.0,
                    maxValue = maxValue
                )
            } else {
                LineChart(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                    data = lines,
                    animationMode = AnimationMode.None,
                    animationDelay = 0,
                    indicatorProperties = indicatorProperties,
                    labelProperties = LabelProperties(enabled = false),
                    labelHelperProperties = LabelHelperProperties(enabled = false),
                    minValue = 0.0
                )
            }

            if (legendItems.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    legendItems.forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(item.color, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = item.label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
