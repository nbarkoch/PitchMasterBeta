package com.example.pitchmasterbeta.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun VerticalSeekBar(
    modifier: Modifier = Modifier,
    initialOffsetPercent: Float = 1f,
    onProgressChanged: (Float) -> Unit,
) {

    var percent by remember { mutableFloatStateOf(initialOffsetPercent.coerceIn(.05f, 1f)) }
    var height by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, _ ->
                    val offsetY =
                        (1 - percent) * height + change.position.y - change.previousPosition.y
                    percent = (1 - (offsetY / size.height.toFloat())).coerceIn(.05f, 1f)
                    onProgressChanged(percent)
                }
            }
            .clip(shape = RoundedCornerShape(10.dp))
            .onGloballyPositioned {
                height = it.size.height.toFloat()
            }
    ) {
        val gradientBrush = Brush.verticalGradient(
            colors = listOf(Color.Gray, Color(0xFFD6D6D6)),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(brush = gradientBrush)
        )

        Box(
            modifier = Modifier
                .graphicsLayer(
                    translationY = height * (1f - percent),
                )
                .fillMaxSize(1f)
                .background(Color.White)
        )
    }
}

@Preview
@Composable
fun VerticalSeekBarPreview() {
    MaterialTheme {
        VerticalSeekBar(
            modifier = Modifier
                .height(100.dp)
                .width(40.dp),
            initialOffsetPercent = 0.5f,
            onProgressChanged = {})
    }
}