package space.kiibou.gui

import space.kiibou.GApplet

/**
 * Switches between top-level views ("screens"). Every screen stays registered with the
 * app; all but the current one are hidden (not drawn) and inactive (skipped by event
 * dispatch), so showing a screen is a pair of flag flips, not a re-registration.
 */
class ScreenManager(private val app: GApplet) {

    private val screens = mutableListOf<GraphicsElement>()

    var current: GraphicsElement? = null
        private set

    fun add(screen: GraphicsElement) {
        screens += screen
        app.registerGraphicsElement(screen)
        screen.hide()
        screen.deactivate()
    }

    fun show(screen: GraphicsElement) {
        require(screen in screens) { "Screen $screen was never added to this ScreenManager" }

        current?.let {
            it.hide()
            it.deactivate()
        }

        screen.show()
        screen.activate()
        current = screen
    }
}
