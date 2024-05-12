package com.example.pitchmasterbeta.ui.lyrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.example.pitchmasterbeta.MainActivity.Companion.getWorkspaceViewModel
import com.example.pitchmasterbeta.MainActivity.Companion.isPreview
import com.example.pitchmasterbeta.ui.workspace.WorkspaceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LyricsLazyColumn(
    modifier: Modifier = Modifier,
) {
    val viewModel: WorkspaceViewModel = getWorkspaceViewModel()
    // Observe changes in scrollToPosition and trigger smooth scroll when it changes
    val listState = rememberLazyListState()
    val scrollToPosition by rememberUpdatedState(viewModel.lyricsScrollToPosition.collectAsState())
    val activeWordIndex by rememberUpdatedState(viewModel.lyricsActiveWordIndex.collectAsState())
    val segments by viewModel.lyricsSegments.collectAsState()
    var isLazyColumnActiveByUser by remember { mutableStateOf(false) }
    var lastTimeoutJob by remember { mutableStateOf<Job?>(null) }

    // Create element height in pixel state
    var columnMidpoint by remember {
        mutableFloatStateOf(0f)
    }


    LazyColumn(state = listState,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .pointerInteropFilter {
                isLazyColumnActiveByUser = true
                false
            }
            .onGloballyPositioned { coordinates ->
                columnMidpoint = coordinates.size.height.toFloat() / 2f
            }) {
        item {
            Spacer(modifier = Modifier.height(Dp((columnMidpoint * 0.9f) / LocalDensity.current.density)))
        }

        itemsIndexed(segments) { i, item ->
            val opacity by remember {
                derivedStateOf {
                    val currentItemInfo =
                        listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == i }
                            ?: return@derivedStateOf 0.65f
                    val currentMidpoint = (columnMidpoint * 2)
                    val divider = if (i == 0) 1.75f else 1.25f
                    val itemMidpoint = (currentItemInfo.size / 2f) + currentItemInfo.offset
                    (1f - minOf(
                        1f, abs(columnMidpoint / divider - itemMidpoint) / currentMidpoint
                    ))
                }
            }
            LyricsText(
                segment = item,
                isActive = scrollToPosition.value > i,
                scale = opacity,
                activeWord = if (scrollToPosition.value == i) activeWordIndex.value else -1
            ) {
                viewModel.viewModelScope.launch {
                    if (opacity > 0.8) {
                        viewModel.jumpToTimestamp(item.text.first().start)
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(Dp((columnMidpoint * 0.9f) / LocalDensity.current.density)))
        }
    }

    LaunchedEffect(scrollToPosition.value) {
        if (!isLazyColumnActiveByUser) {
            listState.animateScrollToItem(scrollToPosition.value + 1, -columnMidpoint.toInt())
        }
    }

    LaunchedEffect(isLazyColumnActiveByUser) {
        if (isLazyColumnActiveByUser) {
            val timeoutMillis = 5000L  // Timeout duration in milliseconds (adjust as needed)
            lastTimeoutJob?.cancel()
            lastTimeoutJob = withContext(Dispatchers.Main) {
                delay(timeoutMillis)
                isLazyColumnActiveByUser = false
                null // Reset lastTimeoutJob after completion
            }
        }
    }
}

@Preview
@Composable
fun CustomLazyColumnPreview() {
    isPreview = true
    MaterialTheme {
        LyricsLazyColumn(modifier = Modifier.fillMaxSize())
    }
}