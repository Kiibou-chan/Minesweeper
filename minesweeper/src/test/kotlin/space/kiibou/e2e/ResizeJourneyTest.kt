package space.kiibou.e2e

import org.junit.BeforeClass
import org.junit.Test
import space.kiibou.e2e.WindowedAppHarness.app
import space.kiibou.e2e.WindowedAppHarness.awaitUntil
import space.kiibou.e2e.WindowedAppHarness.robot
import space.kiibou.game.Tile

/**
 * The Expert board (30x16) forces the window to grow after Start. This is the exact
 * scenario that defeated the external xdotool scripts (stale window handles after
 * resize); in-process, positions come from the live scene graph, so far-corner tiles
 * must stay clickable.
 */
class ResizeJourneyTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun boot() = WindowedAppHarness.boot()
    }

    @Test
    fun expert_board_resizes_the_window_and_stays_clickable() {
        robot.clickOn("menu.singleplayer")
        awaitUntil("lobby shown", 5_000) { !robot.findByTag("screen.lobby").effectivelyHidden }

        robot.clickOn("lobby.preset.expert")
        robot.clickOn("lobby.start")
        awaitUntil("map screen shown", 5_000) {
            robot.findByTagOrNull("screen.map")?.effectivelyHidden == false
        }

        awaitUntil("window grown to fit the 30x16 board", 15_000) { app.width > 900 }

        val farTile = robot.findByTag("tile.29.15") as Tile
        robot.clickOn(farTile)
        awaitUntil("far-corner tile revealed after resize", 5_000) { farTile.revealed }

        WindowedAppHarness.screenshot("build/reports/gui-e2e/expert-board.png")
    }
}
