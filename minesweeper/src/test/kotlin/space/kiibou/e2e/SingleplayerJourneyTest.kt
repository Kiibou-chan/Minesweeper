package space.kiibou.e2e

import org.junit.BeforeClass
import org.junit.Test
import space.kiibou.e2e.WindowedAppHarness.awaitUntil
import space.kiibou.e2e.WindowedAppHarness.robot
import space.kiibou.game.Tile
import kotlin.test.assertNotEquals

class SingleplayerJourneyTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun boot() = WindowedAppHarness.boot()
    }

    @Test
    fun singleplayer_reaches_the_board_and_reveals_a_tile() {
        robot.clickOn("menu.singleplayer")
        awaitUntil("lobby shown after creating a room", 5_000) {
            !robot.findByTag("screen.lobby").effectivelyHidden
        }

        robot.clickOn("lobby.start")
        awaitUntil("map screen shown after start", 5_000) {
            robot.findByTagOrNull("screen.map")?.effectivelyHidden == false
        }

        val tile = robot.findByTag("tile.4.4") as Tile
        robot.clickOn(tile)
        awaitUntil("tile revealed via the real server round trip", 5_000) { tile.revealed }

        // The board actually rendered: the revealed tile's pixels are not the flat background.
        val pixel = WindowedAppHarness.probe(tile.x + tile.width / 2, tile.y + tile.height / 2)
        assertNotEquals(0xFFCCCCCC.toInt(), pixel, "tile area must not be the bare background color")

        WindowedAppHarness.screenshot("build/reports/gui-e2e/singleplayer-board.png")
    }
}
