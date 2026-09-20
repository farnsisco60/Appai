package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.GhostStudioViewModel
import com.example.ui.GhostUiState
import com.example.ui.screens.BatchScreen
import com.example.ui.screens.CameraScreen
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.GhostStudioTheme
import com.example.ui.theme.StudioDarkBg

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GhostStudioTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = StudioDarkBg
                ) {
                    GhostStudioApp()
                }
            }
        }
    }
}

@Composable
fun GhostStudioApp(
    viewModel: GhostStudioViewModel = viewModel()
) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            when (val state = uiState) {
                is GhostUiState.Ready -> {
                    EditorScreen(
                        viewModel = viewModel,
                        state = state,
                        onNavigateBack = {
                            viewModel.resetToHome()
                        }
                    )
                }
                is GhostUiState.BatchProcessing -> {
                    BatchScreen(
                        viewModel = viewModel,
                        batchState = state,
                        onNavigateBack = {
                            viewModel.resetToHome()
                        }
                    )
                }
                else -> {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToCamera = {
                            navController.navigate("camera")
                        }
                    )
                }
            }
        }

        composable("camera") {
            CameraScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
