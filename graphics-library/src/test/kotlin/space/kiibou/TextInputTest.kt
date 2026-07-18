package space.kiibou

import space.kiibou.event.KeyEvent
import space.kiibou.gui.text.TextInput
import space.kiibou.reactive.now
import space.kiibou.reactive.observe
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TextInputTest {

    private val app = GApplet()
    private val input = TextInput(app)

    private fun type(key: Char) = input.keyEvent(
        KeyEvent(processing.event.KeyEvent(null, 0L, processing.event.KeyEvent.TYPE, 0, key, 0)),
    )

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
    fun value_changes_are_observable() {
        val seen = mutableListOf<String>()
        // Var.observe emits the current value on subscription, then every change.
        input.value.observe { seen += it }
        type('x')
        assertEquals(listOf("", "x"), seen)
    }
}
