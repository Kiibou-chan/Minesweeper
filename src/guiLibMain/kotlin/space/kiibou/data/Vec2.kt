package space.kiibou.data

import kotlinx.serialization.Serializable

@Serializable
data class Vec2(val x: Int, val y: Int) {
    fun scale(scalar: Int): Vec2 {
        return Vec2(x * scalar, y * scalar)
    }
}