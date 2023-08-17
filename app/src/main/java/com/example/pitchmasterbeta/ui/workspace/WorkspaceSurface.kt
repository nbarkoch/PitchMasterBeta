package com.example.pitchmasterbeta.ui.workspace

import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.pitchmasterbeta.MainActivity.Companion.viewModelStore
import com.example.pitchmasterbeta.ui.theme.PitchMasterBetaTheme

@Composable
fun WorkspaceSurface(modifier: Modifier = Modifier
) {
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF403C63),
            Color(0xFF2E265E),
            Color(0xFF121314),
        ),
    )

    // Obtain the ViewModelStore
    if (viewModelStore["workspace"] == null) {
        viewModelStore.put("workspace", WorkspaceViewModel())
    }
    // Create or retrieve the ViewModel associated with the ViewModelStore
    val viewModel: WorkspaceViewModel = viewModelStore["workspace"] as WorkspaceViewModel
    BackPressHandler(viewModel)
    Box(modifier = modifier
        .fillMaxSize()
        .background(brush = gradientBrush))
    {
        BackgroundMic(viewModel = viewModel)
        WorkspaceBody(Modifier.fillMaxSize(), viewModel = viewModel)
        WorkspaceFooter(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter), viewModel = viewModel)
        WorkspaceHeader(
            Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter), viewModel = viewModel)
        PitchDecorations(viewModel = viewModel)
    }
}



@Preview(showBackground = true)
@Composable
fun WorkspaceSurfacePreview(
    viewModel: WorkspaceViewModel = WorkspaceViewModel()
) {
    PitchMasterBetaTheme {
        WorkspaceSurface()
    }
}