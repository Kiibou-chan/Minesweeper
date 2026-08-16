package space.kiibou

import javafx.scene.input.KeyCode
import space.kiibou.event.KeyEvent
import space.kiibou.gui.BorderBox
import space.kiibou.gui.BorderStyle
import space.kiibou.gui.text.EstimatingTextMetrics
import space.kiibou.gui.text.TextElement
import space.kiibou.gui.text.TextInput
import space.kiibou.reactive.now
import space.kiibou.reactive.observe
import space.kiibou.test.Keys
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TextInputTest {

    private val app = GApplet().also { it.textMetrics = EstimatingTextMetrics }
    private val input = TextInput(app)
    private var now = 10_000L

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
    fun typing_inserts_at_the_cursor() {
        "ab".forEach(::type)
        press(KeyCode.LEFT)
        type('c')

        assertEquals("acb", input.value.now)
    }

    @Test
    fun backspace_deletes_before_the_cursor() {
        "abc".forEach(::type)
        press(KeyCode.LEFT)
        press(KeyCode.BACK_SPACE)

        assertEquals("ac", input.value.now)
    }

    @Test
    fun delete_removes_after_the_cursor() {
        "abc".forEach(::type)
        press(KeyCode.LEFT)
        press(KeyCode.DELETE)

        assertEquals("ab", input.value.now)
    }

    @Test
    fun home_and_end_move_the_cursor_to_the_ends() {
        "ab".forEach(::type)
        press(KeyCode.HOME)
        type('x')
        press(KeyCode.END)
        type('y')

        assertEquals("xaby", input.value.now)
    }

    @Test
    fun the_cursor_stops_at_both_ends() {
        type('a')
        press(KeyCode.LEFT)
        press(KeyCode.LEFT)
        press(KeyCode.RIGHT)
        press(KeyCode.RIGHT)
        type('z')

        assertEquals("az", input.value.now)
    }

    @Test
    fun the_caret_is_solid_while_typing() {
        input.clock = { now }

        type('a')
        now += 50

        assertTrue(input.caretVisible)
    }

    @Test
    fun the_caret_blinks_once_typing_stops() {
        input.clock = { now }

        type('a')

        now += TextInput.CARET_SOLID_MS + TextInput.CARET_BLINK_MS
        assertFalse(input.caretVisible, "dark phase of the blink")

        now += TextInput.CARET_BLINK_MS
        assertTrue(input.caretVisible, "lit phase of the blink")
    }

    @Test
    fun the_caret_is_a_hairline_spanning_the_text_line() {
        input.init()

        val caret = caret()

        assertEquals(1, caret.width, "one unscaled pixel wide at scale 1")
        assertEquals(label().height, caret.height)
    }

    @Test
    fun the_caret_follows_the_cursor_through_the_text() {
        input.init()
        "abc".forEach(::type)

        val atEnd = caret().x
        press(KeyCode.HOME)
        val atStart = caret().x

        assertTrue(atStart < atEnd, "caret at $atStart after Home, $atEnd at the end")
    }

    private fun label() = input.children.filterIsInstance<BorderBox>().single()
        .children.filterIsInstance<TextElement>().single()

    private fun caret() = label().children.single()

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
