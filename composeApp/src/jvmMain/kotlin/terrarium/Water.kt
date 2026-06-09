package terrarium

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import terrarium.model.TerrariumEnvironmentState
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun WaterCanvas(
    environment: TerrariumEnvironmentState = TerrariumEnvironmentState.healthy(),
    modifier: Modifier = Modifier.fillMaxSize()
) {
    Canvas(modifier = modifier) {
        drawTerrariumWater(environment, phase = 0f)
    }
}

internal fun DrawScope.drawTerrariumWater(
    environment: TerrariumEnvironmentState,
    phase: Float,
    style: EnvironmentStyle = environmentStyle(environment)
) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(style.waterTop, style.waterBottom)
        ),
        size = size
    )

    val clarityLoss = 1f - environment.clarity / 100f
    val hazeAlpha = when (style.band) {
        EnvironmentBand.THRIVING -> 0.02f
        EnvironmentBand.STABLE -> 0.07f
        EnvironmentBand.STRESSED -> 0.16f
        EnvironmentBand.CRITICAL -> 0.28f
    } + clarityLoss * 0.22f
    drawRect(style.haze.copy(alpha = hazeAlpha.coerceIn(0f, 0.5f)), size = size)

    if (environment.temperatureStress > 35) {
        drawRect(
            Color(0xFFFF8A65).copy(alpha = environment.temperatureStress / 700f),
            size = size
        )
    }

    val lineCount = (2 + environment.motion / 18).coerceIn(2, 7)
    val movement = phase * size.width * 0.18f
    repeat(lineCount) { index ->
        val baseY = size.height * (0.18f + index * 0.09f)
        val path = Path()
        val startX = -size.width * 0.12f + movement % (size.width * 0.3f)
        path.moveTo(startX, baseY)
        val amplitude = size.height * (0.008f + environment.waveIntensity.toFloat() * 0.012f)
        val segments = 6
        repeat(segments) { segment ->
            val x = startX + size.width * (segment + 1) / segments
            val y = baseY + sin((segment + phase * 2f) * PI).toFloat() * amplitude
            path.lineTo(x, y)
        }
        drawPath(
            path,
            style.current.copy(alpha = 0.08f + environment.motion / 550f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f)
        )
    }

    val count = (5 + environment.bubbleIntensity * 13).toInt().coerceIn(5, 18)
    repeat(count) { index ->
        val seed = index * 73 + 19
        val x = size.width * (0.08f + ((seed % 83) / 100f))
        val baseY = size.height * (0.25f + ((seed * 7 % 65) / 100f))
        val travel = (phase + (seed % 10) / 10f) % 1f
        val y = (baseY - travel * size.height * 0.32f).let {
            if (it < size.height * 0.08f) it + size.height * 0.68f else it
        }
        val radius = 2.2f + (seed % 5)
        drawCircle(
            Color.White.copy(alpha = 0.18f + environment.clarity / 500f),
            radius = radius,
            center = Offset(x, y),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.1f)
        )
    }
}
