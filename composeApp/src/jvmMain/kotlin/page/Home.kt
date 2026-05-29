package page

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.ProgressIndicatorDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import state.rememberSystemUsage
import terrarium.FishCanvas
import terrarium.WaterCanvas
import kotlin.math.roundToInt

@Composable
fun Home(){
    val usage = rememberSystemUsage()

    val cpuAnimatedProgress by animateFloatAsState(targetValue = usage?.cpuUsagePercent()?.toFloat()?.div(100) ?: 0f, animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec)

    val memoryAnimatedProgress by animateFloatAsState(
        targetValue = usage?.memoryUsagePercent()?.toFloat()?.div(100)
            ?: 0f, animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    )

    Column(modifier = Modifier.padding(16.dp)) {
        Box(modifier = Modifier.fillMaxWidth().weight(3f).clip(RoundedCornerShape(12.dp))) {
            WaterCanvas()
            FishCanvas()
        }

        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Card(modifier = Modifier.weight(1f).fillMaxHeight(), shape = RoundedCornerShape(12.dp), elevation = 4.dp) {
                Box(modifier = Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CPU", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${usage?.cpuUsagePercent?.roundToInt() ?: 0}%",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { cpuAnimatedProgress },
                            modifier = Modifier.fillMaxWidth(),
                            color = androidx.compose.material3.ProgressIndicatorDefaults.linearColor,
                            trackColor = Color(0xFFE6E0E9),
                            strokeCap = StrokeCap.Round
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Card(modifier = Modifier.weight(1f).fillMaxHeight(), shape = RoundedCornerShape(12.dp), elevation = 4.dp) {
                Box(modifier = Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Memory", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${
                                "%.1f / %.1f".format(
                                    usage?.memoryUsedBytes?.toFloat()?.div(1024)?.div(1024)?.div(1024) ?: 0f,
                                    usage?.memoryTotalBytes?.toFloat()?.div(1024)?.div(1024)?.div(1024) ?: 0f
                                )
                            } GB", fontSize = 20.sp, fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { memoryAnimatedProgress },
                            modifier = Modifier.fillMaxWidth(),
                            color = androidx.compose.material3.ProgressIndicatorDefaults.linearColor,
                            trackColor = Color(0xFFE6E0E9),
                            strokeCap = StrokeCap.Round
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Card(modifier = Modifier.fillMaxWidth().weight(1f), shape = RoundedCornerShape(12.dp), elevation = 4.dp) {
                    Box(modifier = Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.Center){
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically){
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Download,
                                    contentDescription = "Download"
                                )
                                Text("Download:", fontWeight = FontWeight.SemiBold)
                            }
                            Text("${formatBytes(usage?.downloadBytesPerSecond ?: 0)}/s", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth().weight(1f), shape = RoundedCornerShape(12.dp), elevation = 4.dp) {
                    Box(modifier = Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.Center){
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically){
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Upload,
                                    contentDescription = "Upload"
                                )
                                Text("Upload:", fontWeight = FontWeight.SemiBold)
                            }
                            Text("${formatBytes(usage?.uploadBytesPerSecond ?: 0)}/s", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0

    return when {
        bytes >= gb -> "%.2f GB".format(bytes / gb)
        bytes >= mb -> "%.2f MB".format(bytes / mb)
        bytes >= kb -> "%.2f KB".format(bytes / kb)
        else -> "$bytes B"
    }
}