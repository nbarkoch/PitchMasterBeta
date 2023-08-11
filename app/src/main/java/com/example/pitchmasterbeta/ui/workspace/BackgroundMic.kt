package com.example.pitchmasterbeta.ui.workspace

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.pitchmasterbeta.R

@Composable
fun BackgroundMic(viewModel: WorkspaceViewModel) {
    val workspaceState by rememberUpdatedState(viewModel.workspaceState.collectAsState())
    AnimatedVisibility(
        visible = workspaceState.value == WorkspaceViewModel.WorkspaceState.IDLE,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(durationMillis = 300)
        ),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(durationMillis = 300)
        ),
        modifier = Modifier
            .fillMaxSize()
            .alpha(0.15f)
            .offset(y = 70.dp)
    ) {
        Image(
            modifier = Modifier.fillMaxSize(),
            alignment = Alignment.BottomCenter,
            painter = painterResource(id = R.drawable.image_smic_bg),
            contentDescription = ""
        )
    }
}