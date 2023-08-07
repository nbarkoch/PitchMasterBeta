package com.example.pitchmasterbeta.ui.workspace

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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

@Composable
fun WorkspaceHeader(modifier: Modifier = Modifier,
                    viewModel: WorkspaceViewModel) {
    val screenHeightPx = with(LocalDensity.current) {
        LocalConfiguration.current.screenHeightDp.dp.toPx()
    }
    val colorState = viewModel.similarityColor.collectAsState()
    val color = animateColorAsState(targetValue = colorState.value, label = "", animationSpec = tween(1000))
    val score by rememberUpdatedState(viewModel.score.collectAsState())
    val playState = viewModel.playingState.collectAsState()


    val transition = updateTransition(targetState = playState.value == WorkspaceViewModel.PlayerState.END,
        label = ""
    )
    val translateY by transition.animateFloat(
        transitionSpec = { tween(1000) },
        label = "",
        targetValueByState = {
            if (it) screenHeightPx / 2.2f else 0f
        }
    )
    val scale by transition.animateFloat(
        transitionSpec = { tween(1000) },
        label = "",
        targetValueByState = {
            if (it) 1.6f else 1f
        }
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

    Box(modifier = Modifier
        .fillMaxSize()
        .alpha(alpha)
        .background(color = Color.Black))
    Box(modifier = modifier
        .background(brush = gradientBrush)
        .padding(20.dp)
        .graphicsLayer {
            translationY = translateY
            scaleY = scale
            scaleX = scale
        },
        contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .border(color = color.value, shape = CircleShape, width = 3.dp)
                    .padding(7.5.dp)
                    .widthIn(50.dp, 300.dp)
                    .heightIn(50.dp, 300.dp),
                contentAlignment = Alignment.Center) {
                Text(text = "${score.value}",
                    color = Color.White,
                    fontSize = 30.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.W500)
            }

            AnimatedVisibility(
                visible = playState.value == WorkspaceViewModel.PlayerState.END,
                enter = fadeIn(
                    animationSpec = tween(durationMillis = 200, delayMillis = 1000)
                ),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Well Done!", color = Color.White,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.W400,
                        modifier = Modifier.padding(10.dp)
                    )
                    Button(colors = ButtonDefaults.buttonColors(Color.White), onClick = {
                        viewModel.resetScoreAndWorkspaceState()
                    }) {
                        Text(text = "cancel", color = Color.Black,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,)
                    }
                }
                }
            }
    }
}


@Preview
@Composable
fun WorkspaceHeaderPreview() {
    val viewModel = WorkspaceViewModel()
    viewModel.setWorkspaceState(WorkspaceViewModel.WorkspaceState.IDLE)
    MaterialTheme {
        WorkspaceHeader(modifier = Modifier.fillMaxWidth(), viewModel = viewModel)
    }
}