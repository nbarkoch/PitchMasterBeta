package com.example.pitchmasterbeta.ui.workspace

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

@Composable
fun WorkspaceBody(modifier: Modifier, viewModel: WorkspaceViewModel) {
    val workspaceState by rememberUpdatedState(viewModel.workspaceState.collectAsState())
    when (workspaceState.value) {
        WorkspaceViewModel.WorkspaceState.IDLE -> {
            LyricsLazyColumn(modifier.fillMaxSize(), viewModel = viewModel)
        }
        WorkspaceViewModel.WorkspaceState.PICK -> {
            Box( modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center) {
                Text(text = "Pick a song from gallery",
                    color = Color.White,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.W400
                )
            }
        }
        else -> {

        }
    }
}