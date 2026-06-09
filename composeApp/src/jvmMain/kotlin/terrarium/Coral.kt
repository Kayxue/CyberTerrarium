package terrarium

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import terrarium.model.TerrariumEnvironmentState

internal fun DrawScope.drawTerrariumCoral(
    environment: TerrariumEnvironmentState,
    style: EnvironmentStyle
) {
    val colonySeeds = listOf(0x2D31L, 0x71A9L, 0xB43FL, 0xE217L, 0x1259DL, 0x18C47L)
    val visibleColonyCount = 1.5f + environment.health / 100f * 4.5f
    val vitality = (environment.health / 100f).coerceIn(0f, 1f)

    colonySeeds.forEachIndexed { index, seed ->
        val alpha = (visibleColonyCount - index).coerceIn(0f, 1f)
        if (alpha <= 0f) {
            return@forEachIndexed
        }

        val base = Offset(
            x = size.width * (0.23f + coralRatio(seed, 3) * 0.56f),
            y = size.height * (0.875f + coralRatio(seed, 7) * 0.012f)
        )
        val height = size.height * (0.072f + coralRatio(seed, 11) * 0.070f)
        val spread = height * (0.30f + coralRatio(seed, 17) * 0.30f)
        val color = style.coral[index % style.coral.size].copy(alpha = alpha)
        drawCoralColony(
            base = base,
            height = height,
            spread = spread,
            seed = seed,
            color = color,
            vitality = vitality
        )
    }
}

private fun DrawScope.drawCoralColony(
    base: Offset,
    height: Float,
    spread: Float,
    seed: Long,
    color: Color,
    vitality: Float
) {
    val trunkLean = (coralRatio(seed, 23) - 0.5f) * spread * 0.32f
    val trunkTop = Offset(base.x + trunkLean, base.y - height)
    val trunk = Path().apply {
        moveTo(base.x, base.y)
        cubicTo(
            base.x - trunkLean * 0.35f,
            base.y - height * 0.36f,
            trunkTop.x + trunkLean * 0.22f,
            base.y - height * 0.70f,
            trunkTop.x,
            trunkTop.y
        )
    }
    drawCoralBranch(trunk, color, 4.8f)

    val branchCount = 4 + (coralRatio(seed, 29) * 3f).toInt()
    repeat(branchCount) { branchIndex ->
        val branchSeed = seed + branchIndex * 0x9E37L
        val startRatio = 0.23f + branchIndex / (branchCount + 1f) * 0.59f
        val start = Offset(
            x = base.x + trunkLean * startRatio,
            y = base.y - height * startRatio
        )
        val side = when {
            branchIndex == 0 -> -1f
            branchIndex == 1 -> 1f
            coralRatio(branchSeed, 31) < 0.5f -> -1f
            else -> 1f
        }
        val branchLength = height * (0.25f + coralRatio(branchSeed, 37) * 0.24f)
        val branchReach = spread * (0.48f + coralRatio(branchSeed, 41) * 0.52f)
        val end = Offset(
            x = start.x + side * branchReach,
            y = start.y - branchLength
        )
        val bend = side * spread * (coralRatio(branchSeed, 43) - 0.35f) * 0.34f
        val branch = Path().apply {
            moveTo(start.x, start.y)
            cubicTo(
                start.x + side * branchReach * 0.24f,
                start.y - branchLength * 0.18f,
                end.x - side * branchReach * 0.18f + bend,
                end.y + branchLength * 0.28f,
                end.x,
                end.y
            )
        }
        drawCoralBranch(branch, color, 3.7f)

        val twigCount = 1 + (coralRatio(branchSeed, 47) * 2f).toInt()
        repeat(twigCount) { twigIndex ->
            val twigSeed = branchSeed + twigIndex * 0x51EDL
            val twigStartRatio = 0.48f + twigIndex * 0.20f
            val twigStart = Offset(
                x = start.x + (end.x - start.x) * twigStartRatio,
                y = start.y + (end.y - start.y) * twigStartRatio
            )
            val twigSide = if (twigIndex % 2 == 0) -side else side
            val twigLength = height * (0.11f + coralRatio(twigSeed, 53) * 0.10f)
            val twigEnd = Offset(
                x = twigStart.x + twigSide * spread * (0.16f + coralRatio(twigSeed, 59) * 0.18f),
                y = twigStart.y - twigLength
            )
            val twig = Path().apply {
                moveTo(twigStart.x, twigStart.y)
                quadraticTo(
                    (twigStart.x + twigEnd.x) / 2f + twigSide * spread * 0.06f,
                    twigStart.y - twigLength * 0.35f,
                    twigEnd.x,
                    twigEnd.y
                )
            }
            drawCoralBranch(twig, color, 2.5f)
            drawCoralTip(twigEnd, color, vitality, 2.5f)
        }

        drawCoralTip(end, color, vitality, 3.2f)
    }

    drawCoralTip(trunkTop, color, vitality, 3.8f)
}

private fun DrawScope.drawCoralBranch(path: Path, color: Color, width: Float) {
    drawPath(
        path = path,
        color = Color.Black.copy(alpha = color.alpha * 0.14f),
        style = Stroke(width = width + 1.8f, cap = StrokeCap.Round)
    )
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = width, cap = StrokeCap.Round)
    )
    drawPath(
        path = path,
        color = Color.White.copy(alpha = color.alpha * 0.10f),
        style = Stroke(width = (width * 0.27f).coerceAtLeast(0.8f), cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawCoralTip(
    center: Offset,
    color: Color,
    vitality: Float,
    radius: Float
) {
    drawCircle(
        color = color,
        radius = radius,
        center = center
    )
    drawCircle(
        color = Color.White.copy(alpha = color.alpha * (0.08f + vitality * 0.18f)),
        radius = radius * 0.42f,
        center = center - Offset(radius * 0.22f, radius * 0.24f)
    )
}

private fun coralRatio(seed: Long, salt: Int): Float {
    var value = seed xor (salt.toLong() * -7046029254386353131L)
    value = (value xor (value ushr 30)) * -4658895280553007687L
    value = (value xor (value ushr 27)) * -7723592293110705685L
    value = value xor (value ushr 31)
    return ((value ushr 40) and 0xFFFFFF).toFloat() / 0xFFFFFF
}
