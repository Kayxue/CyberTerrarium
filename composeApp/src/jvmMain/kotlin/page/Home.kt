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
import terrarium.FishCanvas
import terrarium.WaterCanvas

@Composable
fun Home(){
    val cpuAnimatedProgress by animateFloatAsState(targetValue = 0.39f, animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec)

    val memoryAnimatedProgress by animateFloatAsState(targetValue = 0.56f, animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec)

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
                        Text("CPU Usage", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text("39%", fontSize = 24.sp, fontWeight = FontWeight.Bold)
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
                        Text("Memory Usage", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text("4.5 GB", fontSize = 24.sp, fontWeight = FontWeight.Bold)
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
                            Text("Running\nProcess", fontWeight = FontWeight.SemiBold)
                            Text("24", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth().weight(1f), shape = RoundedCornerShape(12.dp), elevation = 4.dp) {
                    Box(modifier = Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.Center){
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically){
                            Text("Today\nFinished", fontWeight = FontWeight.SemiBold)
                            Text("142", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}