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
import com.example.pitchmasterbeta.MainActivity.Companion.appContext
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
        val viewModel = WorkspaceViewModel()
        appContext?.run {
            viewModel.initTempFiles(this)
        }
        viewModelStore.put("workspace", viewModel)
    }
    // Create or retrieve the ViewModel associated with the ViewModelStore
    BackPressHandler()
    Box(modifier = modifier
        .fillMaxSize()
        .background(brush = gradientBrush))
    {
        BackgroundMic()
        WorkspaceBody(Modifier.fillMaxSize())
        WorkspaceFooter(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter))
        WorkspaceHeader(
            Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter))
        PitchDecorations()
    }
}



@Preview(showBackground = true)
@Composable
fun WorkspaceSurfacePreview(
) {
    PitchMasterBetaTheme {
        WorkspaceSurface()
    }
}