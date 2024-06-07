package com.example.pitchmasterbeta.ui.login.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun LoadingOverlay(loading: Boolean) {
    AnimatedVisibility(
        visible = loading,
        enter = fadeIn(animationSpec = tween(1000)),
        exit = fadeOut(animationSpec = tween(1000))
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xC8AC90E0)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Please Wait..", color = Color.White,
                fontSize = 20.sp, fontWeight = FontWeight.W700
            )
        }
    }
}