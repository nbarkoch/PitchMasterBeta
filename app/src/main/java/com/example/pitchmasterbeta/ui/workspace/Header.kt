package com.example.pitchmasterbeta.ui.workspace

import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pitchmasterbeta.MainActivity.Companion.getWorkspaceViewModel
import com.example.pitchmasterbeta.MainActivity.Companion.isPreview
import com.example.pitchmasterbeta.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Composable
fun WorkspaceHeader(
    modifier: Modifier = Modifier,
) {
    val viewModel: WorkspaceViewModel = getWorkspaceViewModel()
    val workspaceState = viewModel.workspaceState.collectAsState()
    val playState = viewModel.playingState.collectAsState()
    val songFullName = viewModel.songFullName.collectAsState()
    val isRecording = viewModel.isRecording.collectAsState()
    val isRecordingEnabled = viewModel.isRecordingEnabled.collectAsState()

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

    Box(
        modifier =
        Modifier
            .fillMaxSize()
            .alpha(alpha)
            .background(color = Color.Black)
    )
    if (workspaceState.value == WorkspaceViewModel.WorkspaceState.IDLE) {
        Column(
            modifier = modifier.defaultMinSize(minHeight = 200.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MarqueeText(songFullName.value)
            Box(Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    ScoreComposable(viewModel)
                }
                RecordButton(
                    isShown = playState.value == WorkspaceViewModel.PlayerState.PLAYING && isRecordingEnabled.value,
                    active = isRecording.value,
                    onClick = { viewModel.setRecording(!viewModel.isRecording.value) },
                    contentDesc = "recording audio button, currently ${if (isRecording.value) "enabled" else "disabled"}"
                )
            }
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

    val micNoteActive = viewModel.micNoteActive.collectAsState()
    val sinNoteActive = viewModel.sinNoteActive.collectAsState()
    val animatedSmallScoreScale by animateFloatAsState(
        if (micNoteActive.value && sinNoteActive.value) 0.05f else 0f,
        label = ""
    )

    val score by rememberUpdatedState(viewModel.score.collectAsState())
    val playState = viewModel.playingState.collectAsState()

    var grade by remember { mutableIntStateOf(0) }

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
                    .background(
                        color = Color(0x3B000000),
                        shape = CircleShape,
                    )
//                    .border(
//                        color = Color.White,
//                        shape = CircleShape,
//                        width = (animatedSmallScoreScale * 50).dp
//                    )
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
                    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ResultedButtons(viewModel)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ResultedButtons(viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResultedButtons(viewModel: WorkspaceViewModel) {
    val isRecordingEnabled = viewModel.isRecordingEnabled.collectAsState()
    val recordSaved = viewModel.recordSaved.collectAsState()
    val isRecording = viewModel.isRecording.collectAsState()
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.saveRecording()
        }
    }

    if (isRecordingEnabled.value && isRecording.value) {
        Button(colors = ButtonDefaults.buttonColors(
            if (recordSaved.value)
                Color(0xFFB297B7)
            else Color(0xFFD183DF)
        ),
            enabled = !recordSaved.value,
            modifier = Modifier.defaultMinSize(
                minWidth = ButtonDefaults.MinWidth, minHeight = 10.dp
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            onClick = {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    requestPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                } else {
                    viewModel.saveRecording()
                }
            }) {
            Text(
                text = "Save Recording", color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Image(
                painterResource(id = R.drawable.baseline_save_24),
                contentDescription = "",
                modifier = Modifier
                    .size(22.dp)
                    .padding(start = 5.dp),
                colorFilter = ColorFilter.tint(Color.White)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
    }
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

@Composable
fun RecordButton(
    onClick: () -> Unit,
    contentDesc: String,
    active: Boolean = true,
    isShown: Boolean = false,
) {
    val scale = remember { Animatable(1f) }
    val volumeScale by animateFloatAsState(
        targetValue = if (isShown) 1f else 0f,
        label = ""
    )

    LaunchedEffect(active) {
        while (active) {
            scale.animateTo(
                targetValue = 1.2f,
                animationSpec = tween(durationMillis = 500, easing = LinearEasing)
            )
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = LinearEasing)
            )
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(start = 15.dp, top = 10.dp)
            .graphicsLayer(scaleX = volumeScale, alpha = volumeScale, scaleY = volumeScale)
            .clickable(onClick = onClick)
            .border(1.dp, color = Color.White, shape = RoundedCornerShape(12.dp))
            .padding(start = 3.dp, end = 6.dp)
    ) {
        Image(
            painterResource(id = R.drawable.baseline_fiber_manual_record_24),
            contentDescription = contentDesc,
            modifier = Modifier
                .size(20.dp)
                .scale(scale.value),
            colorFilter = if (active) ColorFilter.tint(Color(0xFFBE0D31)) else
                ColorFilter.tint(Color(0xFF8A5D66))
        )
        Text(
            text = if (active) "Recording" else "Not Recording",
            color = if (active) Color.White else Color.LightGray,
            fontWeight = FontWeight.W700, fontSize = 11.sp
        )
    }

}

@Preview
@Composable
fun WorkspaceHeaderPreview() {
    isPreview = true
    MaterialTheme {
        WorkspaceHeader(modifier = Modifier.fillMaxWidth())
    }
}