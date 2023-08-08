package com.example.pitchmasterbeta.ui.workspace

import androidx.compose.foundation.Canvas
//import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
//import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CircleProgressbar(modifier: Modifier, progress: Float = 0f) {
    var radius by remember {
        mutableStateOf(0f)
    }

    var shapeCenter by remember {
        mutableStateOf(Offset.Zero)
    }

    var handleCenter by remember {
        mutableStateOf(Offset.Zero)
    }
    val angle = progress * 360.0
//    var angle by remember {
//        mutableStateOf(progress * 360.0)
//    }

    Canvas(
        modifier = modifier
//            .pointerInput(Unit) {
//                detectDragGestures { change, dragAmount ->
//                    handleCenter += dragAmount
//
//                    angle = getRotationAngle(handleCenter, shapeCenter)
//                    change.consume()
//                }
//            }
            .padding(5.dp)

    ) {
        shapeCenter = center

        radius = size.minDimension / 2

        val x = (shapeCenter.x + cos(Math.toRadians(angle)) * radius).toFloat()
        val y = (shapeCenter.y + sin(Math.toRadians(angle)) * radius).toFloat()

        handleCenter = Offset(x, y)

        drawCircle(color = Color(0xFF69357C).copy(alpha = 0.40f), style = Stroke(10f), radius = radius)
        drawArc(
            color = Color(0xFFDF2FA4),
            startAngle = 0f,
            sweepAngle = angle.toFloat(),
            useCenter = false,
            style = Stroke(10f)
        )

        //drawCircle(color = Color(0xFFDF2FA4), center = handleCenter, radius = 7.5f)
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
        CircleProgressbar(Modifier.size(80.dp), progress = 0.4f)
    }
}