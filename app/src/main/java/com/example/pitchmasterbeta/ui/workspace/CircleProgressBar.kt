package com.example.pitchmasterbeta.ui.workspace

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.pitchmasterbeta.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CircleProgressbarButton(
    resource: Int,
    progress: Float = 0f,
    onClick: () -> Unit = {},
    size: Dp = 70.dp,
    onProgressChanged: suspend (newProgress: Float) -> Unit = {},
    onProgressDrag: suspend (newProgress: Float) -> Unit = {}
) {
    var radius by remember {
        mutableFloatStateOf(0f)
    }
    var shapeCenter by remember {
        mutableStateOf(Offset.Zero)
    }
    var handleCenter by remember {
        mutableStateOf(Offset.Zero)
    }
    var isDragged by remember {
        mutableStateOf(false)
    }
    var angle by remember {
        mutableDoubleStateOf(progress * 360.0)
    }

    LaunchedEffect(progress) {
        if (!isDragged) {
            angle = progress * 360.0
        }
    }
    ComplexCircleButton(
        size,
        resource = resource,
        active = true,
        contentDesc = "player button",
        onClick = onClick,
    )
    Canvas(
        modifier = Modifier
            .size(size - 5.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragged = true },
                    onDragCancel = {
                        CoroutineScope(Dispatchers.IO).launch {
                            onProgressChanged(angle.toFloat() / 360f)
                            isDragged = false
                        }
                    },
                    onDragEnd = {
                        CoroutineScope(Dispatchers.IO).launch {
                            onProgressChanged(angle.toFloat() / 360f)
                            isDragged = false
                        }
                    },
                ) { change, dragAmount ->
                    handleCenter += dragAmount
                    val tAngle = getRotationAngle(handleCenter, shapeCenter)
                    if (abs(tAngle - angle) < 180) {
                        angle = tAngle
                    }
                    CoroutineScope(Dispatchers.IO).launch {
                        onProgressDrag(angle.toFloat() / 360f)
                    }
                    change.consume()
                }
            }
            .padding(5.dp),
    ) {
        shapeCenter = center

        radius = this.size.minDimension / 2

        val x = (shapeCenter.x + cos(Math.toRadians(angle)) * radius).toFloat()
        val y = (shapeCenter.y + sin(Math.toRadians(angle)) * radius).toFloat()

        handleCenter = Offset(x, y)

        drawCircle(
            color = Color(0xFF69357C).copy(alpha = 0.40f),
            style = Stroke(10f),
            radius = radius
        )
        drawArc(
            color = Color(0xFFDF2FA4),
            startAngle = 0f,
            sweepAngle = angle.toFloat(),
            useCenter = false,
            style = Stroke(10f)
        )
        if (isDragged) {
            drawCircle(color = Color(0xFFDF40A9), center = handleCenter, radius = 7.5f)
        }
    }
}

private fun getRotationAngle(currentPosition: Offset, center: Offset): Double {
    val (dx, dy) = currentPosition - center
    val theta = atan2(dy, dx).toDouble()

    var angle = Math.toDegrees(theta)

    if (angle < 0) {
        angle += 360.0
    }
    return angle
}

@Preview
@Composable
fun CircleProgressbarPreview() {
    MaterialTheme {
        CircleProgressbarButton(
            size = 80.dp,
            resource = R.drawable.baseline_play_arrow_24,
            progress = 0.4f
        )
    }
}