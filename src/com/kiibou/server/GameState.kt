package com.kiibou.server

import com.kiibou.TileInfo
import com.kiibou.TileType
import space.kiibou.data.Vec2
import java.util.*
import kotlin.streams.toList

class GameState(private val handle: Long, width: Int, height: Int, bombs: Int, private val gameService: GameService) {
    private var width = 0
    private var height = 0
    private var bombs = 0
    private lateinit var revealed: Array<BooleanArray>
    private lateinit var flagged: Array<BooleanArray>
    private lateinit var tiles: Array<Array<TileType>>
    private lateinit var bombTiles: Array<Vec2>
    private var gameRunning = false
    private var revealedTiles = 0
    private val timer = Timer("Timer", true)
    private lateinit var timerTask: TimerTask
    private var time = 0

    fun setupVariables() = setupVariables(width, height, bombs)

    fun setupVariables(width: Int, height: Int, bombs: Int) {
        this.width = width
        this.height = height
        this.bombs = bombs

        if (gameRunning) {
            stopTimer()
        }

        resetTimer()

        revealed = Array(width) { BooleanArray(height) { false } }
        flagged = Array(width) { BooleanArray(height) { false } }
        tiles = Array(width) { Array(height) { TileType.EMPTY } }

        // Find out where to put bombs
        bombTiles = placeBombs()

        // Set tiles around bombs to the correct number
        createNumberTiles()

        gameRunning = false
        revealedTiles = 0
    }

    private fun possibleTilePositions(x: Int = 0, y: Int = 0, width: Int = this.width, height: Int = this.height) =
            List(width * height) { Vec2(x + it % width, y + it / height) }

    private fun placeBombs() = chooseBombPositions().run(::setTilesToBombs)
    private fun chooseBombPositions() = possibleTilePositions().shuffled().take(bombs)
    private fun setTilesToBombs(list: List<Vec2>) = list.onEach { (x, y) -> setTile(x, y, TileType.BOMB) }

    private fun createNumberTiles() = possibleTilePositions()
            .filterNot { (x, y) -> isBomb(x, y) }
            .associateWith { (x, y) -> countSurroundingBombs(x, y) }
            .forEach { (pos, count) -> setTile(pos.x, pos.y, TileType.getTypeFromValue(count)) }

    private fun countSurroundingBombs(x: Int, y: Int) = possibleTilePositions(-1, -1, 3, 3)
            .filter { (px, py) -> isValidTile(x + px, y + py) && isBomb(x + px, y + py) }.count()

    fun reveal(x: Int, y: Int): List<TileInfo> {
        if (!gameRunning) setGameRunning(true)
        val revealed: MutableList<TileInfo> = ArrayList()

        if (isValidTile(x, y)) {
            when (getTile(x, y)) {
                TileType.EMPTY -> possibleTilePositions(x - 1, y - 1, 3, 3)
                        .filter { (tx, ty) -> revealTile(tx, ty, revealed) }
                        .forEach { (tx, ty) -> revealed += reveal(tx, ty) }
                TileType.BOMB -> {
                    setTile(x, y, TileType.RED_BOMB)
                    setGameRunning(false)

                    revealed += bombTiles.filterNot { it == Vec2(x, y) }
                            .map { (x, y) -> TileInfo(x, y, TileType.BOMB.lookup) }

                    revealTile(x, y, revealed)
                    gameService.sendLoose(handle)
                    setGameRunning(false)
                }
                else -> revealTile(x, y, revealed)
            }
        }

        if (revealedTiles == width * height - bombs && gameRunning) {
            gameService.sendWin(handle)

            bombTiles.filter { (x, y) -> !isFlagged(x, y) }
                    .onEach { (x, y) -> flagToggle(x, y) }
                    .forEach { (x, y) -> gameService.sendFlagStatus(handle, x, y) }

            setGameRunning(false)
        }

        return revealed
    }

    private fun revealTile(x: Int, y: Int, revealed: MutableList<TileInfo>): Boolean {
        var empty = false

        if (isValidTile(x, y) && isNotRevealed(x, y)) {
            empty = getTile(x, y) == TileType.EMPTY
            revealed.add(TileInfo(x, y, getTile(x, y).lookup))
            setRevealed(x, y, true)
            revealedTiles++
        }

        return empty
    }

    private fun getTile(x: Int, y: Int) = tiles[x][y]

    private fun setTile(x: Int, y: Int, tileType: TileType) {
        tiles[x][y] = tileType
    }

    private fun isNotRevealed(x: Int, y: Int) = !revealed[x][y]

    private fun setRevealed(x: Int, y: Int, r: Boolean) {
        if (isFlagged(x, y)) {
            flagToggle(x, y)
            gameService.sendFlagStatus(handle, x, y)
        }
        revealed[x][y] = r
    }

    fun isFlagged(x: Int, y: Int) = flagged[x][y]
    private fun isBomb(x: Int, y: Int) = getTile(x, y) == TileType.BOMB
    private fun isValidTile(x: Int, y: Int) = x in 0 until width && y >= 0 && y < height

    private fun setGameRunning(running: Boolean) {
        when {
            !gameRunning && running -> {
                resetTimer()
                startTimer()
            }
            gameRunning && !running -> {
                stopTimer()
            }
        }
        gameRunning = running
    }

    fun flagToggle(x: Int, y: Int): Boolean {
        if (!gameRunning) setGameRunning(true)
        if (isNotRevealed(x, y)) {
            flagged[x][y] = !flagged[x][y]
        }
        return flagged[x][y]
    }

    private fun startTimer() {
        timerTask = object : TimerTask() {
            override fun run() {
                sendTimeToClients()
                time++
            }
        }
        timer.schedule(timerTask, 0, 1000)
    }

    private fun stopTimer() = timerTask.cancel()

    private fun resetTimer() {
        time = 0
        sendTimeToClients()
    }

    private fun sendTimeToClients() = gameService.sendTime(handle, time)

    init {
        setupVariables(width, height, bombs)
    }
}