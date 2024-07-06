package com.example.pitchmasterbeta.ui.workspace

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pitchmasterbeta.MainActivity.Companion.getWorkspaceViewModel
import com.example.pitchmasterbeta.MainActivity.Companion.isPreview
import com.example.pitchmasterbeta.R
import com.example.pitchmasterbeta.ui.theme.HeaderGradientBrush
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Composable
fun WorkspaceHeader(
    modifier: Modifier = Modifier,
    workspaceState: WorkspaceViewModel.WorkspaceState,
) {
    val viewModel: WorkspaceViewModel = getWorkspaceViewModel()
    val playState = viewModel.playingState.collectAsState()
    val songFullName = viewModel.songFullName.collectAsState()
    val isRecording = viewModel.isRecording.collectAsState()
    val isRecordingEnabled = viewModel.isRecordingEnabled.collectAsState()

    if (workspaceState == WorkspaceViewModel.WorkspaceState.IDLE) {
        Column(
            modifier = modifier
                .defaultMinSize(minHeight = 200.dp)
                .background(HeaderGradientBrush)
                .padding(top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MarqueeText(songFullName.value)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ScoreComposable(viewModel)
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

    val micNoteActive = viewModel.micNoteActive.collectAsState()
    val sinNoteActive = viewModel.sinNoteActive.collectAsState()
    val animatedSmallScoreScale by animateFloatAsState(
        if (micNoteActive.value && sinNoteActive.value) 0.05f else 0f,
        label = ""
    )

    val score by rememberUpdatedState(viewModel.score.collectAsState())
    val playState = viewModel.playingState.collectAsState()



    Box(
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .scale(animatedSmallScoreScale + 1f)
                .background(
                    color = Color(0x3B000000),
                    shape = RoundedCornerShape(12.dp),
                )
                .border(1.dp, color = Color.White, shape = RoundedCornerShape(12.dp))
                .padding(horizontal = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Score: ${score.value}",
                color = Color.White,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.W700,
                fontSize = 12.sp
            )
        }


    }
}


@Composable
fun RecordButton(
    onClick: () -> Unit,
    contentDesc: String,
    active: Boolean = true,
    isShown: Boolean = false,
) {
    var scale by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(active, isShown) {
        while (active && isShown) {
            scale = 1.2f
            delay(500)
            scale = 1f
            delay(500)
        }
    }

    val containerScale by animateFloatAsState(
        targetValue = if (isShown) 1f else 0f,
        label = ""
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .graphicsLayer(scaleX = containerScale, alpha = containerScale, scaleY = containerScale)
            .clickable(onClick = onClick)
            .border(1.dp, color = Color.White, shape = RoundedCornerShape(12.dp))
            .padding(start = 3.dp, end = 6.dp)
    ) {
        Image(
            painterResource(id = R.drawable.baseline_fiber_manual_record_24),
            contentDescription = contentDesc,
            modifier = Modifier
                .size(20.dp)
                .scale(scale),
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
        WorkspaceHeader(
            modifier = Modifier.fillMaxWidth(),
            workspaceState = WorkspaceViewModel.WorkspaceState.IDLE
        )
    }
}