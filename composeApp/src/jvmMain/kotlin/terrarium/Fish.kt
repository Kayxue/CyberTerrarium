package terrarium

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp

@Composable
fun FishCanvas() {
    Canvas(
        modifier = Modifier.size(240.dp, 140.dp)
    ) {
        val width = size.width
        val height = size.height

        // Body
        drawOval(
            color = Color(0xFF4FC3F7),
            topLeft = Offset(width * 0.25f, height * 0.25f),
            size = Size(width * 0.5f, height * 0.5f)
        )

        // Tail
        val tail = Path().apply {
            moveTo(width * 0.25f, height * 0.5f)
            lineTo(width * 0.05f, height * 0.25f)
            lineTo(width * 0.05f, height * 0.75f)
            close()
        }

        drawPath(
            path = tail,
            color = Color(0xFF0288D1)
        )

        // Top fin
        val topFin = Path().apply {
            moveTo(width * 0.45f, height * 0.28f)
            lineTo(width * 0.55f, height * 0.05f)
            lineTo(width * 0.65f, height * 0.30f)
            close()
        }

        drawPath(
            path = topFin,
            color = Color(0xFF03A9F4)
        )

        // Bottom fin
        val bottomFin = Path().apply {
            moveTo(width * 0.45f, height * 0.72f)
            lineTo(width * 0.55f, height * 0.95f)
            lineTo(width * 0.65f, height * 0.70f)
            close()
        }

        drawPath(
            path = bottomFin,
            color = Color(0xFF03A9F4)
        )

        // Eye
        drawCircle(
            color = Color.White,
            radius = width * 0.035f,
            center = Offset(width * 0.65f, height * 0.42f)
        )

        drawCircle(
            color = Color.Black,
            radius = width * 0.015f,
            center = Offset(width * 0.66f, height * 0.42f)
        )

        // Mouth
        drawArc(
            color = Color.Black,
            startAngle = 20f,
            sweepAngle = 120f,
            useCenter = false,
            topLeft = Offset(width * 0.68f, height * 0.48f),
            size = Size(width * 0.08f, height * 0.08f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
        )
    }
}