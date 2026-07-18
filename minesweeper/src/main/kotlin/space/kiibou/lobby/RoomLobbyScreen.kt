package space.kiibou.lobby

import space.kiibou.Minesweeper
import space.kiibou.common.MapInfo
import space.kiibou.common.MinesweeperMessageType
import space.kiibou.common.ReadyInfo
import space.kiibou.common.RoomStateInfo
import space.kiibou.gui.Button
import space.kiibou.gui.GraphicsElement
import space.kiibou.gui.VerticalList
import space.kiibou.gui.text.TextElement
import space.kiibou.reactive.observe

/**
 * The ready-lobby of a joined room: member list with ready markers, the settings the
 * owner picked, a Ready toggle for everyone, and (owner only) difficulty presets plus
 * the Start button. Rendered from every [MinesweeperMessageType.RoomState] broadcast.
 */
class RoomLobbyScreen(override val app: Minesweeper) : GraphicsElement(app) {

    private var myReady = false

    private val title = TextElement(app, "Room", fontSize = 25).also { it.testTag = "lobby.title" }
    private val settingsText = TextElement(app, "9x9 · 10 bombs")
    private val members = VerticalList(app, 2)

    private val readyButton = Button(app, TextElement(app, "Ready / Unready")).also { button ->
        button.testTag = "lobby.ready"
        button.clicked observe { app.client.send(MinesweeperMessageType.SetReady, ReadyInfo(!myReady)) }
    }

    private val presets = VerticalList(app, 2).also { presets ->
        listOf(
            Triple("Beginner (9x9, 10)", MapInfo(9, 9, 10), "beginner"),
            Triple("Intermediate (16x16, 40)", MapInfo(16, 16, 40), "intermediate"),
            Triple("Expert (30x16, 99)", MapInfo(30, 16, 99), "expert"),
        ).forEach { (label, settings, tag) ->
            presets += Button(app, TextElement(app, label)).also { button ->
                button.testTag = "lobby.preset.$tag"
                button.clicked observe { app.client.send(MinesweeperMessageType.SetSettings, settings) }
            }
        }
    }

    private val startButton = Button(app, TextElement(app, "Start Game")).also { button ->
        button.testTag = "lobby.start"
        button.clicked observe { app.client.send(MinesweeperMessageType.StartGame) }
    }

    private val leaveButton = Button(app, TextElement(app, "Leave Room")).also { button ->
        button.testTag = "lobby.leave"
        button.clicked observe {
            app.client.send(MinesweeperMessageType.LeaveRoom)
            app.showRoomList()
        }
    }

    private val list = VerticalList(app, 6).also { list ->
        list += title
        list += settingsText
        list += members
        list += readyButton
        list += presets
        list += startButton
        list += leaveButton

        addChild(list)
    }

    init {
        testTag = "screen.lobby"
        list.xProp.bind(xProp)
        list.yProp.bind(yProp)
        widthProp.bind(list.widthProp)
        heightProp.bind(list.heightProp)
    }

    fun update(state: RoomStateInfo, myId: Long?) {
        title.textProperty.value = "Room ${state.handle.gameId}"

        val (width, height, bombs) = state.settings
        settingsText.textProperty.value = "${width}x${height} · $bombs bombs"

        myReady = state.members.firstOrNull { it.id == myId }?.ready ?: false

        members.children.forEach(GraphicsElement::deactivate)
        members.removeAllChildren()
        state.members.forEach { member ->
            val markers = buildString {
                append(if (member.ready) "[ready]" else "[ -- ]")
                if (member.id == state.owner) append(" (owner)")
                if (member.id == myId) append(" (you)")
            }
            members += TextElement(app, "${member.name} $markers")
        }

        val isOwner = myId != null && myId == state.owner
        setOwnerControlsVisible(isOwner)

        if (active) activate()
    }

    private fun setOwnerControlsVisible(visible: Boolean) {
        listOf<GraphicsElement>(presets, startButton).forEach {
            if (visible) it.show() else it.hide()
        }
    }

    override fun activate() {
        super.activate()

        // Hidden owner controls must never react to clicks meant for other elements.
        listOf<GraphicsElement>(presets, startButton).forEach {
            if (it.hidden) it.deactivate()
        }
    }
}
