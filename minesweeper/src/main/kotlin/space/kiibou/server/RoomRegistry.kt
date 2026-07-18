package space.kiibou.server

import space.kiibou.common.GameHandle
import space.kiibou.common.RoomSummary
import space.kiibou.common.RoomPhase
import space.kiibou.net.common.ConnectionHandle

/**
 * Allocates room handles and maps each connection to the room it is in. Rooms dissolve
 * when their last member leaves, and only lobby-phase rooms are listed to joiners.
 */
class RoomRegistry(private val createRoom: (GameHandle) -> Room) {

    private var nextHandle = 0L
    private val rooms = mutableMapOf<GameHandle, Room>()
    private val users = mutableMapOf<ConnectionHandle, GameHandle>()

    fun createAndJoin(handle: ConnectionHandle): Room {
        val roomHandle = GameHandle(nextHandle++)
        val room = createRoom(roomHandle)
        rooms[roomHandle] = room

        room.join(handle)
        users[handle] = roomHandle

        return room
    }

    fun join(handle: ConnectionHandle, roomHandle: GameHandle): Boolean {
        val room = rooms[roomHandle] ?: return false

        if (!room.join(handle)) return false

        users[handle] = roomHandle
        return true
    }

    fun roomFor(handle: ConnectionHandle): Room? = users[handle]?.let { rooms[it] }

    fun leave(handle: ConnectionHandle) {
        val roomHandle = users.remove(handle) ?: return
        val room = rooms[roomHandle] ?: return

        room.leave(handle)
        if (room.isEmpty) rooms.remove(roomHandle)
    }

    fun listLobbyRooms(): List<RoomSummary> = rooms.values
        .filter { it.phase == RoomPhase.LOBBY }
        .map { RoomSummary(it.handle, it.members.size, it.settings) }

    fun activeRoomCount(): Int = rooms.size
}
