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
}
