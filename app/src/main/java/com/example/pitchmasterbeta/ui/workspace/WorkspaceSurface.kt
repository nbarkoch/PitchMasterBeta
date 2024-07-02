package com.example.pitchmasterbeta.ui.workspace

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.example.pitchmasterbeta.MainActivity
import com.example.pitchmasterbeta.MainActivity.Companion.isPreview
import com.example.pitchmasterbeta.ui.theme.DynamicGradientBrush
import com.example.pitchmasterbeta.ui.theme.FooterGradientBrush
import com.example.pitchmasterbeta.ui.theme.HeaderGradientBrush
import com.example.pitchmasterbeta.ui.theme.PitchMasterBetaTheme
import com.example.pitchmasterbeta.ui.theme.PurpleDark10
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WorkspaceSurface(
    modifier: Modifier = Modifier
) {
    val viewModel: WorkspaceViewModel = MainActivity.getWorkspaceViewModel()
    val scaffoldState = rememberScaffoldState()
    val snackbarEvent by viewModel.snackbarEvent.collectAsState(initial = null)
    val colorState = viewModel.similarityColor.collectAsState()
    val color =
        animateColorAsState(targetValue = colorState.value, label = "", animationSpec = tween(300))
    val workspaceState by rememberUpdatedState(viewModel.workspaceState.collectAsState())
    val similarity = viewModel.similarity.collectAsState()

    LaunchedEffect(snackbarEvent) {
        snackbarEvent?.let {
            viewModel.viewModelScope.launch {
                scaffoldState.snackbarHostState.showSnackbar(it.message)
                delay(it.durationMilliseconds)
                scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()
            }
        }
    }


    // Create or retrieve the ViewModel associated with the ViewModelStore
    WorkspaceBackHandler()
    Scaffold(
        scaffoldState = scaffoldState,
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(color = PurpleDark10)
                .background(brush = DynamicGradientBrush(color.value))
                .padding(innerPadding)
        )
        {
            BackgroundMic()
            WorkspaceBody(Modifier.fillMaxSize())
            WorkspaceFooter(
                Modifier
                    .background(FooterGradientBrush)
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
            )
            WorkspaceHeader(
                Modifier
                    .background(HeaderGradientBrush)
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 20.dp)
            )
        }
        AudioVisualizerScreen(
            isVisible = workspaceState.value == WorkspaceViewModel.WorkspaceState.IDLE,
            similarity = similarity.value
        )
    }

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