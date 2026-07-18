package space.kiibou.server

import space.kiibou.common.GameHandle
import space.kiibou.common.MapInfo
import space.kiibou.common.RoomPhase
import space.kiibou.net.common.ConnectionHandle
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoomTest {

    private fun newRoom(): Pair<Room, RecordingRoomEvents> {
        val events = RecordingRoomEvents()
        val room = Room(GameHandle(1), events, { "Name-${it.handle}" }) { settings, handles ->
            GameState(
                handles, settings.width, settings.height, settings.bombs,
                RecordingGameEvents(), ManualTicker(), Random(1L),
            )
        }
        return room to events
    }

    private fun handle(id: Long) = ConnectionHandle(id)

    // --- lobby phase ---

    @Test
    fun first_joiner_becomes_owner_and_state_is_broadcast() {
        val (room, events) = newRoom()
        assertTrue(room.join(handle(1)))
        assertEquals(handle(1), room.owner)
        assertEquals(RoomPhase.LOBBY, events.lastState().phase)
        assertEquals(1L, events.lastState().owner)
    }

    @Test
    fun successful_join_tells_the_joiner_its_member_id() {
        val (room, events) = newRoom()
        room.join(handle(7))
        assertEquals(handle(7) to 7L, events.memberIds.single())
    }

    @Test
    fun refused_join_does_not_emit_a_member_id() {
        val (room, events) = newRoom()
        room.join(handle(1))
        room.setReady(handle(1), true)
        room.startGame(handle(1))
        room.join(handle(2))
        assertEquals(listOf(handle(1) to 1L), events.memberIds)
    }

    @Test
    fun member_names_come_from_the_injected_resolver() {
        val (room, events) = newRoom()
        room.join(handle(1))
        room.join(handle(2))
        assertEquals(listOf("Name-1", "Name-2"), events.lastState().members.map { it.name })
    }

    @Test
    fun refresh_state_rebroadcasts_the_current_state() {
        val (room, events) = newRoom()
        room.join(handle(1))
        val before = events.roomStates.size
        room.refreshState()
        assertEquals(before + 1, events.roomStates.size)
    }

    @Test
    fun new_joiner_is_not_ready() {
        val (room, events) = newRoom()
        room.join(handle(1))
        room.join(handle(2))
        val member2 = events.lastState().members.first { it.id == 2L }
        assertFalse(member2.ready)
    }

    @Test
    fun members_toggle_only_their_own_ready_flag() {
        val (room, events) = newRoom()
        room.join(handle(1))
        room.join(handle(2))
        room.setReady(handle(2), true)
        val state = events.lastState()
        assertTrue(state.members.first { it.id == 2L }.ready)
        assertFalse(state.members.first { it.id == 1L }.ready)
    }

    @Test
    fun settings_are_owner_only_and_broadcast() {
        val (room, events) = newRoom()
        room.join(handle(1))
        room.join(handle(2))
        room.setSettings(handle(2), MapInfo(16, 16, 40)) // non-owner: ignored
        assertEquals(MapInfo(9, 9, 10), room.settings)
        room.setSettings(handle(1), MapInfo(16, 16, 40)) // owner: applied
        assertEquals(MapInfo(16, 16, 40), room.settings)
        assertEquals(MapInfo(16, 16, 40), events.lastState().settings)
    }

    @Test
    fun ownership_transfers_to_longest_present_member_on_owner_leave() {
        val (room, events) = newRoom()
        room.join(handle(1))
        room.join(handle(2))
        room.join(handle(3))
        room.leave(handle(1))
        assertEquals(handle(2), room.owner)
        assertEquals(2L, events.lastState().owner)
    }

    @Test
    fun room_is_empty_after_last_member_leaves() {
        val (room, _) = newRoom()
        room.join(handle(1))
        room.leave(handle(1))
        assertTrue(room.isEmpty)
        assertNull(room.owner)
    }

    // --- start / playing / back to lobby ---

    @Test
    fun start_requires_owner_and_all_ready() {
        val (room, _) = newRoom()
        room.join(handle(1))
        room.join(handle(2))
        room.setReady(handle(1), true)
        assertFalse(room.startGame(handle(1)), "member 2 not ready")
        room.setReady(handle(2), true)
        assertFalse(room.startGame(handle(2)), "non-owner cannot start")
        assertTrue(room.startGame(handle(1)))
        assertEquals(RoomPhase.PLAYING, room.phase)
        assertNotNull(room.gameState)
    }

    @Test
    fun start_emits_game_started_and_room_state() {
        val (room, events) = newRoom()
        room.join(handle(1))
        room.setReady(handle(1), true)
        room.startGame(handle(1))
        assertEquals(1, events.gameStarts)
        assertEquals(RoomPhase.PLAYING, events.lastState().phase)
    }

    @Test
    fun join_is_refused_while_playing() {
        val (room, events) = newRoom()
        room.join(handle(1))
        room.setReady(handle(1), true)
        room.startGame(handle(1))
        assertFalse(room.join(handle(2)))
        assertEquals(handle(2) to GameHandle(1), events.refusals.single())
        assertEquals(1, room.members.size)
    }

    @Test
    fun game_over_returns_to_lobby_and_resets_ready_flags() {
        val (room, events) = newRoom()
        room.join(handle(1))
        room.join(handle(2))
        room.setReady(handle(1), true)
        room.setReady(handle(2), true)
        room.startGame(handle(1))
        room.onGameOver()
        assertEquals(RoomPhase.LOBBY, room.phase)
        assertNull(room.gameState)
        assertTrue(events.lastState().members.none { it.ready })
    }

    @Test
    fun solo_rooms_auto_ready_after_game_over() {
        val (room, events) = newRoom()
        room.join(handle(1))
        room.setReady(handle(1), true)
        room.startGame(handle(1))
        room.onGameOver()
        assertTrue(events.lastState().members.single().ready, "a lone player should not need to re-ready")
    }

    @Test
    fun leave_while_playing_removes_player_from_running_game() {
        val (room, _) = newRoom()
        room.join(handle(1))
        room.join(handle(2))
        room.setReady(handle(1), true)
        room.setReady(handle(2), true)
        room.startGame(handle(1))
        val game = room.gameState!!
        room.leave(handle(2))
        assertFalse(game.handles.contains(handle(2)))
        assertEquals(RoomPhase.PLAYING, room.phase, "game continues for the rest")
    }
}
