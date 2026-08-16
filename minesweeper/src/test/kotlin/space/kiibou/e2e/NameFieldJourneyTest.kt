package space.kiibou.e2e

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Typing into the menu's name field. The pixel probe guards the caret: drawn with the
 * canvas dimensions instead of its own, it covers the window from the text rightwards.
 */
class NameFieldJourneyTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun boot() = WindowedAppHarness.boot()

        @AfterClass
        @JvmStatic
        fun screenshotOnExit() {
            if (System.getProperty("gui.e2e") == "true") {
                WindowedAppHarness.screenshot("build/reports/gui-e2e/name-field.png")
            }
        }
    }

    @Test
    fun typing_a_name_leaves_the_rest_of_the_window_untouched() {
        val app = WindowedAppHarness.app
        // Alpha varies with when in the frame the probe lands; only the colour matters.
        fun corner() = WindowedAppHarness.probe(app.width - 20, app.height - 20) and 0xFFFFFF

        val background = corner()

        WindowedAppHarness.robot.clickOn("menu.nameInput")
        WindowedAppHarness.robot.type("Svenja")

        assertEquals(background, corner(), "the corner must still be background after typing")
    }
}
