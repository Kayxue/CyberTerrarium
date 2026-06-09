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
    val healthRatio = (fish.health / 100f).coerceIn(0f, 1f)
    val stressRatio = (fish.stress / 100f).coerceIn(0f, 1f)
    val statusTint = when (fish.status) {
        TerrariumCreatureStatus.HEALTHY -> Color(0xFFF5FBFF)
        TerrariumCreatureStatus.STRESSED -> Color(0xFFD59A45)
        TerrariumCreatureStatus.SICK -> Color(0xFF737A6D)
        TerrariumCreatureStatus.INACTIVE -> Color(0xFF747A7D)
        TerrariumCreatureStatus.UNKNOWN -> Color(0xFF87929A)
    }
    val statusBlend = when (fish.status) {
        TerrariumCreatureStatus.HEALTHY -> 0.08f
        TerrariumCreatureStatus.STRESSED -> 0.18f + stressRatio * 0.16f
        TerrariumCreatureStatus.SICK -> 0.50f + (1f - healthRatio) * 0.22f
        TerrariumCreatureStatus.INACTIVE -> 0.62f
        TerrariumCreatureStatus.UNKNOWN -> 0.38f
    }
    val tintedBase = base.mixWith(statusTint, statusBlend)
    val brightness = when (fish.status) {
        TerrariumCreatureStatus.HEALTHY -> 0.88f + healthRatio * 0.18f
        TerrariumCreatureStatus.STRESSED -> 0.76f + healthRatio * 0.14f
        TerrariumCreatureStatus.SICK -> 0.58f + healthRatio * 0.18f
        TerrariumCreatureStatus.INACTIVE -> 0.52f
        TerrariumCreatureStatus.UNKNOWN -> 0.68f
    }
    val bodyColor = tintedBase.scaleBrightness(brightness).copy(
        alpha = when (fish.status) {
            TerrariumCreatureStatus.INACTIVE -> 0.48f
            TerrariumCreatureStatus.SICK -> 0.68f + healthRatio * 0.18f
            TerrariumCreatureStatus.UNKNOWN -> 0.72f
            else -> 0.82f + healthRatio * 0.18f
        } * normalizedOpacity
    )
    val finColor = bodyColor
        .mixWith(Color.Black, if (fish.status == TerrariumCreatureStatus.HEALTHY) 0.08f else 0.18f)
        .copy(alpha = bodyColor.alpha * 0.86f)
    val height = width * when (fish.status) {
        TerrariumCreatureStatus.HEALTHY -> 0.54f
        TerrariumCreatureStatus.STRESSED -> 0.51f
        TerrariumCreatureStatus.SICK -> 0.47f
        TerrariumCreatureStatus.INACTIVE -> 0.45f
        TerrariumCreatureStatus.UNKNOWN -> 0.49f
    }

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

        val tailDrop = when (fish.status) {
            TerrariumCreatureStatus.SICK -> height * 0.16f
            TerrariumCreatureStatus.INACTIVE -> height * 0.22f
            else -> 0f
        }
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

private fun Color.mixWith(other: Color, amount: Float): Color {
    val ratio = amount.coerceIn(0f, 1f)
    return Color(
        red = red + (other.red - red) * ratio,
        green = green + (other.green - green) * ratio,
        blue = blue + (other.blue - blue) * ratio,
        alpha = alpha + (other.alpha - alpha) * ratio
    )
}

private fun Color.scaleBrightness(factor: Float): Color = Color(
    red = (red * factor).coerceIn(0f, 1f),
    green = (green * factor).coerceIn(0f, 1f),
    blue = (blue * factor).coerceIn(0f, 1f),
    alpha = alpha
)
