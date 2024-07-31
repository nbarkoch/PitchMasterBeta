package com.example.pitchmasterbeta.ui.workspace

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.pitchmasterbeta.MainActivity
import com.example.pitchmasterbeta.MainActivity.Companion.isPreview
import com.example.pitchmasterbeta.ui.theme.DynamicGradientBrush
import com.example.pitchmasterbeta.ui.theme.HeaderGradientBrush
import com.example.pitchmasterbeta.ui.theme.PitchMasterBetaTheme
import com.example.pitchmasterbeta.ui.theme.PurpleDark10

@Composable
fun WorkspaceSurface(
    modifier: Modifier = Modifier
) {
    val viewModel: WorkspaceViewModel = MainActivity.getWorkspaceViewModel()

    val colorState = viewModel.similarityColor.collectAsState()
    val color = animateColorAsState(
        targetValue = colorState.value,
        label = "", animationSpec = tween(300)
    )
    val workspaceState by rememberUpdatedState(viewModel.workspaceState.collectAsState())
    val playingState by rememberUpdatedState(viewModel.playingState.collectAsState())
    val similarity = viewModel.similarity.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collect {
            snackbarHostState.showSnackbar(
                message = it.message,
                duration = it.duration,
                actionLabel = it.actionLabel,
                withDismissAction = it.withDismissAction
            )
        }
    }

    // Create or retrieve the ViewModel associated with the ViewModelStore
    WorkspaceBackHandler()
    Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        content = { innerPadding ->
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(color = PurpleDark10)
                    .background(brush = DynamicGradientBrush(color.value))
                    .padding(innerPadding)
            ) {
                BackgroundMic()
                WorkspaceBody(Modifier.fillMaxSize())
                WorkspaceFooter(
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    workspaceState = workspaceState.value
                )
                WorkspaceHeader(
                    Modifier
                        .background(HeaderGradientBrush)
                        .fillMaxWidth()
                        .align(Alignment.TopCenter), workspaceState = workspaceState.value
                )
            }
            DialogOverlay(playingState.value == WorkspaceViewModel.PlayerState.END)
            AudioVisualizerScreen(
                isVisible = workspaceState.value == WorkspaceViewModel.WorkspaceState.IDLE,
                similarity = similarity.value
            )
        })

}


@Preview(showBackground = true)
@Composable
fun WorkspaceSurfacePreview(
) {
    isPreview = true
    PitchMasterBetaTheme {
        WorkspaceSurface()
    }
}