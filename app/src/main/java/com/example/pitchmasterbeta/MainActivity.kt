package com.example.pitchmasterbeta

import android.content.ContentResolver
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModelStore
import com.example.pitchmasterbeta.ui.theme.PitchMasterBetaTheme
import com.example.pitchmasterbeta.ui.workspace.WorkspaceSurface

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContentResolver = contentResolver
        appContext = applicationContext
        setContent {
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

    companion object {
        val viewModelStore by lazy { ViewModelStore() }
        var appContentResolver: ContentResolver? = null
        var appContext: Context? = null
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModelStore.clear()
    }
}

@Preview(showBackground = true)
@Composable
fun MainActivityPreview(
) {
    PitchMasterBetaTheme {
        WorkspaceSurface()
    }
}