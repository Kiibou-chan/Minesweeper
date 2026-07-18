package space.kiibou

import processing.core.PConstants
import space.kiibou.gui.GraphicsElement
import kotlin.test.Test
import kotlin.test.assertSame

class FocusTest {

    private class Focusable(app: GApplet) : GraphicsElement(app) {
        init {
            focusable = true
        }
    }

    private val app = GApplet()

    private val left = Focusable(app).also {
        it.width = 10
        it.height = 10
        app.registerMethod("keyEvent", it)
    }

    private val right = Focusable(app).also {
        it.moveTo(100, 0)
        it.width = 10
        it.height = 10
        app.registerMethod("keyEvent", it)
    }

    private fun mouse(action: Int, x: Int, y: Int) =
        processing.event.MouseEvent(null, 0L, action, 0, x, y, PConstants.LEFT, 1)

    private fun dispatch(action: Int, x: Int, y: Int) {
        app.eventDispatcher.mouseEvent(mouse(action, x, y))
        app.eventDispatcher.pre()
    }

    @Test
    fun press_focuses_the_element_under_the_cursor() {
        dispatch(processing.event.MouseEvent.PRESS, 5, 5)
        assertSame(left, app.eventDispatcher.focused)
    }

    @Test
    fun hover_does_not_steal_focus() {
        dispatch(processing.event.MouseEvent.PRESS, 5, 5)
        dispatch(processing.event.MouseEvent.MOVE, 105, 5)
        assertSame(left, app.eventDispatcher.focused, "moving over another focusable must not move focus")
    }

    @Test
    fun press_on_empty_space_keeps_focus() {
        dispatch(processing.event.MouseEvent.PRESS, 105, 5)
        dispatch(processing.event.MouseEvent.PRESS, 500, 500)
        assertSame(right, app.eventDispatcher.focused)
    }
}
