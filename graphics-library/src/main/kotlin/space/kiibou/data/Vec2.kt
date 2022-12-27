package space.kiibou.data

data class Vec2(val x: Int, val y: Int) {
    fun scale(scalar: Int): Vec2 {
        return Vec2(x * scalar, y * scalar)
    }
}