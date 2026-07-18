package space.kiibou.server

import space.kiibou.common.*
import space.kiibou.net.NetUtils
import space.kiibou.net.client.Client
import space.kiibou.net.common.ClientMessageType
import space.kiibou.net.common.Message
import space.kiibou.net.common.MessageType
import space.kiibou.net.common.Serial
import space.kiibou.net.common.ServerMessageType
import space.kiibou.net.server.main
import java.util.Collections
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Boots the real server (services auto-loaded from the test classpath) and drives the
 * full room lifecycle with two real socket clients: create, list, join, settings, ready,
 * start, a shared reveal, and the return to the lobby after the game ends.
 */
class RoomFlowIntegrationTest {

    companion object {
        init {
            Serial.addModule(ClientMessageType.serializersModule)
            Serial.addModule(ServerMessageType.serializersModule)
            Serial.addModule(MinesweeperMessageType.serializersModule)
        }
    }

    private class TestClient(port: Int) {
        val received: MutableList<Message<*>> = Collections.synchronizedList(mutableListOf())

        val client = Client({}, { received += it }, {}).connect("localhost", port)

        fun <T : Any> payloads(type: MessageType<T>): List<T> = synchronized(received) {
            @Suppress("UNCHECKED_CAST")
            received.filter { it.messageType == type }.map { it.payload as T }
        }

        fun lastRoomState(): RoomStateInfo? = payloads(MinesweeperMessageType.RoomState).lastOrNull()
    }

    private fun await(what: String, timeoutMs: Long = 5_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            Thread.sleep(20)
        }
        fail("Timed out waiting for: $what")
    }

    @Test
    fun two_clients_share_a_room_from_lobby_to_game_over() {
        val port = 20_000 + (System.nanoTime() % 10_000).toInt()
        main(arrayOf("--port=$port"))
        await("server to listen") { NetUtils.checkServerListening("localhost", port, 50) }

        val a = TestClient(port)

        // A picks a name, creates a room, and learns its id.
        a.client.send(MinesweeperMessageType.SetName, NameInfo("Alice"))
        a.client.send(MinesweeperMessageType.CreateRoom)
        await("A's YourId and RoomState") {
            a.payloads(MinesweeperMessageType.YourId).isNotEmpty() && a.lastRoomState() != null
        }
        val room = a.lastRoomState()!!.handle
        val aId = a.payloads(MinesweeperMessageType.YourId).single().id
        assertEquals(aId, a.lastRoomState()!!.owner, "creator owns the room")
        assertEquals("Alice", a.lastRoomState()!!.members.single().name, "chosen name appears in the room state")

        // B lists rooms, sees A's room, joins it.
        val b = TestClient(port)
        b.client.send(MinesweeperMessageType.ListRooms)
        await("B's room list showing A's room") {
            b.payloads(MinesweeperMessageType.RoomList).lastOrNull()?.rooms?.any { it.handle == room } == true
        }

        b.client.send(MinesweeperMessageType.JoinRoom, room)
        await("both seeing two members") {
            a.lastRoomState()?.members?.size == 2 && b.lastRoomState()?.members?.size == 2
        }

        // Owner picks settings, everyone sees them.
        a.client.send(MinesweeperMessageType.SetSettings, MapInfo(5, 5, 2))
        await("settings visible to B") { b.lastRoomState()?.settings == MapInfo(5, 5, 2) }

        // Start requires all ready; a premature start does nothing.
        a.client.send(MinesweeperMessageType.StartGame)
        Thread.sleep(150)
        assertTrue(a.payloads(MinesweeperMessageType.GameStarted).isEmpty(), "start before ready must be refused")

        a.client.send(MinesweeperMessageType.SetReady, ReadyInfo(true))
        b.client.send(MinesweeperMessageType.SetReady, ReadyInfo(true))
        await("both ready") { b.lastRoomState()?.members?.all { it.ready } == true }

        a.client.send(MinesweeperMessageType.StartGame)
        await("both getting GameStarted and the fresh board") {
            a.payloads(MinesweeperMessageType.GameStarted).isNotEmpty() &&
                b.payloads(MinesweeperMessageType.GameStarted).isNotEmpty() &&
                b.payloads(MinesweeperMessageType.Restart).isNotEmpty()
        }

        // A reveals; the same tiles must reach both players.
        a.client.send(MinesweeperMessageType.RevealTile, TilePosition(0, 0))
        await("both receiving the same reveal") {
            val fromA = a.payloads(MinesweeperMessageType.RevealTiles).lastOrNull()
            val fromB = b.payloads(MinesweeperMessageType.RevealTiles).lastOrNull()
            fromA != null && fromA == fromB && fromA.tiles.isNotEmpty()
        }

        // Sweep the whole 5x5 board; with 2 bombs this ends in a win or a loss either way.
        for (x in 0 until 5) for (y in 0 until 5) {
            a.client.send(MinesweeperMessageType.RevealTile, TilePosition(x, y))
        }
        await("game over reaching both players") {
            listOf(a, b).all {
                it.payloads(MinesweeperMessageType.Loose).isNotEmpty() ||
                    it.payloads(MinesweeperMessageType.Win).isNotEmpty()
            }
        }

        await("room back in its lobby with ready flags cleared") {
            val state = b.lastRoomState()
            state?.phase == RoomPhase.LOBBY && state.members.none { it.ready }
        }

        a.client.disconnect()
        b.client.disconnect()
    }
}
