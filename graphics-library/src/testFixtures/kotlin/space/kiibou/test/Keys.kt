package space.kiibou.test

import javafx.scene.input.KeyCode
import processing.core.PConstants
import processing.event.KeyEvent

/**
 * Builds the event sequence Processing really delivers, measured against a live sketch:
 * PRESS, then TYPE for printable keys only, then RELEASE. `PSurfaceJOGL` withholds the
 * TYPE event for coded keys (arrows, Home, ...) and for backspace, tab, enter, escape and
 * delete, so a fixture that always emits TYPE tests a key path the app never produces.
 */
object Keys {

    private const val CODED = PConstants.CODED.toChar()

    fun typed(char: Char): List<KeyEvent> {
        val code = when (char) {
            '\b' -> KeyCode.BACK_SPACE
            '\t' -> KeyCode.TAB
            '\n', '\r' -> KeyCode.ENTER
            else -> KeyCode.getKeyCode(char.uppercaseChar().toString()) ?: KeyCode.UNDEFINED
        }

        return sequence(code, char)
    }

    fun pressed(code: KeyCode): List<KeyEvent> = sequence(code, CODED)

    private fun sequence(code: KeyCode, char: Char): List<KeyEvent> {
        val press = event(KeyEvent.PRESS, char, code.code)
        val release = event(KeyEvent.RELEASE, char, code.code)

        if (char == CODED || char.isISOControl()) return listOf(press, release)

        return listOf(press, event(KeyEvent.TYPE, char, 0), release)
    }

    private fun event(action: Int, char: Char, code: Int) =
        KeyEvent(null, 0L, action, 0, char, code)
}
