package com.example.pitchmasterbeta.ui.workspace

import android.animation.ArgbEvaluator
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.example.pitchmasterbeta.ui.theme.PitchMasterBetaTheme
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs

@Composable
fun PitchDecorations(
    viewModel: WorkspaceViewModel = WorkspaceViewModel(),
    chordHeight: Dp = 14.dp,
    maxChordWidth: Dp = 30.dp
) {
    val localDensity = LocalDensity.current
    val chordHeightPx = with(localDensity) { 14.dp.toPx() }
    val screenHeightPx = with(localDensity) { LocalConfiguration.current.screenHeightDp.dp.toPx() }

    val items by remember {
        mutableStateOf(List((screenHeightPx / chordHeightPx).toInt()) {0})
    }

    PitchDecorationColumn(viewModel, 1, maxChordWidth, items, chordHeight)

    PitchDecorationColumn(viewModel, -1, maxChordWidth, items, chordHeight)
}

@Composable
fun PitchDecorationColumn(viewModel: WorkspaceViewModel,
                          direction: Int,
                          maxChordWidth: Dp, items: List<Int>, chordHeight: Dp) {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(
            Color.Transparent,
            Color(0x2DE96ADC),
        ),
    )
    val modifier = Modifier.fillMaxHeight().width(maxChordWidth).run {
        (if (direction < 0) {
            this.offset(x = screenWidthDp.dp - maxChordWidth).scale(scaleX = -1f, scaleY = 1f)
        } else this).background(brush = gradientBrush)
    }

    Box(modifier = modifier) {
        repeat(items.size) { i ->
            PitchItem(
                modifier = Modifier
                    .width(10.dp)
                    .padding(0.dp, 3.dp)
                    .offset(x = 0.dp, y = i * chordHeight),
                color = interpolateColor(i, items.size),
                chordHeight = chordHeight - 6.dp,
            )
        }
        LazyWindowScroller(viewModel, direction, chordHeight, items)
    }

}

@Composable
fun LazyWindowScroller(viewModel: WorkspaceViewModel, direction: Int, chordHeight: Dp, items: List<Any>) {
    val localDensity = LocalDensity.current
    val scrollState = rememberLazyListState()
    val currentMidpoint = with(localDensity) { chordHeight.toPx() } * 2.5f
    val note by rememberUpdatedState(
        if (direction < 0) viewModel.sinNote.collectAsState()
        else viewModel.micNote.collectAsState()
    )
    val noteActive by rememberUpdatedState(
        if (direction < 0) viewModel.sinNoteActive.collectAsState()
        else viewModel.micNoteActive.collectAsState()
    )

    val offset = remember(scrollState) {
        derivedStateOf {
            chordHeight *
            scrollState.firstVisibleItemIndex +
            with(localDensity) { scrollState.firstVisibleItemScrollOffset.toDp() }
        }
    }

    LazyColumn(
        modifier = Modifier
            .height(chordHeight * 5)
            .offset(y = offset.value)
            .alpha(if (noteActive.value) 1f else 0.2f),
        state = scrollState
    ) {
        itemsIndexed(items) { i, _ ->
            val scaleX by remember(scrollState) {
                derivedStateOf {
                    val currentItemInfo = scrollState.layoutInfo.visibleItemsInfo
                        .firstOrNull { it.index == i }
                    if (currentItemInfo != null) {
                        val offsetFromMidpoint = currentItemInfo.offset + (currentItemInfo.size / 2) - currentMidpoint
                        val scalingFactor = (1f - minOf(1f, abs(offsetFromMidpoint) / currentMidpoint) * 0.75f)
                        scalingFactor * 3f * note.value.volume
                    } else {
                        0f
                    }
                }
            }
            PitchItem(
                modifier = Modifier
                    .width(scaleX * 10.dp)
                    .padding(0.dp, 3.dp),
                color = interpolateColor(i, items.size),
                chordHeight = chordHeight - 6.dp
            )
        }
    }
    LaunchedEffect(note.value.noteF) {
        snapshotFlow { note.value.noteF }
            .collectLatest { scrollState.animateScrollToItem((it * items.size).toInt()) }
    }
}



private fun interpolateColor(position: Int, totalItems: Int): Color {
    val fraction = position.toFloat() / (totalItems - 1)
    return Color(ArgbEvaluator().evaluate(fraction, android.graphics.Color.BLUE, android.graphics.Color.RED) as Int)
}


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
    PitchMasterBetaTheme {
        PitchDecorations()
    }
}