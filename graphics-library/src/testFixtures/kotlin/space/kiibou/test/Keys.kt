package space.kiibou.test

import javafx.scene.input.KeyCode
import processing.core.PConstants
import processing.event.Event
import processing.event.KeyEvent
import space.kiibou.event.EventModifier

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

        // A capital arrives with shift held, which is part of the option a callback matches.
        return sequence(code, char, if (char.isUpperCase()) Event.SHIFT else 0)
    }

    fun pressed(code: KeyCode, vararg modifiers: EventModifier): List<KeyEvent> =
        sequence(code, CODED, modifiers.fold(0) { bits, modifier -> bits or bit(modifier) })

    private fun bit(modifier: EventModifier) = when (modifier) {
        EventModifier.SHIFT -> Event.SHIFT
        EventModifier.CTRL -> Event.CTRL
        EventModifier.META -> Event.META
        EventModifier.ALT -> Event.ALT
    }

    private fun sequence(code: KeyCode, char: Char, modifiers: Int): List<KeyEvent> {
        val press = event(KeyEvent.PRESS, char, code.code, modifiers)
        val release = event(KeyEvent.RELEASE, char, code.code, modifiers)

        if (char == CODED || char.isISOControl()) return listOf(press, release)

        return listOf(press, event(KeyEvent.TYPE, char, 0, modifiers), release)
    }

    private fun event(action: Int, char: Char, code: Int, modifiers: Int) =
        KeyEvent(null, 0L, action, modifiers, char, code)
}
