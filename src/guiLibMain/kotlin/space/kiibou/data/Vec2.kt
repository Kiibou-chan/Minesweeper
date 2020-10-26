package space.kiibou.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class Vec2 @JsonCreator constructor(
        @param:JsonProperty("x") val x: Int,
        @param:JsonProperty("y") val y: Int) {

    fun scale(scalar: Int): Vec2 {
        return Vec2(x * scalar, y * scalar)
    }
}