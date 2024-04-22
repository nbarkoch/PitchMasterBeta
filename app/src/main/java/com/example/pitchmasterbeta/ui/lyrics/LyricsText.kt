package com.example.pitchmasterbeta.ui.lyrics

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pitchmasterbeta.MainActivity.Companion.viewModelProvider
import com.example.pitchmasterbeta.model.LyricsSegment
import com.example.pitchmasterbeta.model.LyricsTimestampedSegment
import com.example.pitchmasterbeta.model.LyricsWord
import com.example.pitchmasterbeta.ui.workspace.WorkspaceViewModel
import com.example.pitchmasterbeta.utils.math.findSubArrayListIndices
import kotlinx.coroutines.delay
import kotlin.math.floor
import kotlin.math.max

@Composable
fun LyricsText(
    segment: LyricsTimestampedSegment,
    isActive: Boolean,
    scale: Float,
    activeWord: Int,
    onClick: () -> Unit
) {
    var lines: List<List<LyricsWord>> by remember { mutableStateOf(emptyList()) }
    var isSegmentRTL by remember { mutableStateOf(false) }

    DisposableEffect(segment) {
        onDispose {
            lines = emptyList()
        }
    }

    if (lines.isEmpty()) {
        Text(
            text = segment.text.joinToString(" ") { w -> w.word },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 11.dp, vertical = 7.dp)
                .alpha(0f),
            style = TextStyle(
                color = Color(0xABFFFFFF),
                fontSize = 25.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight(700)
            ),
            onTextLayout = { textLayoutResult: TextLayoutResult ->
                val lineCount = textLayoutResult.lineCount
                val startOffset = IntArray(lineCount)
                val endOffset = IntArray(lineCount)

                // Get the start and end offsets for each line
                for (i in 0 until lineCount) {
                    startOffset[i] = textLayoutResult.getLineStart(i)
                    endOffset[i] = textLayoutResult.getLineEnd(i)
                }

                val text = segment.text.joinToString(" ") { w -> w.word }
                isSegmentRTL = text.isNotEmpty() && Character.getDirectionality(text[0]) in 1..2
                // Split the original text into lines based on offsets

                lines = (0 until lineCount).map { i ->
                    var resList = emptyList<LyricsWord>()
                    val lineText = text.substring(startOffset[i], endOffset[i]).trim().split(" ")
                    findSubArrayListIndices(
                        segment.text.map { w -> w.word },
                        lineText
                    )?.let { offsets ->
                        resList = segment.text.subList(offsets.first, offsets.second + 1)
                    }
                    resList
                }
            }
        )
    } else {
        CompositionLocalProvider(LocalLayoutDirection provides if (isSegmentRTL) LayoutDirection.Rtl else LayoutDirection.Ltr) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 7.dp)
                    .clickable(onClick = onClick),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                for (lineIdx in lines.indices) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(scale),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val lineOffset = lines.subList(0, lineIdx).flatten().size
                        for (index in lines[lineIdx].indices) {
                            val word = lines[lineIdx][index]
                            val isRTL =
                                word.word.isNotEmpty() && Character.getDirectionality(word.word[0]) in 1..2
                            val transition = updateTransition(
                                targetState = activeWord >= lineOffset + index,
                                label = ""
                            )
                            val textStyle = if (!isActive) {
                                val offset by transition.animateFloat(
                                    transitionSpec = {
                                        tween(
                                            durationMillis = (max(
                                                word.end - word.start,
                                                0.1
                                            ) * 900).toInt(),
                                            easing = LinearEasing
                                        )
                                    },
                                    label = "",
                                    targetValueByState = {
                                        if (it) 1f else 0f
                                    }
                                )
                                val colors = listOf(Color.White, Color(0x90CACACA))
                                val brush = remember(offset) {
                                    object : ShaderBrush() {
                                        override fun createShader(size: Size): Shader {
                                            val lineActiveOffset =
                                                (if (activeWord == lineOffset + index) {
                                                    size.width * offset
                                                } else if (activeWord > lineOffset + index) {
                                                    size.width
                                                } else {
                                                    0.0f
                                                }).let { if (isRTL) size.width - it else it }

                                            return LinearGradientShader(
                                                colors = if (isRTL) colors.reversed() else colors,
                                                from = Offset(lineActiveOffset, 0f),
                                                to = Offset(lineActiveOffset + 1f, 0f),
                                                tileMode = TileMode.Clamp
                                            )
                                        }
                                    }
                                }
                                TextStyle(
                                    brush = brush,
                                    fontSize = 24.sp,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight(700)
                                )
                            } else {
                                TextStyle(
                                    color = Color(0x90f2f2f2),
                                    fontSize = 24.sp,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight(700)
                                )
                            }
                            Text(
                                text = "${if (index == 0) "" else " "}${word.word}",
                                modifier = Modifier.alpha(scale * 5f - 4f),
                                style = textStyle
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun LyricsText(segment: LyricsSegment, isActive: Boolean, scale: Float) {
    var lines: List<String> by remember { mutableStateOf(emptyList()) }
    var visibleLines by remember { mutableStateOf(emptyList<Boolean>()) }
    var timeForLine by remember { mutableIntStateOf(0) }
    var isRTL by remember { mutableStateOf(false) }

    DisposableEffect(segment) {
        onDispose {
            lines = emptyList()
            visibleLines = emptyList()
        }
    }



    LaunchedEffect(isActive, lines) {
        if (isActive && lines.isNotEmpty()) {
            val totalDuration = ((segment.end - segment.start) * 1000)
            val linesLength = MutableList(lines.size) { 0 }
            var totalLength = 0
            for (i in lines.indices) {
                linesLength[i] = lines[i].length
                totalLength += lines[i].length
            }
            delay(100)
            for (index in lines.indices) {
                timeForLine =
                    floor((linesLength[index] / totalLength.toDouble()) * totalDuration * 0.95).toInt()
                visibleLines = visibleLines.toMutableList().also { it[index] = true }
                isRTL =
                    lines[index].isNotEmpty() && Character.getDirectionality(lines[index][0]) in 1..2
                delay((timeForLine).toLong())
            }
        }
    }

    if (lines.isEmpty()) {
        Text(
            text = segment.text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 11.dp, vertical = 7.dp)
                .alpha(0f),
            style = TextStyle(
                color = Color(0xABFFFFFF),
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight(700)
            ),
            onTextLayout = { textLayoutResult: TextLayoutResult ->
                val lineCount = textLayoutResult.lineCount
                val startOffset = IntArray(lineCount)
                val endOffset = IntArray(lineCount)

                // Get the start and end offsets for each line
                for (i in 0 until lineCount) {
                    startOffset[i] = textLayoutResult.getLineStart(i)
                    endOffset[i] = textLayoutResult.getLineEnd(i)
                }
                // Split the original text into lines based on offsets
                lines = (0 until lineCount).map { i ->
                    segment.text.substring(startOffset[i], endOffset[i])
                }
                visibleLines = List(lineCount) { false }
            }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            for (index in lines.indices) {
                val line = lines[index]

                val transition = updateTransition(
                    targetState = visibleLines[index],
                    label = ""
                )

                val offset by transition.animateFloat(
                    transitionSpec = {
                        tween(
                            durationMillis = timeForLine,
                            easing = LinearEasing
                        )
                    },
                    label = "",
                    targetValueByState = {
                        if (it) 1f else 0f
                    }
                )

                val brush = remember(offset) {
                    object : ShaderBrush() {
                        override fun createShader(size: Size): Shader {
                            val colors = listOf(Color.White, Color(0x90CACACA))
                            val widthOffset = if (isActive) {
                                size.width * offset
                            } else {
                                0f
                            }
                            val lineActiveOffset = if (isRTL) {
                                size.width - widthOffset
                            } else {
                                widthOffset
                            }
                            return LinearGradientShader(
                                colors = if (isRTL) colors.reversed() else colors,
                                from = Offset(lineActiveOffset, 0f),
                                to = Offset(lineActiveOffset + 1f, 0f),
                                tileMode = TileMode.Clamp
                            )
                        }
                    }
                }

                Text(
                    text = line,
                    modifier = Modifier
                        .scale(scale)
                        .alpha(scale * 3f - 2f),
                    style = TextStyle(
                        brush = brush,
                        fontSize = 23.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight(700)
                    )
                )
            }
        }
    }
}

@Preview
@Composable
fun LyricsTextPreview() {
    val viewModel = viewModelProvider[WorkspaceViewModel::class.java]
    viewModel.mockupLyrics()
    MaterialTheme {
        LyricsText(
            LyricsTimestampedSegment(listOf(LyricsWord("hello", 0.0, 2000.0))),
            true,
            1f,
            2
        ) {}
    }
}