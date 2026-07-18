package space.kiibou.server

import space.kiibou.common.GameHandle
import space.kiibou.common.MapInfo
import space.kiibou.net.common.ConnectionHandle
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoomRegistryTest {

    private fun registry() = RoomRegistry { roomHandle ->
        Room(roomHandle, RecordingRoomEvents()) { settings, handles ->
            GameState(
                handles, settings.width, settings.height, settings.bombs,
                RecordingGameEvents(), ManualTicker(), Random(1L),
            )
        }
    }

    private fun handle(id: Long) = ConnectionHandle(id)

    @Test
    fun create_and_join_allocates_sequential_handles() {
        val r = registry()
        val room1 = r.createAndJoin(handle(1))
        val room2 = r.createAndJoin(handle(2))
        assertEquals(GameHandle(0), room1.handle)
        assertEquals(GameHandle(1), room2.handle)
        assertEquals(2, r.activeRoomCount())
    }

    @Test
    fun join_maps_the_connection_to_the_room() {
        val r = registry()
        val room = r.createAndJoin(handle(1))
        assertTrue(r.join(handle(2), room.handle))
        assertEquals(room, r.roomFor(handle(2)))
        assertEquals(2, room.members.size)
    }

    @Test
    fun join_to_unknown_room_fails_safely() {
        val r = registry()
        assertFalse(r.join(handle(1), GameHandle(99)))
        assertNull(r.roomFor(handle(1)))
    }

    @Test
    fun listing_hides_playing_rooms() {
        val r = registry()
        val lobbyRoom = r.createAndJoin(handle(1))
        val playingRoom = r.createAndJoin(handle(2))
        playingRoom.setSettings(handle(2), MapInfo(9, 9, 10))
        playingRoom.setReady(handle(2), true)
        playingRoom.startGame(handle(2))

        val listed = r.listLobbyRooms()
        assertEquals(listOf(lobbyRoom.handle), listed.map { it.handle })
        assertEquals(1, listed.single().memberCount)
    }

    @Test
    fun last_player_leaving_dissolves_the_room() {
        val r = registry()
        val room = r.createAndJoin(handle(1))
        r.join(handle(2), room.handle)
        r.leave(handle(1))
        assertEquals(1, r.activeRoomCount(), "room survives while occupied")
        r.leave(handle(2))
        assertEquals(0, r.activeRoomCount())
    }

    @Test
    fun lookup_or_leave_for_unknown_handle_is_safe() {
        val r = registry()
        assertNull(r.roomFor(handle(99)))
        r.leave(handle(99)) // must not throw
        assertEquals(0, r.activeRoomCount())
    }
}
