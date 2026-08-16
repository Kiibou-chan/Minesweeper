package space.kiibou

import javafx.scene.input.KeyCode
import space.kiibou.event.KeyEvent
import space.kiibou.gui.BorderBox
import space.kiibou.gui.BorderStyle
import space.kiibou.gui.text.EstimatingTextMetrics
import space.kiibou.gui.text.TextInput
import space.kiibou.reactive.now
import space.kiibou.reactive.observe
import space.kiibou.test.Keys
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TextInputTest {

    private val app = GApplet().also { it.textMetrics = EstimatingTextMetrics }
    private val input = TextInput(app)

    private fun type(key: Char) = Keys.typed(key).forEach { input.keyEvent(KeyEvent(it)) }

    private fun press(code: KeyCode) = Keys.pressed(code).forEach { input.keyEvent(KeyEvent(it)) }

    @Test
    fun typed_characters_are_appended() {
        type('a')
        type('b')
        assertEquals("ab", input.value.now)
    }

    @Test
    fun backspace_deletes_the_last_character() {
        type('a')
        type('b')
        type('\b')
        assertEquals("a", input.value.now)
    }

    @Test
    fun backspace_on_empty_value_is_a_no_op() {
        type('\b')
        assertEquals("", input.value.now)
    }

    @Test
    fun enter_submits_the_current_value() {
        var submitted: String? = null
        input.onSubmit = { submitted = it }
        type('h')
        type('i')
        type('\n')
        assertEquals("hi", submitted)
    }

    @Test
    fun control_characters_are_not_appended() {
        type('\t')
        type('￿') // Processing's CHAR_UNDEFINED for coded keys
        assertEquals("", input.value.now)
    }

    @Test
    fun events_are_ignored_while_inactive() {
        var submitted: String? = null
        input.onSubmit = { submitted = it }
        input.deactivate()
        type('a')
        type('\n')
        assertEquals("", input.value.now)
        assertNull(submitted)
    }

    @Test
    fun an_empty_input_is_drawn_as_a_sunken_box() {
        input.init()

        val border = input.children.filterIsInstance<BorderBox>().single()
        assertEquals(BorderStyle.IN, border.style)
    }

    @Test
    fun an_empty_input_keeps_a_name_sized_width() {
        input.init()

        assertTrue(input.width >= 100, "an empty input measured ${input.width}")
    }

    @Test
    fun value_changes_are_observable() {
        val seen = mutableListOf<String>()
        // Var.observe emits the current value on subscription, then every change.
        input.value.observe { seen += it }
        type('x')
        assertEquals(listOf("", "x"), seen)
    }
}
