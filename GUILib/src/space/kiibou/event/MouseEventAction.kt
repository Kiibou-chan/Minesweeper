package space.kiibou.event

import processing.event.MouseEvent
import java.util.*

enum class MouseEventAction(private val id: Int) {
    PRESS(MouseEvent.PRESS),
    RELEASE(MouseEvent.RELEASE),
    CLICK(MouseEvent.CLICK),
    DRAG(MouseEvent.DRAG),
    MOVE(MouseEvent.MOVE),
    WINDOW_ENTER(MouseEvent.ENTER),
    WINDOW_EXIT(MouseEvent.EXIT),
    WHEEL(MouseEvent.WHEEL),
    ELEMENT_ENTER(-1),
    ELEMENT_EXIT(-2);

    companion object {
        private val mapper: Map<Int, MouseEventAction> = values().filter { it.id > 0 }.map { it.id to it }.toMap()

        fun fromProcessingEvent(event: MouseEvent): EnumSet<MouseEventAction> = EnumSet.of(mapper[event.action])
    }

}