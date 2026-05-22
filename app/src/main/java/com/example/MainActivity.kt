package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.game.GameUiState
import com.example.game.GameViewModel
import com.example.ui.GameScreen
import com.example.ui.LevelSelectScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CandyMatchApp(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun CandyMatchApp(
    modifier: Modifier = Modifier,
    viewModel: GameViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when (val currentState = state) {
        is GameUiState.Idle -> {
            // Loading levels or prepping database
        }
        is GameUiState.LevelSelect -> {
            LevelSelectScreen(
                levels = currentState.levels,
                onLevelSelected = { level -> viewModel.selectLevel(level) },
                modifier = modifier
            )
        }
        is GameUiState.Playing -> {
            GameScreen(
                state = currentState,
                onCellSelected = { r, c -> viewModel.handleCellSelection(r, c) },
                onSwipe = { r, c, dRow, dCol -> viewModel.handleSwipe(r, c, dRow, dCol) },
                onRestart = { viewModel.selectLevel(currentState.level) },
                onExit = { viewModel.goBackToLevelSelect() },
                onAdvanceLevel = { viewModel.selectNextLevel(currentState.level.id) },
                onSelectPowerUp = { powerUp -> viewModel.selectPowerUp(powerUp) },
                modifier = modifier
            )
        }
    }
}
