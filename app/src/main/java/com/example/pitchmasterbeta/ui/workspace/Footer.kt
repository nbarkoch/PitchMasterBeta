package com.example.pitchmasterbeta.ui.workspace

import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentCompositionLocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewModelScope
import com.example.pitchmasterbeta.MainActivity
import com.example.pitchmasterbeta.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@Composable
fun WorkspaceFooter(modifier: Modifier = Modifier,
                    viewModel: WorkspaceViewModel) {
    val context = LocalContext.current.applicationContext
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,
            Color(0xFF1E0D3A),
            Color(0xFF0E0D3A)
        ),
    )

    val loadMusicFromGalleryResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == ComponentActivity.RESULT_OK) {
            viewModel.viewModelScope.launch(Dispatchers.IO) {
                viewModel.handleResultUriForAudioIntent(context, MainActivity.appContentResolver, result.data?.data)
            }
        }
    }

    Row(
        modifier.background(gradientBrush),
        horizontalArrangement = Arrangement.SpaceAround,
        ) {
        ComplexCircleButton(R.drawable.baseline_library_music_24, onClick = {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
            loadMusicFromGalleryResultLauncher.launch(intent)
        }, "pick a song from gallery", active = true)
    }


}

@Composable
fun SimpleCircleButton(resource: Int, onClick: () -> Unit, contentDesc: String, active: Boolean) {
    val boxSize = with(LocalDensity.current) { 60.dp.toPx() }
    val gradientBrush = Brush.radialGradient(
        colors = listOf(
            Color(0xFFFFFFFF),
            Color(0xFFE6E6E6),
            Color(0xFFCECECE)
        ),
        center = Offset(x = boxSize / 2, y = boxSize / 4f)
    )
    Box(modifier = Modifier
        .shadow(elevation = 10.dp, shape = CircleShape, clip = false)
        .zIndex(10f)
        .padding(10.dp)
        .border(BorderStroke(4.dp, gradientBrush), CircleShape)
        .background(
            color = Color(0xFFE9E9E9),
            shape = CircleShape
        )
        .clip(CircleShape)) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.padding(8.dp)
        ) {
            Image(
                painterResource(id = resource),
                contentDescription = contentDesc,
                modifier = Modifier.size(40.dp),
                colorFilter = ColorFilter.tint(Color(0xFFDF2FA4))
            )
        }
    }

}




@Composable
fun ComplexCircleButton(resource: Int, onClick: () -> Unit, contentDesc: String, active: Boolean) {
        Box(
            modifier = Modifier.padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = CircleShape,
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 20.dp,
                ),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFFFFF),
                ),
                modifier = Modifier
                    .size(60.dp)
                    .shadow(20.dp, CircleShape)
            ) {
                // Placeholder for progress circle content
            }
            Card(
                shape = CircleShape,
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 40.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE6E6E6),
                ),
                modifier = Modifier
                    .size(50.dp)
                    .shadow(20.dp, CircleShape)
            ) {
                IconButton(
                    onClick = onClick,
                    modifier = Modifier
                        .padding(5.dp),
                    enabled = active
                ) {
                    Image(
                        painter = painterResource(id = resource),
                        contentDescription = contentDesc,
                        modifier = Modifier.size(40.dp),
                        colorFilter = ColorFilter.tint(if (active) Color(0xFFDF2FA4) else Color(0xFF333333))
                    )
                }
            }
        }

}