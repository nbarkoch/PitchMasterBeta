package com.example.pitchmasterbeta.ui.workspace

data class SnackbarEvent(
    val message: String,
    val durationMilliseconds: Long,
    val actionText: String?,
    val action: (() -> Unit)?
)