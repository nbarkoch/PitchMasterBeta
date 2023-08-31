package com.example.pitchmasterbeta.ui.lyrics

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pitchmasterbeta.MainActivity
import com.example.pitchmasterbeta.model.LyricsSegment
import com.example.pitchmasterbeta.ui.workspace.WorkspaceViewModel
import kotlinx.coroutines.delay
import kotlin.math.floor

@OptIn(ExperimentalTextApi::class)
@Composable
fun LyricsText(segment: LyricsSegment, isActive: Boolean, scale: Float) {
    var lines: List<String> by remember { mutableStateOf(emptyList()) }
    var visibleLines by remember { mutableStateOf(emptyList<Boolean>()) }
    var timeForLine by remember { mutableStateOf(0) }

    DisposableEffect(segment) {
        onDispose {
            lines = emptyList()
            visibleLines = emptyList()
        }
    }

    LaunchedEffect(isActive, lines) {
        if (isActive && lines.isNotEmpty()) {
            val totalDuration = ((segment.end - segment.start) * 1000)
            val linesLength =  MutableList(lines.size) { 0 }
            var totalLength = 0
            for (i in lines.indices) {
                linesLength[i] = lines[i].length
                totalLength += lines[i].length
            }
            delay(100)
            for (index in lines.indices) {
                timeForLine = floor((linesLength[index] / totalLength.toDouble()) * totalDuration * 0.95).toInt()
                visibleLines = visibleLines.toMutableList().also { it[index] = true }
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
                fontSize = 19.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
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
                            val widthOffset = if (isActive) {
                                size.width * offset
                            } else {
                                0f
                            }
                            return LinearGradientShader(
                                colors = listOf(Color.White, Color(0x90CACACA)),
                                from = Offset(widthOffset, 0f),
                                to = Offset(widthOffset + 1f, 0f),
                                tileMode = TileMode.Clamp
                            )
                        }
                    }
                }

                Text(
                    text = line,
                    modifier = Modifier
                        .scale(scale)
                        .alpha(scale * 4f - 3f),
                    style = TextStyle(
                        brush = brush,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = if (offset > 0 || isActive) FontWeight.Bold else FontWeight(400)
                    )
                )
            }
        }
    }
}

@Preview
@Composable
fun LyricsTextPreview() {
    if (MainActivity.viewModelStore["workspace"] == null) {
        val viewModel = WorkspaceViewModel()
        viewModel.mockupLyrics()
        MainActivity.viewModelStore.put("workspace", viewModel)
    }
    MaterialTheme {
        LyricsText(LyricsSegment("This is a simple text", 0.0, 7000.0), true,1f)
    }
}