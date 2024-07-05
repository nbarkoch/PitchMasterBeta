package com.example.pitchmasterbeta.ui.workspace

import android.Manifest
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pitchmasterbeta.MainActivity
import com.example.pitchmasterbeta.R
import com.example.pitchmasterbeta.ui.theme.PitchMasterBetaTheme

@Composable
fun DialogOverlay(visible: Boolean) {
    val viewModel: WorkspaceViewModel = MainActivity.getWorkspaceViewModel()
    var grade by remember { mutableIntStateOf(0) }
    var opinion by remember { mutableStateOf("") }
    val score by rememberUpdatedState(viewModel.score.collectAsState())

    LaunchedEffect(visible) {
        if (visible) {
            opinion = viewModel.giveOpinionForScore(score.value)
            val expectedScore = viewModel.getExpectedScore()
            if (expectedScore > 0) {
                grade = ((score.value * 100f) / viewModel.getExpectedScore()).toInt()
            }
        }
    }


    val transition = updateTransition(
        targetState = visible,
        label = ""
    )

//    val offsetY =
//        (with(LocalDensity.current) { LocalConfiguration.current.screenHeightDp.dp.toPx() / 2f - 100.dp.toPx() * 1f })
//    val translateY by transition.animateFloat(
//        transitionSpec = { tween(1000) },
//        label = "",
//        targetValueByState = {
//            if (it) offsetY else 0f
//        }
//    )

    val scale by transition.animateFloat(
        transitionSpec = { tween(500) },
        label = "",
        targetValueByState = {
            if (it) 1f else 0f
        }
    )

    val alpha by transition.animateFloat(
        transitionSpec = { tween(1000) },
        label = "",
        targetValueByState = {
            if (it) 0.75f else 0.0f
        }
    )

    val alpha2 by animateFloatAsState(
        targetValue = if (scale > 0.8f) 1f else 0f,
        animationSpec = tween(durationMillis = 500), label = ""
    )

    Box(
        modifier =
        Modifier
            .fillMaxSize()
            .background(color = Color.Black.copy(alpha))
            .then(if (visible) Modifier.pointerInput(Unit) {} else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier =
            Modifier
                .scale(scale)
                .background(color = Color(0xFF0E0D3A), shape = RoundedCornerShape(20.dp))
                .padding(30.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(alpha2)
            ) {
                Text(
                    text = "%$grade", color = Color.White,
                    fontSize = 32.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.W600,
                )
                Text(
                    text = opinion, color = Color.White,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.W400,
                    modifier = Modifier.padding(5.dp)
                )
                Box(Modifier.padding(top = 20.dp)) {
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
                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                } else {
                    viewModel.saveRecording()
                }
            }) {
            Text(
                text = "Save Recording", color = Color.White,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
            )
            Image(
                painterResource(id = R.drawable.baseline_save_24),
                contentDescription = "",
                modifier = Modifier
                    .size(32.dp)
                    .padding(start = 5.dp),
                colorFilter = ColorFilter.tint(Color.White)
            )
        }
        Spacer(
            modifier = Modifier
                .width(16.dp)
                .height(10.dp)
        )
    }
    Button(colors = ButtonDefaults.buttonColors(Color.White),
        modifier = Modifier.defaultMinSize(
            minWidth = ButtonDefaults.MinWidth, minHeight = 10.dp
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        onClick = {
            viewModel.resetScoreAndPlayingState()
        }) {
        Text(
            text = "Cancel", color = Color.Black,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DialogOverlayPreview(
) {
    MainActivity.isPreview = true
    PitchMasterBetaTheme {
        DialogOverlay(true)
    }
}