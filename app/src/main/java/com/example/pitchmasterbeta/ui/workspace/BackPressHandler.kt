package com.example.pitchmasterbeta.ui.workspace

import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.pitchmasterbeta.MainActivity


@Composable
fun BackPressHandler(
) {
    val viewModel: WorkspaceViewModel = MainActivity.viewModelStore["workspace"] as WorkspaceViewModel
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val workspaceState = viewModel.workspaceState.collectAsState()
    val playState = viewModel.playingState.collectAsState()
    val backPressEnabled = remember(workspaceState) {
        derivedStateOf {
            workspaceState.value !== WorkspaceViewModel.WorkspaceState.PICK &&
                    workspaceState.value !== WorkspaceViewModel.WorkspaceState.INTRO
        }
    }


    val backCallbackHandler = object : OnBackPressedCallback(true) {
        val context = LocalContext.current.applicationContext
        override fun handleOnBackPressed() {
            super.isEnabled = backPressEnabled.value
            if (!isEnabled) {
                return
            }
            when (workspaceState.value) {
                WorkspaceViewModel.WorkspaceState.IDLE ->
                {
                    when (playState.value) {
                        WorkspaceViewModel.PlayerState.PLAYING -> {
                            viewModel.pauseAudioDispatchers()
                        }
                        WorkspaceViewModel.PlayerState.IDLE,
                        WorkspaceViewModel.PlayerState.PAUSE ->{
                            viewModel.resetWorkspace()
                        }
                        WorkspaceViewModel.PlayerState.END ->{
                            viewModel.resetScoreAndPlayingState()
                        }
                    }
                }
                else -> {
                    viewModel.stopGenerateKaraoke(context)
                    viewModel.resetWorkspace()
                }
            }
        }
    }

    DisposableEffect(onBackPressedDispatcher) {
        onBackPressedDispatcher?.addCallback(backCallbackHandler)
        onDispose {
            backCallbackHandler.remove()
        }
    }
}