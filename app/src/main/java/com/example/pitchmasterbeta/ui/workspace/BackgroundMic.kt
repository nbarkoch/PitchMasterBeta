package com.example.pitchmasterbeta.ui.workspace

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
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
import com.example.pitchmasterbeta.MainActivity
import com.example.pitchmasterbeta.R
import com.example.pitchmasterbeta.ui.theme.PitchMasterBetaTheme
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun BackgroundMic() {
    val viewModel: WorkspaceViewModel =
        MainActivity.viewModelStore["workspace"] as WorkspaceViewModel
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
    val viewModel: WorkspaceViewModel =
        MainActivity.viewModelStore["workspace"] as WorkspaceViewModel
    val playState by rememberUpdatedState(viewModel.playingState.collectAsState())

    repeat(60) {
        BubbleAnimation(playState.value == WorkspaceViewModel.PlayerState.PLAYING)
    }
}

@Composable
fun BubbleAnimation(active: Boolean) {
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp.value
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp.value
    var bubbleSizeDp by remember { mutableStateOf(50.dp) }
    var bubbleOpacity by remember { mutableStateOf(0.6f) }
    var duration by remember { mutableStateOf(3000) }
    var isVisible by remember { mutableStateOf(false) }
    var initialX by remember {
        mutableStateOf(
            (0..screenWidthDp.toInt() - bubbleSizeDp.value.toInt()).random().toFloat()
        )
    }
    var targetX by remember {
        mutableStateOf(
            (0..screenWidthDp.toInt() - bubbleSizeDp.value.toInt()).random().toFloat()
        )
    }
    var initialY by remember { mutableStateOf(screenHeightDp) }
    var targetY by remember { mutableStateOf(bubbleSizeDp.value) }
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

    val color by transition.animateColor(
        transitionSpec = {
            tween(durationMillis = duration)
        }, label = ""
    ) {
        Color(if (it) android.graphics.Color.BLUE else android.graphics.Color.RED)
    }


    LaunchedEffect(active) {
        while (active) {
            delay((500..16000).random().toLong())
            bubbleSizeDp = (20..screenWidthDp.toInt() / 2).random().dp
            val halfBubbleSize = bubbleSizeDp.value.toInt() / 2
            initialX = (-halfBubbleSize..screenWidthDp.toInt() - halfBubbleSize).random().toFloat()
            targetX = (0..screenWidthDp.toInt() - bubbleSizeDp.value.toInt()).random().toFloat()
            duration = (7000..9000).random()
            bubbleOpacity = Random.nextDouble(0.1, 0.25).toFloat()
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

@Preview(showBackground = true)
@Composable
fun BackgroundMicPreview(
) {
    if (MainActivity.viewModelStore["workspace"] == null) {
        val viewModel = WorkspaceViewModel()
        viewModel.setWorkspaceState(WorkspaceViewModel.WorkspaceState.IDLE)
        viewModel.setPlayingState(WorkspaceViewModel.PlayerState.PLAYING)
        MainActivity.viewModelStore.put("workspace", viewModel)
    }
    PitchMasterBetaTheme {
        BackgroundMic()
    }
}