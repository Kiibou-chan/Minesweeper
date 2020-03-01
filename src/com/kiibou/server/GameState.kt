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

    private fun setupVariables(width: Int, height: Int, bombs: Int) {
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
        bombTiles = (0 until width).flatMap { x ->
            (0 until height).map { y ->
                Vec2(x, y)
            }
        }.shuffled().stream().limit(bombs.toLong()).peek {
            tiles[it.x][it.y] = TileType.BOMB
        }.toList().toTypedArray()

        // Set tiles around bombs to the correct number
        for (x in tiles.indices) {
            for (y in tiles[x].indices) {
                if (isBomb(x, y)) continue
                val count = (-1..1).flatMap { px ->
                    (-1..1).filter { py -> isValidTile(x + px, y + py) && isBomb(x + px, y + py) }
                }.count()
                setTile(x, y, TileType.getTypeFromValue(count))
            }
        }

        gameRunning = false
        revealedTiles = 0
    }

    fun reveal(x: Int, y: Int): List<TileInfo> {
        if (!gameRunning) setGameRunning(true)
        val revealed: MutableList<TileInfo> = ArrayList()

        if (isValidTile(x, y)) {
            if (getTile(x, y) == TileType.EMPTY) {
                for (px in -1..1) {
                    for (py in -1..1) {
                        if (revealTile(x + px, y + py, revealed)) {
                            revealed.addAll(reveal(x + px, y + py))
                        }
                    }
                }
            } else {
                if (isBomb(x, y)) {
                    setTile(x, y, TileType.RED_BOMB)
                    setGameRunning(false)

                    revealed.addAll(
                            bombTiles.filter { !(it.x == x && it.y == y) }
                                    .map { TileInfo(it.x, it.y, TileType.BOMB.lookup) }
                    )

                    gameService.sendLoose(handle)
                    setGameRunning(false)
                }

                revealTile(x, y, revealed)
            }
        }

        if (revealedTiles == width * height - bombs && gameRunning) {
            gameService.sendWin(handle)

            bombTiles.filter { !isFlagged(it.x, it.y) }
                    .forEach { gameService.sendFlagToggle(handle, it.x, it.y) }

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
            gameService.sendFlagToggle(handle, x, y)
        }
        revealed[x][y] = r
    }

    private fun isFlagged(x: Int, y: Int) = flagged[x][y]
    private fun isBomb(x: Int, y: Int) = getTile(x, y) == TileType.BOMB
    private fun isValidTile(x: Int, y: Int) = x in 0 until width && y >= 0 && y < height

    private fun setGameRunning(running: Boolean) {
        if (!gameRunning && running) {
            resetTimer()
            startTimer()
        } else if (gameRunning && !running) {
            stopTimer()
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

    fun flag(x: Int, y: Int) = flagged[x][y]

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