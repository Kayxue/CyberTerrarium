package terrarium

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

@Composable
fun WaterCanvas() {
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val width = size.width
        val height = size.height

        // Water background
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF81D4FA),
                    Color(0xFF0288D1)
                )
            ),
            size = size
        )

        // First wave
        val wave1 = Path().apply {
            moveTo(0f, height * 0.35f)

            cubicTo(
                width * 0.25f, height * 0.25f,
                width * 0.25f, height * 0.45f,
                width * 0.5f, height * 0.35f
            )

            cubicTo(
                width * 0.75f, height * 0.25f,
                width * 0.75f, height * 0.45f,
                width, height * 0.35f
            )

            lineTo(width, height)
            lineTo(0f, height)
            close()
        }

        drawPath(
            path = wave1,
            color = Color(0x6633B5E5)
        )

        // Second wave
        val wave2 = Path().apply {
            moveTo(0f, height * 0.5f)

            cubicTo(
                width * 0.2f, height * 0.4f,
                width * 0.3f, height * 0.6f,
                width * 0.5f, height * 0.5f
            )

            cubicTo(
                width * 0.7f, height * 0.4f,
                width * 0.8f, height * 0.6f,
                width, height * 0.5f
            )

            lineTo(width, height)
            lineTo(0f, height)
            close()
        }

        drawPath(
            path = wave2,
            color = Color(0x5529B6F6)
        )

        // Bubbles
        drawCircle(
            color = Color.White.copy(alpha = 0.45f),
            radius = 8f,
            center = Offset(width * 0.2f, height * 0.75f)
        )

        drawCircle(
            color = Color.White.copy(alpha = 0.35f),
            radius = 5f,
            center = Offset(width * 0.3f, height * 0.55f)
        )

        drawCircle(
            color = Color.White.copy(alpha = 0.4f),
            radius = 10f,
            center = Offset(width * 0.75f, height * 0.65f)
        )

        drawCircle(
            color = Color.White.copy(alpha = 0.3f),
            radius = 6f,
            center = Offset(width * 0.85f, height * 0.35f)
        )
    }
}