package com.example

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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup Dependency Injection container manually for 100% robust compiles
        val appContainer = AppContainer(applicationContext)
        
        // Initialize viewmodel
        val viewModel: ScenicViewModel by viewModels {
            ScenicViewModel.Factory(
                appContainer.scenicRepository,
                appContainer.settingsManager,
                appContainer.firebaseBackupManager
            )
        }
        
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
}
