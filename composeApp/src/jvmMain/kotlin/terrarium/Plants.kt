package terrarium

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import terrarium.model.TerrariumEnvironmentState
import kotlin.math.PI
import kotlin.math.sin

internal fun DrawScope.drawTerrariumPlants(
    environment: TerrariumEnvironmentState,
    phase: Float,
    style: EnvironmentStyle
) {
    val clumpPositions = listOf(0.07f, 0.17f, 0.31f, 0.45f, 0.59f, 0.72f, 0.84f, 0.93f)
    val visibleClumpCount = 3f + environment.health / 100f * 5f
    clumpPositions.forEachIndexed { clumpIndex, xRatio ->
        val clumpAlpha = (visibleClumpCount - clumpIndex).coerceIn(0f, 1f)
        if (clumpAlpha <= 0f) {
            return@forEachIndexed
        }

        val baseX = size.width * xRatio
        val baseY = size.height * 0.89f
        val bladeCount = 5 + ((environment.health + clumpIndex * 13) / 35).coerceIn(0, 3)
        repeat(bladeCount) { bladeIndex ->
            val centeredBlade = bladeIndex - (bladeCount - 1) / 2f
            val rootSpread = size.width * 0.0065f
            val bladeHeight = size.height * (
                0.10f +
                    (bladeIndex % 4) * 0.018f +
                    (clumpIndex % 3) * 0.008f
                )
            val naturalLean = centeredBlade * size.width * 0.006f
            val sway = sin(
                (phase * 2f + clumpIndex * 0.43f + bladeIndex * 0.17f) * PI
            ).toFloat() * size.width * (0.004f + environment.motion / 8500f)
            val rootX = baseX + centeredBlade * rootSpread
            val tipX = rootX + naturalLean + sway
            val path = Path().apply {
                moveTo(rootX, baseY)
                cubicTo(
                    rootX - naturalLean * 0.35f - sway * 0.25f,
                    baseY - bladeHeight * 0.32f,
                    tipX - sway * 0.45f,
                    baseY - bladeHeight * 0.72f,
                    tipX,
                    baseY - bladeHeight
                )
            }
            drawPath(
                path,
                style.plant.copy(
                    alpha = clumpAlpha *
                        (0.48f + environment.health / 300f) *
                        (0.78f + (bladeIndex % 3) * 0.08f)
                ),
                style = Stroke(width = 1.8f + (bladeIndex % 3) * 0.75f)
            )
        }
    }
}
