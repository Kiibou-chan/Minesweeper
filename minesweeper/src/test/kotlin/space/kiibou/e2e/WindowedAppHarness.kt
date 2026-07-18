package space.kiibou.e2e

import org.junit.Assume
import processing.core.PApplet
import space.kiibou.Minesweeper
import space.kiibou.common.MinesweeperMessageType
import space.kiibou.net.NetUtils
import space.kiibou.net.server.main
import space.kiibou.test.GuiRobot
import java.io.File
import kotlin.test.fail

/**
 * Boots ONE real windowed [Minesweeper] sketch (xvfb in CI-like environments) for all
 * Tier-2 journeys in the JVM, plus an in-process server on 8454. Input still goes
 * through the synthetic dispatcher path (Tier-1 robot); the pump waits for the sketch's
 * own frame loop to drain the queues instead of calling `pre()` itself.
 *
 * Gated: every journey class calls [boot] in `@BeforeClass`, which skips the class via
 * JUnit assumption unless `-Dgui.e2e=true`.
 */
object WindowedAppHarness {

    lateinit var app: Minesweeper
        private set

    lateinit var robot: GuiRobot
        private set

    private val hook = DrawHook()

    fun boot() {
        Assume.assumeTrue("windowed GUI e2e disabled (run with -Dgui.e2e=true)", System.getProperty("gui.e2e") == "true")

        if (::app.isInitialized) {
            reset()
            return
        }

        if (!NetUtils.checkServerListening("localhost", 8454, 100)) {
            main(arrayOf("--port=8454"))
            awaitUntil("server listening", 10_000) { NetUtils.checkServerListening("localhost", 8454, 100) }
        }

        app = Minesweeper().also { it.suppressSystemExit = true }
        PApplet.runSketch(arrayOf("space.kiibou.Minesweeper"), app)

        awaitUntil("first frame drawn", 30_000) { app.frameCount > 0 }
        app.registerMethod("draw", hook)

        robot = GuiRobot(app) {
            awaitUntil("events drained by the frame loop", 5_000) { app.eventDispatcher.isIdle() }
        }
    }

    /** Returns the shared sketch to the main menu with no joined room. */
    private fun reset() {
        app.client.send(MinesweeperMessageType.LeaveRoom)
        app.showMainMenu()
        awaitUntil("main menu visible", 5_000) { !robot.findByTag("screen.menu").effectivelyHidden }
    }

    fun awaitUntil(what: String, timeoutMs: Long, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            Thread.sleep(20)
        }
        fail("Timed out waiting for: $what")
    }

    /** Reads a pixel from the live frame, executed on the sketch's draw thread. */
    fun probe(x: Int, y: Int): Int {
        hook.probeRequest = x to y
        awaitUntil("pixel probe at ($x,$y)", 5_000) { hook.probeResult != null }
        return hook.probeResult!!.also { hook.probeResult = null }
    }

    /** Saves the current frame, executed on the sketch's draw thread. */
    fun screenshot(path: String) {
        File(path).parentFile?.mkdirs()
        hook.saveRequest = path
        awaitUntil("screenshot to $path", 5_000) { hook.saveRequest == null }
    }

    /** Registered with Processing: [draw] runs after every frame on the sketch thread. */
    class DrawHook {
        @Volatile var probeRequest: Pair<Int, Int>? = null
        @Volatile var probeResult: Int? = null
        @Volatile var saveRequest: String? = null

        @Suppress("unused")
        fun draw() {
            probeRequest?.let { (x, y) ->
                probeRequest = null
                probeResult = app.get(x, y)
            }

            saveRequest?.let { path ->
                app.g.save(path)
                saveRequest = null
            }
        }
    }
}
