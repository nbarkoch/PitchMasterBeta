package com.example.pitchmasterbeta.ui.workspace

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import com.example.pitchmasterbeta.MainActivity.Companion.viewModelProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun WorkspaceHeader(
    modifier: Modifier = Modifier,
) {
    val viewModel: WorkspaceViewModel =
        viewModelProvider[WorkspaceViewModel::class.java]
    val workspaceState = viewModel.workspaceState.collectAsState()
    val playState = viewModel.playingState.collectAsState()
    val songFullName = viewModel.songFullName.collectAsState()

    val transition = updateTransition(
        targetState = playState.value == WorkspaceViewModel.PlayerState.END,
        label = ""
    )
    val alpha by transition.animateFloat(
        transitionSpec = { tween(1000) },
        label = "",
        targetValueByState = {
            if (it) 0.75f else 0.0f
        }
    )

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF132152),
            Color(0x750E0D3A),
            Color.Transparent
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha)
            .background(color = Color.Black)
    )
    Column(
        modifier = modifier
            .background(brush = gradientBrush)
            .defaultMinSize(minHeight = 200.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MarqueeText(songFullName.value)
        if (workspaceState.value == WorkspaceViewModel.WorkspaceState.IDLE) {
            ScoreComposable(viewModel)
        }
    }

}

@Composable
fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    scrollSpeed: Int = 250, // Adjust the speed as needed
) {
    val textState = rememberScrollState()


    suspend fun animateScrollTo(value: Int) {
        withContext(Dispatchers.Main) {
            textState.animateScrollTo(
                value = value,
                animationSpec = tween(
                    durationMillis = (scrollSpeed * text.length),
                    easing = LinearEasing
                )
            )
        }
    }

    LaunchedEffect(text) {
        launch {
            while (true) {
                animateScrollTo(textState.maxValue)
                delay(scrollSpeed * text.length.toLong())
                animateScrollTo(0)
                delay(scrollSpeed * text.length.toLong())
            }
        }
    }

    Box(
        modifier = modifier
            .horizontalScroll(state = textState)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.W400,
            modifier = Modifier.padding(horizontal = 40.dp)
        )
    }
}

@Composable
fun ScoreComposable(viewModel: WorkspaceViewModel) {
    val offsetY =
        (with(LocalDensity.current) { LocalConfiguration.current.screenHeightDp.dp.toPx() / 2f - 100.dp.toPx() * 1f })

    val colorState = viewModel.similarityColor.collectAsState()
    val color =
        animateColorAsState(targetValue = colorState.value, label = "", animationSpec = tween(1000))

    val micNoteActive = viewModel.micNoteActive.collectAsState()
    val sinNoteActive = viewModel.sinNoteActive.collectAsState()
    val animatedSmallScoreScale by animateFloatAsState(
        if (micNoteActive.value && sinNoteActive.value) 0.05f else 0f,
        label = ""
    )

    val score by rememberUpdatedState(viewModel.score.collectAsState())
    val playState = viewModel.playingState.collectAsState()

    var grade by remember { mutableStateOf(0) }

    var opinion by remember { mutableStateOf("") }
    LaunchedEffect(playState.value) {
        if (playState.value == WorkspaceViewModel.PlayerState.END) {
            opinion = viewModel.giveOpinionForScore(score.value)
            val expectedScore = viewModel.getExpectedScore()
            if (expectedScore > 0) {
                grade = ((score.value * 100f) / viewModel.getExpectedScore()).toInt()
            }
        } else {
            opinion = ""
            grade = 0
        }
    }

    val transition = updateTransition(
        targetState = playState.value == WorkspaceViewModel.PlayerState.END,
        label = "",
    )
    val translateY by transition.animateFloat(
        transitionSpec = { tween(1000) },
        label = "",
        targetValueByState = {
            if (it) offsetY else 0f
        }
    )
    val scale by transition.animateFloat(
        transitionSpec = { tween(1000) },
        label = "",
        targetValueByState = {
            if (it) 1.6f else 1f
        }
    )

    Box(
        modifier = Modifier
            .padding(20.dp)
            .graphicsLayer {
                translationY = translateY
                scaleY = scale
                scaleX = scale
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .scale(animatedSmallScoreScale + 1f)
                    .border(
                        color = color.value,
                        shape = CircleShape,
                        width = (animatedSmallScoreScale * 4 + 1) * 3.dp
                    )
                    .padding(7.5.dp)
                    .widthIn(50.dp, 300.dp)
                    .heightIn(50.dp, 300.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${score.value}",
                    color = Color.White,
                    fontSize = 30.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.W500
                )
            }

            AnimatedVisibility(
                visible = playState.value == WorkspaceViewModel.PlayerState.END,
                enter = fadeIn(
                    animationSpec = tween(durationMillis = 200, delayMillis = 1000)
                ),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "($grade%) $opinion", color = Color.White,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.W400,
                        modifier = Modifier.padding(10.dp)
                    )
                    Button(colors = ButtonDefaults.buttonColors(Color.White),
                        modifier = Modifier.defaultMinSize(
                            minWidth = ButtonDefaults.MinWidth, minHeight = 10.dp
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        onClick = {
                            viewModel.resetScoreAndPlayingState()

                        }) {
                        Text(
                            text = "Cancel", color = Color.Black,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun WorkspaceHeaderPreview() {
    MaterialTheme {
        WorkspaceHeader(modifier = Modifier.fillMaxWidth())
    }
}