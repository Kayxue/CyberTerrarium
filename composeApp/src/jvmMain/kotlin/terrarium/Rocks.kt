package terrarium

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope

internal fun DrawScope.drawTerrariumRocks(style: EnvironmentStyle) {
    val clusters = listOf(
        listOf(
            RockShape(0.12f, 0.89f, 0.145f, 0.072f, -0.10f, 0.82f),
            RockShape(0.055f, 0.89f, 0.072f, 0.050f, -0.22f, 0.64f),
            RockShape(0.185f, 0.89f, 0.082f, 0.057f, 0.16f, 0.72f),
            RockShape(0.125f, 0.835f, 0.083f, 0.058f, 0.08f, 1f)
        ),
        listOf(
            RockShape(0.81f, 0.89f, 0.18f, 0.082f, 0.08f, 0.82f),
            RockShape(0.72f, 0.89f, 0.090f, 0.061f, -0.18f, 0.68f),
            RockShape(0.895f, 0.89f, 0.095f, 0.068f, 0.18f, 0.74f),
            RockShape(0.785f, 0.827f, 0.098f, 0.066f, -0.08f, 0.94f),
            RockShape(0.855f, 0.842f, 0.072f, 0.052f, 0.20f, 1f)
        )
    )

    clusters.forEach { cluster ->
        val left = cluster.minOf { it.centerX - it.width / 2f }
        val right = cluster.maxOf { it.centerX + it.width / 2f }
        val floor = cluster.maxOf { it.floorY }
        val shadowHeight = size.height * 0.012f
        drawOval(
            color = Color.Black.copy(alpha = 0.18f),
            topLeft = Offset(size.width * left, size.height * floor - shadowHeight / 2f),
            size = Size(
                width = size.width * (right - left),
                height = shadowHeight
            )
        )

        cluster.forEach { rock ->
            drawRock(rock, style.rock)
        }
    }
}

private fun DrawScope.drawRock(rock: RockShape, baseColor: Color) {
    val centerX = size.width * rock.centerX
    val floorY = size.height * rock.floorY
    val width = size.width * rock.width
    val height = size.height * rock.height
    val left = centerX - width / 2f
    val right = centerX + width / 2f
    val lean = width * rock.lean
    val color = baseColor.mixWith(Color.White, 0.04f + rock.light * 0.08f)

    val silhouette = Path().apply {
        moveTo(left, floorY)
        lineTo(left + width * 0.04f, floorY - height * 0.24f)
        cubicTo(
            left + width * 0.13f + lean * 0.15f,
            floorY - height * 0.66f,
            left + width * 0.32f + lean * 0.55f,
            floorY - height * 0.96f,
            centerX + lean,
            floorY - height
        )
        cubicTo(
            right - width * 0.29f + lean * 0.35f,
            floorY - height * 0.98f,
            right - width * 0.10f + lean * 0.08f,
            floorY - height * 0.61f,
            right - width * 0.02f,
            floorY - height * 0.20f
        )
        lineTo(right, floorY)
        close()
    }
    drawPath(silhouette, color)

    val shadedFace = Path().apply {
        moveTo(left, floorY)
        lineTo(left + width * 0.04f, floorY - height * 0.24f)
        cubicTo(
            left + width * 0.18f,
            floorY - height * 0.14f,
            right - width * 0.18f,
            floorY - height * 0.18f,
            right - width * 0.02f,
            floorY - height * 0.20f
        )
        lineTo(right, floorY)
        close()
    }
    drawPath(shadedFace, Color.Black.copy(alpha = 0.12f))

    val highlight = Path().apply {
        moveTo(left + width * 0.18f + lean * 0.25f, floorY - height * 0.62f)
        cubicTo(
            left + width * 0.27f + lean * 0.45f,
            floorY - height * 0.86f,
            centerX + lean * 0.82f,
            floorY - height * 0.92f,
            right - width * 0.25f + lean * 0.30f,
            floorY - height * 0.76f
        )
        cubicTo(
            centerX + lean * 0.45f,
            floorY - height * 0.78f,
            left + width * 0.30f + lean * 0.20f,
            floorY - height * 0.72f,
            left + width * 0.18f + lean * 0.25f,
            floorY - height * 0.62f
        )
        close()
    }
    drawPath(highlight, Color.White.copy(alpha = 0.055f + rock.light * 0.05f))
}

private fun Color.mixWith(other: Color, amount: Float): Color {
    val ratio = amount.coerceIn(0f, 1f)
    return Color(
        red = red + (other.red - red) * ratio,
        green = green + (other.green - green) * ratio,
        blue = blue + (other.blue - blue) * ratio,
        alpha = alpha
    )
}

private data class RockShape(
    val centerX: Float,
    val floorY: Float,
    val width: Float,
    val height: Float,
    val lean: Float,
    val light: Float
)
