package space.kiibou

import space.kiibou.gui.Button
import space.kiibou.gui.VerticalList
import space.kiibou.gui.text.EstimatingTextMetrics
import space.kiibou.gui.text.TextElement
import space.kiibou.gui.text.TextInput
import space.kiibou.reactive.now
import space.kiibou.reactive.observe
import space.kiibou.test.GuiRobot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GuiFrameworkTest {

    private class TestApp : GApplet() {
        val manager get() = graphicsManager
    }

    private fun app() = TestApp().also { it.textMetrics = EstimatingTextMetrics }

    @Test
    fun text_element_lays_out_headless() {
        val app = app()
        val text = TextElement(app, "Hello")
        assertTrue(text.width > 0, "estimating metrics must give text a width without a sketch")
        assertTrue(text.height > 0)
    }

    @Test
    fun find_by_tag_resolves_nested_elements() {
        val app = app()
        val robot = GuiRobot(app)

        val list = VerticalList(app, 2)
        val button = Button(app, TextElement(app, "Nested")).also { it.testTag = "nested.button" }
        list += button
        app.registerGraphicsElement(list)

        assertEquals(button, robot.findByTag("nested.button"))
    }

    @Test
    fun robot_click_fires_button() {
        val app = app()
        val robot = GuiRobot(app)

        var clicks = 0
        val button = Button(app, TextElement(app, "Click me")).also {
            it.testTag = "btn"
            it.clicked observe { clicks++ }
        }
        app.registerGraphicsElement(button)
        app.manager.pre()

        robot.clickOn("btn")

        assertEquals(1, clicks, "a robot click at the button's real position must fire it")
    }

    @Test
    fun robot_click_then_type_edits_text_input() {
        val app = app()
        val robot = GuiRobot(app)

        var submitted: String? = null
        val input = TextInput(app, "x").also {
            it.testTag = "input"
            it.onSubmit = { text -> submitted = text }
        }
        app.registerGraphicsElement(input)
        app.manager.pre()

        robot.clickOn("input") // focus via the real click-to-focus path
        robot.type("ab")
        assertEquals("xab", input.value.now)

        robot.pressEnter()
        assertEquals("xab", submitted)
    }

    @Test
    fun dispatcher_reports_idle_only_when_queues_are_empty() {
        val app = app()
        assertTrue(app.eventDispatcher.isIdle(), "no events queued yet")

        app.eventDispatcher.mouseEvent(
            processing.event.MouseEvent(null, 0L, processing.event.MouseEvent.PRESS, 0, 5, 5, processing.core.PConstants.LEFT, 1),
        )
        assertTrue(!app.eventDispatcher.isIdle(), "queued event must show as pending")

        app.eventDispatcher.pre()
        assertTrue(app.eventDispatcher.isIdle(), "drained after pre()")
    }

    @Test
    fun robot_uses_a_custom_pump_strategy_when_given() {
        val app = app()
        var pumps = 0
        val robot = GuiRobot(app) { pumps++; app.eventDispatcher.pre() }

        val button = Button(app, TextElement(app, "P")).also { it.testTag = "p" }
        app.registerGraphicsElement(button)
        app.manager.pre()

        robot.clickOn("p")
        assertTrue(pumps > 0, "the injected pump strategy must be used")
    }

    @Test
    fun empty_text_input_is_clickable() {
        val app = app()
        val input = TextInput(app)
        app.registerGraphicsElement(input)
        app.manager.pre()

        assertTrue(input.width >= 60, "an empty input needs a minimum clickable width, was ${input.width}")
    }
}
