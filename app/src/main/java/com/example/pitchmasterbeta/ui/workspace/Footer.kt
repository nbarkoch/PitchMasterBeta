package com.example.pitchmasterbeta.ui.workspace

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import com.example.pitchmasterbeta.MainActivity
import com.example.pitchmasterbeta.MainActivity.Companion.getWorkspaceViewModel
import com.example.pitchmasterbeta.MainActivity.Companion.isPreview

import com.example.pitchmasterbeta.R
import com.example.pitchmasterbeta.ui.theme.DarkGrey10
import com.example.pitchmasterbeta.ui.theme.Pink10
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@Composable
fun WorkspaceFooter(
    modifier: Modifier = Modifier,
) {
    val viewModel: WorkspaceViewModel = getWorkspaceViewModel()
    val context = LocalContext.current.applicationContext
    val workspaceState by rememberUpdatedState(viewModel.workspaceState.collectAsState())
    val showNotificationDialog by rememberUpdatedState(viewModel.showNotificationDialog.collectAsState())
    val showSaveAudioDialog by rememberUpdatedState(viewModel.showSaveAudioDialog.collectAsState())

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
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.viewModelScope.launch(Dispatchers.IO) {
                viewModel.handleResultUriForAudioIntent(
                    context,
                    result.data?.data
                )
                if (!viewModel.isDevMode() && !NotificationManagerCompat.from(context)
                        .areNotificationsEnabled()
                ) {
                    viewModel.showNotificationDialog()
                }
            }
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            loadMusicFromGalleryResultLauncher.launch(
                Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                )
            )
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (workspaceState.value == WorkspaceViewModel.WorkspaceState.IDLE) {
            ControlsRow()
        }
        Column(
            modifier = modifier
                .background(gradientBrush)
                .pointerInput(Unit) { detectTapGestures {} },
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
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                                requestPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                            } else {
                                loadMusicFromGalleryResultLauncher.launch(
                                    Intent(
                                        Intent.ACTION_PICK,
                                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                                    )
                                )
                            }
                        }, "pick a song from gallery", active = true)
                    }

                    else -> Box {}
                }
            }
            if (workspaceState.value == WorkspaceViewModel.WorkspaceState.IDLE) {
                DurationRow()
            }
        }
    }

    if (showNotificationDialog.value) {
        Dialog(onDismissRequest = { viewModel.hideNotificationDialog() }) {
            AlertDialog(
                onDismissRequest = { viewModel.hideNotificationDialog() },
                title = { Text("Notify when ready") },
                text = { Text("Would you like to be notify when the Karaoke is ready?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val intent = Intent()
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            } else
                                intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
                            intent.putExtra("app_package", context.packageName)
                            intent.putExtra("app_uid", context.applicationInfo.uid)
                            context.startActivity(intent)
                            viewModel.hideNotificationDialog()
                        }
                    ) {
                        Text("SURE")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.hideNotificationDialog() }
                    ) {
                        Text("NO")
                    }
                }
            )
        }
    }

    if (showSaveAudioDialog.value) {
        Dialog(onDismissRequest = { viewModel.hideSaveAudioDialog() }) {
            AlertDialog(
                onDismissRequest = { viewModel.hideSaveAudioDialog() },
                title = { Text("Save Karaoke") },
                text = { Text("Would you like to save this karaoke?") },
                confirmButton = {
                    TextButton(
                        onClick = viewModel::hideSaveAudioDialog
                    ) {
                        Text("SURE")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            viewModel.forgetKaraoke()
                            viewModel.hideSaveAudioDialog()
                        }
                    ) {
                        Text("NO")
                    }
                }
            )
        }
    }


}

@Composable
fun SimpleCircleButton(
    size: Dp,
    resource: Int,
    onClick: () -> Unit,
    contentDesc: String,
    active: Boolean = true
) {
    val boxSize = with(LocalDensity.current) { size.toPx() }
    val gradientBrush = Brush.radialGradient(
        colors = listOf(
            Color(0xFFFFFFFF),
            Color(0xFFE6E6E6),
            Color(0xFFCECECE)
        ),
        center = Offset(x = boxSize / 2, y = boxSize / 4f)
    )
    Box(
        modifier = Modifier
            .size(size)
            .shadow(elevation = 10.dp, shape = CircleShape, clip = false)
            .zIndex(10f)
            .padding(10.dp)
            .border(BorderStroke(4.dp, gradientBrush), CircleShape)
            .background(
                color = Color(0xFFE9E9E9),
                shape = CircleShape
            )
            .clip(CircleShape)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.padding(8.dp),
            enabled = active
        ) {
            Image(
                painterResource(id = resource),
                contentDescription = contentDesc,
                modifier = Modifier.size(size - 15.dp),
                colorFilter = ColorFilter.tint(
                    if (active) Pink10 else DarkGrey10
                )
            )
        }
    }

}


@Composable
fun ComplexCircleButton(
    size: Dp,
    resource: Int,
    onClick: () -> Unit = {},
    contentDesc: String,
    active: Boolean
) {
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
                containerColor = Color.White,
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
                    colorFilter = ColorFilter.tint(
                        if (active) Pink10 else DarkGrey10
                    )
                )
            }
        }
    }

}


