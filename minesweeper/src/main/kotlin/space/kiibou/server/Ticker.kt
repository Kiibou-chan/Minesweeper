package space.kiibou.server

import java.util.Timer
import kotlin.concurrent.fixedRateTimer

/**
 * Abstraction over the game clock. Production uses a real background timer;
 * tests use a manual ticker that advances time on command, so the timer never
 * spawns a thread or depends on wall-clock time in a test.
 */
interface Ticker {
    fun start(onTick: () -> Unit)
    fun stop()
}

class FixedRateTicker(private val name: String) : Ticker {
    private var timer: Timer? = null

    override fun start(onTick: () -> Unit) {
        stop()
        timer = fixedRateTimer(name, daemon = true, initialDelay = 0L, period = 1000L) { onTick() }
    }

    override fun stop() {
        timer?.cancel()
        timer = null
    }
}
