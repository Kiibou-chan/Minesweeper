package space.kiibou.server

import space.kiibou.game.TileType
import space.kiibou.net.common.ConnectionHandle
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameStateTest {

    private fun newGame(
        w: Int = 9,
        h: Int = 9,
        bombs: Int = 10,
        seed: Long = 1L,
    ): Pair<GameState, RecordingGameEvents> {
        val events = RecordingGameEvents()
        val game = GameState(mutableListOf(), w, h, bombs, events, ManualTicker(), Random(seed))
        return game to events
    }

    @Test
    fun reset_emits_restart_and_bombs_left() {
        val (game, events) = newGame(bombs = 10)
        game.reset()
        assertTrue(events.restarts >= 1)
        assertEquals(10, events.bombsLeft.last())
    }

    @Test
    fun non_square_board_positions_are_valid() {
        val (game, events) = newGame(w = 5, h = 3, bombs = 4)
        game.revealAt(0, 0)
        val revealed = events.reveals.flatten()
        assertTrue(revealed.all { it.x in 0 until 5 && it.y in 0 until 3 }, "all revealed tiles in bounds")
        assertEquals(revealed.size, revealed.map { it.x to it.y }.toSet().size, "no duplicate positions")
    }

    @Test
    fun first_click_is_never_a_bomb_and_opens_a_pocket() {
        // Dense enough that, without first-click safety, (4,4) is almost never a clear pocket.
        val (game, events) = newGame(w = 9, h = 9, bombs = 35, seed = 42L)
        game.revealAt(4, 4)
        val revealed = events.lastReveal()
        assertTrue(revealed.any { it.x == 4 && it.y == 4 }, "clicked tile is revealed")
        assertTrue(revealed.none { it.type == TileType.RED_BOMB }, "first click never detonates")
        assertEquals(0, events.loses, "no loss on first click")
        assertTrue(revealed.size > 1, "first click opens a pocket, not a single number tile")
    }

    @Test
    fun dense_board_falls_back_to_tile_only_exclusion() {
        // 3x3 with 8 bombs: full 3x3 exclusion cannot fit, must shrink to the clicked tile.
        val (game, events) = newGame(w = 3, h = 3, bombs = 8, seed = 7L)
        game.revealAt(1, 1)
        assertEquals(0, events.loses, "clicked tile is safe even on a maximally dense board")
    }
}
