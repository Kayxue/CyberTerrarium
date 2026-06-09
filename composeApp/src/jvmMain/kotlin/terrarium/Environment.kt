package terrarium

import androidx.compose.ui.graphics.Color
import terrarium.model.TerrariumEnvironmentState

internal enum class EnvironmentBand {
    THRIVING,
    STABLE,
    STRESSED,
    CRITICAL
}

internal data class EnvironmentStyle(
    val band: EnvironmentBand,
    val waterTop: Color,
    val waterBottom: Color,
    val haze: Color,
    val substrate: Color,
    val rock: Color,
    val plant: Color,
    val current: Color,
    val coral: List<Color>
)

internal fun environmentStyle(environment: TerrariumEnvironmentState): EnvironmentStyle {
    val band = when {
        environment.health >= 82 &&
            environment.stress < 35 &&
            environment.clarity >= 75 &&
            environment.toxicity < 20 -> EnvironmentBand.THRIVING
        environment.health >= 62 &&
            environment.stress < 65 &&
            environment.clarity >= 50 &&
            environment.toxicity < 42 -> EnvironmentBand.STABLE
        environment.health >= 35 && environment.toxicity < 70 -> EnvironmentBand.STRESSED
        else -> EnvironmentBand.CRITICAL
    }
    val tint = environment.tint
    return when (band) {
        EnvironmentBand.THRIVING -> EnvironmentStyle(
            band,
            tint.topColorRgb.toColor(),
            tint.bottomColorRgb.toColor(),
            Color(0xFFB2EBF2),
            Color(0xFF496B50),
            Color(0xFF52656B),
            Color(0xFF3FAE68),
            Color(0xFFB3E5FC),
            listOf(Color(0xFFFF6F91), Color(0xFFFFC75F), Color(0xFF9BDEAC), Color(0xFF7E8CE0))
        )
        EnvironmentBand.STABLE -> EnvironmentStyle(
            band,
            tint.topColorRgb.toColor(),
            tint.bottomColorRgb.toColor(),
            Color(0xFFCFD8DC),
            Color(0xFF5D6D52),
            Color(0xFF5D6668),
            Color(0xFF4C956C),
            Color(0xFFB0BEC5),
            listOf(Color(0xFFD9898C), Color(0xFFD5AE66), Color(0xFF76A887))
        )
        EnvironmentBand.STRESSED -> EnvironmentStyle(
            band,
            tint.topColorRgb.toColor(),
            tint.bottomColorRgb.toColor(),
            Color(0xFFC9C18A),
            Color(0xFF696649),
            Color(0xFF66645B),
            Color(0xFF758269),
            Color(0xFFB7B69A),
            listOf(Color(0xFFB39B82), Color(0xFF9D8B79), Color(0xFFA7A17C))
        )
        EnvironmentBand.CRITICAL -> EnvironmentStyle(
            band,
            tint.topColorRgb.toColor(),
            tint.bottomColorRgb.toColor(),
            Color(0xFF9C9872),
            Color(0xFF555344),
            Color(0xFF555852),
            Color(0xFF67705F),
            Color(0xFF8C907D),
            listOf(Color(0xFF77756E), Color(0xFF6B6D68))
        )
    }
}

private fun Int.toColor(): Color = Color(0xFF000000L or (toLong() and 0xFFFFFFL))
