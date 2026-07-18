package space.kiibou.server

import space.kiibou.common.GameHandle
import space.kiibou.common.RoomStateInfo
import space.kiibou.net.common.ConnectionHandle

/** Test double that records every emitted room event for assertions. */
class RecordingRoomEvents : RoomEvents {
    val roomStates = mutableListOf<RoomStateInfo>()
    val refusals = mutableListOf<Pair<ConnectionHandle, GameHandle>>()
    var gameStarts = 0

    override fun roomState(state: RoomStateInfo) { roomStates += state }
    override fun joinRefused(to: ConnectionHandle, room: GameHandle) { refusals += to to room }
    override fun gameStarted() { gameStarts++ }

    fun lastState(): RoomStateInfo = roomStates.last()
}
