package terrarium

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import terrarium.model.TerrariumCreatureKind
import terrarium.model.TerrariumEnvironmentState
import terrarium.model.TerrariumFishState
import terrarium.model.TerrariumSnapshot
import terrarium.model.TerrariumWaterTint
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

private data class FishPlacement(
    val fish: TerrariumFishState,
    val center: Offset,
    val width: Float,
    val facingScale: Float,
    val opacity: Float
)

private class FishRenderEntry(initialFish: TerrariumFishState) {
    val id: String = initialFish.id
    val positionSeed: Long = initialFish.visualHint.positionSeed
    val horizontalProgress = Animatable(seededRatio(positionSeed, 91))
    val verticalPhaseOffset: Float = seededRatio(positionSeed, 109) * 2f * PI.toFloat()
    val facingScale = Animatable(
        if (seededRatio(positionSeed, 127) < 0.5f) 1f else -1f
    )
    var facingRight by mutableStateOf(facingScale.value > 0f)
    val opacity = Animatable(0f)
    var fish by mutableStateOf(initialFish)
    var isTargetVisible by mutableStateOf(true)
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
fun TerrariumScene(
    snapshot: TerrariumSnapshot,
    modifier: Modifier = Modifier,
    maxVisibleFish: Int = 100,
    onFishClick: (TerrariumFishState) -> Unit = {}
) {
    val visibleFish = remember(snapshot.fish, maxVisibleFish) {
        if (maxVisibleFish <= 0) {
            emptyList()
        } else {
            snapshot.fish.sortedWith(
                compareByDescending<TerrariumFishState> { it.visualHint.importance }
                    .thenByDescending { if (it.kind == TerrariumCreatureKind.JOB) 1 else 0 }
                    .thenByDescending { it.risk }
                    .thenByDescending { it.activity }
                    .thenBy { it.id }
            ).take(maxVisibleFish)
        }
    }
    val renderedFish = remember { mutableStateMapOf<String, FishRenderEntry>() }
    var lifecycleRevision by remember { mutableIntStateOf(0) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var hoveredFish by remember { mutableStateOf<TerrariumFishState?>(null) }
    var hoverPosition by remember { mutableStateOf(Offset.Zero) }

    val environmentAnimation = tween<Float>(
        durationMillis = 2_000,
        easing = FastOutSlowInEasing
    )
    val environmentColorAnimation = tween<Color>(
        durationMillis = 2_000,
        easing = FastOutSlowInEasing
    )
    val animatedHealth by animateFloatAsState(
        snapshot.environment.health.toFloat(),
        environmentAnimation,
        label = "environment-health"
    )
    val animatedStress by animateFloatAsState(
        snapshot.environment.stress.toFloat(),
        environmentAnimation,
        label = "environment-stress"
    )
    val animatedClarity by animateFloatAsState(
        snapshot.environment.clarity.toFloat(),
        environmentAnimation,
        label = "environment-clarity"
    )
    val animatedTemperatureStress by animateFloatAsState(
        snapshot.environment.temperatureStress.toFloat(),
        environmentAnimation,
        label = "environment-temperature"
    )
    val animatedMotion by animateFloatAsState(
        snapshot.environment.motion.toFloat(),
        environmentAnimation,
        label = "environment-motion"
    )
    val animatedToxicity by animateFloatAsState(
        snapshot.environment.toxicity.toFloat(),
        environmentAnimation,
        label = "environment-toxicity"
    )
    val animatedWaveIntensity by animateFloatAsState(
        snapshot.environment.waveIntensity.toFloat(),
        environmentAnimation,
        label = "environment-wave"
    )
    val animatedBubbleIntensity by animateFloatAsState(
        snapshot.environment.bubbleIntensity.toFloat(),
        environmentAnimation,
        label = "environment-bubbles"
    )
    val animatedTopColor by animateColorAsState(
        Color(0xFF000000L or (snapshot.environment.tint.topColorRgb.toLong() and 0xFFFFFFL)),
        environmentColorAnimation,
        label = "environment-top-color"
    )
    val animatedBottomColor by animateColorAsState(
        Color(0xFF000000L or (snapshot.environment.tint.bottomColorRgb.toLong() and 0xFFFFFFL)),
        environmentColorAnimation,
        label = "environment-bottom-color"
    )
    val animatedHazeColor by animateColorAsState(
        Color(0xFF000000L or (snapshot.environment.tint.hazeColorRgb.toLong() and 0xFFFFFFL)),
        environmentColorAnimation,
        label = "environment-haze-color"
    )
    val animatedEnvironment = TerrariumEnvironmentState(
        animatedHealth.roundToInt(),
        animatedStress.roundToInt(),
        animatedClarity.roundToInt(),
        animatedTemperatureStress.roundToInt(),
        animatedMotion.roundToInt(),
        animatedToxicity.roundToInt(),
        animatedWaveIntensity.toDouble(),
        animatedBubbleIntensity.toDouble(),
        TerrariumWaterTint(
            animatedTopColor.toArgb(),
            animatedBottomColor.toArgb(),
            animatedHazeColor.toArgb()
        )
    )
    val targetStyle = environmentStyle(snapshot.environment)
    val animatedStyleHaze by animateColorAsState(
        targetStyle.haze,
        environmentColorAnimation,
        label = "environment-style-haze"
    )
    val animatedSubstrate by animateColorAsState(
        targetStyle.substrate,
        environmentColorAnimation,
        label = "environment-substrate"
    )
    val animatedRock by animateColorAsState(
        targetStyle.rock,
        environmentColorAnimation,
        label = "environment-rock"
    )
    val animatedPlant by animateColorAsState(
        targetStyle.plant,
        environmentColorAnimation,
        label = "environment-plant"
    )
    val animatedCurrent by animateColorAsState(
        targetStyle.current,
        environmentColorAnimation,
        label = "environment-current"
    )
    val animatedCoral0 by animateColorAsState(
        targetStyle.coral.getOrElse(0) { targetStyle.coral.last() },
        environmentColorAnimation,
        label = "environment-coral-0"
    )
    val animatedCoral1 by animateColorAsState(
        targetStyle.coral.getOrElse(1) { targetStyle.coral.last() },
        environmentColorAnimation,
        label = "environment-coral-1"
    )
    val animatedCoral2 by animateColorAsState(
        targetStyle.coral.getOrElse(2) { targetStyle.coral.last() },
        environmentColorAnimation,
        label = "environment-coral-2"
    )
    val animatedCoral3 by animateColorAsState(
        targetStyle.coral.getOrElse(3) { targetStyle.coral.last() },
        environmentColorAnimation,
        label = "environment-coral-3"
    )

    LaunchedEffect(visibleFish, maxVisibleFish, lifecycleRevision) {
        val targetById = visibleFish.associateBy { it.id }
        val lifecycleAnimation = tween<Float>(
            durationMillis = 2_000,
            easing = FastOutSlowInEasing
        )

        val exitingEntries = renderedFish.values.filter { it.id !in targetById }
        renderedFish.values.toList().forEach { entry ->
            val updatedFish = targetById[entry.id]
            if (updatedFish != null) {
                entry.fish = updatedFish
                entry.isTargetVisible = true
                launch {
                    entry.opacity.animateTo(1f, lifecycleAnimation)
                }
            } else {
                entry.isTargetVisible = false
            }
        }

        val availableSlots = (maxVisibleFish - renderedFish.size).coerceAtLeast(0)
        visibleFish.asSequence()
            .filterNot { renderedFish.containsKey(it.id) }
            .take(availableSlots)
            .forEach { fish ->
                val entry = FishRenderEntry(fish)
                renderedFish[fish.id] = entry
                launch {
                    entry.opacity.animateTo(1f, lifecycleAnimation)
                }
            }

        if (exitingEntries.isNotEmpty()) {
            exitingEntries.map { entry ->
                launch {
                    entry.opacity.animateTo(0f, lifecycleAnimation)
                }
            }.joinAll()
            exitingEntries.forEach { entry ->
                if (!entry.isTargetVisible && renderedFish[entry.id] === entry) {
                    renderedFish.remove(entry.id)
                }
            }
            lifecycleRevision += 1
        }
    }

    val transition = rememberInfiniteTransition(label = "terrarium-scene")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(44_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "terrarium-phase"
    )

    val fishEntries = renderedFish.values.toList()
    fishEntries.forEach { entry ->
        key(entry.id) {
            LaunchedEffect(entry.id) {
                while (true) {
                    val target = if (entry.facingRight) 1f else 0f
                    val remainingDistance = kotlin.math.abs(target - entry.horizontalProgress.value)
                    entry.horizontalProgress.animateTo(
                        targetValue = target,
                        animationSpec = tween(
                            durationMillis = (20_700 * remainingDistance)
                                .roundToInt()
                                .coerceAtLeast(1),
                            easing = LinearEasing
                        )
                    )
                    delay(700)
                    entry.facingRight = !entry.facingRight
                    entry.facingScale.animateTo(
                        targetValue = if (entry.facingRight) 1f else -1f,
                        animationSpec = tween(
                            durationMillis = 600,
                            easing = FastOutSlowInEasing
                        )
                    )
                }
            }
        }
    }

    val placements = remember(fishEntries, canvasSize, phase) {
        if (canvasSize.width <= 0 || canvasSize.height <= 0) {
            emptyList()
        } else {
            val baseWidth = (minOf(canvasSize.width, canvasSize.height) * 0.052f)
                .coerceIn(24f, 58f)
            fishEntries.map { entry ->
                val item = entry.fish
                val seed = entry.positionSeed
                val width = baseWidth *
                    item.visualHint.sizeWeight.toFloat().coerceIn(0.65f, 1.55f)
                val horizontalRoom = (canvasSize.width / 2f - width * 0.65f).coerceAtLeast(0f)
                val horizontalSpan = horizontalRoom * (0.75f + seededRatio(seed, 71) * 0.20f)
                val leftX = canvasSize.width / 2f - horizontalSpan
                val rightX = canvasSize.width / 2f + horizontalSpan
                val minY = maxOf(width * 0.5f, canvasSize.height * 0.10f)
                val maxY = canvasSize.height * 0.76f
                val verticalRoom = (maxY - minY).coerceAtLeast(0f)
                val verticalCenter = minY + verticalRoom *
                    (0.42f + seededRatio(seed, 53) * 0.16f)
                val verticalSpan = verticalRoom * (0.22f + seededRatio(seed, 83) * 0.20f)
                val verticalFrequency = if (seededRatio(seed, 97) < 0.5f) 1f else 2f
                val verticalAngle = phase * 2f * PI.toFloat() * verticalFrequency +
                    entry.verticalPhaseOffset
                val center = Offset(
                    leftX + (rightX - leftX) * entry.horizontalProgress.value,
                    (verticalCenter + sin(verticalAngle) * verticalSpan)
                        .coerceIn(minY, maxY)
                )
                FishPlacement(
                    fish = item,
                    center = center,
                    width = width,
                    facingScale = entry.facingScale.value,
                    opacity = entry.opacity.value
                )
            }
        }
    }
    val currentPlacements by rememberUpdatedState(placements)
    val currentOnFishClick by rememberUpdatedState(onFishClick)
    val style = EnvironmentStyle(
        band = environmentStyle(animatedEnvironment).band,
        waterTop = animatedTopColor,
        waterBottom = animatedBottomColor,
        haze = animatedStyleHaze,
        substrate = animatedSubstrate,
        rock = animatedRock,
        plant = animatedPlant,
        current = animatedCurrent,
        coral = listOf(animatedCoral0, animatedCoral1, animatedCoral2, animatedCoral3)
    )

    Box(
        modifier = modifier
            .background(style.waterBottom)
            .onSizeChanged { canvasSize = it }
            .onPointerEvent(PointerEventType.Move) { event ->
                event.changes.firstOrNull()?.position?.let { position ->
                    hoverPosition = position
                    hoveredFish = findFishAt(currentPlacements, position)
                }
            }
            .onPointerEvent(PointerEventType.Exit) {
                hoveredFish = null
            }
            .pointerInput(Unit) {
                detectTapGestures { position ->
                    findFishAt(currentPlacements, position)?.let(currentOnFishClick)
                }
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawTerrariumWater(animatedEnvironment, phase, style)
            drawTerrariumSubstrate(style)
            drawTerrariumRocks(style)
            drawTerrariumPlants(animatedEnvironment, phase, style)
            drawTerrariumCoral(animatedEnvironment, style)
            placements.forEach { placement ->
                drawTerrariumFish(
                    fish = placement.fish,
                    center = placement.center,
                    width = placement.width,
                    facingScale = placement.facingScale,
                    opacity = placement.opacity
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.22f), RoundedCornerShape(4.dp))
                .padding(horizontal = 9.dp, vertical = 6.dp)
        ) {
            Text(
                "${
                    when (style.band) {
                        EnvironmentBand.THRIVING -> "Thriving"
                        EnvironmentBand.STABLE -> "Stable"
                        EnvironmentBand.STRESSED -> "Stressed"
                        EnvironmentBand.CRITICAL -> "Critical"
                    }
                } | ${animatedEnvironment.health}%",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
            Text(
                "${visibleFish.size}/${snapshot.fish.size} fish",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.76f)
            )
        }

        hoveredFish?.let { fish ->
            Surface(
                modifier = Modifier.offset {
                    val maxX = (canvasSize.width - 280).coerceAtLeast(8)
                    val maxY = (canvasSize.height - 72).coerceAtLeast(8)
                    IntOffset(
                        (hoverPosition.x + 14f).roundToInt().coerceIn(8, maxX),
                        (hoverPosition.y + 14f).roundToInt().coerceIn(8, maxY)
                    )
                },
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                tonalElevation = 3.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)) {
                    Text(fish.label, style = MaterialTheme.typography.labelMedium)
                    Text(
                        "${fish.kind.name.lowercase()} | ${fish.status.name.lowercase()} | health ${fish.health}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun findFishAt(
    placements: List<FishPlacement>,
    position: Offset
): TerrariumFishState? {
    return placements.asReversed().firstOrNull { placement ->
        if (placement.opacity < 0.15f) {
            return@firstOrNull false
        }
        val dx = position.x - placement.center.x
        val dy = position.y - placement.center.y
        val radius = placement.width * 0.48f
        dx * dx + dy * dy <= radius * radius
    }?.fish
}

private fun seededRatio(seed: Long, salt: Int): Float {
    var value = seed xor (salt.toLong() * -7046029254386353131L)
    value = value xor (value ushr 33)
    value *= -49064778989728563L
    value = value xor (value ushr 33)
    return ((value ushr 11) and 0xFFFF).toFloat() / 65535f
}
