package space.kiibou.server

import space.kiibou.common.GameHandle
import space.kiibou.net.common.ConnectionHandle
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GameRegistryTest {

    private fun registry() = GameRegistry {
        GameState(mutableListOf(), 9, 9, 10, RecordingGameEvents(), ManualTicker(), Random(1L))
    }

    private fun handle(id: Long) = ConnectionHandle(id)

    @Test
    fun last_player_leaving_removes_the_game() {
        val r = registry()
        val h = handle(1)
        r.join(h, GameHandle(0))
        assertEquals(1, r.activeGameCount())
        r.leave(h)
        assertEquals(0, r.activeGameCount())
    }

    @Test
    fun game_survives_while_one_player_remains() {
        val r = registry()
        val a = handle(1)
        val b = handle(2)
        r.join(a, GameHandle(0))
        r.join(b, GameHandle(0))
        r.leave(a)
        assertEquals(1, r.activeGameCount())
    }

    @Test
    fun lookup_or_leave_for_unknown_handle_is_safe() {
        val r = registry()
        assertNull(r.gameFor(handle(99)))
        r.leave(handle(99)) // must not throw
        assertEquals(0, r.activeGameCount())
    }
}
