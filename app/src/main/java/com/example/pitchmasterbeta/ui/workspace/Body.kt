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
import com.example.pitchmasterbeta.MainActivity
import com.example.pitchmasterbeta.ui.lyrics.LyricsLazyColumn
import com.example.pitchmasterbeta.ui.lyrics.LyricsText

@Composable
fun WorkspaceBody(modifier: Modifier) {
    val viewModel: WorkspaceViewModel =
        MainActivity.viewModelStore["workspace"] as WorkspaceViewModel
    val workspaceState by rememberUpdatedState(viewModel.workspaceState.collectAsState())
    val loadingMessage by rememberUpdatedState(viewModel.notificationMessage.collectAsState())

    when (workspaceState.value) {
        WorkspaceViewModel.WorkspaceState.IDLE -> {
            LyricsLazyColumn(modifier.fillMaxSize())
        }

        WorkspaceViewModel.WorkspaceState.PICK -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Pick a song from gallery",
                    color = Color.White,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.W400
                )
            }
        }

        WorkspaceViewModel.WorkspaceState.WAITING -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LyricsText(
                    segment = loadingMessage.value,
                    true,
                    1f
                )
            }
        }

        else -> {

        }
    }
}