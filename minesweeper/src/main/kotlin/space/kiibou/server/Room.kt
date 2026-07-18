package space.kiibou.server

import space.kiibou.common.GameHandle
import space.kiibou.common.MapInfo
import space.kiibou.common.MemberState
import space.kiibou.common.RoomPhase
import space.kiibou.common.RoomStateInfo
import space.kiibou.net.common.ConnectionHandle

/**
 * A game room with an explicit lifecycle: members gather and ready-up in [RoomPhase.LOBBY]
 * while the owner adjusts [settings]; once everyone is ready the owner starts the game
 * ([RoomPhase.PLAYING], joins refused); when the game ends the room returns to the lobby
 * with ready flags cleared. The first joiner owns the room and ownership passes to the
 * longest-present member when the owner leaves.
 */
class Room(
    val handle: GameHandle,
    private val events: RoomEvents,
    private val createGame: (MapInfo, MutableList<ConnectionHandle>) -> GameState,
) {
    var phase: RoomPhase = RoomPhase.LOBBY
        private set

    var settings: MapInfo = MapInfo(9, 9, 10)
        private set

    /** Join-ordered members, value = ready flag. */
    private val readyByMember = LinkedHashMap<ConnectionHandle, Boolean>()

    val members: List<ConnectionHandle> get() = readyByMember.keys.toList()

    val owner: ConnectionHandle? get() = readyByMember.keys.firstOrNull()

    var gameState: GameState? = null
        private set

    val isEmpty: Boolean get() = readyByMember.isEmpty()

    fun join(handle: ConnectionHandle): Boolean {
        if (phase != RoomPhase.LOBBY) {
            events.joinRefused(handle, this.handle)
            return false
        }

        readyByMember[handle] = false
        broadcastState()

        return true
    }

    fun leave(handle: ConnectionHandle) {
        if (readyByMember.remove(handle) == null) return

        if (phase == RoomPhase.PLAYING) {
            gameState?.removePlayer(handle)
        }

        if (!isEmpty) broadcastState()
    }

    fun setReady(handle: ConnectionHandle, ready: Boolean) {
        if (handle !in readyByMember) return

        readyByMember[handle] = ready
        broadcastState()
    }

    fun setSettings(handle: ConnectionHandle, settings: MapInfo) {
        if (phase != RoomPhase.LOBBY || handle != owner) return

        this.settings = settings
        broadcastState()
    }

    fun startGame(handle: ConnectionHandle): Boolean {
        if (phase != RoomPhase.LOBBY) return false
        if (handle != owner) return false
        if (!readyByMember.values.all { it }) return false

        val handles = readyByMember.keys.toMutableList()
        gameState = createGame(settings, handles)
        phase = RoomPhase.PLAYING

        events.gameStarted()
        broadcastState()

        return true
    }

    fun onGameOver() {
        if (phase != RoomPhase.PLAYING) return

        phase = RoomPhase.LOBBY
        gameState = null
        readyByMember.replaceAll { _, _ -> false }

        broadcastState()
    }

    fun stateInfo() = RoomStateInfo(
        handle,
        owner = owner?.handle ?: -1L,
        members = readyByMember.map { (member, ready) -> MemberState(member.handle, ready) },
        settings = settings,
        phase = phase,
    )

    private fun broadcastState() = events.roomState(stateInfo())
}
