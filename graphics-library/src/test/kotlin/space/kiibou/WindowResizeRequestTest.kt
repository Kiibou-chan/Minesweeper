package space.kiibou

import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class WindowResizeRequestTest {

    private class RecordingApp : GApplet() {
        val applied = CompletableFuture<Pair<Pair<Int, Int>, Thread>>()

        override fun applyWindowSize(width: Int, height: Int) {
            applied.complete((width to height) to Thread.currentThread())
        }
    }

    @Test
    fun requested_window_size_is_applied_off_the_calling_thread() {
        val app = RecordingApp()

        app.requestWindowSize(800, 600)

        val (size, thread) = app.applied.get(5, TimeUnit.SECONDS)
        assertEquals(800 to 600, size)
        assertNotEquals(Thread.currentThread(), thread)
    }
}
