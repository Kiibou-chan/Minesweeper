package space.kiibou.server

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
}
