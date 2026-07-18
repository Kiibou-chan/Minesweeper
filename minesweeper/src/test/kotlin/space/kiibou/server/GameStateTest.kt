package space.kiibou.server

import space.kiibou.data.Vec2
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

    // --- chording ---

    private data class ChordTarget(val x: Int, val y: Int, val bombNeighbors: List<Vec2>, val safeHidden: List<Vec2>)

    /** Reveals a starting pocket and finds a revealed number tile with hidden safe neighbors. */
    private fun chordSetup(seed: Long): Triple<GameState, RecordingGameEvents, ChordTarget> {
        val (game, events) = newGame(w = 6, h = 6, bombs = 4, seed = seed)
        game.revealAt(0, 0)
        val bombs = game.bombPositions.toSet()
        val shown = events.reveals.flatten().associateBy { it.x to it.y }

        val target = shown.values.filter { it.type.lookup in 1..8 }.firstNotNullOf { tile ->
            val neighbors = (-1..1).flatMap { dx -> (-1..1).map { dy -> Vec2(tile.x + dx, tile.y + dy) } }
                .filter { (nx, ny) -> nx in 0 until 6 && ny in 0 until 6 && !(nx == tile.x && ny == tile.y) }
            val bombNeighbors = neighbors.filter { it in bombs }
            val safeHidden = neighbors.filter { it !in bombs && (it.x to it.y) !in shown }
            if (safeHidden.isNotEmpty()) ChordTarget(tile.x, tile.y, bombNeighbors, safeHidden) else null
        }

        return Triple(game, events, target)
    }

    @Test
    fun chord_reveals_hidden_neighbors_when_flags_match_the_number() {
        val (game, events, target) = chordSetup(seed = 11L)
        target.bombNeighbors.forEach { (x, y) -> game.flagToggle(x, y) }

        events.reveals.clear()
        game.revealAt(target.x, target.y)

        val newlyShown = events.reveals.flatten().map { it.x to it.y }.toSet()
        target.safeHidden.forEach { (x, y) ->
            assertTrue((x to y) in newlyShown, "chord must reveal hidden safe neighbor ($x,$y)")
        }
        assertEquals(0, events.loses)
    }

    @Test
    fun chord_does_nothing_when_flag_count_does_not_match() {
        val (game, events, target) = chordSetup(seed = 11L)
        // no flags placed at all

        events.reveals.clear()
        game.revealAt(target.x, target.y)

        assertTrue(events.lastReveal().isEmpty(), "chord with wrong flag count must reveal nothing")
    }

    @Test
    fun chord_detonates_when_a_flag_is_wrong() {
        val (game, events, target) = chordSetup(seed = 11L)
        // Flag the right NUMBER of neighbors, but flag a safe one instead of a bomb.
        val wrong = listOf(target.safeHidden.first()) + target.bombNeighbors.drop(1)
        wrong.forEach { (x, y) -> game.flagToggle(x, y) }

        game.revealAt(target.x, target.y)

        assertEquals(1, events.loses, "chording over a wrong flag must detonate the unflagged bomb")
    }

    @Test
    fun wrongly_flagged_tile_in_same_row_is_shown_on_loss() {
        val (game, events) = newGame(w = 6, h = 6, bombs = 6, seed = 3L)
        game.revealAt(0, 0) // first click places bombs and is safe
        val bombs = game.bombPositions.toSet()
        val revealedSet = events.reveals.flatten().map { it.x to it.y }.toSet()

        // Find a bomb and a safe, still-unrevealed tile in the SAME ROW as that bomb.
        val bomb = bombs.first { b ->
            (0 until 6).any { sx -> sx != b.x && Vec2(sx, b.y) !in bombs && (sx to b.y) !in revealedSet }
        }
        val safeX = (0 until 6).first { sx ->
            sx != bomb.x && Vec2(sx, bomb.y) !in bombs && (sx to bomb.y) !in revealedSet
        }

        game.flagToggle(safeX, bomb.y) // wrongly flag a safe tile in the bomb's row
        events.reveals.clear()
        game.revealAt(bomb.x, bomb.y) // detonate

        val lastReveal = events.lastReveal()
        assertTrue(
            lastReveal.any { it.x == safeX && it.y == bomb.y && it.type == TileType.NO_BOMB },
            "a wrongly-flagged tile in the detonated bomb's row must be shown as NO_BOMB",
        )
        assertEquals(1, events.loses)
    }
}
