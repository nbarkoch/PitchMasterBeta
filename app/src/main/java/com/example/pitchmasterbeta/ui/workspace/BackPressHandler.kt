package com.example.pitchmasterbeta.ui.workspace

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.example.pitchmasterbeta.MainActivity.Companion.viewModelProvider


@Composable
fun WorkspaceBackHandler() {
    val viewModel: WorkspaceViewModel =
        viewModelProvider[WorkspaceViewModel::class.java]
    val workspaceState = viewModel.workspaceState.collectAsState()
    val playState = viewModel.playingState.collectAsState()
    val context = LocalContext.current.applicationContext
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
                viewModel.stopGenerateKaraoke(context)
                viewModel.stopLoadKaraoke()
                viewModel.resetWorkspace()
            }

            else -> {
                // do nothing
            }
        }
    }
}