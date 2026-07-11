package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.data.AppContainer
import com.example.ui.ScenicApp
import com.example.ui.ScenicViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    
    private val viewModel: ScenicViewModel by viewModels {
        val appContainer = AppContainer(applicationContext)
        ScenicViewModel.Factory(
            appContainer.scenicRepository,
            appContainer.settingsManager,
            appContainer.firebaseBackupManager,
            appContainer.weatherApi
        )
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Handle incoming intent action
        handleIntent(intent, viewModel)
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ScenicApp(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent, viewModel)
    }

    private fun handleIntent(intent: Intent?, viewModel: ScenicViewModel) {
        if (intent != null && intent.getStringExtra("action") == "quick_scout") {
            viewModel.triggerQuickScoutFromTile()
        }
    }
}
