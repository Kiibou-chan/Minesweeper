package space.kiibou.lobby

import space.kiibou.Minesweeper
import space.kiibou.gui.Button
import space.kiibou.gui.GraphicsElement
import space.kiibou.gui.VerticalList
import space.kiibou.gui.text.TextElement
import space.kiibou.gui.text.TextInput
import space.kiibou.reactive.now
import space.kiibou.reactive.observe

/**
 * The entry screen: pick a name, then Singleplayer (create a room and auto-ready — the
 * ready-lobby doubles as the difficulty selector) or Multiplayer (browse rooms). New
 * entries (Leaderboard, Settings, ...) are additive rows in the button list.
 */
class MainMenuScreen(override val app: Minesweeper) : GraphicsElement(app) {

    private val nameInput = TextInput(app, initial = "").also { input ->
        input.onSubmit = { app.setPlayerName(it) }
    }

    private val list = VerticalList(app, 8).also { list ->
        list += TextElement(app, "Minesweeper", fontSize = 30)

        list += TextElement(app, "Name (click, type, Enter):")
        list += nameInput

        list += Button(app, TextElement(app, "Singleplayer")).also { button ->
            button.clicked observe {
                submitPendingName()
                app.startSingleplayer()
            }
        }

        list += Button(app, TextElement(app, "Multiplayer")).also { button ->
            button.clicked observe {
                submitPendingName()
                app.showRoomList()
            }
        }

        addChild(list)
    }

    init {
        list.xProp.bind(xProp)
        list.yProp.bind(yProp)
        widthProp.bind(list.widthProp)
        heightProp.bind(list.heightProp)
    }

    /** A typed-but-unsubmitted name should still count when leaving the menu. */
    private fun submitPendingName() {
        nameInput.value.now?.takeIf { it.isNotBlank() }?.let { app.setPlayerName(it) }
    }
}
