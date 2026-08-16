package space.kiibou

import javafx.scene.input.KeyCode
import processing.event.Event
import space.kiibou.event.KeyAction
import space.kiibou.event.KeyEvent
import space.kiibou.event.MouseAction
import space.kiibou.event.MouseButton
import space.kiibou.event.MouseEvent
import space.kiibou.event.anyModifiers
import space.kiibou.event.options
import space.kiibou.gui.GraphicsElement
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Callbacks are looked up by an exact (key or button, action, modifiers) triple, so a
 * registration without modifiers cannot match a shifted event. [anyModifiers] registers
 * one that matches whatever is held.
 */
class EventOptionTest {

    private val app = GApplet()
    private val element = object : GraphicsElement(app) {}

    private fun key(action: Int, code: KeyCode, char: Char, modifiers: Int) =
        element.keyEvent(KeyEvent(processing.event.KeyEvent(null, 0L, action, modifiers, char, code.code)))

    private fun mouse(action: Int, modifiers: Int) =
        element.mouseEvent(MouseEvent(processing.event.MouseEvent(null, 0L, action, modifiers, 1, 1, processing.core.PConstants.LEFT, 1)))

    @Test
    fun a_modifier_agnostic_key_callback_fires_for_a_shifted_press() {
        var fired = 0
        element.registerCallback(anyModifiers(KeyCode.A, KeyAction.PRESS)) { fired++ }

        key(processing.event.KeyEvent.PRESS, KeyCode.A, 'A', Event.SHIFT)

        assertEquals(1, fired)
    }

    @Test
    fun an_exact_key_callback_wins_over_the_modifier_agnostic_one() {
        var exact = 0
        var any = 0
        element.registerCallback(options(KeyCode.A, KeyAction.PRESS)) { exact++ }
        element.registerCallback(anyModifiers(KeyCode.A, KeyAction.PRESS)) { any++ }

        key(processing.event.KeyEvent.PRESS, KeyCode.A, 'a', 0)
        key(processing.event.KeyEvent.PRESS, KeyCode.A, 'A', Event.SHIFT)

        assertEquals(1, exact, "unmodified press goes to the exact callback")
        assertEquals(1, any, "shifted press falls back to the modifier-agnostic one")
    }

    @Test
    fun a_modifier_agnostic_mouse_callback_fires_for_a_shift_click() {
        var fired = 0
        element.registerCallback(anyModifiers(MouseButton.LEFT, MouseAction.RELEASE)) { fired++ }

        mouse(processing.event.MouseEvent.RELEASE, Event.SHIFT)

        assertEquals(1, fired)
    }
}
