package com.example.pitchmasterbeta.model

import androidx.compose.runtime.Immutable

@Immutable
data class LyricsSegment(
    val text: String,
    val start: Double,
    val end: Double
)