package space.kiibou.gui.vector

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

data class Color @JsonCreator constructor(
        @param:JsonProperty("red") private val red: Int,
        @param:JsonProperty("green") private val green: Int,
        @param:JsonProperty("blue") private val blue: Int,
        @param:JsonProperty("alpha") private val alpha: Int) {

    @JsonIgnore
    private val color: Int = alpha and 0xFF shl 24 or (red and 0xFF shl 16) or (green and 0xFF shl 8) or (blue and 0xFF)

    fun toInt(): Int {
        return color
    }

}