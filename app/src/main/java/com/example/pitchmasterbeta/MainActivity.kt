package com.example.pitchmasterbeta

import android.content.ContentResolver
import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.ViewModelStore
import com.example.pitchmasterbeta.ui.theme.PitchMasterBetaTheme
import com.example.pitchmasterbeta.ui.workspace.WorkspaceSurface
import com.example.pitchmasterbeta.ui.workspace.WorkspaceViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContentResolver = contentResolver
        appContext = applicationContext
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr ) {
                PitchMasterBetaTheme {
                    // A surface container using the 'background' color from the theme
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        WorkspaceSurface()
                    }
                }
            }
        }
    }

    companion object {
        val viewModelStore by lazy { ViewModelStore() }
        var appContentResolver: ContentResolver? = null
        var appContext: Context? = null
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModelStore.clear()
    }

    fun keepAwake(flag: Boolean) {
        window.apply {
            if (flag) {
                addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainActivityPreview(
) {
    val viewModel = WorkspaceViewModel()
    viewModel.setWorkspaceState(WorkspaceViewModel.WorkspaceState.IDLE)
    MainActivity.viewModelStore.put("workspace", viewModel)
    PitchMasterBetaTheme {
        WorkspaceSurface()
    }
}