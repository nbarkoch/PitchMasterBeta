package com.example.pitchmasterbeta.ui.workspace

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import com.example.pitchmasterbeta.MainActivity
import com.example.pitchmasterbeta.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@Composable
fun WorkspaceFooter(modifier: Modifier = Modifier,
                    viewModel: WorkspaceViewModel) {
    val context = LocalContext.current.applicationContext
    val workspaceState by rememberUpdatedState(viewModel.workspaceState.collectAsState())

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,
            Color(0xFF1E0D3A),
            Color(0xFF0E0D3A)
        ),
    )

    val loadMusicFromGalleryResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == ComponentActivity.RESULT_OK) {
            viewModel.viewModelScope.launch(Dispatchers.IO) {
                viewModel.handleResultUriForAudioIntent(context, MainActivity.appContentResolver, result.data?.data)
            }
        }
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally)  {
        if (workspaceState.value == WorkspaceViewModel.WorkspaceState.IDLE){
            ControlsRow(viewModel = viewModel)
        }

        Column( modifier = modifier.background(gradientBrush),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,

                ) {

                when (workspaceState.value) {
                    WorkspaceViewModel.WorkspaceState.IDLE -> {
                        PlaygroundFooter(context, viewModel)
                    }

                    WorkspaceViewModel.WorkspaceState.PICK -> {
                        ComplexCircleButton(70.dp, R.drawable.baseline_library_music_24, onClick = {
                            val intent =
                                Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
                            loadMusicFromGalleryResultLauncher.launch(intent)
                        }, "pick a song from gallery", active = true)
                    }

                    else -> Box{}
                }
            }
            if (workspaceState.value == WorkspaceViewModel.WorkspaceState.IDLE) {
                DurationRow(viewModel = viewModel)
            }
        }
    }



}

@Composable
fun SimpleCircleButton(size: Dp, resource: Int, onClick: () -> Unit, contentDesc: String, active: Boolean = true) {
    val boxSize = with(LocalDensity.current) { size.toPx() }
    val gradientBrush = Brush.radialGradient(
        colors = listOf(
            Color(0xFFFFFFFF),
            Color(0xFFE6E6E6),
            Color(0xFFCECECE)
        ),
        center = Offset(x = boxSize / 2, y = boxSize / 4f)
    )
    Box(modifier = Modifier
        .size(size)
        .shadow(elevation = 10.dp, shape = CircleShape, clip = false)
        .zIndex(10f)
        .padding(10.dp)
        .border(BorderStroke(4.dp, gradientBrush), CircleShape)
        .background(
            color = Color(0xFFE9E9E9),
            shape = CircleShape
        )
        .clip(CircleShape)) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.padding(8.dp),
            enabled = active
        ) {
            Image(
                painterResource(id = resource),
                contentDescription = contentDesc,
                modifier = Modifier.size(size - 15.dp),
                colorFilter = if (active) ColorFilter.tint(Color(0xFFDF2FA4)) else
                                ColorFilter.tint(Color(0xFF343435))
            )
        }
    }

}




@Composable
fun ComplexCircleButton(size: Dp, resource: Int, onClick: () -> Unit, contentDesc: String, active: Boolean) {
        Box(
            modifier = Modifier.padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = CircleShape,
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 20.dp,
                ),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFFFFF),
                ),
                modifier = Modifier
                    .size(size - 10.dp)
                    .shadow(20.dp, CircleShape)
            ) {
                // Placeholder for progress circle content
            }
            Card(
                shape = CircleShape,
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 40.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE6E6E6),
                ),
                modifier = Modifier
                    .size(size - 20.dp)
                    .shadow(20.dp, CircleShape)
            ) {
                IconButton(
                    onClick = onClick,
                    modifier = Modifier
                        .padding(5.dp),
                    enabled = active
                ) {
                    Image(
                        painter = painterResource(id = resource),
                        contentDescription = contentDesc,
                        modifier = Modifier.size(size - 20.dp),
                        colorFilter = ColorFilter.tint(if (active) Color(0xFFDF2FA4) else Color(0xFF333333))
                    )
                }
            }
        }

}


