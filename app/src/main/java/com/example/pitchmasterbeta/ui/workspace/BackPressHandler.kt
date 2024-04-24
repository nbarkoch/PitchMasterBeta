package com.example.pitchmasterbeta.ui.workspace

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.example.pitchmasterbeta.MainActivity.Companion.getWorkspaceViewModel


@Composable
fun WorkspaceBackHandler() {
    val viewModel: WorkspaceViewModel = getWorkspaceViewModel()
    val workspaceState = viewModel.workspaceState.collectAsState()
    val playState = viewModel.playingState.collectAsState()
    BackHandler(
        enabled = !(workspaceState.value == WorkspaceViewModel.WorkspaceState.PICK ||
                workspaceState.value == WorkspaceViewModel.WorkspaceState.INTRO)
    ) {
        when (workspaceState.value) {
            WorkspaceViewModel.WorkspaceState.IDLE -> {
                when (playState.value) {
                    WorkspaceViewModel.PlayerState.PLAYING -> {
                        viewModel.pauseAudioDispatchers()
                    }

                    WorkspaceViewModel.PlayerState.IDLE,
                    WorkspaceViewModel.PlayerState.PAUSE -> {
                        viewModel.resetWorkspace()
                    }

                    WorkspaceViewModel.PlayerState.END -> {
                        viewModel.resetScoreAndPlayingState()
                    }
                }
            }

            WorkspaceViewModel.WorkspaceState.WAITING -> {
                viewModel.stopGenerateKaraoke()
                viewModel.stopLoadKaraoke()
                viewModel.resetWorkspace()
            }

            else -> {
                // do nothing
            }
        }
    }
}