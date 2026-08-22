package com.example

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class Difficulty(val rows: Int, val cols: Int, val mines: Int, val fontSizeSp: Int) {
    BEGINNER(11, 8, 12, 16),
    INTERMEDIATE(16, 10, 28, 14),
    EXPERT(22, 12, 50, 12)
}

class MinesweeperViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("neo_mines_prefs", Context.MODE_PRIVATE)

    var currentDifficulty by mutableStateOf(Difficulty.BEGINNER)
        private set

    var lives by mutableStateOf(5)
        private set

    var wins by mutableStateOf(0)
        private set

    var loses by mutableStateOf(0)
        private set

    var gameGrid by mutableStateOf(GameGrid(Difficulty.BEGINNER.rows, Difficulty.BEGINNER.cols, Difficulty.BEGINNER.mines))
        private set

    var gameLogic by mutableStateOf(GameLogic(gameGrid, {}, {}))
        private set

    // Timer state
    var timeElapsed by mutableStateOf(0)
        private set

    private var timerJob: Job? = null

    // Room Database and Repository
    private val database = ScoreDatabase.getDatabase(application)
    private val repository = ScoreRepository(database.completionTimeDao())

    var topTimesForCurrentDifficulty by mutableStateOf<List<CompletionTime>>(emptyList())
        private set

    private var scoreJob: Job? = null

    private fun observeTopTimes() {
        scoreJob?.cancel()
        scoreJob = viewModelScope.launch {
            repository.getTopTimesForDifficulty(currentDifficulty.name).collect { times ->
                topTimesForCurrentDifficulty = times
            }
        }
    }

    fun clearScores() {
        viewModelScope.launch {
            repository.clear()
        }
    }

    // Modal/Dialog states
    var showLoseWithLivesModal by mutableStateOf(false)
    var showOutOfLivesModal by mutableStateOf(false)
    var showWinModal by mutableStateOf(false)
    var showHowToModal by mutableStateOf(false)
    var showSaveConfirmationModal by mutableStateOf(false)

    // UI Toast or status messages
    var toastMessage by mutableStateOf<String?>(null)

    init {
        // Load persisted values
        lives = prefs.getInt("neo_mines_lives", 5)
        if (lives <= 0) {
            lives = 5 // fallback jika 0 saat boot awal
        }
        wins = prefs.getInt("neo_mines_win", 0)
        loses = prefs.getInt("neo_mines_lose", 0)

        // Load saved game atau start beginner
        if (hasSavedGame()) {
            loadSavedGame()
        } else {
            startNewGame(Difficulty.BEGINNER)
        }
        observeTopTimes()
    }

    fun startTimer() {
        stopTimer()
        timerJob = viewModelScope.launch {
            while (isActive && gameLogic.status == GameStatus.PLAYING) {
                delay(1000L)
                timeElapsed++
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    fun startNewGame(difficulty: Difficulty) {
        stopTimer()
        clearSavedGame()
        timeElapsed = 0
        currentDifficulty = difficulty
        gameGrid = GameGrid(difficulty.rows, difficulty.cols, difficulty.mines)

        val onWinCallback = {
            wins++
            prefs.edit().putInt("neo_mines_win", wins).apply()
            showWinModal = true
            stopTimer()
            clearSavedGame()
            viewModelScope.launch {
                repository.insert(CompletionTime(difficulty = currentDifficulty.name, timeSeconds = timeElapsed))
            }
            Unit
        }

        val onGameOverCallback = {
            loses++
            prefs.edit().putInt("neo_mines_lose", loses).apply()
            stopTimer()
            clearSavedGame()

            if (lives > 0) {
                lives--
                prefs.edit().putInt("neo_mines_lives", lives).apply()
                if (lives <= 0) {
                    showOutOfLivesModal = true
                } else {
                    showLoseWithLivesModal = true
                }
            } else {
                showOutOfLivesModal = true
            }
        }

        gameLogic = GameLogic(
            grid = gameGrid,
            onWin = onWinCallback,
            onGameOver = onGameOverCallback
        )

        showLoseWithLivesModal = false
        showOutOfLivesModal = false
        showWinModal = false
    }

    fun revealTile(row: Int, col: Int) {
        if (lives <= 0 && gameLogic.status != GameStatus.WON) {
            showOutOfLivesModal = true
            return
        }
        val wasGenerated = gameGrid.isGenerated
        gameLogic.revealTile(row, col)

        if (!wasGenerated && gameGrid.isGenerated) {
            startTimer()
        }

        if (gameLogic.status != GameStatus.PLAYING) {
            stopTimer()
        }

        forceStateRefresh()
    }

    fun toggleFlag(row: Int, col: Int): Boolean {
        val changed = gameLogic.toggleFlag(row, col)
        if (changed) {
            forceStateRefresh()
        }
        return changed
    }

    private fun forceStateRefresh() {
        val oldLogic = gameLogic
        gameLogic = GameLogic(
            grid = oldLogic.grid,
            onWin = {
                wins++
                prefs.edit().putInt("neo_mines_win", wins).apply()
                showWinModal = true
                stopTimer()
                clearSavedGame()
                viewModelScope.launch {
                    repository.insert(CompletionTime(difficulty = currentDifficulty.name, timeSeconds = timeElapsed))
                }
                Unit
            },
            onGameOver = {
                loses++
                prefs.edit().putInt("neo_mines_lose", loses).apply()
                stopTimer()
                clearSavedGame()

                if (lives > 0) {
                    lives--
                    prefs.edit().putInt("neo_mines_lives", lives).apply()
                    if (lives <= 0) {
                        showOutOfLivesModal = true
                    } else {
                        showLoseWithLivesModal = true
                    }
                } else {
                    showOutOfLivesModal = true
                }
            }
        ).apply {
            status = oldLogic.status
            remainingFlags = oldLogic.remainingFlags
        }
    }

    fun changeDifficulty(difficulty: Difficulty) {
        startNewGame(difficulty)
        observeTopTimes()
    }

    // 🎁 FUNGSI BARU: Menambah 1 Nyawa dari Tombol (+) Top Bar
    fun addLife() {
        lives++
        prefs.edit().putInt("neo_mines_lives", lives).apply()
        showToast("❤️ +1 Extra Life Claimed!")
    }

    // 🎬 FUNGSI: Refill 5 Nyawa dari Nonton Video Iklan (Out of Lives Dialog)
    fun refillLivesWatchVideo() {
        lives = 5
        prefs.edit().putInt("neo_mines_lives", lives).apply()
        showOutOfLivesModal = false
        showToast("🚀 SUCCESS! +5 Lives Loaded.")
        startNewGame(currentDifficulty)
    }

    // 🎁 FUNGSI: Klaim Free 1 Nyawa (Out of Lives Dialog)
    fun claimFreeLive() {
        lives = 1
        prefs.edit().putInt("neo_mines_lives", lives).apply()
        showOutOfLivesModal = false
        showToast("🎁 BONUS! +1 Live Claimed.")
        startNewGame(currentDifficulty)
    }

    private fun showToast(message: String) {
        toastMessage = message
    }

    fun clearToast() {
        toastMessage = null
    }

    // --- GAME STATE PERSISTENCE ---

    fun hasSavedGame(): Boolean {
        return prefs.getBoolean("has_saved_game", false)
    }

    fun saveGameState() {
        val editor = prefs.edit()
        editor.putString("saved_difficulty", currentDifficulty.name)
        editor.putInt("saved_lives", lives)
        editor.putInt("saved_time_elapsed", timeElapsed)
        editor.putBoolean("saved_is_generated", gameGrid.isGenerated)
        editor.putInt("saved_remaining_flags", gameLogic.remainingFlags)
        editor.putString("saved_status", gameLogic.status.name)

        editor.putInt("saved_rows", gameGrid.rows)
        editor.putInt("saved_cols", gameGrid.cols)
        editor.putInt("saved_total_dogs", gameGrid.totalDogs)

        val sb = StringBuilder()
        for (r in 0 until gameGrid.rows) {
            for (c in 0 until gameGrid.cols) {
                val tile = gameGrid.board[r][c]
                val dog = if (tile.isDog) "1" else "0"
                val rev = if (tile.isRevealed) "1" else "0"
                val flag = if (tile.isFlagged) "1" else "0"
                sb.append("$dog,$rev,$flag,${tile.barkVolume};")
            }
        }
        editor.putString("saved_board_data", sb.toString())
        editor.putBoolean("has_saved_game", true)
        editor.apply()
        showToast("💾 Game progress saved successfully!")
    }

    fun loadSavedGame(): Boolean {
        if (!hasSavedGame()) return false

        val diffName = prefs.getString("saved_difficulty", Difficulty.BEGINNER.name) ?: Difficulty.BEGINNER.name
        val difficulty = try { Difficulty.valueOf(diffName) } catch(e: Exception) { Difficulty.BEGINNER }

        val savedLives = prefs.getInt("saved_lives", lives)
        val savedTime = prefs.getInt("saved_time_elapsed", 0)
        val savedIsGenerated = prefs.getBoolean("saved_is_generated", false)
        val savedRemainingFlags = prefs.getInt("saved_remaining_flags", difficulty.mines)
        val savedStatusName = prefs.getString("saved_status", GameStatus.PLAYING.name) ?: GameStatus.PLAYING.name
        val savedStatus = try { GameStatus.valueOf(savedStatusName) } catch(e: Exception) { GameStatus.PLAYING }

        val rows = prefs.getInt("saved_rows", difficulty.rows)
        val cols = prefs.getInt("saved_cols", difficulty.cols)
        val totalDogs = prefs.getInt("saved_total_dogs", difficulty.mines)

        val boardData = prefs.getString("saved_board_data", null) ?: return false

        stopTimer()
        currentDifficulty = difficulty
        lives = savedLives
        timeElapsed = savedTime

        val newGrid = GameGrid(rows, cols, totalDogs)
        newGrid.isGenerated = savedIsGenerated

        val tileStrings = boardData.split(";").filter { it.isNotEmpty() }
        var index = 0
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (index < tileStrings.size) {
                    val parts = tileStrings[index].split(",")
                    if (parts.size >= 4) {
                        newGrid.board[r][c].isDog = parts[0] == "1"
                        newGrid.board[r][c].isRevealed = parts[1] == "1"
                        newGrid.board[r][c].isFlagged = parts[2] == "1"
                        newGrid.board[r][c].barkVolume = parts[3].toIntOrNull() ?: 0
                    }
                }
                index++
            }
        }

        gameGrid = newGrid

        val onWinCallback = {
            wins++
            prefs.edit().putInt("neo_mines_win", wins).apply()
            showWinModal = true
            stopTimer()
            clearSavedGame()
            viewModelScope.launch {
                repository.insert(CompletionTime(difficulty = currentDifficulty.name, timeSeconds = timeElapsed))
            }
            Unit
        }

        val onGameOverCallback = {
            loses++
            prefs.edit().putInt("neo_mines_lose", loses).apply()
            stopTimer()
            clearSavedGame()

            if (lives > 0) {
                lives--
                prefs.edit().putInt("neo_mines_lives", lives).apply()
                if (lives <= 0) {
                    showOutOfLivesModal = true
                } else {
                    showLoseWithLivesModal = true
                }
            } else {
                showOutOfLivesModal = true
            }
        }

        gameLogic = GameLogic(
            grid = gameGrid,
            onWin = onWinCallback,
            onGameOver = onGameOverCallback
        ).apply {
            status = savedStatus
            remainingFlags = savedRemainingFlags
        }

        if (savedStatus == GameStatus.PLAYING && savedIsGenerated) {
            startTimer()
        }

        showToast("📂 Game progress loaded!")
        return true
    }

    fun clearSavedGame() {
        prefs.edit().remove("has_saved_game").apply()
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}