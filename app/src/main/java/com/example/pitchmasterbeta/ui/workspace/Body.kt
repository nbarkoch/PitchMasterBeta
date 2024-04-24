package com.example.pitchmasterbeta.ui.workspace

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.pitchmasterbeta.MainActivity.Companion.getWorkspaceViewModel
import com.example.pitchmasterbeta.MainActivity.Companion.isPreview
import com.example.pitchmasterbeta.ui.lyrics.LyricsLazyColumn
import com.example.pitchmasterbeta.ui.lyrics.LyricsText
import com.example.pitchmasterbeta.ui.pickSong.PickAudioLayout
import com.example.pitchmasterbeta.ui.theme.PitchMasterBetaTheme

@Composable
fun WorkspaceBody(modifier: Modifier) {
    val viewModel: WorkspaceViewModel = getWorkspaceViewModel()
    val workspaceState by rememberUpdatedState(viewModel.workspaceState.collectAsState())
    val loadingMessage by rememberUpdatedState(viewModel.notificationMessage.collectAsState())

    when (workspaceState.value) {
        WorkspaceViewModel.WorkspaceState.IDLE -> {
            LyricsLazyColumn(modifier.fillMaxSize())
        }

        WorkspaceViewModel.WorkspaceState.PICK -> {
            PickAudioLayout(modifier.fillMaxSize())
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

@Preview(showBackground = true)
@Composable
fun WorkspaceBodyPreview(
) {
    isPreview = true
    PitchMasterBetaTheme {
        WorkspaceBody(Modifier.fillMaxSize())
    }
}