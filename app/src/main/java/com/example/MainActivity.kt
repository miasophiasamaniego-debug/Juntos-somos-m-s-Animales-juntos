package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.CharacterSelectScreen
import com.example.ui.screens.EcoHouseScreen
import com.example.ui.screens.GameScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LevelSelectScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AnimalEcoJumpTheme
import com.example.ui.viewmodel.GameViewModel
import com.example.ui.viewmodel.ScreenState

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AnimalEcoJumpTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: GameViewModel) {
    val screenState by viewModel.screenState.collectAsState()
    val progress by viewModel.progress.collectAsState()

    when (screenState) {
        ScreenState.MAIN_MENU -> {
            HomeScreen(
                progress = progress,
                onNavigate = { viewModel.navigateTo(it) },
                onStartGame = { viewModel.startNewGame() }
            )
        }
        ScreenState.PLAYING, ScreenState.PAUSED, ScreenState.GAME_OVER, ScreenState.VICTORY -> {
            GameScreen(
                viewModel = viewModel,
                onNavigateHome = { viewModel.navigateTo(ScreenState.MAIN_MENU) }
            )
        }
        ScreenState.ECO_HOUSE -> {
            EcoHouseScreen(
                progress = progress,
                onUpgrade = { part, bottles, cardboard ->
                    viewModel.upgradeHouse(part, bottles, cardboard)
                },
                onBack = { viewModel.navigateTo(ScreenState.MAIN_MENU) }
            )
        }
        ScreenState.CHARACTER_SELECT -> {
            CharacterSelectScreen(
                progress = progress,
                onSelectAnimal = { animalId -> viewModel.selectAnimal(animalId) },
                onUnlockAnimal = { animal -> viewModel.unlockAnimal(animal) },
                onBack = { viewModel.navigateTo(ScreenState.MAIN_MENU) }
            )
        }
        ScreenState.LEVEL_SELECT -> {
            LevelSelectScreen(
                progress = progress,
                onSelectLevel = { level -> viewModel.selectLevel(level) },
                onBack = { viewModel.navigateTo(ScreenState.MAIN_MENU) }
            )
        }
        ScreenState.SETTINGS -> {
            SettingsScreen(
                progress = progress,
                onToggleSound = { viewModel.toggleSound() },
                onToggleVibration = { viewModel.toggleVibration() },
                onBack = { viewModel.navigateTo(ScreenState.MAIN_MENU) }
            )
        }
    }
}
