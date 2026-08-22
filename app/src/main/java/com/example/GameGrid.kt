package com.example

import kotlin.math.abs
import kotlin.random.Random

data class Tile(
    var isDog: Boolean = false, // true if mine/dog
    var isRevealed: Boolean = false,
    var isFlagged: Boolean = false,
    var barkVolume: Int = 0 // number of neighbor mines (0-8)
)

class GameGrid(val rows: Int, val cols: Int, val totalDogs: Int) {
    val board: Array<Array<Tile>> = Array(rows) { Array(cols) { Tile() } }
    var isGenerated: Boolean = false

    fun generateMines(firstRow: Int, firstCol: Int) {
        if (isGenerated) return
        var dogsPlaced = 0
        val random = Random.Default

        while (dogsPlaced < totalDogs) {
            val randomRow = random.nextInt(rows)
            val randomCol = random.nextInt(cols)

            // ZONA AMAN 3x3: abs(row - firstRow) <= 1 && abs(col - firstCol) <= 1
            val inSafeZone = abs(randomRow - firstRow) <= 1 && abs(randomCol - firstCol) <= 1

            if (!board[randomRow][randomCol].isDog && !inSafeZone) {
                board[randomRow][randomCol].isDog = true
                dogsPlaced++
            }
        }

        calculateBarkVolumes()
        isGenerated = true
    }

    private fun calculateBarkVolumes() {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (board[r][c].isDog) continue
                var count = 0
                for (dr in -1..1) {
                    for (dc in -1..1) {
                        val nr = r + dr
                        val nc = c + dc
                        if (nr in 0 until rows && nc in 0 until cols) {
                            if (board[nr][nc].isDog) {
                                count++
                            }
                        }
                    }
                }
                board[r][c].barkVolume = count
            }
        }
    }
}
