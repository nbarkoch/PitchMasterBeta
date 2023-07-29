package com.example.pitchmasterbeta.model

import androidx.compose.runtime.Immutable
import java.sql.Timestamp

@Immutable
data class LyricsSegment(
    val text: String,
    val start: String,
    val end: String
)