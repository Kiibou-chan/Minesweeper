package space.kiibou.e2e

import org.junit.BeforeClass
import org.junit.Test
import space.kiibou.gui.text.TextInput
import space.kiibou.reactive.now
import java.awt.event.KeyEvent

/**
 * Editing keys over a real keyboard. [GuiRobot] synthesizes events, so it cannot prove
 * that Processing delivers what the widget listens for: it withholds the TYPE event for
 * backspace and enter, which once left both of them dead in the shipped app.
 */
class RealKeyboardJourneyTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun boot() = WindowedAppHarness.boot()
    }

    private val field get() = WindowedAppHarness.robot.findByTag("menu.nameInput") as TextInput

    @Test
    fun typing_and_backspace_work_on_a_real_keyboard() {
        WindowedAppHarness.robot.clickOn("menu.nameInput")
        val before = field.value.now ?: ""

        WindowedAppHarness.pressKeys(KeyEvent.VK_A, KeyEvent.VK_B)
        WindowedAppHarness.awaitUntil("both characters typed", 5_000) { field.value.now == before + "ab" }

        WindowedAppHarness.pressKeys(KeyEvent.VK_BACK_SPACE)
        WindowedAppHarness.awaitUntil("last character deleted", 5_000) { field.value.now == before + "a" }
    }

    @Test
    fun control_backspace_deletes_a_word_on_a_real_keyboard() {
        WindowedAppHarness.robot.clickOn("menu.nameInput")
        val before = field.value.now ?: ""

        WindowedAppHarness.pressKeys(KeyEvent.VK_SPACE, KeyEvent.VK_O, KeyEvent.VK_N, KeyEvent.VK_E)
        WindowedAppHarness.awaitUntil("word typed", 5_000) { field.value.now == "$before one" }

        WindowedAppHarness.pressChord(KeyEvent.VK_CONTROL, KeyEvent.VK_BACK_SPACE)
        WindowedAppHarness.awaitUntil("whole word deleted", 5_000) { field.value.now == "$before " }
    }
}
