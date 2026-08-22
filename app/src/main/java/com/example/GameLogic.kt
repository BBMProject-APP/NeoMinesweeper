package com.example

enum class GameStatus {
    PLAYING, WON, GAMEOVER
}

class GameLogic(
    val grid: GameGrid,
    private val onWin: () -> Unit,
    private val onGameOver: () -> Unit
) {
    var status = GameStatus.PLAYING
    var remainingFlags = grid.totalDogs

    fun revealTile(row: Int, col: Int) {
        if (status != GameStatus.PLAYING) return

        var tile = grid.board[row][col]
        if (tile.isRevealed || tile.isFlagged) return

        // Klik pertama sebar ranjau
        if (!grid.isGenerated) {
            grid.generateMines(row, col)
            tile = grid.board[row][col]
        }

        // 1. KALAH: Kena bom
        if (tile.isDog) {
            tile.isRevealed = true
            status = GameStatus.GAMEOVER

            // Buka kedok semua bom yang tersembunyi agar terlihat di layar
            for (r in 0 until grid.rows) {
                for (c in 0 until grid.cols) {
                    val bomSembunyi = grid.board[r][c]
                    if (bomSembunyi.isDog) {
                        bomSembunyi.isRevealed = true
                    }
                }
            }

            onGameOver()
            return
        }

        // 2. AMAN: Buka ubin
        tile.isRevealed = true

        // 3. Efek Berantai (Cascade Reveal)
        if (tile.barkVolume == 0) {
            revealNeighbors(row, col)
        }

        // 4. CEK KEMENANGAN BARU:
        if (checkWinCondition()) {
            status = GameStatus.WON
            autoFlagAllMines() // Otomatis pasang bendera di sisa bom jika ada
            onWin()
        }
    }

    fun checkWinCondition(): Boolean {
        for (r in 0 until grid.rows) {
            for (c in 0 until grid.cols) {
                val tile = grid.board[r][c]
                // Jika ada ubin yang BUKAN bom tapi BELUM dibuka, berarti belum menang
                if (!tile.isDog && !tile.isRevealed) {
                    return false
                }
            }
        }
        return true // Semua ubin aman sudah terbuka!
    }

    fun autoFlagAllMines() {
        remainingFlags = 0
        for (r in 0 until grid.rows) {
            for (c in 0 until grid.cols) {
                val tile = grid.board[r][c]
                if (tile.isDog) {
                    tile.isFlagged = true
                }
            }
        }
    }

    fun revealNeighbors(row: Int, col: Int) {
        for (dr in -1..1) {
            for (dc in -1..1) {
                val r = row + dr
                val c = col + dc

                if (r in 0 until grid.rows && c in 0 until grid.cols) {
                    val neighbor = grid.board[r][c]
                    if (!neighbor.isRevealed && !neighbor.isDog && !neighbor.isFlagged) {
                        revealTile(r, c)
                    }
                }
            }
        }
    }

    fun toggleFlag(row: Int, col: Int): Boolean {
        if (status != GameStatus.PLAYING) return false

        val tile = grid.board[row][col]
        if (tile.isRevealed) return false

        if (tile.isFlagged) {
            // Lepas bendera -> Stok bendera kembali bertambah
            tile.isFlagged = false
            remainingFlags++
            return true
        } else {
            // Pasang bendera -> Hanya bisa jika stok bendera masih ada (di atas 0)
            if (remainingFlags > 0) {
                tile.isFlagged = true
                remainingFlags--
                return true
            }
        }
        return false
    }
}
