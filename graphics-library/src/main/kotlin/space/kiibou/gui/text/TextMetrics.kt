package space.kiibou.gui.text

/**
 * Text measurement seam. Production leaves [space.kiibou.GApplet.textMetrics] null and
 * measures through [FontRegistry] + the GL renderer; headless tests install
 * [EstimatingTextMetrics] so text widgets can size themselves without a running sketch.
 */
interface TextMetrics {
    fun width(fontName: String, sizePx: Int, text: String): Int
    fun height(fontName: String, sizePx: Int, text: String): Int
}

/** Deterministic estimate: good enough for headless layout and hit-testing. */
object EstimatingTextMetrics : TextMetrics {
    override fun width(fontName: String, sizePx: Int, text: String): Int =
        (text.length * sizePx * 0.6).toInt()

    override fun height(fontName: String, sizePx: Int, text: String): Int = sizePx
}
