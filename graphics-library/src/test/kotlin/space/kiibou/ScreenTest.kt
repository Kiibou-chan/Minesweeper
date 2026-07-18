package space.kiibou

import processing.core.PConstants
import space.kiibou.event.MouseAction
import space.kiibou.event.MouseButton
import space.kiibou.event.options
import space.kiibou.gui.GraphicsElement
import space.kiibou.gui.ScreenManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScreenTest {

    private class TestApp : GApplet() {
        val manager get() = graphicsManager
    }

    private open class Element(app: GApplet) : GraphicsElement(app) {
        var inits = 0
        override fun initImpl() {
            inits++
        }
    }

    @Test
    fun show_switches_hidden_and_active_flags() {
        val app = TestApp()
        val screens = ScreenManager(app)
        val a = Element(app)
        val b = Element(app)
        screens.add(a)
        screens.add(b)

        assertTrue(a.hidden, "screens start hidden")
        assertFalse(a.active, "screens start inactive")

        screens.show(a)
        assertFalse(a.hidden)
        assertTrue(a.active)

        screens.show(b)
        assertTrue(a.hidden)
        assertFalse(a.active)
        assertFalse(b.hidden)
        assertTrue(b.active)
        assertEquals(b, screens.current)
    }

    @Test
    fun elements_registered_after_startup_are_initialized() {
        val app = TestApp()
        val early = Element(app)
        app.registerGraphicsElement(early)

        app.manager.pre() // the one-shot init pass at sketch start

        assertEquals(1, early.inits)

        val late = Element(app)
        app.registerGraphicsElement(late)
        assertEquals(1, late.inits, "late registration must still run initImpl")
    }

    @Test
    fun inactive_elements_do_not_swallow_mouse_events_or_focus() {
        val app = TestApp()

        var clicks = 0
        val below = Element(app).also {
            it.width = 10
            it.height = 10
            it.registerCallback(options(MouseButton.LEFT, MouseAction.PRESS)) { clicks++ }
        }

        // An overlapping element that is deeper in the hierarchy but inactive.
        val container = object : GraphicsElement(app) {}
        val overlay = Element(app).also {
            it.width = 10
            it.height = 10
            it.focusable = true
            container.addChild(it)
            it.registerCallback(options(MouseButton.LEFT, MouseAction.PRESS)) { }
            app.registerMethod("keyEvent", it)
            it.deactivate()
        }

        app.eventDispatcher.mouseEvent(
            processing.event.MouseEvent(null, 0L, processing.event.MouseEvent.PRESS, 0, 5, 5, PConstants.LEFT, 1),
        )
        app.eventDispatcher.pre()

        assertEquals(1, clicks, "the active element below must receive the press")
        assertNull(app.eventDispatcher.focused, "an inactive focusable must not take focus")
        assertFalse(overlay.active)
    }
}
