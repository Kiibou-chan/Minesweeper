package space.kiibou.server

import space.kiibou.common.GameHandle
import space.kiibou.common.RoomStateInfo
import space.kiibou.net.common.ConnectionHandle

/**
 * Output port for room lifecycle notifications, mirroring the [GameEvents] pattern: the
 * production implementation broadcasts to the room's members over the network, tests
 * record the calls.
 */
interface RoomEvents {
    /** Full room state, re-sent to all members after every change. */
    fun roomState(state: RoomStateInfo)

    /** Told to a single connection right after it joins: the member id others see it as. */
    fun memberId(to: ConnectionHandle, id: Long)

    /** Told to a single connection whose join attempt was rejected (room already playing). */
    fun joinRefused(to: ConnectionHandle, room: GameHandle)

    /** The owner started the game; members should switch to the game view. */
    fun gameStarted()
}
