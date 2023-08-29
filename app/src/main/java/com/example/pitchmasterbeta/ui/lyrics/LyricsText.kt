package com.example.pitchmasterbeta.ui.lyrics

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@OptIn(ExperimentalTextApi::class)
@Composable
fun LyricsText(text: String, isActive: Boolean, scale: Float, start: Long = 0, end: Long =8000){
    var lines: List<String> by remember { mutableStateOf(emptyList()) }
    var visibles by remember { mutableStateOf(emptyList<Boolean>()) }
    var timeForLine by remember { mutableStateOf(0) }

    LaunchedEffect(isActive, lines){
        if (isActive) {
            timeForLine = ((end - start) / lines.size).toInt() + 500
            for (index in lines.indices) {
                visibles = visibles.toMutableList().also { it[index] = true }
                delay(timeForLine.toLong() - 500)
            }
        }
    }

    if (lines.isEmpty()) {
            Text(
                text = text,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 7.dp)
                    .alpha(0f),
                style = TextStyle(
                    color = Color.White,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    fontWeight =  FontWeight.Bold
                ),
                onTextLayout = {textLayoutResult: TextLayoutResult ->
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
                        text.substring(startOffset[i], endOffset[i])
                    }
                    visibles = List(lineCount) { false }
                }
            )
    } else {
        LazyColumn {
            items(lines.size) { index ->
                val line = lines[index]
                val transition = updateTransition(
                    targetState = visibles[index],
                    label = ""
                )

                val offset by transition.animateFloat(
                    transitionSpec = { tween(timeForLine) },
                    label = "",
                    targetValueByState = {
                        if (it) 1f else 0f
                    }
                )

                val brush = remember(offset) {
                    object : ShaderBrush() {
                        override fun createShader(size: Size): Shader {
                            val widthOffset = size.width * offset
                            return LinearGradientShader(
                                colors = if (visibles[index]) listOf(Color.White, Color.White,Color.White,Color.Black) else listOf(Color.Black, Color.Black),
                                from = Offset(0f, size.height),
                                to = Offset(widthOffset * 1.4f, size.height),
                                tileMode = TileMode.Clamp
                            )
                        }
                    }
                }

                Text(
                    text = line,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                        .scale(scale)
                        .alpha(scale * 4f - 3f),
                    style = TextStyle(
                        brush = brush,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight(400)
                    )
                )
            }
        }
    }
}