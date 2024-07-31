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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.pitchmasterbeta.MainActivity.Companion.isPreview
import com.example.pitchmasterbeta.ui.app.AppNavGraph
import com.example.pitchmasterbeta.ui.login.AuthViewModel
import com.example.pitchmasterbeta.ui.theme.PitchMasterBetaTheme
import com.example.pitchmasterbeta.ui.workspace.WorkspaceSurface
import com.example.pitchmasterbeta.ui.workspace.WorkspaceViewModel
import kotlinx.serialization.Serializable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init()
        handleIntent(intent = intent, isAlive = false)

        setContent {
            val navController = rememberNavController()
            val authViewModel = getAuthViewModel()
            val workspaceViewModel = getWorkspaceViewModel()
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                PitchMasterBetaTheme {
                    // A surface container using the 'background' color from the theme
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavGraph(
                            navController = navController,
                            authViewModel = authViewModel,
                            workspaceViewModel = workspaceViewModel
                        )
                    }
                }
            }

            DisposableEffect(navController) {
                val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
                    if (destination.route == null) {
                        // Invalid destination, reset to start
                        navController.navigate(AuthIntro) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                }
                navController.addOnDestinationChangedListener(listener)
                onDispose {
                    navController.removeOnDestinationChangedListener(listener)
                }
            }

            val lifecycle = LocalLifecycleOwner.current.lifecycle
            DisposableEffect(lifecycle) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        authViewModel.checkLoginStatus()
                    }
                }
                lifecycle.addObserver(observer)
                onDispose {
                    lifecycle.removeObserver(observer)
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

        fun getAuthViewModel(): AuthViewModel {
            if (isPreview) {
                return AuthViewModel()
            }
            return viewModelProvider[AuthViewModel::class.java]
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

    override fun onStop() {
        super.onStop()
        getWorkspaceViewModel().onMoveToBackground()
    }


    private fun init() {
        viewModelProvider = ViewModelProvider(this)
        appContext = applicationContext
        getWorkspaceViewModel().apply {
            if (!getIsInitialized()) {
                init(this@MainActivity)
            }
        }
    }
}

@Serializable
object AuthIntro

@Serializable
object WorkspaceIntro

@Preview(showBackground = true)
@Composable
fun MainActivityPreview(
) {
    isPreview = true
    PitchMasterBetaTheme {
        WorkspaceSurface()
    }
}