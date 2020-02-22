package space.kiibou.gui.vector

enum class Constants(val kind: Int) {
    NONE(0),
    POINTS(3),
    LINES(5),
    TRIANGLES(9),
    TRIANGLE_FAN(11),
    TRIANGLE_STRIP(10),
    QUADS(17),
    QUAD_STRIP(18),
    CLOSE(2),
    ROUND(2),
    PROJECT(4),
    MITER(8),
    BEVEL(32);
}