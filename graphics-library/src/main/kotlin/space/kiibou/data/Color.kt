package space.kiibou.data

@JvmInline
value class Color(val value: Int) {
    constructor(
        red: Int,
        green: Int,
        blue: Int,
        alpha: Int = 0xFF
    ) : this(((alpha and 0xFF) shl 24) or ((red and 0xFF) shl 16) or ((green and 0xFF) shl 16) or (blue and 0xFF))

    val red: Int
        get() = (value shr 16) and 0xFF

    val green: Int
        get() = (value shr 8) and 0xFF

    val blue: Int
        get() = value and 0xFF

    val alpha: Int
        get() = (value shr 24) and 0xFF
}

val BLACK = Color(0)
val WHITE = Color(255)
val RED = Color(255, 0, 0)
val GREEN = Color(0, 255, 0)
val BLUE = Color(0, 0, 255)
