package com.example.pitchmasterbeta.ui.workspace

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pitchmasterbeta.MainActivity.Companion.getWorkspaceViewModel
import com.example.pitchmasterbeta.MainActivity.Companion.isPreview
import com.example.pitchmasterbeta.R
import com.example.pitchmasterbeta.ui.theme.PitchMasterBetaTheme
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun BackgroundMic() {
    val viewModel: WorkspaceViewModel = getWorkspaceViewModel()
    val workspaceState by rememberUpdatedState(viewModel.workspaceState.collectAsState())

    AnimatedVisibility(
        visible = workspaceState.value == WorkspaceViewModel.WorkspaceState.IDLE,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(durationMillis = 300)
        ),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(durationMillis = 300)
        ),
        modifier = Modifier
            .fillMaxSize()
            .alpha(0.15f)
            .offset(y = 70.dp)
    ) {
        Image(
            modifier = Modifier.fillMaxSize(),
            alignment = Alignment.BottomCenter,
            painter = painterResource(id = R.drawable.image_smic_bg),
            contentDescription = ""
        )
    }
    BubbleMaker()
}

@Composable
fun BubbleMaker() {
    val viewModel: WorkspaceViewModel = getWorkspaceViewModel()
    val playState by rememberUpdatedState(viewModel.playingState.collectAsState())
    val workspaceState by rememberUpdatedState(viewModel.workspaceState.collectAsState())

    repeat(10) {
        BubbleAnimation(playState.value == WorkspaceViewModel.PlayerState.PLAYING ||
                workspaceState.value == WorkspaceViewModel.WorkspaceState.WAITING)
    }
}

@Composable
fun BubbleAnimation(active: Boolean) {
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp.value
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp.value
    var bubbleSizeDp by remember { mutableStateOf(50.dp) }
    var bubbleOpacity by remember { mutableFloatStateOf(0.6f) }
    var duration by remember { mutableIntStateOf(3000) }
    var isVisible by remember { mutableStateOf(false) }
    var initialX by remember {
        mutableFloatStateOf(
            (0..screenWidthDp.toInt() - bubbleSizeDp.value.toInt()).random().toFloat()
        )
    }
    var targetX by remember {
        mutableFloatStateOf(
            (0..screenWidthDp.toInt() - bubbleSizeDp.value.toInt()).random().toFloat()
        )
    }
    var initialY by remember { mutableFloatStateOf(screenHeightDp) }
    var targetY by remember { mutableFloatStateOf(bubbleSizeDp.value) }
    var color by remember { mutableStateOf(getRandomColorFromGradient(Color.Blue, Color.Red)) }
    val transition = updateTransition(
        targetState = isVisible,
        label = ""
    )
    val y by transition.animateFloat(
        transitionSpec = { tween(duration) },
        label = "",
        targetValueByState = {
            if (it) targetY else initialY
        }
    )
    val x by transition.animateFloat(
        transitionSpec = { tween(duration) },
        label = "",
        targetValueByState = {
            if (it) targetX else initialX
        }
    )

    LaunchedEffect(active) {
        while (active) {
            delay((500..16000).random().toLong())
            color = getRandomColorFromGradient(Color(0xFFdd308f), Color(0xFF3834C0))
            bubbleSizeDp = (screenWidthDp.toInt() / 2..screenWidthDp.toInt()).random().dp
            val halfBubbleSize = bubbleSizeDp.value.toInt() / 2
            initialX = (-halfBubbleSize..screenWidthDp.toInt() - halfBubbleSize).random().toFloat()
            targetX = (-halfBubbleSize..screenWidthDp.toInt() - halfBubbleSize).random().toFloat()
            duration = (7000..9000).random()
            bubbleOpacity = Random.nextDouble(0.1, 0.2).toFloat()
            initialY = screenHeightDp + (0..1).random()
            targetY = -bubbleSizeDp.value + (0..1).random()
            isVisible = true
            delay(duration.toLong() + 300)
            duration = 10
            isVisible = false
        }
    }

    val brush = Brush.radialGradient(
        colors = listOf(
            color,
            color,
            Color.Transparent,
        )
    )

    Box(
        modifier = Modifier
            .offset(x = x.dp, y = y.dp)
            .size(bubbleSizeDp)
            .alpha(if (isVisible) bubbleOpacity else 0f)
            .background(brush = brush, shape = CircleShape)
    )

}

fun getRandomColorFromGradient(startColor: Color, endColor: Color): Color {
    val randomFactor = Random.nextDouble()
    val interpolatedRed = (startColor.red + randomFactor * (endColor.red - startColor.red)).toFloat()
    val interpolatedGreen = (startColor.green + randomFactor * (endColor.green - startColor.green)).toFloat()
    val interpolatedBlue = (startColor.blue + randomFactor * (endColor.blue - startColor.blue)).toFloat()

    return Color(interpolatedRed, interpolatedGreen, interpolatedBlue)
}

@Preview(showBackground = true)
@Composable
fun BackgroundMicPreview(
) {
    isPreview = true
    val viewModel = getWorkspaceViewModel()
    viewModel.setWorkspaceState(WorkspaceViewModel.WorkspaceState.IDLE)
    viewModel.setPlayingState(WorkspaceViewModel.PlayerState.PLAYING)
    PitchMasterBetaTheme {
        BackgroundMic()
    }
}