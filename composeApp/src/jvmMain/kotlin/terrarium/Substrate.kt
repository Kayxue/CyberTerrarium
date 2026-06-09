package terrarium

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

internal fun DrawScope.drawTerrariumSubstrate(style: EnvironmentStyle) {
    val substrate = Path().apply {
        moveTo(0f, size.height * 0.84f)
        cubicTo(
            size.width * 0.22f, size.height * 0.80f,
            size.width * 0.38f, size.height * 0.90f,
            size.width * 0.58f, size.height * 0.85f
        )
        cubicTo(
            size.width * 0.74f, size.height * 0.81f,
            size.width * 0.88f, size.height * 0.89f,
            size.width, size.height * 0.83f
        )
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(substrate, style.substrate)
    drawPath(
        substrate,
        Color.White.copy(alpha = 0.08f),
        style = Stroke(width = 2f)
    )
}
