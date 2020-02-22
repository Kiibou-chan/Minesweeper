package space.kiibou.event

import processing.core.PConstants
import processing.event.MouseEvent

enum class MouseEventButton(private val id: Int) {
    LEFT(PConstants.LEFT),
    RIGHT(PConstants.RIGHT),
    CENTER(PConstants.CENTER);

    companion object {
        private val mapper: Map<Int, MouseEventButton> = values().map { it.id to it }.toMap()

        fun fromProcessingEvent(event: MouseEvent): MouseEventButton {
            return mapper[event.button]
                    ?: error("Could not associate processingMouseButton:${event.button} with a MouseEventButton")
        }
    }

}