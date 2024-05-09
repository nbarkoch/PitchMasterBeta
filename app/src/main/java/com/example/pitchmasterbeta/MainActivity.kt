package com.example.pitchmasterbeta

import android.content.Context
import android.content.Intent
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
import androidx.lifecycle.ViewModelProvider
import com.example.pitchmasterbeta.MainActivity.Companion.isPreview
import com.example.pitchmasterbeta.bridge.NativeWaveBridge
import com.example.pitchmasterbeta.ui.theme.PitchMasterBetaTheme
import com.example.pitchmasterbeta.ui.workspace.WorkspaceSurface
import com.example.pitchmasterbeta.ui.workspace.WorkspaceViewModel

class MainActivity : ComponentActivity() {
    private val nativeWaveBridge = NativeWaveBridge()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModelProvider = ViewModelProvider(this)
        appContext = applicationContext
        val viewModel = getWorkspaceViewModel()
        if (!viewModel.getIsInitialized()) {
            viewModel.init(this)
        }
        handleIntent(intent = intent, isAlive = false)
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
        private lateinit var viewModelProvider: ViewModelProvider
        var isPreview = false
        var appContext: Context? = null
        fun getWorkspaceViewModel(): WorkspaceViewModel {
            if (isPreview) {
                val viewModel = WorkspaceViewModel()
                viewModel.mockupLyrics()
                return viewModel
            }
            return viewModelProvider[WorkspaceViewModel::class.java]
        }
    }

    //viewModelStore.clear()

    fun keepAwake(flag: Boolean) {
        window.apply {
            if (flag) {
                addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    private fun handleIntent(intent: Intent?, isAlive: Boolean) {
        val extras = intent?.extras
        if (extras != null) {
            val dl = extras.getString("DL", "")
            when (dl) {
                "12" -> {
                    val jsonString = extras.getString("ref")
                    val audioPath = extras.getString("audioPath")
                    if (jsonString != null && audioPath != null) {
                        val viewModel = getWorkspaceViewModel()
                        viewModel.loadKaraokeFromIntent(jsonString, audioPath)
                    }
                }
                "11" -> {
                    if (!isAlive) {
                        val message = extras.getString("message")
                        val progress = extras.getInt("progress")
                        val duration = extras.getDouble("duration")
                        val audioPath = extras.getString("audioPath")
                        if (message != null && audioPath != null) {
                            val viewModel = getWorkspaceViewModel()
                            viewModel.loadProgressFromIntent(progress, message, duration, audioPath)
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent, isAlive = true)
    }
}

@Preview(showBackground = true)
@Composable
fun MainActivityPreview(
) {
    isPreview = true
    PitchMasterBetaTheme {
        WorkspaceSurface()
    }
}