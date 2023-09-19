package com.example.pitchmasterbeta.model

import androidx.compose.runtime.Immutable

@Immutable
data class LyricsSegment(
    val text: String,
    val start: Double,
    val end: Double
)

@Immutable
data class LyricsTimestampedSegment(
    val text: List<LyricsWord>,
)

@Immutable
data class LyricsWord(
    val word: String,
    val start: Double,
    val duration: Double,
)

