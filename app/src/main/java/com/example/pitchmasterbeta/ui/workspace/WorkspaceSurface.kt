package com.example.pitchmasterbeta.ui.workspace

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewModelScope
import com.example.pitchmasterbeta.MainActivity
import com.example.pitchmasterbeta.MainActivity.Companion.isPreview
import com.example.pitchmasterbeta.ui.theme.PitchMasterBetaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WorkspaceSurface(
    modifier: Modifier = Modifier
) {
    val viewModel: WorkspaceViewModel = MainActivity.getWorkspaceViewModel()
    val scaffoldState = rememberScaffoldState()
    val snackbarEvent by viewModel.snackbarEvent.collectAsState(initial = null)
    LaunchedEffect(snackbarEvent) {
        snackbarEvent?.let {
            viewModel.viewModelScope.launch {
                scaffoldState.snackbarHostState.showSnackbar(it.message)
                delay(it.durationMilliseconds)
                scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()
            }
        }
    }

    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF403C63),
            Color(0xFF2E265E),
            Color(0xFF121314),
        ),
    )

    // Create or retrieve the ViewModel associated with the ViewModelStore
    WorkspaceBackHandler()
    Scaffold(
        scaffoldState = scaffoldState,
    ) { innerPadding ->
        Box(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(brush = gradientBrush)
        )
        {
            BackgroundMic()
            WorkspaceBody(Modifier.fillMaxSize())
            WorkspaceFooter(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            )
            WorkspaceHeader(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )
            PitchDecorations()
        }
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