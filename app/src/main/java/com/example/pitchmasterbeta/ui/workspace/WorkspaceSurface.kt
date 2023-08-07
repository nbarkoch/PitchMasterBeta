package com.example.pitchmasterbeta.ui.workspace

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pitchmasterbeta.MainActivity.Companion.viewModelStore
import com.example.pitchmasterbeta.R
import com.example.pitchmasterbeta.ui.theme.PitchMasterBetaTheme

@Composable
fun WorkspaceSurface(modifier: Modifier = Modifier
) {
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF443E7C),
            Color(0xFF2E265E),
            Color(0xFF121314),
        ),
    )
    // Obtain the ViewModelStore
    if (viewModelStore["workspace"] == null) {
        val it = WorkspaceViewModel()
        it.mockupLyrics()
        viewModelStore.put("workspace", it)
    }
    // Create or retrieve the ViewModel associated with the ViewModelStore
    val viewModel: WorkspaceViewModel = viewModelStore["workspace"] as WorkspaceViewModel

    Box(modifier = modifier
        .fillMaxSize()
        .background(brush = gradientBrush))
    {
        Box (
            modifier = Modifier.fillMaxSize().alpha(0.1f).padding(50.dp),
            contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(id = R.drawable.image_mic_bg),
                contentDescription = ""
            )
        }
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