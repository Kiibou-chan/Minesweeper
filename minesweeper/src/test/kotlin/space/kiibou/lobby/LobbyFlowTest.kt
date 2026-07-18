package space.kiibou.lobby

import space.kiibou.Minesweeper
import space.kiibou.common.*
import space.kiibou.gui.text.TextElement
import space.kiibou.net.common.Message
import space.kiibou.net.common.MessageType
import space.kiibou.test.GuiRobot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Headless GUI flow tests: real screens, real dispatcher and hit-testing, fake network.
 */
class LobbyFlowTest {

    private val app = Minesweeper()
    private val fake = FakeGameConnection()
    private val robot = GuiRobot(app)

    init {
        app.initHeadless(fake)
    }

    private fun <T : Any> receive(type: MessageType<T>, payload: T) {
        app.eventDispatcher.messageEvent(Message(type, payload))
        robot.pump()
    }

    private fun receive(type: MessageType<Unit>) = receive(type, Unit)

    private fun state(
        owner: Long,
        vararg members: MemberState,
        settings: MapInfo = MapInfo(9, 9, 10),
        phase: RoomPhase = RoomPhase.LOBBY,
    ) = RoomStateInfo(GameHandle(0), owner, members.toList(), settings, phase)

    @Test
    fun singleplayer_click_creates_and_readies_a_room() {
        robot.clickOn("menu.singleplayer")

        assertTrue(MinesweeperMessageType.CreateRoom in fake.sentTypes, "must create a room")
        assertTrue(MinesweeperMessageType.SetReady in fake.sentTypes, "must auto-ready")

        receive(MinesweeperMessageType.YourId, YourIdInfo(0))
        receive(MinesweeperMessageType.RoomState, state(owner = 0, MemberState(0, "Player 0", true)))

        assertFalse(robot.findByTag("screen.lobby").effectivelyHidden, "lobby screen must be shown")
        assertEquals("Room 0", (robot.findByTag("lobby.title") as TextElement).textProperty.value)
        assertFalse(robot.findByTag("lobby.start").effectivelyHidden, "owner sees the start button")
    }

    @Test
    fun non_owners_do_not_see_owner_controls() {
        robot.clickOn("menu.singleplayer")
        receive(MinesweeperMessageType.YourId, YourIdInfo(0))
        receive(
            MinesweeperMessageType.RoomState,
            state(owner = 99, MemberState(99, "Boss", false), MemberState(0, "Player 0", false)),
        )

        assertTrue(robot.findByTag("lobby.start").effectivelyHidden, "non-owner must not see start")
    }

    @Test
    fun multiplayer_lists_rooms_and_joins_by_click() {
        robot.clickOn("menu.multiplayer")
        assertTrue(MinesweeperMessageType.ListRooms in fake.sentTypes)

        receive(
            MinesweeperMessageType.RoomList,
            RoomListInfo(listOf(RoomSummary(GameHandle(5), 2, MapInfo(9, 9, 10)))),
        )

        robot.clickOn("rooms.row.5")

        assertTrue(
            fake.sent.any { it.messageType == MinesweeperMessageType.JoinRoom && it.payload == GameHandle(5) },
            "clicking a room row must join that room",
        )
    }

    @Test
    fun typing_a_room_number_joins_it() {
        robot.clickOn("menu.multiplayer")

        robot.clickOn("rooms.joinInput")
        robot.type("7")
        robot.pressEnter()

        assertTrue(
            fake.sent.any { it.messageType == MinesweeperMessageType.JoinRoom && it.payload == GameHandle(7) },
            "typing a room number and pressing enter must join it",
        )
    }

    @Test
    fun name_typed_on_the_menu_is_sent_before_entering_multiplayer() {
        robot.clickOn("menu.nameInput")
        robot.type("Svenja")
        robot.clickOn("menu.multiplayer")

        val nameIndex = fake.sent.indexOfFirst {
            it.messageType == MinesweeperMessageType.SetName && it.payload == NameInfo("Svenja")
        }
        val listIndex = fake.sentTypes.indexOf(MinesweeperMessageType.ListRooms)

        assertTrue(nameIndex >= 0, "the typed name must be sent even without pressing enter")
        assertTrue(nameIndex < listIndex, "the name must be set before listing rooms")
    }
}
