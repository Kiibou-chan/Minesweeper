package space.kiibou.lobby

import space.kiibou.Minesweeper
import space.kiibou.common.MinesweeperMessageType
import space.kiibou.common.RoomSummary
import space.kiibou.gui.Button
import space.kiibou.gui.GraphicsElement
import space.kiibou.gui.VerticalList
import space.kiibou.gui.text.TextElement
import space.kiibou.gui.text.TextInput
import space.kiibou.reactive.observe

/**
 * The entry screen: create a room or join one of the listed lobby-phase rooms by
 * clicking it. The row list is rebuilt from every [MinesweeperMessageType.RoomList]
 * message.
 */
class RoomListScreen(override val app: Minesweeper) : GraphicsElement(app) {

    private val rooms = VerticalList(app, 2)

    private val list = VerticalList(app, 6).also { list ->
        list += TextElement(app, "Rooms", fontSize = 25)

        list += Button(app, TextElement(app, "Back to Menu")).also { button ->
            button.clicked observe { app.showMainMenu() }
        }

        list += Button(app, TextElement(app, "Create Room")).also { button ->
            button.clicked observe { app.client.send(MinesweeperMessageType.CreateRoom) }
        }

        list += Button(app, TextElement(app, "Refresh")).also { button ->
            button.clicked observe { app.client.send(MinesweeperMessageType.ListRooms) }
        }

        list += TextElement(app, "Join room by number (click, type, Enter):")
        list += TextInput(app).also { input ->
            input.onSubmit = { text ->
                text.trim().toLongOrNull()?.let { id ->
                    app.client.send(MinesweeperMessageType.JoinRoom, space.kiibou.common.GameHandle(id))
                }
            }
        }

        list += rooms

        addChild(list)
    }

    init {
        list.xProp.bind(xProp)
        list.yProp.bind(yProp)
        widthProp.bind(list.widthProp)
        heightProp.bind(list.heightProp)
    }

    fun update(summaries: List<RoomSummary>) {
        rooms.children.forEach(GraphicsElement::deactivate)
        rooms.removeAllChildren()

        summaries.forEach { summary ->
            val (width, height, bombs) = summary.settings
            val label = "Room ${summary.handle.gameId} · ${summary.memberCount} player(s) · ${width}x${height}/${bombs}"

            rooms += Button(app, TextElement(app, label)).also { button ->
                button.clicked observe { app.client.send(MinesweeperMessageType.JoinRoom, summary.handle) }
            }
        }

        if (active) rooms.activate()
    }
}
