package space.kiibou

import javafx.scene.input.KeyCode
import space.kiibou.event.KeyEvent
import kotlin.test.Test
import kotlin.test.assertEquals

class KeyCodeMappingTest {

    private fun keyEvent(key: Char, keyCode: Int) =
        KeyEvent(processing.event.KeyEvent(null, 0L, processing.event.KeyEvent.PRESS, 0, key, keyCode))

    @Test
    fun letter_key_codes_map_to_javafx_key_codes() {
        assertEquals(KeyCode.A, keyEvent('a', 65).keyCode)
    }

    @Test
    fun backspace_maps_to_back_space() {
        assertEquals(KeyCode.BACK_SPACE, keyEvent('\b', 8).keyCode)
    }

    @Test
    fun unknown_key_code_maps_to_undefined() {
        assertEquals(KeyCode.UNDEFINED, keyEvent('￿', 0).keyCode)
    }
}
