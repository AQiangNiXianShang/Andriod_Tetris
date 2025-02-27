package com.android_tetris.logic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android_tetris.infoStorage
import com.android_tetris.utils.SoundType
import com.android_tetris.utils.SoundUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TetrisViewModel : ViewModel() {

    companion object {
        private var DOWN_SPEED = 500L
        private const val CLEAR_SCREEN_SPEED = 30L
    }

    private val _tetrisStateLD: MutableStateFlow<TetrisState> = MutableStateFlow(TetrisState())

    val tetrisStateLD = _tetrisStateLD.asStateFlow()

    private val tetrisState: TetrisState
        get() = _tetrisStateLD.value

    private var downJob: Job? = null

    private var clearScreenJob: Job? = null

    fun dispatch(action: Action) {
        playSound(action)
        val unit = when (action) {
            Action.Welcome, Action.Reset -> {
                onWelcome()
            }
            Action.Start -> {
                if (!tetrisState.canStartGame) {
                    return
                }
                if (tetrisState.gameStatus == GameStatus.Paused) {
                    dispatchState(tetrisState.copy(gameStatus = GameStatus.Running))
                    startDownJob()
                }
                else {
                    onStartGame()
                }
            }
            Action.Background, Action.Pause -> {
                onPauseGame()
            }
            Action.Resume -> {

            }
            Action.Sound -> {
                dispatchState(tetrisState.copy(soundEnable = !tetrisState.soundEnable))
            }
            Action.Settings -> {
                infoStorage.currentScreen = 1
            }
            is Action.Transformation -> {
                if (!tetrisState.isRunning) {
                    return
                }
                val viewState =
                    tetrisState.onTransformation(transformationType = action.transformationType)
                        ?: return
                when (viewState.gameStatus) {
                    GameStatus.Running -> {
                        dispatchState(viewState)
                        val unit = when (action.transformationType) {
                            TransformationType.Left, TransformationType.Right -> {

                            }
                            TransformationType.FastDown -> {
                                startDownJob()
                            }
                            TransformationType.Fall -> {

                            }
                            TransformationType.Down -> {
                                startDownJob()
                            }
                            TransformationType.Rotate -> {

                            }
                        }
                    }
                    GameStatus.LineClearing -> {
                        playSound(SoundType.Clean)
                        dispatchState(viewState.copy(gameStatus = GameStatus.Running))
                        startDownJob()
                    }
                    GameStatus.GameOver -> {
                        dispatchState(viewState)
                        onGameOver()
                    }
                    else -> {
                        Unit
                    }
                }
            }
        }
    }

    fun changeDownSpeed(newSpeed :Long) {
        DOWN_SPEED = newSpeed
    }

    private fun onWelcome() {
        startClearScreenJob {
            // 每次重新开始游戏时，清零得分
            infoStorage.currentScore = 0

            dispatchState(
                TetrisState().copy(gameStatus = GameStatus.Welcome)
            )
        }
    }

    private fun onStartGame() {
        dispatchState(
            TetrisState().copy(gameStatus = GameStatus.Running)
        )
        startDownJob()
    }

    private fun onPauseGame() {
        if (tetrisState.isRunning) {
            cancelDownJob()
            dispatchState(
                tetrisState.copy(gameStatus = GameStatus.Paused)
            )
        }
    }

    private fun onGameOver() {
        startClearScreenJob {
            dispatchState(
                TetrisState().copy(gameStatus = GameStatus.GameOver)
            )
        }
    }

    private fun startClearScreenJob(invokeOnCompletion: () -> Unit) {
        cancelDownJob()
        cancelClearScreenJob()
        clearScreenJob = viewModelScope.launch {
            playSound(Action.Welcome)
            val width = tetrisState.width
            val height = tetrisState.height
            for (y in height - 1 downTo 0) {
                val brickArray = tetrisState.brickArray
                for (x in 0 until width) {
                    brickArray[y][x] = 1
                }
                dispatchState(
                    tetrisState.copy(
                        gameStatus = GameStatus.ScreenClearing,
                        tetris = Tetris()
                    )
                )
                delay(CLEAR_SCREEN_SPEED)
            }
            for (y in 0 until height) {
                val brickArray = tetrisState.brickArray
                for (x in 0 until width) {
                    brickArray[y][x] = 0
                }
                dispatchState(
                    tetrisState.copy(
                        gameStatus = GameStatus.ScreenClearing,
                        tetris = Tetris()
                    )
                )
                delay(CLEAR_SCREEN_SPEED)
            }
            delay(100)
        }.apply {
            invokeOnCompletion {
                if (it == null) {
                    invokeOnCompletion()
                }
            }
        }
    }

    private fun cancelClearScreenJob() {
        clearScreenJob?.cancel()
        clearScreenJob = null
    }

    private fun startDownJob() {
        cancelDownJob()
        cancelClearScreenJob()
        downJob = viewModelScope.launch {
            delay(DOWN_SPEED)
            dispatch(Action.Transformation(TransformationType.Down))
        }
    }

    private fun cancelDownJob() {
        downJob?.cancel()
        downJob = null
    }

    private fun dispatchState(tetrisState: TetrisState) {
        _tetrisStateLD.value = tetrisState
    }

    private fun playSound(action: Action) {
        val unit = when (action) {
            Action.Welcome -> {
                playSound(SoundType.Welcome)
            }
            Action.Start, Action.Pause -> {
                playSound(SoundType.Transformation)
            }
            Action.Reset -> {

            }
            Action.Background -> {

            }
            Action.Resume -> {

            }
            Action.Sound -> {
                playSound(SoundType.Transformation)
            }
            Action.Settings -> {

            }
            is Action.Transformation -> {
                when (action.transformationType) {
                    TransformationType.Left, TransformationType.Right, TransformationType.FastDown -> {
                        playSound(SoundType.Transformation)
                    }
                    TransformationType.Fall -> {
                        playSound(SoundType.Fall)
                    }
                    TransformationType.Down -> {

                    }
                    TransformationType.Rotate -> {
                        playSound(SoundType.Rotate)
                    }
                }
            }
        }
    }

    private fun playSound(soundType: SoundType) {
        tetrisState.soundEnable.takeIf { it } ?: return
        SoundUtil.play(soundType)
    }

}