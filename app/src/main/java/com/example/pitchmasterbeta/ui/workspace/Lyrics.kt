package com.example.pitchmasterbeta.ui.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pitchmasterbeta.model.LyricsSegment
import kotlin.math.abs

@Composable
fun LyricsLazyColumn(modifier: Modifier = Modifier,
                     viewModel: WorkspaceViewModel) {
    // Observe changes in scrollToPosition and trigger smooth scroll when it changes
    val listState = rememberLazyListState()
    val scrollToPosition by rememberUpdatedState(viewModel.scrollToPosition.collectAsState())
    val segments by viewModel.yourItemsList.collectAsState()


    // Create element height in pixel state
    var columnMidpoint by remember {
        mutableStateOf(0f)
    }


    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                columnMidpoint = coordinates.size.height.toFloat() / 2f
            }) {
        item {
            Spacer(modifier = Modifier.height(Dp((columnMidpoint * 0.9f) / LocalDensity.current.density)))
        }

        itemsIndexed(segments) { i, item ->
            val opacity by remember {
                derivedStateOf {
                    val currentItemInfo = listState.layoutInfo.visibleItemsInfo
                        .firstOrNull { it.index == i }
                        ?: return@derivedStateOf 0.65f
                    val currentMidpoint = (columnMidpoint * 2)
                    //val parallaxOffset = (i.toFloat() / (segments.size - 1)) * (columnMidpoint * 2)
                    val divider = if (i == 0) 1.75f else 1.25f
                    val itemMidpoint = (currentItemInfo.size / 2f) + currentItemInfo.offset
                    (1f - minOf(1f, abs(columnMidpoint / divider - itemMidpoint) / currentMidpoint) * 0.75f)
                }
            }

            Column {
                Text(
                    text = item.text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                        .scale(opacity)
                        .alpha(opacity * 4f - 3f),
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = if (scrollToPosition.value == i) FontWeight.Bold else FontWeight(400)
                    )
                )
            }
        }
        item {
            Spacer(modifier = Modifier.height(Dp((columnMidpoint * 0.9f) / LocalDensity.current.density)))
        }
    }

    LaunchedEffect(scrollToPosition.value) {
        listState.animateScrollToItem(scrollToPosition.value, -(columnMidpoint * 0.7f).toInt() )
    }
}

@Preview
@Composable
fun CustomLazyColumnPreview() {
    val viewModel = WorkspaceViewModel()
    viewModel.mockupLyrics()
    MaterialTheme {
        LyricsLazyColumn(modifier = Modifier.fillMaxSize(), viewModel = viewModel)
    }
}