package com.example.pitchmasterbeta

import android.content.ContentResolver
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelStore
import com.example.pitchmasterbeta.ui.theme.PitchMasterBetaTheme
import com.example.pitchmasterbeta.ui.workspace.WorkspaceSurface

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContentResolver = contentResolver
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
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModelStore.clear()
    }

}

