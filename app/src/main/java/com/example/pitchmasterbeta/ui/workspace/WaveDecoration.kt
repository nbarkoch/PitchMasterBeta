package com.example.pitchmasterbeta.ui.workspace

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pitchmasterbeta.MainActivity
import com.example.pitchmasterbeta.MainActivity.Companion.isPreview
import com.example.pitchmasterbeta.model.AudioProcessor
import kotlinx.coroutines.delay


val colorOptionsBest = listOf(
    Color(0xFF00D1FE),
    Color(0xFF8A2BE2),
    Color(0xFFF70AE8),
    Color(0xFF1158FF),
)

val colorOptionsBad = listOf(
    Color(0xFFE90404), Color(0xFFFF11A0), Color(0xFFFF5411)
)

val colorOptionsIdle = listOf(
    Color(0x00DB11FF)
)

val colorOptionsNatural = listOf(
    Color(0x61DF2BFF)
)

val colorOptionsClose = listOf(
    Color(0xFF6100FE), Color(0xFFDB11FF)
)

fun colorOptions(similarity: AudioProcessor.NotesSimilarity): List<Color> {
    return when (similarity) {
        AudioProcessor.NotesSimilarity.Wrong -> colorOptionsBad
        AudioProcessor.NotesSimilarity.Neutral -> colorOptionsNatural
        AudioProcessor.NotesSimilarity.Idle -> colorOptionsIdle
        AudioProcessor.NotesSimilarity.Close -> colorOptionsClose
        AudioProcessor.NotesSimilarity.Equal -> colorOptionsBest
    }
}

@Composable
fun AnimatedColorBorder(
    modifier: Modifier = Modifier,
    similarity: AudioProcessor.NotesSimilarity,
    volume: Float,
    leftAudioData: List<Float>,
    rightAudioData: List<Float>
) {
    val colorCountInGradient = 4

    var colorUpdateTrigger by remember { mutableIntStateOf(0) }

    val colorOptions = remember { mutableStateOf(colorOptions(similarity)) }

    LaunchedEffect(similarity) {
        colorOptions.value = colorOptions(similarity)
    }

    LaunchedEffect(colorUpdateTrigger) {
        colorOptions.value = colorOptions.value.shuffled()
    }

    // Create animated colors
    val animatedColors = List(colorCountInGradient) { index ->
        val color = remember { mutableStateOf(colorOptions.value[index % colorOptions.value.size]) }
        val animatedColor by animateColorAsState(
            targetValue = color.value,
            animationSpec = tween(500),
            label = "color_$index"
        )
        LaunchedEffect(colorUpdateTrigger) {
            color.value = colorOptions.value[index % colorOptions.value.size]
        }
        animatedColor
    }

    // Trigger color update every 500ms
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            colorUpdateTrigger++
        }
    }

    // Use the animatedColors in your BorderAudioVisualizer
    BorderAudioVisualizer(
        modifier = modifier,
        colors = animatedColors,
        volumeScale = volume,
        leftAudioData,
        rightAudioData
    )
}

@Composable
fun BorderAudioVisualizer(
    modifier: Modifier = Modifier,
    colors: List<Color>,
    volumeScale: Float,
    leftAudioData: List<Float>,
    rightAudioData: List<Float>
) {

    val gradientOffset = remember { Animatable(0f) }
    // Launch an animation that repeats infinitely
    LaunchedEffect(Unit) {
        gradientOffset.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    // Compute the animated gradient brush
    val gradientBrush = remember(gradientOffset.value) {
        Brush.linearGradient(
            colors = colors,
            start = Offset(1000f * gradientOffset.value, 1000f * gradientOffset.value),
            end = Offset(
                1000f + 1000f * gradientOffset.value,
                1000f + 1000f * gradientOffset.value
            ),
            tileMode = TileMode.Mirror
        )
    }


    Canvas(
        modifier = modifier
            .fillMaxSize()
            .padding(2.dp)
            .blur(20.dp)
    ) {
        val width = size.width
        val height = size.height
        val borderWidth = 5.dp.toPx() * (1f + volumeScale)
        val cornerRadius = 40.dp.toPx()

        drawRoundRect(
            brush = gradientBrush,
            size = Size(width, height),
            cornerRadius = CornerRadius(cornerRadius),
            style = Stroke(width = borderWidth)
        )
    }
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .padding(2.dp)
    ) {
        val width = size.width
        val height = size.height
        val borderWidth = 5.dp.toPx()
        val cornerRadius = 40.dp.toPx()

        drawRoundRect(
            brush = gradientBrush,
            size = Size(width, height),
            cornerRadius = CornerRadius(cornerRadius),
            style = Stroke(width = borderWidth)
        )


        // Draw left edge wave
        drawWave(leftAudioData, true, gradientBrush, borderWidth)

        // Draw right edge wave
        drawWave(rightAudioData, false, gradientBrush, borderWidth)
    }
}


fun DrawScope.drawWave(
    audioData: List<Float>,
    isLeftEdge: Boolean,
    brush: Brush,
    strokeWidth: Float
) {
    val path = Path()
    val height = size.height
    val width = strokeWidth * 3 // Width of the wave area

    val startX = if (isLeftEdge) 0f else size.width - width
    path.moveTo(if (isLeftEdge) startX else startX + width, height)

    val segmentHeight = height / (audioData.size - 1)
    audioData.forEachIndexed { index, amplitude ->
        val x = if (isLeftEdge) {
            startX + width * amplitude
        } else {
            startX + width * (1 - amplitude)
        }
        val y = height - index * segmentHeight
        path.lineTo(x, y)
    }

    path.lineTo(if (isLeftEdge) startX else startX + width, 0f)
    path.close()

    drawPath(path, brush)
}

@Composable
fun AudioVisualizerScreen(isVisible: Boolean = false, similarity: AudioProcessor.NotesSimilarity) {
    val viewModel: WorkspaceViewModel = MainActivity.getWorkspaceViewModel()
    val sinNote by rememberUpdatedState(viewModel.sinNote.collectAsState())
    val micNote by rememberUpdatedState(viewModel.micNote.collectAsState())

    val audioData = viewModel.audioData.collectAsState()


    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(1000)),
        exit = fadeOut(animationSpec = tween(1000))
    ) {
        AnimatedColorBorder(
            Modifier,
            similarity,
            volume = sinNote.value.volume + micNote.value.volume,
            audioData.value.first,
            audioData.value.second
        )
    }
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp)
    ) {
        val width = size.width
        val height = size.height
        // main rounded rectangle border
        drawRoundRect(
            color = Color.Black,
            size = Size(width + 100f, height + 100f),
            topLeft = Offset(-100f * 0.5f, -100f * 0.5f),
            cornerRadius = CornerRadius(57.dp.toPx()),
            style = Stroke(width = 100f),
        )
    }
}


@Preview(showBackground = true)
@Composable
fun AudioVisualizerScreenPreview() {
    isPreview = true
    AudioVisualizerScreen(true, similarity = AudioProcessor.NotesSimilarity.Equal)
}