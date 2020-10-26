package space.kiibou.data

data class Color(val red: Int, val green: Int, val blue: Int, val alpha: Int = 255) {
    constructor(grey: Int) : this(grey, grey, grey)
}

fun Color.toInt(): Int = (alpha shl 24) or (red shl 16) or (green shl 8) or blue
val BLACK = Color(0)
val WHITE = Color(255)
val RED = Color(255, 0, 0)
val GREEN = Color(0, 255, 0)
val BLUE = Color(0, 0, 255)