@Composable
fun PlaygroundFooter(context: Context, viewModel: WorkspaceViewModel) {
    val activity = (LocalContext.current as? MainActivity)
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startAudioDispatchers()
        }
    }

    val progress = viewModel.progress.collectAsState()
    val playState = viewModel.playingState.collectAsState()

//    SimpleCircleButton(
//        65.dp, R.drawable.filters_1_svgrepo_com,
//        onClick = { viewModel.displayPitchControls(!viewModel.displayPitchControls.value) },
//        contentDesc = "singer voice volume"
//    )

    SimpleCircleButton(
        65.dp,
        R.drawable.wave_sine,
        onClick = { viewModel.displayPitchFactor(!viewModel.displayPitchFactor.value) },
        contentDesc = "audio pitch factor"
    )

    Box(contentAlignment = Alignment.Center) {
        CircleProgressbarButton(
            size = 70.dp,
            resource = when (playState.value) {
                WorkspaceViewModel.PlayerState.IDLE,
                WorkspaceViewModel.PlayerState.END,
                WorkspaceViewModel.PlayerState.PAUSE -> R.drawable.baseline_play_arrow_24

                WorkspaceViewModel.PlayerState.PLAYING -> R.drawable.ic_baseline_pause_24
            }, progress = progress.value, onClick = {
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
            }, onProgressChanged = { progress ->
                viewModel.jumpToTimestamp(viewModel.getTimestampFromProgress(progress))
            }, onProgressDrag = { progress ->
                viewModel.dragAndSetCurrentTime(progress)
            })
    }

    SimpleCircleButton(
        65.dp,
        R.drawable.noun_singing_mic_3242509,
        onClick = { viewModel.displaySingerVolume(!viewModel.displaySingerVolume.value) },
        contentDesc = "singer voice volume"
    )

    LaunchedEffect(playState.value) {
        activity?.keepAwake(playState.value == WorkspaceViewModel.PlayerState.PLAYING)
    }
}


@Composable
fun DurationRow() {
    val viewModel: WorkspaceViewModel = getWorkspaceViewModel()
    val current = viewModel.currentTime.collectAsState()
    val duration = viewModel.durationTime.collectAsState()
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.padding(vertical = 5.dp)
    ) {
        Text(
            color = Color.White,
            text = "${current.value}/${duration.value}",
            textAlign = TextAlign.Center,
            fontSize = 12.sp
        )
    }
}

@Composable
fun ControlsRow() {
    val viewModel: WorkspaceViewModel = getWorkspaceViewModel()
    val displaySingerVolume by rememberUpdatedState(viewModel.displaySingerVolume.collectAsState())
    val displayPitchFactor by rememberUpdatedState(viewModel.displayPitchFactor.collectAsState())
//    val displayPitchControls by rememberUpdatedState(viewModel.displayPitchControls.collectAsState())
    val volumeScale by animateFloatAsState(
        targetValue = if (displaySingerVolume.value) 1f else 0f,
        label = ""
    )
    val pitchFactorScale by animateFloatAsState(
        targetValue = if (displayPitchFactor.value) 1f else 0f,
        label = ""
    )
//    val pitchesControlsScale by animateFloatAsState(
//        targetValue = if (displayPitchControls.value) 1f else 0f,
//        label = ""
//    )
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
//        Box(
//            Modifier
//                .padding(horizontal = 10.dp)
//                .offset(x = (-50).dp)
//                .graphicsLayer(
//                    scaleX = pitchesControlsScale, scaleY = pitchesControlsScale,
//                    alpha = pitchesControlsScale
//                )
//        ) {
//            PitchControls(
//                Modifier
//                    .background(
//                        color = Color.White,
//                        shape = RoundedCornerShape(20.dp)
//                    )
//                    .padding(10.dp)
//            )
//        }
        VerticalSeekBar(
            modifier = Modifier
                .height(100.dp)
                .width(35.dp)
                .offset(x = (-55).dp)
                .graphicsLayer(
                    scaleX = pitchFactorScale,
                    alpha = pitchFactorScale,
                    scaleY = pitchFactorScale
                ),
            onProgressChanged = {
                viewModel.setPitchFactor(it)
            }, initialOffsetPercent = viewModel.getPitchFactor()
        )
        VerticalSeekBar(
            modifier = Modifier
                .height(100.dp)
                .width(35.dp)
                .offset(x = (55).dp)
                .graphicsLayer(scaleX = volumeScale, alpha = volumeScale, scaleY = volumeScale),
            onProgressChanged = {
                viewModel.setSingerVolume(it)
            }, initialOffsetPercent = viewModel.getSingerVolume()
        )

    }
}

@Composable
fun PitchControls(modifier: Modifier) {
    val viewModel: WorkspaceViewModel = getWorkspaceViewModel()
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
    isPreview = true
    MaterialTheme {
        WorkspaceFooter(modifier = Modifier.fillMaxWidth())
    }
}