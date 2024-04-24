package com.example.pitchmasterbeta.ui.pickSong

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pitchmasterbeta.MainActivity.Companion.getWorkspaceViewModel
import com.example.pitchmasterbeta.MainActivity.Companion.isPreview
import com.example.pitchmasterbeta.R
import com.example.pitchmasterbeta.ui.workspace.WorkspaceViewModel

@Composable
fun PickAudioLayout(modifier: Modifier) {
    val viewModel: WorkspaceViewModel = getWorkspaceViewModel()
    val audioList by viewModel.audioPickList().collectAsState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (audioList.isEmpty()) {
            Box(modifier = modifier,
                contentAlignment = Alignment.Center) {
                Text(
                    text = "Pick a song from gallery",
                    color = Color.White,
                    fontSize = 23.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.W700
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 150.dp)
            ) {
                item {
                    Box(
                        modifier
                            .fillMaxWidth()
                            .padding(10.dp)) {
                        Text(
                            text = "Recent Karaokes",
                            color = Color.White,
                            fontSize = 23.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.W700
                        )
                    }

                }
                items(audioList) { item ->
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, color = Color.White, shape = RoundedCornerShape(12.dp)),
                        verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {viewModel.forgetKaraoke(item.path)},
                        ) {
                            Image(
                                painterResource(id = R.drawable.app_icon_me),
                                contentDescription = "delete audio",
                                modifier = Modifier.size(22.dp),
                                colorFilter =  ColorFilter.tint(Color.White)
                            )
                        }

                        Box(modifier = Modifier.weight(1f)
                            .clickable(role = Role.Button, onClick = {
                            viewModel.onPickAudio(item)
                        }), contentAlignment = Alignment.Center) {
                            Text(text = item.name,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                style = TextStyle(color = Color.White),
                                fontSize = 17.sp)
                        }
                        IconButton(
                            onClick = {viewModel.forgetKaraoke(item.path)},
                        ) {
                            Image(
                                painterResource(id = R.drawable.baseline_close_24),
                                contentDescription = "delete audio",
                                modifier = Modifier.size(22.dp),
                                colorFilter =  ColorFilter.tint(Color.White)
                            )
                        }
                    }
                }
            }
        }
    }


}

@Preview
@Composable
fun PickAudioLayoutPreview() {
    isPreview = true
    MaterialTheme {
        PickAudioLayout(modifier = Modifier.fillMaxSize())
    }
}