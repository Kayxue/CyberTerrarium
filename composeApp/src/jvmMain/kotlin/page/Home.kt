package page

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import state.rememberSystemUsage
import terrarium.Terrarium
import kotlin.math.roundToInt

@Composable
fun Home(){
    val usage = rememberSystemUsage()
    var availableFish by remember { mutableIntStateOf(0) }
    var maxVisibleFish by remember { mutableIntStateOf(0) }
    var hasInitializedFishLimit by remember { mutableStateOf(false) }

    val cpuAnimatedProgress by animateFloatAsState(targetValue = usage?.cpuUsagePercent()?.toFloat()?.div(100) ?: 0f, animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec)

    val memoryAnimatedProgress by animateFloatAsState(
        targetValue = usage?.memoryUsagePercent()?.toFloat()?.div(100)
            ?: 0f, animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    )

    val colors = MaterialTheme.colorScheme

    Column(modifier = Modifier.padding(16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(3f)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surfaceVariant)
        ) {
            Terrarium(
                systemUsage = usage,
                maxVisibleFish = maxVisibleFish,
                onAvailableFishChanged = { count ->
                    if (!hasInitializedFishLimit && count > 0) {
                        availableFish = count
                        maxVisibleFish = minOf(100, count)
                        hasInitializedFishLimit = true
                    } else {
                        val wasShowingAll = maxVisibleFish == availableFish
                        availableFish = count
                        maxVisibleFish = when {
                            wasShowingAll -> count
                            maxVisibleFish > count -> count
                            else -> maxVisibleFish
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Available fish",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Slider(
                    value = maxVisibleFish.toFloat(),
                    onValueChange = {
                        maxVisibleFish = it.roundToInt().coerceIn(0, availableFish)
                    },
                    valueRange = 0f..availableFish.coerceAtLeast(1).toFloat(),
                    enabled = availableFish > 0,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.30f),
                        disabledThumbColor = Color.White.copy(alpha = 0.55f),
                        disabledActiveTrackColor = Color.White.copy(alpha = 0.55f),
                        disabledInactiveTrackColor = Color.White.copy(alpha = 0.18f),
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent,
                        disabledActiveTickColor = Color.Transparent,
                        disabledInactiveTickColor = Color.Transparent
                    ),
                    modifier = Modifier.width(260.dp)
                )
                Text(
                    "$maxVisibleFish",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "All fish: $availableFish",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Card(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colors.surface,
                    contentColor = colors.onSurface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CPU", fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${usage?.cpuUsagePercent?.roundToInt() ?: 0}% / ${usage?.cpuTemperature?.roundToInt() ?: 0}°C",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                        Spacer(Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { cpuAnimatedProgress },
                            modifier = Modifier.fillMaxWidth(),
                            color = colors.primary,
                            trackColor = colors.surfaceVariant,
                            strokeCap = StrokeCap.Round
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Card(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colors.surface,
                    contentColor = colors.onSurface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Memory", fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${
                                "%.1f / %.1f".format(
                                    usage?.memoryUsedBytes?.toFloat()?.div(1024)?.div(1024)?.div(1024) ?: 0f,
                                    usage?.memoryTotalBytes?.toFloat()?.div(1024)?.div(1024)?.div(1024) ?: 0f
                                )
                            } GB", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.onSurface
                        )
                        Spacer(Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { memoryAnimatedProgress },
                            modifier = Modifier.fillMaxWidth(),
                            color = colors.secondary,
                            trackColor = colors.surfaceVariant,
                            strokeCap = StrokeCap.Round
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Card(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colors.surface,
                        contentColor = colors.onSurface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.Center){
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically){
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Download,
                                    contentDescription = "Download",
                                    tint = colors.primary
                                )
                                Text("Download:", fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                            }
                            Text(Utils.formatSpeed(usage?.downloadBytesPerSecond ?: 0), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colors.surface,
                        contentColor = colors.onSurface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.Center){
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically){
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Upload,
                                    contentDescription = "Upload",
                                    tint = colors.secondary
                                )
                                Text("Upload:", fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                            }
                            Text(Utils.formatSpeed(usage?.uploadBytesPerSecond ?: 0), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
                        }
                    }
                }
            }
        }
    }
}
