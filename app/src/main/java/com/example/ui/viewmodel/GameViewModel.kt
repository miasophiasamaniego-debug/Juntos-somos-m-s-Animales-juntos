package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundEffects
import com.example.data.AppDatabase
import com.example.data.GameProgressEntity
import com.example.data.GameRepository
import com.example.data.HousePart
import com.example.game.AnimalCharacter
import com.example.game.GameEngine
import com.example.game.GameEvent
import com.example.game.GameLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScreenState {
    MAIN_MENU,
    LEVEL_SELECT,
    CHARACTER_SELECT,
    ECO_HOUSE,
    SETTINGS,
    PLAYING,
    PAUSED,
    GAME_OVER,
    VICTORY
}

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GameRepository
    val soundEffects = SoundEffects(application.applicationContext)

    val progress: StateFlow<GameProgressEntity>
    private val _screenState = MutableStateFlow(ScreenState.MAIN_MENU)
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    private val _selectedLevel = MutableStateFlow(GameLevel.ALL_LEVELS.first())
    val selectedLevel: StateFlow<GameLevel> = _selectedLevel.asStateFlow()

    var gameEngine: GameEngine? = null
        private set

    // Realtime UI updates for HUD during gameplay
    private val _hudState = MutableStateFlow(HudData())
    val hudState: StateFlow<HudData> = _hudState.asStateFlow()

    private var gameLoopJob: Job? = null

    init {
        val database = AppDatabase.getDatabase(application)
        repository = GameRepository(database.gameDao())
        
        progress = repository.gameProgress.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GameProgressEntity()
        )

        // Keep repository synced
        viewModelScope.launch(Dispatchers.IO) {
            progress.collectLatest { p ->
                // Ensures initial database record exists
                if (p.id == 0) {
                    repository.saveProgress(GameProgressEntity())
                }
            }
        }
    }

    fun navigateTo(state: ScreenState) {
        if (state != ScreenState.PLAYING && state != ScreenState.PAUSED) {
            stopGameLoop()
        }
        _screenState.value = state
    }

    fun selectLevel(level: GameLevel) {
        _selectedLevel.value = level
        startNewGame(level)
    }

    fun selectAnimal(animalId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSelectedAnimal(animalId)
        }
    }

    fun unlockAnimal(animal: AnimalCharacter) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = repository.unlockAnimal(
                animal.id,
                animal.bottleCost,
                animal.cardboardCost
            )
            if (success) {
                soundEffects.playVictorySound(progress.value.soundEnabled)
            }
        }
    }

    fun upgradeHouse(part: HousePart, bottleCost: Int, cardboardCost: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = repository.upgradeHousePart(part, bottleCost, cardboardCost)
            if (success) {
                soundEffects.playVictorySound(progress.value.soundEnabled)
            }
        }
    }

    fun startNewGame(level: GameLevel = _selectedLevel.value) {
        val currentProgress = progress.value
        val animal = AnimalCharacter.getById(currentProgress.selectedAnimalId)

        gameEngine = GameEngine(character = animal, level = level)
        _screenState.value = ScreenState.PLAYING
        startGameLoop()
    }

    fun pauseGame() {
        if (_screenState.value == ScreenState.PLAYING) {
            stopGameLoop()
            _screenState.value = ScreenState.PAUSED
        }
    }

    fun resumeGame() {
        if (_screenState.value == ScreenState.PAUSED) {
            _screenState.value = ScreenState.PLAYING
            startGameLoop()
        }
    }

    fun onJumpPressed() {
        gameEngine?.let { engine ->
            engine.jump()
            soundEffects.playJumpSound(progress.value.soundEnabled)
        }
    }

    fun onDuckPressed(isDucking: Boolean) {
        gameEngine?.isDucking = isDucking
    }

    fun toggleSound() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleSound(!progress.value.soundEnabled)
        }
    }

    fun toggleVibration() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleVibration(!progress.value.vibrationEnabled)
        }
    }

    private fun startGameLoop() {
        stopGameLoop()
        gameLoopJob = viewModelScope.launch(Dispatchers.Default) {
            val engine = gameEngine ?: return@launch
            var frameCounter = 0

            while (_screenState.value == ScreenState.PLAYING) {
                val event = engine.updateGameStep()

                // Trigger sounds and haptics
                val soundOn = progress.value.soundEnabled
                val vibOn = progress.value.vibrationEnabled

                when (event) {
                    GameEvent.PICKUP_BOTTLE -> {
                        soundEffects.playPickupBottleSound(soundOn)
                        soundEffects.triggerVibration(vibOn, 30)
                    }
                    GameEvent.PICKUP_CARDBOARD -> {
                        soundEffects.playPickupCardboardSound(soundOn)
                        soundEffects.triggerVibration(vibOn, 40)
                    }
                    GameEvent.PICKUP_SUPER_JUMP, GameEvent.PICKUP_LEAF_SHIELD, GameEvent.PICKUP_SPEED_BOOTS, GameEvent.PICKUP_STAR -> {
                        soundEffects.playVictorySound(soundOn)
                        soundEffects.triggerVibration(vibOn, 60)
                    }
                    GameEvent.SHIELD_BROKEN -> {
                        soundEffects.playCrashSound(soundOn)
                        soundEffects.triggerVibration(vibOn, 100)
                    }
                    GameEvent.CAR_CRASH -> {
                        soundEffects.playCrashSound(soundOn)
                        soundEffects.triggerVibration(vibOn, 120)
                    }
                    GameEvent.GAME_OVER -> {
                        soundEffects.playCrashSound(soundOn)
                        soundEffects.triggerVibration(vibOn, 200)
                        saveGameResult(engine)
                        _screenState.value = ScreenState.GAME_OVER
                        break
                    }
                    GameEvent.VICTORY -> {
                        soundEffects.playVictorySound(soundOn)
                        soundEffects.triggerVibration(vibOn, 250)
                        saveGameResult(engine, levelCompleted = engine.level.id)
                        _screenState.value = ScreenState.VICTORY
                        break
                    }
                    GameEvent.NONE -> {}
                }

                // Update HUD state every ~4 frames or on event to avoid UI recomposition overload
                frameCounter++
                if (frameCounter % 4 == 0 || event != GameEvent.NONE) {
                    _hudState.value = HudData(
                        score = engine.score,
                        bottles = engine.bottlesCollected,
                        cardboard = engine.cardboardCollected,
                        lives = engine.lives,
                        distance = engine.distanceTraveled,
                        targetDistance = engine.level.targetDistance,
                        hasShield = engine.leafShieldActive,
                        superJumpTimer = engine.superJumpFrames,
                        speedBootsTimer = engine.speedBootsFrames
                    )
                }

                delay(16) // Approx 60 FPS tick
            }
        }
    }

    private fun stopGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = null
    }

    private fun saveGameResult(engine: GameEngine, levelCompleted: Int? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.recordGameResult(
                score = engine.score,
                bottlesCollected = engine.bottlesCollected,
                cardboardCollected = engine.cardboardCollected,
                levelCompleted = levelCompleted
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopGameLoop()
    }
}

data class HudData(
    val score: Int = 0,
    val bottles: Int = 0,
    val cardboard: Int = 0,
    val lives: Int = 3,
    val distance: Float = 0f,
    val targetDistance: Float = 1000f,
    val hasShield: Boolean = false,
    val superJumpTimer: Int = 0,
    val speedBootsTimer: Int = 0
)
