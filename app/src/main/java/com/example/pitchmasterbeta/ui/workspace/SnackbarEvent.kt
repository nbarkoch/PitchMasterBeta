package com.example.pitchmasterbeta.ui.workspace

import androidx.compose.material3.SnackbarDuration

data class SnackbarEvent(
    val message: String,
    val actionLabel: String?,
    val withDismissAction: Boolean,
    val duration: SnackbarDuration
)