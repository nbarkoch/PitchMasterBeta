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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.example.pitchmasterbeta.ui.theme.PitchMasterBetaTheme
import kotlin.math.abs

@Composable
fun PitchDecorations(
    viewModel: WorkspaceViewModel = WorkspaceViewModel(),
    chordHeight: Dp = 14.dp,
    maxChordWidth: Dp = 30.dp
) {

    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val items by remember {
        mutableStateOf(List((screenHeightDp.dp / chordHeight).toInt()) {0})
    }

    PitchDecorationColumn(viewModel, 1, maxChordWidth, items, chordHeight)

    PitchDecorationColumn(viewModel, -1, maxChordWidth, items, chordHeight)
}

@Composable
fun PitchDecorationColumn(viewModel: WorkspaceViewModel,
                          direction: Int,
                          maxChordWidth: Dp, items: List<Int>, chordHeight: Dp) {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val modifier = if (direction > 0) Modifier
        .fillMaxHeight()
        .width(maxChordWidth) else Modifier
        .fillMaxHeight()
        .width(maxChordWidth)
        .offset(x = screenWidthDp.dp - maxChordWidth)
        .scale(scaleX = -1f, scaleY = 1f)
    Box(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(items) { i, _ ->
                PitchItem(
                    modifier = Modifier
                        .width(10.dp)
                        .padding(0.dp, 3.dp),
                    color = interpolateColor(i, items.size),
                    chordHeight = chordHeight - 6.dp
                )
            }
        }
        LazyWindowScroller(viewModel, direction, chordHeight, items)
    }

}

@Composable
fun LazyWindowScroller(viewModel: WorkspaceViewModel, direction: Int, chordHeight: Dp, items: List<Any>) {
    val localDensity = LocalDensity.current
    val scrollState = rememberLazyListState()
    val note by rememberUpdatedState(
        if (direction > 0)
            viewModel.sinNoteI.collectAsState()
        else
            viewModel.micNoteI.collectAsState()
    )
    val volume by rememberUpdatedState(
        if (direction > 0)
            viewModel.sinVolume.collectAsState()
        else
            viewModel.micVolume.collectAsState()
    )
    val active by rememberUpdatedState(
        if (direction > 0)
            viewModel.sinActive.collectAsState()
        else
            viewModel.micActive.collectAsState()
    )
    val offset = remember(scrollState) {
        derivedStateOf {
            chordHeight *
            scrollState.firstVisibleItemIndex +
            with(localDensity)
            { scrollState.firstVisibleItemScrollOffset.toDp() }
        }
    }

    LazyColumn(
        modifier = Modifier
            .height(chordHeight * 5)
            .offset(y = offset.value).alpha(if (active.value) 1f else 0.3f),
        state = scrollState
    ) {
        itemsIndexed(items) { i, _ ->
            val scaleX by remember {
                derivedStateOf {
                    val currentItemInfo = scrollState.layoutInfo.visibleItemsInfo
                        .firstOrNull { it.index == i }
                        ?: return@derivedStateOf 0f
                    val itemHalfSize = currentItemInfo.size / 2
                    val currentMidpoint = with(localDensity) { chordHeight.toPx() } * 2.5f
                    val scalingFactor = (1f - minOf(1f, abs(currentItemInfo.offset + itemHalfSize - currentMidpoint) / currentMidpoint) * 0.75f)
                    scalingFactor * 4f
                }
            }
            PitchItem(
                modifier = Modifier
                    .width((scaleX * volume.value) * 10.dp)
                    .padding(0.dp, 3.dp),
                color = interpolateColor(i, items.size),
                chordHeight = chordHeight - 6.dp
            )
        }
    }
    LaunchedEffect(note) {
        val chordIndex = (note.value * items.size).toInt()
        scrollState.animateScrollToItem(chordIndex)
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