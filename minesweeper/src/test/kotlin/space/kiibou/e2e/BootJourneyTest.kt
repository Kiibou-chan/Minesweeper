package space.kiibou.e2e

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import kotlin.test.assertFalse

class BootJourneyTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun boot() = WindowedAppHarness.boot()

        @AfterClass
        @JvmStatic
        fun screenshotOnExit() {
            // Artifact for eyeballing what the window showed.
            if (System.getProperty("gui.e2e") == "true") {
                WindowedAppHarness.screenshot("build/reports/gui-e2e/boot-menu.png")
            }
        }
    }

    @Test
    fun app_boots_into_a_visible_main_menu() {
        assertFalse(
            WindowedAppHarness.robot.findByTag("screen.menu").effectivelyHidden,
            "the main menu must be the visible screen after boot",
        )
    }
}
