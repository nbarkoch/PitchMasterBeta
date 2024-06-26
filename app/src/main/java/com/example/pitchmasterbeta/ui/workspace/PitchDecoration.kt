package com.example.pitchmasterbeta.ui.workspace

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.example.pitchmasterbeta.MainActivity.Companion.getWorkspaceViewModel
import com.example.pitchmasterbeta.MainActivity.Companion.isPreview
import com.example.pitchmasterbeta.ui.theme.PitchMasterBetaTheme
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs


@Composable
fun PitchDecorations(
    chordHeight: Dp = 14.dp,
    maxChordWidth: Dp = 30.dp,
    pitchItemWidth: Dp = 9.dp
) {
    val viewModel: WorkspaceViewModel = getWorkspaceViewModel()
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val numOfItems = (screenHeightDp.dp / chordHeight).toInt()
    PitchDecorationColumn(viewModel, 1, maxChordWidth, numOfItems, chordHeight, pitchItemWidth)

    PitchDecorationColumn(viewModel, -1, maxChordWidth, numOfItems, chordHeight, pitchItemWidth)
}

@Composable
fun PitchDecorationColumn(
    viewModel: WorkspaceViewModel,
    direction: Int,
    maxChordWidth: Dp, numOfItems: Int, chordHeight: Dp, pitchItemWidth: Dp
) {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val colorState = viewModel.similarityColor.collectAsState()
    val workspaceState by rememberUpdatedState(viewModel.workspaceState.collectAsState())

    val color =
        animateColorAsState(targetValue = colorState.value, label = "", animationSpec = tween(300))

    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(
            color.value.copy(alpha = 0.3f),
            Color.Transparent,
        ),
        startX = 0.0f,
        endX = 40f
    )


    val baseModifier = Modifier
        .fillMaxHeight()
        .width(maxChordWidth)
    val modifier = (if (direction < 0) {
        baseModifier
            .offset(x = screenWidthDp.dp - maxChordWidth)
            .scale(scaleX = -1f, scaleY = 1f)
    } else baseModifier)



    AnimatedVisibility(
        visible = workspaceState.value == WorkspaceViewModel.WorkspaceState.IDLE,
        enter = slideInHorizontally(
            initialOffsetX = { direction * -it },
            animationSpec = tween(durationMillis = 300)
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { direction * -it },
            animationSpec = tween(durationMillis = 300)
        )
    ) {
        Box(modifier = modifier.background(brush = gradientBrush)) {
            repeat(numOfItems) { i ->
                PitchItem(
                    modifier = Modifier
                        .width(pitchItemWidth)
                        .padding(0.dp, 3.dp)
                        .offset(x = 0.dp, y = i * chordHeight),
                    color = color.value,
                    chordHeight = chordHeight - 6.dp,
                )
            }
            LazyWindowScroller(viewModel, direction, chordHeight, numOfItems, pitchItemWidth)
        }
    }
}

@Composable
fun LazyWindowScroller(
    viewModel: WorkspaceViewModel,
    direction: Int,
    chordHeight: Dp,
    numOfItems: Int,
    pitchItemWidth: Dp,
) {
    val localDensity = LocalDensity.current
    val scrollState = rememberLazyListState()
    val colorState = viewModel.similarityColor.collectAsState()
    val color =
        animateColorAsState(targetValue = colorState.value, label = "", animationSpec = tween(300))

    val chordHeightPx = with(localDensity) { chordHeight.toPx() }
    val currentMidpoint = chordHeightPx * 2.5f
    val note by rememberUpdatedState(
        if (direction < 0) viewModel.sinNote.collectAsState()
        else viewModel.micNote.collectAsState()
    )
    val noteActive by rememberUpdatedState(
        if (direction < 0) viewModel.sinNoteActive.collectAsState()
        else viewModel.micNoteActive.collectAsState()
    )
    val items by remember {
        mutableStateOf(List(numOfItems) { 0 })
    }
    val offsetY = remember(scrollState) {
        derivedStateOf {
            chordHeightPx *
                    scrollState.firstVisibleItemIndex + scrollState.firstVisibleItemScrollOffset
        }
    }

    LazyColumn(
        modifier = Modifier
            .height(chordHeight * 5)
            .graphicsLayer {
                translationY = offsetY.value
            }
            .alpha(if (noteActive.value) 1f else 0.2f),
        state = scrollState
    ) {
        itemsIndexed(items) { i, _ ->
            val scaleX by remember(scrollState) {
                derivedStateOf {
                    val currentItemInfo = scrollState.layoutInfo.visibleItemsInfo
                        .firstOrNull { it.index == i }
                    if (currentItemInfo != null) {
                        val offsetFromMidpoint =
                            currentItemInfo.offset + (currentItemInfo.size / 2) - currentMidpoint
                        val scalingFactor =
                            (1f - minOf(1f, abs(offsetFromMidpoint) / currentMidpoint))
                        scalingFactor * 3.5f * note.value.volume
                    } else {
                        0f
                    }
                }
            }
            PitchItem(
                modifier = Modifier
                    .width(scaleX * pitchItemWidth)
                    .padding(0.dp, 3.dp),
                color = color.value,
                chordHeight = chordHeight - 6.dp
            )
        }
    }
    LaunchedEffect(note.value.noteF) {
        snapshotFlow { note.value.noteF }
            .collectLatest {
                scrollState.animateScrollToItem(
                    (it * items.size).toInt(),
                )
            }
    }
}


//val argbEvaluator = ArgbEvaluator()
//val firstColor = Color(0xFF9830dd).toArgb()
//val secondColor = Color(0xFFdd308f).toArgb()
//private fun interpolateColor(position: Int, totalItems: Int): Color {
//    return Color(
//        argbEvaluator.evaluate(
//            position.toFloat() / (totalItems - 1), firstColor, secondColor
//        ) as Int
//    )
//}


@Composable
fun PitchItem(
    modifier: Modifier = Modifier,
    color: Color,
    chordHeight: Dp
) {
    Box(
        modifier = modifier
            .height(chordHeight)
            .clip(shape = RoundedCornerShape(0.dp, 20.dp, 20.dp, 0.dp))
            .background(color = color)
    )
}

@Preview(showBackground = true)
@Composable
fun PitchDecorationsPreview() {
    isPreview = true
    val viewModelDummy = WorkspaceViewModel()
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val chordHeight: Dp = 14.dp
    val maxChordWidth: Dp = 30.dp
    val pitchItemWidth: Dp = 10.dp
    PitchMasterBetaTheme {
        PitchDecorationColumn(
            viewModel = viewModelDummy,
            direction = 1,
            maxChordWidth = maxChordWidth,
            numOfItems = (screenHeightDp.dp / chordHeight).toInt(),
            chordHeight = chordHeight,
            pitchItemWidth = pitchItemWidth
        )
    }
}