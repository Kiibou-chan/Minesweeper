package space.kiibou.server

import space.kiibou.common.*
import space.kiibou.data.Vec2
import space.kiibou.game.TileType
import space.kiibou.net.common.ConnectionHandle
import kotlin.random.Random

class GameState(
    val handles: MutableList<ConnectionHandle>,
    private var width: Int,
    private var height: Int,
    private var bombs: Int,
    private val events: GameEvents,
    private val ticker: Ticker,
    private val random: Random = Random.Default,
) {
    private lateinit var revealed: Array<BooleanArray>
    private lateinit var flagged: Array<BooleanArray>
    private var bombsLeft = 0
    private lateinit var tiles: Array<Array<TileType>>
    private var bombTiles: List<Vec2> = emptyList()
    private var bombsPlaced = false
    private var gameRunning = false
    private var revealedTiles = 0
    private var time = 0

    init {
        setupVariables()
    }

    fun reset() {
        setupVariables()
        events.restart()
    }

    fun reset(width: Int, height: Int, bombs: Int) {
        setupVariables(width, height, bombs)
        events.restart()
    }

    fun setupVariables() = setupVariables(width, height, bombs)

    fun setupVariables(width: Int, height: Int, bombs: Int) {
        this.width = width
        this.height = height
        this.bombs = bombs

        setBombsLeft(bombs)

        if (gameRunning) {
            stopTimer()
        }

        resetTimer()

        revealed = Array(width) { BooleanArray(height) { false } }
        flagged = Array(width) { BooleanArray(height) { false } }
        tiles = Array(width) { Array(height) { TileType.EMPTY } }

        // Bombs are placed on the first reveal so the first click is always safe.
        bombTiles = emptyList()
        bombsPlaced = false

        gameRunning = false
        revealedTiles = 0
    }

    private fun possibleTilePositions(x: Int = 0, y: Int = 0, width: Int = this.width, height: Int = this.height) =
        List(width * height) { Vec2(x + it % width, y + it / width) }

    /**
     * Places bombs on the first reveal, keeping the clicked tile (and, when the board has room,
     * its eight neighbours) bomb-free so the first click never loses and opens a pocket. If the
     * board is too dense to exclude the full 3x3, only the clicked tile is excluded.
     */
    private fun placeBombsAvoiding(cx: Int, cy: Int) {
        val fullExclusion = possibleTilePositions(cx - 1, cy - 1, 3, 3)
            .filter { (px, py) -> isValidTile(px, py) }
            .toSet()

        val exclusion = if (bombs <= width * height - fullExclusion.size) {
            fullExclusion
        } else {
            setOf(Vec2(cx, cy))
        }

        bombTiles = possibleTilePositions()
            .filterNot { it in exclusion }
            .shuffled(random)
            .take(bombs)
            .onEach { (x, y) -> setTile(x, y, TileType.BOMB) }

        createNumberTiles()

        bombsPlaced = true
    }

    private fun createNumberTiles() = possibleTilePositions()
        .filterNot { (x, y) -> isBomb(x, y) }
        .associateWith { (x, y) -> countSurroundingBombs(x, y) }
        .forEach { (pos, count) -> setTile(pos.x, pos.y, TileType.getTypeFromValue(count)) }

    private fun countSurroundingBombs(x: Int, y: Int) = possibleTilePositions(-1, -1, 3, 3)
        .count { (px, py) -> isValidTile(x + px, y + py) && isBomb(x + px, y + py) }

    fun revealAt(x: Int, y: Int) {
        events.revealTiles(reveal(x, y))
    }

    private fun reveal(x: Int, y: Int): List<TileInfo> {
        if (!gameRunning) setGameRunning(true)
        val revealed: MutableList<TileInfo> = ArrayList()

        if (isValidTile(x, y)) {
            if (!bombsPlaced) placeBombsAvoiding(x, y)

            when (getTile(x, y)) {
                TileType.EMPTY -> possibleTilePositions(x - 1, y - 1, 3, 3)
                    .filter { (tx, ty) -> revealTile(tx, ty, revealed) }
                    .forEach { (tx, ty) -> revealed += reveal(tx, ty) }

                TileType.BOMB -> {
                    setTile(x, y, TileType.RED_BOMB)
                    setGameRunning(false)

                    revealed += bombTiles.filterNot { it == Vec2(x, y) }
                        .map { (x, y) -> TileInfo(x, y, TileType.BOMB) }

                    for (flagX in 0 until width) {
                        for (flagY in 0 until height) {
                            // The detonated tile is a bomb, so !isBomb already excludes it; every
                            // other wrongly-flagged (flagged but safe) tile must be shown, whatever
                            // its row or column.
                            if (isFlagged(flagX, flagY) && !isBomb(flagX, flagY)) {
                                revealed.add(TileInfo(flagX, flagY, TileType.NO_BOMB))
                            }
                        }
                    }

                    revealTile(x, y, revealed)

                    events.lose()

                    setGameRunning(false)
                }

                else ->
                    if (isNotRevealed(x, y)) {
                        revealTile(x, y, revealed)
                    } else {
                        chord(x, y, revealed)
                    }
            }
        }

        if (revealedTiles == width * height - bombs && gameRunning) {
            events.win()

            bombTiles.filter { (x, y) -> !isFlagged(x, y) }
                .forEach { (x, y) -> flagToggle(x, y) }

            setGameRunning(false)
        }

        return revealed
    }

    /**
     * Chording: clicking an already-revealed number tile whose flagged-neighbor count
     * matches its number reveals every unflagged hidden neighbor. A wrong flag means an
     * unflagged neighbor is a bomb — revealing it detonates, as in classic Minesweeper.
     */
    private fun chord(x: Int, y: Int, revealed: MutableList<TileInfo>) {
        val number = getTile(x, y).lookup
        if (number !in 1..8) return

        val neighbors = possibleTilePositions(x - 1, y - 1, 3, 3)
            .filter { (nx, ny) -> isValidTile(nx, ny) && !(nx == x && ny == y) }

        if (neighbors.count { (nx, ny) -> isFlagged(nx, ny) } != number) return

        for ((nx, ny) in neighbors) {
            if (isFlagged(nx, ny) || !isNotRevealed(nx, ny)) continue
            revealed += reveal(nx, ny)
            // A detonation (or win) stops the game mid-chord; do not keep revealing.
            if (!gameRunning) break
        }
    }

    private fun revealTile(x: Int, y: Int, revealed: MutableList<TileInfo>): Boolean {
        var empty = false

        if (isValidTile(x, y) && isNotRevealed(x, y)) {
            empty = getTile(x, y) == TileType.EMPTY
            revealed.add(TileInfo(x, y, getTile(x, y)))
            setRevealed(x, y)
            revealedTiles++
        }

        return empty
    }

    private fun getTile(x: Int, y: Int) = tiles[x][y]

    private fun setTile(x: Int, y: Int, tileType: TileType) {
        tiles[x][y] = tileType
    }

    private fun isNotRevealed(x: Int, y: Int) = !revealed[x][y]

    private fun setRevealed(x: Int, y: Int) {
        if (isFlagged(x, y)) {
            flagToggle(x, y)
        }

        revealed[x][y] = true
    }

    private fun isFlagged(x: Int, y: Int) = flagged[x][y]
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

        events.setFlag(x, y, flagged[x][y])

        if (flagged[x][y]) {
            setBombsLeft(bombsLeft - 1)
        } else {
            setBombsLeft(bombsLeft + 1)
        }

        return flagged[x][y]
    }

    private fun setBombsLeft(left: Int) {
        bombsLeft = left
        events.setBombsLeft(left)
    }

    private fun startTimer() {
        ticker.start {
            events.setTime(time)
            time++
        }
    }

    private fun stopTimer() {
        ticker.stop()
    }

    private fun resetTimer() {
        time = 0
        events.setTime(time)
    }

    fun stopGame() = setGameRunning(false)

    /** Test-only view of where bombs were placed (empty until the first reveal). */
    internal val bombPositions: List<Vec2> get() = bombTiles

    fun addPlayer(handle: ConnectionHandle) {
        handles += handle
    }

    fun removePlayer(handle: ConnectionHandle) {
        handles -= handle

        if (handles.isEmpty()) stopGame()
    }

}
