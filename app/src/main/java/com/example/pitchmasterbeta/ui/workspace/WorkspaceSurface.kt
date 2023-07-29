package com.example.pitchmasterbeta.ui.workspace

import android.content.ContentResolver
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelStore
import com.example.pitchmasterbeta.MainActivity.Companion.viewModelStore
import com.example.pitchmasterbeta.ui.theme.PitchMasterBetaTheme
import kotlin.random.Random

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
        WorkspaceBody(Modifier.fillMaxSize(), viewModel = viewModel)
        WorkspaceFooter(Modifier.fillMaxWidth().align(Alignment.BottomCenter), viewModel = viewModel)
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