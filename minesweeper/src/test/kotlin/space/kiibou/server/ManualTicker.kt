package space.kiibou.server

/** Test double that fires ticks only when [advance] is called. */
class ManualTicker : Ticker {
    private var onTick: (() -> Unit)? = null
    var running = false
        private set

    override fun start(onTick: () -> Unit) {
        this.onTick = onTick
        running = true
    }

    override fun stop() {
        running = false
    }

    fun advance(seconds: Int) {
        repeat(seconds) { if (running) onTick?.invoke() }
    }
}