@Composable
fun PlaygroundFooter(context: Context, viewModel: WorkspaceViewModel) {

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {}

    val progress = viewModel.progress.collectAsState()
    val playState = viewModel.playingState.collectAsState()

    SimpleCircleButton(65.dp, R.drawable.filters_1_svgrepo_com,
        onClick = { viewModel.displayPitchControls(!viewModel.displayPitchControls.value) },
        contentDesc = "singer voice volume")

    Box(  contentAlignment = Alignment.Center) {
        ComplexCircleButton(70.dp,
            when (playState.value) {
                WorkspaceViewModel.PlayerState.IDLE,
                WorkspaceViewModel.PlayerState.END,
                WorkspaceViewModel.PlayerState.PAUSE -> R.drawable.baseline_play_arrow_24
                WorkspaceViewModel.PlayerState.PLAYING -> R.drawable.ic_baseline_pause_24
            }, onClick = {
            when (playState.value) {
                WorkspaceViewModel.PlayerState.IDLE -> {
                    val recordPermissionGranted = ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (recordPermissionGranted) {
                        viewModel.startAudioDispatchers()
                    } else {
                        requestPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                    }
                }
                WorkspaceViewModel.PlayerState.PLAYING -> {
                    viewModel.pauseAudioDispatchers()
                }
                WorkspaceViewModel.PlayerState.PAUSE -> {
                    viewModel.continueAudioDispatchers()
                }
                else -> {}
            }
        }, "start karaoke", active = true)
        CircleProgressbar(Modifier.size(65.dp), progress = progress.value)
    }

    SimpleCircleButton(65.dp, R.drawable.noun_singing_mic_3242509,
        onClick = { viewModel.displaySingerVolume(!viewModel.displaySingerVolume.value) }, contentDesc = "singer voice volume")
}


@Composable
fun DurationRow(viewModel: WorkspaceViewModel) {
    val current = viewModel.currentTime.collectAsState()
    val duration = viewModel.durationTime.collectAsState()
    Box(contentAlignment = Alignment.Center,
        modifier = Modifier.padding(vertical = 5.dp)) {
        Text(color = Color.White,
            text = "${current.value}/${duration.value}",
            textAlign = TextAlign.Center,
            fontSize = 12.sp)
    }
}

@Composable
fun ControlsRow(viewModel: WorkspaceViewModel) {
    val displaySingerVolume by rememberUpdatedState(viewModel.displaySingerVolume.collectAsState())
    val displayPitchControls by rememberUpdatedState(viewModel.displayPitchControls.collectAsState())
    val volumeScale by animateFloatAsState(targetValue = if (displaySingerVolume.value) 1f else 0f,
        label = ""
    )
    val pitchesControlsScale by animateFloatAsState(targetValue = if (displayPitchControls.value) 1f else 0f,
        label = ""
    )
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ){
        Box(
            Modifier
                .padding(horizontal = 10.dp)
                .offset(x = (-50).dp)
                .graphicsLayer(
                    scaleX = pitchesControlsScale, scaleY = pitchesControlsScale,
                    alpha = pitchesControlsScale
                )) {
            PitchControls(
                Modifier
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(10.dp), viewModel = viewModel)
        }
        VerticalSeekBar(modifier = Modifier
            .height(100.dp)
            .width(35.dp)
            .offset(x = (-5).dp)
            .graphicsLayer(scaleX = volumeScale, alpha = volumeScale, scaleY = volumeScale),
            onProgressChanged = {
                viewModel.setSingerVolume(it)
            }, initialOffsetPercent = viewModel.getSingerVolume())
    }
}

@Composable
fun PitchControls(modifier: Modifier, viewModel: WorkspaceViewModel) {
    val isComputingPitchMic by rememberUpdatedState(viewModel.isComputingPitchMic.collectAsState())
    val isComputingPitchSinger by rememberUpdatedState(viewModel.isComputingPitchSinger.collectAsState())

    Column(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,

        ) {
            Text(text = "Singer", Modifier.padding(horizontal = 10.dp))
            Switch(checked = isComputingPitchSinger.value, onCheckedChange = {
                viewModel.isComputingPitchSinger(it)
            })
        }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Mic", Modifier.padding(horizontal = 10.dp))
            Switch(checked = isComputingPitchMic.value, onCheckedChange = {
                viewModel.isComputingPitchMic(it)
            })
        }
    }
}

@Preview
@Composable
fun WorkspaceFooterPreview() {
    val viewModel = WorkspaceViewModel()
    viewModel.setWorkspaceState(WorkspaceViewModel.WorkspaceState.IDLE)
    MaterialTheme {
        WorkspaceFooter(modifier = Modifier.fillMaxWidth(), viewModel = viewModel)
    }
}