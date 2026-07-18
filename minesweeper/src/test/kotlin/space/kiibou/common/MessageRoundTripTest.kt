package space.kiibou.common

import space.kiibou.net.common.ClientMessageType
import space.kiibou.net.common.InternalMessageSerializer
import space.kiibou.net.common.Message
import space.kiibou.net.common.Serial
import space.kiibou.net.common.ServerMessageType
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageRoundTripTest {

    companion object {
        init {
            Serial.addModule(ClientMessageType.serializersModule)
            Serial.addModule(ServerMessageType.serializersModule)
            Serial.addModule(MinesweeperMessageType.serializersModule)
        }
    }

    private val samples: List<Message<*>> = listOf(
        // pre-existing types (regression guard)
        Message(MinesweeperMessageType.InitMap, MapInfo(9, 9, 10)),
        Message(MinesweeperMessageType.SetFlag, FlagInfo(1, 2, true)),
        Message(MinesweeperMessageType.Win, Unit),
        // room lifecycle types
        Message(MinesweeperMessageType.CreateRoom, Unit),
        Message(MinesweeperMessageType.ListRooms, Unit),
        Message(MinesweeperMessageType.JoinRoom, GameHandle(3)),
        Message(MinesweeperMessageType.LeaveRoom, Unit),
        Message(MinesweeperMessageType.SetReady, ReadyInfo(true)),
        Message(MinesweeperMessageType.SetSettings, MapInfo(16, 16, 40)),
        Message(MinesweeperMessageType.StartGame, Unit),
        Message(
            MinesweeperMessageType.RoomList,
            RoomListInfo(listOf(RoomSummary(GameHandle(1), 2, MapInfo(9, 9, 10)))),
        ),
        Message(
            MinesweeperMessageType.RoomState,
            RoomStateInfo(
                GameHandle(1),
                owner = 7L,
                members = listOf(MemberState(7L, true), MemberState(8L, false)),
                settings = MapInfo(9, 9, 10),
                phase = RoomPhase.LOBBY,
            ),
        ),
        Message(MinesweeperMessageType.JoinRefused, GameHandle(2)),
        Message(MinesweeperMessageType.GameStarted, Unit),
    )

    @Test
    fun every_message_type_round_trips_through_the_wire_format() {
        val serializer = InternalMessageSerializer(Serial.json)

        samples.forEach { message ->
            val encoded = Serial.json.encodeToString(serializer, message)
            val decoded = Serial.json.decodeFromString(serializer, encoded)

            assertEquals(message.messageType, decoded.messageType, "type of $message")
            assertEquals(message.payload, decoded.payload, "payload of $message")
        }
    }
}
