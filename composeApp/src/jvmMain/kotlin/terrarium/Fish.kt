package terrarium

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import terrarium.model.TerrariumCreatureKind
import terrarium.model.TerrariumCreatureStatus
import terrarium.model.TerrariumFishState
import kotlin.math.abs

@Composable
fun FishCanvas() {
    Canvas(modifier = Modifier.size(240.dp, 140.dp)) {
        drawOval(
            Color(0xFF4FC3F7),
            topLeft = Offset(size.width * 0.25f, size.height * 0.25f),
            size = Size(size.width * 0.5f, size.height * 0.5f)
        )
    }
}

internal fun DrawScope.drawTerrariumFish(
    fish: TerrariumFishState,
    center: Offset,
    width: Float,
    facingScale: Float,
    opacity: Float = 1f
) {
    val normalizedOpacity = opacity.coerceIn(0f, 1f)
    val jobPalette = listOf(
        Color(0xFFFFB74D),
        Color(0xFFFF8A65),
        Color(0xFFBA68C8),
        Color(0xFFFFD54F)
    )
    val processPalette = listOf(
        Color(0xFF4FC3F7),
        Color(0xFF4DB6AC),
        Color(0xFF81C784),
        Color(0xFF7986CB)
    )
    val palette = if (fish.kind == TerrariumCreatureKind.JOB) jobPalette else processPalette
    val base = palette[abs(fish.visualHint.colorSeed.toLong()).rem(palette.size).toInt()]
    val damage = ((60 - fish.health).coerceAtLeast(0) / 60f).coerceIn(0f, 1f)
    val sickTarget = if (fish.status == TerrariumCreatureStatus.SICK) {
        Color(0xFF797D70)
    } else {
        Color(0xFF87959A)
    }
    val bodyColor = Color(
        red = base.red * (1f - damage) + sickTarget.red * damage,
        green = base.green * (1f - damage) + sickTarget.green * damage,
        blue = base.blue * (1f - damage) + sickTarget.blue * damage,
        alpha = (0.58f + fish.health / 240f).coerceIn(0.58f, 1f) * normalizedOpacity
    )
    val finColor = bodyColor.copy(alpha = bodyColor.alpha * 0.78f)
    val height = width * 0.52f

    withTransform({
        translate(center.x, center.y)
        scale(
            scaleX = facingScale.coerceIn(-1f, 1f),
            scaleY = 1f,
            pivot = Offset.Zero
        )
    }) {
        drawOval(
            bodyColor,
            topLeft = Offset(-width * 0.30f, -height * 0.32f),
            size = Size(width * 0.62f, height * 0.64f)
        )

        val tailDrop = if (fish.status == TerrariumCreatureStatus.SICK) height * 0.14f else 0f
        val tail = Path().apply {
            moveTo(-width * 0.28f, 0f)
            lineTo(-width * 0.52f, -height * 0.35f + tailDrop)
            lineTo(-width * 0.52f, height * 0.35f + tailDrop)
            close()
        }
        drawPath(tail, finColor)

        val topFin = Path().apply {
            moveTo(-width * 0.08f, -height * 0.28f)
            lineTo(width * 0.04f, -height * 0.56f)
            lineTo(width * 0.13f, -height * 0.24f)
            close()
        }
        drawPath(topFin, finColor)

        if (fish.kind == TerrariumCreatureKind.JOB) {
            drawLine(
                Color.White.copy(alpha = 0.38f * normalizedOpacity),
                Offset(-width * 0.02f, -height * 0.24f),
                Offset(-width * 0.02f, height * 0.24f),
                strokeWidth = (width * 0.045f).coerceAtLeast(1.5f)
            )
        } else {
            repeat(3) { index ->
                drawCircle(
                    Color.White.copy(alpha = 0.28f * normalizedOpacity),
                    radius = width * 0.025f,
                    center = Offset(-width * 0.10f + index * width * 0.09f, height * 0.05f)
                )
            }
        }

        drawCircle(
            Color.White.copy(alpha = normalizedOpacity),
            radius = width * 0.055f,
            center = Offset(width * 0.19f, -height * 0.09f)
        )
        drawCircle(
            (if (fish.stress > 70) Color(0xFFB71C1C) else Color(0xFF172126))
                .copy(alpha = normalizedOpacity),
            radius = width * 0.025f,
            center = Offset(width * 0.205f, -height * 0.09f)
        )

        val mouthStart = if (fish.health < 45) 205f else 20f
        drawArc(
            Color(0xFF172126).copy(alpha = normalizedOpacity),
            startAngle = mouthStart,
            sweepAngle = 120f,
            useCenter = false,
            topLeft = Offset(width * 0.20f, height * 0.02f),
            size = Size(width * 0.12f, height * 0.16f),
            style = Stroke(width = (width * 0.018f).coerceAtLeast(1f))
        )
    }
}
